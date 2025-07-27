package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Hotel;
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
} 