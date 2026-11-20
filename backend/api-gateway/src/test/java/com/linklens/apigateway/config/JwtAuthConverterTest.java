package com.linklens.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;

class JwtAuthConverterTest {
 @Test void mapsScopeAndRealmRolesAndUsesSubjectAsPrincipal() {
  Jwt jwt = Jwt.withTokenValue("opaque-test")
    .header("alg","none").subject("stable-subject")
    .claim("scope","links.read notifications.read")
    .claim("realm_access", java.util.Map.of("roles",java.util.List.of("USER","ADMIN")))
    .build();
  var authentication = new JwtAuthConverter().convert(jwt);
  assertEquals("stable-subject",authentication.getName());
  assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("SCOPE_links.read")));
  assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
  assertTrue(authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
 }
}
