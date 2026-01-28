package com.services.mini_doodle.repo;

import com.services.mini_doodle.model.AvailabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<AvailabilityEntity, UUID> {

    @Query("SELECT a FROM AvailabilityEntity a WHERE a.userId = :userId " +
            "AND a.startTime <= :requestedEnd " +
            "AND a.endTime >= :requestedStart")
    List<AvailabilityEntity> findOverlappingSlots(
            @Param("userId") UUID userId,
            @Param("requestedStart") OffsetDateTime start,
            @Param("requestedEnd") OffsetDateTime end
    );

    @Query("SELECT COUNT(a) FROM AvailabilityEntity a " +
     "WHERE a.userId = :userId " +
     "AND a.startTime <= :meetingStart " +
     "AND a.endTime >= :meetingEnd")
    long isUserAvailable(@Param("userId") UUID userId,
                            @Param("meetingStart") OffsetDateTime meetingStart,
                            @Param("meetingEnd") OffsetDateTime meetingEnd);

    List<AvailabilityEntity> findAllByUserId(UUID userId);

}