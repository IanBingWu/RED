/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.refactoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robotframework.red.junit.jupiter.ProjectExtension.configure;
import static org.robotframework.red.junit.jupiter.ProjectExtension.createFile;
import static org.robotframework.red.junit.jupiter.ProjectExtension.getFile;

import java.util.Objects;
import java.util.Optional;

import org.assertj.core.api.Condition;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.ExcludedPath;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfigReader;
import org.robotframework.red.junit.Editors;
import org.robotframework.red.junit.jupiter.Project;
import org.robotframework.red.junit.jupiter.ProjectExtension;

@ExtendWith(ProjectExtension.class)
public class RedXmlInTextEditorChangesCollectorTest {

    @Project(dirs = { "a", "a/b", "c", "libs", "libs/inner_lib" }, files = { "libs/inner_lib/__init__.py" })
    static IProject project;

    @BeforeAll
    public static void beforeSuite() throws Exception {
        createFile(project, "libs/lib.py", "class lib(object):", "    ROBOT_LIBRARY_VERSION = 1.0",
                "    def __init__(self):", "        pass", "    def keyword(self):", "        pass");
        createFile(project, "libs/inner_lib/inside.py", "class inside(object):",
                "    ROBOT_LIBRARY_VERSION = 1.0", "    def __init__(self):", "        pass",
                "    def inside_keyword(self):", "        pass");
    }

