package com.linklens.extractionservice.messaging;

import com.linkcontext.contracts.events.UserFootprintDeletedEvent;
import com.linklens.extractionservice.repo.ExtractionContentRepository;
import com.linklens.extractionservice.websiteanalysis.repo.WebsiteAnalysisRepository;
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

    @Mock ExtractionContentRepository contentRepository;
    @Mock WebsiteAnalysisRepository websiteAnalysisRepository;
    @InjectMocks UserFootprintDeletedConsumer consumer;

    @Test
    void deletesOnlyUserOwnedExtractionRecords() {
        var event = new UserFootprintDeletedEvent(UUID.randomUUID(), "user-1", Instant.now(), "FOOTPRINT_ONLY", 1);
        consumer.consume(event);
        verify(contentRepository).deleteByUserId("user-1");
        verify(websiteAnalysisRepository).deleteByUserId("user-1");
    }

    @Test
    void ignoresUnsupportedVersion() {
        var event = new UserFootprintDeletedEvent(UUID.randomUUID(), "user-1", Instant.now(), "FOOTPRINT_ONLY", 2);
        consumer.consume(event);
        verifyNoInteractions(contentRepository, websiteAnalysisRepository);
    }
}
