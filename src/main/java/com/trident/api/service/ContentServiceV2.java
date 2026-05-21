package com.trident.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trident.api.config.AppProperties;
import com.trident.api.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;


@Service
public class ContentServiceV2 {
    private static final Set<String> ALLOWED_FILES = Set.of("content", "products", "settings", "testimonials", "faq");

    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public ContentServiceV2(ObjectMapper objectMapper, AppProperties properties) {
        this.objectMapper = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.dataDir = Path.of(properties.getDataDir()).toAbsolutePath().normalize();

        System.out.println("========================================");
        System.out.println("CONTENT SERVICE V2 INITIALIZED");
        System.out.println("USING FILE SYSTEM DATA DIRECTORY");
        System.out.println("----------------------------------------");
        System.out.println("DATA DIRECTORY:");
        System.out.println(dataDir);

        try {
            System.out.println("----------------------------------------");
            System.out.println("VERIFYING DATA FILES");

            for (String fileName : ALLOWED_FILES) {
                Path path = dataDir.resolve(fileName + ".json");
                System.out.println(path + " -> " + Files.exists(path));
            }
        } catch (Exception exception) {
            System.out.println("----------------------------------------");
            System.out.println("FAILED TO VERIFY DATA FILES");
            exception.printStackTrace();
        }

        System.out.println("========================================");
    }

    public JsonNode read(String name) {
        System.out.println("========================================");
        System.out.println("CONTENT SERVICE V2 READ OPERATION");
        System.out.println("========================================");
        System.out.println("REQUESTED FILE NAME:");
        System.out.println(name);

        validateName(name);

        Path path = dataDir.resolve(name + ".json");

        System.out.println("----------------------------------------");
        System.out.println("RESOLVED FILE PATH:");
        System.out.println(path);
        System.out.println("----------------------------------------");
        System.out.println("FILE EXISTS:");
        System.out.println(Files.exists(path));

        if (!Files.exists(path)) {
            System.out.println("----------------------------------------");
            System.out.println("FILE NOT FOUND");
            throw new ApiException(HttpStatus.NOT_FOUND, name + " not found");
        }
        try {
            JsonNode result = objectMapper.readTree(path.toFile());

            System.out.println("----------------------------------------");
            System.out.println("FILE READ SUCCESSFULLY");

            return result;
        } catch (IOException exception) {
            System.out.println("----------------------------------------");
            System.out.println("FAILED TO READ FILE");
            exception.printStackTrace();

            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read content file");
        }
    }

    public void write(String name, JsonNode body) {
        System.out.println("========================================");
        System.out.println("CONTENT SERVICE V2 WRITE OPERATION");
        System.out.println("========================================");
        System.out.println("REQUESTED FILE NAME:");
        System.out.println(name);

        validateName(name);

        if (body == null || body.isNull()) {
            System.out.println("----------------------------------------");
            System.out.println("WRITE BODY MISSING");
            throw new ApiException(HttpStatus.BAD_REQUEST, "Body required");
        }

        JsonNode data = body.has("data") ? body.get("data") : body;
        Path path = dataDir.resolve(name + ".json");

        System.out.println("----------------------------------------");
        System.out.println("BODY WRAPPED WITH DATA FIELD:");
        System.out.println(body.has("data"));
        System.out.println("----------------------------------------");
        System.out.println("RESOLVED FILE PATH:");
        System.out.println(path);

        try {
            Files.createDirectories(dataDir);
            objectMapper.writeValue(path.toFile(), data);

            System.out.println("----------------------------------------");
            System.out.println("FILE WRITTEN SUCCESSFULLY");
        } catch (IOException exception) {
            System.out.println("----------------------------------------");
            System.out.println("FAILED TO WRITE FILE");
            exception.printStackTrace();

            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to write content file");
        }
    }

    private void validateName(String name) {
        System.out.println("----------------------------------------");
        System.out.println("VALIDATING FILE NAME:");
        System.out.println(name);

        if (!ALLOWED_FILES.contains(name)) {
            System.out.println("INVALID FILE NAME");

            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid content file");
        }

        System.out.println("FILE NAME VALID");
    }
}
