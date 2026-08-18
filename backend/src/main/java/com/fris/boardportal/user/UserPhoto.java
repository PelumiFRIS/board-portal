package com.fris.boardportal.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserPhoto {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "photo_data", nullable = false)
    private byte[] photoData;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserPhoto create(UUID userId, String contentType, byte[] photoData) {
        UserPhoto photo = new UserPhoto();
        photo.setUserId(userId);
        photo.setContentType(contentType);
        photo.setPhotoData(photoData);
        photo.setUpdatedAt(Instant.now());
        return photo;
    }
}
