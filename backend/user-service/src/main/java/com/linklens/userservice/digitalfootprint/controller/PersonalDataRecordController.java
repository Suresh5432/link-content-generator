package com.linklens.userservice.digitalfootprint.controller;

import com.linklens.userservice.digitalfootprint.dto.CreatePersonalDataRecordRequest;
import com.linklens.userservice.digitalfootprint.dto.UpdatePersonalDataRecordRequest;
import com.linklens.userservice.digitalfootprint.entity.PersonalDataRecord;
import com.linklens.userservice.digitalfootprint.service.PersonalDataRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/digital-footprint")
public class PersonalDataRecordController {

    private final PersonalDataRecordService service;

    @PostMapping("/websites/{websiteId}/data-records")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalDataRecord create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID websiteId, @Valid @RequestBody CreatePersonalDataRecordRequest request) {
        return service.create(jwt.getSubject(), websiteId, request);
    }

    @GetMapping("/websites/{websiteId}/data-records")
    public List<PersonalDataRecord> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID websiteId) {
        return service.list(jwt.getSubject(), websiteId);
    }

    @GetMapping("/data-records")
    public List<PersonalDataRecord> listAll(@AuthenticationPrincipal Jwt jwt) {
        return service.listAll(jwt.getSubject());
    }

    @PatchMapping("/data-records/{recordId}")
    public PersonalDataRecord update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID recordId, @RequestBody UpdatePersonalDataRecordRequest request) {
        return service.update(jwt.getSubject(), recordId, request);
    }

    @DeleteMapping("/data-records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID recordId) {
        service.delete(jwt.getSubject(), recordId);
    }
}
