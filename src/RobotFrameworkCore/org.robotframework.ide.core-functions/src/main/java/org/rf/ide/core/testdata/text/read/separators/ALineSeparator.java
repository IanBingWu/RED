/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.read.separators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;
import org.rf.ide.core.testdata.text.read.separators.Separator.SeparatorType;

public abstract class ALineSeparator {

    private final AtomicBoolean wasInitialized = new AtomicBoolean(false);
    private int prevElementIndex = -1;
    private int elementIndex = -1;
    protected final int lineNumber;
    protected final String line;
    private final List<IRobotLineElement> lineElements = new ArrayList<>();

    protected ALineSeparator(final int lineNumber, final String line) {
        this.lineNumber = lineNumber;
        this.line = line;
    }

    private void init() {
        synchronized (this) {
            if (!wasInitialized.get()) {
                wasInitialized.set(true);
                int lastColumnProcessed = 0;
                final int textLength = line.length();

                if (textLength > 0) {
                    while (lastColumnProcessed < textLength) {
                        if (hasNextSeparator()) {
                            final Separator currentSeparator = nextSeparator();
                            final int startColumn = currentSeparator.getStartColumn();
                            final int remainingData = startColumn - lastColumnProcessed;
                            final String rawText = line.substring(lastColumnProcessed, startColumn);
                            if (remainingData > 0 || !lineElements.isEmpty()) {
                                lineElements.add(RobotToken.create(rawText, lineNumber, lastColumnProcessed,
                                        RobotTokenType.UNKNOWN));
                            }

                            lineElements.add(currentSeparator);
                            lastColumnProcessed = currentSeparator.getEndColumn();
                        } else {
                            final String rawText = line.substring(lastColumnProcessed);
                            lineElements.add(RobotToken.create(rawText, lineNumber, lastColumnProcessed,
                                    RobotTokenType.UNKNOWN));

                            lastColumnProcessed = textLength;
                        }
                    }
                }
            }
        }
    }

    protected abstract Separator nextSeparator();

    public Separator next() {
        init();
        updateIndex();

        final Separator found = elementIndex > prevElementIndex ? (Separator) lineElements.get(elementIndex) : null;

        prevElementIndex = elementIndex;

        return found;
    }

    protected abstract boolean hasNextSeparator();

    public boolean hasNext() {
        init();
        updateIndex();

        return elementIndex > prevElementIndex;
    }

    private void updateIndex() {
        final int elementsSize = lineElements.size();
        if (prevElementIndex < elementsSize && elementIndex <= prevElementIndex) {
            for (int index = elementIndex + 1; index < elementsSize; index++) {
                if (isSeparator(lineElements.get(index))) {
                    elementIndex = index;
                    break;
                }
            }
        }
    }

    private boolean isSeparator(final IRobotLineElement elem) {
        for (final IRobotTokenType type : elem.getTypes()) {
            if (type == SeparatorType.PIPE || type == SeparatorType.TABULATOR_OR_DOUBLE_SPACE) {
                return true;
            }
        }
        return false;
    }

    public abstract SeparatorType getProducedType();

    public List<IRobotLineElement> getSplittedLine() {
        init();
        return Collections.unmodifiableList(lineElements);
    }

    public int getPreviousElementIndex() {
        return prevElementIndex;
    }

    public int getCurrentElementIndex() {
        return elementIndex;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getLine() {
        return line;
    }
}
