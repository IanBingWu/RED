/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.general;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.services.IDirtyProviderService;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.Section;
import org.rf.ide.core.environment.IRuntimeEnvironment;
import org.rf.ide.core.environment.PythonInstallationDirectoryFinder.PythonInstallationDirectory;
import org.rf.ide.core.environment.SuiteExecutor;
import org.rf.ide.core.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.RedWorkspace;
import org.robotframework.ide.eclipse.main.plugin.preferences.InstalledRobotsEnvironmentsLabelProvider.InstalledRobotsNamesLabelProvider;
import org.robotframework.ide.eclipse.main.plugin.preferences.InstalledRobotsEnvironmentsLabelProvider.InstalledRobotsPathsLabelProvider;
import org.robotframework.ide.eclipse.main.plugin.preferences.InstalledRobotsPreferencesPage;
import org.robotframework.ide.eclipse.main.plugin.project.RedProjectConfigEventData;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.ide.eclipse.main.plugin.project.editor.Environments;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.HeaderFilterMatchesCollection;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.ISectionFormFragment;
import org.robotframework.red.forms.RedFormToolkit;
import org.robotframework.red.graphics.ImagesManager;
import org.robotframework.red.jface.viewers.ViewerColumnsFactory;
import org.robotframework.red.viewers.ListInputStructuredContentProvider;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.html.HtmlEscapers;

class FrameworksSectionFormFragment implements ISectionFormFragment {

    private static final String IMAGE_FOR_LINK = "image";
    private static final String PATH_LINK = "systemPath";
    private static final String PREFERENCES_LINK = "preferences";

    @Inject
    private RedFormToolkit toolkit;

    @Inject
    private IDirtyProviderService dirtyProviderService;

    @Inject
    private RedProjectEditorInput editorInput;


    private CheckboxTableViewer viewer;

    private FormText currentFramework;

    private Button sourceButton;

    TableViewer getViewer() {
        return viewer;
    }

    @Override
    public void initialize(final Composite parent) {
        final Section section = toolkit.createSection(parent,
                ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR
                | Section.DESCRIPTION);
        section.setText("Robot framework");
        section.setDescription(
                "Specify which Robot Framework should be used by this project. Currently following framework is in use:");
        GridDataFactory.fillDefaults().grab(true, true).applyTo(section);

        final Composite sectionInternal = toolkit.createComposite(section);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(sectionInternal);
        GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 5).applyTo(sectionInternal);
        section.setClient(sectionInternal);

