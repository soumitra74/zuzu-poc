package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.ProviderHotelSummary;
import org.soumitra.reviewsystem.model.ProviderHotelSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderHotelSummaryRepository extends JpaRepository<ProviderHotelSummary, ProviderHotelSummaryId> {
    
    /**
     * Find provider hotel summary by hotel and provider
     */
    Optional<ProviderHotelSummary> findByHotelHotelIdAndProviderProviderId(Integer hotelId, Short providerId);
    
    /**
     * Check if provider hotel summary exists by hotel and provider
     */
    boolean existsByHotelHotelIdAndProviderProviderId(Integer hotelId, Short providerId);
} 