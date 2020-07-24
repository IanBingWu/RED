/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.rf.ide.core.environment.RobotRuntimeEnvironment;
import org.rf.ide.core.environment.RobotVersion;
import org.rf.ide.core.execution.context.RobotModelTestProvider;
import org.rf.ide.core.testdata.RobotParser;
import org.rf.ide.core.testdata.model.table.KeywordTable;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.TestCaseTable;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.setting.SuiteDocumentation;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;

/**
 * @author wypych
 */
public class DocumentationCacherInsideRobotFileOutputTest {

    private static RobotFileOutput out;

    private static SettingTable settingTable;

    private static KeywordTable keywordTable;

    private static TestCaseTable testCaseTable;

    @BeforeAll
    public static void setup() throws Exception {
        final Path path = getFile("presenter//DocPositionsFind.robot");
        final RobotParser parser = new RobotParser(new RobotProjectHolder(new RobotRuntimeEnvironment(null, "3.0.0")),
                new RobotVersion(3, 0));
        final RobotFile modelFile = RobotModelTestProvider.getModelFile(path, parser);
        out = modelFile.getParent();
        assertThat(out).isNotNull();

        settingTable = modelFile.getSettingTable();
        keywordTable = modelFile.getKeywordTable();
        testCaseTable = modelFile.getTestCaseTable();

        assertThat(settingTable.isPresent());
        assertThat(keywordTable.isPresent());
        assertThat(testCaseTable.isPresent());
    }

    private static Path getFile(final String path) throws URISyntaxException {
        final URL resource = DocumentationCacherInsideRobotFileOutputTest.class.getResource(path);
        return Paths.get(resource.toURI());
    }

    @Test
    public void test_checkForOffsetLessThanFileLength() {
        // given
        final long offset = -1;

        // when
        final Optional<IDocumentationHolder> docFound = out.findDocumentationForOffset((int) offset);

        // then
        assertThat(docFound).isNotPresent();
    }

    @Test
    public void test_checkForOffsetOutsideFileLength() {
        // given
        final long offset = out.getProcessedFile().length() + 10;

        // when
        final Optional<IDocumentationHolder> docFound = out.findDocumentationForOffset((int) offset);

        // then
        assertThat(docFound).isNotPresent();
    }

    @Test
    public void test_assertCacherContainsOnlyWhatIsExpected() {
        // given
        final SuiteDocumentation suiteDocumentation = settingTable.getDocumentation().get(0);

        final LocalSetting<UserKeyword> keyKw1Documentation = keywordTable.getKeywords()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<UserKeyword> keyKw2Documentation = keywordTable.getKeywords()
                .get(1)
                .getDocumentation()
                .get(0);
        final LocalSetting<TestCase> testTc1Documentation = testCaseTable.getTestCases()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<TestCase> testTc2Documentation = testCaseTable.getTestCases()
                .get(1)
                .getDocumentation()
                .get(0);

        // when
        final Set<IRegionCacheable<IDocumentationHolder>> cache = out.getDocumentationCacher()
                .getUnmodificableCacheContent();

        // then
        assertThat(cache).hasSameElementsAs(
                Arrays.asList(suiteDocumentation, keyKw1Documentation.adaptTo(IDocumentationHolder.class),
                        keyKw2Documentation.adaptTo(IDocumentationHolder.class),
                        testTc1Documentation.adaptTo(IDocumentationHolder.class),
                        testTc2Documentation.adaptTo(IDocumentationHolder.class)));
    }

    @Test
    public void test_givenTestCaseTable_and_positionAtMiddleOfDocumentation_shouldReturn_documentation() {
        // given
        final LocalSetting<TestCase> testTc1Documentation = testCaseTable.getTestCases()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<TestCase> testTc2Documentation = testCaseTable.getTestCases()
                .get(1)
                .getDocumentation()
                .get(0);
        final IDocumentationHolder adapter1 = testTc1Documentation.adaptTo(IDocumentationHolder.class);
        final IDocumentationHolder adapter2 = testTc2Documentation.adaptTo(IDocumentationHolder.class);

        // when

        final Optional<IDocumentationHolder> docTc1Found = out.findDocumentationForOffset(middleOffset(adapter1));
        final Optional<IDocumentationHolder> docTc2Found = out.findDocumentationForOffset(middleOffset(adapter2));

        // then
        assertThat(docTc1Found).hasValue(adapter1);
        assertThat(docTc2Found).hasValue(adapter2);
    }

    @Test
    public void test_givenTestCaseTable_and_positionAtOneByteAfterEndOfDocumentation_shouldReturn_absent() {
        // given
        final LocalSetting<TestCase> testTC1Documentation = testCaseTable.getTestCases().get(0).getDocumentation().get(0);
        final LocalSetting<TestCase> testTC2Documentation = testCaseTable.getTestCases().get(1).getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docTC1Found = out
                .findDocumentationForOffset(testTC1Documentation.getEndPosition().getOffset() + 1);
        final Optional<IDocumentationHolder> docTC2Found = out
                .findDocumentationForOffset(testTC2Documentation.getEndPosition().getOffset() + 1);

        // then
        assertThat(docTC1Found).isNotPresent();
        assertThat(docTC2Found).isNotPresent();
    }

