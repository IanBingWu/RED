/*
 * Copyright 2019 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.TemplateSetting;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class LocalSettingTest {

    @Test
    public void tokenCreationDoesNothing_whenInsertingOutOfTheSetting() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.createToken(-1);
        setting.createToken(6);
        setting.createToken(7);
        setting.createToken(10);

        assertThat(cellsOf(setting)).containsExactly("[Tags]", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenCreationIsNotPossible_whenInsertingAtSettingName() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        assertThatIllegalArgumentException().isThrownBy(() -> setting.createToken(0));
    }

    @Test
    public void tokenCreationInsertsEmptyToken_whenInsertingInSettingBody() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting1.createToken(1);
        setting2.createToken(2);

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "", "2", "# c1", "c2");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenCreationInsertsEmptyToken_whenInsertingAtFirstCommentToken() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.createToken(3);

        assertThat(cellsOf(setting)).containsExactly("[Tags]", "1", "2", "", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenCreationInsertsEmptyToken_whenInsertingAtNonFirstComment() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.createToken(4);

        assertThat(cellsOf(setting)).containsExactly("[Tags]", "1", "2", "# c1", "", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenDeletionIsNotPossible_whenRemovingSettingName() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args(), comment());
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args(), comment("c1", "c2"));

        assertThatIllegalArgumentException().isThrownBy(() -> setting1.deleteToken(0));
        assertThatIllegalArgumentException().isThrownBy(() -> setting2.deleteToken(0));
    }

    @Test
    public void tokenDeletionDoesNothing_whenRemovingOutOfTheSetting() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.deleteToken(-1);
        setting.deleteToken(5);
        setting.deleteToken(6);
        setting.deleteToken(10);

        assertThat(cellsOf(setting)).containsExactly("[Tags]", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenDeletionIsPerformed_whenRemovingInsideSettingBody() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting1.deleteToken(1);
        setting2.deleteToken(2);

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "2", "# c1", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "# c1", "c2");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenDeletionIsPerformed_whenRemovingFirstComment() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"),
                comment("c1", "c2", "# c3", "c4"));

        setting1.deleteToken(3);
        setting2.deleteToken(3);

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "1", "2", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "2", "c2", "# c3", "c4");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenDeletionIsPerformed_whenRemovingNonFirstComment() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"),
                comment("c1", "c2", "# c3", "c4"));

        setting1.deleteToken(4);
        setting2.deleteToken(5);

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "1", "2", "# c1");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "2", "# c1", "c2", "c4");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateDoesNothing_whenNegativeIndexIsGiven() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.updateToken(-1, "a");
        setting.updateToken(-5, "b");
        setting.updateToken(-10, "c");

        assertThat(cellsOf(setting)).containsExactly("[Tags]", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateIsNotPossible_whenUpdatingSettingNameToNonSetting() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        assertThatIllegalArgumentException().isThrownBy(() -> setting.updateToken(0, "action"));
    }

    @Test
    public void tokenUpdateChangesName_whenUpdatingSettingNameToSameSetting() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.updateToken(0, "[tags]");

        assertThat(cellsOf(setting)).containsExactly("[tags]", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateChangesNameAndType_whenUpdatingSettingNameToDifferentSetting() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.updateToken(0, "[Setup]");

        assertThat(setting.getModelType()).isEqualTo(ModelType.TEST_CASE_SETUP);
        assertThat(cellsOf(setting)).containsExactly("[Setup]", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_SETUP,
                RobotTokenType.TEST_CASE_SETTING_SETUP_KEYWORD_NAME,
                RobotTokenType.TEST_CASE_SETTING_SETUP_KEYWORD_ARGUMENT, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateChangesNameAndType_whenUpdatingSettingNameToUnrecognizedSetting() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting.updateToken(0, "[unknown]");

        assertThat(setting.getModelType()).isEqualTo(ModelType.TEST_CASE_SETTING_UNKNOWN);
        assertThat(cellsOf(setting)).containsExactly("[unknown]", "1", "2", "# c1", "c2");
        assertThat(typesOf(setting)).containsExactly(RobotTokenType.TEST_CASE_SETTING_UNKNOWN_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_UNKNOWN_ARGUMENTS, RobotTokenType.TEST_CASE_SETTING_UNKNOWN_ARGUMENTS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateChangesElement_whenUpdatingInSettingBody() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting1.updateToken(1, "11");
        setting2.updateToken(2, "22");

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "11", "2", "# c1", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "22", "# c1", "c2");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateChangesElement_whenUpdatingInSettingBodyWithCommentedValue() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting1.updateToken(1, "# comment");
        setting2.updateToken(2, "# comment");

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "# comment", "2", "# c1", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "# comment", "# c1", "c2");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateChangesComment_whenUpdatingInsideComments() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));

        setting1.updateToken(3, "# d1");
        setting2.updateToken(4, "d2");

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "1", "2", "# d1", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "2", "# c1", "d2");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateChangesCommentIntoArgument_whenUpdatingInsideCommentsWithUncommentedValue() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment("c1", "c2"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"),
                comment("c1", "# c2", "c3"));

        setting1.updateToken(3, "d1");
        setting2.updateToken(3, "d1");

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "1", "2", "d1", "c2");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "2", "d1", "# c2", "c3");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateFillsMissingArguments_whenUpdatingSettingWithoutCommentsAfterEndOfTokens() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1", "2"), comment());
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1", "2"), comment());
        final LocalSetting<TestCase> setting3 = createSetting(test, "[Tags]", args("1", "2"), comment());

        setting1.updateToken(3, "3");
        setting2.updateToken(4, "4");
        setting3.updateToken(5, "# 5");

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "1", "2", "3");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "2", "\\", "4");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS);

        assertThat(cellsOf(setting3)).containsExactly("[Tags]", "1", "2", "\\", "\\", "# 5");
        assertThat(typesOf(setting3)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.TEST_CASE_SETTING_TAGS,
                RobotTokenType.COMMENT);
    }

    @Test
    public void tokenUpdateFillsMissingComments_whenUpdatingSettingAfterEndOfComments() {
        final TestCase test = new TestCase(RobotToken.create("test"));
        final LocalSetting<TestCase> setting1 = createSetting(test, "[Tags]", args("1"), comment("c1"));
        final LocalSetting<TestCase> setting2 = createSetting(test, "[Tags]", args("1"), comment("c1"));
        final LocalSetting<TestCase> setting3 = createSetting(test, "[Tags]", args("1"), comment("c1"));

        setting1.updateToken(3, "3");
        setting2.updateToken(4, "4");
        setting3.updateToken(5, "# 5");

        assertThat(cellsOf(setting1)).containsExactly("[Tags]", "1", "# c1", "3");
        assertThat(typesOf(setting1)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);

        assertThat(cellsOf(setting2)).containsExactly("[Tags]", "1", "# c1", "\\", "4");
        assertThat(typesOf(setting2)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT);

        assertThat(cellsOf(setting3)).containsExactly("[Tags]", "1", "# c1", "\\", "\\", "# 5");
        assertThat(typesOf(setting3)).containsExactly(RobotTokenType.TEST_CASE_SETTING_TAGS_DECLARATION,
                RobotTokenType.TEST_CASE_SETTING_TAGS, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT, RobotTokenType.COMMENT,
                RobotTokenType.COMMENT);
    }

    @Test
    public void templateAdapterIsNotReturnedForNonTemplateSetting() {
        assertThat(createSetting(null, "[Setup]", newArrayList("1", "2"), newArrayList("c", "d"))
                .adaptTo(TemplateSetting.class)).isNull();
        assertThat(createSetting(null, "[Teardown]", newArrayList("1", "2"), newArrayList("c", "d"))
                .adaptTo(TemplateSetting.class)).isNull();
        assertThat(createSetting(null, "[Documentation]", newArrayList("1", "2"), newArrayList("c", "d"))
                .adaptTo(TemplateSetting.class)).isNull();
    }

    @Test
    public void templateAdapterIsProperlyConstructed() {
        final LocalSetting<TestCase> template = createSetting(null, "[Template]", newArrayList("1", "2"),
                newArrayList("c", "d"));

        final TemplateSetting templateSetting = template.adaptTo(TemplateSetting.class);

        assertThat(templateSetting).isNotNull();
        assertThat(templateSetting.getDeclaration()).isSameAs(template.getDeclaration());
        assertThat(templateSetting.getDeclaration().getText()).isEqualTo("[Template]");
        assertThat(templateSetting.getKeywordName()).isSameAs(template.getTokens().get(1));
        assertThat(templateSetting.getKeywordName().getText()).isEqualTo("1");
        assertThat(templateSetting.getUnexpectedArguments()).hasSize(1);
        assertThat(templateSetting.getUnexpectedArguments().get(0)).isSameAs(template.getTokens().get(2));
        assertThat(templateSetting.getUnexpectedArguments().get(0).getText()).isEqualTo("2");
    }

    private static List<String> args(final String... arguments) {
        return newArrayList(arguments);
    }

    private static List<String> comment(final String... comments) {
        final List<String> cmts = newArrayList(comments);
        if (!cmts.isEmpty()) {
            cmts.set(0, "# " + cmts.get(0));
        }
        return cmts;
    }

    private static LocalSetting<TestCase> createSetting(final TestCase test, final String settingName,
            final List<String> args, final List<String> cmts) {
        final ModelType modelType = LocalSettingTokenTypes.getModelTypeFromDeclarationType(
                RobotTokenType.findTypeOfDeclarationForTestCaseSettingTable(settingName));
        final LocalSetting<TestCase> setting = new LocalSetting<>(modelType, RobotToken.create(settingName));
        setting.setParent(test);
        args.stream().map(RobotToken::create).forEach(setting::addToken);
        cmts.stream().map(RobotToken::create).forEach(setting::addCommentPart);
        return setting;
    }

    private static List<String> cellsOf(final LocalSetting<?> setting) {
        return setting.getElementTokens().stream().map(RobotToken::getText).collect(toList());
    }

    private static List<IRobotTokenType> typesOf(final LocalSetting<?> setting) {
        return setting.getElementTokens().stream().map(token -> token.getTypes().get(0)).collect(toList());
    }
}
