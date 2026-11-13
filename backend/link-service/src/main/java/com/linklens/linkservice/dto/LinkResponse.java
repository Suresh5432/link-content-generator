package com.linklens.linkservice.dto;

import com.linklens.linkservice.enums.LinkStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkResponse {
    private String linkId;
    private LinkStatus status;
    private String message;
    private Instant createdAt;
}
