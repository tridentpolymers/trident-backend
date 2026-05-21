package com.trident.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.trident.api.exception.ApiException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public ContentService(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper.copy()
                .enable(SerializationFeature.INDENT_OUTPUT);

        System.out.println("========================================");
        System.out.println("CONTENT SERVICE INITIALIZED");
        System.out.println("USING CLASSPATH RESOURCES");
        System.out.println("========================================");

        try {

            System.out.println("----------------------------------------");
            System.out.println("VERIFYING RESOURCE FILES");

            for (String fileName : ALLOWED_FILES) {

                String resourcePath = "data/" + fileName + ".json";

                ClassPathResource resource =
                        new ClassPathResource(resourcePath);

                System.out.println(
                        resourcePath + " -> " + resource.exists()
                );
            }

        } catch (Exception exception) {

            System.out.println("----------------------------------------");
            System.out.println("FAILED TO VERIFY RESOURCES");

            exception.printStackTrace();
        }

        System.out.println("========================================");
    }

    public JsonNode read(String name) {

        validateName(name);

        String resourcePath = "data/" + name + ".json";

        System.out.println("========================================");
        System.out.println("READ OPERATION");
        System.out.println("========================================");

        System.out.println("REQUESTED FILE NAME:");
        System.out.println(name);

        System.out.println("----------------------------------------");
        System.out.println("RESOURCE PATH:");
        System.out.println(resourcePath);

        try {

            ClassPathResource resource =
                    new ClassPathResource(resourcePath);

            System.out.println("----------------------------------------");
            System.out.println("RESOURCE EXISTS:");
            System.out.println(resource.exists());

            if (!resource.exists()) {

                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        name + " not found"
                );
            }

            JsonNode result =
                    objectMapper.readTree(resource.getInputStream());

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