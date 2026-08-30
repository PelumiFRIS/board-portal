package com.fris.boardportal.security;

import com.fris.boardportal.apikey.ApiKey;
import com.fris.boardportal.apikey.ApiKeyHasher;
import com.fris.boardportal.apikey.ApiKeyRepository;
import com.fris.boardportal.user.UserRepository;
import com.fris.boardportal.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String PUBLIC_API_PATH_PREFIX = "/api/v1/";

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, UserRepository userRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith(PUBLIC_API_PATH_PREFIX)) {
            String rawKey = request.getHeader(API_KEY_HEADER);
            if (rawKey != null && !rawKey.isBlank()) {
                resolvePrincipal(rawKey).ifPresent(principal -> authenticate(principal, request));
            }
        }
        filterChain.doFilter(request, response);
    }

    private Optional<AppUserPrincipal> resolvePrincipal(String rawKey) {
        Optional<ApiKey> apiKey = apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(rawKey));
        if (apiKey.isEmpty()) {
            return Optional.empty();
        }
        Optional<AppUserPrincipal> principal = userRepository.findById(apiKey.get().getCreatedBy())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(AppUserPrincipal::new);
        if (principal.isPresent()) {
            ApiKey key = apiKey.get();
            key.setLastUsedAt(Instant.now());
            apiKeyRepository.save(key);
        }
        return principal;
    }

    private void authenticate(AppUserPrincipal principal, HttpServletRequest request) {
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
