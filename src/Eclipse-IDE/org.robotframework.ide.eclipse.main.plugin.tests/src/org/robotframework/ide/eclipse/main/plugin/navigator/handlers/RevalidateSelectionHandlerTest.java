/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.navigator.handlers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.red.junit.ProjectProvider;

public class RevalidateSelectionHandlerTest {

    @ClassRule
    public static ProjectProvider projectProvider = new ProjectProvider(RevalidateSelectionHandlerTest.class);

    private static IFolder topLevelDirectory;

    private static IFolder nestedDirectory;

    private static IFile topLevelFile;

    private static IFile initFile;

    private static IFile tsvFile;

    private static IFile txtFile;

    private static IFile robotFile;

    private static IFile notRobotFile;

    @BeforeClass
    public static void beforeSuite() throws Exception {
        topLevelDirectory = projectProvider.createDir("dir1");
        nestedDirectory = projectProvider.createDir("dir1/dir2");

        topLevelFile = projectProvider.createFile("file1.robot", "*** Keywords ***");
        initFile = projectProvider.createFile("dir1/__init__.robot", "*** Settings ***");
        tsvFile = projectProvider.createFile("dir1/file2.tsv", "*** Keywords ***");
        txtFile = projectProvider.createFile("dir1/file3.txt", "*** Test Cases ***");
        robotFile = projectProvider.createFile("dir1/dir2/file4.robot", "*** Test Cases ***");

        notRobotFile = projectProvider.createFile("lib.py", "def kw():", "  pass");
    }

    @Test
    public void onlySelectedFileShouldBeCollected() throws Exception {
        List<IResource> selectedResources = new ArrayList<>();
        selectedResources.add(topLevelFile);
        selectedResources.add(initFile);

        Set<RobotSuiteFile> files = RevalidateSelectionHandler.RobotSuiteFileCollector.collectFiles(selectedResources);
        assertThat(files).isEqualTo(suiteFiles(topLevelFile, initFile));
    }

    @Test
    public void allFilesFromDirectoryShouldBeCollected() throws Exception {
        List<IResource> selectedResources = new ArrayList<>();
        selectedResources.add(topLevelDirectory);

        Set<RobotSuiteFile> files = RevalidateSelectionHandler.RobotSuiteFileCollector.collectFiles(selectedResources);
        assertThat(files).isEqualTo(suiteFiles(initFile, tsvFile, txtFile, robotFile));
    }

    @Test
    public void allFilesFromProjectShouldBeCollected() throws Exception {
        List<IResource> selectedResources = new ArrayList<>();
        selectedResources.add(projectProvider.getProject());

        Set<RobotSuiteFile> files = RevalidateSelectionHandler.RobotSuiteFileCollector.collectFiles(selectedResources);
        assertThat(files).isEqualTo(suiteFiles(topLevelFile, initFile, tsvFile, txtFile, robotFile));
    }

    @Test
    public void filesShouldBeCollectedOnlyOnce() throws Exception {
        List<IResource> selectedResources = new ArrayList<>();
        selectedResources.add(topLevelDirectory);
        selectedResources.add(tsvFile);
        selectedResources.add(robotFile);

        Set<RobotSuiteFile> files = RevalidateSelectionHandler.RobotSuiteFileCollector.collectFiles(selectedResources);
        assertThat(files).isEqualTo(suiteFiles(initFile, tsvFile, txtFile, robotFile));
    }

    @Test
    public void filesAndDirectoriesCanBeMixed() throws Exception {
        List<IResource> selectedResources = new ArrayList<>();
        selectedResources.add(nestedDirectory);
        selectedResources.add(topLevelFile);

        Set<RobotSuiteFile> files = RevalidateSelectionHandler.RobotSuiteFileCollector.collectFiles(selectedResources);
        assertThat(files).isEqualTo(suiteFiles(topLevelFile, robotFile));
    }

    @Test
    public void notRobotFilesShouldBeIgnored() throws Exception {
        List<IResource> selectedResources = new ArrayList<>();
        selectedResources.add(notRobotFile);
        selectedResources.add(topLevelFile);

        Set<RobotSuiteFile> files = RevalidateSelectionHandler.RobotSuiteFileCollector.collectFiles(selectedResources);
        assertThat(files).isEqualTo(suiteFiles(topLevelFile));
    }

    private Set<RobotSuiteFile> suiteFiles(IFile... files) {
        Set<RobotSuiteFile> robotSuiteFiles = new HashSet<>();
        for (IFile file : files) {
            robotSuiteFiles.add(RedPlugin.getModelManager().createSuiteFile(file));
        }
        return robotSuiteFiles;
    }
}
