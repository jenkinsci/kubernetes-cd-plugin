/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.service;

import com.microsoft.jenkins.kubernetes.command.IBaseCommandData;
import com.microsoft.jenkins.kubernetes.command.ICommand;
import com.microsoft.jenkins.kubernetes.command.TransitionInfo;

import java.util.Map;

public interface ICommandServiceData {
    Class getStartCommandClass();

    Map<Class, TransitionInfo> getCommands();

    IBaseCommandData getDataForCommand(ICommand command);
}
