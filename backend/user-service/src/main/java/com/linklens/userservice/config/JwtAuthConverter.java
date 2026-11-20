package com.linklens.userservice.config;

import org.hibernate.sql.ast.tree.expression.Collation;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopesConverter =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        Collection<GrantedAuthority> authorities = new HashSet<>(scopesConverter.convert(jwt));

        Map<String,Object> realmAccess=
                jwt.getClaim("realm_access");

        if (realmAccess!=null) {

            Object rolesObject=realmAccess.get("roles");

            if (rolesObject instanceof Collection<?> roles) {
                roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role->new SimpleGrantedAuthority("ROLE_"+role))
                        .forEach(authorities::add);
            }
        }
        return new JwtAuthenticationToken(jwt,authorities);
    }
}
