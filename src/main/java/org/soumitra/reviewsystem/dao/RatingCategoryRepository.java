package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.RatingCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RatingCategoryRepository extends JpaRepository<RatingCategory, Short> {
    
    /**
     * Find rating category by name
     */
    Optional<RatingCategory> findByCategoryName(String categoryName);
    
    /**
     * Check if rating category exists by name
     */
    boolean existsByCategoryName(String categoryName);
} 