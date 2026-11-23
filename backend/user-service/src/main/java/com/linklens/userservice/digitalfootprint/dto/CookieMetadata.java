package com.linklens.userservice.digitalfootprint.dto;

/**
 * Cookie metadata only. There is deliberately no value field: raw cookie values,
 * session identifiers and tokens must never leave the browser.
 */
public record CookieMetadata(
        String name,
        String domain,
        Boolean firstParty,
        String expiration,
        String purpose,
        String category,
        Boolean secure,
        Boolean httpOnly,
        String sameSite,
        Boolean trackingRelated
) {}
