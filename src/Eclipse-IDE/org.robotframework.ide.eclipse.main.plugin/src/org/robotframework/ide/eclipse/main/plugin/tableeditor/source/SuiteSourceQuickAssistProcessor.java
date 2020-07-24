/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.IProblemCause;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.EmptyCompletionProposal;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.RedSuiteMarkerResolution;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.RedXmlConfigMarkerResolution;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.QuickAssistProvider;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.RedCompletionProposal;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist.RedCompletionProposalAdapter;

import com.google.common.base.Predicates;
import com.google.common.collect.Range;

/**
 * @author Michal Anglart
 */
public class SuiteSourceQuickAssistProcessor implements IQuickAssistProcessor, ICompletionListener,
        ICompletionListenerExtension, ICompletionListenerExtension2 {

    private final RobotSuiteFile suiteModel;

    private final SourceViewer sourceViewer;

    private final List<QuickAssistProvider> assistProviders = new ArrayList<>();

    public SuiteSourceQuickAssistProcessor(final RobotSuiteFile fileModel, final ISourceViewer sourceViewer) {
        this.suiteModel = fileModel;
        this.sourceViewer = (SourceViewer) sourceViewer;
    }

    public void addAssistProviders(final Collection<? extends QuickAssistProvider> providers) {
        this.assistProviders.addAll(providers);
    }

    @Override
    public String getErrorMessage() {
        return null;
    }

    @Override
    public boolean canFix(final Annotation annotation) {
        if (annotation instanceof MarkerAnnotation) {
            try {
                final IMarker marker = ((MarkerAnnotation) annotation).getMarker();
                if (RobotProblem.TYPE_ID.equals(marker.getType())) {
                    final IProblemCause cause = RobotProblem.getCause(marker);
                    return cause != null && cause.hasResolution();
                }
            } catch (final CoreException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean canAssist(final IQuickAssistInvocationContext invocationContext) {
        return assistProviders.stream().anyMatch(provider -> provider.canAssist(invocationContext));
    }

    @Override
    public ICompletionProposal[] computeQuickAssistProposals(final IQuickAssistInvocationContext invocationContext) {
        final List<ICompletionProposal> proposals = new ArrayList<>();
        proposals.addAll(computeQuickFixes(invocationContext));
        proposals.addAll(computeQuickAssists(invocationContext));
        if (proposals.isEmpty()) {
            // add empty proposal for informative purpose
            proposals.add(
                    new EmptyCompletionProposal("No quick fix available", "No quick fix was found for this issue"));
        }
        return proposals.toArray(new ICompletionProposal[0]);
    }

    private List<ICompletionProposal> computeQuickAssists(
            final IQuickAssistInvocationContext invocationContext) {
        return assistProviders.stream()
                .flatMap(provider -> provider.computeQuickAssistProposals(suiteModel, invocationContext).stream())
                .collect(toList());
    }

    private List<ICompletionProposal> computeQuickFixes(final IQuickAssistInvocationContext invocationContext) {
        final Map<PositionedCauseName, List<ICompletionProposal>> proposals = new HashMap<>();

        final int offset = invocationContext.getOffset();
        final IDocument document = invocationContext.getSourceViewer().getDocument();

        final Iterator<?> annotations = invocationContext.getSourceViewer()
                .getAnnotationModel()
                .getAnnotationIterator();
        while (annotations.hasNext()) {
            final Annotation annotation = (Annotation) annotations.next();
            if (annotation instanceof MarkerAnnotation) {
                final MarkerAnnotation markerAnnotation = (MarkerAnnotation) annotation;
                final IMarker marker = markerAnnotation.getMarker();
                try {
                    if (RobotProblem.TYPE_ID.equals(marker.getType())) {
                        final IProblemCause cause = RobotProblem.getCause(marker);
                        final Range<Integer> range = getRange(marker);
                        if (cause.hasResolution() && isInvokedWithinAnnotationPosition(offset, range)) {
                            final List<ICompletionProposal> fixes = computeRobotProblemsFixes(document, marker, cause);
                            if (!fixes.isEmpty()) {
                                proposals.put(new PositionedCauseName(range, RobotProblem.getCauseName(marker)), fixes);
                            }
                        }
                    }
                } catch (final CoreException e) {
                    // ok let's go to next annotation
                }
            }
        }

        return proposals.keySet()
                .stream()
                .sorted(comparing(problemStartProximity(offset)).thenComparing(problemEndProximity(offset))
                        .thenComparing(causeName()))
                .flatMap(positionedCause -> proposals.get(positionedCause).stream())
                .collect(toList());
    }

    private static Function<PositionedCauseName, Integer> problemStartProximity(final int currentOffset) {
        return positionedCause -> currentOffset - positionedCause.range.lowerEndpoint().intValue();
    }

    private static Function<PositionedCauseName, Integer> problemEndProximity(final int currentOffset) {
        return positionedCause -> positionedCause.range.upperEndpoint().intValue() - currentOffset;
    }

    private static Function<PositionedCauseName, String> causeName() {
        return positionedCause -> positionedCause.causeName;
    }

    private static Range<Integer> getRange(final IMarker marker) {
        try {
            return RobotProblem.getRangeOf(marker);
        } catch (final IllegalStateException e) {
            return null;
        }
    }

    private List<ICompletionProposal> computeRobotProblemsFixes(
            final IDocument document, final IMarker marker, final IProblemCause cause) {

        final List<? extends IMarkerResolution> fixers = cause.createFixers(marker);
        final Stream<ICompletionProposal> redXmlRepairProposals = fixers.stream()
                .filter(RedXmlConfigMarkerResolution.class::isInstance)
                .map(RedXmlConfigMarkerResolution.class::cast)
                .map(resolution -> resolution.asContentProposal(marker));
        final Stream<ICompletionProposal> suiteRepairProposals = fixers.stream()
                .filter(RedSuiteMarkerResolution.class::isInstance)
                .map(RedSuiteMarkerResolution.class::cast)
                .map(resolution -> resolution.asContentProposal(marker, document, suiteModel).orElse(null));
        return Stream.concat(redXmlRepairProposals, suiteRepairProposals)
                .filter(Predicates.notNull())
                .collect(toList());
    }

    private boolean isInvokedWithinAnnotationPosition(final int invocationOffset,
            final Range<Integer> annotationRange) {
        return annotationRange != null && annotationRange.contains(invocationOffset);
    }

    @Override
    public void applied(final ICompletionProposal proposal) {
        // this method is called also for processors from which the proposal was not chosen
        // hence canReopenAssistantProgramatically is holding information which processor
        // is able to open proposals after accepting
        if (shouldActivateAssist(proposal)) {
            Display.getCurrent().asyncExec(() -> sourceViewer.doOperation(ISourceViewer.CONTENTASSIST_PROPOSALS));
        }
        getOperationsAfterAccept(proposal).forEach(Runnable::run);
    }

    private boolean shouldActivateAssist(final ICompletionProposal proposal) {
        return proposal instanceof RedCompletionProposal
                && ((RedCompletionProposal) proposal).shouldActivateAssistantAfterAccepting()
                || proposal instanceof RedCompletionProposalAdapter
                        && ((RedCompletionProposalAdapter) proposal).shouldActivateAssistantAfterAccepting();
    }

    private Collection<Runnable> getOperationsAfterAccept(final ICompletionProposal proposal) {
        if (proposal instanceof RedCompletionProposal) {
            return ((RedCompletionProposal) proposal).operationsToPerformAfterAccepting();
        } else if (proposal instanceof RedCompletionProposalAdapter) {
            return ((RedCompletionProposalAdapter) proposal).operationsToPerformAfterAccepting();
        }
        return new ArrayList<>();
    }

    @Override
    public void assistSessionEnded(final ContentAssistEvent event) {
        // nothing to do
    }

    @Override
    public void assistSessionStarted(final ContentAssistEvent event) {
        // nothing to do
    }

    @Override
    public void assistSessionRestarted(final ContentAssistEvent event) {
        // nothing to do
    }

    @Override
    public void selectionChanged(final ICompletionProposal proposal, final boolean smartToggle) {
        // nothing to do
    }

    private static class PositionedCauseName {

        private final Range<Integer> range;

        private final String causeName;

        public PositionedCauseName(final Range<Integer> range, final String causeName) {
            this.range = range;
            this.causeName = causeName;
        }
    }
}
