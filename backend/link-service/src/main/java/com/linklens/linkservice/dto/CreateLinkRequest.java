package com.linklens.linkservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateLinkRequest {

    @NotBlank(message = "URL cannot be empty")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;
}
