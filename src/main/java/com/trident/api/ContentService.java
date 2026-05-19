package com.trident.api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Service
public class ContentService {

    private static final Set<String> ALLOWED_FILES = Set.of(
            "content",
            "products",
            "settings",
            "testimonials",
            "faq"
    );

    private final ObjectMapper objectMapper;
    private final Path dataDir;

    public ContentService(ObjectMapper objectMapper, AppProperties properties) {

        this.objectMapper = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT);

        // RAW PROPERTY VALUE
        System.out.println("========================================");
        System.out.println("CONTENT SERVICE INITIALIZATION");
        System.out.println("========================================");

        System.out.println("DATA_DIR PROPERTY VALUE:");
        System.out.println(properties.getDataDir());

        // CURRENT WORKING DIRECTORY
        System.out.println("----------------------------------------");
        System.out.println("CURRENT WORKING DIRECTORY:");
        System.out.println(Paths.get("").toAbsolutePath());

        // RESOLVED DATA DIR
        this.dataDir = Path.of(properties.getDataDir())
                .toAbsolutePath()
                .normalize();

        System.out.println("----------------------------------------");
        System.out.println("RESOLVED DATA DIRECTORY:");
        System.out.println(this.dataDir);

        // DIRECTORY EXISTS?
        System.out.println("----------------------------------------");
        System.out.println("DATA DIRECTORY EXISTS:");
        System.out.println(Files.exists(this.dataDir));

        // IS DIRECTORY?
        System.out.println("----------------------------------------");
        System.out.println("IS DIRECTORY:");
        System.out.println(Files.isDirectory(this.dataDir));

        // LIST FILES
        System.out.println("----------------------------------------");
        System.out.println("FILES INSIDE DATA DIRECTORY:");

        try {
            if (Files.exists(this.dataDir)) {
                Files.list(this.dataDir)
                        .forEach(path -> System.out.println("FILE -> " + path));
            } else {
                System.out.println("DATA DIRECTORY DOES NOT EXIST");
            }
        } catch (IOException e) {
            System.out.println("FAILED TO LIST DIRECTORY FILES");
            e.printStackTrace();
        }

        System.out.println("========================================");
    }

    public JsonNode read(String name) {

        validateName(name);

        Path path = dataDir.resolve(name + ".json");

        System.out.println("========================================");
        System.out.println("READ OPERATION");
        System.out.println("========================================");

        System.out.println("REQUESTED FILE NAME:");
        System.out.println(name);

        System.out.println("----------------------------------------");
        System.out.println("FULL FILE PATH:");
        System.out.println(path);

        System.out.println("----------------------------------------");
        System.out.println("FILE EXISTS:");
        System.out.println(Files.exists(path));

        System.out.println("----------------------------------------");
        System.out.println("IS REGULAR FILE:");
        System.out.println(Files.isRegularFile(path));

        try {

            if (Files.exists(path)) {
                System.out.println("----------------------------------------");
                System.out.println("FILE SIZE:");
                System.out.println(Files.size(path) + " bytes");
            }

        } catch (IOException e) {
            System.out.println("FAILED TO GET FILE SIZE");
            e.printStackTrace();
        }

        if (!Files.exists(path)) {
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

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read content file"
            );
        }
    }

    public void write(String name, JsonNode body) {

        validateName(name);

        System.out.println("========================================");
        System.out.println("WRITE OPERATION");
        System.out.println("========================================");

        System.out.println("REQUESTED FILE NAME:");
        System.out.println(name);

        if (body == null || body.isNull()) {

            System.out.println("----------------------------------------");
            System.out.println("BODY IS NULL");

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Body required"
            );
        }

        JsonNode data = body.has("data")
                ? body.get("data")
                : body;

        Path targetFile = dataDir.resolve(name + ".json");

        System.out.println("----------------------------------------");
        System.out.println("TARGET FILE:");
        System.out.println(targetFile);

        try {

            System.out.println("----------------------------------------");
            System.out.println("CREATING DIRECTORIES IF NOT EXISTS");

            Files.createDirectories(dataDir);

            System.out.println("DIRECTORY READY");

            objectMapper.writeValue(targetFile.toFile(), data);

            System.out.println("----------------------------------------");
            System.out.println("FILE WRITTEN SUCCESSFULLY");

            System.out.println("FILE EXISTS AFTER WRITE:");
            System.out.println(Files.exists(targetFile));

            System.out.println("FILE SIZE:");
            System.out.println(Files.size(targetFile) + " bytes");

        } catch (IOException exception) {

            System.out.println("----------------------------------------");
            System.out.println("FAILED TO WRITE FILE");

            exception.printStackTrace();

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to write content file"
            );
        }
    }

    private void validateName(String name) {

        System.out.println("----------------------------------------");
        System.out.println("VALIDATING FILE NAME:");
        System.out.println(name);

        if (!ALLOWED_FILES.contains(name)) {

            System.out.println("INVALID FILE NAME");

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid content file"
            );
        }

        System.out.println("FILE NAME VALID");
    }
}