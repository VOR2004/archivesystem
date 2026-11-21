package ru.itis.service;

import ru.itis.exception.ArchiveProcessingException;
import ru.itis.model.ArchiveMetadata;
import ru.itis.model.ArchiveTask;
import ru.itis.port.ArchiveRepository;
import ru.itis.port.ArchiveStorage;
import ru.itis.port.FileDownloader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ArchiveService {
    private final FileDownloader downloader;
    private final ArchiveStorage storage;
    private final ArchiveRepository repository;

    public ArchiveService(FileDownloader downloader, ArchiveStorage storage, ArchiveRepository repository) {
        this.downloader = downloader;
        this.storage = storage;
        this.repository = repository;
    }

    public ArchiveMetadata archive(ArchiveTask task, String token) {
        String key = UuidV7.generate() + ".zip";
        try (TemporaryFiles temporary = new TemporaryFiles()) {
            downloader.download(URI.create(task.url()), temporary.source);

            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary.zip))) {
                zip.putNextEntry(new ZipEntry(task.name()));
                Files.copy(temporary.source, zip);
                zip.closeEntry();
            }
            ArchiveMetadata metadata = new ArchiveMetadata(task.name(), task.url(), key);
            storage.upload(temporary.zip, key, token);
            repository.save(metadata, token);
            return metadata;
        } catch (IOException error) {
            throw new ArchiveProcessingException("Cannot create ZIP archive", error);
        }
    }

    private static final class TemporaryFiles implements AutoCloseable {
        private final Path directory;
        private final Path source;
        private final Path zip;

        private TemporaryFiles() {
            try {
                directory = Files.createTempDirectory("vvot11-archive-");
            } catch (IOException error) {
                throw new ArchiveProcessingException("Cannot create archive temporary directory", error);
            }
            source = directory.resolve("source");
            zip = directory.resolve("archive.zip");
        }

        @Override
        public void close() {
            // Try-with-resources preserves the original failure if cleanup also fails.
            ArchiveProcessingException failure = null;
            for (Path path : new Path[]{zip, source, directory}) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException error) {
                    if (failure == null) {
                        failure = new ArchiveProcessingException("Cannot clean up archive temporary files", error);
                    } else {
                        failure.addSuppressed(error);
                    }
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
