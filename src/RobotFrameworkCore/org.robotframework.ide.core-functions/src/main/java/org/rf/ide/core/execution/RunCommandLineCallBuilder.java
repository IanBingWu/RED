/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution;

import static com.google.common.base.Predicates.not;
import static java.util.stream.Collectors.joining;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.rf.ide.core.RedTemporaryDirectory;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder.PythonInstallationDirectory;
import org.rf.ide.core.environment.SuiteExecutor;

import com.google.common.base.Strings;

/**
 * @author Michal Anglart
 */
public class RunCommandLineCallBuilder {

    public static interface IRunCommandLineBuilder {

        IRunCommandLineBuilder withExecutableFile(final File executableFile);

        IRunCommandLineBuilder addUserArgumentsForExecutableFile(final Collection<String> arguments);

        IRunCommandLineBuilder useSingleRobotCommandLineArg(boolean shouldUseSingleRobotCommandLineArg);

        IRunCommandLineBuilder addUserArgumentsForInterpreter(final Collection<String> arguments);

        IRunCommandLineBuilder useArgumentFile(boolean shouldUseArgumentFile);

        IRunCommandLineBuilder addLocationsToPythonPath(final Collection<String> paths);

        IRunCommandLineBuilder addLocationsToClassPath(final Collection<String> paths);

        IRunCommandLineBuilder suitesToRun(final Collection<String> suites);

        IRunCommandLineBuilder testsToRun(final Collection<String> tests);

        IRunCommandLineBuilder includeTags(final Collection<String> tags);

        IRunCommandLineBuilder excludeTags(final Collection<String> tags);

        IRunCommandLineBuilder addUserArgumentsForRobot(final Collection<String> arguments);

        IRunCommandLineBuilder withDataSources(final Collection<File> dataSources);

        RunCommandLine build() throws IOException;
    }

    private static class Builder implements IRunCommandLineBuilder {

        private final SuiteExecutor executor;

        private final String executorPath;

        private final int listenerPort;

        private boolean useArgumentFile = false;

        private final List<String> pythonPathLocations = new ArrayList<>();

        private final List<String> classPathLocations = new ArrayList<>();

        private final List<String> suitesToRun = new ArrayList<>();

        private final List<String> testsToRun = new ArrayList<>();

        private final List<String> tagsToInclude = new ArrayList<>();

        private final List<String> tagsToExclude = new ArrayList<>();

        private final List<File> dataSources = new ArrayList<>();

        private final List<String> robotUserArgs = new ArrayList<>();

        private final List<String> interpreterUserArgs = new ArrayList<>();

        private File executableFile = null;

        private final List<String> executableFileArgs = new ArrayList<>();

        private boolean useSingleRobotCommandLineArg = false;

        private Builder(final SuiteExecutor executor, final String executorPath, final int listenerPort) {
            this.executor = executor;
            this.executorPath = executorPath;
            this.listenerPort = listenerPort;
        }

        @Override
        public IRunCommandLineBuilder withExecutableFile(final File executableFile) {
            this.executableFile = executableFile;
            return this;
        }

        @Override
        public IRunCommandLineBuilder addUserArgumentsForExecutableFile(final Collection<String> arguments) {
            this.executableFileArgs.addAll(arguments);
            return this;
        }

        @Override
        public IRunCommandLineBuilder useSingleRobotCommandLineArg(final boolean shouldUseSingleRobotCommandLineArg) {
            this.useSingleRobotCommandLineArg = shouldUseSingleRobotCommandLineArg;
            return this;
        }

        @Override
        public IRunCommandLineBuilder addUserArgumentsForInterpreter(final Collection<String> arguments) {
            this.interpreterUserArgs.addAll(arguments);
            return this;
        }

        @Override
        public IRunCommandLineBuilder useArgumentFile(final boolean shouldUseArgumentFile) {
            this.useArgumentFile = shouldUseArgumentFile;
            return this;
        }

        @Override
        public IRunCommandLineBuilder addLocationsToPythonPath(final Collection<String> paths) {
            pythonPathLocations.addAll(paths);
            return this;
        }

