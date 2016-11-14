/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.code;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.ICommentHolder;
import org.rf.ide.core.testdata.model.ModelType;
import org.rf.ide.core.testdata.model.presenter.CommentServiceHandler.ETokenSeparator;
import org.rf.ide.core.testdata.model.table.IExecutableStepsHolder;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.RobotExecutableRowView;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.cmd.SetKeywordCallArgumentCommand2;
import org.robotframework.ide.eclipse.main.plugin.model.cmd.SetKeywordCallCommentCommand;
import org.robotframework.ide.eclipse.main.plugin.model.cmd.SetKeywordCallNameCommand;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.EditorCommand;

/**
 * @author wypych
 */
public class ExecutablesRowHolderCommentService {

    public static boolean wasHandledAsComment(final List<EditorCommand> commands, final RobotKeywordCall call,
            final String value, final int column, final int numberOfColumns) {

        boolean handled = false;
        final List<RobotToken> execRowView = execRowView(call);
        if (!execRowView.isEmpty()) {
            final int indexOfComment = findIndexOfTheFirstCommentBefore(execRowView, column);
            if (looksLikeComment(value) && (indexOfComment > column || indexOfComment < 0)) {
                // conversion to comment
                commands.add(new ConversionToCommentCommand(call, value, column, execRowView));
                handled = true;
            } else if (indexOfComment == column) {
                // conversion from comment
                commands.add(new ConversionFromCommentCommand(call, value, column, execRowView));
                handled = true;
            } else if (indexOfComment == 0) {
                // update comment begin in action token
                commands.add(new SetKeywordCallCommentCommand(call,
                        commentUpdatedValue((ICommentHolder) call.getLinkedElement(), column, value)));
                handled = true;
            } else if (indexOfComment > 0) {
                // update comment
                commands.add(new SetKeywordCallCommentCommand(call,
                        commentUpdatedValue((ICommentHolder) call.getLinkedElement(), column - indexOfComment, value)));

                handled = true;
            }
        }

        return handled;
    }

    private static class ConversionToCommentCommand extends EditorCommand {

        private final RobotKeywordCall call;

        private final String value;

        private final int column;

        private final List<RobotToken> execRowView;

        private List<EditorCommand> executedCommands;

        public ConversionToCommentCommand(final RobotKeywordCall call, final String value, final int column,
                final List<RobotToken> execRowView) {
            this.call = call;
            this.column = column;
            this.value = value;
            this.execRowView = execRowView;
        }

        @Override
        public void execute() throws CommandExecutionException {
            this.executedCommands = new ArrayList<>(0);

            List<RobotToken> commentToken = new ArrayList<>(0);

            RobotKeywordCall callToUse = call;
            int startColumn = column;
            if (column == 0) {
                startColumn = 1;
                final SetKeywordCallNameCommand changeTmpName = new SetKeywordCallNameCommand(eventBroker, call, value);
                changeTmpName.execute();
                executedCommands.add(changeTmpName);
            }

            boolean notComment = true;
            for (int i = startColumn; i < execRowView.size(); i++) {
                final String elementValue = execRowView.get(i).getText();
                if (column == i) {
                    final SetKeywordCallArgumentCommand2 argSet = new SetKeywordCallArgumentCommand2(eventBroker,
                            callToUse, startColumn - 1, null);
                    argSet.execute();
                    executedCommands.add(argSet);
                    commentToken.add(RobotToken.create(value));
                } else if (elementValue.trim().startsWith("#") || !notComment) {
                    commentToken.add(execRowView.get(i));
                    notComment = false;
                } else if (notComment) {
                    final SetKeywordCallArgumentCommand2 argSetNotTheSameColumn = new SetKeywordCallArgumentCommand2(
                            eventBroker, callToUse, startColumn - 1, null);
                    argSetNotTheSameColumn.execute();
                    commentToken.add(execRowView.get(i));
                    executedCommands.add(argSetNotTheSameColumn);
                }
            }

            fillMissingColumns(callToUse);

            if (column > 0 && commentToken.isEmpty()) {
                commentToken.add(RobotToken.create(value));
            }

            String newComment = null;
            if (!commentToken.isEmpty()) {
                newComment = commentViewBuild(commentToken);
            }

            final SetKeywordCallCommentCommand commentUpdate = new SetKeywordCallCommentCommand(eventBroker, callToUse,
                    newComment);
            commentUpdate.execute();
            executedCommands.add(commentUpdate);
            call.resetStored();
        }

