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
        // 3. Call the Reusable Engine
        DbResult<Availability> result = availabilityAccessor.addAvailability(userId, start, end);

        if (result.isSuccess() && result.getValue().isPresent()) {
            Availability savedSlot = result.getValue().get();
            return buildAvailabilityRes(savedSlot.getUserId(), savedSlot.getId());
        }

        // 4. Handle Errors
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
        DbResult<String> reductionResult = availabilityAccessor.reduceAvailability(userId, removeStart, removeEnd);
        // 4. Handle Specific Errors
        if (!reductionResult.isSuccess()) {
            // If the accessor said "error" (DB exception), map to 500
            throw InternalServerErrorException.builder()
                    .msg("Some error occurred while deleting the availability. Try again later")
                    .code(2003)
                    .build();
        }
        return BaseApiResponse.builder()
                .message("Success")
                .statusCode(0)
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
