package org.soumitra.reviewsystem.dao;

import org.soumitra.reviewsystem.model.StayInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StayInfoRepository extends JpaRepository<StayInfo, Long> {
    
    /**
     * Find stay info by review ID
     */
    Optional<StayInfo> findByReviewId(Long reviewId);
    
    /**
     * Check if stay info exists by review ID
     */
    boolean existsByReviewId(Long reviewId);
    
    /**
     * Upsert stay info - insert if not exists, update if exists
     * Since StayInfo uses review_id as primary key, this method handles both insert and update
     */
    default StayInfo upsertStayInfo(StayInfo stayInfo) {
        Optional<StayInfo> existing = findByReviewId(stayInfo.getReviewId());
        
        if (existing.isPresent()) {
            // Update existing stay info
            StayInfo existingStayInfo = existing.get();
            existingStayInfo.setRoomTypeId(stayInfo.getRoomTypeId());
            existingStayInfo.setRoomTypeName(stayInfo.getRoomTypeName());
            existingStayInfo.setReviewGroupId(stayInfo.getReviewGroupId());
            existingStayInfo.setReviewGroupName(stayInfo.getReviewGroupName());
            existingStayInfo.setLengthOfStay(stayInfo.getLengthOfStay());
            return save(existingStayInfo);
        } else {
            // Insert new stay info
            return save(stayInfo);
        }
    }
    
    /**
     * Upsert stay info by review ID and stay info data
     * Alternative method that takes individual parameters
     */
    default StayInfo upsertStayInfo(Long reviewId, Integer roomTypeId, String roomTypeName, 
                                   Integer reviewGroupId, String reviewGroupName, Short lengthOfStay) {
        Optional<StayInfo> existing = findByReviewId(reviewId);
        
        if (existing.isPresent()) {
            // Update existing stay info
            StayInfo existingStayInfo = existing.get();
            existingStayInfo.setRoomTypeId(roomTypeId);
            existingStayInfo.setRoomTypeName(roomTypeName);
            existingStayInfo.setReviewGroupId(reviewGroupId);
            existingStayInfo.setReviewGroupName(reviewGroupName);
            existingStayInfo.setLengthOfStay(lengthOfStay);
            return save(existingStayInfo);
        } else {
            // Create new stay info
            StayInfo newStayInfo = StayInfo.builder()
                .reviewId(reviewId)
                .roomTypeId(roomTypeId)
                .roomTypeName(roomTypeName)
                .reviewGroupId(reviewGroupId)
                .reviewGroupName(reviewGroupName)
                .lengthOfStay(lengthOfStay)
                .build();
            return save(newStayInfo);
        }
    }
} 