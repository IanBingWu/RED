/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.recognizer.settings;

import java.util.regex.Pattern;

import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.text.read.recognizer.ATokenRecognizer;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class SettingDocumentRecognizer extends ATokenRecognizer {

    public static final RobotTokenType TOKEN_TYPE = RobotTokenType.SETTING_DOCUMENTATION_DECLARATION;

    public static final Pattern EXPECTED = Pattern.compile("[ ]?(" + createUpperLowerCaseWord("Document") + "[\\s]*:"
            + "|" + createUpperLowerCaseWord("Document") + ")");

    public SettingDocumentRecognizer() {
        super(EXPECTED, RobotTokenType.SETTING_DOCUMENTATION_DECLARATION);
    }

    @Override
    public boolean isApplicableFor(final RobotVersion robotVersion) {
        return robotVersion.isOlderThan(new RobotVersion(3, 1));
    }

    @Override
    public ATokenRecognizer newInstance() {
        return new SettingDocumentRecognizer();
    }
}
