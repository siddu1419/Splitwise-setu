package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            logger.debug("Processing request for URI: {}", request.getRequestURI());
            logger.debug("Authorization header: {}", request.getHeader("Authorization"));
            logger.debug("JWT token present: {}", jwt != null);
            
            if (jwt != null) {
                logger.debug("JWT token length: {}", jwt.length());
                logger.debug("JWT token: {}", jwt);
                
                if (tokenProvider.validateToken(jwt)) {
                    logger.debug("Token validation successful");
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    logger.debug("Username from token: {}", username);
                    
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.debug("User details loaded successfully for: {}", username);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("Authentication successful for user: {}", username);
                    logger.debug("User authorities: {}", userDetails.getAuthorities());
                } else {
                    logger.error("Token validation failed");
                }
            } else {
                logger.error("No JWT token found in request");
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
            logger.error("Exception details: {}", ex.getMessage());
            ex.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Raw Authorization header: {}", bearerToken);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            logger.debug("Extracted token length: {}", token.length());
            return token;
        }
        logger.debug("No valid Bearer token found in Authorization header");
        return null;
    }
} 