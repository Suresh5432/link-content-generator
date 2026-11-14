package com.linklens.linkservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@Configuration
public class LinkSecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth->
                        auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                                .anyRequest().authenticated()
                        )
                .oauth2ResourceServer(oauth->
                        oauth
                                .jwt(jwt->
                                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt->{
            Map<String,Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) {
                return new JwtAuthenticationToken(jwt);
            }

            Object rolesObject=realmAccess.get("roles");

            if (!(rolesObject instanceof Collection<?> roles)) {
                return new JwtAuthenticationToken(jwt);
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(role->new SimpleGrantedAuthority("ROLE_"+role))
                    .toList();

            String subject = jwt.getSubject();

            assert subject != null;

            return new JwtAuthenticationToken(jwt,authorities, subject);
        };
    }

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

}
