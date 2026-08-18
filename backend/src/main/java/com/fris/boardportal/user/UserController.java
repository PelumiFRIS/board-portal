package com.fris.boardportal.user;

import com.fris.boardportal.security.AppUserPrincipal;
import com.fris.boardportal.user.dto.CreateUserRequest;
import com.fris.boardportal.user.dto.UpdateUserRequest;
import com.fris.boardportal.user.dto.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/directory")
    public List<UserSummary> directory(@AuthenticationPrincipal AppUserPrincipal principal) {
        return userService.listDirectory(principal);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummary> createUser(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(principal, request));
    }

    @PatchMapping("/{id}")
    public UserSummary updateUser(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return userService.updateUser(principal, id, request);
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<Void> uploadPhoto(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        userService.uploadPhoto(principal, id, file);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        UserPhoto photo = userService.getPhoto(principal, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .body(photo.getPhotoData());
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<Void> deletePhoto(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        userService.deletePhoto(principal, id);
        return ResponseEntity.noContent().build();
    }
}
