/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.recognizer.header;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.test.helpers.CombinationGenerator;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class KeywordsTableHeaderRecognizerTest {

    private final KeywordsTableHeaderRecognizer rec = new KeywordsTableHeaderRecognizer();

    @Test
    public void test_check_KeywordsAllPossibilities_withAsterisks_atTheBeginAndEnd() {
        assertAllCombinations("Keywords");
    }

    @Test
    public void test_check_KeywordAllPossibilities_withAsterisks_atTheBeginAndEnd() {
        assertAllCombinations("Keyword");
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
    public void test_check_Keywords_withAsterisk_atTheBeginAndEnd_spaceLetterT() {
        final String expectedToCut = " *  Keywords *";
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
    public void test_check_commented_Keywords_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("# *  Keywords ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_check_spaceKeywords_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder(" *  Keywords ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keywords_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("*  Keywords ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keywords_withAsterisks_atTheBeginAndEnd_spaceLetterT() {
        final String expectedToCut = " ***  Keywords ***";
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
    public void test_check_commented_Keywords_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("# ***  Keywords ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_check_spaceKeywords_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder(" ***  Keywords ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keywords_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("***  Keywords ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keyword_withAsterisk_atTheBeginAndEnd_spaceLetterT() {
        final String expectedToCut = " * Keyword *";
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
    public void test_check_commented_Keyword_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("# * Keyword ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_check_spaceKeyword_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder(" * Keyword ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keyword_withAsterisk_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("* Keyword ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keyword_withAsterisks_atTheBeginAndEnd_spaceLetterT() {
        final String expectedToCut = " *** Keyword ***";
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
    public void test_check_commented_Keyword_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("# *** Keyword ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_check_spaceKeyword_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder(" *** Keyword ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keyword_withAsterisks_atTheBeginAndEnd() {
        final StringBuilder text = new StringBuilder("*** Keyword ***");

        assertThat(rec.hasNext(text, 1, 0)).isTrue();
        final RobotToken token = rec.next();
        assertThat(token.getStartColumn()).isEqualTo(0);
        assertThat(token.getLineNumber()).isEqualTo(1);
        assertThat(token.getEndColumn()).isEqualTo(text.length());
        assertThat(token.getText().toString()).isEqualTo(text.toString());
        assertThat(token.getTypes()).containsExactly(rec.getProducedType());
    }

    @Test
    public void test_check_Keyword_withSpacesInside() {
        final StringBuilder text = new StringBuilder("*** K e y w o r d ***");

        assertThat(rec.hasNext(text, 1, 0)).isFalse();
    }

    @Test
    public void test_getPattern() {
        assertThat(rec.getPattern().pattern()).isNotEmpty();
    }

    @Test
    public void test_getProducedType() {
        assertThat(rec.getProducedType()).isEqualTo(RobotTokenType.KEYWORDS_TABLE_HEADER);
    }
}