        private void fillMissingColumns(RobotKeywordCall callToUse) {
            final int columnsInView = execRowView.size() - 1;
            if (columnsInView < column) {
                for (int i = columnsInView; i < column; i++) {
                    if (i == 0 && (execRowView.size() == 0 || execRowView.get(0).getText().isEmpty())) {
                        final SetKeywordCallNameCommand changeTmpName = new SetKeywordCallNameCommand(eventBroker, call,
                                "\\");
                        changeTmpName.execute();
                        executedCommands.add(changeTmpName);
                    } else {
                        final SetKeywordCallArgumentCommand2 argSetNotTheSameColumn = new SetKeywordCallArgumentCommand2(
                                eventBroker, callToUse, column - 2, "\\");
                        argSetNotTheSameColumn.execute();
                        executedCommands.add(argSetNotTheSameColumn);
                        break;
                    }
                }
            }
        }

        @Override
        public List<EditorCommand> getUndoCommands() {
            final List<EditorCommand> undoCommands = new ArrayList<>(0);
            for (final EditorCommand executedCommand : executedCommands) {
                undoCommands.addAll(0, executedCommand.getUndoCommands());
            }
            return newUndoCommands(undoCommands);
        }
    }

    private static class ConversionFromCommentCommand extends EditorCommand {

        private final RobotKeywordCall call;

        private final String value;

        private final int column;

        private final List<RobotToken> execRowView;

        private List<EditorCommand> executedCommands;

        public ConversionFromCommentCommand(final RobotKeywordCall call, final String value, final int column,
                final List<RobotToken> execRowView) {
            this.call = call;
            this.column = column;
            this.value = value;
            this.execRowView = execRowView;
        }

        @Override
        public void execute() throws CommandExecutionException {
            this.executedCommands = new ArrayList<>(0);

            Stack<EditorCommand> executionContext = new Stack<>();
            call.resetStored();
            int startColumn = column;
            if (column == 0) {
                final SetKeywordCallNameCommand changeTmpName = new SetKeywordCallNameCommand(eventBroker, call,
                        execRowView.get(0).getText());
                changeTmpName.execute();
                executionContext.push(changeTmpName);
                startColumn = 1;
            }

            List<RobotToken> commentToken = new ArrayList<>(0);
            boolean notComment = true;
            for (int i = startColumn; i < execRowView.size(); i++) {
                final String elementValue = execRowView.get(i).getText();
                if (column == i) {
                    final SetKeywordCallArgumentCommand2 argSet = new SetKeywordCallArgumentCommand2(eventBroker, call,
                            call.getArguments().size(), value);
                    argSet.execute();
                    executionContext.push(argSet);
                } else if (elementValue.trim().startsWith("#") || !notComment) {
                    commentToken.add(execRowView.get(i));
                    notComment = false;
                } else if (notComment) {
                    final SetKeywordCallArgumentCommand2 argSetNotTheSameColumn = new SetKeywordCallArgumentCommand2(
                            eventBroker, call, call.getArguments().size(), elementValue);
                    argSetNotTheSameColumn.execute();
                    executionContext.push(argSetNotTheSameColumn);
                }
            }

            String newComment = null;
            if (!commentToken.isEmpty()) {
                newComment = commentViewBuild(commentToken);
            }

            final SetKeywordCallCommentCommand commentUpdate = new SetKeywordCallCommentCommand(eventBroker, call,
                    newComment);
            commentUpdate.execute();
            executionContext.push(commentUpdate);

            if (column == 0) {
                final SetKeywordCallNameCommand updateKeywordName = new SetKeywordCallNameCommand(eventBroker, call,
                        value);
                updateKeywordName.execute();
                executionContext.push(updateKeywordName);
            }

            executedCommands.addAll(executionContext);
        }

