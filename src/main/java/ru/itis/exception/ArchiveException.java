package ru.itis.exception;

public abstract class ArchiveException extends RuntimeException {
    protected ArchiveException(String message) {
        super(message);
    }

    protected ArchiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
