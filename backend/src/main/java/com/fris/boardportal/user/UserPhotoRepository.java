package com.fris.boardportal.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPhotoRepository extends JpaRepository<UserPhoto, UUID> {
}
