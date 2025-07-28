package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Hotel;
import org.soumitra.reviewsystem.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Integer> {
    
    /**
     * Find hotel by external ID
     */
    Optional<Hotel> findByExternalId(Integer externalId);
    
    /**
     * Check if hotel exists by external ID
     */
    boolean existsByExternalId(Integer externalId);
    
    /**
     * Find hotel by external ID and provider
     */
    Optional<Hotel> findByExternalIdAndProvider(Integer externalId, Provider provider);
    
    /**
     * Check if hotel exists by external ID and provider
     */
    boolean existsByExternalIdAndProvider(Integer externalId, Provider provider);
} 