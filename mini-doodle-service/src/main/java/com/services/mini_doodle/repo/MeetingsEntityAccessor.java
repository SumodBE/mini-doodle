package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.MeetingEntity;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MeetingsEntityAccessor {

    private final MeetingRepository meetingRepository;

    public DbResult<MeetingEntity> saveMeeting(MeetingEntity meeting) {
        try {
            // Since save() returns the saved entity, we wrap it in success
            return DbResult.success(meetingRepository.save(meeting));
        } catch (DataAccessException e) {
            return DbResult.error("Failed to save meeting: " + e.getMessage());
        }
    }

    public DbResult<MeetingEntity> findById(UUID id) {
        try {
            // Using the pattern we fixed: unwrap the Optional into success
            return DbResult.success(meetingRepository.findById(id).orElse(null));
        } catch (DataAccessException e) {
            return DbResult.error("Error finding meeting by ID: " + e.getMessage());
        }
    }

    public DbResult<String> deleteById(UUID id) {
        try {
            meetingRepository.deleteById(id);
            return DbResult.success("Deleted Successfully");
        } catch (DataAccessException e) {
            return DbResult.error("Failed to delete meeting: " + e.getMessage());
        }
    }

    public DbResult<Long> countConflicts(List<String> emails, OffsetDateTime start, OffsetDateTime end){
        try {
            return DbResult.success(meetingRepository.countConflicts(emails, start, end));
        } catch (DataAccessException e) {
            return DbResult.error("Failed to count conflicts: " + e.getMessage());
        }
    }


}
