package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.ProviderHotelSummary;
import org.soumitra.reviewsystem.model.ProviderHotelSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderHotelSummaryRepository extends JpaRepository<ProviderHotelSummary, ProviderHotelSummaryId> {
    
    /**
     * Find provider hotel summary by hotel, provider, and review
     */
    Optional<ProviderHotelSummary> findByHotelHotelIdAndProviderProviderIdAndReviewReviewId(
        Integer hotelId, Short providerId, Long reviewId);
    
    /**
     * Check if provider hotel summary exists by hotel, provider, and review
     */
    boolean existsByHotelHotelIdAndProviderProviderIdAndReviewReviewId(
        Integer hotelId, Short providerId, Long reviewId);
} 