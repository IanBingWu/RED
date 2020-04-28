/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.keywords.names.EmbeddedKeywordNamesSupport;
import org.rf.ide.core.testdata.model.table.variables.descs.VariableUse;
import org.rf.ide.core.testdata.model.table.variables.descs.VariablesAnalyzer;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.validation.RobotTimeFormat;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.ValidationReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ArgumentProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.GeneralSettingsProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.KeywordsProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.versiondependent.VersionDependentValidators;

import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * @author Michal Anglart
 *
 */
class KeywordSettingsValidator implements ModelUnitValidator {

    private static final Pattern VAR_ARG_PATTERN = Pattern.compile("^[$@&]\\{[^\\}]+\\}");

    private final FileValidationContext validationContext;

    private final UserKeyword keyword;

    private final ValidationReportingStrategy reporter;

    private final VersionDependentValidators versionDependentValidators;

    KeywordSettingsValidator(final FileValidationContext validationContext, final UserKeyword keyword,
            final ValidationReportingStrategy reporter) {
        this.validationContext = validationContext;
        this.keyword = keyword;
        this.reporter = reporter;
        this.versionDependentValidators = new VersionDependentValidators(validationContext, reporter);
    }

    @Override
    public void validate(final IProgressMonitor monitor) throws CoreException {
        reportVersionSpecificProblems();
        reportUnknownSettings();

        reportReturnProblems();
        reportTagsProblems();
        reportTimeoutsProblems();
        reportDocumentationsProblems();
        reportTeardownProblems();
        reportArgumentsProblems();

        reportUnknownVariablesInNonExecutables();
    }

    private void reportVersionSpecificProblems() {
        versionDependentValidators.getKeywordSettingsValidators(keyword).forEach(ModelUnitValidator::validate);
    }

