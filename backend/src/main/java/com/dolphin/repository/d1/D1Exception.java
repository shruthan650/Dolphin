package com.dolphin.repository.d1;

/** A D1 query failed (network error, SQL error, or a Cloudflare API/quota error). */
public class D1Exception extends RuntimeException {

    public D1Exception(String message) {
        super(message);
    }

    public D1Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
