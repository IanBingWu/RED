/*
* Copyright 2016 Nokia Solutions and Networks
* Licensed under the Apache License, Version 2.0,
* see license.txt file for details.
*/
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.colouring;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.rules.Token;
import org.junit.jupiter.api.Test;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.read.separators.Separator;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.colouring.ISyntaxColouringRule.PositionedTextToken;

public class SettingsTemplateRuleTest {

    private final SettingsTemplateRule testedRule = new SettingsTemplateRule(new Token("token"),
            new Token("var_token"), () -> new RobotVersion(3, 1));

    @Test
    public void ruleIsApplicableOnlyForRobotTokens() {
        assertThat(testedRule.isApplicable(new RobotToken())).isTrue();
        assertThat(testedRule.isApplicable(new Separator())).isFalse();
        assertThat(testedRule.isApplicable(mock(IRobotLineElement.class))).isFalse();
    }

    @Test
    public void generalSettingKeywordCallIsRecognized() {
        boolean thereWasName = false;
        for (final RobotToken token : createTokens(TokensSource::createTokensOfTemplatedCases)) {
            final Optional<PositionedTextToken> evaluatedToken = evaluate(token);

            if (token.getText().startsWith("general_setting_template")
                    || token.getText().startsWith("tc_setting_template")) {
                thereWasName = true;

                assertThat(evaluatedToken).isPresent();
                assertThat(evaluatedToken.get().getPosition())
                        .isEqualTo(new Position(token.getStartOffset(), token.getText().length()));
                assertThat(evaluatedToken.get().getToken().getData()).isEqualTo("token");

            } else {
                assertThat(evaluatedToken).isNotPresent();
            }
        }
        assertThat(thereWasName).isTrue();
    }

    @Test
    public void generalSettingKeywordCallIsRecognized_evenWhenPositionIsInsideToken() {
        boolean thereWasName = false;
        for (final RobotToken token : createTokens(TokensSource::createTokensOfTemplatedCases)) {
            final int positionInsideToken = new Random().nextInt(token.getText().length());
            final Optional<PositionedTextToken> evaluatedToken = evaluate(token, positionInsideToken);

            if (token.getText().startsWith("general_setting_template")
                    || token.getText().startsWith("tc_setting_template")) {
                thereWasName = true;

                assertThat(evaluatedToken).isPresent();
                assertThat(evaluatedToken.get().getPosition())
                        .isEqualTo(new Position(token.getStartOffset(), token.getText().length()));
                assertThat(evaluatedToken.get().getToken().getData()).isEqualTo("token");

            } else {
                assertThat(evaluatedToken).isNotPresent();
            }
        }
        assertThat(thereWasName).isTrue();
    }

    @Test
    public void variableTokenIsDetected_whenPositionedInsideVariable() {
        final String var1 = "${var1}";
        final String var2 = "${var2}";
        final String var3 = "${var3}";

        final String content = "abc" + var1 + "def" + var2 + "ghi" + var3 + "jkl";

        final List<Position> varPositions = new ArrayList<>();
        varPositions.add(new Position(content.indexOf(var1), var1.length()));
        varPositions.add(new Position(content.indexOf(var2), var2.length()));
        varPositions.add(new Position(content.indexOf(var3), var3.length()));

        final RobotToken token = createToken(content);

        for (final Position position : varPositions) {
            for (int offset = 0; offset < position.getLength(); offset++) {
                final Optional<PositionedTextToken> evaluatedToken = evaluate(token, position.getOffset() + offset);
                assertThat(evaluatedToken).isPresent();
                assertThat(evaluatedToken.get().getPosition())
                        .isEqualTo(new Position(position.getOffset() + offset, position.getLength() - offset));
                assertThat(evaluatedToken.get().getToken().getData()).isEqualTo("var_token");
            }
        }
    }

    @Test
    public void templateTokenIsDetected_whenPositionedOutsideVariables() {
        final String text1 = "abc";
        final String text2 = "def";
        final String text3 = "ghi";
        final String text4 = "jkl";
        final String content = text1 + "${var1}" + text2 + "${var2}" + text3 + "${var3}" + text4;

        final List<Position> nonVarPositions = new ArrayList<>();
        nonVarPositions.add(new Position(content.indexOf(text1), text1.length()));
        nonVarPositions.add(new Position(content.indexOf(text2), text2.length()));
        nonVarPositions.add(new Position(content.indexOf(text3), text3.length()));
        nonVarPositions.add(new Position(content.indexOf(text4), text4.length()));

        final RobotToken token = createToken(content);

        for (final Position position : nonVarPositions) {
            for (int offset = 0; offset < position.getLength(); offset++) {
                final Optional<PositionedTextToken> evaluatedToken = evaluate(token, position.getOffset() + offset);
                assertThat(evaluatedToken).isPresent();
                assertThat(evaluatedToken.get().getPosition())
                        .isEqualTo(new Position(position.getOffset() + offset, position.getLength() - offset));
                assertThat(evaluatedToken.get().getToken().getData()).isEqualTo("token");
            }
        }
    }

    @Test
    public void templateSettingsArgsAreNotRecognizeIfNoneIsUsedAsKeywordName() {
        final List<PositionedTextToken> coloredTokens = createTokens(TokensSource::createTokensOfNoneAwareSettings)
                .stream()
                .map(this::evaluate)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());

        assertThat(coloredTokens).isEmpty();
    }

    private Optional<PositionedTextToken> evaluate(final RobotToken token) {
        return evaluate(token, 0);
    }

    private Optional<PositionedTextToken> evaluate(final RobotToken token, final int position) {
        return testedRule.evaluate(token, position, new ArrayList<>());
    }

    private static List<RobotToken> createTokens(final Supplier<List<RobotLine>> linesSupplier) {
        final List<RobotLine> lines = linesSupplier.get();
        return lines.stream()
                .flatMap(line -> line.getLineElements().stream())
                .filter(RobotToken.class::isInstance)
                .map(RobotToken.class::cast)
                .collect(toList());
    }

    private RobotToken createToken(final String kwName) {
        final RobotToken token = RobotToken.create(kwName,
                newArrayList(RobotTokenType.TEST_CASE_SETTING_TEMPLATE_KEYWORD_NAME, RobotTokenType.VARIABLE_USAGE));
        token.setLineNumber(1);
        token.setStartColumn(0);
        token.setStartOffset(0);
        return token;
    }

}
