package com.linklens.apigateway.filter;

import com.linklens.apigateway.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected  void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException{

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId;

        if (authentication instanceof JwtAuthenticationToken jwtAuth){
            userId = jwtAuth.getToken().getSubject();
        }
        else{
            userId = authentication.getName();
        }

        boolean aiPath = request.getRequestURI().startsWith("/api/v1/privacy-assistant")
                || request.getRequestURI().startsWith("/api/v1/privacy-request-drafts");

        try{
            boolean allowed = aiPath
                    ? rateLimitService.isAllowed(userId, "ai", 10)
                    : rateLimitService.isAllowed(userId);

            if(!allowed){

                log.warn("Rate limit exceeded | userId={}", userId);

                response.setStatus(429);

                response.setContentType("application/json");

                response.setHeader("Retry-After", "60");

                response.getWriter()
                        .write("""
                                {
                                  "status": 429,
                                  "error": "Too Many Requests",
                                  "message": "Rate limit exceeded. Please try again later."
                                }
                                """);

                return;
            }

            filterChain.doFilter(request,response);

        }
        catch (DataAccessException | IllegalStateException exception){

            log.error(
                    "Redis unavailable while checking rate limit | userId={}",
                    userId,
                    exception
            );

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            response.setContentType("application/json");

            response.getWriter().write("""
                       {
                          "status": 503,
                          "error": "Service Unavailable",
                          "message": "Rate limiting service is temporarily unavailable."
                       } 
                        """);
        }

    }

}