        @Override
        public IRunCommandLineBuilder addLocationsToClassPath(final Collection<String> paths) {
            classPathLocations.addAll(paths);
            return this;
        }

        @Override
        public IRunCommandLineBuilder suitesToRun(final Collection<String> suites) {
            suitesToRun.addAll(suites);
            return this;
        }

        @Override
        public IRunCommandLineBuilder testsToRun(final Collection<String> tests) {
            testsToRun.addAll(tests);
            return this;
        }

        @Override
        public IRunCommandLineBuilder includeTags(final Collection<String> tags) {
            tagsToInclude.addAll(tags);
            return this;
        }

        @Override
        public IRunCommandLineBuilder excludeTags(final Collection<String> tags) {
            tagsToExclude.addAll(tags);
            return this;
        }

        @Override
        public IRunCommandLineBuilder addUserArgumentsForRobot(final Collection<String> arguments) {
            this.robotUserArgs.addAll(arguments);
            return this;
        }

        @Override
        public IRunCommandLineBuilder withDataSources(final Collection<File> dataSources) {
            this.dataSources.addAll(dataSources);
            return this;
        }

        @Override
        public RunCommandLine build() throws IOException {
            final RunCommandLine robotRunCommandLine = buildRobotRunCommandLine();

            if (executableFile != null) {
                return buildRunCommandLineWrappedWithExecutable(robotRunCommandLine);
            }

            return robotRunCommandLine;
        }

        private RunCommandLine buildRunCommandLineWrappedWithExecutable(final RunCommandLine robotRunCommandLine) {
            final List<String> cmdLine = new ArrayList<>();
            cmdLine.add(executableFile.getAbsolutePath());
            cmdLine.addAll(executableFileArgs);

            if (useSingleRobotCommandLineArg) {
                cmdLine.add(String.join(" ", robotRunCommandLine.getCommandLine()));
            } else {
                cmdLine.addAll(Arrays.asList(robotRunCommandLine.getCommandLine()));
            }

            return new RunCommandLine(cmdLine, robotRunCommandLine.getArgumentFile().orElse(null));
        }

        private RunCommandLine buildRobotRunCommandLine() throws IOException {
            final List<String> cmdLine = new ArrayList<>();
            cmdLine.add(executorPath);
            cmdLine.addAll(createInterpreterArguments());
            cmdLine.add("-m");
            cmdLine.add("robot.run");

            cmdLine.add("--listener");
            cmdLine.add(RedTemporaryDirectory.copyScriptFile(RedTemporaryDirectory.TEST_RUNNER_AGENT).toPath() + ":"
                    + listenerPort);

            ArgumentsFile argumentsFile = null;
            if (useArgumentFile) {
                argumentsFile = createArgumentsFile();
                cmdLine.add("--argumentfile");
                cmdLine.add(argumentsFile.writeToTemporaryOrUseAlreadyExisting().toPath().toString());
            } else {
                cmdLine.addAll(createInlinedArguments());
            }

            for (final File dataSource : dataSources) {
                cmdLine.add(dataSource.getAbsolutePath());
            }
            return new RunCommandLine(cmdLine, argumentsFile);
        }

        private List<String> createInterpreterArguments() {
            final List<String> interpreterArgs = new ArrayList<>();
            if (executor == SuiteExecutor.Jython) {
                // in case of 'robot' folder existing in project
                final Optional<Path> jythonSitePackagesPath = findJythonSitePackagesPath();
                if (jythonSitePackagesPath.isPresent()) {
                    interpreterArgs.add("-J-Dpython.path=" + jythonSitePackagesPath.get().toString());
                }

                final String classPath = classPath();
                if (!classPath.isEmpty()) {
                    interpreterArgs.add("-J-cp");
                    interpreterArgs.add(classPath);
                }
            }
            interpreterArgs.addAll(interpreterUserArgs);
            return interpreterArgs;
        }

