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

    /** Geçersiz değerler hata vermez: sayfa 0'a, boyut 50'ye (en fazla 200), yön "desc"e döner. */
    public static PageQuery of(Integer page, Integer size, String sort) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        String dir = (sort != null && sort.trim().equalsIgnoreCase("asc")) ? "asc" : "desc";
        return new PageQuery(p, s, dir);
    }

    public Pageable toPageable(String primaryProperty, String secondaryProperty) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, primaryProperty).and(Sort.by(secondaryProperty)));
    }
}
