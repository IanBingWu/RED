/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.debug;

import static com.google.common.base.Predicates.or;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.rf.ide.core.execution.agent.RobotDefaultAgentEventListener;
import org.rf.ide.core.execution.agent.event.KeywordEndedEvent;
import org.rf.ide.core.execution.agent.event.KeywordStartedEvent;
import org.rf.ide.core.execution.agent.event.ResourceImportEvent;
import org.rf.ide.core.execution.agent.event.SuiteEndedEvent;
import org.rf.ide.core.execution.agent.event.SuiteStartedEvent;
import org.rf.ide.core.execution.agent.event.TestEndedEvent;
import org.rf.ide.core.execution.agent.event.TestStartedEvent;
import org.rf.ide.core.execution.agent.event.VariablesEvent;
import org.rf.ide.core.execution.debug.StackFrame.FrameCategory;
import org.rf.ide.core.execution.debug.contexts.CaseContext;
import org.rf.ide.core.execution.debug.contexts.ForLoopContext;
import org.rf.ide.core.execution.debug.contexts.ForLoopIterationContext;
import org.rf.ide.core.execution.debug.contexts.KeywordContext;
import org.rf.ide.core.execution.debug.contexts.SuiteContext;
import org.rf.ide.core.testdata.model.table.keywords.names.QualifiedKeywordName;

public class StacktraceBuilder extends RobotDefaultAgentEventListener {

    private final Stacktrace stacktrace;

    private final ElementsLocator locator;

    private final RobotBreakpointSupplier breakpointSupplier;

    private final Set<URI> currentlyImportedResources = new LinkedHashSet<>();

    public StacktraceBuilder(final Stacktrace stacktrace, final ElementsLocator locator,
            final RobotBreakpointSupplier breakpointSupplier) {
        this.stacktrace = stacktrace;
        this.locator = locator;
        this.breakpointSupplier = breakpointSupplier;
    }

    @Override
    public void handleSuiteStarted(final SuiteStartedEvent event) {
        final URI currentPath = stacktrace.getPath(false).orElse(null);
        final String suiteName = event.getName();
        final URI suitePath = event.getPath();
        final boolean suiteIsDirectory = event.isDirectory();

        final SuiteContext context = locator.findContextForSuite(suiteName, suitePath, suiteIsDirectory, currentPath);
        stacktrace.push(
                new StackFrame(suiteName, FrameCategory.SUITE, stacktrace.size(), context, currentlyImportedResources));

        currentlyImportedResources.clear();
    }

    @Override
    public void handleTestStarted(final TestStartedEvent event) {
        final String testName = event.getName();
        final Optional<String> template = event.getTemplate();

        final URI path = stacktrace.getPath(false).orElse(null);
        final CaseContext context = locator.findContextForCase(testName, path, template);
        stacktrace.push(new StackFrame(testName, FrameCategory.TEST, stacktrace.size(), context));
    }

    @Override
    public void handleKeywordAboutToStart(final KeywordStartedEvent event) {
        final RunningKeyword keyword = event.getRunningKeyword();

        stacktrace.peekCurrentFrame().ifPresent(frame -> frame.moveToKeyword(keyword, breakpointSupplier));
    }

    @Override
    public void handleKeywordStarted(final KeywordStartedEvent event) {
        final String keywordName = event.getName();
        final String libraryName = event.getLibraryName();
        final RunningKeyword keyword = event.getRunningKeyword();

        final URI currentSuitePath = stacktrace.getPath(keyword.isSetup() || keyword.isTeardown()).orElse(null);
        
        final String frameNamePrefix;
        final FrameCategory category;
        final int level;
        final StackFrameContext context;
        final Supplier<URI> contextPathSupplier;
        if (keyword.isForLoop()) {
            final StackFrameContext currentContext = stacktrace.peekCurrentFrame().get().getContext();
            context = ForLoopContext.findContextForLoop(currentContext);
            category = FrameCategory.FOR;
            level = stacktrace.peekCurrentFrame().get().getLevel();
            frameNamePrefix = ":FOR ";
            contextPathSupplier = () -> null;

        } else if (stacktrace.hasCategoryOnTop(FrameCategory.FOR)) {
            final StackFrameContext currentContext = stacktrace.peekCurrentFrame().get().getContext();
            context = ForLoopIterationContext.findContextForLoopIteration(currentContext, keywordName);
            category = FrameCategory.FOR_ITEM;
            level = stacktrace.peekCurrentFrame().get().getLevel();
            frameNamePrefix = ":FOR iteration ";
            contextPathSupplier = () -> null;

        } else {
            final Set<URI> currentResources = stacktrace
                    .getFirstFrameSatisfying(StackFrame::isSuiteContext)
                    .get()
                    .getLoadedResources();
            context = locator.findContextForKeyword(libraryName, keywordName, currentSuitePath, currentResources);
            category = FrameCategory.KEYWORD;

            // library keyword have to point to variables taken from lowest test or suite if test
            // does not exist
            level = ((KeywordContext) context).isLibraryKeywordContext()
                    ? stacktrace.getFirstFrameSatisfying(or(StackFrame::isTestContext, StackFrame::isSuiteContext))
                            .map(StackFrame::getLevel)
                            .get()
                    : stacktrace.peekCurrentFrame().get().getLevel() + 1;
            frameNamePrefix = "";
            contextPathSupplier = () -> currentSuitePath;
        }

        final String frameName = frameNamePrefix + QualifiedKeywordName.asCall(keywordName, libraryName);
        stacktrace.push(new StackFrame(frameName, category, level, context, contextPathSupplier));
    }

    @Override
    public void handleKeywordAboutToEnd(final KeywordEndedEvent event) {
        stacktrace.pop();
    }

    @Override
    public void handleKeywordEnded(final KeywordEndedEvent event) {
        stacktrace.peekCurrentFrame().ifPresent(frame -> frame.moveOutOfKeyword());
    }

    @Override
    public void handleTestEnded(final TestEndedEvent event) {
        stacktrace.pop();
    }

    @Override
    public void handleSuiteEnded(final SuiteEndedEvent event) {
        stacktrace.pop();
    }

    @Override
    public void handleResourceImport(final ResourceImportEvent event) {
        if (event.isDynamicallyImported()) {
            stacktrace.getFirstFrameSatisfying(StackFrame::isSuiteContext).get().addLoadedResource(event.getPath());
        } else {
            // if resources is imported normally it is done prior to suite start
            currentlyImportedResources.add(event.getPath());
        }
    }

    @Override
    public void handleVariables(final VariablesEvent event) {
        stacktrace.updateVariables(event.getVariables());
    }

    @Override
    public void handleClosed() {
        stacktrace.destroy();
    }
}
