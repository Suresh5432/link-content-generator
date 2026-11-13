package com.linkcontext.contracts;

public final class EventRoutingKey {
    private EventRoutingKey(){}

    public static final String LINK_SUBMITTED="link.submitted.v1";

    public static final String CONTENT_EXTRACTED="context.extracted.v1";

    public static final String SUMMARY_GENERATED="summary.generated.v1";

    public static final String EXTRACTION_FAILED="extraction.failed.v1";

    public static final String SUMMARIZATION_FAILED="summarization.failed.v1";
}
