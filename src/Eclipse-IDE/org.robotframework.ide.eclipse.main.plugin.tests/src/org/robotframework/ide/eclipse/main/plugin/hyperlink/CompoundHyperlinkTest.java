/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.hyperlink;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.jface.text.Region;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;

public class CompoundHyperlinkTest {

    @Test
    public void testCompoundHyperlinkProperties() {
        final List<RedHyperlink> links = newArrayList(mockHyperlink(), mockHyperlink());

        final CompoundHyperlink link = new CompoundHyperlink("name", new Region(20, 50), links, "Link label");
        assertThat(link.getTypeLabel()).isNull();
        assertThat(link.getHyperlinkRegion()).isEqualTo(new Region(20, 50));
        assertThat(link.getHyperlinkText()).isEqualTo("Link label");
    }

    @Test
    public void testIfPopupOpensCorrectly() {
        final Display display = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getDisplay();
        assertThat(display.getShells()).extracting(Shell::getText).doesNotContain(HyperlinkDialog.POPUP_TEXT);

        final List<RedHyperlink> links = newArrayList(mockHyperlink(), mockHyperlink());

        final CompoundHyperlink link = new CompoundHyperlink("name", new Region(20, 50), links, "Link label");
        link.open();
        assertThat(display.getShells()).extracting(Shell::getText).contains(HyperlinkDialog.POPUP_TEXT);

        for (final Shell shell : display.getShells()) {
            if (shell.getText().equals(HyperlinkDialog.POPUP_TEXT)) {
                shell.close();
            }
        }
    }

    private static RedHyperlink mockHyperlink() {
        final RedHyperlink mock = mock(RedHyperlink.class);
        when(mock.getLabelForCompoundHyperlinksDialog()).thenReturn("");
        when(mock.additionalLabelDecoration()).thenReturn("");
        return mock;
    }
}
