/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.assist;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.robotframework.ide.eclipse.main.plugin.assist.AssistProposals.sortedByLabelsPrefixedFirst;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;

public class RedSettingProposals {

    private static final Table<SettingTarget, String, String> DESCRIBED_SETTINGS;
    static {
        final Builder<SettingTarget, String, String> builder = ImmutableTable.builder();

        builder.put(SettingTarget.TEST_CASE, "[tags]",
                "These tags are set to this test case and they possibly override Default Tags");
        builder.put(SettingTarget.TEST_CASE, "[documentation]",
                "Documentation of current test case");
        builder.put(SettingTarget.TEST_CASE, "[setup]",
                "The keyword %s is executed before other keywords inside the definition");
        builder.put(SettingTarget.TEST_CASE, "[template]",
                "The keyword %s is used as a template");
        builder.put(SettingTarget.TEST_CASE, "[timeout]",
                "Specifies maximum time this test case is allowed to execute before being aborted.\n"
                + "This setting overrides Test Timeout setting set on suite level\n"
                        + "Numerical values are interpreted as seconds but special syntax like '1min 15s' or '2 hours' can be used.");
        builder.put(SettingTarget.TEST_CASE, "[teardown]",
                "The keyword %s is executed after every other keyword inside the definition");

        builder.put(SettingTarget.TASK, "[tags]",
                "These tags are set to this task and they possibly override Default Tags");
        builder.put(SettingTarget.TASK, "[documentation]", "Documentation of current task");
        builder.put(SettingTarget.TASK, "[setup]",
                "The keyword %s is executed before other keywords inside the definition");
        builder.put(SettingTarget.TASK, "[template]", "The keyword %s is used as a template");
        builder.put(SettingTarget.TASK, "[timeout]",
                "Specifies maximum time this task is allowed to execute before being aborted.\n"
                        + "This setting overrides Task Timeout setting set on suite level\n"
                        + "Numerical values are interpreted as seconds but special syntax like '1min 15s' or '2 hours' can be used.");
        builder.put(SettingTarget.TASK, "[teardown]",
                "The keyword %s is executed after every other keyword inside the definition");

        builder.put(SettingTarget.KEYWORD, "[tags]",
                "These tags are set to this keyword and are not affected by Default Tags or Force Tags setting");
        builder.put(SettingTarget.KEYWORD, "[documentation]",
                "Documentation of current keyword");
        builder.put(SettingTarget.KEYWORD, "[teardown]",
                "The keyword %s is executed after every other keyword inside the definition");
        builder.put(SettingTarget.KEYWORD, "[arguments]",
                "Specifies arguments of current keyword");
        builder.put(SettingTarget.KEYWORD, "[timeout]",
                "Specifies maximum time this keyword is allowed to execute before being aborted.\n"
                + "This setting overrides Test Timeout setting set on suite level\n"
                        + "Numerical values are interpreted as seconds but special syntax like '1min 15s' or '2 hours' can be used.");
        builder.put(SettingTarget.KEYWORD, "[return]",
                "Specify the return value for this keyword. Multiple values can be used.");

        builder.put(SettingTarget.GENERAL_TESTS, "library", "Imports library given by its name or path");
        builder.put(SettingTarget.GENERAL_TESTS, "resource", "Imports resource file to be used in current suite");
        builder.put(SettingTarget.GENERAL_TESTS, "variables", "Imports variables file to be used in current suite");
        builder.put(SettingTarget.GENERAL_TESTS, "documentation", "Documentation of current suite");
        builder.put(SettingTarget.GENERAL_TESTS, "metadata", "Metadata current suite hold");
        builder.put(SettingTarget.GENERAL_TESTS, "suite setup",
                "The keyword %s is executed before executing any of the test cases or lower level suites");
        builder.put(SettingTarget.GENERAL_TESTS, "suite teardown",
                "The keyword %s is executed after all test cases and lower level suites have been executed");
        builder.put(SettingTarget.GENERAL_TESTS, "force tags",
                "Sets tags to all test cases in this suite. Inherited tags are not shown here.");
        builder.put(SettingTarget.GENERAL_TESTS, "default tags",
                "Sets tags to all tests cases in this suite, unless test case specifies own tags");
        builder.put(SettingTarget.GENERAL_TESTS, "test setup",
                "The keyword %s is executed before every test cases in this suite unless test cases override it");
        builder.put(SettingTarget.GENERAL_TESTS, "test teardown",
                "The keyword %s is executed after every test cases in this suite unless test cases override it");
        builder.put(SettingTarget.GENERAL_TESTS, "test template",
                "The keyword %s is used as default template keyword in this suite");
        builder.put(SettingTarget.GENERAL_TESTS, "test timeout",
                "Specifies default timeout for each test case in this suite, which can be overridden by test case settings.\n"
                        + "Numerical values are interpreted as seconds but special syntax like '1min 15s' or '2 hours' can be used.");

        builder.put(SettingTarget.GENERAL_TASKS, "library", "Imports library given by its name or path");
        builder.put(SettingTarget.GENERAL_TASKS, "resource", "Imports resource file to be used in current suite");
        builder.put(SettingTarget.GENERAL_TASKS, "variables", "Imports variables file to be used in current suite");
        builder.put(SettingTarget.GENERAL_TASKS, "documentation", "Documentation of current suite");
        builder.put(SettingTarget.GENERAL_TASKS, "metadata", "Metadata current suite hold");
        builder.put(SettingTarget.GENERAL_TASKS, "suite setup",
                "The keyword %s is executed before executing any of the tasks or lower level suites");
        builder.put(SettingTarget.GENERAL_TASKS, "suite teardown",
                "The keyword %s is executed after all tasks and lower level suites have been executed");
        builder.put(SettingTarget.GENERAL_TASKS, "force tags",
                "Sets tags to all tasks in this suite. Inherited tags are not shown here.");
        builder.put(SettingTarget.GENERAL_TASKS, "default tags",
                "Sets tags to all tasks in this suite, unless task specifies own tags");
        builder.put(SettingTarget.GENERAL_TASKS, "task setup",
                "The keyword %s is executed before every task in this suite unless task override it");
        builder.put(SettingTarget.GENERAL_TASKS, "task teardown",
                "The keyword %s is executed after every task in this suite unless task override it");
        builder.put(SettingTarget.GENERAL_TASKS, "task template",
                "The keyword %s is used as default template keyword in this suite");
        builder.put(SettingTarget.GENERAL_TASKS, "task timeout",
                "Specifies default timeout for each task in this suite, which can be overridden by task settings.\n"
                        + "Numerical values are interpreted as seconds but special syntax like '1min 15s' or '2 hours' can be used.");
        DESCRIBED_SETTINGS = builder.build();
    }

