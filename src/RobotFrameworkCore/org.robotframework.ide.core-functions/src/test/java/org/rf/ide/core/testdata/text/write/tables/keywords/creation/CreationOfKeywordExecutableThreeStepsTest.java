/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.keywords.creation;

import org.rf.ide.core.testdata.model.FileFormat;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.IExecutableStepsHolder;
import org.rf.ide.core.testdata.model.table.KeywordTable;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.write.NewRobotFileTestHelper;
import org.rf.ide.core.testdata.text.write.tables.execution.creation.ACreationOfThreeExecutionRowsTest;

public class CreationOfKeywordExecutableThreeStepsTest extends ACreationOfThreeExecutionRowsTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public IExecutableStepsHolder getExecutableWithName() {
        final UserKeyword execUnit = createModelWithOneKeywordInside();
        execUnit.getName().setText("UserKeyword");

        return execUnit;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public IExecutableStepsHolder getExecutableWithoutName() {
        return createModelWithOneKeywordInside();
    }

    private UserKeyword createModelWithOneKeywordInside() {
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify("3.0");
        modelFile.includeKeywordTableSection();
        final KeywordTable keywordTable = modelFile.getKeywordTable();

        final RobotToken keywordName = new RobotToken();
        final UserKeyword execUnit = new UserKeyword(keywordName);
        execUnit.addElement(new RobotExecutableRow<UserKeyword>());
        execUnit.addElement(new RobotExecutableRow<UserKeyword>());
        execUnit.addElement(new RobotExecutableRow<UserKeyword>());
        keywordTable.addKeyword(execUnit);

        return execUnit;
    }

    @Override
    public TestFilesCompareStore getCompareFilesStoreForExecutableWithName(final FileFormat format) {
        final TestFilesCompareStore store = new TestFilesCompareStore();

        store.setThreeLinesWithoutCommentedLineCmpFile(convert("ExecActionAllCombinationsNoCommentLine", format));
        store.setThreeLinesWithCommentAndEmptyLineCmpFile(
                convert("ExecActionWith3ArgsCommentOneCommentedLineAndOneEmpty", format));
        store.setThreeLinesWithCommentTheFirstEmptyLineInTheMiddleCmpFile(
                convert("ExecActionEmptyLineInTheMiddleCommentInFirst", format));

        return store;
    }

    @Override
    public TestFilesCompareStore getCompareFilesStoreForExecutableWithoutName(final FileFormat format) {
        final TestFilesCompareStore store = new TestFilesCompareStore();

        store.setThreeLinesWithoutCommentedLineCmpFile(
                convert("ExecActionAllCombinationsNoCommentLineMissingKwName", format));
        store.setThreeLinesWithCommentAndEmptyLineCmpFile(
                convert("ExecActionWith3ArgsCommentOneCommentedLineAndOneEmptyWithoutKwName", format));
        store.setThreeLinesWithCommentTheFirstEmptyLineInTheMiddleCmpFile(
                convert("ExecActionEmptyLineInTheMiddleCommentInFirstWithoutKwName", format));

        return store;
    }

    private String convert(final String fileName, final FileFormat format) {
        return "keywords/exec/new/oneKw/threeExecs/" + fileName + "." + format.getExtension();
    }
}
