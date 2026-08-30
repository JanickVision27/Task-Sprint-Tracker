package com.tracker.backend.config;

import com.tracker.backend.service.JwtService; // Our token reader
import jakarta.servlet.FilterChain; // Spring's filter pipeline
import jakarta.servlet.http.HttpServletRequest; // The incoming request
import jakarta.servlet.http.HttpServletResponse; // The outgoing response
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Spring's auth object
import org.springframework.security.core.context.SecurityContextHolder; // Spring's memory of who is logged in
import org.springframework.security.core.userdetails.UserDetails; // Spring's user shape
import org.springframework.security.core.userdetails.UserDetailsService; // Our custom database user loader
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; // Attaches request details to auth
import org.springframework.stereotype.Component; // Registers this as a Spring bean
import org.springframework.web.filter.OncePerRequestFilter; // Ensures this filter only runs once per request

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, jakarta.servlet.ServletException {

        // 1. Get the "Authorization" header from the incoming request
        final String authHeader = request.getHeader("Authorization");

        // 2. If the header is missing, or doesn't start with "Bearer ", let the request
        // pass through unauthenticated
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the JWT token by removing the "Bearer " prefix
        final String jwt = authHeader.substring(7);

        // 4. Extract the email from the token using our JwtService
        final String userEmail = jwtService.extractEmail(jwt);

        // 5. If we found an email, and no one is logged in yet for this request...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the user details from our database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Check if the token is still valid (not expired, signature matches)
            if (jwtService.isTokenValid(jwt)) {

                // 7. Create an "Authentication Ticket" telling Spring this user is officially
                // logged in
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                // Attach request details (like IP address) to the ticket
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 8. Hand the ticket to Spring Security's Gatekeeper. The user is now
                // authenticated!
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. Pass the request to the next filter or the Controller
        filterChain.doFilter(request, response);
    }
}
