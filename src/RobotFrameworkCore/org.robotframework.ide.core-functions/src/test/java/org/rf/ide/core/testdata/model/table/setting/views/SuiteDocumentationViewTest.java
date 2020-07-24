/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.setting.views;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.setting.SuiteDocumentation;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public class SuiteDocumentationViewTest {

    private static final String ROBOT_VERSION = "3.0";

    @Test
    public void test_twoSuiteDocDeclarations_shouldReturn_commonView() {
        // prepare
        final RobotFile robotFile = new RobotFile(new RobotFileOutput(RobotVersion.from(ROBOT_VERSION)));
        robotFile.includeSettingTableSection();
        final SettingTable settingTable = robotFile.getSettingTable();

        final RobotToken suiteDocOne = new RobotToken();
        final SuiteDocumentation docOne = new SuiteDocumentation(suiteDocOne);
        docOne.addDocumentationText("doc1");
        docOne.addDocumentationText("doc2");
        settingTable.addDocumentation(docOne);

        final RobotToken suiteDocTwo = new RobotToken();
        final SuiteDocumentation docTwo = new SuiteDocumentation(suiteDocTwo);
        docTwo.addDocumentationText("doc3");
        docTwo.addDocumentationText("doc4");
        settingTable.addDocumentation(docTwo);

        // execute
        final Optional<SuiteDocumentation> documentation = settingTable.documentation();

        // verify
        assertThat(documentation.isPresent());
        assertThat(getText(documentation.get().getDocumentationText())).containsExactly("doc1", "doc2", "doc3", "doc4");
        assertThat(settingTable.getDocumentation()).hasSize(2);
    }

    @Test
    public void test_twoSuiteDocumentations_addOneArgument_shouldReturn_singleSuiteDoc() {
        // prepare
        final RobotFile robotFile = new RobotFile(new RobotFileOutput(RobotVersion.from(ROBOT_VERSION)));
        robotFile.includeSettingTableSection();
        final SettingTable settingTable = robotFile.getSettingTable();

        final RobotToken suiteDocOne = new RobotToken();
        final SuiteDocumentation docOne = new SuiteDocumentation(suiteDocOne);
        docOne.addDocumentationText("doc1");
        docOne.addDocumentationText("doc2");
        settingTable.addDocumentation(docOne);

        final RobotToken suiteDocTwo = new RobotToken();
        final SuiteDocumentation docTwo = new SuiteDocumentation(suiteDocTwo);
        docTwo.addDocumentationText("doc3");
        docTwo.addDocumentationText("doc4");
        settingTable.addDocumentation(docTwo);

        // execute
        final Optional<SuiteDocumentation> documentation = settingTable.documentation();
        assertThat(documentation.isPresent());
        documentation.get().addDocumentationText("doc5");

        // verify
        assertThat(getText(documentation.get().getDocumentationText())).containsExactly("doc1", "doc2", "doc3", "doc4",
                "doc5");
        assertThat(settingTable.getDocumentation()).hasSize(1);
        assertThat(settingTable.getDocumentation().get(0)).isSameAs(documentation.get());
    }

    @Test
    public void test_twoSuiteDocumentations_modificationOfOneArgument_shouldReturn_twoSuiteDocsStill() {
        // prepare
        final RobotFile robotFile = new RobotFile(new RobotFileOutput(RobotVersion.from(ROBOT_VERSION)));
        robotFile.includeSettingTableSection();
        final SettingTable settingTable = robotFile.getSettingTable();

        final RobotToken suiteDocOne = new RobotToken();
        final SuiteDocumentation docOne = new SuiteDocumentation(suiteDocOne);
        docOne.addDocumentationText("doc1");
        docOne.addDocumentationText("doc2");
        settingTable.addDocumentation(docOne);

        final RobotToken suiteDocTwo = new RobotToken();
        final SuiteDocumentation docTwo = new SuiteDocumentation(suiteDocTwo);
        docTwo.addDocumentationText("doc3");
        docTwo.addDocumentationText("doc4");
        settingTable.addDocumentation(docTwo);
        docTwo.getDocumentationText().get(1).setText("doc_mod");

        // execute
        final Optional<SuiteDocumentation> documentation = settingTable.documentation();

        // verify
        assertThat(documentation.isPresent());
        assertThat(getText(documentation.get().getDocumentationText())).containsExactly("doc1", "doc2", "doc3",
                "doc_mod");
        assertThat(settingTable.getDocumentation()).hasSize(2);
    }

    private List<String> getText(final List<RobotToken> tokens) {
        final List<String> text = new ArrayList<>();
        for (final RobotToken tok : tokens) {
            text.add(tok.getText());
        }
        return text;
    }
}
