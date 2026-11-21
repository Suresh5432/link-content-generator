package com.linklens.summarizationservice.service;

import com.linklens.summarizationservice.dto.SummaryResponse;
import com.linklens.summarizationservice.dto.PagedSummaryResponse;
import com.linklens.summarizationservice.entity.Summary;
import com.linklens.summarizationservice.repo.SummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SummaryQueryService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "title", "readingTimeMinutes");

    private final SummaryRepository summaryRepository;

    public PagedSummaryResponse<SummaryResponse> list(String userId, int page, int size, String sortBy, Sort.Direction direction) {
        if (page < 0 || size < 1 || size > 100 || !SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination or sort parameters");
        }
        Page<SummaryResponse> responsePage = summaryRepository.findAllByUserId(userId,
                        PageRequest.of(page, size, Sort.by(direction, sortBy)))
                .map(SummaryResponse::from);
        return PagedSummaryResponse.from(responsePage);
    }

    public SummaryResponse get(String userId, UUID summaryId) {
        return summaryRepository.findByIdAndUserId(summaryId, userId)
                .map(SummaryResponse::from)
                .orElseThrow(() -> notFound());
    }

    public SummaryResponse getByLink(String userId, UUID linkId) {
        return summaryRepository.findByLinkIdAndUserId(linkId, userId)
                .map(SummaryResponse::from)
                .orElseThrow(() -> notFound());
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Summary not found");
    }
}
