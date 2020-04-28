/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.debug;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.rf.ide.core.execution.agent.PausingPoint;
import org.rf.ide.core.execution.agent.event.ConditionEvaluatedEvent;
import org.rf.ide.core.execution.debug.StackFrame.FrameCategory;
import org.rf.ide.core.execution.server.response.ChangeVariable;
import org.rf.ide.core.execution.server.response.EvaluateCondition;
import org.rf.ide.core.execution.server.response.EvaluateExpression;
import org.rf.ide.core.execution.server.response.PauseExecution;
import org.rf.ide.core.execution.server.response.ServerResponse;
import org.rf.ide.core.testdata.model.table.keywords.names.QualifiedKeywordName;

import com.google.common.annotations.VisibleForTesting;

public class UserProcessDebugController extends UserProcessController {

    private final DebuggerPreferences preferences;
    private final Stacktrace stacktrace;

    private final List<PauseReasonListener> pauseListeners = new ArrayList<>();

    private PausingPoint lastPausingPoint;
    private SuspensionData suspensionData = null;

    public UserProcessDebugController(final Stacktrace stacktrace, final DebuggerPreferences preferences) {
        this.stacktrace = stacktrace;
        this.preferences = preferences;
    }

    @VisibleForTesting
    void setLastPausingPoint(final PausingPoint lastPausingPoint) {
        this.lastPausingPoint = lastPausingPoint;
    }

    @VisibleForTesting
    void setSuspensionData(final SuspensionData suspensionData) {
        this.suspensionData = suspensionData;
    }

    @VisibleForTesting
    SuspensionData getSuspensionData() {
        return suspensionData;
    }

    public void whenSuspended(final PauseReasonListener listener) {
        this.pauseListeners.add(listener);
    }

    public boolean isStepping() {
        return suspensionData != null && suspensionData.reason == SuspendReason.STEPPING;
    }

    @Override
    public void conditionEvaluated(final ConditionEvaluatedEvent event) {
        if (!event.getResult().orElse(true)) {
            // condition was evaluated to false so there will be no suspension; otherwise (true or error)
            // execution will pause
            suspensionData = null;
        }
    }

    @Override
    public void executionPaused() {
        if (suspensionData.reason == SuspendReason.BREAKPOINT) {
            final RobotBreakpoint breakpoint = (RobotBreakpoint) suspensionData.data[0];
            pauseListeners.stream().forEach(listener -> listener.pausedOnBreakpoint(breakpoint));

        } else if (suspensionData.reason == SuspendReason.USER_REQUEST) {
            pauseListeners.stream().forEach(listener -> listener.pausedByUser());

        } else if (suspensionData.reason == SuspendReason.STEPPING) {
            pauseListeners.stream().forEach(listener -> listener.pausedByStepping());

        } else if (suspensionData.reason == SuspendReason.VARIABLE_CHANGE) {
            pauseListeners.stream().forEach(listener -> listener.pausedAfterVariableChange());

        } else if (suspensionData.reason == SuspendReason.ERRONEOUS_STATE) {
            final String error = (String) suspensionData.data[0];
            pauseListeners.stream().forEach(listener -> listener.pausedOnError(error));

        } else if (suspensionData.reason == SuspendReason.EXPRESSION_EVALUATED) {
            pauseListeners.stream().forEach(listener -> listener.pausedAfterExpressionEvaluated());
        }
        suspensionData = null;
        frames().forEach(frame -> frame.unmark(StackFrameMarker.STEPPING));
    }

    @Override
    public Optional<ServerResponse> takeCurrentResponse(final PausingPoint pausingPoint,
            final QualifiedKeywordName currentlyFailedKeyword) {
        this.lastPausingPoint = pausingPoint;
        return super.takeCurrentResponse(pausingPoint, currentlyFailedKeyword).map(Optional::of)
                .orElseGet(() -> pauseOnErrorResponse(pausingPoint)).map(Optional::of)
                .orElseGet(() -> breakpointHitResponse(pausingPoint, currentlyFailedKeyword)).map(Optional::of)
                .orElseGet(() -> userSteppingResponse(pausingPoint));
    }

    private Optional<ServerResponse> pauseOnErrorResponse(final PausingPoint pausingPoint) {
        if ((pausingPoint == PausingPoint.PRE_START_KEYWORD || pausingPoint == PausingPoint.START_KEYWORD)
                && frames().map(StackFrame::getContext).anyMatch(StackFrameContext::isErroneous)
                && !frames().anyMatch(StackFrame::isMarkedError)) {

            // we mark all the erroneous frames; once they will be popped from stack it may
            // again suspend
            frames().filter(frame -> frame.getContext().isErroneous())
                    .forEach(frame -> frame.mark(StackFrameMarker.ERROR));

            // this may require user assistance, so has to be asked as the last condition after
            // those in previous if
            if (preferences.shouldPauseOnError()) {

                final String error = frames()
                        .findFirst()
                        .map(StackFrame::getContext)
                        .flatMap(StackFrameContext::getErrorMessage)
                        .orElse("");
                suspensionData = new SuspensionData(SuspendReason.ERRONEOUS_STATE, error);
                return Optional.of(new PauseExecution());
            }
        }
        return Optional.empty();
    }

