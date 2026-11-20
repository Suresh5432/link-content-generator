package com.linklens.notificationservice.dto;
import jakarta.validation.constraints.NotNull;
public record NotificationPreferencesRequest(@NotNull Boolean inAppEnabled, @NotNull Boolean emailEnabled) {}
