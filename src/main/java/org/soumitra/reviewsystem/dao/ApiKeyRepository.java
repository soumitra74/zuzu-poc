package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    /**
     * Find API key by the actual key value
     */
    Optional<ApiKey> findByApiKey(String apiKey);
    
    /**
     * Find all active API keys
     */
    List<ApiKey> findByIsActiveTrue();
    
    /**
     * Find API keys by role
     */
    List<ApiKey> findByRole(String role);
    
    /**
     * Find active API keys by role
     */
    List<ApiKey> findByRoleAndIsActiveTrue(String role);
    
    /**
     * Find API keys by name
     */
    List<ApiKey> findByName(String name);
    
    /**
     * Check if API key exists
     */
    boolean existsByApiKey(String apiKey);
    
    /**
     * Find API keys that are not expired
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.isActive = true AND (ak.expiresAt IS NULL OR ak.expiresAt > CURRENT_TIMESTAMP)")
    List<ApiKey> findValidApiKeys();
    
    /**
     * Find API key by key value and check if it's valid
     */
    @Query("SELECT ak FROM ApiKey ak WHERE ak.apiKey = :apiKey AND ak.isActive = true AND (ak.expiresAt IS NULL OR ak.expiresAt > CURRENT_TIMESTAMP)")
    Optional<ApiKey> findValidApiKey(@Param("apiKey") String apiKey);
}