    @Test
    public void test_givenTestCaseTable_and_positionAtEndOfDocumentation_shouldReturn_documentation() {
        // given
        final LocalSetting<TestCase> testTc1Documentation = testCaseTable.getTestCases()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<TestCase> testTc2Documentation = testCaseTable.getTestCases()
                .get(1)
                .getDocumentation()
                .get(0);
        final IDocumentationHolder adapter1 = testTc1Documentation.adaptTo(IDocumentationHolder.class);
        final IDocumentationHolder adapter2 = testTc2Documentation.adaptTo(IDocumentationHolder.class);

        // when
        final Optional<IDocumentationHolder> docTc1Found = out
                .findDocumentationForOffset(testTc1Documentation.getEndPosition().getOffset());
        final Optional<IDocumentationHolder> docTc2Found = out
                .findDocumentationForOffset(testTc2Documentation.getEndPosition().getOffset());

        // then
        assertThat(docTc1Found).hasValue(adapter1);
        assertThat(docTc2Found).hasValue(adapter2);
    }

    @Test
    public void test_givenTestCaseTable_and_positionAtBeginOfDocumentation_shouldReturn_documentation() {
        // given
        final LocalSetting<TestCase> testTc1Documentation = testCaseTable.getTestCases().get(0).getDocumentation().get(0);
        final LocalSetting<TestCase> testTc2Documentation = testCaseTable.getTestCases().get(1).getDocumentation().get(0);
        final IDocumentationHolder adapter1 = testTc1Documentation.adaptTo(IDocumentationHolder.class);
        final IDocumentationHolder adapter2 = testTc2Documentation.adaptTo(IDocumentationHolder.class);

        // when
        final Optional<IDocumentationHolder> docTc1Found = out
                .findDocumentationForOffset(testTc1Documentation.getBeginPosition().getOffset());
        final Optional<IDocumentationHolder> docTc2Found = out
                .findDocumentationForOffset(testTc2Documentation.getBeginPosition().getOffset());

        // then
        assertThat(docTc1Found).hasValue(adapter1);
        assertThat(docTc2Found).hasValue(adapter2);
    }

    @Test
    public void test_givenTestCaseTable_and_positionAtOneByteBeforeBeginOfDocumentation_shouldReturn_absent() {
        // given
        final LocalSetting<TestCase> testTC1Documentation = testCaseTable.getTestCases().get(0).getDocumentation().get(0);
        final LocalSetting<TestCase> testTC2Documentation = testCaseTable.getTestCases().get(1).getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docTC1Found = out
                .findDocumentationForOffset(testTC1Documentation.getBeginPosition().getOffset() - 1);
        final Optional<IDocumentationHolder> docTC2Found = out
                .findDocumentationForOffset(testTC2Documentation.getBeginPosition().getOffset() - 1);

        // then
        assertThat(docTC1Found).isNotPresent();
        assertThat(docTC2Found).isNotPresent();
    }

    @Test
    public void test_givenKeywordTable_and_positionAtMiddleOfDocumentation_shouldReturn_documentation() {
        // given
        final LocalSetting<UserKeyword> keyKw1Documentation = keywordTable.getKeywords()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<UserKeyword> keyKw2Documentation = keywordTable.getKeywords()
                .get(1)
                .getDocumentation()
                .get(0);
        final IDocumentationHolder adapter1 = keyKw1Documentation.adaptTo(IDocumentationHolder.class);
        final IDocumentationHolder adapter2 = keyKw2Documentation.adaptTo(IDocumentationHolder.class);

        // when
        final Optional<IDocumentationHolder> docKw1Found = out.findDocumentationForOffset(middleOffset(adapter1));
        final Optional<IDocumentationHolder> docKw2Found = out.findDocumentationForOffset(middleOffset(adapter2));

        // then
        assertThat(docKw1Found).hasValue(adapter1);
        assertThat(docKw2Found).hasValue(adapter2);
    }

    @Test
    public void test_givenKeywordTable_and_positionAtOneByteAfterEndOfDocumentation_shouldReturn_absent() {
        // given
        final LocalSetting<UserKeyword> keyKw1Documentation = keywordTable.getKeywords()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<UserKeyword> keyKw2Documentation = keywordTable.getKeywords()
                .get(1)
                .getDocumentation()
                .get(0);

        // when
        final Optional<IDocumentationHolder> docKw1Found = out
                .findDocumentationForOffset(keyKw1Documentation.getEndPosition().getOffset() + 1);
        final Optional<IDocumentationHolder> docKw2Found = out
                .findDocumentationForOffset(keyKw2Documentation.getEndPosition().getOffset() + 1);

        // then
        assertThat(docKw1Found).isNotPresent();
        assertThat(docKw2Found).isNotPresent();
    }

