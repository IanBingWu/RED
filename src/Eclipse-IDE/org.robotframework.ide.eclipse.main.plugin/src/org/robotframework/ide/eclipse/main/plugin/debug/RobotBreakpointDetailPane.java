/*
 * Copyright 2019 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.debug;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.IDetailPane3;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.rf.ide.core.execution.debug.RobotBreakpoint;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.red.graphics.ColorsManager;
import org.robotframework.red.viewers.Selections;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;

public abstract class RobotBreakpointDetailPane implements IDetailPane3 {

    private final ListenerList<IPropertyListener> listenersList = new ListenerList<>();

    private boolean isInitializingValues = false;

    private boolean isDirty;

    private Button hitCountBtn;
    private Text hitCountTxt;
    private ControlDecoration hitCountDecoration;

    private RobotBreakpoint currentBreakpoint;

    @Override
    public void init(final IWorkbenchPartSite partSite) {
        isDirty = false;
        listenersList.clear();
    }

    protected RobotBreakpoint getCurrentBreakpoint() {
        return currentBreakpoint;
    }

    @Override
    public boolean setFocus() {
        return false;
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return false;
    }

    @Override
    public boolean isDirty() {
        return isDirty;
    }

    protected boolean isInitializingValues() {
        return isInitializingValues;
    }

    @Override
    public Control createControl(final Composite parent) {
        final Composite panel = new Composite(parent, SWT.NONE);
        panel.setBackground(panel.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        GridDataFactory.fillDefaults().grab(true, true).applyTo(panel);
        GridLayoutFactory.fillDefaults().numColumns(2).margins(3, 3).applyTo(panel);

        hitCountBtn = new Button(panel, SWT.CHECK);
        hitCountBtn.setText("Hit count:");
        hitCountBtn.addSelectionListener(widgetSelectedAdapter(e -> {
            if (!isInitializingValues) {
                hitCountTxt.setEnabled(hitCountBtn.getSelection());
                setDirty(true);
            }
        }));

        hitCountTxt = new Text(panel, SWT.BORDER);
        hitCountTxt.setEnabled(false);
        GridDataFactory.fillDefaults()
                .align(SWT.BEGINNING, SWT.CENTER)
                .grab(true, false)
                .indent(15, 0)
                .minSize(80, 20)
                .applyTo(hitCountTxt);
        hitCountTxt.addModifyListener(event -> {
            if (!isInitializingValues && validate()) {
                setDirty(true);
            }
        });

        createSpecificControls(panel);

        isDirty = false;

        return panel;
    }

    protected abstract void createSpecificControls(Composite panel);

    @Override
    public void display(final IStructuredSelection selection) {
        if (isDirty) {
            doSave(new NullProgressMonitor());
        }
        isInitializingValues = true;

        currentBreakpoint = selection != null && Selections.getElements(selection, RobotBreakpoint.class).size() == 1
                ? Selections.getSingleElement(selection, RobotBreakpoint.class)
                : null;

        if (getBreakpointClass().isInstance(currentBreakpoint)) {
            display(currentBreakpoint);
        } else {
            displayEmpty();
        }
        isInitializingValues = false;
    }

    protected abstract Class<? extends IBreakpoint> getBreakpointClass();

    protected void display(final RobotBreakpoint currentBreakpoint) {
        final boolean hitCountEnabled = currentBreakpoint.isHitCountEnabled();
        hitCountBtn.setEnabled(true);
        hitCountBtn.setSelection(hitCountEnabled);
        hitCountTxt.setEnabled(hitCountEnabled);
        hitCountTxt.setText(Integer.toString(currentBreakpoint.getHitCount()));
        hitCountTxt.setSelection(hitCountTxt.getText().length());

        validate();
    }

    protected void displayEmpty() {
        hitCountBtn.setEnabled(false);
        hitCountBtn.setSelection(false);
        hitCountTxt.setEnabled(false);
        hitCountTxt.setText("");

        validate();
    }

    private boolean validate() {
        hitCountDecoration = validateWithDecorations(hitCountTxt, hitCountDecoration,
                txt -> {
                    final Integer count = Ints.tryParse(txt.getText());
                    if (count == null || count < 1) {
                        throw new BreakpointValidationException(
                                "Hit count has to be a number greater than zero and less than 2^31");
                    }
                });
        return hitCountDecoration == null;
    }

    protected boolean isValid() {
        return hitCountDecoration == null;
    }

    @Override
    public void dispose() {
        listenersList.clear();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void doSaveAs() {
        // not allowed
    }

    @Override
    public void addPropertyListener(final IPropertyListener listener) {
        listenersList.add(listener);
    }

    @Override
    public void removePropertyListener(final IPropertyListener listener) {
        listenersList.remove(listener);
    }

    protected final <T extends Control> ControlDecoration validateWithDecorations(final T control,
            final ControlDecoration currentDecoration, final Consumer<T> validator) {
        if (currentDecoration != null) {
            currentDecoration.hide();
            currentDecoration.dispose();
        }

        try {
            // only validate if there is a breakpoint displayed now
            if (currentBreakpoint != null) {
                validator.accept(control);
            }

            control.setBackground(null);
            return null;

        } catch (final BreakpointValidationException e) {
            final ControlDecoration decoration = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
            decoration.setDescriptionText(e.getMessage());
            decoration.setImage(FieldDecorationRegistry.getDefault()
                    .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
                    .getImage());
            control.setBackground(ColorsManager.getColor(255, 0, 0));
            control.addDisposeListener(event -> decoration.dispose());
            return decoration;
        }
    }

    @VisibleForTesting
    void setDirty(final boolean dirty) {
        this.isDirty = dirty;
        for (final IPropertyListener listener : listenersList) {
            listener.propertyChanged(this, PROP_DIRTY);
        }
    }

    @Override
    public void doSave(final IProgressMonitor monitor) {
        if (!isValid()) {
            return;
        }

        if (markerExists()) {
            try {
                currentBreakpoint.setHitCountEnabled(hitCountBtn.getSelection());
                currentBreakpoint.setHitCount(getHitCount());

                doSaveSpecificAttributes(currentBreakpoint);

            } catch (final CoreException | BreakpointAttributeException e) {
                ErrorDialog.openError(hitCountTxt.getShell(), "Cannot save breakpoint", "Cannot save breakpoint",
                        new Status(IStatus.ERROR, RedPlugin.PLUGIN_ID, "Unable to set values of breakpoint"));
            }
        }
        setDirty(false);
    }

    private boolean markerExists() {
        final IBreakpoint eclipseBreakpoint = (IBreakpoint) currentBreakpoint;
        return eclipseBreakpoint != null && eclipseBreakpoint.getMarker() != null
                && eclipseBreakpoint.getMarker().exists();
    }

    private int getHitCount() {
        final String hitCountText = hitCountTxt.getText();
        final Integer parsed = Ints.tryParse(hitCountText);
        if (parsed != null && parsed >= 1) {
            return parsed;
        } else {
            hitCountTxt.setText("1");
            return 1;
        }
    }

    protected abstract void doSaveSpecificAttributes(RobotBreakpoint currentBreakpoint) throws CoreException;

    public static class BreakpointValidationException extends RuntimeException {

        private static final long serialVersionUID = 6544605591939988210L;
        
        public BreakpointValidationException(final String message) {
            super(message);
        }
    }

    public static class BreakpointAttributeException extends RuntimeException {

        private static final long serialVersionUID = -7444558359135125786L;

        public BreakpointAttributeException(final CoreException e) {
            super(e);
        }
    }
}
