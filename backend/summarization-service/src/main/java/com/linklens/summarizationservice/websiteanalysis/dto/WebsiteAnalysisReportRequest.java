package com.linklens.summarizationservice.websiteanalysis.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record WebsiteAnalysisReportRequest(@NotNull UUID analysisId) {}