    @Test
    public void test_givenKeywordTable_and_positionAtEndOfDocumentation_shouldReturn_documentation() {
        // given
        final LocalSetting<UserKeyword> keyKw1Documentation = keywordTable.getKeywords()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<UserKeyword> keyKw2Documentation = keywordTable.getKeywords()
                .get(1)
                .getDocumentation()
                .get(0);
        final IDocumentationHolder adapter1 = keyKw1Documentation.adaptTo(IDocumentationHolder.class);
        final IDocumentationHolder adapter2 = keyKw2Documentation.adaptTo(IDocumentationHolder.class);

        // when
        final Optional<IDocumentationHolder> docKw1Found = out
                .findDocumentationForOffset(keyKw1Documentation.getEndPosition().getOffset());
        final Optional<IDocumentationHolder> docKw2Found = out
                .findDocumentationForOffset(keyKw2Documentation.getEndPosition().getOffset());

        // then
        assertThat(docKw1Found).hasValue(adapter1);
        assertThat(docKw2Found).hasValue(adapter2);
    }

    @Test
    public void test_givenKeywordTable_and_positionAtBeginOfDocumentation_shouldReturn_documentation() {
        // given
        final LocalSetting<UserKeyword> keyKw1Documentation = keywordTable.getKeywords()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<UserKeyword> keyKw2Documentation = keywordTable.getKeywords()
                .get(1)
                .getDocumentation()
                .get(0);
        final IDocumentationHolder adapter1 = keyKw1Documentation.adaptTo(IDocumentationHolder.class);
        final IDocumentationHolder adapter2 = keyKw2Documentation.adaptTo(IDocumentationHolder.class);

        // when
        final Optional<IDocumentationHolder> docKW1Found = out
                .findDocumentationForOffset(keyKw1Documentation.getBeginPosition().getOffset());
        final Optional<IDocumentationHolder> docKW2Found = out
                .findDocumentationForOffset(keyKw2Documentation.getBeginPosition().getOffset());

        // then
        assertThat(docKW1Found).hasValue(adapter1);
        assertThat(docKW2Found).hasValue(adapter2);
    }

    @Test
    public void test_givenKeywordTable_and_positionAtOneByteBeforeBeginOfDocumentation_shouldReturn_absent() {
        // given
        final LocalSetting<UserKeyword> keyKw1Documentation = keywordTable.getKeywords()
                .get(0)
                .getDocumentation()
                .get(0);
        final LocalSetting<UserKeyword> keyKw2Documentation = keywordTable.getKeywords()
                .get(1)
                .getDocumentation()
                .get(0);

        // when
        final Optional<IDocumentationHolder> docKW1Found = out
                .findDocumentationForOffset(keyKw1Documentation.getBeginPosition().getOffset() - 1);
        final Optional<IDocumentationHolder> docKW2Found = out
                .findDocumentationForOffset(keyKw2Documentation.getBeginPosition().getOffset() - 1);

        // then
        assertThat(docKW1Found).isNotPresent();
        assertThat(docKW2Found).isNotPresent();
    }

    @Test
    public void test_givenSettingTable_and_positionAtMiddleOfDocumentation_shouldReturn_documentation() {
        // given
        final SuiteDocumentation suiteDocumentation = settingTable.getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docFound = out.findDocumentationForOffset(middleOffset(suiteDocumentation));

        // then
        assertThat(docFound).containsSame(suiteDocumentation);
    }

    @Test
    public void test_givenSettingTable_and_positionAtOneByteAfterEndOfDocumentation_shouldReturn_absent() {
        // given
        final SuiteDocumentation suiteDocumentation = settingTable.getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docFound = out
                .findDocumentationForOffset(suiteDocumentation.getEndPosition().getOffset() + 1);

        // then
        assertThat(docFound).isNotPresent();
    }

    @Test
    public void test_givenSettingTable_and_positionAtEndOfDocumentation_shouldReturn_documentation() {
        // given
        final SuiteDocumentation suiteDocumentation = settingTable.getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docFound = out
                .findDocumentationForOffset(suiteDocumentation.getEndPosition().getOffset());

        // then
        assertThat(docFound).containsSame(suiteDocumentation);
    }

    @Test
    public void test_givenSettingTable_and_positionAtBeginOfDocumentation_shouldReturn_documentation() {
        // given
        final SuiteDocumentation suiteDocumentation = settingTable.getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docFound = out
                .findDocumentationForOffset(suiteDocumentation.getBeginPosition().getOffset());

        // then
        assertThat(docFound).containsSame(suiteDocumentation);
    }

    @Test
    public void test_givenSettingTable_and_positionAtOneByteBeforeBeginOfDocumentation_shouldReturn_absent() {
        // given
        final SuiteDocumentation suiteDocumentation = settingTable.getDocumentation().get(0);

        // when
        final Optional<IDocumentationHolder> docFound = out
                .findDocumentationForOffset(suiteDocumentation.getBeginPosition().getOffset() - 1);

        // then
        assertThat(docFound).isNotPresent();
    }

    private int middleOffset(final IDocumentationHolder holder) {
        final int startOffset = holder.getBeginPosition().getOffset();
        final int endOffset = holder.getContinuousRegions()
                .get(holder.getContinuousRegions().size() - 1)
                .getEnd()
                .getOffset();

        return startOffset + ((endOffset - startOffset) % 2);
    }
}
