package com.fris.boardportal.user;

import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.organization.OrganizationRepository;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserSummary getCurrentUser(AppUserPrincipal principal) {
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserSummary.from(user, organizationName(user.getOrganizationId()));
    }

    public List<UserSummary> listOrganizationUsers(AppUserPrincipal principal) {
        String orgName = organizationName(principal.getOrganizationId());
        return userRepository.findByOrganizationId(principal.getOrganizationId()).stream()
                .map(user -> UserSummary.from(user, orgName))
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
        return UserSummary.from(user, organizationName(admin.getOrganizationId()));
    }

    @Transactional
    public UserSummary updateUser(AppUserPrincipal admin, UUID targetUserId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndOrganizationId(targetUserId, admin.getOrganizationId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        user.setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);
        return UserSummary.from(user, organizationName(admin.getOrganizationId()));
    }

    private String organizationName(UUID organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization not found"))
                .getName();
    }
}
