/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.mapping.setting.test;

import java.util.List;
import java.util.Stack;

import org.rf.ide.core.testdata.mapping.table.ElementsUtility;
import org.rf.ide.core.testdata.mapping.table.IParsingMapper;
import org.rf.ide.core.testdata.mapping.table.ParsingStateHelper;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.setting.TestSetup;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class TestSetupKeywordArgumentMapper implements IParsingMapper {

    protected final ElementsUtility utility = new ElementsUtility();

    private final ParsingStateHelper stateHelper = new ParsingStateHelper();

    @Override
    public boolean checkIfCanBeMapped(final RobotFileOutput robotFileOutput, final RobotLine currentLine,
            final RobotToken rt, final String text, final Stack<ParsingState> processingState) {

        final ParsingState state = stateHelper.getCurrentState(processingState);
        if (state == ParsingState.SETTING_TEST_SETUP) {
            final List<TestSetup> testSetups = robotFileOutput.getFileModel().getSettingTable().getTestSetups();
            return canBeMappedTo(testSetups);
        }
        return state == ParsingState.SETTING_TEST_SETUP_KEYWORD
                || state == ParsingState.SETTING_TEST_SETUP_KEYWORD_ARGUMENT;
    }

    protected boolean canBeMappedTo(final List<TestSetup> testSetups) {
        return utility.checkIfLastHasKeywordNameAlready(testSetups);
    }

    @Override
    public RobotToken map(final RobotLine currentLine, final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final RobotToken rt, final FilePosition fp, final String text) {

        rt.getTypes().add(0, RobotTokenType.SETTING_TEST_SETUP_KEYWORD_ARGUMENT);
        rt.setText(text);

        final SettingTable settings = robotFileOutput.getFileModel().getSettingTable();
        final List<TestSetup> setups = settings.getTestSetups();
        if (!setups.isEmpty()) {
            setups.get(setups.size() - 1).addArgument(rt);
        }

        processingState.push(ParsingState.SETTING_TEST_SETUP_KEYWORD_ARGUMENT);
        return rt;
    }
}
