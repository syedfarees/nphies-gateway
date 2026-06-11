package com.amins.nphies.exception;

public class NphiesException extends RuntimeException {
    public NphiesException(String msg) { super(msg); }
    public NphiesException(String msg, Throwable cause) { super(msg, cause); }

    /** Retryable: 5xx, network errors, timeouts */
    public static class Retryable extends NphiesException {
        public Retryable(String msg) { super(msg); }
        public Retryable(String msg, Throwable cause) { super(msg, cause); }
    }

    /** Non-retryable: 4xx validation/bad request errors */
    public static class NonRetryable extends NphiesException {
        public NonRetryable(String msg) { super(msg); }
        public NonRetryable(String msg, Throwable cause) { super(msg, cause); }
    }
}