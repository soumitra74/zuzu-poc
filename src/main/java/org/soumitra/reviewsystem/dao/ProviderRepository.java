package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Short> {
    
    /**
     * Find provider by external ID
     */
    Optional<Provider> findByExternalId(Short externalId);
    
    /**
     * Check if provider exists by external ID
     */
    boolean existsByExternalId(Short externalId);
} 