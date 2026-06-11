package com.amins.nphies.exception;

public class NphiesAuthException extends RuntimeException {
    public NphiesAuthException(String msg) { super(msg); }
    public NphiesAuthException(String msg, Throwable cause) { super(msg, cause); }
}

