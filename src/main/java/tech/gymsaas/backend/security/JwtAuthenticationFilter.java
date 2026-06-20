package tech.gymsaas.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.gymsaas.backend.service.JwtService;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        return HttpMethod.OPTIONS.matches(request.getMethod())
                || "/".equals(path)
                || "/error".equals(path)
                || "/favicon.ico".equals(path)
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/ping")
                || path.startsWith("/uploads/")
                || path.startsWith("/actuator/health")
                || path.startsWith("/actuator/info");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    SecurityContextHolder.clearContext();
                    writeUnauthorizedResponse(response, "Token is invalid or has expired");
                    return;
                }

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                Long gymId = jwtService.extractGymId(jwt);
                if (gymId != null) {
                    request.setAttribute("gymId", gymId);
                }

                String role = jwtService.extractRole(jwt);
                if (role != null) {
                    request.setAttribute("role", role);
                }
            }

        } catch (Exception ex) {
            System.err.println("JWT Filter extraction error: " + ex.getMessage());
            SecurityContextHolder.clearContext();

            // Fixes the 500 issue by bypassing Spring's filter error forwarder
            writeUnauthorizedResponse(response, "Your session has expired. Please log in again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to bypass Spring Security's default exception router
     * and commit a raw 401 response directly to the client browser.
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonPayload = String.format(
                "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"%s\"}",
                message
        );

        response.getWriter().write(jsonPayload);
        response.getWriter().flush();
    }
}