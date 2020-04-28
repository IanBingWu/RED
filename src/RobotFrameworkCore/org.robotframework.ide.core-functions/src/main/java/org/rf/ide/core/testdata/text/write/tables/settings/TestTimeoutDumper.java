/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.settings;

import java.util.ArrayList;
import java.util.List;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.table.RobotElementsComparatorWithPositionChangedPresave;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.setting.TestTimeout;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.write.DumperHelper;
import org.rf.ide.core.testdata.text.write.tables.ANotExecutableTableElementDumper;

public class TestTimeoutDumper extends ANotExecutableTableElementDumper<SettingTable> {

    public TestTimeoutDumper(final DumperHelper helper) {
        super(helper, ModelType.SUITE_TEST_TIMEOUT);
    }

    @Override
    public RobotElementsComparatorWithPositionChangedPresave getSorter(
            final AModelElement<SettingTable> currentElement) {
        final TestTimeout testTimeout = (TestTimeout) currentElement;

        final RobotElementsComparatorWithPositionChangedPresave sorter = new RobotElementsComparatorWithPositionChangedPresave();
        final List<RobotToken> keys = new ArrayList<>();
        if (testTimeout.getTimeout() != null) {
            keys.add(testTimeout.getTimeout());
        }
        sorter.addPresaveSequenceForType(RobotTokenType.SETTING_TEST_TIMEOUT_VALUE, 1, keys);
        sorter.addPresaveSequenceForType(RobotTokenType.SETTING_TEST_TIMEOUT_MESSAGE, 2,
                testTimeout.getMessageArguments());
        sorter.addPresaveSequenceForType(RobotTokenType.COMMENT, 3,
                elemUtility.filter(testTimeout.getComment(), RobotTokenType.COMMENT));

        return sorter;
    }
}
