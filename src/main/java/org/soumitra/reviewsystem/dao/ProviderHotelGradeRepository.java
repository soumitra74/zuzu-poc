package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.ProviderHotelGrade;
import org.soumitra.reviewsystem.model.ProviderHotelGradeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderHotelGradeRepository extends JpaRepository<ProviderHotelGrade, ProviderHotelGradeId> {
    
    /**
     * Find provider hotel grade by hotel, provider, category, and review
     */
    Optional<ProviderHotelGrade> findByHotelHotelIdAndProviderProviderIdAndCategoryCategoryIdAndReviewReviewId(
        Integer hotelId, Short providerId, Short categoryId, Long reviewId);
    
    /**
     * Check if provider hotel grade exists by hotel, provider, category, and review
     */
    boolean existsByHotelHotelIdAndProviderProviderIdAndCategoryCategoryIdAndReviewReviewId(
        Integer hotelId, Short providerId, Short categoryId, Long reviewId);
} 