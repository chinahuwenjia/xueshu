package com.ruoyi.common.utils;

import java.security.SecureRandom;
import java.util.Random;

public class SentryTraceGenerator {

    private static final Random random = new SecureRandom();

    public static void main(String[] args) {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        boolean sampled = true; // or false, depending on your sampling logic

        String sentryTrace = generateSentryTrace(traceId, spanId, sampled);
        System.out.println("Sentry-Trace: " + sentryTrace);
    }

    public static String generateTraceId() {
        return generateHexId(32);
    }

    public static String generateSpanId() {
        return generateHexId(16);
    }

    public static String generateHexId(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int value = random.nextInt(16);
            result.append(Integer.toHexString(value));
        }
        return result.toString();
    }

    public static String generateSentryTrace(String traceId, String spanId, boolean sampled) {
        return String.format("%s-%s", traceId, spanId, sampled ? "1" : "0");
    }
}
