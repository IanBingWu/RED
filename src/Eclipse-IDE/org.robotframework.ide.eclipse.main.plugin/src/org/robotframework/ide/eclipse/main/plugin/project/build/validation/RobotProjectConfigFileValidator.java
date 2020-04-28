/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.rf.ide.core.EnvironmentVariableReplacer;
import org.rf.ide.core.SystemVariableAccessor;
import org.rf.ide.core.environment.SuiteExecutor;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.ConfigVersion;
import org.rf.ide.core.project.RobotProjectConfig.ExcludedPath;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedVariableFile;
import org.rf.ide.core.project.RobotProjectConfig.RemoteLocation;
import org.rf.ide.core.project.RobotProjectConfig.SearchPath;
import org.rf.ide.core.project.RobotProjectConfigReader.CannotReadProjectConfigurationException;
import org.rf.ide.core.project.RobotProjectConfigReader.RobotProjectConfigWithLines;
import org.rf.ide.core.validation.ProblemPosition;
import org.robotframework.ide.eclipse.main.plugin.RedWorkspace;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfigReader;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.ValidationReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ConfigFileProblem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

public class RobotProjectConfigFileValidator implements ModelUnitValidator {

    private final ValidationContext context;

    private final IFile configFile;

    private final ValidationReportingStrategy reporter;

    private final SystemVariableAccessor variableAccessor;

    public RobotProjectConfigFileValidator(final ValidationContext context, final IFile configFile,
            final ValidationReportingStrategy reporter) {
        this(context, configFile, reporter, new SystemVariableAccessor());
    }

    @VisibleForTesting
    RobotProjectConfigFileValidator(final ValidationContext context, final IFile configFile,
            final ValidationReportingStrategy reporter, final SystemVariableAccessor variableAccessor) {
        this.context = context;
        this.configFile = configFile;
        this.reporter = reporter;
        this.variableAccessor = variableAccessor;
    }

    @Override
    public void validate(final IProgressMonitor monitor) throws CoreException {
        try {
            final RobotProjectConfigWithLines config = new RedEclipseProjectConfigReader()
                    .readConfigurationWithLines(configFile);
            validate(monitor, config);
        } catch (final CannotReadProjectConfigurationException e) {
            // this problem is handled by RobotLibraries builder
            return;
        }
    }

    @VisibleForTesting
    void validate(final IProgressMonitor monitor, final RobotProjectConfigWithLines config) {
        final RobotProjectConfig model = config.getConfigurationModel();

        final boolean shallContinue = validateConfigVersion(model.getVersion(), config);
        if (!shallContinue) {
            return;
        }

        validateLibspecsAreGenerated(monitor, config);

        for (final RemoteLocation location : model.getRemoteLocations()) {
            validateRemoteLocation(monitor, location, config);
        }
        int index = 0;
        for (final ReferencedLibrary library : model.getReferencedLibraries()) {
            validateReferencedLibrary(monitor, library, index, config);
            index++;
        }
        for (final SearchPath path : model.getPythonPaths()) {
            validateSearchPath(monitor, path, config);
        }
        for (final SearchPath path : model.getClassPaths()) {
            validateSearchPath(monitor, path, config);
        }
        for (final ReferencedVariableFile variableFile : model.getReferencedVariableFiles()) {
            validateReferencedVariableFile(monitor, variableFile, config);
        }
        for (final ExcludedPath excludedPath : model.getExcludedPaths()) {
            validateExcludedPath(monitor, excludedPath, model.getExcludedPaths(), config);
        }
    }

    private boolean validateConfigVersion(final ConfigVersion version, final RobotProjectConfigWithLines config) {
        // this should be updated when current version does not support older versions
        final boolean isValidVersion = RobotProjectConfig.CURRENT_VERSION.equals(version.getVersion());
        if (!isValidVersion) {
            final RobotProblem invalidVersionProblem = RobotProblem.causedBy(ConfigFileProblem.INVALID_VERSION)
                    .formatMessageWith(version.getVersion(), RobotProjectConfig.CURRENT_VERSION);
            final ProblemPosition position = new ProblemPosition(config.getLineFor(version));
            reporter.handleProblem(invalidVersionProblem, configFile, position);
        }
        return isValidVersion;
    }

