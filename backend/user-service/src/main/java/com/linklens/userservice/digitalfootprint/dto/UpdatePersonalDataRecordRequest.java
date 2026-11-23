package com.linklens.userservice.digitalfootprint.dto;

public record UpdatePersonalDataRecordRequest(
        String dataCategory,
        String dataType,
        String description,
        String source,
        String dateShared,
        String evidenceSource,
        Boolean confirmedByUser
) {}
