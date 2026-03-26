package com.music.system.config;

import com.music.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7).trim();
                if (!token.isEmpty() && jwtUtils.validateToken(token) && !jwtUtils.isTokenExpired(token)) {
                    Long userId = jwtUtils.getUserId(token);
                    String username = jwtUtils.getUsername(token);
                    if (userId != null) {
                        AppUserPrincipal principal = new AppUserPrincipal(userId, username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES);
                        // 某些情况下即使 token 校验通过，若 authorities 为空可能导致 isAuthenticated=false
                        // 显式标记为已认证，确保 SecurityUtils.getUserId() 能拿到 userId。
                        authentication.setAuthenticated(true);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }

    @RequiredArgsConstructor
    public static class AppUserPrincipal {
        private final Long userId;
        private final String username;

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
