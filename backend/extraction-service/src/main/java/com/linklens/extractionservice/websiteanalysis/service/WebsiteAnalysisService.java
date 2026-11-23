package com.linklens.extractionservice.websiteanalysis.service;

import com.linklens.extractionservice.service.UrlSafetyValidator;
import com.linklens.extractionservice.exception.UnsafeUrlException;
import com.linklens.extractionservice.websiteanalysis.WebsiteAnalysisStatus;
import com.linklens.extractionservice.websiteanalysis.dto.CreateWebsiteAnalysisRequest;
import com.linklens.extractionservice.websiteanalysis.dto.WebsiteAnalysisResponse;
import com.linklens.extractionservice.websiteanalysis.dto.WebsiteAnalysisReportResponse;
import com.linklens.extractionservice.websiteanalysis.entity.WebsiteAnalysis;
import com.linklens.extractionservice.websiteanalysis.entity.WebsiteFinding;
import com.linklens.extractionservice.websiteanalysis.entity.WebsitePage;
import com.linklens.extractionservice.websiteanalysis.repo.WebsiteAnalysisRepository;
import com.linklens.extractionservice.websiteanalysis.repo.WebsiteFindingRepository;
import com.linklens.extractionservice.websiteanalysis.repo.WebsitePageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebsiteAnalysisService {

    private final WebsiteAnalysisRepository analysisRepository;
    private final WebsitePageRepository pageRepository;
    private final WebsiteFindingRepository findingRepository;
    private final WebsiteAnalysisInspectionRunner inspectionRunner;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final UrlSafetyValidator urlSafetyValidator;

    @Transactional
    public WebsiteAnalysisResponse create(String userId, CreateWebsiteAnalysisRequest request) {
        if (request.url() == null || request.url().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url is required");
        }
        String normalized = normalizeUrl(request.url());
        if (normalized == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL");
        String domain = domainOf(normalized);
        WebsiteAnalysis analysis = WebsiteAnalysis.builder()
                .userId(userId)
                .originalUrl(request.url().trim())
                .normalizedUrl(normalized)
                .domain(domain)
                .status(WebsiteAnalysisStatus.QUEUED)
                .build();
        analysis = analysisRepository.save(analysis);
        eventPublisher.publishEvent(new com.linklens.extractionservice.websiteanalysis.event.WebsiteAnalysisCreatedEvent(analysis.getId()));
        return toResponse(analysis);
    }

    @Transactional(readOnly = true)
    public Page<WebsiteAnalysisResponse> list(String userId, int page, int size) {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (size < 1 || size > 100) throw new IllegalArgumentException("size must be between 1 and 100");
        return analysisRepository.findAllByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WebsiteAnalysisResponse get(String userId, UUID id) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        return toResponse(a);
    }

    @Transactional(readOnly = true)
    public List<WebsitePage> pages(String userId, UUID id) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        return pageRepository.findAllByAnalysisId(a.getId());
    }

    @Transactional(readOnly = true)
    public List<WebsiteFinding> findings(String userId, UUID id) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        return findingRepository.findAllByAnalysisId(a.getId());
    }

    @Transactional
    public void delete(String userId, UUID id) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        analysisRepository.delete(a);
    }

    @Transactional
    public WebsiteAnalysisResponse retry(String userId, UUID id) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        a.setStatus(WebsiteAnalysisStatus.QUEUED);
        a.setErrorMessage(null);
        analysisRepository.save(a);
        eventPublisher.publishEvent(new com.linklens.extractionservice.websiteanalysis.event.WebsiteAnalysisCreatedEvent(a.getId()));
        return toResponse(a);
    }

    private String normalizeUrl(String raw) {
        try {
            String candidate = raw.trim();
            if (!candidate.matches("(?i)^https?://.*")) {
                candidate = "https://" + candidate;
            }
            URI uri = urlSafetyValidator.validate(candidate);
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            return uri.getScheme().toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT) + port + path + query;
        } catch (UnsafeUrlException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String domainOf(String normalized) {
        try { return new java.net.URI(normalized).getHost(); } catch (Exception e) { return null; }
    }

    @Transactional(readOnly = true)
    public WebsiteAnalysisReportResponse report(String userId, UUID id) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        return new WebsiteAnalysisReportResponse(a.getId(), toResponse(a), pageRepository.findAllByAnalysisId(a.getId()), findingRepository.findAllByAnalysisId(a.getId()));
    }

    @Transactional
    public void updateReportInternal(UUID id, String report) {
        WebsiteAnalysis a = analysisRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
        a.setReport(report);
        analysisRepository.save(a);
    }

    @Transactional
    public WebsiteAnalysisResponse updateReport(String userId, UUID id, String report) {
        WebsiteAnalysis a = analysisRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new IllegalArgumentException("Not found"));
        a.setReport(report);
        analysisRepository.save(a);
        return toResponse(a);
    }

    private WebsiteAnalysisResponse toResponse(WebsiteAnalysis a) {
        return new WebsiteAnalysisResponse(a.getId(), a.getOriginalUrl(), a.getDomain(), a.getTitle(), a.getStatus(),
                a.getRiskLevel(), a.getConfidence(), a.getCategory(), a.getReport(), a.getErrorMessage(),
                a.getCreatedAt(), a.getUpdatedAt(), a.getCompletedAt());
    }
}
