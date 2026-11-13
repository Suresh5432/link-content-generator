package com.linklens.linkservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LinkRequest {

    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL is too long")
    private String url;
}
