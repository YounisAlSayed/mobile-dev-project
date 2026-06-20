package com.example.recyclerviewwebservice.network;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Bounded exponential-backoff policy for temporary connectivity failures.
 */
public final class NetworkRetryPolicy {
    public static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_MILLIS = 1_000L;
    private static final long MAX_DELAY_MILLIS = 8_000L;

    private NetworkRetryPolicy() {
    }

    public static boolean shouldRetry(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof ConnectException
                    || current instanceof NoRouteToHostException
                    || current instanceof SocketTimeoutException
                    || current instanceof SocketException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * @param failedAttempt one-based number of the request that just failed
     */
    public static long delayAfterAttempt(int failedAttempt) {
        int safeAttempt = Math.max(1, failedAttempt);
        int shift = Math.min(safeAttempt - 1, 3);
        return Math.min(INITIAL_DELAY_MILLIS << shift, MAX_DELAY_MILLIS);
    }
}