        @Override
        public List<EditorCommand> getUndoCommands() {
            final List<EditorCommand> undoCommands = new ArrayList<>(0);
            for (final EditorCommand executedCommand : executedCommands) {
                undoCommands.addAll(0, executedCommand.getUndoCommands());
            }
            return newUndoCommands(undoCommands);
        }
    }

    private static String commentUpdatedValue(final ICommentHolder cmHolder, final int commentIndex,
            final String commentValue) {
        final List<RobotToken> comment = new ArrayList<>(cmHolder.getComment());
        final int cmSize = comment.size();
        if (commentIndex >= cmSize) {
            for (int i = 0; i <= (commentIndex - cmSize); i++) {
                comment.add(RobotToken.create("\\"));
            }
        }

        if (commentValue == null) {
            comment.remove(commentIndex);
        } else {
            comment.set(commentIndex, RobotToken.create(commentValue));
        }

        return commentViewBuild(comment);
    }

    private static String commentViewBuild(final List<RobotToken> comment) {
        final int cmSizeUpdate = comment.size();
        String cmUpdated = null;
        if (cmSizeUpdate > 0) {
            final StringBuilder str = new StringBuilder();
            for (int i = 0; i < cmSizeUpdate; i++) {
                if (i > 0) {
                    str.append(ETokenSeparator.PIPE_WRAPPED_WITH_SPACE.getSeparatorAsText());
                }
                str.append(comment.get(i).getText());
            }

            cmUpdated = str.toString();
        }

        return cmUpdated;
    }

    private static int findIndexOfTheFirstCommentBefore(final List<RobotToken> rts, final int index) {
        int size = rts.size();

        int theFirstIndex = -1;
        final int startValue;
        if (index >= size) {
            startValue = size - 1;
        } else {
            startValue = index;
        }

        for (int i = startValue; i >= 0; i--) {
            if (rts.get(i).getText().trim().startsWith("#")) {
                theFirstIndex = i;
            }
        }

        return theFirstIndex;
    }

    public static List<RobotToken> execRowView(final RobotFileInternalElement element) {
        final List<RobotToken> toks = new ArrayList<>();
        final Object linkedElement = element.getLinkedElement();
        if (linkedElement instanceof AModelElement) {
            final AModelElement<?> modelElement = (AModelElement<?>) linkedElement;

            if (isExecutable(modelElement)) {
                final RobotExecutableRowView view = RobotExecutableRowView
                        .buildView((RobotExecutableRow<? extends IExecutableStepsHolder<?>>) linkedElement);
                toks.addAll(newArrayList(transform(modelElement.getElementTokens(),
                        RobotKeywordCall.tokenViaExecutableViewUpdateToken(view))));
                if (toks.size() >= 2) {
                    final RobotToken actionToken = toks.get(0);
                    if (actionToken.getFilePosition().isNotSet() && actionToken.getText().isEmpty()) {
                        final List<IRobotTokenType> types = toks.get(1).getTypes();
                        if (types.contains(RobotTokenType.START_HASH_COMMENT)
                                || types.contains(RobotTokenType.COMMENT_CONTINUE)) {
                            toks.remove(0);
                        }
                    }
                }
            } else {
                toks.addAll(modelElement.getElementTokens());
            }
        }

        return toks;
    }

    private static boolean isExecutable(final AModelElement<?> linkedElement) {
        return linkedElement.getModelType() == ModelType.TEST_CASE_EXECUTABLE_ROW
                || linkedElement.getModelType() == ModelType.USER_KEYWORD_EXECUTABLE_ROW;
    }

    public static boolean isCommentOperation(final String oldText, final String newText) {
        return looksLikeComment(oldText) || looksLikeComment(newText);
    }

    public static boolean looksLikeComment(final String text) {
        return (text != null) && text.trim().startsWith("#");
    }
}