    public static boolean isSetting(final SettingTarget target, final String label) {
        return label != null && DESCRIBED_SETTINGS.contains(target, label.toLowerCase());
    }

    public static String getSettingDescription(final SettingTarget target, final String settingName,
            final String additionalArgument) {
        final String arg = additionalArgument.isEmpty() ? "given in first argument" : additionalArgument;
        return String.format(DESCRIBED_SETTINGS.get(target, settingName.toLowerCase()), arg);
    }

    public static List<String> getAllSettingNames(final SettingTarget target) {
        return DESCRIBED_SETTINGS.row(target).keySet().stream().map(target::toCanonicalName).sorted().collect(toList());
    }

    private final SettingTarget target;
    private final ProposalMatcher matcher;

    public RedSettingProposals(final SettingTarget target) {
        this(target, ProposalMatchers.substringMatcher());
    }

    @VisibleForTesting
    RedSettingProposals(final SettingTarget target, final ProposalMatcher matcher) {
        this.target = target;
        this.matcher = matcher;
    }

    public List<? extends AssistProposal> getSettingsProposals(final String userContent) {
        return getSettingsProposals(userContent, sortedByLabelsPrefixedFirst(userContent));
    }

    public List<? extends AssistProposal> getSettingsProposals(final String userContent,
            final Comparator<? super RedSettingProposal> comparator) {

        final List<RedSettingProposal> proposals = new ArrayList<>();

        for (final String settingName : DESCRIBED_SETTINGS.row(target).keySet()) {
            final Optional<ProposalMatch> match = matcher.matches(userContent, settingName);

            if (match.isPresent()) {
                final String name = target.toCanonicalName(settingName);
                proposals.add(AssistProposals.createSettingProposal(name, target, match.get()));
            }
        }
        proposals.sort(comparator);
        return proposals;
    }

    public enum SettingTarget {

        TEST_CASE {

            @Override
            String toCanonicalName(final String name) {
                return toLocalSettingName(name);
            }
        },
        TASK {

            @Override
            String toCanonicalName(final String name) {
                return toLocalSettingName(name);
            }
        },
        KEYWORD {

            @Override
            String toCanonicalName(final String name) {
                return toLocalSettingName(name);
            }
        },
        GENERAL_TESTS {

            @Override
            String toCanonicalName(final String name) {
                return toGeneralSettingName(name);
            }
        },
        GENERAL_TASKS {

            @Override
            String toCanonicalName(final String name) {
                return toGeneralSettingName(name);
            }
        };

        abstract String toCanonicalName(String name);

        private static String toLocalSettingName(final String name) {
            final char firstLetter = name.charAt(1);
            return name.replaceAll("\\[" + firstLetter, "[" + Character.toUpperCase(firstLetter));
        }

        private static String toGeneralSettingName(final String name) {
            return Splitter.on(' ')
                    .splitToList(name)
                    .stream()
                    .map(elem -> CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, elem))
                    .collect(joining(" "));
        }
    }
}
