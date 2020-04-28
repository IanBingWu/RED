/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.postfixes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.FilePosition;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.IExecutableStepsHolder;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor.RowType;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

class ExecutableUnitsFixer {

    <T extends AModelElement<? extends ARobotSectionTable>> List<AModelElement<T>> applyFix(
            final IExecutableStepsHolder<T> execUnit) {

        final List<AModelElement<T>> newRows = new ArrayList<>();
        final List<AModelElement<T>> oldRows = execUnit.getElements();
        final List<IExecutableRowDescriptor<T>> preBuildDescriptors = preBuildDescriptors(oldRows);

        int lastForIndex = -1;
        int lastForExecutableIndex = -1;
        final int lineNumbers = preBuildDescriptors.size();
        for (int lineId = 0; lineId < lineNumbers; lineId++) {
            final IExecutableRowDescriptor<T> currentExecLine = preBuildDescriptors.get(lineId);
            final RowType rowType = currentExecLine.getRowType();

            final AModelElement<T> row = currentExecLine.getRow();
            if (rowType == RowType.FOR) {
                if (lastForIndex > -1 && lastForExecutableIndex > -1) {
                    applyArtificialForLineContinue(newRows, lastForIndex, lastForExecutableIndex);
                }
                lastForIndex = newRows.size();
                newRows.add(row);
                lastForExecutableIndex = -1;
            } else {
                if (rowType == RowType.FOR_CONTINUE) {
                    final Optional<RobotToken> previousLineContinue = getPreviousLineContinueToken(
                            row.getElementTokens());
                    if (previousLineContinue.isPresent()) {
                        merge(execUnit, newRows, preBuildDescriptors, lineId, previousLineContinue.get());
                    } else {
                        newRows.add(row);

                        if (lastForIndex > -1 && lastForExecutableIndex > -1) {
                            lastForExecutableIndex = newRows.size() - 1;
                            applyArtificialForLineContinue(newRows, lastForIndex, lastForExecutableIndex);
                        }
                    }
                } else if (rowType == RowType.SIMPLE) {
                    if (row instanceof RobotExecutableRow<?> && ((RobotExecutableRow<?>) row).getAction() != null) {
                        ((RobotExecutableRow<?>) row).getAction().getTypes().remove(RobotTokenType.FOR_CONTINUE_TOKEN);
                    }

                    if (containsArtificialContinueAffectingForLoop(currentExecLine)) {
                        lastForExecutableIndex = newRows.size() - 1;
                        if (lastForIndex == -1) {
                            final Optional<Integer> execLine = findLineWithExecAction(newRows);
                            if (execLine.isPresent()) {
                                final int parentLine = execLine.get();
                                final RobotExecutableRow<T> toMergeLine = (RobotExecutableRow<T>) newRows
                                        .get(parentLine);
                                final int numberOfMerges = lastForExecutableIndex - parentLine;
                                for (int i = 0; i < numberOfMerges; i++) {
                                    merge(toMergeLine, newRows.get(parentLine + 1));
                                    newRows.remove(parentLine + 1);
                                }

                                merge(toMergeLine, row);
                            } else {
                                newRows.add(row);
                            }
                        } else {
                            newRows.add(row);
                            if (lastForIndex > -1 && lastForExecutableIndex > -1) {
                                lastForExecutableIndex = newRows.size() - 1;
                                applyArtificialForLineContinue(newRows, lastForIndex,
                                        lastForExecutableIndex);
                            }
                        }
                    } else {
                        final Optional<RobotToken> previousLineContinue = getPreviousLineContinueToken(
                                row.getElementTokens());
                        if (previousLineContinue.isPresent()) {
                            merge(execUnit, newRows, preBuildDescriptors, lineId,
                                    previousLineContinue.get());
                        } else {
                            newRows.add(row);
                        }

                        if (lastForIndex > -1 && lastForExecutableIndex > -1) {
                            applyArtificialForLineContinue(newRows, lastForIndex, lastForExecutableIndex);
                        }
                        lastForIndex = -1;
                        lastForExecutableIndex = -1;
                    }
                } else if (rowType == RowType.COMMENTED_HASH || rowType == RowType.SETTING
                        || rowType == RowType.FOR_END) {
                    newRows.add(row);
                } else {
                    throw new IllegalStateException("Unsupported executable row type " + rowType + ". In file "
                            + execUnit.getHolder().getParent().getParent().getParent().getProcessedFile() + " near "
                            + row.getBeginPosition());
                }
            }

        }

        if (lastForIndex > -1 && lastForExecutableIndex > -1) {
            applyArtificialForLineContinue(newRows, lastForIndex, lastForExecutableIndex);
        }

        boolean isContinue = false;
        final int size = newRows.size();
        for (int i = size - 1; i >= 0; i--) {
            if (!(newRows.get(i) instanceof RobotExecutableRow)) {
                isContinue = false;
                continue;
            }
            final RobotExecutableRow<T> execLine = (RobotExecutableRow<T>) newRows.get(i);
            final IExecutableRowDescriptor<T> lineDescription = execLine.buildLineDescription();
            final RowType rowType = lineDescription.getRowType();
            if (rowType == RowType.FOR_CONTINUE) {
                if (execLine.getAction().getText().isEmpty()) {
                    if (execLine.getAction().getTypes().contains(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN)) {
                        if (isActionNotTheFirstElement(execLine.getAction(), execLine.getElementTokens())) {
                            final RobotToken actionToBeArgument = execLine.getAction().copy();
                            actionToBeArgument.getTypes().remove(RobotTokenType.KEYWORD_ACTION_NAME);
                            actionToBeArgument.getTypes().remove(RobotTokenType.TEST_CASE_ACTION_NAME);
                            actionToBeArgument.getTypes().remove(RobotTokenType.TASK_ACTION_NAME);
                            actionToBeArgument.getTypes().remove(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
                            actionToBeArgument.setText("\\");
                            execLine.addArgument(0, actionToBeArgument);
                        }
                        execLine.getAction().setText("\\");
                    } else if (!execLine.getAction().getTypes().contains(RobotTokenType.FOR_WITH_END_CONTINUATION)) {
                        execLine.getAction().setText("\\");
                        execLine.getAction().getTypes().add(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
                    }
                } else if (!execLine.getAction().getText().equals("\\")) {
                    final RobotToken actionToBeArgument = execLine.getAction().copy();
                    actionToBeArgument.getTypes().remove(RobotTokenType.KEYWORD_ACTION_NAME);
                    actionToBeArgument.getTypes().remove(RobotTokenType.TEST_CASE_ACTION_NAME);
                    actionToBeArgument.getTypes().remove(RobotTokenType.TASK_ACTION_NAME);
                    actionToBeArgument.getTypes().remove(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
                    execLine.addArgument(0, actionToBeArgument);
                    execLine.getAction().setText("\\");
                    execLine.getAction().getTypes().add(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
                } else {
                    execLine.getAction().getTypes().remove(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
                }
                isContinue = true;
            } else if (rowType == RowType.COMMENTED_HASH) {
                if (execLine.getAction().getTypes().contains(RobotTokenType.FOR_WITH_END_CONTINUATION)) {
                    continue;
                } else if (isContinue) {
                    if (!execLine.getAction().getText().equals("\\")) {
                        execLine.getAction().setText("\\");
                        execLine.getAction().getTypes().add(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
                    }
                } else if (execLine.getAction().getText().isEmpty()) {
                    execLine.getAction().setType(RobotTokenType.COMMENT);
                } else {
                    execLine.getAction().getTypes().remove(RobotTokenType.FOR_CONTINUE_TOKEN);
                }
            } else if (rowType == RowType.FOR || rowType == RowType.SIMPLE) {
                isContinue = false;
            }
        }

        return newRows;
    }

    /**
     * Special handling for TSV format:
     * <code>
     *  :FOR    ${x}    IN  10
        #   d
        ...     kw_w
        \   kw_w
     * </code>
     * 
     * @param action
     * @param elementTokens
     * @return
     */
    private boolean isActionNotTheFirstElement(final RobotToken action, final List<RobotToken> elementTokens) {
        final FilePosition actionPosition = action.getFilePosition();
        if (!actionPosition.isNotSet()) {
            for (final RobotToken tok : elementTokens) {
                final FilePosition currentTokenPosition = tok.getFilePosition();
                if (!currentTokenPosition.isNotSet()) {
                    if (currentTokenPosition.isBefore(actionPosition)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private <T> Optional<Integer> findLineWithExecAction(final List<AModelElement<T>> newExecutionContext) {
        for (int i = newExecutionContext.size() - 1; i >= 0; i--) {
            if (newExecutionContext.get(i) instanceof RobotExecutableRow
                    && ((RobotExecutableRow<T>) newExecutionContext.get(i)).isExecutable()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    private <T extends AModelElement<? extends ARobotSectionTable>> void merge(final RobotExecutableRow<T> outputLine,
            final AModelElement<T> lineToMerge) {
        if (!(lineToMerge instanceof RobotExecutableRow)) {
            return;
        }
        final RobotExecutableRow<T> toMerge = (RobotExecutableRow<T>) lineToMerge;
        if (!toMerge.getAction().getFilePosition().isNotSet()) {
            outputLine.addArgument(toMerge.getAction());
        }

        final List<RobotToken> arguments = toMerge.getArguments();
        for (final RobotToken t : arguments) {
            outputLine.addArgument(t);
        }

        final List<RobotToken> comments = toMerge.getComment();
        for (final RobotToken robotToken : comments) {
            outputLine.addCommentPart(robotToken);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends AModelElement<? extends ARobotSectionTable>> void merge(final IExecutableStepsHolder<T> execUnit,
            final List<AModelElement<T>> newExecutionContext,
            final List<IExecutableRowDescriptor<T>> preBuildDescriptors, final int currentLine,
            final RobotToken previousLineContinueToken) {
        final IExecutableRowDescriptor<T> rowDesc = preBuildDescriptors.get(currentLine);

        RobotExecutableRow<T> toUpdate = null;
        if (newExecutionContext.isEmpty()) {
            toUpdate = new RobotExecutableRow<>();
            toUpdate.setParent((T) execUnit);
            newExecutionContext.add(toUpdate);
        } else if (rowDesc.getRowType() == RowType.SETTING) {
            return;
        } else {
            final AModelElement<T> lastRewritten = newExecutionContext.get(newExecutionContext.size() - 1);
            if (lastRewritten instanceof RobotExecutableRow) {
                toUpdate = (RobotExecutableRow<T>) lastRewritten;
            } else {
                return;
            }
        }

        boolean wasComment = false;
        boolean shouldMerge = false;
        for (final RobotToken token : rowDesc.getRow().getElementTokens()) {
            if (rowDesc.getRowType() == RowType.FOR_CONTINUE && toUpdate.getAction().getFilePosition().isNotSet()
                    && "\\".equals(token.getText())) {
                toUpdate.setAction(token);
            }

            if (token == previousLineContinueToken) {
                shouldMerge = true;
                continue;
            }

            if (shouldMerge) {
                if (token.getTypes().contains(RobotTokenType.COMMENT) || wasComment) {
                    wasComment = true;
                    toUpdate.addCommentPart(token);
                } else {
                    if (toUpdate.getAction().getFilePosition().isNotSet()) {
                        toUpdate.setAction(token);
                    } else {
                        toUpdate.addArgument(token);
                    }
                }
            }
        }
    }

    private <T extends AModelElement<? extends ARobotSectionTable>> void applyArtificialForLineContinue(
            final List<AModelElement<T>> executionContext, final int forStart, final int forEnd) {
        for (int line = forStart + 1; line <= forEnd; line++) {
            executionContext.get(line).getDeclaration().getTypes().add(RobotTokenType.FOR_CONTINUE_ARTIFICIAL_TOKEN);
        }
    }

    private Optional<RobotToken> getPreviousLineContinueToken(final List<RobotToken> tokens) {
        Optional<RobotToken> token = Optional.empty();
        for (final RobotToken rt : tokens) {
            final String text = rt.getText().trim();
            if (rt.getTypes().contains(RobotTokenType.PREVIOUS_LINE_CONTINUE) && "...".equals(text)) {
                token = Optional.of(rt);

            } else if (text.equals("\\") || text.isEmpty()) {
                continue;

            } else {
                break;
            }
        }
        return token;
    }

    private <T extends AModelElement<? extends ARobotSectionTable>> List<IExecutableRowDescriptor<T>> preBuildDescriptors(
            final List<AModelElement<T>> executionContext) {

        final List<IExecutableRowDescriptor<T>> descs = new ArrayList<>(0);
        for (final AModelElement<T> elem : executionContext) {
            if (elem instanceof RobotExecutableRow) {
                descs.add(((RobotExecutableRow<T>) elem).buildLineDescription());
            } else {
                descs.add(new SettingDescriptor<>(elem));
            }
        }
        return descs;
    }

    private <T extends AModelElement<? extends ARobotSectionTable>> boolean containsArtificialContinueAffectingForLoop(
            final IExecutableRowDescriptor<T> execRow) {

        if (execRow.getRowType() == RowType.SIMPLE) {
            final RobotToken actionToken = execRow.getAction();
            if (actionToken != null && actionToken.getFilePosition().isSet()) {
                final List<RobotToken> elementTokens = execRow.getRow().getElementTokens();
                for (final RobotToken rt : elementTokens) {
                    if (rt != actionToken && rt.getLineNumber() < actionToken.getLineNumber()
                            && !isVariableDeclaration(rt.getTypes())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isVariableDeclaration(final List<IRobotTokenType> types) {
        return types.contains(RobotTokenType.VARIABLES_DICTIONARY_DECLARATION)
                || types.contains(RobotTokenType.VARIABLES_LIST_DECLARATION)
                || types.contains(RobotTokenType.VARIABLES_SCALAR_DECLARATION);
    }
}
