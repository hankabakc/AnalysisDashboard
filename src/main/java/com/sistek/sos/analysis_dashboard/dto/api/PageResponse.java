package com.sistek.sos.analysis_dashboard.dto.api;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Sayfalanmış veri listesi standart REST yanıt modeli (T-008).
 * Spring Page nesnesinin doğrudan serileştirilmesini engelleyerek sürümler arası kararlılık sağlar.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    /**
     * Spring Data Page nesnesini PageResponse DTO'suna dönüştürür.
     */
    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        List<T> mappedContent = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(
                mappedContent,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
