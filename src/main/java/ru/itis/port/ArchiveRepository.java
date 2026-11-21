package ru.itis.port;

import ru.itis.model.ArchiveMetadata;

public interface ArchiveRepository {
    void save(ArchiveMetadata metadata, String token);
}
