/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch.local;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.rf.ide.core.RedTemporaryDirectory;
import org.rf.ide.core.SystemVariableAccessor;
import org.rf.ide.core.environment.SuiteExecutor;
import org.rf.ide.core.execution.RunCommandLineCallBuilder.RunCommandLine;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedVariableFile;
import org.rf.ide.core.project.RobotProjectConfig.RelativeTo;
import org.rf.ide.core.project.RobotProjectConfig.RelativityPoint;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.red.junit.jupiter.LaunchConfig;
import org.robotframework.red.junit.jupiter.LaunchConfigExtension;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;
import org.robotframework.red.junit.jupiter.RedTempDirectory;
import org.robotframework.red.junit.jupiter.StatefulProject;
import org.robotframework.red.junit.jupiter.StatefulProject.CleanMode;

import com.google.common.collect.ImmutableMap;

@ExtendWith({ RedTempDirectory.class, ProjectExtension.class, LaunchConfigExtension.class })
public class LocalProcessCommandLineBuilderTest {

    private static final IStringVariableManager VARIABLE_MANAGER = VariablesPlugin.getDefault()
            .getStringVariableManager();

    private static final IValueVariable[] CUSTOM_VARIABLES = new IValueVariable[] {
            VARIABLE_MANAGER.newValueVariable("a_var", "a_desc", true, "a_value"),
            VARIABLE_MANAGER.newValueVariable("b_var", "b_desc", true, "b_value"),
            VARIABLE_MANAGER.newValueVariable("c_var", "c_desc", true, "c_value") };

    @Project(dirs = { "001__suites_a", "002__suites_b", "002__suites_b/nested", "dir.with.dots" },
            files = { "executable_script.bat" },
            cleanUpAfterEach = true)
    public static StatefulProject project;

    @Project(nameSuffix = "Moved", dirs = { "suites" })
    public StatefulProject movedProject;

    @TempDir
    public File tempFolder;

    @LaunchConfig(typeId = RobotLaunchConfiguration.TYPE_ID, name = "robot")
    public ILaunchConfiguration launchCfg;

    private RedPreferences preferences;

    private SystemVariableAccessor variableAccessor;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        VARIABLE_MANAGER.addVariables(CUSTOM_VARIABLES);

        project.createFile(CleanMode.NONTEMPORAL, "001__suites_a/s1.robot", "*** Test Cases ***", "001__case1",
                "  Log  10", "001__case2", "  Log  20");
        project.createFile(CleanMode.NONTEMPORAL, "001__suites_a/s2.robot", "*** Test Cases ***", "001__case3",
                "  Log  10", "001__case4", "  Log  20");

        project.createFile(CleanMode.NONTEMPORAL, "002__suites_b/s3.robot", "*** Test Cases ***", "002__case5",
                "  Log  10", "002__case6", "  Log  20");
        project.createFile(CleanMode.NONTEMPORAL, "002__suites_b/nested/s4.robot", "*** Test Cases ***", "002__case7",
                "  Log  10", "002__case8", "  Log  20");

