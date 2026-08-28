package com.fris.boardportal.notification;

import com.fris.boardportal.actionitem.ActionItem;
import com.fris.boardportal.meeting.Meeting;
import com.fris.boardportal.resolution.Resolution;
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

    public void notifyResolutionOpened(Resolution resolution, String meetingTitle, List<User> recipients) {
        if (recipients.isEmpty()) {
            return;
        }
        String[] bcc = recipients.stream().map(User::getEmail).toArray(String[]::new);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setBcc(bcc);
        message.setSubject("Resolution open for voting: " + resolution.getTitle());
        message.setText(buildResolutionBody(resolution, meetingTitle));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send resolution-opened email for resolution {}", resolution.getId(), e);
        }
    }

    public void notifyActionItemAssigned(ActionItem item, User assignee, String meetingTitle) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(assignee.getEmail());
        message.setSubject("Action item assigned: " + item.getTitle());
        message.setText(buildActionItemBody(item, meetingTitle));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send action-item-assigned email for action item {}", item.getId(), e);
        }
    }

    public void notifyProfileUpdatedByAdmin(User target, List<String> changes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(target.getEmail());
        message.setSubject("Your profile was updated");
        message.setText(buildProfileUpdateBody(changes));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send profile-updated email for user {}", target.getId(), e);
        }
    }

    private String buildProfileUpdateBody(List<String> changes) {
        StringBuilder body = new StringBuilder();
        body.append("An admin updated your profile.\n\n");
        body.append("Changed: ").append(String.join(", ", changes)).append('\n');
        body.append('\n').append(frontendUrl).append("/directory");
        return body.toString();
    }

    private String buildResolutionBody(Resolution resolution, String meetingTitle) {
        StringBuilder body = new StringBuilder();
        body.append('"').append(resolution.getTitle()).append("\" is open for voting.\n\n");
        body.append("Meeting: ").append(meetingTitle).append('\n');
        body.append('\n').append(frontendUrl).append("/meetings/").append(resolution.getMeetingId());
        return body.toString();
    }

    private String buildActionItemBody(ActionItem item, String meetingTitle) {
        StringBuilder body = new StringBuilder();
        body.append("You've been assigned: ").append(item.getTitle()).append("\n\n");
        if (item.getDescription() != null) {
            body.append(item.getDescription()).append("\n\n");
        }
        body.append("Meeting: ").append(meetingTitle).append('\n');
        if (item.getDueDate() != null) {
            body.append("Due: ").append(item.getDueDate()).append('\n');
        }
        body.append('\n').append(frontendUrl).append("/meetings/").append(item.getMeetingId());
        return body.toString();
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
