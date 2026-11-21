package ru.itis.port;

import java.nio.file.Path;

public interface ArchiveStorage {
    void upload(Path zip, String key, String token);
}
