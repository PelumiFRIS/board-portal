package com.fris.boardportal.meeting;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarFeedController {

    private final MeetingService meetingService;

    public CalendarFeedController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping("/feed/{token}")
    public ResponseEntity<byte[]> feed(@PathVariable String token) {
        byte[] ics = meetingService.generateFeed(token);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(ics);
    }
}