/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.project;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rf.ide.core.project.RobotProjectConfig.ExcludedPath;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibraryArgumentsVariant;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedVariableFile;
import org.rf.ide.core.project.RobotProjectConfig.RemoteLocation;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.rf.ide.core.project.RobotProjectConfig.VariableMapping;

public class RobotProjectConfigTest {

    @Test
    public void referenceLibraryIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedLibrary> libs = newArrayList(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));
        config.setReferencedLibraries(libs);

        final boolean result = config
                .addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "Lib3", "ghi/Lib3.py"));

        assertThat(result).isTrue();
        assertThat(config.getReferencedLibraries()).containsExactly(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib3", "ghi/Lib3.py"));
    }

    @Test
    public void referenceLibraryIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedLibrary> libs = newArrayList(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));
        config.setReferencedLibraries(libs);

        final boolean result = config
                .addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));

        assertThat(result).isFalse();
        assertThat(config.getReferencedLibraries()).containsExactly(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));
    }

    @Test
    public void referenceLibraryIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedLibrary> libs = newArrayList(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));
        config.setReferencedLibraries(libs);

        final boolean result = config.removeReferencedLibraries(
                newArrayList(ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py")));

        assertThat(result).isTrue();
        assertThat(config.getReferencedLibraries())
                .containsExactly(ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"));
    }

    @Test
    public void referenceLibraryIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedLibrary> libs = newArrayList(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));
        config.setReferencedLibraries(libs);

        final boolean result = config.removeReferencedLibraries(
                newArrayList(ReferencedLibrary.create(LibraryType.PYTHON, "Lib3", "ghi/Lib3.py")));

        assertThat(result).isFalse();
        assertThat(config.getReferencedLibraries()).containsExactly(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib1", "abc/Lib1.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib2", "def/Lib2.py"));
    }

    @Test
    public void remoteLocationIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<RemoteLocation> locations = newArrayList(RemoteLocation.create("abc"), RemoteLocation.create("def"));
        config.setRemoteLocations(locations);

        final boolean result = config.addRemoteLocation(RemoteLocation.create("ghi"));

        assertThat(result).isTrue();
        assertThat(config.getRemoteLocations()).containsExactly(RemoteLocation.create("abc"),
                RemoteLocation.create("def"), RemoteLocation.create("ghi"));
    }

    @Test
    public void remoteLocationIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<RemoteLocation> locations = newArrayList(RemoteLocation.create("abc"), RemoteLocation.create("def"));
        config.setRemoteLocations(locations);

        final boolean result = config.addRemoteLocation(RemoteLocation.create("def"));

        assertThat(result).isFalse();
        assertThat(config.getRemoteLocations()).containsExactly(RemoteLocation.create("abc"),
                RemoteLocation.create("def"));
    }

    @Test
    public void remoteLocationIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<RemoteLocation> locations = newArrayList(RemoteLocation.create("abc"), RemoteLocation.create("def"));
        config.setRemoteLocations(locations);

        final boolean result = config.removeRemoteLocations(newArrayList(RemoteLocation.create("def")));

        assertThat(result).isTrue();
        assertThat(config.getRemoteLocations()).containsExactly(RemoteLocation.create("abc"));
    }

    @Test
    public void remoteLocationIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<RemoteLocation> locations = newArrayList(RemoteLocation.create("abc"), RemoteLocation.create("def"));
        config.setRemoteLocations(locations);

        final boolean result = config.removeRemoteLocations(newArrayList(RemoteLocation.create("ghi")));

        assertThat(result).isFalse();
        assertThat(config.getRemoteLocations()).containsExactly(RemoteLocation.create("abc"),
                RemoteLocation.create("def"));
    }

    @Test
    public void pythonPathIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setPythonPaths(paths);

        final boolean result = config.addPythonPath(SearchPath.create("ghi"));

        assertThat(result).isTrue();
        assertThat(config.getPythonPaths()).containsExactly(SearchPath.create("abc"), SearchPath.create("def"),
                SearchPath.create("ghi"));
    }

    @Test
    public void pythonPathIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setPythonPaths(paths);

        final boolean result = config.addPythonPath(SearchPath.create("def"));

        assertThat(result).isFalse();
        assertThat(config.getPythonPaths()).containsExactly(SearchPath.create("abc"), SearchPath.create("def"));
    }

    @Test
    public void pythonPathIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setPythonPaths(paths);

        final boolean result = config.removePythonPaths(newArrayList(SearchPath.create("def")));

        assertThat(result).isTrue();
        assertThat(config.getPythonPaths()).containsExactly(SearchPath.create("abc"));
    }

    @Test
    public void pythonPathIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setPythonPaths(paths);

        final boolean result = config.removePythonPaths(newArrayList(SearchPath.create("ghi")));

        assertThat(result).isFalse();
        assertThat(config.getPythonPaths()).containsExactly(SearchPath.create("abc"), SearchPath.create("def"));
    }

    @Test
    public void classPathIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setClassPaths(paths);

        final boolean result = config.addClassPath(SearchPath.create("ghi"));

        assertThat(result).isTrue();
        assertThat(config.getClassPaths()).containsExactly(SearchPath.create("abc"), SearchPath.create("def"),
                SearchPath.create("ghi"));
    }

    @Test
    public void classPathIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setClassPaths(paths);

        final boolean result = config.addClassPath(SearchPath.create("def"));

        assertThat(result).isFalse();
        assertThat(config.getClassPaths()).containsExactly(SearchPath.create("abc"), SearchPath.create("def"));
    }

    @Test
    public void classPathIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setClassPaths(paths);

        final boolean result = config.removeClassPaths(newArrayList(SearchPath.create("def")));

        assertThat(result).isTrue();
        assertThat(config.getClassPaths()).containsExactly(SearchPath.create("abc"));
    }

    @Test
    public void classPathIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<SearchPath> paths = newArrayList(SearchPath.create("abc"), SearchPath.create("def"));
        config.setClassPaths(paths);

        final boolean result = config.removeClassPaths(newArrayList(SearchPath.create("ghi")));

        assertThat(result).isFalse();
        assertThat(config.getClassPaths()).containsExactly(SearchPath.create("abc"), SearchPath.create("def"));
    }

    @Test
    public void variableFileIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedVariableFile> files = newArrayList(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"));
        config.setReferencedVariableFiles(files);

        final boolean result = config.addReferencedVariableFile(ReferencedVariableFile.create("ghi"));

        assertThat(result).isTrue();
        assertThat(config.getReferencedVariableFiles()).containsExactly(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"), ReferencedVariableFile.create("ghi"));
    }

    @Test
    public void variableFileIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedVariableFile> files = newArrayList(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"));
        config.setReferencedVariableFiles(files);

        final boolean result = config.addReferencedVariableFile(ReferencedVariableFile.create("def"));

        assertThat(result).isFalse();
        assertThat(config.getReferencedVariableFiles()).containsExactly(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"));
    }

    @Test
    public void variableFileIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedVariableFile> files = newArrayList(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"));
        config.setReferencedVariableFiles(files);

        final boolean result = config.removeReferencedVariableFiles(newArrayList(ReferencedVariableFile.create("def")));

        assertThat(result).isTrue();
        assertThat(config.getReferencedVariableFiles()).containsExactly(ReferencedVariableFile.create("abc"));
    }

    @Test
    public void variableFileIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ReferencedVariableFile> files = newArrayList(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"));
        config.setReferencedVariableFiles(files);

        final boolean result = config.removeReferencedVariableFiles(newArrayList(ReferencedVariableFile.create("ghi")));

        assertThat(result).isFalse();
        assertThat(config.getReferencedVariableFiles()).containsExactly(ReferencedVariableFile.create("abc"),
                ReferencedVariableFile.create("def"));
    }

    @Test
    public void variableMappingIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<VariableMapping> mappings = newArrayList(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"));
        config.setVariableMappings(mappings);

        final boolean result = config.addVariableMapping(VariableMapping.create("${ghi}", "3"));

        assertThat(result).isTrue();
        assertThat(config.getVariableMappings()).containsExactly(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"), VariableMapping.create("${ghi}", "3"));
    }

    @Test
    public void variableMappingIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<VariableMapping> mappings = newArrayList(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"));
        config.setVariableMappings(mappings);

        final boolean result = config.addVariableMapping(VariableMapping.create("${def}", "2"));

        assertThat(result).isFalse();
        assertThat(config.getVariableMappings()).containsExactly(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"));
    }

    @Test
    public void variableMappingIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<VariableMapping> mappings = newArrayList(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"));
        config.setVariableMappings(mappings);

        final boolean result = config.removeVariableMappings(newArrayList(VariableMapping.create("${def}", "2")));

        assertThat(result).isTrue();
        assertThat(config.getVariableMappings()).containsExactly(VariableMapping.create("${abc}", "1"));
    }

    @Test
    public void variableMappingIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<VariableMapping> mappings = newArrayList(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"));
        config.setVariableMappings(mappings);

        final boolean result = config.removeVariableMappings(newArrayList(VariableMapping.create("${ghi}", "3")));

        assertThat(result).isFalse();
        assertThat(config.getVariableMappings()).containsExactly(VariableMapping.create("${abc}", "1"),
                VariableMapping.create("${def}", "2"));
    }

    @Test
    public void excludedPathIsAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ExcludedPath> paths = newArrayList(ExcludedPath.create("abc"), ExcludedPath.create("def"));
        config.setExcludedPaths(paths);

        final boolean result = config.addExcludedPath("ghi");

        assertThat(result).isTrue();
        assertThat(config.getExcludedPaths()).containsExactly(ExcludedPath.create("abc"), ExcludedPath.create("def"),
                ExcludedPath.create("ghi"));
    }

    @Test
    public void excludedPathIsNotAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ExcludedPath> paths = newArrayList(ExcludedPath.create("abc"), ExcludedPath.create("def"));
        config.setExcludedPaths(paths);

        final boolean result = config.addExcludedPath("def");

        assertThat(result).isFalse();
        assertThat(config.getExcludedPaths()).containsExactly(ExcludedPath.create("abc"), ExcludedPath.create("def"));
    }

    @Test
    public void excludedPathIsRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ExcludedPath> paths = newArrayList(ExcludedPath.create("abc"), ExcludedPath.create("def"));
        config.setExcludedPaths(paths);

        final boolean result = config.removeExcludedPath("def");

        assertThat(result).isTrue();
        assertThat(config.getExcludedPaths()).containsExactly(ExcludedPath.create("abc"));
    }

    @Test
    public void excludedPathIsNotRemoved() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ExcludedPath> paths = newArrayList(ExcludedPath.create("abc"), ExcludedPath.create("def"));
        config.setExcludedPaths(paths);

        final boolean result = config.removeExcludedPath("ghi");

        assertThat(result).isFalse();
        assertThat(config.getExcludedPaths()).containsExactly(ExcludedPath.create("abc"), ExcludedPath.create("def"));
    }

    @Test
    public void excludedPathIsChecked() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        final List<ExcludedPath> paths = newArrayList(ExcludedPath.create("abc"), ExcludedPath.create("def"));
        config.setExcludedPaths(paths);

        assertThat(config.isExcludedPath("abc")).isTrue();
        assertThat(config.isExcludedPath("ghi")).isFalse();
    }

    @Test
    public void sameReferencedLibrariesFromDifferentPaths_areAllAdded() throws Exception {
        final RobotProjectConfig config = new RobotProjectConfig();
        assertThat(config.addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "abc/Lib.py")))
                .isTrue();
        assertThat(config.addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "def/Lib.py")))
                .isTrue();
        assertThat(config.addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "ghi/Lib.py")))
                .isTrue();

        assertThat(config.addReferencedLibrary(ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "abc/Lib.py")))
                .isFalse();

        assertThat(config.getReferencedLibraries()).containsExactly(
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "abc/Lib.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "def/Lib.py"),
                ReferencedLibrary.create(LibraryType.PYTHON, "Lib", "ghi/Lib.py"));
    }

    @Test
    public void nonPythonReferencedLibrary_returnsJustLibraryPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.JAVA, "javaLib", "path/to/return");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void simplePythonReferencedLibrary_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib", "path/to/return/lib.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void dirPythonReferencedLibrary_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib",
                "path/to/return/lib/__init__.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void referencedLibraryWithClass_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib.class",
                "path/to/return/lib.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void dirReferencedLibraryWithClass_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib.class",
                "path/to/return/lib/__init__.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void nestedReferencedLibrary_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.lib",
                "path/to/return/outer/lib.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void nestedDirReferencedLibrary_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.lib",
                "path/to/return/outer/lib/__init__.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void nestedReferencedLibraryWithClass_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.lib.class",
                "path/to/return/outer/lib.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void nestedDirReferencedLibraryWithClass_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.lib.class",
                "path/to/return/outer/lib/__init__.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void evenMoreNestedReferencedLibrary_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.even.more.lib",
                "path/to/return/outer/even/more/lib.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void evenMoreNestedDirReferencedLibrary_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.even.more.lib",
                "path/to/return/outer/even/more/lib/__init__.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void evenMoreNestedReferencedLibraryWithClass_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.even.more.lib.class",
                "path/to/return/outer/even/more/lib.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void evenMoreNestedDirReferencedLibraryWithClass_returnsCorrectParentPath() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "outer.even.more.lib.class",
                "path/to/return/outer/even/more/lib/__init__.py");

        assertThat(lib.getParentPath()).isEqualTo("path/to/return");
    }

    @Test
    public void argumentsVariantForStaticLibraryCanBeAdded_whenLibraryHasNoVariant() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib", "/lib.py");
        final ReferencedLibraryArgumentsVariant variant = ReferencedLibraryArgumentsVariant.create("1", "2", "3");

        assertThat(lib.getArgumentsVariants()).isEmpty();

        lib.addArgumentsVariant(variant);

        assertThat(lib.getArgumentsVariants()).containsExactly(variant);
    }

    @Test
    public void argumentsVariantForStaticLibraryCanBeAdded_butTheLibraryModeSwitches() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib", "/lib.py");
        final ReferencedLibraryArgumentsVariant variant1 = ReferencedLibraryArgumentsVariant.create("1", "2", "3");
        final ReferencedLibraryArgumentsVariant variant2 = ReferencedLibraryArgumentsVariant.create("a", "b", "c");
        final ReferencedLibraryArgumentsVariant variant3 = ReferencedLibraryArgumentsVariant.create("x", "y", "z");

        assertThat(lib.isDynamic()).isFalse();
        lib.addArgumentsVariant(variant1);
        assertThat(lib.isDynamic()).isFalse();
        lib.addArgumentsVariant(variant2);
        assertThat(lib.isDynamic()).isTrue();
        lib.addArgumentsVariant(variant3);
        assertThat(lib.isDynamic()).isTrue();

        assertThat(lib.getArgumentsVariants()).containsExactly(variant1, variant2, variant3);
    }

    @Test
    public void argumentsVariantsForDynamicLibraryCanBeAdded() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib", "/lib.py");
        lib.setDynamic(true);
        final ReferencedLibraryArgumentsVariant variant1 = ReferencedLibraryArgumentsVariant.create("1", "2", "3");
        final ReferencedLibraryArgumentsVariant variant2 = ReferencedLibraryArgumentsVariant.create("a", "b", "c");
        final ReferencedLibraryArgumentsVariant variant3 = ReferencedLibraryArgumentsVariant.create("x", "y", "z");

        assertThat(lib.getArgumentsVariants()).isEmpty();

        assertThat(lib.isDynamic()).isTrue();
        lib.addArgumentsVariant(variant1);
        assertThat(lib.isDynamic()).isTrue();
        lib.addArgumentsVariant(variant2);
        assertThat(lib.isDynamic()).isTrue();
        lib.addArgumentsVariant(variant3);
        assertThat(lib.isDynamic()).isTrue();

        assertThat(lib.getArgumentsVariants()).containsExactly(variant1, variant2, variant3);
    }

    @Test
    public void argumentsVariantsStreamHasEmptyVariant_whenThereIsNoVariantAdded() {
        final ReferencedLibrary lib = ReferencedLibrary.create(LibraryType.PYTHON, "lib", "/lib.py");

        assertThat(lib.getArgumentsVariants()).isEmpty();
        assertThat(lib.getArgsVariantsStream()).containsExactly(ReferencedLibraryArgumentsVariant.create());
    }

    @Nested
    class RemoteLocationTest {

        @Test
        void remoteLocationIsCreated() throws Exception {
            assertThat(RemoteLocation.create("127.0.0.1")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("127.0.0.1"));
            assertThat(RemoteLocation.create("127.0.0.1/")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("127.0.0.1/"));
            assertThat(RemoteLocation.create("http://127.0.0.1:8270")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270"));
            assertThat(RemoteLocation.create("http://127.0.0.1:8270/")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270/"));
            assertThat(RemoteLocation.create("http://127.0.0.1:8270/path")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270/path"));
            assertThat(RemoteLocation.create("http://127.0.0.1:8270/path/")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270/path/"));
            assertThat(RemoteLocation.create("https://127.0.0.1:8270/path/")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("https://127.0.0.1:8270/path/"));
            assertThat(RemoteLocation.create("www.somehost.com")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("www.somehost.com"));
        }

        @Test
        void remoteLocationIsCreatedWithDefaultScheme() throws Exception {
            assertThat(RemoteLocation.create("127.0.0.1:8270")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270"));
            assertThat(RemoteLocation.create("127.0.0.1:8270/")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270/"));
            assertThat(RemoteLocation.create("127.0.0.1:8270/path")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270/path"));
            assertThat(RemoteLocation.create("127.0.0.1:8270/path/")).extracting(RemoteLocation::getUri)
                    .isEqualTo(new URI("http://127.0.0.1:8270/path/"));
        }

        @Test
        void remoteLocationIsNotCreated() throws Exception {
            assertThatIllegalArgumentException().isThrownBy(() -> RemoteLocation.create("://127.0.0.1:8270"));
            assertThatIllegalArgumentException().isThrownBy(() -> RemoteLocation.create("127.0.0.1:9000/%"));
        }

        @Test
        void uriIsUnified() throws Exception {
            assertThat(RemoteLocation.unify(new URI("http://101.102.103.104")))
                    .isEqualTo(new URI("http://101.102.103.104/RPC2"));
            assertThat(RemoteLocation.unify(new URI("http://101.102.103.104/")))
                    .isEqualTo(new URI("http://101.102.103.104"));
            assertThat(RemoteLocation.unify(new URI("http://101.102.103.104:8271")))
                    .isEqualTo(new URI("http://101.102.103.104:8271/RPC2"));
            assertThat(RemoteLocation.unify(new URI("http://101.102.103.104:8271/")))
                    .isEqualTo(new URI("http://101.102.103.104:8271"));
            assertThat(RemoteLocation.unify(new URI("http://101.102.103.104:8271/path")))
                    .isEqualTo(new URI("http://101.102.103.104:8271/path"));
            assertThat(RemoteLocation.unify(new URI("http://101.102.103.104:8271/path/")))
                    .isEqualTo(new URI("http://101.102.103.104:8271/path"));
        }

        @Test
        void defaultPortAndPathAreAddedWhenNotSpecified() throws Exception {
            assertThat(RemoteLocation.addDefaults(new URI("http://1.2.3.4")))
                    .isEqualTo(new URI("http://1.2.3.4:8270/RPC2"));
            assertThat(RemoteLocation.addDefaults(new URI("http://1.2.3.4/")))
                    .isEqualTo(new URI("http://1.2.3.4:8270/"));
            assertThat(RemoteLocation.addDefaults(new URI("http://1.2.3.4/path")))
                    .isEqualTo(new URI("http://1.2.3.4:8270/path"));
            assertThat(RemoteLocation.addDefaults(new URI("http://1.2.3.4:456")))
                    .isEqualTo(new URI("http://1.2.3.4:456/RPC2"));
            assertThat(RemoteLocation.addDefaults(new URI("http://1.2.3.4:456/")))
                    .isEqualTo(new URI("http://1.2.3.4:456/"));
            assertThat(RemoteLocation.addDefaults(new URI("http://1.2.3.4:456/path")))
                    .isEqualTo(new URI("http://1.2.3.4:456/path"));
        }
    }

}
