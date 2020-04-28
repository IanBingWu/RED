/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.variables;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.table.RobotElementsComparatorWithPositionChangedPresave;
import org.rf.ide.core.testdata.model.table.VariableTable;
import org.rf.ide.core.testdata.model.table.variables.UnknownVariable;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.write.DumperHelper;
import org.rf.ide.core.testdata.text.write.tables.ANotExecutableTableElementDumper;

public class UnknownVariableDumper extends ANotExecutableTableElementDumper<VariableTable> {

    public UnknownVariableDumper(final DumperHelper helper) {
        super(helper, ModelType.UNKNOWN_VARIABLE_DECLARATION_IN_TABLE);
    }

    @Override
    public RobotElementsComparatorWithPositionChangedPresave getSorter(
            final AModelElement<VariableTable> currentElement) {
        final UnknownVariable var = (UnknownVariable) currentElement;

        final RobotElementsComparatorWithPositionChangedPresave sorter = new RobotElementsComparatorWithPositionChangedPresave();
        sorter.addPresaveSequenceForType(RobotTokenType.VARIABLES_VARIABLE_VALUE, 1, var.getItems());
        sorter.addPresaveSequenceForType(RobotTokenType.COMMENT, 2,
                elemUtility.filter(var.getComment(), RobotTokenType.COMMENT));

        return sorter;
    }
}
