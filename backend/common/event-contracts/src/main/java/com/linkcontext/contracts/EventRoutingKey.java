package com.linkcontext.contracts;

public final class EventRoutingKey {
    public static final String WEBSITE_ANALYSIS_COMPLETED="website.analysis.completed.v1";
    public static final String PRIVACY_REQUEST_STATUS_CHANGED="privacy.request.status.changed.v1";
    public static final String PRIVACY_REQUEST_REMINDER="privacy.request.reminder.v1";
    private EventRoutingKey(){}

    public static final String LINK_SUBMITTED="link.submitted.v1";

    public static final String CONTENT_EXTRACTED="context.extracted.v1";

    public static final String SUMMARY_GENERATED="summary.generated.v1";

    public static final String EXTRACTION_FAILED="extraction.failed.v1";

    public static final String SUMMARIZATION_FAILED="summarization.failed.v1";
}
