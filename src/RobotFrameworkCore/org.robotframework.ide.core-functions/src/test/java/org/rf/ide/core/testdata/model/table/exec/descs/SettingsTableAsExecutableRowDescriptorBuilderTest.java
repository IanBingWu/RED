/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.exec.descs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor.RowType;
import org.rf.ide.core.testdata.model.table.setting.SuiteSetup;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public class SettingsTableAsExecutableRowDescriptorBuilderTest {

    @Test
    public void test_suiteSetup_withForToken_shouldReturn_simpleLine() {
        assertIsSimpleLineForSettingTable(":FOR");
    }

    @Test
    public void test_suiteSetup_withForContinueToken_shouldReturn_simpleLine() {
        assertIsSimpleLineForSettingTable("\\");
    }

    private void assertIsSimpleLineForSettingTable(final String actionToken) {
        // prepare
        final RobotFileOutput rfo = new RobotFileOutput(RobotVersion.from("3.0"));
        rfo.setProcessedFile(new File("fake.txt"));
        final RobotFile rf = new RobotFile(rfo);

        rf.includeSettingTableSection();
        final SuiteSetup s = rf.getSettingTable().newSuiteSetup();

        final RobotToken keywordAction = new RobotToken();
        keywordAction.setText(actionToken);
        keywordAction.setLineNumber(10);
        s.setKeywordName(keywordAction);
        s.addArgument("a");

        // execute
        final IExecutableRowDescriptor<SettingTable> lineDesc = s.asExecutableRow().buildLineDescription();

        // verify
        assertThat(lineDesc.getRowType()).isEqualTo(RowType.SIMPLE);
    }
}
