/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.service;

import com.microsoft.jenkins.kubernetes.command.DeploymentState;
import com.microsoft.jenkins.kubernetes.command.IBaseCommandData;
import com.microsoft.jenkins.kubernetes.command.ICommand;
import com.microsoft.jenkins.kubernetes.command.INextCommandAware;
import com.microsoft.jenkins.kubernetes.command.TransitionInfo;

import java.util.Map;

public final class CommandService {
    public static boolean executeCommands(ICommandServiceData commandServiceData) {
        Class startCommand = commandServiceData.getStartCommandClass();
        Map<Class, TransitionInfo> commands = commandServiceData.getCommands();
        if (!commands.isEmpty() && startCommand != null) {
            //successfully started
            TransitionInfo current = commands.get(startCommand);
            while (current != null) {
                ICommand<IBaseCommandData> command = current.getCommand();
                IBaseCommandData commandData = commandServiceData.getDataForCommand(command);
                command.execute(commandData);

                INextCommandAware previous = current;
                if (command instanceof INextCommandAware) {
                    previous = (INextCommandAware) command;
                }

                current = null;

                final DeploymentState state = commandData.getDeploymentState();
                if (state == DeploymentState.Success && previous.getSuccess() != null) {
                    current = commands.get(previous.getSuccess());
                } else if (state == DeploymentState.UnSuccessful && previous.getFail() != null) {
                    current = commands.get(previous.getFail());
                } else if (state == DeploymentState.HasError) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    private CommandService() {
        // hide constructor
    }
}
