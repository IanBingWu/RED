/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.environment;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.rf.ide.core.libraries.Documentation.DocFormat;
import org.rf.ide.core.libraries.LibrarySpecification.LibdocFormat;
import org.rf.ide.core.rflint.RfLintRule;

/**
 * @author Michal Anglart
 */
interface RobotCommandExecutor {

    List<File> getModulesSearchPaths();

    File getModulePath(String moduleName, EnvironmentSearchPaths additionalPaths);

    List<String> getClassesFromModule(File moduleLocation, EnvironmentSearchPaths additionalPaths);

    Map<String, Object> getVariables(File source, List<String> arguments, EnvironmentSearchPaths additionalPaths);

    Map<String, Object> getGlobalVariables();

    List<String> getStandardLibrariesNames();

    File getStandardLibraryPath(String libName);

    List<List<String>> getSitePackagesLibrariesNames();

    String getRobotVersion();

    void createLibdoc(String libName, File outputFile, LibdocFormat format, EnvironmentSearchPaths additionalPaths);

    void createLibdocInSeparateProcess(String libName, File outputFile, LibdocFormat format,
            EnvironmentSearchPaths additionalPaths, int timeout);

    String createHtmlDoc(String doc, DocFormat format);

    void startLibraryAutoDiscovering(int port, File dataSource, File projectLocation, boolean supportGevent,
            boolean recursiveInVirtualenv, List<String> excludedPaths, EnvironmentSearchPaths additionalPaths);

    void startKeywordAutoDiscovering(int port, File dataSource, boolean supportGevent,
            EnvironmentSearchPaths additionalPaths);

    void stopAutoDiscovering();

    List<RfLintRule> getRfLintRules(List<String> rulesFiles);

    void runRfLint(String host, int port, File projectLocation, List<String> excludedPaths, File filepath,
            List<RfLintRule> rules, List<String> rulesFiles, List<String> additionalArguments);

    String convertRobotDataFile(File originalFile);
}
