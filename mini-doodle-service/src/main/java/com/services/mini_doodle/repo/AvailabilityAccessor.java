package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.Availability;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
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
            return DbResult.error("Failed to save availability: " + e.getMostSpecificCause().getMessage());
        }
    }

    public DbResult<List<Availability>> saveAll(List<Availability> toSave){
        try {
            return DbResult.success(repository.saveAll(toSave));
        } catch (DataAccessException e) {
            return DbResult.error("Failed to save availability: " + e.getMostSpecificCause().getMessage());
        }
    }

}
