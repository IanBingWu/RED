/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.rf.ide.core.validation.ProblemPosition;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.IProblemCause;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ProblemCategory.Severity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Range;

public class RobotProblem {

    public static final String TYPE_ID = RedPlugin.PLUGIN_ID + ".robotProblem";

    private static final String CAUSE_ENUM_CLASS = "class";

    private static final String CAUSE_ATTRIBUTE = "cause";

    private final IProblemCause cause;

    private String message;
    private Object[] objects;

    public static IRegion getRegionOf(final IMarker marker) {
        final int start = marker.getAttribute(IMarker.CHAR_START, -1);
        final int end = marker.getAttribute(IMarker.CHAR_END, -1);

        return new Region(start, end - start);
    }

    public static Range<Integer> getRangeOf(final IMarker marker) {
        try {
            return Range.closed((Integer) marker.getAttribute(IMarker.CHAR_START),
                    (Integer) marker.getAttribute(IMarker.CHAR_END));
        } catch (final CoreException e) {
            throw new IllegalStateException("Given marker should have offsets defined", e);
        }
    }

    public static Range<Integer> getRangeOf(final IMarker marker, final int defaultValue) {
        return Range.closed((Integer) marker.getAttribute(IMarker.CHAR_START, defaultValue),
                (Integer) marker.getAttribute(IMarker.CHAR_END, defaultValue));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static IProblemCause getCause(final IMarker marker) {
        final String causeEnumClass = marker.getAttribute(CAUSE_ENUM_CLASS, null);
        final String causeStr = marker.getAttribute(CAUSE_ATTRIBUTE, null);
        if (causeEnumClass != null && causeStr != null) {
            try {
                return (IProblemCause) Enum.valueOf((Class<? extends Enum>) Class.forName(causeEnumClass), causeStr);
            } catch (final ClassNotFoundException | IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    public static String getCauseName(final IMarker marker) {
        return marker.getAttribute(CAUSE_ATTRIBUTE, null);
    }

    public static RobotProblem causedBy(final IProblemCause cause) {
        return new RobotProblem(cause);
    }

    private RobotProblem(final IProblemCause cause) {
        this.cause = cause;
        this.objects = null;
    }

    @VisibleForTesting
    public IProblemCause getCause() {
        return cause;
    }

    public RobotProblem withMessage(final String message) {
        this.message = message;
        return this;
    }

    public RobotProblem formatMessageWith(final Object... objects) {
        this.objects = objects;
        return this;
    }

    public void createMarker(final IFile file, final ProblemPosition position,
            final Map<String, Object> additionalAttributes) {
        try {
            final IMarker marker = file.createMarker(TYPE_ID);
            marker.setAttribute(IMarker.MESSAGE, getMessage().intern());
            marker.setAttribute(IMarker.SEVERITY, getSeverity().getLevel());
            if (position.getLine() >= 0) {
                marker.setAttribute(IMarker.LOCATION, ("line " + position.getLine()).intern());
                marker.setAttribute(IMarker.LINE_NUMBER, position.getLine());
            } else {
                marker.setAttribute(IMarker.LOCATION, "unknown line".intern());
            }
            if (position.getRange().isPresent() && position.getRange().get().hasLowerBound()
                    && position.getRange().get().hasUpperBound()) {
                marker.setAttribute(IMarker.CHAR_START, position.getRange().get().lowerEndpoint());
                marker.setAttribute(IMarker.CHAR_END, position.getRange().get().upperEndpoint());
            }

            marker.setAttribute(CAUSE_ENUM_CLASS, cause.getEnumClassName().intern());
            marker.setAttribute(CAUSE_ATTRIBUTE, cause.toString().intern());
            for (final Entry<String, Object> entry : additionalAttributes.entrySet()) {
                Object toPut = entry.getValue();
                if (entry.getValue() instanceof String) {
                    toPut = ((String) entry.getValue()).intern();
                }
                marker.setAttribute(entry.getKey(), toPut);
            }
        } catch (final CoreException e) {
            throw new IllegalStateException("Unable to create marker!", e);
        }
    }

    public String getMessage() {
        final String msgToUse = Stream.of(message, cause.getProblemDescription())
                .filter(msg -> msg != null)
                .findFirst()
                .orElse("");
        return String.format(msgToUse, objects == null ? new Object[0] : objects);
    }

    public Severity getSeverity() {
        return cause.getProblemCategory().getSeverity();
    }
}
