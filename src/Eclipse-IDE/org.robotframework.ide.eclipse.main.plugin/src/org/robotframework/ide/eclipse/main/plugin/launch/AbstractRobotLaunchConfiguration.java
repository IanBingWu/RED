/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch;

import static org.robotframework.ide.eclipse.main.plugin.RedPlugin.newCoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.rf.ide.core.execution.server.AgentConnectionServer;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

public abstract class AbstractRobotLaunchConfiguration implements IRobotLaunchConfiguration {

    private static final String PROJECT_NAME_ATTRIBUTE = "Project name";

    private static final String USE_REMOTE_AGENT_ATTRIBUTE = "Remote agent";

    private static final String AGENT_CONNECTION_HOST_ATTRIBUTE = "Agent connection host";

    private static final String AGENT_CONNECTION_PORT_ATTRIBUTE = "Agent connection port";

    private static final String AGENT_CONNECTION_TIMEOUT_ATTRIBUTE = "Agent connection timeout";

    protected static final String VERSION_OF_CONFIGURATION = "Version of configuration";

    protected final ILaunchConfiguration configuration;

    protected AbstractRobotLaunchConfiguration(final ILaunchConfiguration config) {
        this.configuration = config;
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public String getTypeName() {
        try {
            return configuration.getType().getName();
        } catch (final CoreException e) {
            return null;
        }
    }

    @Override
    public String getProjectName() throws CoreException {
        return configuration.getAttribute(PROJECT_NAME_ATTRIBUTE, "");
    }

    @Override
    public void setProjectName(final String projectName) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(PROJECT_NAME_ATTRIBUTE, projectName);
    }

    @Override
    public IProject getProject() throws CoreException {
        final String projectName = getProjectName();
        if (projectName.isEmpty()) {
            throw newCoreException("Project cannot be empty");
        }
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) {
            throw newCoreException("Project '" + project.getName() + "' cannot be found in workspace");
        }
        if (!project.isOpen()) {
            throw newCoreException("Project '" + project.getName() + "' is currently closed");
        }
        return project;
    }

    @Override
    public boolean isUsingRemoteAgent() throws CoreException {
        return Boolean.valueOf(configuration.getAttribute(USE_REMOTE_AGENT_ATTRIBUTE, "false"));
    }

    @Override
    public String getAgentConnectionHost() throws CoreException {
        if (isUsingRemoteAgent()) {
            final String host = getAgentConnectionHostValue();
            if (host.isEmpty()) {
                throw newCoreException("Server IP cannot be empty");
            }
            return host;
        }
        return AgentConnectionServer.DEFAULT_CONNECTION_HOST;
    }

    @Override
    public int getAgentConnectionPort() throws CoreException {
        if (isUsingRemoteAgent()) {
            final String port = getAgentConnectionPortValue();
            final Integer portAsInt = Ints.tryParse(port);
            if (portAsInt == null || !Range
                    .closed(AgentConnectionServer.MIN_CONNECTION_PORT, AgentConnectionServer.MAX_CONNECTION_PORT)
                    .contains(portAsInt) && portAsInt != 0) {
                throw newCoreException(String.format(
                        "Server port '%s' must be an Integer between %,d and %,d or 0 for dynamic allocation", port,
                        AgentConnectionServer.MIN_CONNECTION_PORT, AgentConnectionServer.MAX_CONNECTION_PORT));
            }
            return portAsInt == 0 ? AgentConnectionServer.findFreePort() : portAsInt;
        }
        return AgentConnectionServer.findFreePort();
    }

    @Override
    public int getAgentConnectionTimeout() throws CoreException {
        if (isUsingRemoteAgent()) {
            final String timeout = getAgentConnectionTimeoutValue();
            final Integer timeoutAsInt = Ints.tryParse(timeout);
            if (timeoutAsInt == null || !Range
                    .closed(AgentConnectionServer.MIN_CONNECTION_TIMEOUT, AgentConnectionServer.MAX_CONNECTION_TIMEOUT)
                    .contains(timeoutAsInt)) {
                throw newCoreException(String.format("Connection timeout '%s' must be an Integer between %,d and %,d",
                        timeout, AgentConnectionServer.MIN_CONNECTION_TIMEOUT,
                        AgentConnectionServer.MAX_CONNECTION_TIMEOUT));
            }
            return timeoutAsInt;
        }
        return AgentConnectionServer.DEFAULT_CONNECTION_TIMEOUT;
    }

    @Override
    public String getAgentConnectionHostValue() throws CoreException {
        return configuration.getAttribute(AGENT_CONNECTION_HOST_ATTRIBUTE, "");
    }

    @Override
    public String getAgentConnectionPortValue() throws CoreException {
        return configuration.getAttribute(AGENT_CONNECTION_PORT_ATTRIBUTE, "");
    }

    @Override
    public String getAgentConnectionTimeoutValue() throws CoreException {
        return configuration.getAttribute(AGENT_CONNECTION_TIMEOUT_ATTRIBUTE, "");
    }

    @Override
    public void setUsingRemoteAgent(final boolean isRemoteAgent) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(USE_REMOTE_AGENT_ATTRIBUTE, String.valueOf(isRemoteAgent));
    }

    @Override
    public void setAgentConnectionHostValue(final String host) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(AGENT_CONNECTION_HOST_ATTRIBUTE, host);
    }

    @Override
    public void setAgentConnectionPortValue(final String port) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(AGENT_CONNECTION_PORT_ATTRIBUTE, port);
    }

    @Override
    public void setAgentConnectionTimeoutValue(final String timeout) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(AGENT_CONNECTION_TIMEOUT_ATTRIBUTE, timeout);
    }

    @Override
    public String getConfigurationVersion() throws CoreException {
        String version = configuration.getAttribute(VERSION_OF_CONFIGURATION, "0");
        // hack for non-versioned otherwise valid RED 7.6 configs
        version = "0".equals(version)
                ? (configuration.getAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, "").isEmpty() ? "0" : "1")
                : version;
        return version;
    }

    @Override
    public void fillDefaults() throws CoreException {
        final RedPreferences preferences = RedPlugin.getDefault().getPreferences();
        setUsingRemoteAgent(preferences.isAgentConnectionCustomized());
        setAgentConnectionHostValue(preferences.getLaunchAgentConnectionHost());
        setAgentConnectionPortValue(preferences.getLaunchAgentConnectionPort());
        setAgentConnectionTimeoutValue(preferences.getLaunchAgentConnectionTimeout());
        setProjectName("");
        setProcessFactory(LaunchConfigurationsWrappers.FACTORY_ID);
        setCurrentConfigurationVersion();
    }

    private void setProcessFactory(final String id) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, id);
    }

    public ILaunchConfigurationWorkingCopy asWorkingCopy() throws CoreException {
        return configuration instanceof ILaunchConfigurationWorkingCopy
                ? (ILaunchConfigurationWorkingCopy) configuration : configuration.getWorkingCopy();
    }

}
