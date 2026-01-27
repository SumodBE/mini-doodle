package com.services.mini_doodle.controller;

import com.services.mini_doodle.api.MeetingsApi;
import com.services.mini_doodle.model.BaseApiResponse;
import com.services.mini_doodle.model.MeetingRequest;
import com.services.mini_doodle.model.MeetingResponse;
import com.services.mini_doodle.model.RegisterUserResponse;
import com.services.mini_doodle.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MeetingsController implements MeetingsApi {

    private final MeetingService meetingService;

    @Override
    public ResponseEntity<BaseApiResponse> cancelMeeting(UUID id) {
        return ResponseEntity.ok(meetingService.cancelMeeting(id));
    }

    @Override
    public ResponseEntity<MeetingResponse> scheduleMeeting(MeetingRequest meetingRequest) {
        return ResponseEntity.ok(meetingService.scheduleMeeting(meetingRequest));
    }
}
