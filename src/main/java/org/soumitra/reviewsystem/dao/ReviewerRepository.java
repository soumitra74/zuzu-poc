package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.Reviewer;
import org.soumitra.reviewsystem.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

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

    /**
     * Find reviewers by provider
     */
    List<Reviewer> findByProvider(Provider provider);
    
    /**
     * Find reviewers by provider ID
     */
    List<Reviewer> findByProviderProviderId(Short providerId);
    
    /**
     * Find reviewer by display name, country, and provider
     */
    Optional<Reviewer> findByDisplayNameAndCountryNameAndProvider(String displayName, String countryName, Provider provider);
    
    /**
     * Check if reviewer exists by display name, country, and provider
     */
    boolean existsByDisplayNameAndCountryNameAndProvider(String displayName, String countryName, Provider provider);
} 