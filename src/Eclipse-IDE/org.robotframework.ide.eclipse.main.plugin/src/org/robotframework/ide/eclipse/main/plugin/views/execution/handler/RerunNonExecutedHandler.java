/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.views.execution.handler;

import static org.robotframework.ide.eclipse.main.plugin.RedPlugin.newCoreException;

import java.util.List;

import javax.inject.Named;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.ISources;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotTestExecutionService.RobotTestsLaunch;
import org.robotframework.ide.eclipse.main.plugin.launch.local.RobotLaunchConfiguration;
import org.robotframework.ide.eclipse.main.plugin.views.execution.ExecutionStatusStore;
import org.robotframework.ide.eclipse.main.plugin.views.execution.ExecutionViewWrapper;
import org.robotframework.ide.eclipse.main.plugin.views.execution.handler.RerunNonExecutedHandler.E4ShowNonExecutedOnlyHandler;
import org.robotframework.red.commands.DIParameterizedHandler;

import com.google.common.annotations.VisibleForTesting;

public class RerunNonExecutedHandler extends DIParameterizedHandler<E4ShowNonExecutedOnlyHandler> {

    public RerunNonExecutedHandler() {
        super(E4ShowNonExecutedOnlyHandler.class);
    }

    public static class E4ShowNonExecutedOnlyHandler {

        @Execute
        public void toggleShowFailedOnly(@Named(ISources.ACTIVE_PART_NAME) final ExecutionViewWrapper view) {
            view.getComponent().getCurrentlyShownLaunch().ifPresent(launch -> {
                final WorkspaceJob job = new WorkspaceJob("Launching Robot Tests") {

                    @Override
                    public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                        ILaunchConfiguration launchConfigCopy;
                        launchConfigCopy = getConfig(launch);
                        launchConfigCopy.launch(ILaunchManager.RUN_MODE, monitor);
                        return Status.OK_STATUS;
                    }
                };
                job.setUser(false);
                job.schedule();
            });
        }

        @VisibleForTesting
        static ILaunchConfiguration getConfig(final RobotTestsLaunch launch) throws CoreException {
            final ILaunchConfiguration launchConfig = launch.getLaunchConfiguration();
            if (launchConfig != null && launchConfig.exists()) {
                final ILaunchConfigurationWorkingCopy launchConfigCopy = launchConfig.copy(launchConfig.getName());
                final RobotLaunchConfiguration robotConfig = new RobotLaunchConfiguration(launchConfigCopy);
                final IProject project = new RobotLaunchConfiguration(launchConfigCopy).getProject();
                final ExecutionStatusStore statusStore = launch.getExecutionData(ExecutionStatusStore.class).get();
                final List<String> nonExecutedTestOrTaskPaths = statusStore.getNonExecutedTestsOrTasksPaths(project, robotConfig);

                if (!nonExecutedTestOrTaskPaths.isEmpty()) {
                    RobotLaunchConfiguration.fillForNonExecutedTestsOrTasksRerun(launchConfigCopy, nonExecutedTestOrTaskPaths);
                    return launchConfigCopy;
                } else {
                    throw newCoreException("Non executed elements do not exist");
                }
            } else {
                throw newCoreException("Launch configuration does not exist");
            }
        }
    }
}
