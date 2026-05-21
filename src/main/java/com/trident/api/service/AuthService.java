package com.trident.api;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.Document;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements ApplicationRunner {
    private static final long ACCESS_TOKEN_MINUTES = 60L * 8L;

    private final MongoTemplate mongoTemplate;
    private final AppProperties properties;

    public AuthService(MongoTemplate mongoTemplate, AppProperties properties) {
        this.mongoTemplate = mongoTemplate;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        mongoTemplate.indexOps("users").ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
        mongoTemplate.indexOps("inquiries").ensureIndex(new Index().on("created_at", Sort.Direction.ASC));
        seedAdmin();
    }

    public void seedAdmin() {
        String email = properties.getAdminEmail().toLowerCase().trim();
        Query query = Query.query(Criteria.where("email").is(email));
        Document existing = mongoTemplate.findOne(query, Document.class, "users");

        if (existing == null) {
            Document user = new Document()
                    .append("id", UUID.randomUUID().toString())
                    .append("email", email)
                    .append("password_hash", hashPassword(properties.getAdminPassword()))
                    .append("name", "Admin")
                    .append("role", "admin")
                    .append("created_at", Instant.now().toString());
            mongoTemplate.insert(user, "users");
            return;
        }

        String passwordHash = existing.getString("password_hash");
        if (passwordHash == null || !verifyPassword(properties.getAdminPassword(), passwordHash)) {
            mongoTemplate.updateFirst(query, new Update().set("password_hash", hashPassword(properties.getAdminPassword())), "users");
        }
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean verifyPassword(String plain, String hashed) {
        return BCrypt.checkpw(plain, hashed);
    }

    public String createAccessToken(String userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("type", "access")
                .expiration(Date.from(now.plus(ACCESS_TOKEN_MINUTES, ChronoUnit.MINUTES)))
                .issuedAt(Date.from(now))
                .signWith(signingKey())
                .compact();
    }

    public Map<String, Object> getCurrentUser(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null || token.isBlank()) {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }
        }

        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        try {
            Claims claims = Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }

            Document user = mongoTemplate.findOne(Query.query(Criteria.where("id").is(claims.getSubject())), Document.class, "users");
            if (user == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "User not found");
            }
            return publicUser(user);
        } catch (ExpiredJwtException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token expired");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }

    public Map<String, Object> publicUser(Document user) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : user.entrySet()) {
            String key = entry.getKey();
            if (!"_id".equals(key) && !"password_hash".equals(key)) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }

    private SecretKey signingKey() {
        byte[] bytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
