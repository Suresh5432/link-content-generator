package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.dto.CreateUserWebsiteAccountRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdateUserWebsiteAccountRequest;
import com.linklens.userservice.digitalfootprint.entity.UserWebsiteAccount;
import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.exception.ResourceNotFoundException;
import com.linklens.userservice.digitalfootprint.repo.TrackedWebsiteRepository;
import com.linklens.userservice.digitalfootprint.repo.UserWebsiteAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserWebsiteAccountService {

    private final UserWebsiteAccountRepository repository;
    private final TrackedWebsiteRepository websiteRepository;

    @Transactional
    public UserWebsiteAccount create(String userId, CreateUserWebsiteAccountRequest request) {
        UUID websiteId = Ids.parseUuid(request.trackedWebsiteId(), "trackedWebsiteId");
        websiteRepository.findByIdAndUserId(websiteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracked website not found"));
        String source = request.discoverySource() == null ? "MANUAL" : request.discoverySource().trim().toUpperCase(Locale.ROOT);
        if (!source.equals("MANUAL") && !source.equals("CONNECTED_EMAIL")) {
            throw new InvalidRequestException("discoverySource must be MANUAL or CONNECTED_EMAIL");
        }
        boolean confirmed = request.confirmed() == null ? source.equals("MANUAL") : request.confirmed();
        UserWebsiteAccount account = UserWebsiteAccount.builder()
                .trackedWebsiteId(websiteId)
                .userId(userId)
                .accountIdentifier(trimToNull(request.accountIdentifier()))
                .accountCreationDate(Ids.parseDate(request.accountCreationDate(), "accountCreationDate"))
                .accountCategory(trimToNull(request.accountCategory()))
                .sharedInformation(trimToNull(request.sharedInformation()))
                .accountStatus(trimToNull(request.accountStatus()))
                .discoverySource(source)
                .confirmed(confirmed)
                .build();
        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public List<UserWebsiteAccount> listForWebsite(String userId, UUID websiteId) {
        websiteRepository.findByIdAndUserId(websiteId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tracked website not found"));
        return repository.findAllByUserIdAndTrackedWebsiteIdOrderByCreatedAtDesc(userId, websiteId);
    }

    @Transactional(readOnly = true)
    public List<UserWebsiteAccount> listAll(String userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public UserWebsiteAccount update(String userId, UUID id, UpdateUserWebsiteAccountRequest request) {
        UserWebsiteAccount account = requireOwned(userId, id);
        if (request.accountIdentifier() != null) account.setAccountIdentifier(trimToNull(request.accountIdentifier()));
        if (request.accountCreationDate() != null) account.setAccountCreationDate(Ids.parseDate(request.accountCreationDate(), "accountCreationDate"));
        if (request.accountCategory() != null) account.setAccountCategory(trimToNull(request.accountCategory()));
        if (request.sharedInformation() != null) account.setSharedInformation(trimToNull(request.sharedInformation()));
        if (request.accountStatus() != null) account.setAccountStatus(trimToNull(request.accountStatus()));
        if (request.confirmed() != null) account.setConfirmed(request.confirmed());
        return repository.save(account);
    }

    @Transactional
    public void delete(String userId, UUID id) {
        repository.delete(requireOwned(userId, id));
    }

    @Transactional(readOnly = true)
    public UserWebsiteAccount requireOwned(String userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Website account not found"));
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
