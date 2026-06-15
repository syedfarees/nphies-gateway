package com.amins.nphies.user.filter;

import com.amins.nphies.model.TenantContext;
import com.amins.nphies.user.JwtService;
import com.amins.nphies.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtService.isValid(token)) {
                    String tenantId = jwtService.extractTenantId(token);
                    String email = jwtService.extractEmail(token);
                    // Both claims must be present — tokens without tid (pre-multi-tenancy)
                    // are treated as unauthenticated to avoid querying the wrong schema.
                    if (tenantId != null && !tenantId.isBlank()
                            && email != null
                            && SecurityContextHolder.getContext().getAuthentication() == null) {
                        TenantContext.set(tenantId);
                        UserDetails userDetails = userService.loadUserByUsername(email);
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            }
            // Public auth endpoints (login, register, etc.) set TenantContext themselves;
            // the finally block clears it for all request paths.
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
