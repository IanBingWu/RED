/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.search.keyword.KeywordScope;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.keywords.names.EmbeddedKeywordNamesSupport;
import org.rf.ide.core.testdata.model.table.setting.SuiteSetup;
import org.rf.ide.core.testdata.model.table.variables.descs.VariablesAnalyzer;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.robotframework.ide.eclipse.main.plugin.model.locators.KeywordEntity;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.ValidationReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.KeywordsProblem;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;


class KeywordValidator implements ModelUnitValidator {

    private static final Pattern VARIABLES_ONLY_PATTERN = Pattern.compile("^([$&@]\\{[^{}]+\\})+$");

    private final FileValidationContext validationContext;
    private final ValidationReportingStrategy reporter;

    private final UserKeyword keyword;

    KeywordValidator(final FileValidationContext validationContext, final UserKeyword keyword,
            final ValidationReportingStrategy reporter) {
        this.validationContext = validationContext;
        this.keyword = keyword;
        this.reporter = reporter;
    }

    @Override
    public void validate(final IProgressMonitor monitor) {
        reportVariableAsKeywordName();
        reportKeywordNameWithDots();
        reportMaskingKeyword();
        reportEmptyKeyword();

        validateSettings();
        validateKeywordsAndVariablesUsages();
    }

    private void reportVariableAsKeywordName() {
        final RobotToken keywordName = keyword.getName();
        final String name = keywordName.getText();

        if (VARIABLES_ONLY_PATTERN.matcher(name).matches()) {
            final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.VARIABLE_AS_KEYWORD_NAME)
                    .formatMessageWith(name);

            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), keywordName, arguments);
        }
    }

    private void reportKeywordNameWithDots() {
        final RobotToken keywordName = keyword.getName();
        final String name = keywordName.getText();
        if (name.contains(".")) {
            final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.KEYWORD_NAME_WITH_DOTS)
                    .formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), keywordName, arguments);
        }
    }

    private void reportMaskingKeyword() {
        final RobotToken keywordName = keyword.getName();
        final String name = keywordName.getText();
        final ListMultimap<KeywordScope, KeywordEntity> possibleKeywords = validationContext.getPossibleKeywords(name,
                true);
        for (final KeywordScope scope : KeywordScope.defaultOrder()) {
            if (scope != KeywordScope.LOCAL && !possibleKeywords.get(scope).isEmpty()) {
                final KeywordEntity maskedKeyword = possibleKeywords.get(scope).get(0);
                final String maskedName = maskedKeyword.getNameFromDefinition();
                final String maskedSource = maskedKeyword.getSourceNameInUse();
                final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.KEYWORD_MASKS_OTHER_KEYWORD)
                        .formatMessageWith(name, maskedName, maskedSource, maskedSource + "." + maskedName);
                reporter.handleProblem(problem, validationContext.getFile(), keywordName);
            }
        }
    }

    private void reportEmptyKeyword() {
        final RobotToken keywordName = keyword.getName();
        final String name = keywordName.getText();
        if (isReturnEmpty(keyword) && !hasAnythingToExecute(keyword)) {
            final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.EMPTY_KEYWORD).formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.of(AdditionalMarkerAttributes.NAME, name);
            reporter.handleProblem(problem, validationContext.getFile(), keywordName, arguments);
        }
    }

    private boolean hasAnythingToExecute(final UserKeyword keyword) {
        return !keyword.getExecutionContext().isEmpty();
    }

    private boolean isReturnEmpty(final UserKeyword keyword) {
        return !keyword.getReturns()
                .stream()
                .reduce((a, b) -> b)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .flatMap(ret -> ret.tokensOf(RobotTokenType.KEYWORD_SETTING_RETURN_VALUE))
                .anyMatch(token -> !token.getText().trim().isEmpty());
    }

    private void validateSettings() {
        new KeywordSettingsValidator(validationContext, keyword, reporter).validate();
    }

    private void validateKeywordsAndVariablesUsages() {
        final Set<String> additionalVariables = new HashSet<>();
        additionalVariables.addAll(extractArgumentVariables());

        final List<ExecutableValidator> execValidators = new ArrayList<>();
        // not validated; will just add variables if any
        getGeneralSettingsSuiteSetups().stream()
                .findFirst()
                .map(suiteSetup -> ExecutableValidator.of(validationContext, additionalVariables, suiteSetup,
                        new SilentReporter()))
                .ifPresent(execValidators::add);

        keyword.getExecutionContext().stream()
                .map(row -> ExecutableValidator.of(validationContext, additionalVariables, row, reporter))
                .forEach(execValidators::add);
        keyword.getTeardowns().stream()
                .map(teardown -> ExecutableValidator.of(validationContext, additionalVariables, teardown, reporter))
                .forEach(execValidators::add);
        execValidators.forEach(ExecutableValidator::validate);

        // also validate variables in [Return] after all executables were checked (that's why this
        // is done here not in KeywordSettingsValidator)
        final UnknownVariables unknownVarsValidator = new UnknownVariables(validationContext, reporter);
        for (final LocalSetting<UserKeyword> kwReturn : keyword.getReturns()) {
            unknownVarsValidator.reportUnknownVars(additionalVariables,
                    kwReturn.tokensOf(RobotTokenType.KEYWORD_SETTING_RETURN_VALUE).collect(toList()));
        }
    }

    private Collection<String> extractArgumentVariables() {
        final VariablesAnalyzer varAnalyzer = VariablesAnalyzer.analyzer(validationContext.getVersion());

        final Set<String> arguments = new HashSet<>();
        // first add arguments embedded in name
        Stream.of(keyword.getName())
                .map(nameToken -> varAnalyzer.getVariablesUnified(nameToken))
                .map(Multimap::keySet)
                .flatMap(Set::stream)
                .map(EmbeddedKeywordNamesSupport::removeRegex)
                .forEach(arguments::add);

        // second add arguments from [Arguments] setting
        keyword.getArguments()
                .stream()
                .flatMap(arg -> arg.tokensOf(RobotTokenType.KEYWORD_SETTING_ARGUMENT))
                .map(argToken -> varAnalyzer.getVariablesUnified(argToken))
                .map(Multimap::keySet)
                .flatMap(Set::stream)
                .forEach(arguments::add);
        return arguments;
    }

    private List<SuiteSetup> getGeneralSettingsSuiteSetups() {
        final RobotFile fileModel = keyword.getParent().getParent();
        return fileModel.getSettingTable().getSuiteSetups();
    }
}
