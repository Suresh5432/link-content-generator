package com.linklens.apigateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        JwtAuthenticationConverter converter= new JwtAuthenticationConverter();


        converter.setJwtGrantedAuthoritiesConverter(this::extractRoles);

        converter.setPrincipalClaimName("sub");

        return converter.convert(jwt);
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {

        List<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));

        Map<String, Object> realmAccess=jwt.getClaim("realm_access");

        if (realmAccess==null){
            return authorities;
        }

        Object rolesObject=realmAccess.get("roles");

        if (rolesObject instanceof Collection<?> roles) {
            for (Object role :roles){
                if(role instanceof String roleName){
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_"+roleName)
                    );
                }
            }
        }

        log.debug("Mapped {} Keycloak realm roles to Spring authorities",
                authorities.size());

        return authorities;
    }
}
