/*
 * Copyright 2018 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.recognizer.header;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.test.helpers.CombinationGenerator;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class MetadataTableHeaderRecognizerTest {

    private final MetadataTableHeaderRecognizer rec = new MetadataTableHeaderRecognizer();

    @Test
    public void recognizerIsApplicable_whenRobotVersionIsOlderThan31() throws Exception {
        assertThat(rec.isApplicableFor(new RobotVersion(3, 0))).isTrue();
        assertThat(rec.isApplicableFor(new RobotVersion(3, 0, 9))).isTrue();
        assertThat(rec.isApplicableFor(new RobotVersion(3, 1))).isFalse();
        assertThat(rec.isApplicableFor(new RobotVersion(3, 1, 5))).isFalse();
        assertThat(rec.isApplicableFor(new RobotVersion(3, 2))).isFalse();
    }

    @Test
    public void test_check_MetadataAllPossibilities_withAsterisks_atTheBeginAndEnd() {
        assertAllCombinations("Metadata");
    }

    private void assertAllCombinations(final String text) {
        final List<String> combinations = new CombinationGenerator().combinations(text);

        for (final String comb : combinations) {
            final StringBuilder textOfHeader = new StringBuilder("*** ").append(comb).append(" ***");

            assertThat(rec.hasNext(textOfHeader, 1, 0)).isTrue();
            final RobotToken token = rec.next();
            assertThat(token.getStartColumn()).isEqualTo(0);
            assertThat(token.getLineNumber()).isEqualTo(1);
            assertThat(token.getEndColumn()).isEqualTo(textOfHeader.length());
            assertThat(token.getText().toString()).isEqualTo(textOfHeader.toString());
            assertThat(token.getTypes()).containsExactly(rec.getProducedType());
        }
    }

    @Test
    public void test_check_Metadata_withAsterisk_atTheBeginAndEnd_spaceLetterT() {
        final String expectedToCut = " * Metadata *";
        final StringBuilder text = new StringBuilder(expectedToCut).append(" T");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(expectedToCut.length());
        assertThat(token.getText().toString()).isEqualTo(expectedToCut);
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_commented_Metadata_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("# * Metadata ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_check_spaceMetadata_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder(" * Metadata ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Metadata_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("* Metadata ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Metadata_withAsterisks_atTheBeginAndEnd_spaceLetterT() {
        final String expectedToCut = " *** Metadata ***";
        final StringBuilder text = new StringBuilder(expectedToCut).append(" T");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(expectedToCut.length());
        assertThat(token.getText().toString()).isEqualTo(expectedToCut);
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_commented_Metadata_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("# *** Metadata ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_check_spaceMetadata_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder(" *** Metadata ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Metadata_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("*** Metadata ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_getPattern() {
        assertThat(rec.getPattern().pattern()).isNotEmpty();
    }

    @Test
    public void test_getProducedType() {
        assertThat(rec.getProducedType()).isEqualTo(RobotTokenType.SETTINGS_TABLE_HEADER);
    }
}
