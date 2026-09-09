package com.fris.boardportal.security;

import com.fris.boardportal.user.User;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long PRESENCE_TOUCH_THROTTLE_SECONDS = 60;

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.parse(token)
                    .flatMap(claims -> resolvePrincipal(claims.userId()))
                    .ifPresent(principal -> authenticate(principal, request));
        }
        filterChain.doFilter(request, response);
    }

    private Optional<AppUserPrincipal> resolvePrincipal(UUID userId) {
        return userRepository.findById(userId)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(user -> {
                    touchLastActive(user);
                    return new AppUserPrincipal(user);
                });
    }

    private void touchLastActive(User user) {
        Instant now = Instant.now();
        if (user.getLastActiveAt() == null
                || user.getLastActiveAt().isBefore(now.minusSeconds(PRESENCE_TOUCH_THROTTLE_SECONDS))) {
            user.setLastActiveAt(now);
            userRepository.save(user);
        }
    }

    private void authenticate(AppUserPrincipal principal, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
