package com.nuix.proserv.t3k;

public class T3KApiException extends RuntimeException {
    public T3KApiException(String message) {
        super(message);
    }

    public T3KApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
