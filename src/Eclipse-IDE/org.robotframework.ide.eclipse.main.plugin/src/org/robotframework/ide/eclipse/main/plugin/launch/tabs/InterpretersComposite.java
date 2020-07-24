/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch.tabs;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.statushandlers.StatusManager;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder.PythonInstallationDirectory;
import org.rf.ide.core.environment.SuiteExecutor;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;

/**
 * @author Michal Anglart
 */
class InterpretersComposite extends Composite {

    private final InterpreterListener listener;

    private Button useProjectExecutorButton;

    private Button useSystemExecutorButton;

    private Combo comboExecutorName;

    private Button checkEnvironmentBtn;

    InterpretersComposite(final Composite parent, final InterpreterListener listener) {
        super(parent, SWT.NONE);
        this.listener = listener;

        GridLayoutFactory.fillDefaults().numColumns(4).spacing(2, 2).margins(0, 5).applyTo(this);

        createProjectInterpreterButton();
        createSystemInterpreterButton();
        createCheckEnvironmentButton();
    }

    private void createProjectInterpreterButton() {
        useProjectExecutorButton = new Button(this, SWT.RADIO);
        useProjectExecutorButton.setText("Use interpreter as defined in project configuration");
        GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(useProjectExecutorButton);
        useProjectExecutorButton.addSelectionListener(widgetSelectedAdapter(eevent -> {
            checkEnvironmentBtn.setEnabled(false);
            comboExecutorName.setEnabled(false);
            listener.interpreterChanged();
        }));
    }

    private void createSystemInterpreterButton() {
        useSystemExecutorButton = new Button(this, SWT.RADIO);
        useSystemExecutorButton.setText("Use");
        GridDataFactory.fillDefaults().applyTo(useSystemExecutorButton);
        useSystemExecutorButton.addSelectionListener(widgetSelectedAdapter(eevent -> {
            checkEnvironmentBtn.setEnabled(true);
            comboExecutorName.setEnabled(true);
            listener.interpreterChanged();
        }));
        comboExecutorName = new Combo(this, SWT.DROP_DOWN | SWT.READ_ONLY);
        comboExecutorName.setItems(Stream.of(SuiteExecutor.values()).map(SuiteExecutor::name).toArray(String[]::new));
        comboExecutorName.addModifyListener(e -> listener.interpreterChanged());
        GridDataFactory.fillDefaults().applyTo(comboExecutorName);
        final Label systemExecutorLbl = new Label(this, SWT.NONE);
        systemExecutorLbl.setText("interpreter taken from system PATH environment variable");
    }

    private void createCheckEnvironmentButton() {
        checkEnvironmentBtn = new Button(this, SWT.PUSH);
        checkEnvironmentBtn.setText("Check interpreter");
        GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(checkEnvironmentBtn);
        checkEnvironmentBtn.addSelectionListener(widgetSelectedAdapter(event -> {
            final SuiteExecutor interpreter = getChosenSystemExecutor();
            try {
                new ProgressMonitorDialog(getShell()).run(false, false, monitor -> {
                    final Optional<PythonInstallationDirectory> installation = PythonInstallationDirectoryFinder
                            .whereIsPythonInterpreter(interpreter);
                    if (installation.isPresent()) {
                        final Optional<String> robotVersion = installation
                                .flatMap(PythonInstallationDirectory::getRobotVersion);
                        if (robotVersion.isPresent()) {
                            MessageDialog.openInformation(getShell(), "Interpreter checked", "The " + interpreter.name()
                                    + " interpreter has " + robotVersion.get() + " installed");
                        } else {
                            MessageDialog.openWarning(getShell(), "Interpreter checked",
                                    "The " + interpreter.name() + " interpreter has no Robot installed");
                        }
                    } else {
                        MessageDialog.openError(getShell(), "Interpreter checked", "There is no " + interpreter.name()
                                + " interpreter in system PATH environment variable");
                    }
                });
            } catch (final InterruptedException e) {
                StatusManager.getManager()
                        .handle(new Status(IStatus.ERROR, RedPlugin.PLUGIN_ID, e.getMessage(), e), StatusManager.BLOCK);
            } catch (final InvocationTargetException e) {
                StatusManager.getManager()
                        .handle(new Status(IStatus.ERROR, RedPlugin.PLUGIN_ID,
                                "Unable to find " + interpreter.executableName() + " executable in the system.",
                                e.getTargetException()), StatusManager.BLOCK);
            }
        }));
    }

    void setInput(final boolean usesProjectInterpreter, final SuiteExecutor executor) {
        useProjectExecutorButton.setSelection(usesProjectInterpreter);
        useSystemExecutorButton.setSelection(!usesProjectInterpreter);
        comboExecutorName.setEnabled(!usesProjectInterpreter);
        checkEnvironmentBtn.setEnabled(!usesProjectInterpreter);

        final String executorName = executor != null ? executor.name() : SuiteExecutor.Python.name();
        comboExecutorName.select(Arrays.asList(comboExecutorName.getItems()).indexOf(executorName));
    }

    boolean isUsingProjectInterpreter() {
        return useProjectExecutorButton.getSelection();
    }

    SuiteExecutor getChosenSystemExecutor() {
        return SuiteExecutor.valueOf(comboExecutorName.getItem(comboExecutorName.getSelectionIndex()));
    }

    @FunctionalInterface
    public interface InterpreterListener {

        void interpreterChanged();
    }
}
