package ru.itis.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.itis.exception.FileDownloadException;
import ru.itis.exception.InvalidTaskException;
import ru.itis.exception.MetadataSaveException;
import ru.itis.exception.ProcessingInterruptedException;
import ru.itis.infrastructure.HttpFileDownloader;
import ru.itis.infrastructure.ObjectStorageArchiveStorage;
import ru.itis.infrastructure.YdbArchiveRepository;
import ru.itis.service.ArchiveService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveHandlerTest {
    private static final Gson JSON = new Gson();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private URI base;
    private ArchiveHandler handler;
    private byte[] source = "document".getBytes(StandardCharsets.UTF_8);
    private int sourceStatus = 200;
    private int databaseStatus = 200;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/source", exchange -> respond(exchange, sourceStatus, source));
        server.createContext("/bucket/", exchange -> respond(exchange, 200, new byte[0]));
        server.createContext("/database", exchange -> respond(exchange, databaseStatus,
                "{}".getBytes(StandardCharsets.UTF_8)));
        server.start();
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        HttpClient cloudHttp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        HttpClient downloader = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        ArchiveService service = new ArchiveService(
                new HttpFileDownloader(downloader),
                new ObjectStorageArchiveStorage(cloudHttp, base.resolve("/bucket/")),
                new YdbArchiveRepository(cloudHttp, base.resolve("/database")));
        handler = new ArchiveHandler(service, () -> "test-iam-token");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    static Stream<byte[]> sourceFiles() {
        return Stream.of(
                "Текст документа".getBytes(StandardCharsets.UTF_8),
                new byte[]{0, 1, 2, (byte) 0xff},
                new byte[0]);
    }

    @ParameterizedTest(name = "Archives source file #{index}")
    @MethodSource("sourceFiles")
    void archivesFileAndWritesMatchingMetadata(byte[] content) throws IOException {
        source = content;
        String name = "документ.bin";
        String url = base.resolve("/source").toString();
        long before = System.currentTimeMillis();

        assertEquals("OK", handler.apply(event(JSON.toJson(Map.of("name", name, "url", url)))));

        long after = System.currentTimeMillis();
        assertEquals(3, requests.size());
        Request download = requests.get(0);
        assertEquals("GET", download.method());
        assertNull(download.authorization(), "Source host must not receive the IAM token");

        Request upload = requests.get(1);
        assertEquals("PUT", upload.method());
        assertEquals("application/zip", upload.contentType());
        assertEquals("Bearer test-iam-token", upload.authorization());
        String key = upload.path().substring("/bucket/".length());
        assertTrue(key.endsWith(".zip"));
        UUID uuid = UUID.fromString(key.substring(0, key.length() - 4));
        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
        long timestamp = uuid.getMostSignificantBits() >>> 16;
        assertTrue(timestamp >= before && timestamp <= after);

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(upload.body()))) {
            assertEquals(name, zip.getNextEntry().getName());
            assertArrayEquals(content, zip.readAllBytes());
            assertNull(zip.getNextEntry(), "Archive must contain exactly the requested file");
        }

        Request save = requests.get(2);
        assertEquals("POST", save.method());
        assertEquals("DynamoDB_20120810.PutItem", save.target());
        assertEquals("Bearer test-iam-token", save.authorization());
        JsonObject document = JsonParser.parseString(new String(save.body(), StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertEquals("archives", document.get("TableName").getAsString());
        JsonObject item = document.getAsJsonObject("Item");
        assertEquals(name, item.getAsJsonObject("name").get("S").getAsString());
        assertEquals(url, item.getAsJsonObject("url").get("S").getAsString());
        assertEquals(key, item.getAsJsonObject("key").get("S").getAsString());
    }

    @Test
    void rejectsInvalidTasksBeforeAnyNetworkRequest() {
        assertAll(
                () -> assertThrows(InvalidTaskException.class,
                        () -> handler.apply(event(JSON.toJson(Map.of("url", base.resolve("/source").toString()))))),
                () -> assertThrows(InvalidTaskException.class,
                        () -> handler.apply(event("{broken-json"))),
                () -> assertThrows(InvalidTaskException.class,
                        () -> handler.apply(event(JSON.toJson(Map.of("name", "document.txt", "url", "http://bad host/file"))))));
        assertTrue(requests.isEmpty());
    }

    @Test
    void failsWhenSourceReturns404WithoutSavingAnything() {
        sourceStatus = 404;

        FileDownloadException failure = assertThrows(FileDownloadException.class,
                () -> handler.apply(validEvent()));

        assertTrue(failure.getMessage().contains("404"));
        assertEquals(1, requests.size());
        assertEquals("/source", requests.getFirst().path());
    }

    @Test
    void propagatesDatabaseFailureToTriggerInsteadOfReportingSuccess() {
        databaseStatus = 500;

        MetadataSaveException failure = assertThrows(MetadataSaveException.class,
                () -> handler.apply(validEvent()));

        assertTrue(failure.getMessage().contains("YDB PutItem"));
        assertEquals(3, requests.size());
        assertEquals("/database", requests.getLast().path());
    }

    @Test
    void preservesInterruptionAndReportsCustomFailure() {
        Thread.currentThread().interrupt();
        try {
            ProcessingInterruptedException failure = assertThrows(ProcessingInterruptedException.class,
                    () -> handler.apply(validEvent()));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    private QueueEvent validEvent() {
        return event(JSON.toJson(Map.of("name", "document.txt", "url", base.resolve("/source").toString())));
    }

    private QueueEvent event(String task) {
        // The queue body is a JSON string nested in the actual trigger envelope.
        Map<String, Object> envelope = Map.of("messages", List.of(Map.of(
                "event_metadata", Map.of("event_type", "yandex.cloud.events.messagequeue.QueueMessage"),
                "details", Map.of("message", Map.of("body", task, "message_id", "test-message")))));
        return JSON.fromJson(JSON.toJson(envelope), QueueEvent.class);
    }

    private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        try (exchange) {
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("X-Amz-Target"),
                    exchange.getRequestBody().readAllBytes()));
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private record Request(String method, String path, String authorization,
                           String contentType, String target, byte[] body) {
    }
}
