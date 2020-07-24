/*
* Copyright 2017 Nokia Solutions and Networks
* Licensed under the Apache License, Version 2.0,
* see license.txt file for details.
*/
package org.rf.ide.core.execution.debug;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.rf.ide.core.execution.agent.Status;
import org.rf.ide.core.execution.agent.event.KeywordEndedEvent;
import org.rf.ide.core.execution.agent.event.KeywordStartedEvent;
import org.rf.ide.core.execution.agent.event.ResourceImportEvent;
import org.rf.ide.core.execution.agent.event.SuiteEndedEvent;
import org.rf.ide.core.execution.agent.event.SuiteStartedEvent;
import org.rf.ide.core.execution.agent.event.SuiteStartedEvent.ExecutionMode;
import org.rf.ide.core.execution.agent.event.TestEndedEvent;
import org.rf.ide.core.execution.agent.event.TestStartedEvent;
import org.rf.ide.core.execution.agent.event.Variable;
import org.rf.ide.core.execution.agent.event.VariableTypedValue;
import org.rf.ide.core.execution.agent.event.VariablesEvent;
import org.rf.ide.core.execution.agent.event.VersionsEvent;
import org.rf.ide.core.execution.debug.StackFrame.FrameCategory;
import org.rf.ide.core.execution.debug.contexts.CaseContext;
import org.rf.ide.core.execution.debug.contexts.KeywordContext;
import org.rf.ide.core.execution.debug.contexts.SuiteContext;

public class StacktraceBuilderTest {

    @Test
    public void frameForSuiteIsCreatedWithCurrentlyImportedResources_whenSuiteStarts() {
        final SuiteContext context = mock(SuiteContext.class);

        final ElementsLocator locator = mock(ElementsLocator.class);
        when(locator.findContextForSuite("Suite", URI.create("file:///suite.robot"), false, null)).thenReturn(context);
        final Stacktrace stack = new Stacktrace();

        final StacktraceBuilder builder = new StacktraceBuilder(stack, locator, new RobotBreakpointSupplier());

        builder.handleResourceImport(
                new ResourceImportEvent(URI.create("file:///res1.robot"), URI.create("file:///suite.robot")));
        builder.handleResourceImport(
                new ResourceImportEvent(URI.create("file:///res2.robot"), URI.create("file:///suite.robot")));
        builder.handleSuiteStarted(new SuiteStartedEvent("Suite", URI.create("file:///suite.robot"), false,
                ExecutionMode.TESTS, 2, newArrayList(), newArrayList("t1", "t2"), newArrayList()));

        assertThat(stack.size()).isEqualTo(1);
        final StackFrame frame = stack.peekCurrentFrame().get();

        assertThat(frame.getName()).isEqualTo("Suite");
        assertThat(frame.getContext()).isSameAs(context);
        assertThat(frame.isSuiteContext()).isTrue();
        assertThat(frame.getLevel()).isEqualTo(0);
        assertThat(frame.getLoadedResources()).containsOnly(URI.create("file:///res1.robot"),
                URI.create("file:///res2.robot"));

        builder.handleSuiteEnded(new SuiteEndedEvent("Suite", 100, Status.PASS, ""));
        assertThat(stack.size()).isEqualTo(0);
    }

    @Test
    public void frameForSuiteIsCreatedWithCurrentlyImportedResources_whenAnotherSuiteStarts() {
        final SuiteContext context = mock(SuiteContext.class);

        final ElementsLocator locator = mock(ElementsLocator.class);
        when(locator.findContextForSuite("Suite", URI.create("file:///suite"), true, null))
                .thenReturn(mock(SuiteContext.class));
        when(locator.findContextForSuite("Inner", URI.create("file:///inner.robot"), false, null)).thenReturn(context);
        final Stacktrace stack = new Stacktrace();

        final StacktraceBuilder builder = new StacktraceBuilder(stack, locator, new RobotBreakpointSupplier());

        builder.handleResourceImport(
                new ResourceImportEvent(URI.create("file:///res1.robot"), URI.create("file:///suite")));
        builder.handleResourceImport(
                new ResourceImportEvent(URI.create("file:///res2.robot"), URI.create("file:///suite")));
        builder.handleSuiteStarted(new SuiteStartedEvent("Suite", URI.create("file:///suite"), true,
                ExecutionMode.TESTS, 2, newArrayList("Inner"), newArrayList(), newArrayList()));
        builder.handleResourceImport(
                new ResourceImportEvent(URI.create("file:///inner_res1.robot"), URI.create("file:///inner.robot")));
        builder.handleResourceImport(
                new ResourceImportEvent(URI.create("file:///inner_res2.robot"), URI.create("file:///inner.robot")));
        builder.handleSuiteStarted(new SuiteStartedEvent("Inner", URI.create("file:///inner.robot"), false,
                ExecutionMode.TESTS, 2, newArrayList(), newArrayList("t1", "t2"), newArrayList()));

        assertThat(stack.size()).isEqualTo(2);
        final StackFrame frame = stack.peekCurrentFrame().get();

        assertThat(frame.getName()).isEqualTo("Inner");
        assertThat(frame.getContext()).isSameAs(context);
        assertThat(frame.isSuiteContext()).isTrue();
        assertThat(frame.getLevel()).isEqualTo(1);
        assertThat(frame.getLoadedResources()).containsOnly(URI.create("file:///inner_res1.robot"),
                URI.create("file:///inner_res2.robot"));

        builder.handleSuiteEnded(new SuiteEndedEvent("Suite", 100, Status.PASS, ""));
        assertThat(stack.size()).isEqualTo(1);
    }

