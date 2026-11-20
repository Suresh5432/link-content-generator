package com.linklens.linkservice.entity;

import com.linklens.linkservice.enums.LinkStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkStatusTransitionTest {
    @Test void failedLinkCanRetryButCompletedLinkIsTerminal() {
        Link link = Link.builder().status(LinkStatus.EXTRACTION_FAILED).build();
        link.updateStatus(LinkStatus.PENDING_EXTRACTION);
        assertEquals(LinkStatus.PENDING_EXTRACTION, link.getStatus());
        link.updateStatus(LinkStatus.COMPLETED);
        assertThrows(IllegalStateException.class, () -> link.updateStatus(LinkStatus.PENDING_EXTRACTION));
    }
    @Test void invalidFailureTransitionIsRejected() {
        Link link = Link.builder().status(LinkStatus.COMPLETED).build();
        assertThrows(IllegalStateException.class, () -> link.markFailed(LinkStatus.EXTRACTION_FAILED, "FETCH_ERROR"));
    }
}
