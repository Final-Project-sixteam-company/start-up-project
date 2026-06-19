package com.startup.common.auth;

import com.startup.common.dto.ApiResponse;
import com.startup.common.dto.ErrorResponse;
import com.startup.common.error.ErrorCode;
import com.startup.domain.auth.entity.User;
import com.startup.domain.auth.error.AuthErrorCode;
import com.startup.domain.auth.error.AuthException;
import com.startup.domain.auth.repository.UserRepository;
import com.startup.domain.auth.support.AuthProperties;
import com.startup.domain.auth.support.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final AuthProperties authProperties;
    private final JsonMapper jsonMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicAuthEndpoint(request) || (isCompatibilityMode() && !authProperties.isJwtSecretConfigured());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String token = resolveBearerToken(request);
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            AuthenticatedUserPrincipal parsedPrincipal = jwtTokenService.parseAccessToken(token);
            User user = userRepository.findById(parsedPrincipal.userId())
                    .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
            if (!user.isActive()) {
                throw new AuthException(AuthErrorCode.USER_NOT_ACTIVE);
            }

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                    user.getId(),
                    user.getEmail(),
                    user.getRole()
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthException e) {
            SecurityContextHolder.clearContext();
            if (canIgnoreAuthFailureInCompatibilityMode(e)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeErrorResponse(response, request, e.getErrorCode(), e.getMessage());
        }
    }

    private boolean isPublicAuthEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "/api/auth/dev".equals(path)
                || "/api/auth/oauth".equals(path)
                || "/api/auth/oauth/kakao/code".equals(path)
                || "/api/auth/refresh".equals(path)
                || "/api/auth/logout".equals(path);
    }

    private boolean isCompatibilityMode() {
        return !authProperties.isRequireAuthentication() && authProperties.isMockFallbackEnabled();
    }

    private boolean canIgnoreAuthFailureInCompatibilityMode(AuthException e) {
        return isCompatibilityMode() && e.getErrorCode() == AuthErrorCode.JWT_SECRET_NOT_CONFIGURED;
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        return token;
    }

    private void writeErrorResponse(
            HttpServletResponse response,
            HttpServletRequest request,
            ErrorCode errorCode,
            String message
    ) throws IOException {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(errorCode.getStatus().value())
                .error(errorCode.getStatus().name())
                .code(errorCode.getCode())
                .message(message)
                .path(request.getRequestURI())
                .build();
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.fail(errorResponse)));
    }
}
