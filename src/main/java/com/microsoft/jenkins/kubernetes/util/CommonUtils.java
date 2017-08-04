/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import com.microsoft.jenkins.kubernetes.Messages;
import hudson.Util;
import hudson.util.VariableResolver;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class CommonUtils {
    /**
     * Replace the variables in the given {@code InputStream} and produce a new stream.
     * If the {@code variableResolver} is null, the original {@code InputStream} will be returned.
     * <p>
     * Note that although we're processing streams, all the contents will be loaded and returned for this substitution.
     *
     * @param original         the original {@code InputStream}
     * @param variableResolver the variable resolver
     * @return a new {@code InputStream} with the variables replaced by their values,
     * or the original if the {@code variableResolver} is {@code null}.
     * @throws IOException error on reading the original InputStream.
     */
    public static InputStream replaceMacro(InputStream original,
                                           VariableResolver<String> variableResolver) throws IOException {
        try {
            if (variableResolver == null) {
                return original;
            }
            String content = IOUtils.toString(original, Constants.DEFAULT_CHARSET);
            content = Util.replaceMacro(content, variableResolver);
            if (content != null) {
                return new ByteArrayInputStream(content.getBytes(Constants.DEFAULT_CHARSET));
            } else {
                throw new IllegalArgumentException(Messages.JobContext_nullContent());
            }
        } finally {
            original.close();
        }
    }

    private CommonUtils() {
        // hide constructor
    }
}
