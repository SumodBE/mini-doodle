package com.services.mini_doodle.service.impl;

import com.services.mini_doodle.exception.BadRequestException;
import com.services.mini_doodle.exception.InternalServerErrorException;
import com.services.mini_doodle.model.*;
import com.services.mini_doodle.repo.AvailabilityAccessor;
import com.services.mini_doodle.repo.MeetingsEntityAccessor;
import com.services.mini_doodle.repo.UserAccessor;
import com.services.mini_doodle.service.MeetingService;
import com.services.mini_doodle.util.DbResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private final MeetingsEntityAccessor meetingsEntityAccessor;
    private final AvailabilityAccessor availabilityAccessor;
    private final UserAccessor userAccessor;

    @Override
    @Transactional
    public MeetingResponse scheduleMeeting(MeetingRequest request) {
        OffsetDateTime start = request.getStartTime();
        OffsetDateTime end = start.plusMinutes(request.getDurationMinutes());

        List<String> allEmails = new ArrayList<>(request.getParticipantEmails());
        allEmails.add(request.getOrganizerEmail());

        DbResult<User> organizer = userAccessor.findByEmail(request.getOrganizerEmail());

        if(!organizer.isSuccess()){
            throw new InternalServerErrorException("Failed to schedule the meeting. Failed to fetch the organizer's details", 3000);
        }

        else if(organizer.getValue().isEmpty()){
            throw new BadRequestException("Failed to schedule the meeting. Organizer is not registered.", 3001);
        }
        List<User> users = new ArrayList<>();
        for (String email : allEmails) {
            DbResult<User> userExist = userAccessor.findByEmail(email);
            if(!userExist.isSuccess() || userExist.getValue().isEmpty()){
                throw new BadRequestException("One or more users is not registered to schedule the meeting or some error occurred. Try again", 3002);
            }
            users.add(userExist.getValue().get());
            DbResult<Long> isUserAvailable = availabilityAccessor.isUserAvailable(userExist.getValue().get().getId(), start, end);
            if(!isUserAvailable.isSuccess() || isUserAvailable.getValue().isEmpty() || isUserAvailable.getValue().get() == 0){
                throw new BadRequestException("One or more users is not available at the provided time to schedule the meeting or some error occurred. Try again.", 3003);
            }
        }
        DbResult<Long> conflicts = meetingsEntityAccessor.countConflicts(allEmails, start, end);
        if(!conflicts.isSuccess() || conflicts.getValue().isEmpty()){
            throw new InternalServerErrorException("Failed to book meeting due to some error. Try again.", 3004);
        }
        if(conflicts.getValue().get() > 0){
            throw new BadRequestException("One or more participants are already booked in another meeting.", 3005);
        }

        MeetingEntity meeting = MeetingEntity.builder()
                .startTime(start)
                .endTime(end)
                .title(request.getTitle())
                .description(request.getDescription())
                .organizer(organizer.getValue().get())
                .participantEmails(allEmails)
                .build();

        DbResult<MeetingEntity> created = meetingsEntityAccessor.saveMeeting(meeting);
        for(User user: users){
            DbResult<String> res = availabilityAccessor.reduceAvailability(user.getId(), start, end);
            if(!res.isSuccess()){
                throw new InternalServerErrorException("Failed to book meeting due to some error. Try again.", 3006);
            }
        }

        if(created.isSuccess() && created.getValue().isPresent()){
            return MeetingResponse.builder()
                    .message("Success")
                    .statusCode(0)
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .organizerEmail(request.getOrganizerEmail())
                    .meetingId(created.getValue().get().getMeetingId())
                    .startTime(start)
                    .durationMinutes(request.getDurationMinutes())
                    .build();
        }

        throw new InternalServerErrorException("Failed to book meeting due to some error. Try again.", 3006);
    }

    @Override
    @Transactional
    public BaseApiResponse cancelMeeting(UUID meetingId) {

        MeetingEntity meeting = meetingsEntityAccessor.findById(meetingId)
                .getValue()
                .orElseThrow(() -> new BadRequestException("Meeting not found with ID: " + meetingId, 3007));

        List<String> all = meeting.getParticipantEmails();
        OffsetDateTime start = meeting.getStartTime();
        OffsetDateTime end = meeting.getEndTime();
        for(String email :  all){
            userAccessor.findByEmail(email).getValue().ifPresent(user -> {
                availabilityAccessor.addAvailability(user.getId(), start, end);
            });
        }

        meetingsEntityAccessor.deleteById(meetingId);
        return BaseApiResponse.builder()
                .message("Meeting cancelled successfully and availability restored for all participants.")
                .statusCode(200)
                .build();
    }
}