    private void reportUnknownSettings() {
        final List<LocalSetting<UserKeyword>> unknownSettings = keyword.getUnknownSettings();
        for (final LocalSetting<UserKeyword> unknownSetting : unknownSettings) {
            final RobotToken token = unknownSetting.getDeclaration();
            final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.UNKNOWN_KEYWORD_SETTING)
                    .formatMessageWith(token.getText());
            final String robotVersion = validationContext.getVersion().asString();
            reporter.handleProblem(problem, validationContext.getFile(), token,
                    ImmutableMap.of(AdditionalMarkerAttributes.NAME, token.getText(),
                            AdditionalMarkerAttributes.ROBOT_VERSION, robotVersion));
        }
    }

    private void reportReturnProblems() {
        keyword.getReturns().stream()
                .filter(ret -> ret.getToken(RobotTokenType.KEYWORD_SETTING_RETURN_VALUE) == null)
                .forEach(this::reportEmptySetting);
    }

    private void reportTagsProblems() {
        keyword.getTags().stream()
                .filter(tag -> tag.getToken(RobotTokenType.KEYWORD_SETTING_TAGS_TAG_NAME) == null)
                .forEach(this::reportEmptySetting);
    }

    private void reportTimeoutsProblems() {
        keyword.getTimeouts().stream()
                .filter(tag -> tag.getToken(RobotTokenType.KEYWORD_SETTING_TIMEOUT_VALUE) == null)
                .forEach(this::reportEmptySetting);

        reportInvalidTimeoutSyntax(keyword.getTimeouts());
    }

    private void reportInvalidTimeoutSyntax(final List<LocalSetting<UserKeyword>> timeouts) {
        for (final LocalSetting<UserKeyword> kwTimeout : timeouts) {
            final RobotToken timeoutToken = kwTimeout.getToken(RobotTokenType.KEYWORD_SETTING_TIMEOUT_VALUE);
            if (timeoutToken != null) {
                final String timeout = timeoutToken.getText();
                if (!timeoutToken.getTypes().contains(RobotTokenType.VARIABLE_USAGE)
                        && !timeout.equalsIgnoreCase("none")
                        && !RobotTimeFormat.isValidRobotTimeArgument(timeout.trim())) {
                    final RobotProblem problem = RobotProblem.causedBy(ArgumentProblem.INVALID_TIME_FORMAT)
                            .formatMessageWith(timeout);
                    reporter.handleProblem(problem, validationContext.getFile(), timeoutToken);
                }
            }
        }
    }

    private void reportDocumentationsProblems() {
        keyword.getDocumentation().stream().findFirst()
                .filter(doc -> doc.getToken(RobotTokenType.KEYWORD_SETTING_DOCUMENTATION_TEXT) == null)
                .ifPresent(this::reportEmptySetting);
    }

    private void reportTeardownProblems() {
        keyword.getTeardowns().stream()
                .filter(teardown -> teardown.getToken(RobotTokenType.KEYWORD_SETTING_TEARDOWN_KEYWORD_NAME) == null)
                .forEach(this::reportEmptySetting);
    }

    private void reportEmptySetting(final AModelElement<?> element) {
        final RobotToken defToken = element.getDeclaration();
        final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.EMPTY_KEYWORD_SETTING)
                .formatMessageWith(defToken.getText());
        reporter.handleProblem(problem, validationContext.getFile(), defToken);
    }

    private void reportUnknownVariablesInNonExecutables() {
        final UnknownVariables unknownVarsValidator = new UnknownVariables(validationContext, reporter);

        for (final LocalSetting<UserKeyword> kwTimeout : keyword.getTimeouts()) {
            unknownVarsValidator.reportUnknownVars(kwTimeout.getToken(RobotTokenType.KEYWORD_SETTING_TIMEOUT_VALUE));
        }
        for (final LocalSetting<UserKeyword> tag : keyword.getTags()) {
            unknownVarsValidator
                    .reportUnknownVars(tag.tokensOf(RobotTokenType.KEYWORD_SETTING_TAGS_TAG_NAME).collect(toList()));
        }
        // KeywordReturn are validated in KeywordValidator, since it requires variables created by executables
    }

    private void reportArgumentsProblems() {
        keyword.getArguments()
                .stream()
                .filter(args -> args.getToken(RobotTokenType.KEYWORD_SETTING_ARGUMENT) == null)
                .forEach(this::reportEmptySetting);

        if (!keyword.getArguments().isEmpty() && hasEmbeddedArguments(keyword)) {
            reporter.handleProblem(
                    RobotProblem.causedBy(GeneralSettingsProblem.DUPLICATED_SETTING)
                            .formatMessageWith("[Arguments]", ". There are variables defined in keyword name"),
                    validationContext.getFile(), keyword.getDeclaration());
        }

        final boolean shouldContinue = reportArgumentsSyntaxProblems();
        if (shouldContinue) {
            reportDuplicatedArguments();
            reportArgumentsOrderProblems();
            reportArgumentsDefaultValuesUnknownVariables();
        }
    }

    private boolean reportArgumentsSyntaxProblems() {
        final AtomicBoolean shouldContinue = new AtomicBoolean(true);
        for (final LocalSetting<UserKeyword> argSetting : keyword.getArguments()) {
            argSetting.tokensOf(RobotTokenType.KEYWORD_SETTING_ARGUMENT)
                    .filter(token -> !hasValidArgumentSyntax(token))
                    .forEach(argToken -> {
                        shouldContinue.set(false);
                        final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.INVALID_KEYWORD_ARG_SYNTAX)
                                .formatMessageWith(argToken.getText());
                        reporter.handleProblem(problem, validationContext.getFile(), argToken);
                    });
        }
        return shouldContinue.get();
    }

    private boolean hasValidArgumentSyntax(final RobotToken argToken) {
        final Matcher matcher = VAR_ARG_PATTERN.matcher(argToken.getText());
        if (matcher.find() && matcher.start() == 0) {
            final String rest = argToken.getText().substring(matcher.end());
            return rest.isEmpty() || rest.startsWith("=") && argToken.getText().startsWith("$");
        }
        // if keyword-only args are supported then @{} marks place where positional arguments ends
        return keywordOnlyArgumentsAreSupported() ? "@{}".equals(argToken.getText()) : false;
    }

    private void reportDuplicatedArguments() {
        final Multimap<String, RobotToken> arguments = extractArgumentVariables(keyword);

        for (final String arg : arguments.keySet()) {
            final Collection<RobotToken> tokens = arguments.get(arg);
            if (tokens.size() > 1) {
                for (final RobotToken token : tokens) {
                    final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.ARGUMENT_DEFINED_TWICE)
                            .formatMessageWith(token.getText());
                    reporter.handleProblem(problem, validationContext.getFile(), token);
                }
            }
        }
    }

    private Multimap<String, RobotToken> extractArgumentVariables(final UserKeyword keyword) {
        final VariablesAnalyzer varAnalyzer = VariablesAnalyzer.analyzer(validationContext.getVersion());
        
        final Multimap<String, RobotToken> arguments = ArrayListMultimap.create();

        // first add arguments embedded in name, then from [Arguments] setting
        final Map<String, List<RobotToken>> embeddedArguments = varAnalyzer.getDefinedVariablesUses(keyword.getName())
                .map(VariableUse::asToken)
                .collect(groupingBy(VariablesAnalyzer::normalizeName));

        for (final String argName : embeddedArguments.keySet()) {
            arguments.putAll(EmbeddedKeywordNamesSupport.removeRegex(argName), embeddedArguments.get(argName));
        }
        for (final LocalSetting<UserKeyword> argument : keyword.getArguments()) {
            for (final RobotToken token : argument.tokensOf(RobotTokenType.KEYWORD_SETTING_ARGUMENT)
                    .collect(toList())) {
                final boolean hasDefault = token.getText().contains("=");
                final Map<String, List<RobotToken>> usedVariables = varAnalyzer.getDefinedVariablesUses(token)
                        .map(VariableUse::asToken)
                        .collect(groupingBy(VariablesAnalyzer::normalizeName));
                if (hasDefault) {
                    final List<String> splitted = Splitter.on('=').limit(2).splitToList(token.getText());
                    final String def = splitted.get(0);
                    final String unifiedDefinitionName = VariablesAnalyzer.normalizeName(def);
                    arguments.put(unifiedDefinitionName,
                            usedVariables.get(unifiedDefinitionName).stream().findFirst().orElse(null));
                } else {
                    for (final String argName : usedVariables.keySet()) {
                        arguments.putAll(argName, usedVariables.get(argName));
                    }
                }
            }
        }
        return arguments;
    }

    private boolean hasEmbeddedArguments(final UserKeyword keyword) {
        return VariablesAnalyzer.analyzer(validationContext.getVersion(), VariablesAnalyzer.ALL_ROBOT)
                .containsVariables(keyword.getName());
    }

    private void reportArgumentsOrderProblems() {
        final List<LocalSetting<UserKeyword>> arguments = keyword.getArguments();
        if (arguments == null || arguments.isEmpty()) {
            return;
        }
        boolean wasVararg = false;
        boolean wasKwarg = false;
        boolean wasDefault = false;
        final List<RobotToken> tokens = arguments.get(arguments.size() - 1)
                .tokensOf(RobotTokenType.KEYWORD_SETTING_ARGUMENT)
                .collect(toList());
        for (final RobotToken argumentToken : tokens) {
            final boolean isDefault = isDefaultArgument(argumentToken);
            final boolean isVararg = argumentToken.getTypes().contains(RobotTokenType.VARIABLES_LIST_DECLARATION);
            final boolean isKwarg = argumentToken.getTypes().contains(RobotTokenType.VARIABLES_DICTIONARY_DECLARATION);

            if (wasDefault && !isDefault && !isVararg && !wasVararg && !isKwarg) {
                final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.NON_DEFAULT_ARGUMENT_AFTER_DEFAULT)
                        .formatMessageWith(argumentToken.getText());
                reporter.handleProblem(problem, validationContext.getFile(), argumentToken);
            }
            // when keyword-only arguments are supported then they are placed after vararg
            if (keywordOnlyArgumentsAreSupported() && wasVararg && isVararg
                    || !keywordOnlyArgumentsAreSupported() && wasVararg && !isKwarg) {
                final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.ARGUMENT_AFTER_VARARG)
                        .formatMessageWith(argumentToken.getText());
                reporter.handleProblem(problem, validationContext.getFile(), argumentToken);
            }
            if (wasKwarg) {
                final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.ARGUMENT_AFTER_KWARG)
                        .formatMessageWith(argumentToken.getText());
                reporter.handleProblem(problem, validationContext.getFile(), argumentToken);
            }
            wasVararg |= isVararg;
            wasDefault |= isDefault;
            wasKwarg |= isKwarg;
        }
    }

    private void reportArgumentsDefaultValuesUnknownVariables() {
        final VariablesAnalyzer varAnalyzer = VariablesAnalyzer.analyzer(validationContext.getVersion());

        for (final LocalSetting<UserKeyword> argSetting : keyword.getArguments()) {
            final Set<String> additionalKnownVariables = new HashSet<>();

            final List<RobotToken> tokens = argSetting.tokensOf(RobotTokenType.KEYWORD_SETTING_ARGUMENT)
                    .collect(toList());
            for (final RobotToken argToken : tokens) {
                final boolean hasDefault = argToken.getText().contains("=");
                if (hasDefault) {
                    final List<String> splitted = Splitter.on('=').limit(2).splitToList(argToken.getText());
                    final String def = splitted.get(0);

                    final String unifiedDefinitionName = VariablesAnalyzer.normalizeName(def);
                    final Stream<VariableUse> usedVariables = varAnalyzer.getDefinedVariablesUses(argToken);

                    final List<RobotToken> varTokens = usedVariables
                            .map(VariableUse::asToken)
                            .filter(varToken -> varToken.getStartOffset() != argToken.getStartOffset()
                                    || varToken.getEndOffset() != argToken.getStartOffset() + def.length())
                            .collect(toList());
                    new UnknownVariables(validationContext, reporter).reportUnknownVars(additionalKnownVariables,
                            varTokens);
                    additionalKnownVariables.add(unifiedDefinitionName);
                } else {
                    additionalKnownVariables.add(VariablesAnalyzer.normalizeName(argToken.getText()));
                }
            }
        }
    }

    private boolean isDefaultArgument(final RobotToken argumentToken) {
        return argumentToken.getText().contains("}=");
    }

    private boolean keywordOnlyArgumentsAreSupported() {
        return validationContext.getVersion().isNewerOrEqualTo(new RobotVersion(3, 1));
    }
}
