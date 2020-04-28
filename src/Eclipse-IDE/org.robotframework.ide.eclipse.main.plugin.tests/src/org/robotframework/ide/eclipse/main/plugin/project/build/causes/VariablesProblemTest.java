/*
 * Copyright 2019 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.causes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.junit.jupiter.api.Test;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.ChangeToFixer;

/**
 * @author lwlodarc
 */
public class VariablesProblemTest {

    @Test
    public void variableElementOldUseHasFix() {
        final IMarker marker = mock(IMarker.class);
        when(marker.getAttribute(AdditionalMarkerAttributes.VALUE, null)).thenReturn("@{list}[id]");
        final List<? extends IMarkerResolution> fixers = VariablesProblem.VARIABLE_ELEMENT_OLD_USE.createFixers(marker);
        assertThat(fixers).hasSize(1);
        assertThat(fixers.get(0)).isInstanceOf(ChangeToFixer.class);
    }

    @Test
    public void variableElementOldUseFixerCanFixProblem() {
        final IMarker marker = mock(IMarker.class);
        when(marker.getAttribute(AdditionalMarkerAttributes.VALUE, null)).thenReturn("@{list}[id]");
        final List<? extends IMarkerResolution> fixers = VariablesProblem.VARIABLE_ELEMENT_OLD_USE.createFixers(marker);
        assertThat(fixers).extracting(IMarkerResolution::getLabel).containsExactly("Change to '${list}[id]'");
    }

    @Test
    public void variableSyntaxProblemHasResolutionAndProvidesAFixer() {
        final IMarker marker = mock(IMarker.class);
        when(marker.getAttribute(AdditionalMarkerAttributes.NAME, "")).thenReturn("${x}");

        final VariablesProblem problem = VariablesProblem.INVALID_SYNTAX_USE;
        
        assertThat(problem.hasResolution()).isTrue();
        
        final List<? extends IMarkerResolution> fixers = problem.createFixers(marker);
        assertThat(fixers).hasSize(1);

        final IMarkerResolution fixer = fixers.get(0);
        assertThat(fixer).isInstanceOf(ChangeToFixer.class);
        assertThat(fixer.getLabel()).isEqualTo("Change to '${x}'");
    }
}
