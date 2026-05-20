package com.trident.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final long ACCESS_TOKEN_SECONDS = 60L * 60L * 8L;

    private final MongoTemplate mongoTemplate;
    private final AuthService authService;
    private final ContentService contentService;

    public ApiController(MongoTemplate mongoTemplate, AuthService authService, ContentService contentService) {
        this.mongoTemplate = mongoTemplate;
        this.authService = authService;
        this.contentService = contentService;
    }

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("message", "Trident Enterprise API", "status", "ok");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest payload) {
        String email = payload.email().toLowerCase().trim();
        Document user = mongoTemplate.findOne(Query.query(Criteria.where("email").is(email)), Document.class, "users");
        if (user == null || !authService.verifyPassword(payload.password(), user.getString("password_hash"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        String token = authService.createAccessToken(user.getString("id"), user.getString("email"));
        ResponseCookie cookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(ACCESS_TOKEN_SECONDS)
                .path("/")
                .build();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", Map.of(
                "id", user.getString("id"),
                "email", user.getString("email"),
                "name", user.getString("name") == null ? "Admin" : user.getString("name"),
                "role", user.getString("role") == null ? "admin" : user.getString("role")
        ));
        response.put("access_token", token);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse servletResponse) {
        ResponseCookie cookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0)
                .path("/")
                .build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(Map.of("message", "Logged out"));
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpServletRequest request) {
        return authService.getCurrentUser(request);
    }

    @GetMapping("/content/{name}")
    public JsonNode getContent(@PathVariable String name) {
        return contentService.read(name);
    }

//    @PutMapping("/content/{name}")
//    public Map<String, String> putContent(@PathVariable String name, @RequestBody(required = false) JsonNode body, HttpServletRequest request) {
//        authService.getCurrentUser(request);
//        contentService.write(name, body);
//        return Map.of("message", "Updated", "name", name);
//    }

    @PostMapping("/inquiries")
    public Map<String, Object> createInquiry(@Valid @RequestBody InquiryCreate payload) {
        Document doc = new Document()
                .append("id", UUID.randomUUID().toString())
                .append("name", payload.name().trim())
                .append("email", payload.email().toLowerCase().trim())
                .append("phone", payload.phone())
                .append("company", payload.company())
                .append("product", payload.product())
                .append("message", payload.message().trim())
                .append("created_at", Instant.now().toString());
        mongoTemplate.insert(doc, "inquiries");
        return inquiryResponse(doc);
    }

    @GetMapping("/inquiries")
    public List<Map<String, Object>> listInquiries(HttpServletRequest request) {
        authService.getCurrentUser(request);
        Query query = new Query().with(Sort.by(Sort.Direction.DESC, "created_at")).limit(1000);
        return mongoTemplate.find(query, Document.class, "inquiries")
                .stream()
                .map(this::inquiryResponse)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/inquiries/{inquiryId}")
    public Map<String, String> deleteInquiry(@PathVariable String inquiryId, HttpServletRequest request) {
        authService.getCurrentUser(request);
        long deleted = mongoTemplate.remove(Query.query(Criteria.where("id").is(inquiryId)), "inquiries").getDeletedCount();
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Inquiry not found");
        }
        return Map.of("message", "Deleted");
    }

    private Map<String, Object> inquiryResponse(Document doc) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", doc.getString("id"));
        response.put("name", doc.getString("name"));
        response.put("email", doc.getString("email"));
        response.put("phone", doc.getString("phone"));
        response.put("company", doc.getString("company"));
        response.put("product", doc.getString("product"));
        response.put("message", doc.getString("message"));
        response.put("created_at", doc.getString("created_at"));
        return response;
    }

    public record LoginRequest(@Email String email, @NotBlank String password) {
    }

    public record InquiryCreate(
            @NotBlank @Size(max = 120) String name,
            @Email String email,
            String phone,
            String company,
            String product,
            @NotBlank @Size(max = 4000) String message
    ) {
    }
}
