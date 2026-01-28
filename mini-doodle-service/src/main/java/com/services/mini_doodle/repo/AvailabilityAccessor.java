package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.AvailabilityEntity;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AvailabilityAccessor {

    private final AvailabilityRepository repository;

    public DbResult<List<AvailabilityEntity>> getOverlappingSlots(UUID userId, OffsetDateTime start, OffsetDateTime end) {
        try {
            List<AvailabilityEntity> data = repository.findOverlappingSlots(userId, start, end);
            return DbResult.success(data);
        } catch (DataAccessException e) {
            // Log once, wrap, and return
            return DbResult.error("Database error while fetching availability: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<AvailabilityEntity> save(AvailabilityEntity availabilityEntity) {
        try {
            return DbResult.success(repository.save(availabilityEntity));
        } catch (DataAccessException e) {
            return DbResult.error("Failed to save availabilityEntity: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<String> deleteAll(List<AvailabilityEntity> availabilities){
        try {
            repository.deleteAll(availabilities);
            return DbResult.success("Successfully Deleted");
        } catch (DataAccessException e) {
            log.error("Failed to delete all availabilities of size {}", availabilities.size());
            return DbResult.error("Failed to delete availabilities: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<List<AvailabilityEntity>> saveAll(List<AvailabilityEntity> toSave){
        try {
            return DbResult.success(repository.saveAll(toSave));
        } catch (DataAccessException e) {
            return DbResult.error("Failed to save availabilities: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<Long> isUserAvailable(UUID userId, OffsetDateTime start, OffsetDateTime end){
        try {
                return DbResult.success(repository.isUserAvailable(userId, start, end));
        } catch (DataAccessException e) {
            log.error("Failed to check if user {} is available for start {}, end {}", userId, start, end);
            return DbResult.error("Failed to save availabilities: " + e.getMostSpecificCause().getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public DbResult<AvailabilityEntity> addAvailability(UUID userId, OffsetDateTime start, OffsetDateTime end) {
        // 1. Find overlapping or adjacent slots
        // Note: You might need to adjust your query slightly to find slots that *touch* // (e.g., end == start) if you want perfect merging, but overlaps are the priority.
        DbResult<List<AvailabilityEntity>> overlapResult = getOverlappingSlots(userId, start, end);

        if (!overlapResult.isSuccess()) {
            return DbResult.error("Failed to fetch overlaps");
        }

        List<AvailabilityEntity> overlaps = overlapResult.getValue().orElse(Collections.emptyList());

        // 2. Perform the Merge Math (Expand the boundaries)
        OffsetDateTime finalStart = start;
        OffsetDateTime finalEnd = end;

        if (!overlaps.isEmpty()) {
            for (AvailabilityEntity existing : overlaps) {
                if (existing.getStartTime().isBefore(finalStart)) {
                    finalStart = existing.getStartTime();
                }
                if (existing.getEndTime().isAfter(finalEnd)) {
                    finalEnd = existing.getEndTime();
                }
            }

            // 3. Delete old fragments
            try {
                repository.deleteAll(overlaps);
            } catch (Exception e) {
                return DbResult.error("Failed to delete overlapping slots: " + e.getMessage());
            }
        }

        // 4. Save the new unified "Super Slot"
        AvailabilityEntity newSlot = AvailabilityEntity.builder()
                .userId(userId)
                .startTime(finalStart)
                .endTime(finalEnd)
                .build();

        try {
            AvailabilityEntity saved = repository.save(newSlot);
            return DbResult.success(saved);
        } catch (Exception e) {
            log.error("Failed to save new availability for user {}. start {}, end {}", userId, start, end);
            return DbResult.error("Failed to save new availability: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public DbResult<String> reduceAvailability(UUID userId, OffsetDateTime removeStart, OffsetDateTime removeEnd) {
        // 1. Find overlapping slots
        DbResult<List<AvailabilityEntity>> overlapResult = getOverlappingSlots(userId, removeStart, removeEnd);
        if (!overlapResult.isSuccess()) {
            return DbResult.error("Failed to fetch overlaps");
        }
        List<AvailabilityEntity> overlaps = overlapResult.getValue().orElse(Collections.emptyList());
        // If nothing overlaps, there is nothing to reduce.
        // We return a specific error so the caller handles the 404/400 logic.
        if (overlaps.isEmpty()) {
            return DbResult.error("No slots found to reduce");
        }
        List<AvailabilityEntity> toSave = new ArrayList<>();
        // 2. Perform the Split Math
        for (AvailabilityEntity existing : overlaps) {
            // Case A: Keep the "Head" (Time BEFORE the removal)
            if (existing.getStartTime().isBefore(removeStart)) {
                toSave.add(AvailabilityEntity.builder()
                        .userId(userId)
                        .startTime(existing.getStartTime())
                        .endTime(removeStart)
                        .build());
            }
            // Case B: Keep the "Tail" (Time AFTER the removal)
            if (existing.getEndTime().isAfter(removeEnd)) {
                toSave.add(AvailabilityEntity.builder()
                        .userId(userId)
                        .startTime(removeEnd)
                        .endTime(existing.getEndTime())
                        .build());
            }
        }

        // 3. Commit the transaction (Delete Old -> Save New)
        try {
            repository.deleteAll(overlaps);
            repository.saveAll(toSave);
            return DbResult.success("success");
        } catch (DataAccessException e) {
            log.error("Failed to reduce availability for user {}, start {}, end {}", userId, removeStart, removeEnd);
            return DbResult.error("Database error during availability reduction: " + e.getMessage());
        }
    }

    @Transactional
    public DbResult<List<AvailabilityEntity>> findAvailabilityForUser(UUID userId){
        try{
            return DbResult.success(repository.findAllByUserId(userId));
        } catch (DataAccessException e){
            log.error("Failed to fetch availabilities for given userId {}", userId);
            return DbResult.error(e.getMessage());
        }
    }

}
