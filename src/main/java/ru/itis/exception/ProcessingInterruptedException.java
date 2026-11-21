package ru.itis.exception;

public final class ProcessingInterruptedException extends ArchiveException {
    public ProcessingInterruptedException(String message, InterruptedException cause) {
        super(message, cause);
    }
}
