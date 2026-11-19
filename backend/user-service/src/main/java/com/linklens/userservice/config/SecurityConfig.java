package com.linklens.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
         return http
                 .csrf(AbstractHttpConfigurer::disable)
                 .sessionManagement(session->
                         session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                 )
                 .authorizeHttpRequests(auth->
                         auth.requestMatchers(
                                 "/actuator/health",
                                 "/actuator/health/**").permitAll()
                                 .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                 .requestMatchers("/internal/v1/**").hasAnyAuthority("SCOPE_users.read")
                                 .requestMatchers("/api/v1/**").hasAnyAuthority("ROLE_USER")
                                 .anyRequest().authenticated()
                 )
                 .oauth2ResourceServer(oauth2->
                         oauth2.jwt(jwt->
                                 jwt.jwtAuthenticationConverter(new JwtAuthConverter()))
                 )
                 .build();


    }
}
