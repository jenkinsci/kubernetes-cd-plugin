/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.microsoft.jenkins.kubernetes.Messages;
import hudson.Util;
import hudson.security.ACL;
import hudson.util.VariableResolver;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Random;

public final class CommonUtils {
    private static final char[] DIGITS;
    private static final char[] DIGITS_ASCII_LOWERCASE;
    private static final char[] DIGITS_ASCII_LETTERS;

    private static final ThreadLocal<Random> THREAD_LOCAL_RANDOM = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    static {
        String digits = "0123456789";
        String asciiLowercase = "abcdefghijklmnopqrstuvwxyz";

        DIGITS = digits.toCharArray();
        DIGITS_ASCII_LOWERCASE = (digits + asciiLowercase).toCharArray();
        DIGITS_ASCII_LETTERS = (digits + asciiLowercase + asciiLowercase.toUpperCase()).toCharArray();
    }

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
        if (variableResolver == null) {
            return original;
        }
        try {
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

    public static Random threadLocalRandom() {
        return THREAD_LOCAL_RANDOM.get();
    }

    public static String randomString() {
        final int defaultLength = 16;
        return randomString(defaultLength, false, false);
    }

    public static String randomString(final int length) {
        return randomString(length, false, false);
    }

    public static String randomString(final int length, final boolean forceLowercase) {
        return randomString(length, forceLowercase, false);
    }

    public static String randomString(final int length, final boolean forceLowercase, final boolean digitsOnly) {
        char[] pool = DIGITS;
        if (!digitsOnly) {
            if (forceLowercase) {
                pool = DIGITS_ASCII_LOWERCASE;
            } else {
                pool = DIGITS_ASCII_LETTERS;
            }
        }
        char[] sample = new char[length];
        Random rand = threadLocalRandom();
        for (int i = 0; i < sample.length; ++i) {
            sample[i] = pool[rand.nextInt(pool.length)];
        }
        return new String(sample);
    }

    public static StandardUsernamePasswordCredentials getCredentials(String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentials(
                        StandardUsernamePasswordCredentials.class,
                        Jenkins.getInstance(),
                        ACL.SYSTEM,
                        Collections.emptyList()),
                CredentialsMatchers.withId(credentialsId));
    }

    private CommonUtils() {
        // hide constructor
    }
}
