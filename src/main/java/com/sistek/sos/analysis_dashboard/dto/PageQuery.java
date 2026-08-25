package com.sistek.sos.analysis_dashboard.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Sayfalama ve sıralama parametrelerinin tek elden normalleştirilmesi ve Pageable üretimi.
 */
public record PageQuery(
        int page,
        int size,
        String sort
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public static PageQuery of(Integer page, Integer size, String sort) {
        int normalizedPage = (page == null || page < 0) ? 0 : page;

        int normalizedSize = DEFAULT_SIZE;
        if (size != null) {
            if (size < 1) {
                normalizedSize = DEFAULT_SIZE;
            } else if (size > MAX_SIZE) {
                normalizedSize = MAX_SIZE;
            } else {
                normalizedSize = size;
            }
        }

        String normalizedSort = "desc";
        if (sort != null && ("asc".equalsIgnoreCase(sort.trim()) || "desc".equalsIgnoreCase(sort.trim()))) {
            normalizedSort = sort.trim().toLowerCase();
        }

        return new PageQuery(normalizedPage, normalizedSize, normalizedSort);
    }

    public Pageable toPageable(String primaryProperty, String secondaryProperty) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, primaryProperty).and(Sort.by(secondaryProperty)));
    }
}
