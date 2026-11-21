package ru.itis.handler;

import ru.itis.config.ArchiveConfiguration;
import ru.itis.exception.InvalidTaskException;
import ru.itis.model.ArchiveMetadata;
import ru.itis.model.ArchiveTask;
import ru.itis.service.ArchiveService;

import java.util.function.Function;
import java.util.function.Supplier;

public final class ArchiveHandler implements Function<QueueEvent, String> {
    private final ArchiveService service;
    private final Supplier<String> tokenProvider;

    public ArchiveHandler() {
        this(ArchiveConfiguration.fromEnvironment());
    }

    private ArchiveHandler(ArchiveConfiguration configuration) {
        this(configuration.service(), configuration.tokenProvider());
    }

    public ArchiveHandler(ArchiveService service, Supplier<String> tokenProvider) {
        this.service = service;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public String apply(QueueEvent event) {
        if (event == null || event.messages == null || event.messages.size() != 1) {
            throw new InvalidTaskException("Expected exactly one Message Queue message");
        }
        QueueEvent.Message message = event.messages.getFirst();
        if (message == null || message.details == null || message.details.message == null
                || message.details.message.body == null) {
            throw new InvalidTaskException("Message Queue event has no task body");
        }
        ArchiveTask task = ArchiveTaskParser.parse(message.details.message.body);
        // Application exceptions propagate to the trigger so its DLQ policy still applies.
        ArchiveMetadata archive = service.archive(task, tokenProvider.get());
        System.out.println("Created archive: " + archive.key());
        return "OK";
    }
}
