package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.MeetingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingRepository extends JpaRepository<MeetingEntity, UUID> {

    @Query(value = """
        SELECT COUNT(*)\s
        FROM meetings m\s
        JOIN users u ON m.organizer_id = u.id\s
        WHERE (
            -- 1. Check if the Organizer matches any of the emails
            u.email IN (:emails)\s
            OR\s
            -- 2. Check if the JSONB list contains any of the emails
            EXISTS (
                SELECT 1\s
                FROM jsonb_array_elements_text(m.participant_emails) as participant_email\s
                WHERE participant_email IN (:emails)
            )
        )\s
        -- 3. Check for Time Overlap
        AND m.start_time < :endTime\s
        AND m.end_time > :startTime
   \s""", nativeQuery = true)
    long countConflicts(@Param("emails") List<String> emails,
                        @Param("startTime") OffsetDateTime startTime,
                        @Param("endTime") OffsetDateTime endTime);

    List<MeetingEntity> findAllByUserId(UUID userId);

}