    private void validateLibspecsAreGenerated(final IProgressMonitor monitor,
            final RobotProjectConfigWithLines config) {
        if (monitor.isCanceled()) {
            return;
        }

        final RobotProject robotProject = context.getModel().createRobotProject(configFile.getProject());

        robotProject.getLibraryEntriesStream()
                .filter(entry -> entry.getValue() == null)
                .map(Entry::getKey)
                .forEach(descriptor -> {
                    final String name;
                    final ProblemPosition position;

                    if (descriptor.isStandardRemoteLibrary()) {
                        final RemoteLocation remoteLocation = RemoteLocation.create(descriptor.getArguments().get(0));
                        name = remoteLocation.getRemoteName();
                        position = new ProblemPosition(config.getLineFor(remoteLocation));

                    } else if (descriptor.isStandardLibrary()) {
                        name = descriptor.getName();
                        position = new ProblemPosition(config.getLineFor(config.getConfigurationModel()));

                    } else {
                        final ReferencedLibrary refLib = ReferencedLibrary.create(descriptor.getLibraryType(),
                                descriptor.getName(), descriptor.getPath());
                        name = refLib.getName();
                        position = new ProblemPosition(config.getLineFor(refLib));
                    }
                    final RobotProblem problem = RobotProblem
                            .causedBy(ConfigFileProblem.LIBRARY_SPEC_CANNOT_BE_GENERATED)
                            .formatMessageWith(name);
                    reporter.handleProblem(problem, configFile, position);
                });
    }

    private void validateRemoteLocation(final IProgressMonitor monitor, final RemoteLocation location,
            final RobotProjectConfigWithLines config) {
        if (monitor.isCanceled()) {
            return;
        }
        if (!location.isReachable(RemoteLocation.DEFAULT_TIMEOUT)) {
            final RobotProblem problem = RobotProblem.causedBy(ConfigFileProblem.UNREACHABLE_HOST)
                    .formatMessageWith(location.getUri());
            final ProblemPosition position = new ProblemPosition(config.getLineFor(location));
            reporter.handleProblem(problem, configFile, position);
        }
    }

    private void validateReferencedLibrary(final IProgressMonitor monitor, final ReferencedLibrary library,
            final int index, final RobotProjectConfigWithLines config) {
        if (monitor.isCanceled()) {
            return;
        }
        final LibraryType libType = library.provideType();
        final IPath libraryPath = Path.fromPortableString(library.getPath());
        final ProblemPosition position = new ProblemPosition(config.getLineFor(library));
        final Map<String, Object> additional = ImmutableMap.of(ConfigFileProblem.LIBRARY_INDEX, index);

        final List<RobotProblem> libProblems = new ArrayList<>();
        libProblems.addAll(validatePath(libraryPath, ConfigFileProblem.MISSING_LIBRARY_FILE));
        if (libType == LibraryType.JAVA) {
            if (context.getExecutorInUse() != SuiteExecutor.Jython) {
                libProblems.add(RobotProblem.causedBy(ConfigFileProblem.JAVA_LIB_IN_NON_JAVA_ENV)
                        .formatMessageWith(libraryPath, context.getExecutorInUse()));
            }
            if (!(libraryPath.toString().toLowerCase().endsWith(".jar")
                    || libraryPath.toString().toLowerCase().endsWith(".zip"))) {
                libProblems.add(RobotProblem.causedBy(ConfigFileProblem.JAVA_LIB_NOT_A_JAR_OR_ZIP_FILE)
                        .formatMessageWith(libraryPath));
            }
        }

        for (final RobotProblem problem : libProblems) {
            reporter.handleProblem(problem, configFile, position, additional);
        }
    }

