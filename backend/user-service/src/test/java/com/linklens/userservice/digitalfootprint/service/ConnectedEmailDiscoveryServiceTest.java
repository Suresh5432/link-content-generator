package com.linklens.userservice.digitalfootprint.service;

import com.linklens.userservice.digitalfootprint.exception.InvalidRequestException;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailAccountRepository;
import com.linklens.userservice.digitalfootprint.repo.ConnectedEmailDiscoveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectedEmailDiscoveryServiceTest {

    @Mock ConnectedEmailAccountRepository accountRepository;
    @Mock ConnectedEmailDiscoveryRepository discoveryRepository;
    ConnectedEmailDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new ConnectedEmailDiscoveryService(accountRepository, discoveryRepository);
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    @Test
    void rejectsWhenFeatureDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThrows(InvalidRequestException.class, () -> service.register("u1", "GOOGLE", "a@example.com", "openid"));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void rejectsWriteScopes() {
        assertThrows(InvalidRequestException.class,
                () -> service.register("u1", "GOOGLE", "a@example.com", "https://www.googleapis.com/auth/gmail.modify"));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void rejectsNonAllowlistedScopes() {
        assertThrows(InvalidRequestException.class,
                () -> service.register("u1", "GOOGLE", "a@example.com", "https://mail.google.com/"));
    }

    @Test
    void disconnectDeletesImportedRecordsAndAccount() {
        var id = java.util.UUID.randomUUID();
        var account = com.linklens.userservice.digitalfootprint.entity.ConnectedEmailAccount.builder()
                .id(id).userId("u1").provider("GOOGLE").status("CONNECTED").build();
        when(accountRepository.findByIdAndUserId(id, "u1")).thenReturn(java.util.Optional.of(account));

        service.disconnect("u1", id);

        verify(discoveryRepository).deleteAllByConnectedAccountIdAndUserId(id, "u1");
        verify(accountRepository).delete(account);
    }
}
