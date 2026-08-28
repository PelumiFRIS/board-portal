package com.fris.boardportal.user;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.committee.CommitteeService;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.organization.OrganizationRepository;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PHOTO_DIMENSION = 512;
    private static final float PHOTO_JPEG_QUALITY = 0.85f;

    private final UserRepository userRepository;
    private final UserPhotoRepository userPhotoRepository;
    private final OrganizationRepository organizationRepository;
    private final CommitteeService committeeService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public UserService(UserRepository userRepository, UserPhotoRepository userPhotoRepository,
            OrganizationRepository organizationRepository, CommitteeService committeeService,
            PasswordEncoder passwordEncoder, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.userPhotoRepository = userPhotoRepository;
        this.organizationRepository = organizationRepository;
        this.committeeService = committeeService;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    public UserSummary getCurrentUser(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserSummary.from(user, organizationName(user.getOrganizationId()), photoUpdatedAt(user.getId()),
                committeeService.committeesForUser(user.getId()));
    }

    public List<UserSummary> listOrganizationUsers(AppUserPrincipal principal) {
        String orgName = organizationName(principal.getOrganizationId());
        return userRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .map(user -> UserSummary.from(user, orgName, photoUpdatedAt(user.getId()),
                        committeeService.committeesForUser(user.getId())))
                .toList();
    }

    public List<UserSummary> listDirectory(AppUserPrincipal principal) {
        String orgName = organizationName(principal.getOrganizationId());
        return userRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> UserSummary.from(user, orgName, photoUpdatedAt(user.getId()),
                        committeeService.committeesForUser(user.getId())))
                .toList();
    }

    @Transactional
    public UserSummary createUser(AppUserPrincipal admin, CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = User.create(
                admin.getOrganizationId(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.role());
        userRepository.save(user);

        auditLogService.record(admin, AuditAction.USER_CREATED, AuditEntityType.USER, user.getId(),
                "Added " + user.getFirstName() + " " + user.getLastName() + " (" + user.getRole() + ")");

        return UserSummary.from(user, organizationName(admin.getOrganizationId()), null, List.of());
    }

    @Transactional
    public UserSummary updateUser(AppUserPrincipal principal, UUID targetUserId, UpdateUserRequest request) {
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        boolean isSelf = targetUserId.equals(principal.getUserId());

        if (!isAdmin && !isSelf) {
            throw ApiException.forbidden("You can only edit your own profile");
        }
        if (!isAdmin && (request.role() != null || request.status() != null)) {
            throw ApiException.forbidden("Only an admin can change role or status");
        }

        User user = userRepository.findByIdAndOrganizationId(targetUserId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        List<String> changes = new ArrayList<>();
        if (request.role() != null && request.role() != user.getRole()) {
            changes.add("role to " + request.role());
            user.setRole(request.role());
        }
        if (request.status() != null && request.status() != user.getStatus()) {
            changes.add("status to " + request.status());
            user.setStatus(request.status());
        }
        if (request.title() != null && !request.title().equals(user.getTitle())) {
            changes.add("title");
            user.setTitle(request.title());
        }
        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            changes.add("phone");
            user.setPhone(request.phone());
        }
        if (request.bio() != null && !request.bio().equals(user.getBio())) {
            changes.add("bio");
            user.setBio(request.bio());
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        if (!changes.isEmpty()) {
            String summary = isSelf
                    ? "Updated their own profile: " + String.join(", ", changes)
                    : "Changed " + user.getFirstName() + " " + user.getLastName() + "'s " + String.join(", ", changes);
            auditLogService.record(principal, AuditAction.USER_UPDATED, AuditEntityType.USER, user.getId(), summary);
        }

        return UserSummary.from(user, organizationName(principal.getOrganizationId()), photoUpdatedAt(user.getId()),
                committeeService.committeesForUser(user.getId()));
    }

    @Transactional
    public void uploadPhoto(AppUserPrincipal principal, UUID targetUserId, MultipartFile file) {
        requireAdminOrSelf(principal, targetUserId);

        User user = userRepository.findByIdAndOrganizationId(targetUserId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("A photo file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("Photo must be an image file");
        }
        if (file.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw ApiException.badRequest("Photo exceeds the 5MB upload limit");
        }

        byte[] rawData;
        try {
            rawData = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded photo", e);
        }

        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(rawData));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded photo", e);
        }
        if (source == null) {
            throw ApiException.badRequest("Unsupported image format — please upload a JPEG, PNG, GIF, or BMP");
        }

        byte[] data = encodeAsJpeg(normalizePhoto(source));
        String normalizedContentType = "image/jpeg";

        UserPhoto photo = userPhotoRepository.findById(user.getId())
                .orElseGet(() -> UserPhoto.create(user.getId(), normalizedContentType, data));
        photo.setContentType(normalizedContentType);
        photo.setPhotoData(data);
        photo.setUpdatedAt(Instant.now());
        userPhotoRepository.save(photo);

        boolean isSelf = targetUserId.equals(principal.getUserId());
        auditLogService.record(principal, AuditAction.USER_UPDATED, AuditEntityType.USER, user.getId(),
                isSelf ? "Updated their own photo" : "Updated " + user.getFirstName() + " " + user.getLastName() + "'s photo");
    }

    public UserPhoto getPhoto(AppUserPrincipal principal, UUID targetUserId) {
        userRepository.findByIdAndOrganizationId(targetUserId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return userPhotoRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("No photo for this user"));
    }

    @Transactional
    public void deletePhoto(AppUserPrincipal principal, UUID targetUserId) {
        requireAdminOrSelf(principal, targetUserId);
        User user = userRepository.findByIdAndOrganizationId(targetUserId, principal.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        userPhotoRepository.deleteById(user.getId());

        boolean isSelf = targetUserId.equals(principal.getUserId());
        auditLogService.record(principal, AuditAction.USER_UPDATED, AuditEntityType.USER, user.getId(),
                isSelf ? "Removed their own photo" : "Removed " + user.getFirstName() + " " + user.getLastName() + "'s photo");
    }

    @Transactional
    public String getOrCreateCalendarToken(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (user.getCalendarToken() == null) {
            user.setCalendarToken(UUID.randomUUID().toString());
            userRepository.save(user);
        }
        return user.getCalendarToken();
    }

    @Transactional
    public String regenerateCalendarToken(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setCalendarToken(UUID.randomUUID().toString());
        userRepository.save(user);
        return user.getCalendarToken();
    }

    private void requireAdminOrSelf(AppUserPrincipal principal, UUID targetUserId) {
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        boolean isSelf = targetUserId.equals(principal.getUserId());
        if (!isAdmin && !isSelf) {
            throw ApiException.forbidden("You can only manage your own photo");
        }
    }

    private BufferedImage normalizePhoto(BufferedImage source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        BufferedImage cropped = source.getSubimage(x, y, size, size);

        int targetSize = Math.min(size, MAX_PHOTO_DIMENSION);
        BufferedImage normalized = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = normalized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, targetSize, targetSize);
        g.drawImage(cropped, 0, 0, targetSize, targetSize, null);
        g.dispose();
        return normalized;
    }

    private byte[] encodeAsJpeg(BufferedImage image) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(PHOTO_JPEG_QUALITY);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), params);
            }
            writer.dispose();
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode photo", e);
        }
    }

    private Instant photoUpdatedAt(UUID userId) {
        return userPhotoRepository.findById(userId).map(UserPhoto::getUpdatedAt).orElse(null);
    }

    private String organizationName(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization not found"))
                .getName();
    }
}
