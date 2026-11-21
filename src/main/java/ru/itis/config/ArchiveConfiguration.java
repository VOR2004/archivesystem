package ru.itis.config;

import ru.itis.exception.FunctionConfigurationException;
import ru.itis.infrastructure.HttpFileDownloader;
import ru.itis.infrastructure.IamTokenProvider;
import ru.itis.infrastructure.ObjectStorageArchiveStorage;
import ru.itis.infrastructure.YdbArchiveRepository;
import ru.itis.service.ArchiveService;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

public record ArchiveConfiguration(ArchiveService service, Supplier<String> tokenProvider) {
    public static ArchiveConfiguration fromEnvironment() {
        URI bucketUri = configuredUri("https://storage.yandexcloud.net/" + requiredEnvironment("BUCKET_NAME") + "/");
        URI documentApiEndpoint = configuredUri(requiredEnvironment("YDB_DOCUMENT_API_ENDPOINT"));
        // Cloud requests never follow redirects, so their IAM token stays on the target host.
        HttpClient cloudHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpClient downloadHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        ArchiveService service = new ArchiveService(
                new HttpFileDownloader(downloadHttp),
                new ObjectStorageArchiveStorage(cloudHttp, bucketUri),
                new YdbArchiveRepository(cloudHttp, documentApiEndpoint));
        return new ArchiveConfiguration(service, new IamTokenProvider(cloudHttp));
    }

    private static String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new FunctionConfigurationException("Missing function environment variable: " + name);
        }
        return value;
    }

    private static URI configuredUri(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException error) {
            throw new FunctionConfigurationException("Invalid URI in function configuration", error);
        }
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null) {
            throw new FunctionConfigurationException("Function endpoints must be absolute HTTP or HTTPS URIs");
        }
        return uri;
    }
}
