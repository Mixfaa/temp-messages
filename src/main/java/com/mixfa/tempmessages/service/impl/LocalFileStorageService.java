package com.mixfa.tempmessages.service.impl;

import com.mixfa.tempmessages.model.FileData;
import com.mixfa.tempmessages.model.FileResponse;
import com.mixfa.tempmessages.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {
    private final String rootPath;

    public LocalFileStorageService(@Value("${filestorage.root}") String rootPath) {
        this.rootPath = rootPath;
    }

    private static String makeFileID() {
        return HexFormat.of().toHexDigits(UUID.randomUUID().getLeastSignificantBits());
    }

    @Override
    public FileData write(MultipartFile file) throws Exception {
        return write(
                Objects.requireNonNullElse(file.getOriginalFilename(), file.getName()),
                file.getInputStream()
        );
    }

    @Override
    public FileData write(String filename, InputStream inputStream) throws Exception {
        var id = makeFileID();

        var path = Path.of(rootPath, id, filename.replace("..", ""));

        Files.createDirectories(Path.of(rootPath, id));

        try (var fos = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            inputStream.transferTo(fos);
        }

        return new FileData(id, path.toAbsolutePath().toString());
    }

    private Path findOrThrow(String id) throws Exception {
        Objects.requireNonNull(id);
        var dirPath = Path.of(rootPath, id);

        Path filePath;
        try (var fileStream = Files.list(dirPath)) {
            filePath = fileStream.filter(p -> !Files.isDirectory(p))
                    .findFirst()
                    .orElseThrow(() -> new Exception("File not found"));
        }

        return filePath;
    }

    @Override
    public FileResponse read(String id) throws Exception {
        var filePath = findOrThrow(id);

        StreamingResponseBody streamingResponse = outputStream -> {
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                inputStream.transferTo(outputStream);
            }
        };

        return new FileResponse(Objects.requireNonNullElse(filePath.getFileName(), "file").toString(), streamingResponse);
    }

    @Override
    public void delete(String id) throws Exception {
        Files.delete(findOrThrow(id));
    }
}
