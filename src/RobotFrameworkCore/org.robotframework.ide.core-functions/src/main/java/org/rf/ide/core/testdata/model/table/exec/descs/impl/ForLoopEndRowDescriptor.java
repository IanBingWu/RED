/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.exec.descs.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.RobotFileOutput.BuildMessage;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor;
import org.rf.ide.core.testdata.model.table.variables.descs.VariableUse;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public class ForLoopEndRowDescriptor<T> implements IExecutableRowDescriptor<T> {

    private final RobotExecutableRow<T> row;

    public ForLoopEndRowDescriptor(final RobotExecutableRow<T> row) {
        this.row = row;
    }

    @Override
    public boolean isCreatingVariables() {
        return false;
    }

    @Override
    public Stream<RobotToken> getCreatingVariables() {
        return Stream.empty();
    }

    @Override
    public RobotToken getAction() {
        return row.getAction();
    }

    @Override
    public RobotToken getKeywordAction() {
        return getAction();
    }

    @Override
    public List<VariableUse> getUsedVariables() {
        return new ArrayList<>();
    }

    @Override
    public List<BuildMessage> getMessages() {
        return new ArrayList<>();
    }

    @Override
    public List<RobotToken> getKeywordArguments() {
        return new ArrayList<>();
    }

    @Override
    public RowType getRowType() {
        return RowType.FOR_END;
    }

    @Override
    public AModelElement<T> getRow() {
        return row;
    }
}