    private List<RobotProblem> validatePath(final IPath path, final ConfigFileProblem missingFileProblem) {
        final List<RobotProblem> problems = new ArrayList<>();
        if (path.isAbsolute()) {
            problems.add(RobotProblem.causedBy(ConfigFileProblem.ABSOLUTE_PATH).formatMessageWith(path));
        }
        if (!RedWorkspace.Paths.toAbsoluteFromWorkspaceRelativeIfPossible(path).toFile().exists()) {
            problems.add(RobotProblem.causedBy(missingFileProblem).formatMessageWith(path));
        }
        return problems;
    }

    private void validateSearchPath(final IProgressMonitor monitor, final SearchPath searchPath,
            final RobotProjectConfigWithLines config) {
        if (monitor.isCanceled()) {
            return;
        }
        final ProblemPosition position = new ProblemPosition(config.getLineFor(searchPath));
        final RedEclipseProjectConfig redConfig = new RedEclipseProjectConfig(configFile.getProject(),
                config.getConfigurationModel(), variableAccessor,
                () -> new EnvironmentVariableReplacer(variableAccessor, varName -> {
                    final RobotProblem problem = RobotProblem.causedBy(ConfigFileProblem.UNKNOWN_ENV_VARIABLE)
                            .formatMessageWith("%{" + varName + "}");
                    reporter.handleProblem(problem, configFile, position);
                }));
        final IPath absolutePath = redConfig.resolveToAbsolutePath(searchPath);
        if (!RedWorkspace.Paths.isCorrect(absolutePath)) {
            final RobotProblem problem = RobotProblem.causedBy(ConfigFileProblem.INVALID_SEARCH_PATH)
                    .formatMessageWith(absolutePath.toOSString());
            reporter.handleProblem(problem, configFile, position);
        } else if (!absolutePath.toFile().exists()) {
            final RobotProblem problem = RobotProblem.causedBy(ConfigFileProblem.MISSING_SEARCH_PATH)
                    .formatMessageWith(absolutePath.toOSString());
            reporter.handleProblem(problem, configFile, position);
        }
    }

    private void validateReferencedVariableFile(final IProgressMonitor monitor,
            final ReferencedVariableFile variableFile, final RobotProjectConfigWithLines config) {
        if (monitor.isCanceled()) {
            return;
        }

        final IPath libraryPath = Path.fromPortableString(variableFile.getPath());
        final List<RobotProblem> pathProblems = validatePath(libraryPath, ConfigFileProblem.MISSING_VARIABLE_FILE);
        for (final RobotProblem pathProblem : pathProblems) {
            final ProblemPosition position = new ProblemPosition(config.getLineFor(variableFile));
            reporter.handleProblem(pathProblem, configFile, position);
        }
    }

    private void validateExcludedPath(final IProgressMonitor monitor, final ExcludedPath excludedPath,
            final List<ExcludedPath> allExcluded, final RobotProjectConfigWithLines config) {
        if (monitor.isCanceled()) {
            return;
        }
        final IProject project = configFile.getProject();
        final IPath asExcludedPath = Path.fromPortableString(excludedPath.getPath());
        final Path projectPath = new Path(project.getName());
        if (!project.exists(asExcludedPath)) {
            final RobotProblem problem = RobotProblem.causedBy(ConfigFileProblem.MISSING_EXCLUDED_FOLDER)
                    .formatMessageWith(projectPath.append(asExcludedPath));
            final ProblemPosition position = new ProblemPosition(config.getLineFor(excludedPath));
            reporter.handleProblem(problem, configFile, position);
        }

        for (final ExcludedPath otherPath : allExcluded) {
            if (otherPath != excludedPath) {
                final IPath otherAsPath = Path.fromPortableString(otherPath.getPath());
                if (otherAsPath.isPrefixOf(asExcludedPath)) {
                    final RobotProblem problem = RobotProblem.causedBy(ConfigFileProblem.USELESS_FOLDER_EXCLUSION)
                            .formatMessageWith(projectPath.append(asExcludedPath), projectPath.append(otherAsPath));
                    final ProblemPosition position = new ProblemPosition(config.getLineFor(excludedPath));
                    reporter.handleProblem(problem, configFile, position);
                }
            }
        }
    }
}
