package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * Find review by external ID
     */
    Optional<Review> findByReviewExternalId(Long reviewExternalId);
    
    /**
     * Check if review exists by external ID
     */
    boolean existsByReviewExternalId(Long reviewExternalId);
    
    /**
     * Find reviews by hotel external ID
     */
    @Query("SELECT r FROM Review r JOIN r.hotel h WHERE h.externalId = :hotelExternalId")
    java.util.List<Review> findByHotelExternalId(@Param("hotelExternalId") Integer hotelExternalId);
    
    /**
     * Find reviews by provider external ID
     */
    @Query("SELECT r FROM Review r JOIN r.provider p WHERE p.externalId = :providerExternalId")
    java.util.List<Review> findByProviderExternalId(@Param("providerExternalId") Short providerExternalId);
} 