/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.assist;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.robotframework.ide.eclipse.main.plugin.assist.Commons.firstProposalContaining;
import static org.robotframework.ide.eclipse.main.plugin.assist.Commons.prefixesMatcher;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;

import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.editor.libraries.Libraries;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;

@ExtendWith(ProjectExtension.class)
public class RedImportProposalsTest {

    @Project
    static IProject project;

    private RobotModel robotModel;

    @BeforeEach
    public void beforeTest() {
        robotModel = new RobotModel();

        final RobotProject robotProject = robotModel.createRobotProject(project);
        robotProject.setStandardLibraries(
                Libraries.createStdLibs("StdLib1", "StdLib2", "StdLib3", "1StdLib", "2StdLib", "3StdLib"));
    }

    @Test
    public void noImportProposalsAreProvided_whenNothingIsImported_1() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        assertThat(proposalsProvider.getImportsProposals("")).isEmpty();
    }

    @Test
    public void noImportProposalsAreProvided_whenNothingIsImported_2() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        assertThat(proposalsProvider.getImportsProposals("")).isEmpty();
    }

    @Test
    public void noImportProposalsAreProvided_whenNothingIsMatchingGivenInput() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Resource  res1.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        assertThat(proposalsProvider.getImportsProposals("unknown")).isEmpty();
    }

    @Test
    public void onlyImportProposalsContainingInputAreProvided_whenDefaultMatcherIsUsed()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Library  RefLib1",
                "Resource  res1.robot",
                "Resource  other.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals("1");
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("StdLib1", "res1");
    }

    @Test
    public void onlyImportProposalsContainingInputAreProvidedWithCorrectOrder_whenDefaultMatcherIsUsed()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Library  1StdLib",
                "Library  RefLib1",
                "Resource  res1.robot",
                "Resource  other.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);

        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals("1");
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("1StdLib", "StdLib1", "res1");
    }

    @Test
    public void onlyImportProposalsMatchingInputAreProvided_whenCustomMatcherIsUsed() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Library  RefLib1",
                "Resource  res1.robot",
                "Resource  other.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);
        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile, prefixesMatcher());

        final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals("s");
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("StdLib1");
    }

    @Test
    public void allImportProposalsAreProvided_whenInputIsEmpty() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Library  RefLib1",
                "Resource  res1.robot",
                "Resource  dir${/}res2.robot",
                "Resource  other.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);
        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals("");
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("StdLib1", "other", "res1", "res2");
    }

    @Test
    public void allImportProposalsAreProvided_whenPrefixIsBddSyntaxEmptyAndDefaultMatcherIsUsed() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Library  RefLib1",
                "Resource  res1.robot",
                "Resource  other.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);
        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        for (final String bddPrefix : newArrayList("Given", "When", "And", "But", "Then")) {
            final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals(bddPrefix + " ");
            assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("StdLib1", "other", "res1");
        }
    }

    @Test
    public void allImportProposalsAreProvidedInOrderInducedByGivenComparator_whenCustomComparatorIsProvided()
            throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1",
                "Library  RefLib1",
                "Resource  res1.robot",
                "Resource  other.robot",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);
        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        final Comparator<? super RedImportProposal> comparator = firstProposalContaining("res1");
        final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals("e", comparator);
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("res1", "other");
    }

    @Test
    public void libraryAliasesAreProvided_whenImportsAreUsingWithNameSyntax() throws Exception {
        final IFile file = createFile(project, "file.robot",
                "*** Settings ***",
                "Library  StdLib1  WITH NAME  lib_y",
                "Library  StdLib2  WITH NAME  lib_x",
                "*** Test Cases ***");
        final RobotSuiteFile suiteFile = robotModel.createSuiteFile(file);
        final RedImportProposals proposalsProvider = new RedImportProposals(suiteFile);

        final List<? extends AssistProposal> proposals = proposalsProvider.getImportsProposals("");
        assertThat(proposals).extracting(AssistProposal::getLabel).containsExactly("lib_x", "lib_y");
    }
}
