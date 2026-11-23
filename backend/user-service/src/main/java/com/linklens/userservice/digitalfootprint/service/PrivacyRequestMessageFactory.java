package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.domain.PrivacyRequestType;

/**
 * Deterministic, user-reviewable draft messages. The user must approve the exact
 * message before submission; AI may suggest an alternative but never auto-sends.
 */
final class PrivacyRequestMessageFactory {
    private PrivacyRequestMessageFactory() {}

    static String build(PrivacyRequestType type, String dataCategories) {
        String categories = dataCategories == null || dataCategories.isBlank()
                ? "the personal information associated with my account"
                : "the following categories of personal information: " + dataCategories;
        return switch (type) {
            case DELETION -> """
                    Subject: Request for Deletion of Personal Data

                    Hello Privacy Team,

                    I would like to request the deletion of my personal information associated with my account on your platform, specifically %s.

                    Please inform me of the verification steps required to process this request.

                    Please also confirm whether any personal information is retained for legal, regulatory or operational reasons.

                    Kindly provide confirmation once the request has been processed.

                    Thank you.
                    """.formatted(categories);
            case ACCOUNT_DELETION -> """
                    Subject: Request for Account Deletion

                    Hello,

                    I would like to request the deletion of my account and the personal information associated with it, specifically %s.

                    Please confirm the verification steps required and let me know whether any data will be retained for legal or operational reasons.

                    Thank you.
                    """.formatted(categories);
            case ACCESS -> """
                    Subject: Request for Access to My Personal Data

                    Hello Privacy Team,

                    I would like to request access to the personal information you hold about me, specifically %s.

                    Please let me know the verification steps required and provide a copy of the data in a commonly used format.

                    Thank you.
                    """.formatted(categories);
            case CORRECTION -> """
                    Subject: Request for Correction of Personal Data

                    Hello Privacy Team,

                    I would like to request the correction of inaccurate personal information you hold about me, specifically %s.

                    Please let me know the verification steps required and confirm once the correction has been made.

                    Thank you.
                    """.formatted(categories);
            case MARKETING_OPT_OUT -> """
                    Subject: Request to Opt Out of Marketing Communications

                    Hello,

                    I would like to opt out of marketing communications and request that my contact details no longer be used for marketing purposes.

                    Please confirm once this has been processed.

                    Thank you.
                    """;
            case TRACKING_OPT_OUT -> """
                    Subject: Request to Opt Out of Tracking

                    Hello,

                    I would like to opt out of tracking and any non-essential analytics or advertising technologies associated with my use of your service.

                    Please confirm the steps you will take and any settings I should change.

                    Thank you.
                    """;
            case COOKIE_REMOVAL -> """
                    Subject: Request for Removal of Tracking Cookies

                    Hello,

                    I would like to request the removal of non-essential tracking cookies associated with my browser and the deletion of any data derived from them.

                    Please confirm once this has been processed.

                    Thank you.
                    """;
            case CONSENT_WITHDRAWAL -> """
                    Subject: Withdrawal of Consent

                    Hello Privacy Team,

                    I would like to withdraw my consent to the processing of my personal information, specifically %s.

                    Please confirm the processing that will stop and any processing that will continue on another legal basis.

                    Thank you.
                    """.formatted(categories);
            case PORTABILITY -> """
                    Subject: Request for Data Portability

                    Hello Privacy Team,

                    I would like to request a copy of my personal information in a structured, commonly used and machine-readable format, specifically %s.

                    Please let me know the verification steps required.

                    Thank you.
                    """.formatted(categories);
        };
    }
}
