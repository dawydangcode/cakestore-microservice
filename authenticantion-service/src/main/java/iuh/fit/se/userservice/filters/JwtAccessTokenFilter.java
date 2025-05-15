package iuh.fit.se.userservice.filters;

import iuh.fit.se.userservice.auths.UserPrincipal;
import iuh.fit.se.userservice.entities.Token;
import iuh.fit.se.userservice.services.TokenService;
import iuh.fit.se.userservice.services.impl.UserDetailsServiceImpl;
import iuh.fit.se.userservice.utils.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAccessTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAccessTokenFilter.class);
    private final JwtDecoder jwtDecoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenService tokenService;

    public JwtAccessTokenFilter(JwtDecoder jwtDecoder, JwtTokenUtil jwtTokenUtil,
                                UserDetailsServiceImpl userDetailsService, TokenService tokenService) {
        this.jwtDecoder = jwtDecoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        logger.info("JwtAccessTokenFilter processing request: {}", requestURI);

        // Bỏ qua các endpoint công khai
        if (requestURI.equals("/sign-in") ||
                requestURI.equals("/sign-up") ||
                requestURI.equals("/forgot-password") ||
                requestURI.equals("/reset-password")) {
            logger.info("Skipping JwtAccessTokenFilter for: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("No token provided for request: {}", requestURI);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please provide a token.");
            return;
        }

        String token = authHeader.substring(7);
        Token tokenEntity = tokenService.findByToken(token);

        try {
            if (tokenEntity != null && tokenEntity.revoked) {
                logger.warn("Token is revoked for request: {}", requestURI);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "The access token you provided is revoked, malformed, or invalid.");
                return;
            }

            Jwt jwtToken = this.jwtDecoder.decode(token);
            String userName = jwtToken.getSubject();

            if (!userName.isEmpty() && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(userName);

                if (jwtTokenUtil.isTokenValid(jwtToken, userPrincipal)) {
                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    UsernamePasswordAuthenticationToken createdToken = new UsernamePasswordAuthenticationToken(
                            userPrincipal.getUsername(), userPrincipal.getPassword(), userPrincipal.getAuthorities());
                    createdToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    securityContext.setAuthentication(createdToken);
                    SecurityContextHolder.setContext(securityContext);
                }
            }

            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            logger.error("JWT validation failed for request: {}, error: {}", requestURI, ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        }
    }
}