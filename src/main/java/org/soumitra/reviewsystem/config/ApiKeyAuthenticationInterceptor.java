package org.soumitra.reviewsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.soumitra.reviewsystem.dao.ApiKeyRepository;
import org.soumitra.reviewsystem.model.ApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ApiKeyAuthenticationInterceptor implements HandlerInterceptor {
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Define role-based permissions
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
        "ADMIN", Set.of("READ", "WRITE", "DELETE", "EXECUTE"),
        "OPERATOR", Set.of("READ", "WRITE", "EXECUTE"),
        "VIEWER", Set.of("READ"),
        "EXECUTOR", Set.of("EXECUTE")
    );
    
    // Define endpoint permissions
    private static final Map<String, String> ENDPOINT_PERMISSIONS = new HashMap<>() {{
        // Job execution endpoints
        put("/api/jobs/run-s3-ingest", "EXECUTE");
        put("/api/jobs/run-record-processor", "EXECUTE");
        
        // Read-only endpoints
        put("/api/jobs/health", "READ");
        put("/api/jobs", "READ");
        put("/api/jobs/s3-files", "READ");
        put("/api/jobs/records", "READ");
        put("/api/jobs/record-errors", "READ");
        
        // Individual resource endpoints
        put("/api/jobs/{jobId}", "READ");
        put("/api/jobs/s3-files/{fileId}", "READ");
        put("/api/jobs/records/{recordId}", "READ");
        put("/api/jobs/record-errors/{recordId}", "READ");
    }};
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Skip authentication for health check endpoint
        if ("/api/jobs/health".equals(requestURI)) {
            return true;
        }
        
        // Extract API key from header
        String apiKeyHeader = request.getHeader("X-API-Key");
        if (apiKeyHeader == null || apiKeyHeader.trim().isEmpty()) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "API key is required");
            return false;
        }
        
        // Validate API key
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findValidApiKey(apiKeyHeader.trim());
        if (apiKeyOpt.isEmpty()) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED.value(), "Invalid or expired API key");
            return false;
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // Update last used timestamp
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        
        // Check permissions
        if (!hasPermission(apiKey, requestURI, method)) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN.value(), "Insufficient permissions for this operation");
            return false;
        }
        
        // Add API key info to request attributes for use in controllers
        request.setAttribute("apiKey", apiKey);
        request.setAttribute("apiKeyRole", apiKey.getRole());
        
        return true;
    }
    
    private boolean hasPermission(ApiKey apiKey, String requestURI, String method) {
        String role = apiKey.getRole();
        Set<String> rolePermissions = ROLE_PERMISSIONS.get(role);
        
        if (rolePermissions == null) {
            return false;
        }
        
        // Determine required permission for this endpoint
        String requiredPermission = getRequiredPermission(requestURI, method);
        
        if (requiredPermission == null) {
            // If no specific permission is defined, allow ADMIN and OPERATOR
            return rolePermissions.contains("ADMIN") || rolePermissions.contains("OPERATOR");
        }
        
        return rolePermissions.contains(requiredPermission);
    }
    
    private String getRequiredPermission(String requestURI, String method) {
        // Handle dynamic path parameters by matching patterns
        if (requestURI.matches("/api/jobs/\\d+")) {
            return "READ";
        }
        if (requestURI.matches("/api/jobs/s3-files/\\d+")) {
            return "READ";
        }
        if (requestURI.matches("/api/jobs/records/\\d+")) {
            return "READ";
        }
        if (requestURI.matches("/api/jobs/record-errors/\\d+")) {
            return "READ";
        }
        
        // Check static endpoints
        return ENDPOINT_PERMISSIONS.get(requestURI);
    }
    
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", message);
        errorResponse.put("status", status);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}