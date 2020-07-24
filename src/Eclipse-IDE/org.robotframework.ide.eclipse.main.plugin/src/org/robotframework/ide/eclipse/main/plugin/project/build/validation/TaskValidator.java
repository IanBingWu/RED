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
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.keywords.names.EmbeddedKeywordNamesSupport;
import org.rf.ide.core.testdata.model.table.setting.SuiteSetup;
import org.rf.ide.core.testdata.model.table.setting.TaskSetup;
import org.rf.ide.core.testdata.model.table.tasks.Task;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.ValidationReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.TasksProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.FileValidationContext.ValidationKeywordEntity;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.versiondependent.VersionDependentValidators;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.RangeSet;


class TaskValidator implements ModelUnitValidator {

    private final FileValidationContext validationContext;
    private final ValidationReportingStrategy reporter;
    private final VersionDependentValidators versionDependentValidators;

    private final Task task;

    TaskValidator(final FileValidationContext validationContext, final Task task,
            final ValidationReportingStrategy reporter) {
        this.validationContext = validationContext;
        this.reporter = reporter;
        this.versionDependentValidators = new VersionDependentValidators(validationContext, reporter);
        this.task = task;
    }

    @Override
    public void validate(final IProgressMonitor monitor) {
        reportVersionSpecificProblems();
        reportEmptyNamesOfTask();
        reportLineContinuationAsNameOfTask();
        reportEmptyTask();

        validateSettings();
        validateKeywordsAndVariablesUsages();
    }

    private void reportVersionSpecificProblems() {
        versionDependentValidators.getTaskValidators(task).forEach(ModelUnitValidator::validate);
    }

    private void reportEmptyNamesOfTask() {
        final RobotToken caseName = task.getName();
        if (caseName.getText().trim().isEmpty()) {
            reporter.handleProblem(RobotProblem.causedBy(TasksProblem.EMPTY_TASK_NAME), validationContext.getFile(),
                    caseName);
        }
    }

    private void reportLineContinuationAsNameOfTask() {
        final RobotToken taskName = task.getName();
        final String name = taskName.getText();
        if ("...".equals(name.trim())) {
            final RobotProblem problem = RobotProblem.causedBy(TasksProblem.TASK_NAME_IS_LINE_CONTINUATION)
                    .formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), taskName, arguments);
        }
    }

    private void reportEmptyTask() {
        final RobotToken caseName = task.getName();

        if (!hasAnythingToExecute(task)) {
            final String name = caseName.getText();
            final RobotProblem problem = RobotProblem.causedBy(TasksProblem.EMPTY_TASK).formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), caseName, arguments);
        }
    }

    private boolean hasAnythingToExecute(final Task task) {
        return !task.getExecutionContext().isEmpty();
    }

    private void validateSettings() {
        new TaskSettingsValidator(validationContext, task, reporter).validate();
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

        if (!task.getSetups().isEmpty()) {
            task.getSetups()
                    .stream()
                    .map(setup -> ExecutableValidator.of(validationContext, additionalVariables, setup, reporter))
                    .forEach(execValidators::add);
        } else {
            // not validated; will just add variables if any
            getGeneralSettingsTaskSetup().stream()
                    .findFirst()
                    .map(setup -> ExecutableValidator.of(validationContext, additionalVariables, setup, silentReporter))
                    .ifPresent(execValidators::add);
        }

        final Optional<String> templateKeywordName = task.getTemplateKeywordName();
        if (templateKeywordName.isPresent()) {
            final ValidationKeywordEntity foundKeyword = validationContext
                    .findAccessibleKeyword(templateKeywordName.get());
            final RangeSet<Integer> embeddedArguments = EmbeddedKeywordNamesSupport
                    .findEmbeddedArgumentsRanges(templateKeywordName.get());
            if (foundKeyword != null || !embeddedArguments.isEmpty()) {
                task.getExecutionContext()
                        .stream()
                        .map(row -> ExecutableValidator.of(validationContext, additionalVariables, row,
                                templateKeywordName.get(), foundKeyword, embeddedArguments, reporter))
                        .forEach(execValidators::add);
            }
        } else {
            task.getExecutionContext()
                    .stream()
                    .map(row -> ExecutableValidator.of(validationContext, additionalVariables, row, reporter))
                    .forEach(execValidators::add);
        }

        task.getTeardowns()
                .stream()
                .map(teardown -> ExecutableValidator.of(validationContext, additionalVariables, teardown, reporter))
                .forEach(execValidators::add);
        execValidators.forEach(ExecutableValidator::validate);
    }

    private List<TaskSetup> getGeneralSettingsTaskSetup() {
        final RobotFile fileModel = task.getParent().getParent();
        return fileModel.getSettingTable().getTaskSetups();
    }

    private List<SuiteSetup> getGeneralSettingsSuiteSetups() {
        final RobotFile fileModel = task.getParent().getParent();
        return fileModel.getSettingTable().getSuiteSetups();
    }
}
