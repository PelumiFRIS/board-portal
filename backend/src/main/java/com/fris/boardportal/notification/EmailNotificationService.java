package com.fris.boardportal.notification;

import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.user.User;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter WHEN_FORMAT = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC);

    private final JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailNotificationService(JavaMailSender mailSender, @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    public void notifyMeetingScheduled(Meeting meeting, List<User> recipients) {
        if (recipients.isEmpty()) {
            return;
        }
        String[] bcc = recipients.stream().map(User::getEmail).toArray(String[]::new);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setBcc(bcc);
        message.setSubject("New meeting scheduled: " + meeting.getTitle());
        message.setText(buildBody(meeting));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send meeting-scheduled email for meeting {}", meeting.getId(), e);
        }
    }

    private String buildBody(Meeting meeting) {
        StringBuilder body = new StringBuilder();
        body.append(meeting.getTitle()).append(" has been scheduled.\n\n");
        body.append("When: ").append(WHEN_FORMAT.format(meeting.getScheduledStart())).append(" UTC\n");
        if (meeting.getLocation() != null) {
            body.append("Where: ").append(meeting.getLocation()).append('\n');
        }
        body.append('\n').append(frontendUrl).append("/meetings/").append(meeting.getId());
        return body.toString();
    }
}