    private Optional<ServerResponse> breakpointHitResponse(final PausingPoint pausingPoint,
            final QualifiedKeywordName currentlyFailedKeyword) {

        Optional<RobotBreakpoint> breakpoint;
        if (pausingPoint == PausingPoint.PRE_START_KEYWORD) {
            breakpoint = frames().findFirst().flatMap(frame -> frame.getLineBreakpoint());

        } else if (pausingPoint == PausingPoint.PRE_END_KEYWORD && currentlyFailedKeyword != null) {
            breakpoint = frames().findFirst().flatMap(frame -> frame.getKeywordFailBreakpoint(currentlyFailedKeyword));

        } else {
            breakpoint = Optional.empty();
        }

        if (breakpoint.isPresent()) {
            final RobotBreakpoint robotBreakpoint = breakpoint.get();

            if (robotBreakpoint.evaluateHitCount()) {
                suspensionData = new SuspensionData(SuspendReason.BREAKPOINT, robotBreakpoint);

                if (robotBreakpoint.isConditionEnabled()) {
                    return Optional.of(new EvaluateCondition(robotBreakpoint.getCondition()));
                } else {
                    return Optional.of(new PauseExecution());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<ServerResponse> userSteppingResponse(final PausingPoint pausingPoint) {
        if (shouldPauseWhenStepping(pausingPoint)) {
            ((Runnable) suspensionData.data[1]).run();
            return Optional.of(new PauseExecution());
        } else {
            return Optional.empty();
        }
    }

    private boolean shouldPauseWhenStepping(final PausingPoint pausingPoint) {
        final SteppingMode mode = suspensionData == null ? null : (SteppingMode) suspensionData.data[0];
        return mode == SteppingMode.INTO && shouldPauseOnStepInto(pausingPoint)
                || mode == SteppingMode.OVER && shouldPauseOnStepOver(pausingPoint)
                || mode == SteppingMode.RETURN && shouldPauseOnStepReturn(pausingPoint);
    }

    private boolean shouldPauseOnStepInto(final PausingPoint pausingPoint) {
        if (stacktrace.hasCategoryOnTop(FrameCategory.FOR)) {
            // never step into when FOR is on top; will stop on for-iteration
            return false;
        }

        if (preferences.shouldGoIntoLibKeywords()) {
            // we'll not pause when checking at END_KEYWORD, since there is nothing to step into
            return pausingPoint != PausingPoint.END_KEYWORD;
        } else {
            return EnumSet.of(PausingPoint.PRE_START_KEYWORD, PausingPoint.START_KEYWORD).contains(pausingPoint)
                    && !topFrame().isLibraryKeywordFrame();
        }
    }

    private boolean shouldPauseOnStepOver(final PausingPoint pausingPoint) {
        if (pausingPoint == PausingPoint.START_KEYWORD) {
            // we pause on start keyword when in for-iterations
            return stacktrace.hasCategoryOnTop(FrameCategory.FOR_ITEM)
                    && (frames().skip(1).findFirst().filter(StackFrame::isMarkedStepping).isPresent()
                            || !frames().anyMatch(StackFrame::isMarkedStepping));

        } else if (pausingPoint == PausingPoint.PRE_START_KEYWORD) {
            // pause on pre-start keyword when outside of loops
            return !stacktrace.hasCategoryOnTop(FrameCategory.FOR)
                    && (topFrame().isMarkedStepping()
                            || !frames().anyMatch(StackFrame::isMarkedStepping));
        }
        return false;
    }

    private boolean shouldPauseOnStepReturn(final PausingPoint pausingPoint) {
        // we'll pause execution in STEPPING-RETURN whenever marked frame is already gone
        if (preferences.shouldGoIntoLibKeywords()) {
            return EnumSet.of(PausingPoint.PRE_START_KEYWORD, PausingPoint.PRE_END_KEYWORD).contains(pausingPoint)
                    && !frames().anyMatch(StackFrame::isMarkedStepping);
        } else {
            return EnumSet.of(PausingPoint.PRE_START_KEYWORD, PausingPoint.PRE_END_KEYWORD).contains(pausingPoint)
                    && !topFrame().isLibraryKeywordFrame()
                    && !frames().anyMatch(StackFrame::isMarkedStepping);
        }
    }

    private StackFrame topFrame() {
        return stacktrace.peekCurrentFrame().get();
    }

    private Stream<StackFrame> frames() {
        return stacktrace.stream();
    }

    @Override
    public void pause(final Runnable whenResponseIsSent) {
        super.pause(whenResponseIsSent);
        suspensionData = new SuspensionData(SuspendReason.USER_REQUEST);
    }

    public void stepInto(final Runnable whenResponseIsSent, final Runnable whenSteppingEnds) {
        // it's not possible to step into frame other than the one on stack top
        step(SteppingMode.INTO, whenResponseIsSent, whenSteppingEnds);
    }

    public void stepOver(final StackFrame frameToStepOver, final Runnable whenResponseIsSent,
            final Runnable whenSteppingEnds) {
        if (lastPausingPoint == PausingPoint.START_KEYWORD) {
            final StackFrame parentFrame = stacktrace.findParentFrame(frameToStepOver);
            parentFrame.mark(StackFrameMarker.STEPPING);
        } else {
            frameToStepOver.mark(StackFrameMarker.STEPPING);
        }
        step(SteppingMode.OVER, whenResponseIsSent, whenSteppingEnds);
    }

    public void stepReturn(final StackFrame frameToStepReturn, final Runnable whenResponseIsSent,
            final Runnable whenSteppingEnds) {
        frameToStepReturn.mark(StackFrameMarker.STEPPING);
        step(SteppingMode.RETURN, whenResponseIsSent, whenSteppingEnds);
    }

    private void step(final SteppingMode mode, final Runnable whenResponseIsSent, final Runnable whenSteppingEnds) {
        suspensionData = new SuspensionData(SuspendReason.STEPPING, mode, whenSteppingEnds);

        resume(whenResponseIsSent);
    }

    public void changeVariable(final StackFrame frame, final StackFrameVariable variable,
            final List<String> arguments) {
        final ChangeVariable changeVarResponse = new ChangeVariable(variable.getName(), variable.getScope(),
                frame.getLevel(), arguments);
        suspensionData = new SuspensionData(SuspendReason.VARIABLE_CHANGE);
        offer(new ResponseWithCallback(changeVarResponse, () -> {}));
    }

    public void changeVariableInnerValue(final StackFrame frame, final StackFrameVariable variable,
            final List<Object> path, final List<String> arguments) {
        final ChangeVariable changeVarResponse = new ChangeVariable(variable.getName(), variable.getScope(),
                frame.getLevel(), path, arguments);
        suspensionData = new SuspensionData(SuspendReason.VARIABLE_CHANGE);
        offer(new ResponseWithCallback(changeVarResponse, () -> {}));
    }

    public void evaluateRobotKeywordCall(final int exprId, final String keywordName, final List<String> arguments) {
        final EvaluateExpression evalResponse = EvaluateExpression.robot(exprId, keywordName, arguments);
        suspensionData = new SuspensionData(SuspendReason.EXPRESSION_EVALUATED);
        offer(new ResponseWithCallback(evalResponse, () -> {}));
    }

    public void evaluateRobotVariable(final int exprId, final String variable) {
        final EvaluateExpression evalResponse = EvaluateExpression.variable(exprId, variable);
        suspensionData = new SuspensionData(SuspendReason.EXPRESSION_EVALUATED);
        offer(new ResponseWithCallback(evalResponse, () -> {}));
    }

    public void evaluatePythonExpression(final int exprId, final String expression) {
        final EvaluateExpression evalResponse = EvaluateExpression.python(exprId, expression);
        suspensionData = new SuspensionData(SuspendReason.EXPRESSION_EVALUATED);
        offer(new ResponseWithCallback(evalResponse, () -> {}));
    }

    public static interface PauseReasonListener {

        public void pausedOnBreakpoint(RobotBreakpoint breakpoint);

        public void pausedByUser();

        public void pausedByStepping();

        public void pausedOnError(String error);

        public void pausedAfterVariableChange();

        public void pausedAfterExpressionEvaluated();
    }

    public static class DebuggerPreferences {

        private final Supplier<Boolean> pauseOnError;

        private final boolean goIntoLibKeyword;

        public DebuggerPreferences(final Supplier<Boolean> pauseOnError, final boolean goIntoLibKeyword) {
            this.pauseOnError = pauseOnError;
            this.goIntoLibKeyword = goIntoLibKeyword;
        }

        public boolean shouldPauseOnError() {
            return pauseOnError.get();
        }

        public boolean shouldGoIntoLibKeywords() {
            return goIntoLibKeyword;
        }
    }

    @VisibleForTesting
    public static class SuspensionData {

        public SuspendReason reason;

        public Object[] data;

        public SuspensionData(final SuspendReason reason, final Object... data) {
            this.reason = reason;
            this.data = data;
        }
    }

    @VisibleForTesting
    public enum SuspendReason {
        USER_REQUEST, BREAKPOINT, STEPPING, VARIABLE_CHANGE, EXPRESSION_EVALUATED, ERRONEOUS_STATE
    }

    @VisibleForTesting
    public enum SteppingMode {
        INTO, OVER, RETURN
    }
}
