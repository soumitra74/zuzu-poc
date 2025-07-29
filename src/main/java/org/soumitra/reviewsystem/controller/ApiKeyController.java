package org.soumitra.reviewsystem.controller;

import org.soumitra.reviewsystem.dao.ApiKeyRepository;
import org.soumitra.reviewsystem.model.ApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/keys")
@CrossOrigin(origins = "*")
public class ApiKeyController {
    
    @Autowired
    private ApiKeyRepository apiKeyRepository;
    
    /**
     * Create a new API key
     * POST /api/admin/keys
     */
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createApiKey(@RequestBody CreateApiKeyRequest request) {
        try {
            // Generate a secure API key
            String apiKeyValue = generateApiKey();
            
            // Create new API key
            ApiKey apiKey = new ApiKey(apiKeyValue, request.getName(), request.getRole());
            apiKey.setPermissions(request.getPermissions());
            
            if (request.getExpiresAt() != null) {
                apiKey.setExpiresAt(request.getExpiresAt());
            }
            
            ApiKey savedApiKey = apiKeyRepository.save(apiKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "API key created successfully");
            response.put("apiKey", savedApiKey);
            response.put("keyValue", apiKeyValue); // Only returned once for security
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to create API key: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * List all API keys
     * GET /api/admin/keys
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> getAllApiKeys() {
        try {
            List<ApiKey> apiKeys = apiKeyRepository.findAll();
            
            // Remove the actual key values for security
            apiKeys.forEach(key -> key.setApiKey("***"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("apiKeys", apiKeys);
            response.put("totalKeys", apiKeys.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve API keys: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get API key by ID
     * GET /api/admin/keys/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getApiKeyById(@PathVariable Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                apiKey.setApiKey("***"); // Hide the actual key
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("apiKey", apiKey);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "API key not found with ID: " + id);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to retrieve API key: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Deactivate API key
     * PUT /api/admin/keys/{id}/deactivate
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Map<String, Object>> deactivateApiKey(@PathVariable Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                apiKey.setIsActive(false);
                apiKeyRepository.save(apiKey);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "API key deactivated successfully");
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "API key not found with ID: " + id);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to deactivate API key: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Activate API key
     * PUT /api/admin/keys/{id}/activate
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Map<String, Object>> activateApiKey(@PathVariable Long id) {
        try {
            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(id);
            
            if (apiKeyOpt.isPresent()) {
                ApiKey apiKey = apiKeyOpt.get();
                apiKey.setIsActive(true);
                apiKeyRepository.save(apiKey);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "API key activated successfully");
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "API key not found with ID: " + id);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to activate API key: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Delete API key
     * DELETE /api/admin/keys/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteApiKey(@PathVariable Long id) {
        try {
            if (apiKeyRepository.existsById(id)) {
                apiKeyRepository.deleteById(id);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "API key deleted successfully");
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "API key not found with ID: " + id);
                
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to delete API key: " + e.getMessage());
            errorResponse.put("exception", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Generate a secure API key
     */
    private String generateApiKey() {
        return "key-" + UUID.randomUUID().toString().replace("-", "");
    }
    
    // Request DTOs
    public static class CreateApiKeyRequest {
        private String name;
        private String role;
        private String permissions;
        private LocalDateTime expiresAt;
        
        // Getters and setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getPermissions() {
            return permissions;
        }
        
        public void setPermissions(String permissions) {
            this.permissions = permissions;
        }
        
        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
        
        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}