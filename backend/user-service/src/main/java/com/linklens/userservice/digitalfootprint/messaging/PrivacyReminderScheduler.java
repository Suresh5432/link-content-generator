package com.linklens.userservice.digitalfootprint.messaging;

import com.linklens.userservice.digitalfootprint.domain.PrivacyRequestStatus;
import com.linklens.userservice.digitalfootprint.event.PrivacyReminderDueEvent;
import com.linklens.userservice.digitalfootprint.repo.PrivacyRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrivacyReminderScheduler {

    private static final List<String> PENDING = List.of(
            PrivacyRequestStatus.SUBMITTED.name(),
            PrivacyRequestStatus.ACKNOWLEDGED.name(),
            PrivacyRequestStatus.IN_PROGRESS.name(),
            PrivacyRequestStatus.NEEDS_VERIFICATION.name());

    private final PrivacyRequestRepository repository;
    private final ApplicationEventPublisher events;

    @Scheduled(fixedDelayString = "${digital-footprint.reminders.fixed-delay-ms:3600000}", initialDelayString = "${digital-footprint.reminders.initial-delay-ms:60000}")
    @Transactional
    public void sendDueReminders() {
        Instant now = Instant.now();
        var due = repository.findByStatusInAndReminderAtBefore(PENDING, now);
        if (due.isEmpty()) return;
        log.info("Publishing {} privacy request reminders", due.size());
        for (var request : due) {
            request.setLastReminderAt(now);
            request.setReminderAt(now.plus(14, ChronoUnit.DAYS));
            events.publishEvent(new PrivacyReminderDueEvent(request.getId(), request.getUserId(), request.getRequestType(), request.getStatus()));
        }
    }
}
