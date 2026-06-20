package com.example.recyclerviewwebservice.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class NetworkRetryPolicyTest {
    @Test
    public void retriesTemporaryNetworkFailures() {
        assertTrue(NetworkRetryPolicy.shouldRetry(new UnknownHostException()));
        assertTrue(NetworkRetryPolicy.shouldRetry(new ConnectException()));
        assertTrue(NetworkRetryPolicy.shouldRetry(new SocketTimeoutException()));
        assertTrue(NetworkRetryPolicy.shouldRetry(
                new RuntimeException(new UnknownHostException())
        ));
    }

    @Test
    public void doesNotRetryUnrelatedFailures() {
        assertFalse(NetworkRetryPolicy.shouldRetry(new IOException("HTTP 404")));
        assertFalse(NetworkRetryPolicy.shouldRetry(new IllegalArgumentException("Bad JSON")));
    }

    @Test
    public void backoffIsBounded() {
        long[] actual = new long[] {
                NetworkRetryPolicy.delayAfterAttempt(1),
                NetworkRetryPolicy.delayAfterAttempt(2),
                NetworkRetryPolicy.delayAfterAttempt(3),
                NetworkRetryPolicy.delayAfterAttempt(4),
                NetworkRetryPolicy.delayAfterAttempt(5)
        };
        assertArrayEquals(new long[] {1_000L, 2_000L, 4_000L, 8_000L, 8_000L}, actual);
    }
}