    @BeforeEach
    public void beforeTest() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        config.addExcludedPath("a");
        config.addExcludedPath("a/b");
        config.addExcludedPath("c");
        config.addReferencedLibrary(
                ReferencedLibrary.create(LibraryType.PYTHON, "lib", project.getName() + "/libs/lib.py"));
        config.addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "inner_lib",
                project.getName() + "/libs/inner_lib/__init__.py"));
        config.addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "inner_lib.inside",
                project.getName() + "/libs/inner_lib/inside.py"));
        config.addReferencedLibrary(
                ReferencedLibrary.create(LibraryType.PYTHON, "inside",
                        project.getName() + "/libs/inner_lib/inside.py"));
        configure(project, config);
    }

    @AfterEach
    public void afterTest() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
    }

    @Test
    public void noChangeIsCollected_whenTheFileIsNotOpened() {
        final IFile redXmlFile = getFile(project, "red.xml");

        final RedXmlInTextEditorChangesCollector collector = new RedXmlInTextEditorChangesCollector(redXmlFile,
                new Path(project.getName() + "/a"), Optional.empty());
        final Optional<Change> change = collector.collect();

        assertThat(change).isNotPresent();
    }

    @Test
    public void noChangeIsCollected_whenTheFileIsOpenedInProjectConfigEditor() throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        Editors.openInProjectEditor(redXmlFile);

        final RedXmlInTextEditorChangesCollector collector = new RedXmlInTextEditorChangesCollector(redXmlFile,
                new Path(project.getName() + "/a"), Optional.empty());
        final Optional<Change> change = collector.collect();

        assertThat(change).isNotPresent();
    }

    @Test
    public void noChangeIsCollected_whenRemovedResourceDoesNotAnything() throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        Editors.openInTextEditor(redXmlFile);

        final RedXmlInFileChangesCollector collector = new RedXmlInFileChangesCollector(redXmlFile,
                new Path(project.getName() + "/x"), Optional.empty());
        final Optional<Change> change = collector.collect();

        assertThat(change).isNotPresent();
    }

    @Test
    public void noChangeIsCollected_whenMovedResourceDoesNotAffectAnything() throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        Editors.openInTextEditor(redXmlFile);

        final RedXmlInFileChangesCollector collector = new RedXmlInFileChangesCollector(redXmlFile,
                new Path(project.getName() + "/x"), Optional.of(new Path(project.getName() + "/renamed")));
        final Optional<Change> change = collector.collect();

        assertThat(change).isNotPresent();
    }

    @Test
    public void documentChangeIsCollected_whenFileIsOpenedInProjectConfigEditorAndResourceRemoveAffectsExcludedPaths()
            throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        final IEditorPart editor = Editors.openInTextEditor(redXmlFile);

        final RedXmlInTextEditorChangesCollector collector = new RedXmlInTextEditorChangesCollector(redXmlFile,
                new Path(project.getName() + "/a"), Optional.empty());
        final Optional<Change> change = collector.collect();

        assertThat(change).isPresent();

        change.get().perform(new NullProgressMonitor());

        assertThat(editor.isDirty()).isTrue();

        // no change in file yet
        final RedEclipseProjectConfigReader reader = new RedEclipseProjectConfigReader();
        assertThat(reader.readConfiguration(redXmlFile).getExcludedPaths()).containsOnly(ExcludedPath.create("a"),
                ExcludedPath.create("a/b"), ExcludedPath.create("c"));

        // after saving the change should be written to the file
        editor.doSave(new NullProgressMonitor());
        assertThat(reader.readConfiguration(redXmlFile).getExcludedPaths()).containsOnly(ExcludedPath.create("c"));
    }

    @Test
    public void documentChangeIsCollected_whenFileIsOpenedInProjectConfigEditorAndResourceMoveAffectsExcludedPaths()
            throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        final IEditorPart editor = Editors.openInTextEditor(redXmlFile);

        final RedXmlInTextEditorChangesCollector collector = new RedXmlInTextEditorChangesCollector(redXmlFile,
                new Path(project.getName() + "/a"), Optional.of(new Path(project.getName() + "/moved")));
        final Optional<Change> change = collector.collect();

        assertThat(change).isPresent();

        change.get().perform(new NullProgressMonitor());

        assertThat(editor.isDirty()).isTrue();

        // no change in file yet
        final RedEclipseProjectConfigReader reader = new RedEclipseProjectConfigReader();
        assertThat(reader.readConfiguration(redXmlFile).getExcludedPaths()).containsOnly(ExcludedPath.create("a"),
                ExcludedPath.create("a/b"), ExcludedPath.create("c"));

        // after saving the change should be written to the file
        editor.doSave(new NullProgressMonitor());
        assertThat(reader.readConfiguration(redXmlFile).getExcludedPaths()).containsOnly(ExcludedPath.create("moved"),
                ExcludedPath.create("moved/b"), ExcludedPath.create("c"));
    }

    @Test
    public void documentChangeIsCollected_whenFileIsOpenedInProjectConfigEditorAndResourceRemoveAffectsLibraries()
            throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        final IEditorPart editor = Editors.openInTextEditor(redXmlFile);

        final RedXmlInTextEditorChangesCollector collector = new RedXmlInTextEditorChangesCollector(redXmlFile,
                new Path(project.getName() + "/libs/inner_lib"), Optional.empty());
        final Optional<Change> change = collector.collect();

        assertThat(change).isPresent();

        change.get().perform(new NullProgressMonitor());

        assertThat(editor.isDirty()).isTrue();

        // no change in file yet
        final RedEclipseProjectConfigReader reader = new RedEclipseProjectConfigReader();
        RobotProjectConfig config = reader.readConfiguration(redXmlFile);
        assertThat(config.getReferencedLibraries()).hasSize(4);
        assertThat(config.getReferencedLibraries().get(0))
                .has(sameFieldsAs(
                        ReferencedLibrary.create(LibraryType.PYTHON, "lib", project.getName() + "/libs/lib.py")));
        assertThat(config.getReferencedLibraries().get(1)).has(sameFieldsAs(ReferencedLibrary.create(LibraryType.PYTHON,
                "inner_lib", project.getName() + "/libs/inner_lib/__init__.py")));
        assertThat(config.getReferencedLibraries().get(2)).has(sameFieldsAs(ReferencedLibrary.create(LibraryType.PYTHON,
                "inner_lib.inside", project.getName() + "/libs/inner_lib/inside.py")));
        assertThat(config.getReferencedLibraries().get(3)).has(sameFieldsAs(
                ReferencedLibrary.create(LibraryType.PYTHON, "inside",
                        project.getName() + "/libs/inner_lib/inside.py")));

        // after saving the change should be written to the file
        editor.doSave(new NullProgressMonitor());
        config = reader.readConfiguration(redXmlFile);
        assertThat(config.getReferencedLibraries()).hasSize(1);
        assertThat(config.getReferencedLibraries().get(0))
                .has(sameFieldsAs(
                        ReferencedLibrary.create(LibraryType.PYTHON, "lib", project.getName() + "/libs/lib.py")));
    }

    @Test
    public void documentChangeIsCollected_whenFileIsOpenedInProjectConfigEditorAndResourceMoveAffectsLibraries()
            throws Exception {
        final IFile redXmlFile = getFile(project, "red.xml");
        final IEditorPart editor = Editors.openInTextEditor(redXmlFile);

        final RedXmlInTextEditorChangesCollector collector = new RedXmlInTextEditorChangesCollector(redXmlFile,
                new Path(project.getName() + "/libs/inner_lib"),
                Optional.of(new Path(project.getName() + "/libs/moved")));
        final Optional<Change> change = collector.collect();

        assertThat(change).isPresent();

        change.get().perform(new NullProgressMonitor());

        assertThat(editor.isDirty()).isTrue();

        // no change in file yet
        final RedEclipseProjectConfigReader reader = new RedEclipseProjectConfigReader();
        RobotProjectConfig config = reader.readConfiguration(redXmlFile);
        assertThat(config.getReferencedLibraries()).hasSize(4);
        assertThat(config.getReferencedLibraries().get(0))
                .has(sameFieldsAs(
                        ReferencedLibrary.create(LibraryType.PYTHON, "lib", project.getName() + "/libs/lib.py")));
        assertThat(config.getReferencedLibraries().get(1)).has(sameFieldsAs(ReferencedLibrary.create(LibraryType.PYTHON,
                "inner_lib", project.getName() + "/libs/inner_lib/__init__.py")));
        assertThat(config.getReferencedLibraries().get(2)).has(sameFieldsAs(ReferencedLibrary.create(LibraryType.PYTHON,
                "inner_lib.inside", project.getName() + "/libs/inner_lib/inside.py")));
        assertThat(config.getReferencedLibraries().get(3)).has(sameFieldsAs(
                ReferencedLibrary.create(LibraryType.PYTHON, "inside",
                        project.getName() + "/libs/inner_lib/inside.py")));

        // after saving the change should be written to the file
        editor.doSave(new NullProgressMonitor());
        config = reader.readConfiguration(redXmlFile);
        assertThat(config.getReferencedLibraries()).hasSize(4);
        assertThat(config.getReferencedLibraries().get(0))
                .has(sameFieldsAs(
                        ReferencedLibrary.create(LibraryType.PYTHON, "lib", project.getName() + "/libs/lib.py")));
        assertThat(config.getReferencedLibraries().get(1)).has(sameFieldsAs(
                ReferencedLibrary.create(LibraryType.PYTHON, "moved", project.getName() + "/libs/moved/__init__.py")));
        assertThat(config.getReferencedLibraries().get(2)).has(sameFieldsAs(
                ReferencedLibrary.create(LibraryType.PYTHON, "moved.inside",
                        project.getName() + "/libs/moved/inside.py")));
        assertThat(config.getReferencedLibraries().get(3)).has(sameFieldsAs(
                ReferencedLibrary.create(LibraryType.PYTHON, "inside", project.getName() + "/libs/moved/inside.py")));
    }

    private static Condition<? super ReferencedLibrary> sameFieldsAs(final ReferencedLibrary library) {
        return new Condition<ReferencedLibrary>() {

            @Override
            public boolean matches(final ReferencedLibrary toMatch) {
                return Objects.equals(library.getType(), toMatch.getType())
                        && Objects.equals(library.getName(), toMatch.getName())
                        && Objects.equals(library.getPath(), toMatch.getPath());
            }
        };
    }
}
