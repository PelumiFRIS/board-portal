package com.fris.boardportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.meeting.dto.CreateMeetingRequest;
import com.fris.boardportal.meeting.dto.MeetingSummary;
import com.fris.boardportal.support.IntegrationTestSupport;
import com.fris.boardportal.user.dto.CalendarTokenResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CalendarFeedFlowTest extends IntegrationTestSupport {

    @Test
    void tokenIsLazilyCreatedAndStableAcrossCalls() {
        AuthResponse admin = signup(uniqueEmail(), "Feed Token Org");

        ResponseEntity<CalendarTokenResponse> first = restTemplate.exchange(
                "/api/users/me/calendar-token", HttpMethod.GET, authedRequest(admin.accessToken()),
                CalendarTokenResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(first.getBody().token()).isNotBlank();

        ResponseEntity<CalendarTokenResponse> second = restTemplate.exchange(
                "/api/users/me/calendar-token", HttpMethod.GET, authedRequest(admin.accessToken()),
                CalendarTokenResponse.class);
        assertThat(second.getBody().token()).isEqualTo(first.getBody().token());
    }

    @Test
    void regeneratingInvalidatesTheOldToken() {
        AuthResponse admin = signup(uniqueEmail(), "Feed Regen Org");
        String oldToken = restTemplate.exchange(
                "/api/users/me/calendar-token", HttpMethod.GET, authedRequest(admin.accessToken()),
                CalendarTokenResponse.class).getBody().token();

        ResponseEntity<CalendarTokenResponse> regenerated = restTemplate.exchange(
                "/api/users/me/calendar-token/regenerate", HttpMethod.POST, authedRequest(admin.accessToken()),
                CalendarTokenResponse.class);
        String newToken = regenerated.getBody().token();
        assertThat(newToken).isNotEqualTo(oldToken);

        ResponseEntity<String> oldFeed = restTemplate.exchange(
                "/api/calendar/feed/" + oldToken, HttpMethod.GET, null, String.class);
        assertThat(oldFeed.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<String> newFeed = restTemplate.exchange(
                "/api/calendar/feed/" + newToken, HttpMethod.GET, null, String.class);
        assertThat(newFeed.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void feedIsUnauthenticatedAndScopedToOwnOrganization() {
        AuthResponse admin = signup(uniqueEmail(), "Feed Org A");
        AuthResponse otherAdmin = signup(uniqueEmail(), "Feed Org B");

        scheduleMeeting(admin.accessToken(), "Org A Meeting");
        scheduleMeeting(otherAdmin.accessToken(), "Org B Meeting");

        String token = restTemplate.exchange(
                "/api/users/me/calendar-token", HttpMethod.GET, authedRequest(admin.accessToken()),
                CalendarTokenResponse.class).getBody().token();

        ResponseEntity<String> feed = restTemplate.exchange(
                "/api/calendar/feed/" + token, HttpMethod.GET, null, String.class);
        assertThat(feed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(feed.getHeaders().getContentType()).isNotNull();
        assertThat(feed.getHeaders().getContentType().toString()).contains("text/calendar");
        assertThat(feed.getBody()).contains("BEGIN:VCALENDAR");
        assertThat(feed.getBody()).contains("SUMMARY:Org A Meeting");
        assertThat(feed.getBody()).doesNotContain("Org B Meeting");
    }

    @Test
    void unknownTokenReturnsNotFound() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/calendar/feed/does-not-exist", HttpMethod.GET, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void scheduleMeeting(String adminToken, String title) {
        Instant start = Instant.now().plus(7, ChronoUnit.DAYS);
        ResponseEntity<MeetingSummary> response = restTemplate.exchange(
                "/api/meetings", HttpMethod.POST,
                authedRequest(adminToken, new CreateMeetingRequest(title, null, null, start, null, null, defaultMeetingTypeId(adminToken))),
                MeetingSummary.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}