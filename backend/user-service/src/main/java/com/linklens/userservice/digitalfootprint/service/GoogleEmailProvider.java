package com.linklens.userservice.digitalfootprint.service;

import java.time.Instant;
import java.util.List;

public interface GoogleEmailProvider {
    OAuthTokens exchangeCode(String code, String verifier);
    OAuthTokens refreshAccessToken(String refreshToken);
    String mailboxEmail(String accessToken);
    MailboxScanResult recentRegistrationMessages(String accessToken);
    boolean revoke(String refreshToken);

    record OAuthTokens(String accessToken, String refreshToken, String grantedScopes) {}
    record MailboxMessageMetadata(String senderDomain, String subjectCategory, Instant receivedAt) {}
    record MailboxScanResult(int messagesReviewed, List<MailboxMessageMetadata> matches) {}
}
