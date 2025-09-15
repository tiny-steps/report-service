package com.tinysteps.reportservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to extract user ID from JWT token and add it to request attributes
 * This follows SRP by separating authentication concerns from business logic
 */
@Component
@Slf4j
public class JwtUserExtractionFilter extends OncePerRequestFilter {

    private static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        log.info("JwtUserExtractionFilter called for: {} {}", request.getMethod(), request.getRequestURI());
        
        // Extract user ID from JWT token if present
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("Authentication object: {}", authentication != null ? authentication.getClass().getSimpleName() : "null");
        
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String userId = jwt.getClaimAsString("id");
            
            if (userId != null) {
                // Add user ID to request attributes for downstream use
                request.setAttribute(USER_ID_ATTRIBUTE, Long.parseLong(userId));
                log.info("Extracted user ID from JWT: {}", userId);
            } else {
                log.warn("JWT token present but no 'id' claim found. Available claims: {}", jwt.getClaims().keySet());
            }
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only apply this filter to API endpoints that need user context
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/");
    }
}