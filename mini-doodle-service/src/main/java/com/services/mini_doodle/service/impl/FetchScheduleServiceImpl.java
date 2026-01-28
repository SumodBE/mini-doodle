package com.services.mini_doodle.service.impl;

import com.services.mini_doodle.exception.BadRequestException;
import com.services.mini_doodle.exception.InternalServerErrorException;
import com.services.mini_doodle.model.*;
import com.services.mini_doodle.repo.AvailabilityAccessor;
import com.services.mini_doodle.repo.MeetingsEntityAccessor;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.FetchScheduleService;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FetchScheduleServiceImpl implements FetchScheduleService {

    private final UserAccessor userAccessor;
    private final AvailabilityAccessor availabilityAccessor;
    private final MeetingsEntityAccessor meetingsEntityAccessor;


    @Override
    public FetchScheduleResponse fetchSchedule(FetchScheduleRequest request) {

        //Haven't implemented the logic with start and end
        // As of now fetch everything for a given user.
        OffsetDateTime start = request.getFrom();
        OffsetDateTime end = request.getTo();
        DbResult<User> userDbResult = userAccessor.findByEmail(request.getEmail());
        if(!userDbResult.isSuccess()){
            throw new InternalServerErrorException("Couldn't fetch user schedule. Some error occurred.", 4001);
        }
        if(userDbResult.getValue().isEmpty()){
            throw new BadRequestException("Couldn't fetch user schedule. User not found.", 4002);
        }

        DbResult<List<AvailabilityEntity>> availabilityDbResult = availabilityAccessor
                .findAvailabilityForUser(userDbResult.getValue().get().getId());

        if(!availabilityDbResult.isSuccess() || availabilityDbResult.getValue().isEmpty()){
            throw new InternalServerErrorException("Couldn't fetch user schedule. Some error occurred.", 4001);
        }

        List<AvailabilityEntity> availabilities = availabilityDbResult.getValue().get();

        DbResult<List<MeetingEntity>> meetingEntities = meetingsEntityAccessor
                .findMeetingsByUserId(userDbResult.getValue().get().getId());

        if(!meetingEntities.isSuccess() || meetingEntities.getValue().isEmpty()){
            throw new InternalServerErrorException("Couldn't fetch user schedule. Some error occurred.", 4001);
        }

        List<Availability> availabilitiesRes = availabilities.stream().map(
                availabilityEntity -> Availability.builder()
                        .id(availabilityEntity.getId())
                        .start(availabilityEntity.getStartTime())
                        .end(availabilityEntity.getEndTime())
                        .userId(availabilityEntity.getUserId())
                        .build()
        ).toList();

        List<Meeting> meetings = meetingEntities.getValue().get().stream().map(meetingEntity ->
                Meeting.builder()
                        .title(meetingEntity.getTitle())
                        .id(meetingEntity.getMeetingId())
                        .participants(meetingEntity.getParticipantEmails())
                        .start(meetingEntity.getStartTime())
                        .end(meetingEntity.getEndTime())
                        .organizerId(meetingEntity.getOrganizer().getId())
                        .build()).toList();

        return FetchScheduleResponse.builder()
                .availabilities(availabilitiesRes)
                .meetings(meetings)
                .statusCode(0)
                .message("Success")
                .build();
    }

}
