/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.keywords.names.EmbeddedKeywordNamesSupport;
import org.rf.ide.core.testdata.model.table.setting.SuiteSetup;
import org.rf.ide.core.testdata.model.table.setting.TestSetup;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.ValidationReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.TestCasesProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.FileValidationContext.ValidationKeywordEntity;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.versiondependent.VersionDependentValidators;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.RangeSet;


class TestCaseValidator implements ModelUnitValidator {

    private final FileValidationContext validationContext;
    private final ValidationReportingStrategy reporter;
    private final VersionDependentValidators versionDependentValidators;

    private final TestCase testCase;

    TestCaseValidator(final FileValidationContext validationContext, final TestCase testCase,
            final ValidationReportingStrategy reporter) {
        this.validationContext = validationContext;
        this.reporter = reporter;
        this.versionDependentValidators = new VersionDependentValidators(validationContext, reporter);
        this.testCase = testCase;
    }

    @Override
    public void validate(final IProgressMonitor monitor) {
        reportVersionSpecificProblems();
        reportEmptyNamesOfCase();
        reportLineContinuationAsNameOfCase();
        reportEmptyCase();

        validateSettings();
        validateKeywordsAndVariablesUsages();
    }

    private void reportVersionSpecificProblems() {
        versionDependentValidators.getTestCaseValidators(testCase).forEach(ModelUnitValidator::validate);
    }

    private void reportEmptyNamesOfCase() {
        final RobotToken caseName = testCase.getName();
        if (caseName.getText().trim().isEmpty()) {
            reporter.handleProblem(RobotProblem.causedBy(TestCasesProblem.EMPTY_CASE_NAME), validationContext.getFile(),
                    caseName);
        }
    }

    private void reportLineContinuationAsNameOfCase() {
        final RobotToken caseName = testCase.getName();
        final String name = caseName.getText();
        if ("...".equals(name.trim())) {
            TestCasesProblem cause;
            if (validationContext.getVersion().isNewerOrEqualTo(new RobotVersion(3, 2))) {
                cause = TestCasesProblem.TEST_CASE_NAME_IS_LINE_CONTINUATION_3_2;
            } else {
                cause = TestCasesProblem.TEST_CASE_NAME_IS_LINE_CONTINUATION_PRE_3_2;
            }
            final RobotProblem problem = RobotProblem.causedBy(cause).formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), caseName, arguments);
        }
    }

    private void reportEmptyCase() {
        final RobotToken caseName = testCase.getName();

        if (!hasAnythingToExecute(testCase)) {
            final String name = caseName.getText();
            final RobotProblem problem = RobotProblem.causedBy(TestCasesProblem.EMPTY_CASE).formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), caseName, arguments);
        }
    }

    private boolean hasAnythingToExecute(final TestCase testCase) {
        return !testCase.getExecutionContext().isEmpty();
    }

    private void validateSettings() {
        new TestCaseSettingsValidator(validationContext, testCase, reporter).validate();
    }

    private void validateKeywordsAndVariablesUsages() {
        final Set<String> additionalVariables = new HashSet<>();
        final List<ExecutableValidator> execValidators = new ArrayList<>();

        final SilentReporter silentReporter = new SilentReporter();

        // not validated; will just add variables if any
        getGeneralSettingsSuiteSetups().stream()
                .findFirst()
                .map(suiteSetup -> ExecutableValidator.of(validationContext, additionalVariables, suiteSetup,
                        silentReporter))
                .ifPresent(execValidators::add);

        if (!testCase.getSetups().isEmpty()) {
            testCase.getSetups().stream()
                    .map(setup -> ExecutableValidator.of(validationContext, additionalVariables, setup, reporter))
                    .forEach(execValidators::add);
        } else {
            // not validated; will just add variables if any
            getGeneralSettingsTestSetup().stream()
                    .findFirst()
                    .map(setup -> ExecutableValidator.of(validationContext, additionalVariables, setup, silentReporter))
                    .ifPresent(execValidators::add);
        }

        final Optional<String> templateKeywordName = testCase.getTemplateKeywordName();
        if (templateKeywordName.isPresent()) {
            final ValidationKeywordEntity foundKeyword = validationContext
                    .findAccessibleKeyword(templateKeywordName.get());
            final RangeSet<Integer> embeddedArguments = EmbeddedKeywordNamesSupport
                    .findEmbeddedArgumentsRanges(templateKeywordName.get());
            if (foundKeyword != null || !embeddedArguments.isEmpty()) {
                testCase.getExecutionContext()
                        .stream()
                        .map(row -> ExecutableValidator.of(validationContext, additionalVariables, row,
                                templateKeywordName.get(), foundKeyword, embeddedArguments, reporter))
                        .forEach(execValidators::add);
            }
        } else {
            testCase.getExecutionContext()
                    .stream()
                    .map(row -> ExecutableValidator.of(validationContext, additionalVariables, row, reporter))
                    .forEach(execValidators::add);
        }

        testCase.getTeardowns()
                .stream()
                .map(teardown -> ExecutableValidator.of(validationContext, additionalVariables, teardown, reporter))
                .forEach(execValidators::add);
        execValidators.forEach(ExecutableValidator::validate);
    }

    private List<TestSetup> getGeneralSettingsTestSetup() {
        final RobotFile fileModel = testCase.getParent().getParent();
        return fileModel.getSettingTable().getTestSetups();
    }

    private List<SuiteSetup> getGeneralSettingsSuiteSetups() {
        final RobotFile fileModel = testCase.getParent().getParent();
        return fileModel.getSettingTable().getSuiteSetups();
    }
}
