package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.ProviderHotelGrade;
import org.soumitra.reviewsystem.model.ProviderHotelGradeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProviderHotelGradeRepository extends JpaRepository<ProviderHotelGrade, ProviderHotelGradeId> {
    
    /**
     * Find provider hotel grade by hotel, provider, and category
     */
    Optional<ProviderHotelGrade> findByHotelHotelIdAndProviderProviderIdAndCategoryCategoryId(
        Integer hotelId, Short providerId, Short categoryId);
    
    /**
     * Check if provider hotel grade exists by hotel, provider, and category
     */
    boolean existsByHotelHotelIdAndProviderProviderIdAndCategoryCategoryId(
        Integer hotelId, Short providerId, Short categoryId);
} 