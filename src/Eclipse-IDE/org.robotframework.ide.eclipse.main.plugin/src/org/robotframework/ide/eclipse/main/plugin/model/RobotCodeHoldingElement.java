/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see licence.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model;

import static com.google.common.collect.Lists.newArrayList;

import java.io.Serializable;
import java.util.List;

import org.eclipse.ui.IWorkbenchPage;

public abstract class RobotCodeHoldingElement implements IRobotCodeHoldingElement, Serializable {

    private transient RobotSuiteFileSection parent;

    private String name;
    private String comment;

    private final List<RobotKeywordCall> calls = newArrayList();

    RobotCodeHoldingElement(final RobotSuiteFileSection parent, final String name, final String comment) {
        this.parent = parent;
        this.name = name;
        this.comment = comment;
    }

    public RobotKeywordCall createKeywordCall(final String name, final List<String> args, final String comment) {
        return createKeywordCall(calls.size(), name, args, comment);
    }

    public RobotKeywordCall createKeywordCall(final int index, final String name, final List<String> args,
            final String comment) {
        final RobotKeywordCall call = new RobotKeywordCall(this, name, args, comment);
        getChildren().add(index, call);
        return call;
    }

    public RobotDefinitionSetting createDefinitionSetting(final String name, final List<String> args,
            final String comment) {
        return createDefinitionSetting(getChildren().size(), name, args, comment);
    }

    public RobotDefinitionSetting createDefinitionSetting(final int index, final String name, final List<String> args,
            final String comment) {
        final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, name, args, comment);
        getChildren().add(index, setting);
        return setting;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String newName) {
        this.name = newName;
    }

    @Override
    public String getComment() {
        return comment;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    @Override
    public RobotSuiteFileSection getParent() {
        return parent;
    }

    public void setParent(final RobotSuiteFileSection parent) {
        this.parent = parent;
    }

    public void fixParents() {
        for (final RobotKeywordCall call : calls) {
            call.fixParents(this);
        }
    }

    @Override
    public List<RobotKeywordCall> getChildren() {
        return calls;
    }

    @Override
    public RobotSuiteFile getSuiteFile() {
        return getParent().getSuiteFile();
    }

    @Override
    public OpenStrategy getOpenRobotEditorStrategy(final IWorkbenchPage page) {
        return new PageActivatingOpeningStrategy(page, getSuiteFile().getFile(), getParent(), this);
    }
}