    @Test
    public void frameForTestIsCreated_whenTestStarts() {
        final CaseContext context = mock(CaseContext.class);

        final ElementsLocator locator = mock(ElementsLocator.class);
        when(locator.findContextForCase("test", null, Optional.empty())).thenReturn(context);

        final Stacktrace stack = new Stacktrace();
        stack.push(new StackFrame("Suite", FrameCategory.SUITE, 0, mock(StackFrameContext.class)));

        final StacktraceBuilder builder = new StacktraceBuilder(stack, locator, new RobotBreakpointSupplier());
        builder.handleTestStarted(new TestStartedEvent("test", "test", "Suite.Test", null));

        assertThat(stack.size()).isEqualTo(2);
        final StackFrame frame = stack.peekCurrentFrame().get();

        assertThat(frame.getName()).isEqualTo("test");
        assertThat(frame.getContext()).isSameAs(context);
        assertThat(frame.isTestContext()).isTrue();
        assertThat(frame.getLevel()).isEqualTo(1);

        builder.handleTestEnded(new TestEndedEvent("test", "test", "Suite.Test", 100, Status.PASS, ""));
        assertThat(stack.size()).isEqualTo(1);
    }

    @Test
    public void currentFrameSwitchesContext_whenKeywordIsAboutToStart() {
        final RobotBreakpointSupplier breakpointsSupplier = new RobotBreakpointSupplier();

        final CaseContext newContext = mock(CaseContext.class);
        final StackFrameContext oldContext = mock(StackFrameContext.class);
        when(oldContext.moveTo(new RunningKeyword("lib", "kw", KeywordCallType.NORMAL_CALL), breakpointsSupplier))
                .thenReturn(newContext);
        when(newContext.previousContext()).thenReturn(oldContext);

        final Stacktrace stack = new Stacktrace();
        stack.push(new StackFrame("Test", FrameCategory.TEST, 1, oldContext));

        final StacktraceBuilder builder = new StacktraceBuilder(stack, mock(ElementsLocator.class),
                breakpointsSupplier);
        builder.handleVersions(new VersionsEvent(null, "", "", "3.0", 1, Optional.of(42L))); // has to be called to initialize types fixer
        builder.handleKeywordAboutToStart(new KeywordStartedEvent("kw", "keyword", "lib"));

        assertThat(stack.size()).isEqualTo(1);
        final StackFrame frame = stack.peekCurrentFrame().get();

        assertThat(frame.getContext()).isSameAs(newContext);
        assertThat(frame.isTestContext()).isTrue();
        assertThat(frame.getLevel()).isEqualTo(1);

        builder.handleKeywordEnded(new KeywordEndedEvent("lib", "kw", "keyword", Status.PASS));
        assertThat(stack.size()).isEqualTo(1);
        assertThat(frame.getContext()).isSameAs(oldContext);
        assertThat(frame.isTestContext()).isTrue();
        assertThat(frame.getLevel()).isEqualTo(1);
    }

    @Test
    public void frameForKeywordIsCreated_whenKeywordStarted_1() {
        final KeywordContext context = mock(KeywordContext.class);

        final ElementsLocator locator = mock(ElementsLocator.class);
        when(locator.findContextForKeyword("lib", "kw", null, newHashSet())).thenReturn(context);

        final Stacktrace stack = new Stacktrace();
        stack.push(new StackFrame("Suite", FrameCategory.SUITE, 2, mock(StackFrameContext.class)));
        stack.push(new StackFrame("Test", FrameCategory.TEST, 3, mock(StackFrameContext.class)));

        final StacktraceBuilder builder = new StacktraceBuilder(stack, locator, new RobotBreakpointSupplier());
        builder.handleVersions(new VersionsEvent(null, "", "", "3.0", 1, Optional.of(42L))); // has to be called to
                                                                           // initialize types fixer
        builder.handleKeywordStarted(new KeywordStartedEvent("kw", "keyword", "lib"));

        assertThat(stack.size()).isEqualTo(3);
        final StackFrame frame = stack.peekCurrentFrame().get();

        assertThat(frame.getName()).isEqualTo("lib.kw");
        assertThat(frame.getContext()).isSameAs(context);
        assertThat(frame.getLevel()).isEqualTo(4);

        builder.handleKeywordAboutToEnd(new KeywordEndedEvent("lib", "kw", "keyword", Status.PASS));
        assertThat(stack.size()).isEqualTo(2);
    }

