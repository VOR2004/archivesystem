package ru.itis.infrastructure;

import com.google.gson.Gson;
import ru.itis.exception.MetadataSaveException;
import ru.itis.exception.ProcessingInterruptedException;
import ru.itis.model.ArchiveMetadata;
import ru.itis.port.ArchiveRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class YdbArchiveRepository implements ArchiveRepository {
    private static final Gson JSON = new Gson();
    private final HttpClient http;
    private final URI documentApiEndpoint;

    public YdbArchiveRepository(HttpClient http, URI documentApiEndpoint) {
        this.http = http;
        this.documentApiEndpoint = documentApiEndpoint;
    }

    @Override
    public void save(ArchiveMetadata metadata, String token) {
        // Document API uses DynamoDB AttributeValue objects, not plain JSON strings.
        Map<String, Object> body = Map.of(
                "TableName", "archives",
                "Item", Map.of(
                        "name", Map.of("S", metadata.name()),
                        "url", Map.of("S", metadata.url()),
                        "key", Map.of("S", metadata.key())));
        HttpRequest request = HttpRequest.newBuilder(documentApiEndpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/x-amz-json-1.0")
                .header("X-Amz-Target", "DynamoDB_20120810.PutItem")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(body)))
                .build();
        try {
            HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MetadataSaveException("YDB PutItem failed: HTTP " + response.statusCode());
            }
        } catch (IOException error) {
            throw new MetadataSaveException("Cannot save archive metadata in YDB", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ProcessingInterruptedException("YDB metadata save interrupted", error);
        }
    }
}
