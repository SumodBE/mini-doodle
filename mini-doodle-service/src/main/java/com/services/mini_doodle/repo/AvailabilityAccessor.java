package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.Availability;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
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
public class AvailabilityAccessor {

    private final AvailabilityRepository repository;

    public DbResult<List<Availability>> getOverlappingSlots(UUID userId, OffsetDateTime start, OffsetDateTime end) {
        try {
            List<Availability> data = repository.findOverlappingSlots(userId, start, end);
            return DbResult.success(data);
        } catch (DataAccessException e) {
            // Log once, wrap, and return
            return DbResult.error("Database error while fetching availability: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<Availability> save(Availability availability) {
        try {
            return DbResult.success(repository.save(availability));
        } catch (DataAccessException e) {
            return DbResult.error("Failed to save availability: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<String> deleteAll(List<Availability> availabilities){
        try {
            repository.deleteAll(availabilities);
            return DbResult.success("Successfully Deleted");
        } catch (DataAccessException e) {
            return DbResult.error("Failed to delete availabilities: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<List<Availability>> saveAll(List<Availability> toSave){
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
            return DbResult.error("Failed to save availabilities: " + e.getMostSpecificCause().getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public DbResult<Availability> addAvailability(UUID userId, OffsetDateTime start, OffsetDateTime end) {
        // 1. Find overlapping or adjacent slots
        // Note: You might need to adjust your query slightly to find slots that *touch* // (e.g., end == start) if you want perfect merging, but overlaps are the priority.
        DbResult<List<Availability>> overlapResult = getOverlappingSlots(userId, start, end);

        if (!overlapResult.isSuccess()) {
            return DbResult.error("Failed to fetch overlaps");
        }

        List<Availability> overlaps = overlapResult.getValue().orElse(Collections.emptyList());

        // 2. Perform the Merge Math (Expand the boundaries)
        OffsetDateTime finalStart = start;
        OffsetDateTime finalEnd = end;

        if (!overlaps.isEmpty()) {
            for (Availability existing : overlaps) {
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
        Availability newSlot = Availability.builder()
                .userId(userId)
                .startTime(finalStart)
                .endTime(finalEnd)
                .build();

        try {
            Availability saved = repository.save(newSlot);
            return DbResult.success(saved);
        } catch (Exception e) {
            return DbResult.error("Failed to save new availability: " + e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public DbResult<String> reduceAvailability(UUID userId, OffsetDateTime removeStart, OffsetDateTime removeEnd) {
        // 1. Find overlapping slots
        DbResult<List<Availability>> overlapResult = getOverlappingSlots(userId, removeStart, removeEnd);
        if (!overlapResult.isSuccess()) {
            return DbResult.error("Failed to fetch overlaps");
        }
        List<Availability> overlaps = overlapResult.getValue().orElse(Collections.emptyList());
        // If nothing overlaps, there is nothing to reduce.
        // We return a specific error so the caller handles the 404/400 logic.
        if (overlaps.isEmpty()) {
            return DbResult.error("No slots found to reduce");
        }
        List<Availability> toSave = new ArrayList<>();
        // 2. Perform the Split Math
        for (Availability existing : overlaps) {
            // Case A: Keep the "Head" (Time BEFORE the removal)
            if (existing.getStartTime().isBefore(removeStart)) {
                toSave.add(Availability.builder()
                        .userId(userId)
                        .startTime(existing.getStartTime())
                        .endTime(removeStart)
                        .build());
            }
            // Case B: Keep the "Tail" (Time AFTER the removal)
            if (existing.getEndTime().isAfter(removeEnd)) {
                toSave.add(Availability.builder()
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
            return DbResult.error("Database error during availability reduction: " + e.getMessage());
        }
    }

}
