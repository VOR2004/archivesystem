package ru.itis.exception;

public final class MetadataSaveException extends ArchiveException {
    public MetadataSaveException(String message) {
        super(message);
    }

    public MetadataSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
