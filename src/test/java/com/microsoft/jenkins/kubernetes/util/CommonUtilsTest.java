/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.jenkins.kubernetes.util;

import com.google.common.collect.ImmutableMap;
import hudson.util.VariableResolver;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link CommonUtils}.
 */
public class CommonUtilsTest {
    @Test
    public void testReplaceMacro() throws Exception {
        testReplaceMacro("", "", ImmutableMap.<String, String>of());
        testReplaceMacro("$a", "$a", ImmutableMap.<String, String>of());
        testReplaceMacro("", "", ImmutableMap.of("a", "b"));
        testReplaceMacro("1$def", "$abc$def", ImmutableMap.of("abc", "1"));
        testReplaceMacro("$abc1", "$abc${a}", ImmutableMap.of("a", "1"));
        testReplaceMacro("hello, world!", "hello, $who${mark}", ImmutableMap.of("who", "world", "mark", "!"));
    }

    private void testReplaceMacro(String expected, String original, Map<String, String> variables) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(original.getBytes(Constants.DEFAULT_CHARSET));
        InputStream result = CommonUtils.replaceMacro(in, new VariableResolver.ByMap<>(variables));
        assertEquals(expected, IOUtils.toString(result, Constants.DEFAULT_CHARSET));
    }

    @Test
    public void testRandomString() {
        assertEquals(16, CommonUtils.randomString().length());

        int mask = 0;
        for (int i = 0; i < 100; ++i) {
            String s = CommonUtils.randomString();
            assertEquals(16, s.length());
            mask |= charsetMask(s);
            assertEquals(0, mask & 0x8);
            if (mask == 0x7) {
                break;
            }
        }
        assertEquals(0x7, mask);
    }

    @Test
    public void testRandomStringLength() {
        assertEquals(0, CommonUtils.randomString(0).length());
        assertEquals(12, CommonUtils.randomString(12).length());
        assertEquals(14, CommonUtils.randomString(14).length());
        try {
            CommonUtils.randomString(-1);
            fail();
        } catch (NegativeArraySizeException ex) {
            // ignore
        }

        int mask = 0;
        Random r = CommonUtils.threadLocalRandom();
        for (int i = 0; i < 100; ++i) {
            int length = 10 + r.nextInt(7);
            String s = CommonUtils.randomString(length);
            assertEquals(length, s.length());
            mask |= charsetMask(s);
            assertEquals(0, mask & 0x8);
            if (mask == 0x7) {
                break;
            }
        }
        assertEquals(0x7, mask);
    }

    @Test
    public void testRandomStringForceLowercase() {
        int mask = 0;
        Random r = CommonUtils.threadLocalRandom();
        for (int i = 0; i < 10; ++i) {
            String s = CommonUtils.randomString(16 + r.nextInt(5), true);
            mask |= charsetMask(s);
            assertTrue((mask & 0x2) == 0);
            assertTrue((mask & 0x8) == 0);
        }
    }

    @Test
    public void testRandomStringDigitsOnly() {
        int mask = 0;
        Random r = CommonUtils.threadLocalRandom();
        for (int i = 0; i < 10; ++i) {
            String s = CommonUtils.randomString(16 + r.nextInt(5), false, true);
            mask |= charsetMask(s);
            assertTrue((mask & 0x1) == 0);
            assertTrue((mask & 0x2) == 0);
            assertTrue((mask & 0x8) == 0);
        }
    }

    /**
     * Check the character set in a string, and returns an integer mask which represents the results as followed:
     * <p>
     * * 1st bit is 1 if there is lower case letter
     * * 2nd bit is 1 if there is upper case letter
     * * 3rd bit is 1 if there is digit
     * * 4th bit is 1 if there is other characters
     *
     * @param s the checked string
     * @return the character set mask
     */
    private int charsetMask(String s) {
        int mask = 0;
        for (int i = 0, ie = s.length(); i < ie; ++i) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'z') {
                mask |= 0x1;
            } else if (c >= 'A' && c <= 'Z') {
                mask |= 0x2;
            } else if (c >= '0' && c <= '9') {
                mask |= 0x4;
            } else {
                mask |= 0x8;
            }
        }
        return mask;
    }
}
