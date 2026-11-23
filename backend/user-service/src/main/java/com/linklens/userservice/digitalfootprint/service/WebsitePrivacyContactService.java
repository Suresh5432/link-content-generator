package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.UpdatePrivacyContactRequest;
import com.linklens.userservice.digitalfootprint.entity.WebsitePrivacyContact;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import com.linklens.userservice.digitalfootprint.repo.WebsitePrivacyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WebsitePrivacyContactService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final WebsitePrivacyContactRepository repository;
    private final TrackedWebsiteRepository websiteRepository;

    @Transactional
    public WebsitePrivacyContact upsert(String userId, UUID websiteId, UpdatePrivacyContactRequest request) {
        websiteRepository.findByIdAndUserId(websiteId, userId)
                .orElseThrow(() -> new com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException("Tracked website not found"));
        if (request.privacyContactEmail() != null && !request.privacyContactEmail().isBlank()
                && !EMAIL.matcher(request.privacyContactEmail().trim()).matches()) {
            throw new InvalidRequestException("privacyContactEmail is not a valid email address");
        }
        WebsitePrivacyContact contact = repository.findByUserIdAndTrackedWebsiteId(userId, websiteId)
                .orElseGet(() -> WebsitePrivacyContact.builder().userId(userId).trackedWebsiteId(websiteId).build());
        if (request.privacyPolicyUrl() != null) contact.setPrivacyPolicyUrl(trimToNull(request.privacyPolicyUrl()));
        if (request.deletionPolicyUrl() != null) contact.setDeletionPolicyUrl(trimToNull(request.deletionPolicyUrl()));
        if (request.accessRequestUrl() != null) contact.setAccessRequestUrl(trimToNull(request.accessRequestUrl()));
        if (request.accountDeletionUrl() != null) contact.setAccountDeletionUrl(trimToNull(request.accountDeletionUrl()));
        if (request.cookieManagementUrl() != null) contact.setCookieManagementUrl(trimToNull(request.cookieManagementUrl()));
        if (request.consentManagementUrl() != null) contact.setConsentManagementUrl(trimToNull(request.consentManagementUrl()));
        if (request.privacyContactEmail() != null) contact.setPrivacyContactEmail(trimToNull(request.privacyContactEmail()));
        if (request.dpoContact() != null) contact.setDpoContact(trimToNull(request.dpoContact()));
        if (request.sourceUrls() != null) contact.setSourceUrls(trimToNull(request.sourceUrls()));
        return repository.save(contact);
    }

    @Transactional(readOnly = true)
    public WebsitePrivacyContact get(String userId, UUID websiteId) {
        return repository.findByUserIdAndTrackedWebsiteId(userId, websiteId)
                .orElseThrow(() -> new com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException("No privacy contact recorded for this website"));
    }

    @Transactional(readOnly = true)
    public List<WebsitePrivacyContact> list(String userId) {
        return repository.findAllByUserIdOrderByUpdatedAtDesc(userId);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
