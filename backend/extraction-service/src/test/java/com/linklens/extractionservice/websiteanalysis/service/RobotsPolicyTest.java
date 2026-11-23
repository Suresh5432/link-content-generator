package com.linklens.extractionservice.websiteanalysis.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RobotsPolicyTest {

    @Test
    void appliesMostSpecificUserAgentGroupAndAllowOverride() {
        RobotsPolicy policy = RobotsPolicy.parse("""
                User-agent: *
                Disallow: /

                User-agent: LinkContextBot
                Disallow: /private
                Allow: /private/public$
                """, "LinkContextBot/1.0");

        assertTrue(policy.allows("https://example.org/private/public"));
        assertFalse(policy.allows("https://example.org/private/account"));
        assertTrue(policy.allows("https://example.org/"));
    }

    @Test
    void appliesWildcardAndQueryRules() {
        RobotsPolicy policy = RobotsPolicy.parse("""
                User-agent: *
                Disallow: /search*
                Allow: /search/public$
                """, "LinkContextBot/1.0");

        assertFalse(policy.allows("https://example.org/search?q=term"));
        assertTrue(policy.allows("https://example.org/search/public"));
    }

    @Test
    void denyAllPolicyBlocksPagesWhenRobotsPolicyCannotBeVerified() {
        assertFalse(RobotsPolicy.denyAll().allows("https://example.org/"));
        assertTrue(RobotsPolicy.allowAll().allows("https://example.org/"));
    }
}
