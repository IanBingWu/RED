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
import org.rf.ide.core.testdata.model.table.variables.ScalarVariable;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.write.DumperHelper;
import org.rf.ide.core.testdata.text.write.tables.ANotExecutableTableElementDumper;

public class ScalarVariableDumper extends ANotExecutableTableElementDumper<VariableTable> {

    public ScalarVariableDumper(final DumperHelper helper) {
        super(helper, ModelType.SCALAR_VARIABLE_DECLARATION_IN_TABLE);
    }

    @Override
    public RobotElementsComparatorWithPositionChangedPresave getSorter(
            final AModelElement<VariableTable> currentElement) {
        final ScalarVariable var = (ScalarVariable) currentElement;

        final RobotElementsComparatorWithPositionChangedPresave sorter = new RobotElementsComparatorWithPositionChangedPresave();
        sorter.addPresaveSequenceForType(RobotTokenType.VARIABLES_VARIABLE_VALUE, 1, var.getValues());
        sorter.addPresaveSequenceForType(RobotTokenType.COMMENT, 2,
                elemUtility.filter(var.getComment(), RobotTokenType.COMMENT));

        return sorter;
    }
}
