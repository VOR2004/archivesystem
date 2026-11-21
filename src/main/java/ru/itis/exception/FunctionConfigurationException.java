package ru.itis.exception;

public final class FunctionConfigurationException extends ArchiveException {
    public FunctionConfigurationException(String message) {
        super(message);
    }

    public FunctionConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
