/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.recognizer.header;

import java.util.regex.Pattern;

import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.text.read.recognizer.ATokenRecognizer;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class MetadataTableHeaderRecognizer extends ATokenRecognizer {

    public static final Pattern EXPECTED = Pattern
            .compile("[ ]?([*][\\s]*)+[\\s]*" + createUpperLowerCaseWordWithSpacesInside("Metadata") + "([\\s]*[*])*");

    public MetadataTableHeaderRecognizer() {
        super(EXPECTED, RobotTokenType.SETTINGS_TABLE_HEADER);
    }

    @Override
    public boolean isApplicableFor(final RobotVersion robotVersion) {
        return robotVersion.isOlderThan(new RobotVersion(3, 1));
    }

    @Override
    public ATokenRecognizer newInstance() {
        return new MetadataTableHeaderRecognizer();
    }

    @Override
    public boolean shouldContinueWithOtherRecognizers() {
        return false;
    }
}