        project.createFile(CleanMode.NONTEMPORAL, "dir.with.dots/s.5.robot", "*** Test Cases ***", "case9", "  Log  10",
                "case10", "  Log  20");
    }

    @BeforeEach
    public void beforeTest() throws Exception {
        project.configure();

        preferences = mock(RedPreferences.class);
        variableAccessor = mock(SystemVariableAccessor.class);
    }

    @AfterAll
    public static void afterSuite() throws Exception {
        VARIABLE_MANAGER.removeVariables(CUSTOM_VARIABLES);
    }

    @Test
    public void commandLineIsCreated_whenProjectDoesNotContainConfigurationFile() throws Exception {
        project.deconfigure();

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(6)
                .startsWith("/path/to/python", "-m", "robot.run")
                .doesNotContain("-J-cp", "-P", "-V")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineStartsWithInterpreterPathAndInterpreterArguments() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setInterpreterArguments("-a1 -a2");

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .startsWith("/path/to/python", "-a1", "-a2", "-m", "robot.run");
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsPathToListenerAndArgumentFile_whenPreferenceIsSet() throws Exception {
        when(preferences.shouldLaunchUsingArgumentsFile()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("--listener", RedTemporaryDirectory.getTemporaryFile("TestRunnerAgent.py") + ":12345",
                        "--argumentfile")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getCommandLine()[6])
                .startsWith(RedTemporaryDirectory.createTemporaryDirectoryIfNotExists().toString());
        assertThat(commandLine.getArgumentFile())
                .hasValueSatisfying(argumentFile -> assertThat(argumentFile.generateContent())
                        .isEqualTo("# arguments automatically generated"));
    }

    @Test
    public void commandLineContainsSuitesToRun() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", emptyList(), "002__suites_b", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(10)
                .containsSequence("-s", project.getName() + ".Suites A.S1")
                .containsSequence("-s", project.getName() + ".Suites B")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsOnlySelectedSuitesToRun() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", emptyList(), "001__suites_a/s2.robot",
                emptyList(), "002__suites_b", emptyList()));
        robotConfig.setUnselectedSuitePaths(newHashSet("001__suites_a/s1.robot", "002__suites_b"));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-s", project.getName() + ".Suites A.S2")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsSuitesToRun_whenThereAreDotsInSuiteNames1() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("dir.with.dots", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-s", project.getName() + ".Dir.with.dots")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsSuitesToRun_whenThereAreDotsInSuiteNames2() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("dir.with.dots/s.5.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-s", project.getName() + ".Dir.with.dots.S.5")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsTestsToRun() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", asList("001__case1", "001__case2"),
                "001__suites_a/s2.robot", asList("001__case3")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(16)
                .containsSequence("-s", project.getName() + ".Suites A.S1")
                .containsSequence("-s", project.getName() + ".Suites A.S2")
                .containsSequence("-t", project.getName() + ".Suites A.S1.001__case1")
                .containsSequence("-t", project.getName() + ".Suites A.S1.001__case2")
                .containsSequence("-t", project.getName() + ".Suites A.S2.001__case3")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsTestsToRun_whenThereAreDotsInSuiteNames() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("dir.with.dots/s.5.robot", newArrayList("test 9")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(10)
                .containsSequence("-s", project.getName() + ".Dir.with.dots.S.5")
                .containsSequence("-t", project.getName() + ".Dir.with.dots.S.5.test 9")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineDoesNotContainNestedSuitesToRun() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "001__suites_a/s1.robot", emptyList(),
                "002__suites_b/nested/s4.robot", emptyList(), "002__suites_b/nested", emptyList(), "002__suites_b",
                emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(10)
                .containsSequence("-s", project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + ".Suites B")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void coreExceptionIsThrown_whenResourceDoesNotExist() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("not_existig_suite", asList("not_existig_case")));

        assertThatExceptionOfType(CoreException.class)
                .isThrownBy(() -> createCommandLine(interpreter, robotProject, robotConfig))
                .withMessage("Suite '%s' does not exist in project '%s'", "not_existig_suite", project.getName())
                .withNoCause();
    }

    @Test
    public void commandLineTranslatesSuitesNames_whenNamesContainsDoubleUnderscores() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8).containsSequence("-s", project.getName() + ".Suites A");
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineDoesNotTranslateTestNames_whenNamesContainsDoubleUnderscores() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", asList("001__case1", "001__case2")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(12)
                .containsSequence("-s", project.getName() + ".Suites A.S1")
                .containsSequence("-t", project.getName() + ".Suites A.S1.001__case1")
                .containsSequence("-t", project.getName() + ".Suites A.S1.001__case2");
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsAdditionalDataSource_whenWholeProjectIsSelected() throws Exception {
        final File nonWorkspaceFile = RedTempDirectory.createNewFile(tempFolder, "non_workspace.robot",
                "*** Test Cases ***");
        project.createFileLink("LinkedFile.robot", nonWorkspaceFile.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(7)
                .doesNotContain("-s", "-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceFile.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsAdditionalDataSource_whenLinkedSuiteFileIsSelected() throws Exception {
        final File nonWorkspaceTest = RedTempDirectory.createNewFile(tempFolder, "non_workspace_test.robot",
                "*** Test Cases ***");
        project.createFileLink("LinkedTestFile.robot", nonWorkspaceTest.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "LinkedTestFile.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11)
                .containsSequence("-s", project.getName() + " & Non Workspace Test." + project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + " & Non Workspace Test.Non Workspace Test")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceTest.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsAdditionalDataSource_whenLinkedRpaSuiteFileIsSelected() throws Exception {
        final File nonWorkspaceTask = RedTempDirectory.createNewFile(tempFolder, "non_workspace_task.robot",
                "*** Tasks ***");
        project.createFileLink("LinkedTaskFile.robot", nonWorkspaceTask.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "LinkedTaskFile.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11)
                .containsSequence("-s", project.getName() + " & Non Workspace Task." + project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + " & Non Workspace Task.Non Workspace Task")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceTask.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsAdditionalDataSource_whenLinkedFolderIsSelected() throws Exception {
        final File nonWorkspaceDir = RedTempDirectory.createNewDir(tempFolder, "non_workspace_dir");
        project.createDirLink("LinkedFolder", nonWorkspaceDir.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "LinkedFolder", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11)
                .containsSequence("-s", project.getName() + " & Non Workspace Dir." + project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + " & Non Workspace Dir.Non Workspace Dir")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceDir.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineDoesNotContainAdditionalDataSource_whenLinkedResourceFileIsSelected() throws Exception {
        final File nonWorkspaceResource = RedTempDirectory.createNewFile(tempFolder, "non_workspace_resource.robot",
                "*** Settings ***");
        project.createFileLink("LinkedResourceFile.robot", nonWorkspaceResource.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig
                .setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "LinkedResourceFile.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-s", project.getName() + ".Suites A")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineDoesNotContainAdditionalDataSource_whenVirtualFolderWithoutLinkedResourcesIsSelected()
            throws Exception {
        project.createVirtualDir("VirtualDir");
        project.createVirtualDir("VirtualDir/NestedDir");

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "VirtualDir", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-s", project.getName() + ".Suites A")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsAdditionalDataSource_whenVirtualFolderWithLinkedFileIsSelected() throws Exception {
        project.createVirtualDir("VirtualDir");
        project.createVirtualDir("VirtualDir/NestedDir");

        final File nonWorkspaceFile = RedTempDirectory.createNewFile(tempFolder, "non_workspace_file.robot",
                "*** Test Cases ***");
        project.createFileLink("VirtualDir/LinkedFileInsideVirtualDir.robot", nonWorkspaceFile.toURI());
        project.createFileLink("VirtualDir/NestedDir/LinkedFileInsideVirtualDir.robot", nonWorkspaceFile.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "VirtualDir", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11)
                .containsSequence("-s", project.getName() + " & Non Workspace File." + project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + " & Non Workspace File.Non Workspace File")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceFile.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsSeveralAdditionalDataSource_whenSeveralLinkedItemsAreFoundInSelection()
            throws Exception {
        project.createVirtualDir("100__VirtualDir");
        project.createVirtualDir("100__VirtualDir/NestedDir");

        final File nonWorkspaceDir = RedTempDirectory.createNewDir(tempFolder, "dir");
        project.createDirLink("100__VirtualDir/LinkedFolder", nonWorkspaceDir.toURI());

        final File nonWorkspaceFile1 = RedTempDirectory.createNewFile(tempFolder, "file 1.robot", "*** Test Cases ***");
        final File nonWorkspaceFile2 = RedTempDirectory.createNewFile(tempFolder, "file 2.robot", "*** Test Cases ***");
        project.createFileLink("100__VirtualDir/NestedDir/LinkedInsideVirtualDir.robot", nonWorkspaceFile1.toURI());
        project.createFileLink("200__Linked.robot", nonWorkspaceFile2.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList(), "100__VirtualDir", emptyList(),
                "200__Linked.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(17)
                .containsSequence("-s",
                        project.getName() + " & Dir & File 1 & File 2." + project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + " & Dir & File 1 & File 2.Dir")
                .containsSequence("-s", project.getName() + " & Dir & File 1 & File 2.File 1")
                .containsSequence("-s", project.getName() + " & Dir & File 1 & File 2.File 2")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceDir.getPath(),
                        nonWorkspaceFile1.getPath(), nonWorkspaceFile2.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
        assertThat(robotConfig.getLinkedResourcesPaths()).containsExactly(
                "/" + project.getName() + "/100__VirtualDir/LinkedFolder",
                "/" + project.getName() + "/100__VirtualDir/NestedDir/LinkedInsideVirtualDir.robot",
                "/" + project.getName() + "/200__Linked.robot");
    }

    @Test
    public void commandLineContainsSeveralAdditionalDataSource_whenSeveralLinkedItemsAreFoundInTestCaseSelection()
            throws Exception {
        project.createVirtualDir("100__VirtualDir");
        project.createVirtualDir("100__VirtualDir/NestedDir");

        final File nonWorkspaceFile1 = RedTempDirectory.createNewFile(tempFolder, "file 1.robot", "*** Test Cases ***");
        final File nonWorkspaceFile2 = RedTempDirectory.createNewFile(tempFolder, "file 2.robot", "*** Test Cases ***");
        project.createFileLink("100__VirtualDir/NestedDir/LinkedInsideVirtualDir.robot", nonWorkspaceFile1.toURI());
        project.createFileLink("200__Linked.robot", nonWorkspaceFile2.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", asList("001__case1", "001__case2"),
                "100__VirtualDir/NestedDir/LinkedInsideVirtualDir.robot", asList("virt_case_1", "virt_case_2"),
                "200__Linked.robot", asList("link_case_1", "link_case_2")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(26)
                .containsSequence("-s", project.getName() + " & File 1 & File 2." + project.getName() + ".Suites A.S1")
                .containsSequence("-s", project.getName() + " & File 1 & File 2.File 1")
                .containsSequence("-s", project.getName() + " & File 1 & File 2.File 2")
                .containsSequence("-t",
                        project.getName() + " & File 1 & File 2." + project.getName() + ".Suites A.S1.001__case1")
                .containsSequence("-t",
                        project.getName() + " & File 1 & File 2." + project.getName() + ".Suites A.S1.001__case2")
                .containsSequence("-t", project.getName() + " & File 1 & File 2.File 1.virt_case_1")
                .containsSequence("-t", project.getName() + " & File 1 & File 2.File 1.virt_case_2")
                .containsSequence("-t", project.getName() + " & File 1 & File 2.File 2.link_case_1")
                .containsSequence("-t", project.getName() + " & File 1 & File 2.File 2.link_case_1")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceFile1.getPath(),
                        nonWorkspaceFile2.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
        assertThat(robotConfig.getLinkedResourcesPaths()).containsExactly(
                "/" + project.getName() + "/100__VirtualDir/NestedDir/LinkedInsideVirtualDir.robot",
                "/" + project.getName() + "/200__Linked.robot");
    }

    @Test
    public void commandLineContainsPythonPathsDefinedInRedXml_whenProjectRelativityPointIsUsed() throws Exception {
        final SearchPath searchPath1 = SearchPath.create("folder1");
        final SearchPath searchPath2 = SearchPath.create("folder2");
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addPythonPath(searchPath1);
        config.addPythonPath(searchPath2);
        config.setRelativityPoint(RelativityPoint.create(RelativeTo.PROJECT));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-P", String.format("%1$s%2$sfolder1:%1$s%2$sfolder2",
                        project.getProject().getLocation().toOSString(), File.separator));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsPythonPathsDefinedInRedXml_whenWorkspaceRelativityPointIsUsed() throws Exception {
        final SearchPath searchPath1 = SearchPath.create(project.getName() + "/folder1");
        final SearchPath searchPath2 = SearchPath.create(project.getName() + "/folder2");
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addPythonPath(searchPath1);
        config.addPythonPath(searchPath2);
        config.setRelativityPoint(RelativityPoint.create(RelativeTo.WORKSPACE));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-P", String.format("%1$s%2$sfolder1:%1$s%2$sfolder2",
                        project.getProject().getLocation().toOSString(), File.separator));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsPythonPathsForPythonLibrariesAddedToRedXml() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addReferencedLibrary(
                ReferencedLibrary.create(LibraryType.PYTHON, "PyLib1", project.getName() + "/folder1/PyLib1.py"));
        config.addReferencedLibrary(
                ReferencedLibrary.create(LibraryType.PYTHON, "PyLib2",
                        project.getName() + "/folder2/PyLib2/__init__.py"));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-P", String.format("%1$s%2$sfolder1:%1$s%2$sfolder2",
                        project.getProject().getLocation().toOSString(), File.separator));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsClassPathsDefinedInRedXml_whenProjectRelativityPointIsUsed() throws Exception {
        final SearchPath searchPath1 = SearchPath.create("JavaLib1.jar");
        final SearchPath searchPath2 = SearchPath.create("JavaLib2.jar");
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addClassPath(searchPath1);
        config.addClassPath(searchPath2);
        config.setRelativityPoint(RelativityPoint.create(RelativeTo.PROJECT));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Jython);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-J-cp", String.format(".%1$s%2$s%3$sJavaLib1.jar%1$s%2$s%3$sJavaLib2.jar",
                        File.pathSeparator, project.getProject().getLocation().toOSString(), File.separator));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsClassPathsDefinedInRedXml_whenWorkspaceRelativityPointIsUsed() throws Exception {
        final SearchPath searchPath1 = SearchPath.create(project.getName() + "/JavaLib1.jar");
        final SearchPath searchPath2 = SearchPath.create(project.getName() + "/JavaLib2.jar");
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addClassPath(searchPath1);
        config.addClassPath(searchPath2);
        config.setRelativityPoint(RelativityPoint.create(RelativeTo.WORKSPACE));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Jython);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-J-cp", String.format(".%1$s%2$s%3$sJavaLib1.jar%1$s%2$s%3$sJavaLib2.jar",
                        File.pathSeparator, project.getProject().getLocation().toOSString(), File.separator));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsClassPathsForJavaLibrariesAddedToRedXml() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addReferencedLibrary(
                ReferencedLibrary.create(LibraryType.JAVA, "JavaLib1", project.getName() + "/JavaLib1.jar"));
        config.addReferencedLibrary(
                ReferencedLibrary.create(LibraryType.JAVA, "JavaLib2", project.getName() + "/JavaLib2.jar"));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Jython);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-J-cp", String.format(".%1$s%2$s%3$sJavaLib1.jar%1$s%2$s%3$sJavaLib2.jar",
                        File.pathSeparator, project.getProject().getLocation().toOSString(), File.separator));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsClassPathsFromSystemEnvironmentVariable() throws Exception {
        when(variableAccessor.getPaths("CLASSPATH"))
                .thenReturn(newArrayList("FirstClassPath.jar", "SecondClassPath.jar"));

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Jython);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-J-cp", String.join(File.pathSeparator,
                        newArrayList(".", "FirstClassPath.jar", "SecondClassPath.jar")));
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineDoesNotContainPathsForVariableFiles() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addReferencedVariableFile(ReferencedVariableFile.create(project.getName() + "/vars1.py"));
        config.addReferencedVariableFile(ReferencedVariableFile.create(project.getName() + "/vars2.py"));
        project.configure(config);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Jython);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8).doesNotContain("-V");
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsTags() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setIsExcludeTagsEnabled(true);
        robotConfig.setExcludedTags(asList("EX_1", "EX_2"));
        robotConfig.setIsIncludeTagsEnabled(true);
        robotConfig.setIncludedTags(asList("IN_1", "IN_2"));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(14)
                .containsSequence("-i", "IN_1", "-i", "IN_2", "-e", "EX_1", "-e", "EX_2");
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineStartsWitExecutableFilePath() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        final String executablePath = project.getFile("executable_script.bat").getLocation().toOSString();
        robotConfig.setExecutableFilePath(executablePath);

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(7).startsWith(executablePath);
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineStartsWithExecutableFilePath_whenPathContainsVariables() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setExecutableFilePath("${workspace_loc:/" + project.getName() + "/executable_script.bat}");

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(7)
                .startsWith(project.getFile("executable_script.bat").getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsExecutableFilePathWithArguments() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        final String executablePath = project.getFile("executable_script.bat").getLocation().toOSString();
        robotConfig.setExecutableFilePath(executablePath);
        robotConfig.setExecutableFileArguments("-arg1 abc -arg2 xyz");

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11).startsWith(executablePath, "-arg1", "abc", "-arg2", "xyz");
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }


    @Test
    public void commandLineStartsWithExecutableFilePathWithArgumentsAndEndsWithSingleCommandLineArg_whenPreferenceIsSet()
            throws Exception {
        when(preferences.shouldUseSingleCommandLineArgument()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        final String executablePath = project.getFile("executable_script.bat").getLocation().toOSString();
        robotConfig.setExecutableFilePath(executablePath);
        robotConfig.setExecutableFileArguments("-a -b -c");

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(5).startsWith(executablePath, "-a", "-b", "-c");
        assertThat(commandLine.getCommandLine()[4]).startsWith("/path/to/python -m robot.run --listener")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void coreExceptionIsThrown_whenExecutableFileDoesNotExist() throws Exception {
        final String executablePath = project.getFile("not_existing.bat").getLocation().toOSString();

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setExecutableFilePath(executablePath);

        assertThatExceptionOfType(CoreException.class)
                .isThrownBy(() -> createCommandLine(interpreter, robotProject, robotConfig))
                .withMessage("Executable file '%s' does not exist", executablePath)
                .withNoCause();
    }

    @Test
    public void coreExceptionIsThrown_whenExecutableFileDefinedWithVariableDoesNotExist() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setExecutableFilePath("${workspace_loc:/" + project.getName() + "/not_existing.bat}");

        assertThatExceptionOfType(CoreException.class)
                .isThrownBy(() -> createCommandLine(interpreter, robotProject, robotConfig))
                .withMessage("Variable references non-existent resource : ${workspace_loc:/" + project.getName()
                        + "/not_existing.bat}")
                .withNoCause();
    }

    @Test
    public void pathToSingleSuiteIsUsed_whenWholeProjectIsRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(6)
                .doesNotContain("-s", "-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsUsed_whenSingleSuiteIsRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(6)
                .doesNotContain("-s", "-t")
                .endsWith(project.getFile("001__suites_a/s1.robot").getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsUsed_whenSingleLinkedSuiteIsRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final File nonWorkspaceFile = RedTempDirectory.createNewFile(tempFolder, "non_workspace_suite.robot",
                "*** Test Cases ***");
        project.createFileLink("LinkedSuite.robot", nonWorkspaceFile.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("LinkedSuite.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(6)
                .doesNotContain("-s", "-t")
                .endsWith(nonWorkspaceFile.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsUsed_whenSingleFolderIsRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(6)
                .doesNotContain("-s", "-t")
                .endsWith(project.getFile("001__suites_a").getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsUsed_whenTestsFromSingleSuiteAreRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", asList("001__case1")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-t", "S1.001__case1")
                .doesNotContain("-s")
                .endsWith(project.getFile("001__suites_a/s1.robot").getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsUsed_whenTestsFromSingleLinkedSuiteAreRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final File nonWorkspaceFile = RedTempDirectory.createNewFile(tempFolder, "non_workspace_suite.robot",
                "*** Test Cases ***");
        project.createFileLink("LinkedSuite.robot", nonWorkspaceFile.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("LinkedSuite.robot", asList("case1", "case2")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(10)
                .containsSequence("-t", "Non Workspace Suite.case1", "-t", "Non Workspace Suite.case2")
                .doesNotContain("-s")
                .endsWith(nonWorkspaceFile.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsNotUsed_whenSeveralResourcesAreRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(
                ImmutableMap.of("001__suites_a/s1.robot", emptyList(), "001__suites_a/s2.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(10)
                .containsSequence("-s", project.getName() + ".Suites A.S1")
                .containsSequence("-s", project.getName() + ".Suites A.S2")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsNotUsed_whenSingleFolderWithLinkedSuiteIsRunAndPreferenceIsSet() throws Exception {
        when(preferences.shouldUseSingleFileDataSource()).thenReturn(true);

        final File nonWorkspaceFile = RedTempDirectory.createNewFile(tempFolder, "non_workspace_suite.robot",
                "*** Test Cases ***");
        project.createFileLink("001__suites_a/LinkedSuite.robot", nonWorkspaceFile.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11)
                .containsSequence("-s", project.getName() + " & Non Workspace Suite." + project.getName() + ".Suites A")
                .containsSequence("-s", project.getName() + " & Non Workspace Suite.Non Workspace Suite")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString(), nonWorkspaceFile.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void pathToSingleSuiteIsNotUsed_whenSingleSuiteIsRunAndPreferenceIsNotSet() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("001__suites_a/s1.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(8)
                .containsSequence("-s", project.getName() + ".Suites A.S1")
                .doesNotContain("-t")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void knownVariablesAreResolvedInAdditionalArguments() throws Exception {
        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(project.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(project.getProject());
        robotConfig.setRobotArguments("a ${a_var} ${a}");
        robotConfig.setInterpreterArguments("${b} b ${b_var}");
        final String executablePath = project.getFile("executable_script.bat").getLocation().toOSString();
        robotConfig.setExecutableFilePath(executablePath);
        robotConfig.setExecutableFileArguments("${c_var} ${c} c");

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(16)
                .startsWith(executablePath, "c_value", "${c}", "c")
                .containsSequence("${b}", "b", "b_value", "-m", "robot.run")
                .containsSequence("a", "a_value", "${a}")
                .endsWith(project.getProject().getLocation().toOSString());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }



    @Test
    public void commandLineContainsSuitesToRun_whenProjectIsOutsideOfWorkspace() throws Exception {
        movedProject.createFile("suites/s1.robot", "*** Test Cases ***", "c1", " Log 1");
        movedProject.createFile("suites/s2.robot", "*** Test Cases ***", "c2", " Log 1");

        final File nonWorkspaceDir = RedTempDirectory.createNewDir(tempFolder, "Project_outside");

        movedProject.move(nonWorkspaceDir);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(movedProject.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(movedProject.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("suites/s1.robot", emptyList(), "suites/s2.robot", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(10)
                .containsSequence("-s", "Project Outside.Suites.S1")
                .containsSequence("-s", "Project Outside.Suites.S2")
                .doesNotContain("-t")
                .endsWith(nonWorkspaceDir.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsTestsToRun_whenProjectIsOutsideOfWorkspace() throws Exception {
        movedProject.createFile("suites/s1.robot", "*** Test Cases ***", "c11", " Log 1", "c12", " Log 2");
        movedProject.createFile("suites/s2.robot", "*** Test Cases ***", "c21", " Log 1", "c22", " Log 2");

        final File nonWorkspaceDir = RedTempDirectory.createNewDir(tempFolder, "Project_outside_with_tests");

        movedProject.move(nonWorkspaceDir);

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(movedProject.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(movedProject.getProject());
        robotConfig.setSuitePaths(
                ImmutableMap.of("suites/s1.robot", asList("c11", "c12"), "suites/s2.robot", asList("c21", "c22")));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(18)
                .containsSequence("-s", "Project Outside With Tests.Suites.S1")
                .containsSequence("-s", "Project Outside With Tests.Suites.S2")
                .containsSequence("-t", "Project Outside With Tests.Suites.S1.c11")
                .containsSequence("-t", "Project Outside With Tests.Suites.S1.c12")
                .containsSequence("-t", "Project Outside With Tests.Suites.S2.c21")
                .containsSequence("-t", "Project Outside With Tests.Suites.S2.c22")
                .endsWith(nonWorkspaceDir.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    @Test
    public void commandLineContainsAdditionalDataSource_whenProjectIsOutsideOfWorkspace() throws Exception {
        final File nonWorkspaceDir = RedTempDirectory.createNewDir(tempFolder, "Project_outside");
        final File nonWorkspaceTest = RedTempDirectory.createNewFile(tempFolder, "non_workspace_test.robot",
                "*** Test Cases ***");

        movedProject.move(nonWorkspaceDir);
        movedProject.createFile("suites/s1.robot", "*** Test Cases ***", "c1", " Log 1");
        movedProject.createFileLink("suites/LinkedTest.robot", nonWorkspaceTest.toURI());

        final LocalProcessInterpreter interpreter = createInterpreter(SuiteExecutor.Python);
        final RobotProject robotProject = createRobotProject(movedProject.getProject());
        final RobotLaunchConfiguration robotConfig = createRobotLaunchConfiguration(movedProject.getProject());
        robotConfig.setSuitePaths(ImmutableMap.of("suites", emptyList()));

        final RunCommandLine commandLine = createCommandLine(interpreter, robotProject, robotConfig);

        assertThat(commandLine.getCommandLine()).hasSize(11)
                .containsSequence("-s", "Project Outside & Non Workspace Test.Project Outside.Suites")
                .containsSequence("-s", "Project Outside & Non Workspace Test.Non Workspace Test")
                .doesNotContain("-t")
                .endsWith(nonWorkspaceDir.getPath(), nonWorkspaceTest.getPath());
        assertThat(commandLine.getArgumentFile()).isNotPresent();
    }

    private RunCommandLine createCommandLine(final LocalProcessInterpreter interpreter, final RobotProject robotProject,
            final RobotLaunchConfiguration robotConfig) throws CoreException, IOException {
        return new LocalProcessCommandLineBuilder(interpreter, robotConfig, robotProject, variableAccessor)
                .createRunCommandLine(12345, preferences);
    }

    private LocalProcessInterpreter createInterpreter(final SuiteExecutor interpreter) {
        return new LocalProcessInterpreter(interpreter, "/path/to/python", "RF 1.2.3");
    }

    private RobotProject createRobotProject(final IProject project) {
        return new RobotModel().createRobotProject(project);
    }

    private RobotLaunchConfiguration createRobotLaunchConfiguration(final IProject project) throws CoreException {
        final RobotLaunchConfiguration robotConfig = new RobotLaunchConfiguration(launchCfg);
        robotConfig.fillDefaults();
        robotConfig.setProjectName(project.getName());
        return robotConfig;
    }
}
