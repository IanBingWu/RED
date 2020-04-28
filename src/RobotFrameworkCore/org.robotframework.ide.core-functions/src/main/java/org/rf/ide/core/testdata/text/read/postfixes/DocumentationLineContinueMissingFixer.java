/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.postfixes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.IDocumentationHolder;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.KeywordTable;
import org.rf.ide.core.testdata.model.table.LocalSetting;
import org.rf.ide.core.testdata.model.table.SettingTable;
import org.rf.ide.core.testdata.model.table.TaskTable;
import org.rf.ide.core.testdata.model.table.TestCaseTable;
import org.rf.ide.core.testdata.model.table.keywords.UserKeyword;
import org.rf.ide.core.testdata.model.table.setting.SuiteDocumentation;
import org.rf.ide.core.testdata.model.table.tasks.Task;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.postfixes.PostProcessingFixActions.IPostProcessFixer;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.read.separators.Separator.SeparatorType;

/**
 * @author wypych
 */
class DocumentationLineContinueMissingFixer implements IPostProcessFixer {

    @Override
    public void applyFix(final RobotFileOutput parsingOutput) {
        final RobotFile model = parsingOutput.getFileModel();
        final List<RobotLine> fileContent = model.getFileContent();
        final SettingTable settingTable = model.getSettingTable();
        if (settingTable.isPresent()) {
            suiteDocumentationApplyContinueTokens(fileContent, settingTable);
        }

        final KeywordTable keywordTable = model.getKeywordTable();
        if (keywordTable.isPresent()) {
            for (final UserKeyword keyword : keywordTable.getKeywords()) {
                applyContinueTokens(fileContent, keyword.getDocumentation(),
                        RobotTokenType.KEYWORD_SETTING_DOCUMENTATION_TEXT);
            }
        }

        final TestCaseTable testCaseTable = model.getTestCaseTable();
        if (testCaseTable.isPresent()) {
            for (final TestCase testCase : testCaseTable.getTestCases()) {
                applyContinueTokens(fileContent, testCase.getDocumentation(),
                        RobotTokenType.TEST_CASE_SETTING_DOCUMENTATION_TEXT);
            }
        }

        final TaskTable tasksTable = model.getTasksTable();
        if (tasksTable.isPresent()) {
            for (final Task task : tasksTable.getTasks()) {
                applyContinueTokens(fileContent, task.getDocumentation(),
                        RobotTokenType.TASK_SETTING_DOCUMENTATION_TEXT);
            }
        }
    }

    private void suiteDocumentationApplyContinueTokens(final List<RobotLine> fileContent,
            final SettingTable settingTable) {
        final List<SuiteDocumentation> docDeclarations = settingTable.getDocumentation();
        for (final SuiteDocumentation docDec : docDeclarations) {
            final RobotToken declaration = docDec.getDeclaration();
            docDec.clearDocumentation();
            final List<RobotToken> docTokens = tokensBelongs(fileContent, declaration,
                    RobotTokenType.SETTING_DOCUMENTATION_TEXT, RobotTokenType.PRETTY_ALIGN_SPACE,
                    RobotTokenType.PREVIOUS_LINE_CONTINUE);
            for (final RobotToken textDoc : docTokens) {
                docDeclarations.get(0).addDocumentationText(textDoc);
            }
        }
    }

    private void applyContinueTokens(final List<RobotLine> fileContent, final List<? extends LocalSetting<?>> docs,
            final RobotTokenType docTextType) {
        for (final LocalSetting<?> documentation : docs) {
            final RobotToken declaration = documentation.getDeclaration();
            documentation.adaptTo(IDocumentationHolder.class).clearDocumentation();
            final List<RobotToken> docTokens = tokensBelongs(fileContent, declaration, docTextType,
                    RobotTokenType.PRETTY_ALIGN_SPACE, RobotTokenType.PREVIOUS_LINE_CONTINUE);
            for (final RobotToken textDoc : docTokens) {
                docs.get(0).addToken(textDoc);
            }
        }
    }

    private List<RobotToken> tokensBelongs(final List<RobotLine> fileContent, final RobotToken declaration,
            final IRobotTokenType... acceptable) {
        final FilePosition declarationPosition = declaration.getFilePosition();

        if (declarationPosition.isNotSet()) {
            return tokensBelongsByToken(fileContent, 0, declaration, acceptable);
        } else {
            return tokensBelongsByToken(fileContent, declarationPosition.getLine() - 1, declaration, acceptable);
        }
    }

    private List<RobotToken> tokensBelongsByToken(final List<RobotLine> fileContent, final int searchStartNumber,
            final RobotToken declaration, final IRobotTokenType... acceptable) {
        final List<RobotToken> toks = new ArrayList<>(0);

        Optional<Integer> elementPositionInLine = Optional.empty();
        final int lines = fileContent.size();
        boolean fetchMode = false;
        for (int lineNumber = searchStartNumber; lineNumber < lines; lineNumber++) {
            final RobotLine line = fileContent.get(lineNumber);
            if (fetchMode) {
                if (line.isEmpty()) {
                    continue;
                }

                final List<IRobotLineElement> lineElements = line.getLineElements();
                for (int i = 0; i < lineElements.size(); i++) {
                    final IRobotLineElement elem = lineElements.get(i);
                    final List<IRobotTokenType> elemTypes = elem.getTypes();
                    if (isAnyOfType(elemTypes, SeparatorType.PIPE, SeparatorType.TABULATOR_OR_DOUBLE_SPACE,
                            RobotTokenType.COMMENT)) {
                        continue;
                    } else if (isAnyOfType(elemTypes, acceptable)) {
                        if (elemTypes.contains(RobotTokenType.PREVIOUS_LINE_CONTINUE)
                                && !elem.getText().trim().equals("...")) {
                            return toks;
                        }
                        toks.add((RobotToken) elem);
                    } else {
                        return toks;
                    }
                }

            } else if (elementPositionInLine.isPresent()) {
                // line with declaration
                final List<IRobotLineElement> lineElements = line.getLineElements();
                for (int i = elementPositionInLine.get() + 1; i < lineElements.size(); i++) {
                    final IRobotLineElement elem = lineElements.get(i);
                    final List<IRobotTokenType> elemTypes = elem.getTypes();
                    if (isAnyOfType(elemTypes, SeparatorType.PIPE, SeparatorType.TABULATOR_OR_DOUBLE_SPACE,
                            RobotTokenType.COMMENT)) {
                        continue;
                    } else if (isAnyOfType(elemTypes, acceptable)) {
                        toks.add((RobotToken) elem);
                    } else {
                        return toks;
                    }
                }

                fetchMode = true;
            } else {
                elementPositionInLine = line.getElementPositionInLine(declaration);
                if (elementPositionInLine.isPresent()) {
                    lineNumber--;
                }
            }
        }
        return toks;
    }

    private boolean isAnyOfType(final List<IRobotTokenType> elemTypes, final IRobotTokenType... acceptable) {
        final List<IRobotTokenType> accept = Arrays.asList(acceptable);
        return elemTypes.stream().anyMatch(accept::contains);
    }
}
