package com.linklens.linkservice.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        Long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
