package com.linklens.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.*;

class AudienceValidatorTest {
 @Test void acceptsOnlyTokensContainingConfiguredAudience() {
  var validator = new AudienceValidator("gateway");
  var accepted = Jwt.withTokenValue("token").header("alg","none").audience(java.util.List.of("gateway","account")).build();
  var rejected = Jwt.withTokenValue("token").header("alg","none").audience(java.util.List.of("account")).build();
  assertTrue(validator.validate(accepted).hasErrors() == false);
  assertTrue(validator.validate(rejected).hasErrors());
 }
}
