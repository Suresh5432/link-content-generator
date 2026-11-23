package com.linklens.summarizationservice.messaging;

import com.linkcontext.contracts.events.UserFootprintDeletedEvent;
import com.linklens.summarizationservice.repo.SummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserFootprintDeletedConsumerTest {

    @Mock SummaryRepository summaryRepository;
    @InjectMocks UserFootprintDeletedConsumer consumer;

    @Test
    void deletesOnlyUserOwnedSummaries() {
        consumer.consume(new UserFootprintDeletedEvent(UUID.randomUUID(), "user-1", Instant.now(), "FOOTPRINT_ONLY", 1));
        verify(summaryRepository).deleteByUserId("user-1");
    }

    @Test
    void ignoresInvalidEvent() {
        consumer.consume(new UserFootprintDeletedEvent(UUID.randomUUID(), " ", Instant.now(), "FOOTPRINT_ONLY", 1));
        verifyNoInteractions(summaryRepository);
    }
}
