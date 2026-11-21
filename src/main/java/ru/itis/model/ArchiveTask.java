package ru.itis.model;

import ru.itis.exception.InvalidTaskException;

import java.net.URI;

public record ArchiveTask(String name, String url) {
    public ArchiveTask {
        if (name == null || name.isBlank() || name.equals(".") || name.equals("..")
                || name.contains("/") || name.contains("\\") || name.indexOf('\0') >= 0) {
            throw new InvalidTaskException("name must be a non-empty file name without a path");
        }
        if (url == null || url.isBlank()) {
            throw new InvalidTaskException("url is required");
        }
        URI source;
        try {
            source = URI.create(url);
        } catch (IllegalArgumentException error) {
            throw new InvalidTaskException("url must be a valid URI", error);
        }
        if (!("https".equals(source.getScheme()) || "http".equals(source.getScheme()))
                || source.getHost() == null) {
            throw new InvalidTaskException("url must be an absolute HTTP or HTTPS URL");
        }
    }
}
