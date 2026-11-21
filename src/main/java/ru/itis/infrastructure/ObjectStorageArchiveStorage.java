package ru.itis.infrastructure;

import ru.itis.exception.ArchiveUploadException;
import ru.itis.exception.ProcessingInterruptedException;
import ru.itis.port.ArchiveStorage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public final class ObjectStorageArchiveStorage implements ArchiveStorage {
    private final HttpClient http;
    private final URI bucketUri;

    public ObjectStorageArchiveStorage(HttpClient http, URI bucketUri) {
        this.http = http;
        this.bucketUri = bucketUri;
    }

    @Override
    public void upload(Path zip, String key, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder(bucketUri.resolve(key))
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", "attachment; filename=\"" + key + "\"")
                    .PUT(HttpRequest.BodyPublishers.ofFile(zip))
                    .build();
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ArchiveUploadException("Object Storage upload failed: HTTP " + response.statusCode());
            }
        } catch (IOException error) {
            throw new ArchiveUploadException("Cannot upload ZIP archive", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ProcessingInterruptedException("Object Storage upload interrupted", error);
        }
    }
}
