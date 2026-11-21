package ru.itis.exception;

public final class TokenAcquisitionException extends ArchiveException {
    public TokenAcquisitionException(String message) {
        super(message);
    }

    public TokenAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
