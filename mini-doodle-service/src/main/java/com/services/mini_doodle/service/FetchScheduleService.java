package com.services.mini_doodle.service;

import com.services.mini_doodle.model.FetchScheduleRequest;
import com.services.mini_doodle.model.FetchScheduleResponse;

public interface FetchScheduleService {

    FetchScheduleResponse fetchSchedule(FetchScheduleRequest request);

}
