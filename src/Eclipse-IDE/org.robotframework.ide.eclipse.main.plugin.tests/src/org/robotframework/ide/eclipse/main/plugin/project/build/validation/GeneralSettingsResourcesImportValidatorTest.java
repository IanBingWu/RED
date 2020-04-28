/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Condition;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.environment.SuiteExecutor;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.rf.ide.core.testdata.model.RobotProjectHolder;
import org.rf.ide.core.testdata.model.table.setting.ResourceImport;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.validation.ProblemPosition;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSettingsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.GeneralSettingsProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.MockReporter.Problem;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;
import org.robotframework.red.junit.jupiter.RedTempDirectory;
import org.robotframework.red.junit.jupiter.StatefulProject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Range;

@ExtendWith({ ProjectExtension.class, RedTempDirectory.class })
public class GeneralSettingsResourcesImportValidatorTest {

    @Project(cleanUpAfterEach = true, createDefaultRedXml = true)
    static StatefulProject project;

    @TempDir
    static File tempFolder;

    private RobotModel model;

    private MockReporter reporter;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        final File externalDir = RedTempDirectory.createNewDir(tempFolder, "external_dir");

        RedTempDirectory.createNewFile(tempFolder, "external.robot");

        RedTempDirectory.createNewFile(externalDir, "external_nested.robot");
        RedTempDirectory.createNewFile(externalDir, "external_nested.txt");
        RedTempDirectory.createNewFile(externalDir, "external_nested.tsv");
        RedTempDirectory.createNewFile(externalDir, "external_nested.html");
        RedTempDirectory.createNewFile(externalDir, "external_nested.jpg");
    }

    @BeforeEach
    public void beforeTest() {
        model = new RobotModel();
        reporter = new MockReporter();
    }

    @Test
    public void markerIsReported_whenImportIsNotSpecified() {
        validateResourceImport("");

        assertThat(reporter.getReportedProblems()).containsExactly(new Problem(
                GeneralSettingsProblem.MISSING_RESOURCE_NAME, new ProblemPosition(2, Range.closed(17, 25))));
    }

    @Test
    public void markerIsReported_whenImportingUnknownThing() {
        validateResourceImport("something");

        assertThat(reporter.getReportedProblems()).containsExactly(new Problem(
                GeneralSettingsProblem.NON_EXISTING_RESOURCE_IMPORT, new ProblemPosition(2, Range.closed(27, 36))));
    }

    @Test
    public void markerIsReported_whenImportContainsUnknownVariables() {
        validateResourceImport("${unknown}/file.robot");

        assertThat(reporter.getReportedProblems()).containsExactly(new Problem(
                GeneralSettingsProblem.PARAMETERIZED_LIBRARY_IMPORT, new ProblemPosition(2, Range.closed(27, 48))));
    }

    @Test
    public void markerIsReported_whenUsingUnescapedWindowsPaths_1() {
        validateResourceImport("d:\\folder\\res.robot");

        assertThat(reporter.getReportedProblems())
                .containsOnly(new Problem(GeneralSettingsProblem.IMPORT_PATH_USES_SINGLE_WINDOWS_SEPARATORS,
                        new ProblemPosition(2, Range.closed(27, 46))));
    }

    @Test
    public void markerIsReported_whenUsingUnescapedWindowsPaths_2() {
        validateResourceImport("..\\..\\res.robot");

        assertThat(reporter.getReportedProblems())
                .containsOnly(new Problem(GeneralSettingsProblem.IMPORT_PATH_USES_SINGLE_WINDOWS_SEPARATORS,
                        new ProblemPosition(2, Range.closed(27, 42))));
    }

    @Test
    public void markerIsReported_whenUsingUnescapedWindowsPaths_3() {
        validateResourceImport("..\\../res.robot");

        assertThat(reporter.getReportedProblems())
                .containsOnly(new Problem(GeneralSettingsProblem.IMPORT_PATH_USES_SINGLE_WINDOWS_SEPARATORS,
                        new ProblemPosition(2, Range.closed(27, 42))));
    }

    @Test
    public void markerIsReported_whenUsingAbsolutePathImport() throws Exception {
        final File tmpFile = RedTempDirectory.createNewFile(tempFolder, "file.robot");

        final String absPath = tmpFile.getAbsolutePath().replaceAll("\\\\", "/");
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems()).contains(new Problem(GeneralSettingsProblem.IMPORT_PATH_ABSOLUTE,
                new ProblemPosition(2, Range.closed(27, 27 + absPath.length()))));
    }

    @Test
    public void markerIsReported_whenFileIsImportedRelativelyViaSysPathInsteadOfLocally() throws Exception {
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.robot");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(newArrayList(tmpFile.getParentFile()));

        validateResourceImport("external_nested.robot");
        assertThat(reporter.getReportedProblems())
                .contains(new Problem(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH,
                        new ProblemPosition(2, Range.closed(27, 48))));
    }

    @Test
    public void markerIsReported_whenFileIsImportedRelativelyViaRedXmlPythonPathInsteadOfLocally() throws Exception {
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.robot");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(new ArrayList<>());

        final List<SearchPath> paths = newArrayList(SearchPath.create(tmpFile.getParent()));
        robotProject.getRobotProjectConfig().setPythonPaths(paths);

        validateResourceImport("external_nested.robot");
        assertThat(reporter.getReportedProblems())
                .contains(new Problem(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH,
                        new ProblemPosition(2, Range.closed(27, 48))));

        robotProject.getRobotProjectConfig().setPythonPaths(null);
    }

    @Test
    public void markerIsReported_whenFileIsImportedFromOutsideOfWorkspace_1() {
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.robot");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(newArrayList(tmpFile.getParentFile()));

        validateResourceImport("external_nested.robot");

        assertThat(reporter.getReportedProblems()).contains(new Problem(
                GeneralSettingsProblem.IMPORT_PATH_OUTSIDE_WORKSPACE, new ProblemPosition(2, Range.closed(27, 48))));
    }

    @Test
    public void markerIsReported_whenFileIsImportedFromOutsideOfWorkspace_2() {
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.robot");

        final String absPath = tmpFile.getAbsolutePath().replaceAll("\\\\", "/");
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems())
                .contains(new Problem(GeneralSettingsProblem.IMPORT_PATH_OUTSIDE_WORKSPACE,
                        new ProblemPosition(2, Range.closed(27, 27 + absPath.length()))));
    }

    @Test
    public void markerIsReported_whenExternalFileDoesNotExist() {
        final File tmpFile = getFile(tempFolder, "external_dir", "non_existing.robot");

        final String absPath = tmpFile.getAbsolutePath().replaceAll("\\\\", "/");
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems())
                .contains(new Problem(GeneralSettingsProblem.NON_EXISTING_RESOURCE_IMPORT,
                        new ProblemPosition(2, Range.closed(27, 27 + absPath.length()))));
    }

    @Test
    public void markerIsReported_whenFileInWorkspaceDoesNotExist() {
        validateResourceImport("non_existing.robot");

        assertThat(reporter.getReportedProblems()).contains(new Problem(
                GeneralSettingsProblem.NON_EXISTING_RESOURCE_IMPORT, new ProblemPosition(2, Range.closed(27, 45))));
    }

    @Test
    public void markerIsReported_whenExternalImportIsNotAFile() {
        final File tmpFile = getFile(tempFolder, "external_dir");

        final String absPath = tmpFile.getAbsolutePath().replaceAll("\\\\", "/");
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems()).contains(new Problem(GeneralSettingsProblem.INVALID_RESOURCE_IMPORT,
                new ProblemPosition(2, Range.closed(27, 27 + absPath.length()))));
    }

    @Test
    public void markerIsReported_whenWorkspaceImportIsNotAFile() throws Exception {
        final IFolder dir = project.createDir("directory");

        validateResourceImport("directory");
        assertThat(reporter.getReportedProblems()).contains(new Problem(GeneralSettingsProblem.INVALID_RESOURCE_IMPORT,
                new ProblemPosition(2, Range.closed(27, 36))));

        dir.delete(true, null);
    }

    @Test
    public void markerIsReported_whenExternalImportPointsToHtmlFile() {
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.html");

        final String absPath = tmpFile.getAbsolutePath().replaceAll("\\\\", "/");
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems()).contains(new Problem(GeneralSettingsProblem.HTML_RESOURCE_IMPORT,
                new ProblemPosition(2, Range.closed(27, 27 + absPath.length()))));
    }

    @Test
    public void markerIsReported_whenWorkspaceImportPointsToHtmlFile() throws Exception {
        final IFile file = project.createFile("res.html");

        validateResourceImport("res.html");
        assertThat(reporter.getReportedProblems()).contains(
                new Problem(GeneralSettingsProblem.HTML_RESOURCE_IMPORT, new ProblemPosition(2, Range.closed(27, 35))));

        file.delete(true, null);
    }

    @Test
    public void markerIsReported_whenExternalImportIsNotAResourceFile() {
        // approximation, since we don't check if external files are really resources
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.jpg");

        final String absPath = tmpFile.getAbsolutePath().replaceAll("\\\\", "/");
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems()).contains(new Problem(GeneralSettingsProblem.INVALID_RESOURCE_IMPORT,
                new ProblemPosition(2, Range.closed(27, 27 + absPath.length()))));
    }

    @Test
    public void markerIsReported_whenWorkspaceImportIsNotAResourceFile() throws Exception {
        // it contains test cases section, so it is not a resource file
        final IFile file = project.createFile("res.robot", "*** Test Cases ***");

        validateResourceImport("res.robot");
        assertThat(reporter.getReportedProblems()).contains(new Problem(GeneralSettingsProblem.INVALID_RESOURCE_IMPORT,
                new ProblemPosition(2, Range.closed(27, 36))));

        file.delete(true, null);
    }

    @Test
    public void markerIsReported_whenImportedResourceLiesInDifferentDirectory() throws Exception {
        final IFolder dir = project.createDir("dir");
        project.createFile("dir/res.robot");

        validateResourceImport("res.robot");
        assertThat(reporter.getReportedProblems()).containsExactly(new Problem(
                GeneralSettingsProblem.NON_EXISTING_RESOURCE_IMPORT, new ProblemPosition(2, Range.closed(27, 36))));

        dir.delete(true, null);
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileExistLocally_1() throws Exception {
        final IFile file = project.createFile("res.robot");

        validateResourceImport("res.robot");
        assertThat(reporter.getReportedProblems()).isEmpty();

        file.delete(true, null);
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileExistLocally_2() throws Exception {
        final IFolder dir = project.createDir("directory");
        project.createFile("directory/res.robot");

        validateResourceImport("directory/res.robot");
        assertThat(reporter.getReportedProblems()).isEmpty();

        dir.delete(true, null);
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileExistLocallyButIsImportedUsingAbsolutePath()
            throws Exception {
        final IFile resFile = project.createFile("res.robot");

        final String absPath = resFile.getLocation().toString();
        validateResourceImport(absPath);

        assertThat(reporter.getReportedProblems()).are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_ABSOLUTE));

        resFile.delete(true, null);
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileExistLocallyAsALinkToExternalFile() throws Exception {
        final File tmpFile = getFile(tempFolder, "external_dir", "external_nested.robot");

        project.createFileLink("link.robot", tmpFile.toURI());

        validateResourceImport(tmpFile.getAbsolutePath().replaceAll("\\\\", "/"));
        assertThat(reporter.getReportedProblems()).are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_ABSOLUTE));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedViaSysPathFromWorkspace() throws Exception {
        final IFolder dir = project.createDir("dir");
        project.createFile("dir/res.robot");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(newArrayList(dir.getLocation().toFile()));

        validateResourceImport("res.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH));

        dir.delete(true, null);
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedViaSysPathFromExternalLocation() {
        final File dir = getFile(tempFolder, "external_dir");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(newArrayList(dir));

        validateResourceImport("external_nested.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH,
                        GeneralSettingsProblem.IMPORT_PATH_OUTSIDE_WORKSPACE,
                        GeneralSettingsProblem.NON_WORKSPACE_LINKABLE_RESOURCE_IMPORT));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedViaSysPathFromExternalLocationWhichIsLinkedInWorkspace()
            throws Exception {
        final File dir = getFile(tempFolder, "external_dir");

        project.createDirLink("linking_dir", dir.toURI());

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(newArrayList(dir));

        validateResourceImport("external_nested.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedViaRedXmlPythonPathFromWorkspace()
            throws Exception {
        final IFolder dir = project.createDir("dir");
        project.createFile("dir/res.robot");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(new ArrayList<>());

        final List<SearchPath> paths = newArrayList(SearchPath.create(dir.getLocation().toString()));
        robotProject.getRobotProjectConfig().setPythonPaths(paths);

        validateResourceImport("res.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH));

        dir.delete(true, null);
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedViaRedXmlPythonPathFromExternalLocation() {
        final File dir = getFile(tempFolder, "external_dir");

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(new ArrayList<File>());

        final List<SearchPath> paths = newArrayList(SearchPath.create(dir.getAbsolutePath()));
        robotProject.getRobotProjectConfig().setPythonPaths(paths);

        validateResourceImport("external_nested.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH,
                        GeneralSettingsProblem.IMPORT_PATH_OUTSIDE_WORKSPACE,
                        GeneralSettingsProblem.NON_WORKSPACE_LINKABLE_RESOURCE_IMPORT));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedFromExternalUnlinkableLocationWithEmptyParent() {
        final File resource = mock(File.class);
        when(resource.isFile()).thenReturn(true);
        when(resource.getAbsolutePath()).thenReturn("absolute/path/to/resource.robot");
        when(resource.getParent()).thenReturn("");

        validateResourceFile(resource, "absolute/path/to/resource.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.NON_WORKSPACE_UNLINKABLE_RESOURCE_IMPORT));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedFromExternalUnlinkableLocationWithEmptyAbsolutePath() {
        final File resource = mock(File.class);
        when(resource.isFile()).thenReturn(true);
        when(resource.getAbsolutePath()).thenReturn("absolute/path/to/parent/resource.robot");
        when(resource.getParent()).thenReturn("parent");

        validateResourceFile(resource, "resource_with_empty_absolute_path.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.NON_WORKSPACE_UNLINKABLE_RESOURCE_IMPORT));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedFromExternalUnlinkableLocationWithIncorrectAbsolutePathWithDots() {
        final File resource = mock(File.class);
        when(resource.isFile()).thenReturn(true);
        when(resource.getAbsolutePath()).thenReturn("absolute/path/to/parent/resource.robot");
        when(resource.getParent()).thenReturn("parent");

        validateResourceFile(resource, "absolute/path/with/../to/parent/resource.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.NON_WORKSPACE_UNLINKABLE_RESOURCE_IMPORT));
    }

    @Test
    public void noMajorProblemsAreReported_whenResourceFileIsImportedViaRedXmlPythonPathFromExternalLocationWhichIsLinkedInWorkspace()
            throws Exception {
        final File dir = getFile(tempFolder, "external_dir");

        project.createDirLink("linking_dir", dir.toURI());

        final RobotProject robotProject = model.createRobotProject(project.getProject());
        final RobotProjectHolder projectHolder = robotProject.getRobotProjectHolder();
        projectHolder.setModuleSearchPaths(new ArrayList<>());

        final List<SearchPath> paths = newArrayList(SearchPath.create(dir.getAbsolutePath()));
        robotProject.getRobotProjectConfig().setPythonPaths(paths);

        validateResourceImport("external_nested.robot");
        assertThat(reporter.getReportedProblems())
                .are(onlyCausedBy(GeneralSettingsProblem.IMPORT_PATH_RELATIVE_VIA_MODULES_PATH));
    }

    private Condition<Problem> onlyCausedBy(final GeneralSettingsProblem... causes) {
        final Set<GeneralSettingsProblem> causesSet = newHashSet(causes);
        return new Condition<MockReporter.Problem>() {
            @Override
            public boolean matches(final Problem problem) {
                return causesSet.contains(problem.getCause());
            }
        };
    }

    private void validateResourceImport(final String toImport) {
        final RobotSuiteFile suiteFile = createResourceImportingSuite(toImport);
        final ResourceImport resImport = getImport(suiteFile);

        final FileValidationContext context = prepareContext(suiteFile);

        final GeneralSettingsResourcesImportValidator validator = new GeneralSettingsResourcesImportValidator(context,
                suiteFile, newArrayList(resImport), reporter);
        try {
            validator.validate(null);
        } catch (final CoreException e) {
            throw new IllegalStateException("Unable to validate", e);
        }
    }

    private void validateResourceFile(final File resource, final String toImport) {
        final RobotSuiteFile suiteFile = createResourceImportingSuite(toImport);
        final ResourceImport resImport = getImport(suiteFile);

        final FileValidationContext context = prepareContext(suiteFile);

        final GeneralSettingsResourcesImportValidator validator = new GeneralSettingsResourcesImportValidator(context,
                suiteFile, newArrayList(resImport), reporter);

        final RobotToken token = RobotToken.create(toImport);

        validator.validateFile(resource, toImport, token, null);
    }

    private RobotSuiteFile createResourceImportingSuite(final String toImport) {
        try {
            final IFile file = project.createFile("suite.robot", "*** Settings ***", "Resource  " + toImport);
            final RobotSuiteFile suite = model.createSuiteFile(file);
            suite.dispose();
            return suite;
        } catch (IOException | CoreException e) {
            throw new IllegalStateException("Cannot create file", e);
        }
    }

    private FileValidationContext prepareContext(final RobotSuiteFile suiteFile) {
        final ValidationContext parentContext = new ValidationContext(
                suiteFile.getRobotProject().getRobotProjectConfig(),
                model, RobotVersion.from("0.0"), SuiteExecutor.Python, ArrayListMultimap.create());
        return new FileValidationContext(parentContext, suiteFile.getFile());
    }

    private ResourceImport getImport(final RobotSuiteFile suiteFile) {
        return (ResourceImport) suiteFile.findSection(RobotSettingsSection.class).get().getChildren().get(0)
                .getLinkedElement();
    }

    private static File getFile(final File tempFolder, final String... path) {
        if (path == null || path.length == 0) {
            return tempFolder;
        } else {
            return getFile(new File(tempFolder, path[0]), Arrays.copyOfRange(path, 1, path.length));
        }
    }
}
