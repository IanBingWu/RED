/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.preferences;


import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder.PythonInstallationDirectory;
import org.rf.ide.core.execution.server.AgentConnectionServer;
import org.rf.ide.core.testdata.formatter.RedFormatter.FormattingSeparatorType;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.CellCommitBehavior;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.CellWrappingStrategy;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.FormatterType;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.IssuesStrategy;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.LibraryPrefixStrategy;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.LinkedModeStrategy;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.MatchingKeywordStrategy;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences.SeparatorsMode;
import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement.ElementOpenMode;
import org.robotframework.ide.eclipse.main.plugin.preferences.InstalledRobotEnvironments.InterpreterWithPath;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotTask.Priority;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ProblemCategory;

import com.google.common.annotations.VisibleForTesting;

public class RedPreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        initializeDefaultPreferences(DefaultScope.INSTANCE.getNode(RedPlugin.PLUGIN_ID),
                PythonInstallationDirectoryFinder::whereArePythonInterpreters);
    }

    @VisibleForTesting
    void initializeDefaultPreferences(final IEclipsePreferences preferences,
            final Supplier<List<PythonInstallationDirectory>> pythonInterpretersFinder) {
        initializeFrameworkPreferences(preferences, pythonInterpretersFinder);
        initializeEditorPreferences(preferences);
        initializeSourceFoldingPreferences(preferences);
        initializeSourceEditorAssistantPreferences(preferences);
        initializeSyntaxColoringPreferences(preferences);
        initializeFormatterPreferences(preferences);
        initializeSaveActionsPreferences(preferences);
        initializeLibrariesPreferences(preferences);
        initializeProblemSeverityPreferences(preferences);
        initializeDefaultLaunchConfigurationPreferences(preferences);
        initializeStringVariablesPreferences(preferences);
        initializeRfLintPreferences(preferences);
        initializeValidationPreferences(preferences);
        initializeTasksPreferences(preferences);
    }

    private void initializeFrameworkPreferences(final IEclipsePreferences preferences,
            final Supplier<List<PythonInstallationDirectory>> pythonInterpretersFinder) {
        final List<PythonInstallationDirectory> pythonDirs = pythonInterpretersFinder.get();
        final PythonInstallationDirectory firstPythonDir = pythonDirs.stream().findFirst().orElse(null);

        final InterpreterWithPath activeInstallation = createInstallation(firstPythonDir);
        preferences.put(RedPreferences.ACTIVE_INSTALLATION,
                InstalledRobotEnvironments.writeInstallation(activeInstallation));

        final List<InterpreterWithPath> allInstallations = pythonDirs.stream()
                .map(RedPreferencesInitializer::createInstallation)
                .collect(Collectors.toList());
        preferences.put(RedPreferences.ALL_INSTALLATIONS,
                InstalledRobotEnvironments.writeInstallations(allInstallations));
    }

    private static InterpreterWithPath createInstallation(final PythonInstallationDirectory dir) {
        return new InterpreterWithPath(
                Optional.ofNullable(dir).map(PythonInstallationDirectory::getInterpreter).orElse(null),
                Optional.ofNullable(dir).map(PythonInstallationDirectory::getAbsolutePath).orElse(null));
    }

    private void initializeEditorPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.PARENT_DIRECTORY_NAME_IN_TAB, false);
        preferences.put(RedPreferences.FILE_ELEMENTS_OPEN_MODE, ElementOpenMode.OPEN_IN_SOURCE.name());
        preferences.putBoolean(RedPreferences.LIBRARY_KEYWORD_HYPERLINKS, true);
        preferences.putBoolean(RedPreferences.KEYWORD_ARGUMENTS_CELL_COLORING, false);
        preferences.putInt(RedPreferences.MINIMAL_NUMBER_OF_ARGUMENT_COLUMNS, 5);
        preferences.put(RedPreferences.BEHAVIOR_ON_CELL_COMMIT, CellCommitBehavior.MOVE_TO_ADJACENT_CELL.name());
        preferences.put(RedPreferences.CELL_WRAPPING, CellWrappingStrategy.SINGLE_LINE_CUT.name());
        preferences.put(RedPreferences.SEPARATOR_MODE, SeparatorsMode.FILE_TYPE_DEPENDENT.name());
        preferences.put(RedPreferences.SEPARATOR_TO_USE, "ssss");
        preferences.putBoolean(RedPreferences.SEPARATOR_JUMP_MODE_ENABLED, false);
        preferences.putBoolean(RedPreferences.VARIABLES_BRACKETS_INSERTION_ENABLED, false);
        preferences.putBoolean(RedPreferences.VARIABLES_BRACKETS_INSERTION_WRAPPING_ENABLED, false);
        preferences.put(RedPreferences.VARIABLES_BRACKETS_INSERTION_WRAPPING_PATTERN, "\\w+");
    }

    private void initializeSourceFoldingPreferences(final IEclipsePreferences preferences) {
        preferences.putInt(RedPreferences.FOLDING_LINE_LIMIT, 2);
        preferences.putBoolean(RedPreferences.FOLDABLE_SECTIONS, true);
        preferences.putBoolean(RedPreferences.FOLDABLE_CASES, true);
        preferences.putBoolean(RedPreferences.FOLDABLE_TASKS, true);
        preferences.putBoolean(RedPreferences.FOLDABLE_KEYWORDS, true);
        preferences.putBoolean(RedPreferences.FOLDABLE_DOCUMENTATION, true);
    }

    private void initializeSourceEditorAssistantPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.ASSISTANT_AUTO_INSERT_ENABLED, false);
        preferences.putBoolean(RedPreferences.ASSISTANT_AUTO_ACTIVATION_ENABLED, true);
        preferences.putInt(RedPreferences.ASSISTANT_AUTO_ACTIVATION_DELAY, 100);
        preferences.put(RedPreferences.ASSISTANT_AUTO_ACTIVATION_CHARS, "");
        preferences.put(RedPreferences.ASSISTANT_KEYWORD_PREFIX_AUTO_ADDITION, LibraryPrefixStrategy.AUTOMATIC.name());
        preferences.putBoolean(RedPreferences.ASSISTANT_KEYWORD_FROM_NOT_IMPORTED_LIBRARY_ENABLED, false);
        preferences.put(RedPreferences.ASSISTANT_LINKED_ARGUMENTS_MODE, LinkedModeStrategy.EXIT_ON_LAST.name());
        preferences.put(RedPreferences.ASSISTANT_MATCHING_KEYWORD, MatchingKeywordStrategy.FIRST_FOUND.name());
    }

    private void initializeSyntaxColoringPreferences(final IEclipsePreferences preferences) {
        for (final SyntaxHighlightingCategory category : EnumSet.allOf(SyntaxHighlightingCategory.class)) {
            preferences.put(category.getPreferenceId(), category.getDefault().toPreferenceString());
        }
    }

    private void initializeFormatterPreferences(final IEclipsePreferences preferences) {
        preferences.put(RedPreferences.FORMATTER_TYPE, FormatterType.CUSTOM.name());
        preferences.putBoolean(RedPreferences.FORMATTER_SEPARATOR_ADJUSTMENT_ENABLED, false);
        preferences.put(RedPreferences.FORMATTER_SEPARATOR_TYPE, FormattingSeparatorType.CONSTANT.name());
        preferences.putInt(RedPreferences.FORMATTER_SEPARATOR_LENGTH, 4);
        preferences.putBoolean(RedPreferences.FORMATTER_IGNORE_LONG_CELLS_ENABLED, true);
        preferences.putInt(RedPreferences.FORMATTER_LONG_CELL_LENGTH_LIMIT, 40);
        preferences.putBoolean(RedPreferences.FORMATTER_RIGHT_TRIM_ENABLED, false);
    }

    private void initializeSaveActionsPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.SAVE_ACTIONS_CODE_FORMATTING_ENABLED, false);
        preferences.putBoolean(RedPreferences.SAVE_ACTIONS_CHANGED_LINES_ONLY_ENABLED, false);
        preferences.putBoolean(RedPreferences.SAVE_ACTIONS_AUTO_DISCOVERING_ENABLED, true);
        preferences.putBoolean(RedPreferences.SAVE_ACTIONS_AUTO_DISCOVERING_SUMMARY_WINDOW_ENABLED, false);
    }

    private void initializeLibrariesPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.PROJECT_MODULES_RECURSIVE_ADDITION_ON_VIRTUALENV_ENABLED, false);
        preferences.putBoolean(RedPreferences.AUTODISCOVERY_GEVENT_SUPPORT, false);
        preferences.putBoolean(RedPreferences.PYTHON_LIBRARIES_LIBDOCS_GENERATION_IN_SEPARATE_PROCESS_ENABLED, true);
        preferences.putInt(RedPreferences.PYTHON_LIBRARIES_LIBDOCS_GENERATION_TIMEOUT, 30);
        preferences.putBoolean(RedPreferences.LIBDOCS_AUTO_RELOAD_ENABLED, true);
    }

    private void initializeProblemSeverityPreferences(final IEclipsePreferences preferences) {
        for (final ProblemCategory category : EnumSet.allOf(ProblemCategory.class)) {
            preferences.put(category.getId(), category.getDefaultSeverity().name());
        }
    }

    private void initializeDefaultLaunchConfigurationPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.LAUNCH_USE_ARGUMENT_FILE, true);
        preferences.putBoolean(RedPreferences.LAUNCH_USE_SINGLE_COMMAND_LINE_ARGUMENT, false);
        preferences.putBoolean(RedPreferences.LAUNCH_USE_SINGLE_FILE_DATA_SOURCE, false);
        preferences.putBoolean(RedPreferences.LIMIT_MSG_LOG_OUTPUT, false);
        preferences.putInt(RedPreferences.LIMIT_MSG_LOG_LENGTH, 80_000);
        preferences.put(RedPreferences.LAUNCH_AGENT_CONNECTION_HOST, AgentConnectionServer.DEFAULT_CONNECTION_HOST);
        preferences.putInt(RedPreferences.LAUNCH_AGENT_CONNECTION_PORT, AgentConnectionServer.DEFAULT_CONNECTION_PORT);
        preferences.putInt(RedPreferences.LAUNCH_AGENT_CONNECTION_TIMEOUT,
                AgentConnectionServer.DEFAULT_CONNECTION_TIMEOUT);
        preferences.put(RedPreferences.LAUNCH_ENVIRONMENT_VARIABLES, "{\"PYTHONIOENCODING\":\"utf8\"}");
        preferences.put(RedPreferences.DEBUGGER_SUSPEND_ON_ERROR, IssuesStrategy.PROMPT.name().toLowerCase());
        preferences.putBoolean(RedPreferences.DEBUGGER_OMIT_LIB_KEYWORDS, false);
    }

    private void initializeStringVariablesPreferences(final IEclipsePreferences preferences) {
        preferences.put(RedPreferences.STRING_VARIABLES_SETS, "");
        preferences.put(RedPreferences.STRING_VARIABLES_ACTIVE_SET, "");
    }

    private void initializeRfLintPreferences(final IEclipsePreferences preferences) {
        preferences.put(RedPreferences.RFLINT_RULES_FILES, "");
        preferences.put(RedPreferences.RFLINT_RULES_CONFIG_NAMES, "");
        preferences.put(RedPreferences.RFLINT_RULES_CONFIG_SEVERITIES, "");
        preferences.put(RedPreferences.RFLINT_RULES_CONFIG_ARGS, "");
    }

    private void initializeValidationPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.TURN_OFF_VALIDATION, false);
    }

    private void initializeTasksPreferences(final IEclipsePreferences preferences) {
        preferences.putBoolean(RedPreferences.TASKS_DETECTION_ENABLED, true);
        preferences.put(RedPreferences.TASKS_TAGS, "FIXME;TODO");
        preferences.put(RedPreferences.TASKS_PRIORITIES, Priority.HIGH.name() + ";" + Priority.NORMAL.name());
    }
}
