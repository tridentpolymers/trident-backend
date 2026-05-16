package com.trident.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ContentService {
    private static final Set<String> ALLOWED_FILES = Set.of("content", "products", "settings", "testimonials", "faq");

    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public ContentService(ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.dataDir = Path.of(properties.getDataDir()).toAbsolutePath().normalize();
    }

    public JsonNode read(String name) {
        validateName(name);
        Path path = dataDir.resolve(name + ".json");
        if (!Files.exists(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, name + " not found");
        }
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read content file");
        }
    }

    public void write(String name, JsonNode body) {
        validateName(name);
        if (body == null || body.isNull()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Body required");
        }
        JsonNode data = body.has("data") ? body.get("data") : body;
        try {
            Files.createDirectories(dataDir);
            objectMapper.writeValue(dataDir.resolve(name + ".json").toFile(), data);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to write content file");
        }
    }

    private void validateName(String name) {
        if (!ALLOWED_FILES.contains(name)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid content file");
        }
    }
}
