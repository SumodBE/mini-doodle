package com.services.mini_doodle.service;


import com.services.mini_doodle.model.BaseApiResponse;
import com.services.mini_doodle.model.MeetingRequest;
import com.services.mini_doodle.model.MeetingResponse;

import java.util.UUID;

public interface MeetingService {

    MeetingResponse scheduleMeeting(MeetingRequest request);

    BaseApiResponse cancelMeeting(UUID meetingId);
}
