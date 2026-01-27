package com.services.mini_doodle.service.impl;

import com.services.mini_doodle.exception.BadRequestException;
import com.services.mini_doodle.exception.InternalServerErrorException;
import com.services.mini_doodle.model.*;
import com.services.mini_doodle.repo.AvailabilityAccessor;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.AvailabilityService;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    private final AvailabilityAccessor availabilityAccessor;
    private final UserAccessor userAccessor;

    @Override
    @Transactional
    public AvailabilityResponse addAvailability(AvailabilityRequest request) {

        DbResult<User> userDbResult = userAccessor.findByEmail(request.getEmail());
        if(!userDbResult.isSuccess() || (userDbResult.isSuccess() && userDbResult.getValue().isEmpty())){
            throw BadRequestException.builder()
                    .msg("User not registered. Can't create the availability")
                    .code(2001)
                    .build();
        }

        OffsetDateTime start = request.getStart().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime end = start.plusMinutes(request.getDurationMinutes());

        UUID userId = userDbResult.getValue().get().getId();
        DbResult<List<Availability>> overlapResult = availabilityAccessor.getOverlappingSlots(userId, start, end);

        if (!overlapResult.isSuccess()) {
            throw InternalServerErrorException.builder()
                    .msg("Some error occurred while creating the availability. Try again later")
                    .code(2002)
                    .build();
        }

        if(overlapResult.getValue().isPresent()){
            List<Availability> overlaps = overlapResult.getValue().get();

            // 4. Perform the Merge Math
            if (!overlaps.isEmpty()) {
                for (Availability existing : overlaps) {
                    // Expand our range to include the existing slot boundaries
                    if (existing.getStartTime().isBefore(start)) {
                        start = existing.getStartTime();
                    }
                    if (existing.getEndTime().isAfter(end)) {
                        end = existing.getEndTime();
                    }
                }
                // 5. Delete the old fragments via Accessor
                DbResult<String> deleteResult = availabilityAccessor.deleteAll(overlaps);
                if (!deleteResult.isSuccess()) {
                    throw InternalServerErrorException.builder()
                            .msg("Some error occurred while creating the availability. Try again later")
                            .code(2002)
                            .build();
                }
            }
        }

        // 6. Save the new unified "Super Slot"
        Availability newSlot = Availability.builder()
                .userId(userId)
                .startTime(start)
                .endTime(end)
                .build();

        DbResult<Availability> persisted = availabilityAccessor.save(newSlot);
        if(persisted.isSuccess()){
            return buildAvailabilityRes(newSlot.getUserId(), newSlot.getId());
        }
        throw InternalServerErrorException.builder()
                .msg("Some error occurred while creating the availability. Try again later")
                .code(2002)
                .build();
    }

    @Override
    @Transactional
    public BaseApiResponse removeAvailability(AvailabilityRequest request) {
        DbResult<User> userDbResult = userAccessor.findByEmail(request.getEmail());
        if (!userDbResult.isSuccess() || userDbResult.getValue().isEmpty()) {
            throw BadRequestException.builder()
                    .msg("User not registered. Can't delete the availability")
                    .code(2001)
                    .build();
        }

        UUID userId = userDbResult.getValue().get().getId();
        // 2. Calculate UTC Range to remove
        OffsetDateTime removeStart = request.getStart().withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime removeEnd = removeStart.plusMinutes(request.getDurationMinutes());
        // 3. Find Overlapping Slots
        DbResult<List<Availability>> overlapResult = availabilityAccessor.getOverlappingSlots(userId, removeStart, removeEnd);
        if (!overlapResult.isSuccess()) {
            throw InternalServerErrorException.builder()
                    .msg("Some error occurred while deleting the availability. Try again later")
                    .code(2003)
                    .build();
        }
        List<Availability> toSave = new ArrayList<>();
        if(overlapResult.getValue().isEmpty()){
            throw new BadRequestException("No availabilities found to be deleted.", 2004);
        }
        if(overlapResult.getValue().isPresent()){
            List<Availability> overlaps = overlapResult.getValue().get();
            for (Availability existing : overlaps) {
                // Case A: Part of the slot remains BEFORE the removal range
                if (existing.getStartTime().isBefore(removeStart)) {
                    toSave.add(Availability.builder()
                            .userId(userId)
                            .startTime(existing.getStartTime())
                            .endTime(removeStart)
                            .build());
                }

                // Case B: Part of the slot remains AFTER the removal range
                if (existing.getEndTime().isAfter(removeEnd)) {
                    toSave.add(Availability.builder()
                            .userId(userId)
                            .startTime(removeEnd)
                            .endTime(existing.getEndTime())
                            .build());
                }
            }
            DbResult<String> deleteRes = availabilityAccessor.deleteAll(overlaps);
            if(!deleteRes.isSuccess()){
                throw InternalServerErrorException.builder()
                        .msg("Some error occurred while deleting the availability. Try again later")
                        .code(2003)
                        .build();
            }
        }
        DbResult<List<Availability>> res = availabilityAccessor.saveAll(toSave);

        if(res.isSuccess()){
            return BaseApiResponse.builder()
                    .message("Success")
                    .statusCode(0)
                    .build();
        }

        throw InternalServerErrorException.builder()
                .msg("Some error occurred while deleting the availability. Try again later")
                .code(2003)
                .build();
    }

    private static AvailabilityResponse buildAvailabilityRes(UUID userId, UUID availabilityId){
        return AvailabilityResponse
                .builder()
                .message("Success")
                .userId(userId)
                .id(availabilityId)
                .statusCode(0)
                .build();
    }
}
