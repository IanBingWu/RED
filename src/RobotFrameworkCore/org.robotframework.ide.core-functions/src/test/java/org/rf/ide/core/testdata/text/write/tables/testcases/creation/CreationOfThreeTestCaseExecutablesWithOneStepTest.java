/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.testcases.creation;

import java.util.ArrayList;
import java.util.List;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.FileFormat;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.IExecutableStepsHolder;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.TestCaseTable;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.write.NewRobotFileTestHelper;
import org.rf.ide.core.testdata.text.write.tables.execution.creation.ACreationOfThreeExecUnitsTest;

public class CreationOfThreeTestCaseExecutablesWithOneStepTest extends ACreationOfThreeExecUnitsTest {

    private static final String ROBOT_VERSION = "3.0";

    @Override
    public List<IExecutableStepsHolder<? extends AModelElement<? extends ARobotSectionTable>>> getExecutablesAllWithNames() {
        final List<IExecutableStepsHolder<? extends AModelElement<? extends ARobotSectionTable>>> units = createModelWithOneTestCaseInside();
        for (int index = 0; index < 3; index++) {
            String name = "TestCase";
            if (index > 0) {
                name += (index + 1);
            }

            ((TestCase) units.get(index).getHolder()).getName().setText(name);
        }

        return units;
    }

    @Override
    public List<IExecutableStepsHolder<? extends AModelElement<? extends ARobotSectionTable>>> getExecutablesTheFirstWithoutName() {
        final List<IExecutableStepsHolder<? extends AModelElement<? extends ARobotSectionTable>>> units = createModelWithOneTestCaseInside();
        for (int index = 1; index < 3; index++) {
            String name = "TestCase";
            name += (index + 1);
            ((TestCase) units.get(index).getHolder()).getName().setText(name);
        }

        return units;
    }

    private List<IExecutableStepsHolder<? extends AModelElement<? extends ARobotSectionTable>>> createModelWithOneTestCaseInside() {
        final List<IExecutableStepsHolder<? extends AModelElement<? extends ARobotSectionTable>>> execUnits = new ArrayList<>(
                0);

        final RobotFile modelFile = NewRobotFileTestHelper.getModelFileToModify(ROBOT_VERSION);
        modelFile.includeTestCaseTableSection();
        final TestCaseTable keywordTable = modelFile.getTestCaseTable();

        for (int index = 0; index < 3; index++) {
            final RobotToken testName = new RobotToken();
            final TestCase execUnit = new TestCase(testName);
            execUnit.addElement(new RobotExecutableRow<TestCase>());
            keywordTable.addTest(execUnit);
            execUnits.add(execUnit);
        }

        return execUnits;
    }

    @Override
    public TestFilesCompareStore getCompareFilesStoreForExecutableWithName(final FileFormat format) {
        final TestFilesCompareStore store = new TestFilesCompareStore();

        store.setThreeExecUnitsWithOneLineEachOtherInsideCmpFile(
                convert("ExecActionAllCombinationsNoCommentLine", format));

        return store;
    }

    @Override
    public TestFilesCompareStore getCompareFilesStoreForExecutableWithTheFirstWithoutName(final FileFormat format) {
        final TestFilesCompareStore store = new TestFilesCompareStore();

        store.setThreeExecUnitsWithOneLineEachOtherInsideCmpFile(
                convert("ExecActionAllCombinationsNoCommentLineMissingTestName", format));

        return store;
    }

    private String convert(final String fileName, final FileFormat format) {
        return "testCases/exec/new/threeTcs/oneExecInEachTc/" + fileName + "." + format.getExtension();
    }
}
