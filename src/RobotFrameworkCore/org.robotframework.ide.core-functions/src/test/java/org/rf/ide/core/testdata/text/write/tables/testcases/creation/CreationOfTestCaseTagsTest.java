/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.testcases.creation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rf.ide.core.testdata.model.FileFormat;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.TestCaseTable;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.write.NewRobotFileTestHelper;

public class CreationOfTestCaseTagsTest {

    private static final String ROBOT_VERSION = "3.0";

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withoutTestName_andTagsDecOnly(final FileFormat format)
            throws Exception {
        test_tagsDecOnly("EmptyTestCaseTagsNoTestName", "", format);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withTestName_andTagsDecOnly(final FileFormat format)
            throws Exception {
        test_tagsDecOnly("EmptyTestCaseTags", "TestCase", format);
    }

    private void test_tagsDecOnly(final String fileNameWithoutExt, final String userTestName, final FileFormat format)
            throws Exception {
        // prepare
        final String filePath = convert(fileNameWithoutExt, format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeTestCaseTableSection();
        final TestCaseTable testCaseTable = modelFile.getTestCaseTable();

        final RobotToken testName = new RobotToken();
        testName.setText(userTestName);
        final TestCase test = new TestCase(testName);
        testCaseTable.addTest(test);
        test.newTags(0);

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(filePath, modelFile);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withoutTestName_andTags_andComment(final FileFormat format)
            throws Exception {
        test_tagsDec_andComment("EmptyTestCaseTagsCommentNoTestName", "", format);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withTestName_andTags_andComment(final FileFormat format)
            throws Exception {
        test_tagsDec_andComment("EmptyTestCaseTagsComment", "TestCase", format);
    }

    private void test_tagsDec_andComment(final String fileNameWithoutExt, final String userTestName,
            final FileFormat format) throws Exception {
        // prepare
        final String filePath = convert(fileNameWithoutExt, format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeTestCaseTableSection();
        final TestCaseTable testCaseTable = modelFile.getTestCaseTable();

        final RobotToken testName = new RobotToken();
        testName.setText(userTestName);
        final TestCase test = new TestCase(testName);
        testCaseTable.addTest(test);
        final LocalSetting<TestCase> testTags = test.newTags(0);

        final RobotToken cmTok1 = new RobotToken();
        cmTok1.setText("cm1");
        final RobotToken cmTok2 = new RobotToken();
        cmTok2.setText("cm2");
        final RobotToken cmTok3 = new RobotToken();
        cmTok3.setText("cm3");

        testTags.addCommentPart(cmTok1);
        testTags.addCommentPart(cmTok2);
        testTags.addCommentPart(cmTok3);

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(filePath, modelFile);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withoutTestName_andTags_and3Tags(final FileFormat format)
            throws Exception {
        test_tags_withTagsAnd3Tags("TestCaseTagsAnd3TagsNoTestName", "", format);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withTestName_andTags_and3Tags(final FileFormat format)
            throws Exception {
        test_tags_withTagsAnd3Tags("TestCaseTagsAnd3Tags", "TestCase", format);
    }

    private void test_tags_withTagsAnd3Tags(final String fileNameWithoutExt, final String userTestName,
            final FileFormat format) throws Exception {
        // prepare
        final String filePath = convert(fileNameWithoutExt, format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeTestCaseTableSection();
        final TestCaseTable testCaseTable = modelFile.getTestCaseTable();

        final RobotToken testName = new RobotToken();
        testName.setText(userTestName);
        final TestCase test = new TestCase(testName);
        testCaseTable.addTest(test);
        final LocalSetting<TestCase> testTags = test.newTags(0);

        testTags.addToken("tag1");
        testTags.addToken("tag2");
        testTags.addToken("tag3");

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(filePath, modelFile);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withoutTestName_andTags_and3Tags_andComment(
            final FileFormat format) throws Exception {
        test_tags_with3Tags_andComment("TestCaseTagsAnd3TagsCommentNoTestName", "", format);
    }

    @ParameterizedTest
    @EnumSource(value = FileFormat.class, names = { "TXT_OR_ROBOT", "TSV" })
    public void test_emptyFile_and_thanCreateTestCaseTags_withTestName_andTags_and3Tags_andComment(
            final FileFormat format) throws Exception {
        test_tags_with3Tags_andComment("TestCaseTagsAnd3TagsComment", "TestCase", format);
    }

    private void test_tags_with3Tags_andComment(final String fileNameWithoutExt, final String userTestName,
            final FileFormat format) throws Exception {
        // prepare
        final String filePath = convert(fileNameWithoutExt, format);
        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);

        // test data prepare
        modelFile.includeTestCaseTableSection();
        final TestCaseTable testCaseTable = modelFile.getTestCaseTable();

        final RobotToken testName = new RobotToken();
        testName.setText(userTestName);
        final TestCase test = new TestCase(testName);
        testCaseTable.addTest(test);
        final LocalSetting<TestCase> testTags = test.newTags(0);

        testTags.addToken("tag1");
        testTags.addToken("tag2");
        testTags.addToken("tag3");

        testTags.addCommentPart("cm1");
        testTags.addCommentPart("cm2");
        testTags.addCommentPart("cm3");

        // verify
        NewRobotFileTestHelper.assertNewModelTheSameAsInFile(filePath, modelFile);
    }

    private String convert(final String fileName, final FileFormat format) {
        return "testCases/setting/tags/new/" + fileName + "." + format.getExtension();
    }
}
