/*
 * Copyright 2020 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.red.junit.jupiter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.testdata.text.read.EndOfLineBuilder.EndOfLineTypes;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfigWriter;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectNature;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;


public class ProjectExtension
        implements Extension, BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Namespace NAMESPACE = Namespace.create(ProjectExtension.class);

    private static final String PROJECT_PARAM = "project.state";

    public static void addRobotNature(final IProject project) throws CoreException {
        RobotProjectNature.addRobotNature(project, null, p -> true);
    }

    public static void removeRobotNature(final IProject project) throws CoreException {
        RobotProjectNature.removeRobotNature(project, null, p -> true);
    }

    public static void configure(final IProject project) throws IOException, CoreException {
        configure(project, new RobotProjectConfig());
    }

    public static void configure(final IProject project, final RobotProjectConfig config)
            throws IOException, CoreException {
        createFile(project, RobotProjectConfig.FILENAME, "");
        new RedEclipseProjectConfigWriter().writeConfiguration(config, project);
    }

    public static void deconfigure(final IProject project) throws CoreException {
        project.findMember(RobotProjectConfig.FILENAME).delete(true, null);
    }

    public static IFolder getDir(final IProject project, final String dirPath) {
        return project.getFolder(Path.fromPortableString(dirPath));
    }

    public static IFile getFile(final IProject project, final String filePath) {
        return project.getFile(Path.fromPortableString(filePath));
    }

    public static List<String> getFileContent(final IProject project, final String filePath) {
        return getFileContent(getFile(project, filePath));
    }

    public static List<String> getFileContent(final IFile file) {
        try (final InputStream stream = file.getContents()) {
            return Splitter.on('\n').splitToList(CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8)));
        } catch (IOException | CoreException e) {
            return new ArrayList<>();
        }
    }

    public static IFile createFile(final IProject project, final String filePath, final String... lines)
            throws IOException, CoreException {
        return createFile(getFile(project, filePath), EndOfLineTypes.LF, lines);
    }

    public static IFile createFile(final IProject project, final String filePath, final EndOfLineTypes eolType,
            final String... lines) throws IOException, CoreException {
        return createFile(getFile(project, filePath), eolType, lines);
    }

    public static void createFile(final IProject project, final String filePath, final InputStream inputStream)
            throws IOException, CoreException {
        createFile(getFile(project, filePath), inputStream);
    }

    public static IFolder createDir(final IProject project, final String filePath)
            throws IOException, CoreException {
        return createDir(getDir(project, filePath));
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        FieldsSupport.handleFields(context.getRequiredTestClass(), true, Project.class,
                createAndSetNewProject(context, null));
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        final Object testInstance = context.getRequiredTestInstance();

        FieldsSupport.handleFields(testInstance.getClass(), false, Project.class,
                createAndSetNewProject(context, testInstance));
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        cleanUpAfterTest(context);

        final Object testInstance = context.getRequiredTestInstance();
        FieldsSupport.handleFields(testInstance.getClass(), false, Project.class, deleteProject(testInstance));
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        FieldsSupport.handleFields(context.getRequiredTestClass(), true, Project.class, deleteProject(null));
    }

    private static Consumer<Field> createAndSetNewProject(final ExtensionContext context, final Object testInstance) {
        return field -> {
            assertSupportedType("field", field.getType());

            final Project projectAnnotation = field.getAnnotation(Project.class);
            String projectName = projectAnnotation.name();
            if (projectName.isEmpty()) {
                projectName = field.getDeclaringClass().getSimpleName();
            }
            projectName += projectAnnotation.nameSuffix();
            final String[] directoryPaths = projectAnnotation.dirs();
            final String[] filePaths = projectAnnotation.files();
            final boolean createRedXml = projectAnnotation.createDefaultRedXml();
            final boolean useRobotNature = projectAnnotation.useRobotNature();

            final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            try {
                project.create(null);
                project.open(null);

                createDirectories(project, directoryPaths);
                createFiles(project, filePaths);

                if (createRedXml) {
                    configure(project);
                }
                if (useRobotNature) {
                    addRobotNature(project);
                }
                project.refreshLocal(IResource.DEPTH_INFINITE, null);

                if (field.getType() == IProject.class) {
                    field.set(testInstance, project);

                } else if (field.getType() == StatefulProject.class) {
                    final StatefulProject statefulProject = new StatefulProject(project,
                            projectAnnotation.cleanUpAfterEach());
                    field.set(testInstance, statefulProject);

                    final String key = createStoreKey(context);
                    final Store store = context.getStore(NAMESPACE);
                    @SuppressWarnings("unchecked")
                    List<StatefulProject> projects = store.get(key, List.class);
                    if (projects == null) {
                        projects = new ArrayList<>();
                        store.put(key, projects);
                    }
                    projects.add(statefulProject);
                }

            } catch (CoreException | IllegalArgumentException | IllegalAccessException | IOException e) {
                if (project.exists()) {
                    try {
                        project.delete(true, null);
                    } catch (final CoreException e1) {
                    }
                }
            }
        };
    }

    private static void cleanUpAfterTest(final ExtensionContext context) {
        final Store store = context.getStore(NAMESPACE);
        
        final String staticLevelKey = PROJECT_PARAM + ":" + context.getRequiredTestClass().getSimpleName();
        final String testLevelKey = PROJECT_PARAM + ":" + context.getRequiredTestClass().getSimpleName() + "#"
                + context.getRequiredTestMethod().getName();
        
        @SuppressWarnings("unchecked")
        final List<StatefulProject> staticLevelProject = store.get(staticLevelKey, List.class);
        if (staticLevelProject != null) {
            staticLevelProject.forEach(StatefulProject::cleanUp);
        }
        @SuppressWarnings("unchecked")
        final List<StatefulProject> testLevelProject = store.get(testLevelKey, List.class);
        if (testLevelProject != null) {
            staticLevelProject.forEach(StatefulProject::cleanUp);
        }
    }

    private static String createStoreKey(final ExtensionContext context) {
        final String className = context.getRequiredTestClass().getSimpleName();
        return PROJECT_PARAM + ":"
                + context.getTestMethod().map(method -> className + "#" + method.getName()).orElse(className);
    }

    private static void createDirectories(final IProject project, final String[] directoryPaths) throws CoreException {
        for (final String dirPath : directoryPaths) {
            final IFolder directory = getDir(project, dirPath);
            directory.create(true, true, null);
        }
    }

    private static void createFiles(final IProject project, final String[] filePaths)
            throws IOException, CoreException {
        for (final String filePath : filePaths) {
            createFile(project, filePath);
        }
    }

    private static IFile createFile(final IFile file, final EndOfLineTypes eolType, final String... lines)
            throws IOException, CoreException {
        try (InputStream source = new ByteArrayInputStream(
                String.join(eolType.getRepresentation().get(0), lines).getBytes(Charsets.UTF_8))) {
            return createFile(file, source);
        }
    }

    private static IFile createFile(final IFile file, final InputStream fileSource) throws IOException, CoreException {
        if (file.exists()) {
            file.setContents(fileSource, true, false, null);
        } else {
            file.create(fileSource, true, null);
        }
        file.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
        return file;
    }

    private static IFolder createDir(final IFolder directory) throws CoreException {
        directory.create(true, true, null);
        return directory;
    }

    private static Consumer<Field> deleteProject(final Object instance) {
        return field -> {
            try {
                Object project = field.get(instance);
                if (project instanceof StatefulProject) {
                    project = ((StatefulProject) project).getProject();
                }
                if (project instanceof IProject && ((IProject) project).exists()) {
                    ((IProject) project).delete(true, null);
                }
            } catch (final Throwable e) {
            } finally {
                try {
                    field.set(instance, null);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                }
            }
        };
    }

    private static void assertSupportedType(final String target, final Class<?> type) {
        if (type != IProject.class && type != StatefulProject.class) {
            throw new ExtensionConfigurationException("Can only resolve @" + Project.class.getSimpleName() + " "
                    + target + " of type " + IProject.class.getName() + " or " + StatefulProject.class.getName()
                    + " but was: " + type.getName());
        }
    }
}
