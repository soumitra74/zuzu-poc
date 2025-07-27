package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Reviewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewerRepository extends JpaRepository<Reviewer, Long> {
    
    /**
     * Find reviewer by display name and country
     */
    Optional<Reviewer> findByDisplayNameAndCountryName(String displayName, String countryName);
    
    /**
     * Check if reviewer exists by display name and country
     */
    boolean existsByDisplayNameAndCountryName(String displayName, String countryName);
} 