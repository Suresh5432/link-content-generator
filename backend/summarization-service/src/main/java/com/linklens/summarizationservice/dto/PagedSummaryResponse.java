package com.linklens.summarizationservice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedSummaryResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedSummaryResponse<T> from(Page<T> page) {
        return new PagedSummaryResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
