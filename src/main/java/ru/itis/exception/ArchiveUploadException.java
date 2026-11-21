package ru.itis.exception;

public final class ArchiveUploadException extends ArchiveException {
    public ArchiveUploadException(String message) {
        super(message);
    }

    public ArchiveUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
