package ru.itis.exception;

public final class FileDownloadException extends ArchiveException {
    public FileDownloadException(String message) {
        super(message);
    }

    public FileDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
