/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.credentials;

import hudson.model.Item;

/**
 * Indicates whether a model needs to be bound to the owner parent project of the build.
 */
public interface AncestorAware {
    void bindToAncestor(Item owner);
}
