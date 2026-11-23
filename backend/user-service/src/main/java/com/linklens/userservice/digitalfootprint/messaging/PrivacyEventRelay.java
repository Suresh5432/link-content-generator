package com.linklens.userservice.digitalfootprint.messaging;

import com.linklens.userservice.digitalfootprint.event.PrivacyReminderDueEvent;
import com.linklens.userservice.digitalfootprint.event.PrivacyRequestChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PrivacyEventRelay {

    private final PrivacyEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChanged(PrivacyRequestChangedEvent event) {
        publisher.statusChanged(event.requestId(), event.userId(), event.requestType(), event.previousStatus(), event.newStatus());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReminder(PrivacyReminderDueEvent event) {
        publisher.reminder(event.requestId(), event.userId(), event.requestType(), event.status());
    }
}
