/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.command;

/**
 * Indicates that a command is aware of its next command to be executed based on its execution status.
 */
public interface INextCommandAware {
    Class getSuccess();

    Class getFail();
}
