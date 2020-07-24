/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.server;

import java.util.Optional;

import org.rf.ide.core.environment.RobotVersion;

public class AgentServerVersionsDebugChecker extends AgentServerVersionsChecker {

    @Override
    protected Optional<String> checkRobot(final String robotVersion) {
        final RobotVersion actualVersion = RobotVersion.from(robotVersion);
        if (actualVersion.isOlderThan(new RobotVersion(3, 0))) {
            return Optional.of("RED debugger requires Robot Framework in version 3.0 or newer.\n\tRobot Framework: "
                    + actualVersion.asString());
        }
        return Optional.empty();
    }
}