        private List<String> createInlinedArguments() throws IOException {
            final List<String> robotArgs = new ArrayList<>();
            final String pythonPath = pythonPath();
            if (!pythonPath.isEmpty()) {
                robotArgs.add("-P");
                robotArgs.add(pythonPath);
            }
            for (final String tagToInclude : tagsToInclude) {
                robotArgs.add("-i");
                robotArgs.add(tagToInclude);
            }
            for (final String tagToExclude : tagsToExclude) {
                robotArgs.add("-e");
                robotArgs.add(tagToExclude);
            }
            for (final String suite : suitesToRun) {
                robotArgs.add("-s");
                robotArgs.add(suite);
            }
            for (final String test : testsToRun) {
                robotArgs.add("-t");
                robotArgs.add(test);
            }
            robotArgs.addAll(robotUserArgs);
            return robotArgs;
        }

        private ArgumentsFile createArgumentsFile() throws IOException {
            final ArgumentsFile argumentsFile = new ArgumentsFile();
            argumentsFile.addCommentLine("arguments automatically generated");
            final String pythonPath = pythonPath();
            if (!pythonPath.isEmpty()) {
                argumentsFile.addLine("--pythonpath", pythonPath);
            }
            for (final String tagToInclude : tagsToInclude) {
                argumentsFile.addLine("--include", tagToInclude);
            }
            for (final String tagToExclude : tagsToExclude) {
                argumentsFile.addLine("--exclude", tagToExclude);
            }
            for (final String suiteToRun : suitesToRun) {
                argumentsFile.addLine("--suite", suiteToRun);
            }
            for (final String testToRun : testsToRun) {
                argumentsFile.addLine("--test", testToRun);
            }
            if (!robotUserArgs.isEmpty()) {
                addUserArguments(argumentsFile);
            }
            return argumentsFile;
        }

        private void addUserArguments(final ArgumentsFile argumentsFile) {
            argumentsFile.addCommentLine("arguments specified manually by user");

            int i = 0;
            while (i < robotUserArgs.size()) {
                if (robotUserArgs.get(i).startsWith("-") && i < robotUserArgs.size() - 1
                        && !robotUserArgs.get(i + 1).startsWith("-")) {
                    argumentsFile.addLine(robotUserArgs.get(i), robotUserArgs.get(i + 1));
                    i++;
                } else {
                    argumentsFile.addLine(robotUserArgs.get(i));
                }
                i++;
            }
        }

        private String classPath() {
            return classPathLocations.stream().filter(not(Strings::isNullOrEmpty)).collect(joining(File.pathSeparator));
        }

        private String pythonPath() {
            return pythonPathLocations.stream().filter(not(Strings::isNullOrEmpty)).collect(joining(":"));
        }

        private Optional<Path> findJythonSitePackagesPath() {
            final Path jythonPath = Paths.get(executorPath);
            Optional<Path> jythonParentPath = Optional.ofNullable(jythonPath.getParent());
            if (!jythonParentPath.isPresent()) {
                jythonParentPath = PythonInstallationDirectoryFinder.whereIsPythonInterpreter(SuiteExecutor.Jython)
                        .map(PythonInstallationDirectory::toPath);
            }
            return jythonParentPath.filter(path -> path.getFileName() != null)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("bin"))
                    .map(path -> Paths.get(path.getParent().toString(), "Lib", "site-packages"));
        }
    }

    public static IRunCommandLineBuilder create(final SuiteExecutor executor, final String executorPath,
            final int listenerPort) {
        return new Builder(executor, executorPath, listenerPort);
    }

    public static class RunCommandLine {

        private final List<String> commandLine;

        private final ArgumentsFile argFile;

        RunCommandLine(final List<String> commandLine, final ArgumentsFile argFile) {
            this.commandLine = new ArrayList<>(commandLine);
            this.argFile = argFile;
        }

        public Optional<ArgumentsFile> getArgumentFile() {
            return Optional.ofNullable(argFile);
        }

        public String[] getCommandLine() {
            return commandLine.toArray(new String[0]);
        }
    }
}