    @Test
    public void frameForKeywordIsCreated_whenKeywordStarted_2() {
        final KeywordContext context = mock(KeywordContext.class);
        when(context.isLibraryKeywordContext()).thenReturn(true);

        final ElementsLocator locator = mock(ElementsLocator.class);
        when(locator.findContextForKeyword("lib", "kw", null, newHashSet())).thenReturn(context);

        final Stacktrace stack = new Stacktrace();
        stack.push(new StackFrame("Suite", FrameCategory.SUITE, 2, mock(StackFrameContext.class)));
        stack.push(new StackFrame("Test", FrameCategory.TEST, 3, mock(StackFrameContext.class)));

        final StacktraceBuilder builder = new StacktraceBuilder(stack, locator, new RobotBreakpointSupplier());
        builder.handleVersions(new VersionsEvent(null, "", "", "3.0", 1, Optional.of(42L))); // has to be called to initialize types fixer
        builder.handleKeywordStarted(new KeywordStartedEvent("kw", "keyword", "lib"));

        assertThat(stack.size()).isEqualTo(3);
        final StackFrame frame = stack.peekCurrentFrame().get();

        assertThat(frame.getName()).isEqualTo("lib.kw");
        assertThat(frame.getContext()).isSameAs(context);
        assertThat(frame.getLevel()).isEqualTo(3);

        builder.handleKeywordAboutToEnd(new KeywordEndedEvent("lib", "kw", "keyword", Status.PASS));
        assertThat(stack.size()).isEqualTo(2);
    }

    @Test
    public void dynamicallyLoadedResourceIsAddedToSuiteFrame() {
        final Stacktrace stack = new Stacktrace();
        stack.push(new StackFrame("Suite", FrameCategory.SUITE, 0, mock(StackFrameContext.class)));
        final StackFrame innerSuiteFrame = new StackFrame("Inner", FrameCategory.SUITE, 1,
                mock(StackFrameContext.class));
        stack.push(innerSuiteFrame);
        stack.push(new StackFrame("Test", FrameCategory.TEST, 2, mock(StackFrameContext.class)));
        stack.push(new StackFrame("keyword", FrameCategory.KEYWORD, 2, mock(StackFrameContext.class)));

        final StacktraceBuilder builder = new StacktraceBuilder(stack, mock(ElementsLocator.class),
                new RobotBreakpointSupplier());

        assertThat(stack.stream()).allSatisfy(f -> assertThat(f.getLoadedResources().isEmpty()));

        builder.handleResourceImport(new ResourceImportEvent(URI.create("file://res1.robot"), null));
        builder.handleResourceImport(new ResourceImportEvent(URI.create("file://res2.robot"), null));

        assertThat(innerSuiteFrame.getLoadedResources()).containsOnly(URI.create("file://res1.robot"),
                URI.create("file://res2.robot"));
        assertThat(stack.stream().filter(f -> f.getLoadedResources().isEmpty())).hasSize(3);
    }

    @Test
    public void variablesAreUpdated_whenVariablesEventIsHandled() {
        final Stacktrace stack = spy(new Stacktrace());

        final StacktraceBuilder builder = new StacktraceBuilder(stack, mock(ElementsLocator.class),
                new RobotBreakpointSupplier());

        final List<Map<Variable, VariableTypedValue>> variables = newArrayList(newHashMap());
        builder.handleVariables(new VariablesEvent(variables, ""));

        verify(stack).updateVariables(same(variables));
    }

    @Test
    public void stackIsDestroyed_whenExecutionEnds() {
        final Stacktrace stack = new Stacktrace();
        stack.push(new StackFrame("Suite", FrameCategory.SUITE, 0, mock(StackFrameContext.class)));
        stack.push(new StackFrame("Test", FrameCategory.TEST, 1, mock(StackFrameContext.class)));

        final StacktraceBuilder builder = new StacktraceBuilder(stack, mock(ElementsLocator.class),
                new RobotBreakpointSupplier());

        assertThat(stack.size()).isEqualTo(2);
        builder.handleClosed();
        assertThat(stack.size()).isEqualTo(0);
    }
}
