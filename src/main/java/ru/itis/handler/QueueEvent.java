package ru.itis.handler;

import java.util.List;

// Plain public DTOs are deserialized by the Cloud Functions Java runtime.
public final class QueueEvent {
    public List<Message> messages;

    public static final class Message {
        public Details details;
    }

    public static final class Details {
        public Body message;
    }

    public static final class Body {
        public String body;
    }
}
