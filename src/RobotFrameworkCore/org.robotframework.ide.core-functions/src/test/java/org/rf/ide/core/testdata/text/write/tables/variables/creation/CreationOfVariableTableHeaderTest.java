/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.variables.creation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rf.ide.core.testdata.model.FileFormat;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.TableHeader;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.write.NewRobotFileTestHelper;

public class CreationOfVariableTableHeaderTest {

    private static final String ROBOT_VERSION = "3.0";

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateVariableHeaderOnly(final FileFormat format) throws Exception {
        // prepare
        final String fileName = convert("VariablesHeaderOnly", format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeVariableTableSection();

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(fileName, modelFile);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateVariableHeader_withTwoNamedColumns(final FileFormat format)
            throws Exception {
        // prepare
        final String fileName = convert("VariablesHeaderWithColumns", format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeVariableTableSection();
        final TableHeader<? extends ARobotSectionTable> tableHeader = modelFile.getVariableTable().getHeaders().get(0);
        final RobotToken columnOne = new RobotToken();
        columnOne.setText("*** col1 ***");
        tableHeader.addColumnName(columnOne);
        final RobotToken columnTwo = new RobotToken();
        columnTwo.setText("*** col2 ***");
        tableHeader.addColumnName(columnTwo);

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(fileName, modelFile);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateVariableHeader_withTwoCommentTokens(final FileFormat format)
            throws Exception {
        // prepare
        final String fileName = convert("VariablesHeaderWithComments", format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeVariableTableSection();
        final TableHeader<? extends ARobotSectionTable> tableHeader = modelFile.getVariableTable().getHeaders().get(0);
        final RobotToken commentOne = new RobotToken();
        commentOne.setText("comment");
        tableHeader.addCommentPart(commentOne);
        final RobotToken commentTwo = new RobotToken();
        commentTwo.setText("comment2");
        tableHeader.addCommentPart(commentTwo);

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(fileName, modelFile);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateVariableHeader_withTwoNamedColumns_and_withTwoCommentTokens(
            final FileFormat format) throws Exception {
        // prepare
        final String fileName = convert("VariablesHeaderWithColumnsAndComments", format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeVariableTableSection();
        final TableHeader<? extends ARobotSectionTable> tableHeader = modelFile.getVariableTable().getHeaders().get(0);

        final RobotToken columnOne = new RobotToken();
        columnOne.setText("*** col1 ***");
        tableHeader.addColumnName(columnOne);

        final RobotToken commentOne = new RobotToken();
        commentOne.setText("comment");
        tableHeader.addCommentPart(commentOne);

        final RobotToken columnTwo = new RobotToken();
        columnTwo.setText("*** col2 ***");
        tableHeader.addColumnName(columnTwo);

        final RobotToken commentTwo = new RobotToken();
        commentTwo.setText("comment2");
        tableHeader.addCommentPart(commentTwo);

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(fileName, modelFile);
    }

    private String convert(final String fileName, final FileFormat format) {
        return "variables/header/new/" + fileName + "." + format.getExtension();
    }
}
