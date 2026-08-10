package com.fris.boardportal.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByOrganizationId(UUID organizationId);

    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
