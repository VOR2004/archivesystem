package ru.itis.infrastructure;

import ru.itis.exception.FileDownloadException;
import ru.itis.exception.ProcessingInterruptedException;
import ru.itis.port.FileDownloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public final class HttpFileDownloader implements FileDownloader {
    private final HttpClient http;

    public HttpFileDownloader(HttpClient http) {
        this.http = http;
    }

    @Override
    public void download(URI source, Path destination) {
        HttpRequest request = HttpRequest.newBuilder(source)
                .timeout(Duration.ofSeconds(240))
                .GET()
                .build();
        try {
            HttpResponse<Path> response = http.send(request, HttpResponse.BodyHandlers.ofFile(destination));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileDownloadException("Source download failed: HTTP " + response.statusCode());
            }
        } catch (IOException error) {
            throw new FileDownloadException("Cannot download source file", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ProcessingInterruptedException("Source download interrupted", error);
        }
    }
}
