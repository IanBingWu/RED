/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.libraries;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;

@XmlRootElement(name = "kw")
public class KeywordSpecification {

    public static KeywordSpecification create(final String name, final String... arguments) {
        final KeywordSpecification spec = new KeywordSpecification();
        spec.setName(name);
        for (final String arg : arguments) {
            spec.arguments.add(arg);
        }
        return spec;
    }

    private String name;
    private String documentation;

    private List<String> arguments = new ArrayList<>();

    private Boolean isDeprecated;

    private String sourcePath;
    private Integer lineNumber;

    public String getName() {
        return name;
    }

    @XmlAttribute
    public void setName(final String name) {
        this.name = name;
    }

    public String getDocumentation() {
        return documentation;
    }

    @XmlElement(name = "doc")
    public void setDocumentation(final String documentation) {
        this.documentation = documentation;
    }

    public List<String> getArguments() {
        return arguments;
    }

    @XmlElementWrapper(name = "arguments")
    @XmlElement(name = "arg")
    public void setArguments(final List<String> arguments) {
        this.arguments = arguments;
    }

    public ArgumentsDescriptor createArgumentsDescriptor() {
        return ArgumentsDescriptor.createDescriptor(arguments);
    }

    @XmlAttribute(name = "deprecated")
    public void setDeprecated(final boolean deprecated) {
        this.isDeprecated = Boolean.valueOf(deprecated);
    }

    Boolean getDeprecatedState() {
        return isDeprecated;
    }

    public boolean isDeprecated() {
        if (isDeprecated == null) {
            isDeprecated = Boolean.valueOf(documentation != null
                    && Pattern.compile("^\\*deprecated[^\\n\\r]*\\*.*").matcher(documentation.toLowerCase()).find());
        }
        return isDeprecated.booleanValue();
    }

    @XmlAttribute(name = "source")
    public void setSourcePath(final String path) {
        this.sourcePath = path;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public Optional<File> getSource() {
        return Optional.ofNullable(sourcePath).map(File::new);
    }

    @XmlAttribute(name = "lineno")
    public void setLineNumber(final Integer lineNo) {
        this.lineNumber = lineNo;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (KeywordSpecification.class == obj.getClass()) {
            final KeywordSpecification that = (KeywordSpecification) obj;
            return Objects.equal(this.name, that.name) && Objects.equal(this.arguments, that.arguments);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, arguments);
    }
}
