package com.fris.boardportal.user;

import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserSummary me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.getCurrentUser(principal);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserSummary> listUsers(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.listOrganizationUsers(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummary> createUser(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(principal, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserSummary updateUser(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return userService.updateUser(principal, id, request);
    }
}
