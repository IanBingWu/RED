/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.comment;

import java.util.List;

import org.rf.ide.core.testdata.mapping.IHashCommentMapper;
import org.rf.ide.core.testdata.mapping.table.ParsingStateHelper;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.TableHeader;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public class TableHeaderCommentMapper implements IHashCommentMapper {

    private final ParsingStateHelper stateHelper;

    public TableHeaderCommentMapper() {
        this.stateHelper = new ParsingStateHelper();
    }

    @Override
    public boolean isApplicable(final ParsingState state) {
        return stateHelper.isTableHeaderState(state);
    }

    @Override
    public void map(final RobotLine currentLine, final RobotToken rt, final ParsingState currentState,
            final RobotFile fileModel) {
        ARobotSectionTable table;
        if (currentState == ParsingState.SETTING_TABLE_HEADER) {
            table = fileModel.getSettingTable();
        } else if (currentState == ParsingState.VARIABLE_TABLE_HEADER) {
            table = fileModel.getVariableTable();
        } else if (currentState == ParsingState.KEYWORD_TABLE_HEADER) {
            table = fileModel.getKeywordTable();
        } else if (currentState == ParsingState.TEST_CASE_TABLE_HEADER) {
            table = fileModel.getTestCaseTable();
        } else if (currentState == ParsingState.TASKS_TABLE_HEADER) {
            table = fileModel.getTasksTable();
        } else {
            throw new IllegalStateException();
        }

        final List<TableHeader<? extends ARobotSectionTable>> headers = table.getHeaders();
        if (!headers.isEmpty()) {
            final TableHeader<?> header = headers.get(headers.size() - 1);
            header.addCommentPart(rt);
        }
    }
}
