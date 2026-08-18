package com.fris.boardportal.organization;

import com.fris.boardportal.audit.AuditAction;
import com.fris.boardportal.audit.AuditEntityType;
import com.fris.boardportal.audit.AuditLogService;
import com.fris.boardportal.auth.dto.AuthResponse;
import com.fris.boardportal.common.ApiException;
import com.fris.boardportal.organization.dto.OrganizationSignupRequest;
import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.security.JwtService;
import com.fris.boardportal.user.Role;
import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.dto.UserSummary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public OrganizationService(OrganizationRepository organizationRepository, UserRepository userRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, AuditLogService auditLogService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse signup(OrganizationSignupRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.adminEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        Organization organization = Organization.create(request.organizationName());
        organizationRepository.save(organization);

        User admin = User.create(
                organization.getId(),
                request.adminEmail(),
                passwordEncoder.encode(request.adminPassword()),
                request.adminFirstName(),
                request.adminLastName(),
                Role.ADMIN);
        userRepository.save(admin);

        auditLogService.record(new AppUserPrincipal(admin), AuditAction.ORGANIZATION_SIGNUP,
                AuditEntityType.ORGANIZATION, organization.getId(),
                "Created organization \"" + organization.getName() + "\" and admin account");

        String token = jwtService.issueToken(admin.getId(), organization.getId(), admin.getEmail(), admin.getRole());
        return new AuthResponse(token, UserSummary.from(admin, organization.getName(), null));
    }
}
