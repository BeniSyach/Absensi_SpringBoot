package com.absensi.absensi_app.exception;

public class AbsensiException extends RuntimeException {
    public AbsensiException(String message) {
        super(message);
    }
    public AbsensiException(String message, Throwable cause) {
        super(message, cause);
    }
}
