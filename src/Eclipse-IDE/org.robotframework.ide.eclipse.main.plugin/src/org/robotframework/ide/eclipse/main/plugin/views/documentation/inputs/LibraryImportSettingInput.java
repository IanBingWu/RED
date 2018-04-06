/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.views.documentation.inputs;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.resources.IFile;
import org.rf.ide.core.libraries.Documentation;
import org.rf.ide.core.libraries.LibrarySpecification;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSetting;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSetting.ImportedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.build.BuildLogger;
import org.robotframework.ide.eclipse.main.plugin.project.build.libs.LibrariesBuilder;
import org.robotframework.ide.eclipse.main.plugin.views.documentation.LibraryUri;


public class LibraryImportSettingInput extends InternalElementInput<RobotSetting> {

    private LibrarySpecification specification;

    public LibraryImportSettingInput(final RobotSetting libraryImportSetting) {
        super(libraryImportSetting);
    }

    @Override
    public void prepare() {
        specification = element.getImportedLibrary().map(ImportedLibrary::getSpecification).orElseThrow(
                () -> new DocumentationInputGenerationException("Library specification not found, nothing to display"));
    }

    @Override
    public URI getInputUri() throws URISyntaxException {
        final String projectName = element.getSuiteFile().getProject().getName();
        return LibraryUri.createShowLibraryDocUri(projectName, specification.getName());
    }

    @Override
    protected String createHeader() {
        return LibrarySpecificationInput.createHeader(specification);
    }

    @Override
    protected Documentation createDocumentation() {
        return specification.createDocumentation();
    }

    @Override
    protected String localKeywordsLinker(final String name) {
        try {
            final String projectName = element.getSuiteFile().getProject().getName();
            return LibraryUri.createShowKeywordDocUri(projectName, specification.getName(), name).toString();
        } catch (final URISyntaxException e) {
            return "#";
        }
    }

    @Override
    public IFile generateHtmlLibdoc() {
        return new LibrariesBuilder(new BuildLogger()).buildHtmlLibraryDoc(element.getSuiteFile().getProject(),
                specification);
    }
}