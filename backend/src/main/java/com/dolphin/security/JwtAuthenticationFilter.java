package com.dolphin.security;

import com.dolphin.model.User;
import com.dolphin.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Reads "Authorization: Bearer &lt;jwt&gt;", validates it and populates the
 * SecurityContext.
 * Invalid tokens leave the request unauthenticated; the entry point then
 * answers 401 for protected routes.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    public static final String AUTH_ERROR_ATTRIBUTE = "dolphin.authError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(header.substring(BEARER_PREFIX.length()).trim(), request);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        JwtService.TokenClaims claims;
        try {
            claims = jwtService.parse(token);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Token has expired");
            log.warn("Rejected expired JWT for {} {}", request.getMethod(), request.getRequestURI());
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Invalid token");
            log.warn("Rejected invalid JWT for {} {}", request.getMethod(), request.getRequestURI());
            return;
        }

        // Re-check the user on every request so deactivated or deleted accounts lose
        // access immediately.
        Optional<User> user = userRepository.findById(claims.userId());
        if (user.isEmpty() || !user.get().isActive()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Account is inactive or no longer exists");
            log.warn("JWT authentication denied for user {}: inactive or missing account", claims.userId());
            return;
        }
        // A password or email change bumps the version, which signs out every session
        // using an older token.
        if (user.get().getTokenVersion() != claims.tokenVersion()) {
            request.setAttribute(AUTH_ERROR_ATTRIBUTE, "Your session has ended. Please sign in again.");
            log.warn("JWT authentication denied for user {}: stale token version", claims.userId());
            return;
        }

        User u = user.get();
        AuthenticatedUser principal = new AuthenticatedUser(u.getId(), u.getEmail(), u.getName(), u.getRole());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
