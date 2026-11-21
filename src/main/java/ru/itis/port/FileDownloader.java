package ru.itis.port;

import java.net.URI;
import java.nio.file.Path;

public interface FileDownloader {
    void download(URI source, Path destination);
}