        createCurrentFrameworkInfo(sectionInternal);
        createSeparator(sectionInternal);
        createSourceButton(sectionInternal);
        createViewer(sectionInternal);
    }

    private void createCurrentFrameworkInfo(final Composite parent) {
        currentFramework = toolkit.createFormText(parent, true);
        currentFramework.setImage(IMAGE_FOR_LINK, ImagesManager.getImage(RedImages.getRobotImage()));
        GridDataFactory.fillDefaults().grab(true, false).indent(15, 10).applyTo(currentFramework);

        final IHyperlinkListener hyperlinkListener = createHyperlinkListener();
        currentFramework.addHyperlinkListener(hyperlinkListener);
        currentFramework.addDisposeListener(e -> currentFramework.removeHyperlinkListener(hyperlinkListener));
    }

    private IHyperlinkListener createHyperlinkListener() {
        return new HyperlinkAdapter() {

            @Override
            public void linkActivated(final HyperlinkEvent event) {
                final Shell shell = viewer.getTable().getShell();
                if (PATH_LINK.equals(event.getHref())) {
                    if (Desktop.isDesktopSupported()) {
                        final File file = RedWorkspace.Paths
                                .toAbsoluteFromWorkspaceRelativeIfPossible(new Path(event.getLabel()))
                                .toFile();
                        try {
                            Desktop.getDesktop().open(file);
                        } catch (final IOException e) {
                            ErrorDialog.openError(shell, "Unable to open location",
                                    "Unable to open location: " + file.getAbsolutePath(), null);
                        }
                    }

                } else if (PREFERENCES_LINK.equals(event.getHref())) {
                    PreferencesUtil.createPreferenceDialogOn(shell, InstalledRobotsPreferencesPage.ID,
                            new String[] { InstalledRobotsPreferencesPage.ID }, null).open();
                }
            }
        };
    }

    private void createSeparator(final Composite parent) {
        final Label separator = toolkit.createSeparator(parent, SWT.HORIZONTAL);
        GridDataFactory.fillDefaults().indent(0, 10).applyTo(separator);
    }

    private void createSourceButton(final Composite parent) {
        sourceButton = toolkit.createButton(parent, "Use local settings for this project", SWT.CHECK);
        sourceButton.setEnabled(false);

        sourceButton.addSelectionListener(widgetSelectedAdapter(e -> {
            viewer.getTable().setEnabled(sourceButton.getSelection());

            final Object[] elements = viewer.getCheckedElements();
            if (elements.length == 1 && sourceButton.getSelection()) {
                final IRuntimeEnvironment env = (IRuntimeEnvironment) elements[0];
                assignPythonLocation(env.getFile());
            } else {
                assignPythonLocation(null);
            }
            setDirty(true);
        }));
    }

    private void createViewer(final Composite tableParent) {
        final Table table = new Table(tableParent, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL
                | SWT.V_SCROLL);
        viewer = new CheckboxTableViewer(table);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getTable());
        viewer.getTable().setLinesVisible(true);
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setEnabled(false);

        final ICheckStateListener checkListener = createCheckListener();
        viewer.addCheckStateListener(checkListener);
        viewer.getTable().addDisposeListener(e -> viewer.removeCheckStateListener(checkListener));

        ColumnViewerToolTipSupport.enableFor(viewer);

        final Predicate<Object> shouldBeBold = elem -> Arrays.asList(viewer.getCheckedElements()).contains(elem);
        viewer.setContentProvider(new ListInputStructuredContentProvider());
        ViewerColumnsFactory.newColumn("Name")
                .withWidth(320)
                .labelsProvidedBy(new InstalledRobotsNamesLabelProvider(shouldBeBold))
                .createFor(viewer);
        ViewerColumnsFactory.newColumn("Path")
                .withWidth(200)
                .shouldShowLastVerticalSeparator(false)
                .shouldGrabAllTheSpaceLeft(true)
                .labelsProvidedBy(new InstalledRobotsPathsLabelProvider(shouldBeBold))
                .createFor(viewer);
    }

    private ICheckStateListener createCheckListener() {
        return event -> {
            if (event.getChecked()) {
                final IRuntimeEnvironment env = (IRuntimeEnvironment) event.getElement();
                viewer.setCheckedElements(new Object[] { env });
                assignPythonLocation(env.getFile());
            } else {
                sourceButton.setSelection(false);
                viewer.getTable().setEnabled(false);
                assignPythonLocation(null);
            }
            setDirty(true);
            viewer.refresh();
        };
    }

    private void assignPythonLocation(final File file) {
        final String path = file != null
                ? RedWorkspace.Paths.toWorkspaceRelativeIfPossible(new Path(file.getAbsolutePath())).toPortableString()
                : null;
        final SuiteExecutor executor = file instanceof PythonInstallationDirectory
                ? ((PythonInstallationDirectory) file).getInterpreter()
                : null;
        editorInput.getProjectConfiguration().assignPythonLocation(path, executor);
    }

    @Override
    public void setFocus() {
        viewer.getTable().getParent().setFocus();
    }

    private void setDirty(final boolean isDirty) {
        dirtyProviderService.setDirtyState(isDirty);
    }

    @Override
    public HeaderFilterMatchesCollection collectMatches(final String filter) {
        return null;
    }

    @Inject
    @Optional
    private void whenEnvironmentLoadingStarted(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_ENV_LOADING_STARTED) final RedProjectConfigEventData<RobotProjectConfig> eventData) {
        if (eventData.isApplicable(editorInput.getRobotProject())) {
            currentFramework.setText("", false, false);
            sourceButton.setEnabled(false);
            viewer.getTable().setEnabled(false);

            sourceButton.setSelection(!editorInput.getProjectConfiguration().usesPreferences());
        }
    }

    @Inject
    @Optional
    private void whenEnvironmentsWereLoaded(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_ENV_LOADED) final RedProjectConfigEventData<Environments> eventData) {
        if (viewer.getTable() == null || viewer.getTable().isDisposed()
                || !eventData.isApplicable(editorInput.getRobotProject())) {
            return;
        }

        final List<IRuntimeEnvironment> allEnvironments = eventData.getChangedElement().getAllEnvironments();
        final IRuntimeEnvironment env = eventData.getChangedElement().getActiveEnvironment();

        final boolean isEditable = editorInput.isEditable();
        final boolean isUsingPrefs = editorInput.getProjectConfiguration().usesPreferences();

        sourceButton.setEnabled(isEditable);

        viewer.setInput(allEnvironments);
        if (!env.isNullEnvironment()) {
            viewer.setCheckedElements(new Object[] { env });
        }
        viewer.getTable().setEnabled(isEditable && !isUsingPrefs);
        viewer.refresh();

        currentFramework.setText(createActiveFrameworkText(env, isUsingPrefs), true, true);
        currentFramework.getParent().layout();
    }

    @VisibleForTesting
    static String createActiveFrameworkText(final IRuntimeEnvironment env, final boolean isUsingPrefs) {
        final StringBuilder activeText = new StringBuilder();
        activeText.append("<form>");
        activeText.append("<p><img href=\"" + IMAGE_FOR_LINK + "\"/>");
        if (!env.isNullEnvironment()) {
            activeText.append(" <a href=\"" + PATH_LINK + "\">");
            final Path fullPath = new Path(env.getFile().getAbsolutePath());
            activeText.append(RedWorkspace.Paths.toWorkspaceRelativeIfPossible(fullPath).toOSString());
            activeText.append("</a>");
        }
        activeText.append(" " + HtmlEscapers.htmlEscaper().escape(env.getVersion()));
        if (isUsingPrefs) {
            activeText.append(" (<a href=\"" + PREFERENCES_LINK + "\">from Preferences</a>)");
        }
        activeText.append("</p>");
        activeText.append("</form>");
        return activeText.toString();
    }
}
