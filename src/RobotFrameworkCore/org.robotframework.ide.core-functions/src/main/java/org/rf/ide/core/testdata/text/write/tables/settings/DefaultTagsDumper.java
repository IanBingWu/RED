/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write.tables.settings;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.table.RobotElementsComparatorWithPositionChangedPresave;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.setting.DefaultTags;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.write.DumperHelper;
import org.rf.ide.core.testdata.text.write.tables.ANotExecutableTableElementDumper;

public class DefaultTagsDumper extends ANotExecutableTableElementDumper<SettingTable> {

    public DefaultTagsDumper(final DumperHelper helper) {
        super(helper, ModelType.DEFAULT_TAGS_SETTING);
    }

    @Override
    public RobotElementsComparatorWithPositionChangedPresave getSorter(
            final AModelElement<SettingTable> currentElement) {
        final DefaultTags defaultTags = (DefaultTags) currentElement;
        final RobotElementsComparatorWithPositionChangedPresave sorter = new RobotElementsComparatorWithPositionChangedPresave();
        sorter.addPresaveSequenceForType(RobotTokenType.SETTING_DEFAULT_TAG, 1, defaultTags.getTags());
        sorter.addPresaveSequenceForType(RobotTokenType.COMMENT, 2,
                elemUtility.filter(defaultTags.getComment(), RobotTokenType.COMMENT));

        return sorter;
    }

}
