package com.linklens.linkservice.dto;

import com.linklens.linkservice.enums.LinkStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkResponse {
    private UUID id;
    private String url;
    private LinkStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
