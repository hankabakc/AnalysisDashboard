package com.sistek.sos.analysis_dashboard.dto;

import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Sayfalama ve sıralama sorgu parametreleri: ?page=&size=&sort=
 * Controller metoduna doğrudan parametre olarak bağlanır.
 * Geçersiz değer hata vermez: sayfa 0'a, boyut 50'ye (en fazla 200), yön "desc"e döner.
 */
public record PageQuery(
        @Parameter(description = "Sayfa numarası (0 tabanlı)") Integer page,
        @Parameter(description = "Sayfa boyutu (varsayılan 50, en fazla 200)") Integer size,
        @Parameter(description = "Sıralama yönü (asc/desc)") String sort
) {
    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public PageQuery {
        page = (page == null || page < 0) ? 0 : page;
        size = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        sort = (sort != null && sort.strip().equalsIgnoreCase("asc")) ? "asc" : "desc";
    }

    public Sort.Direction direction() {
        return "asc".equals(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    /** İkinci alan eşitlik durumunda sırayı belirler; yönü birinci alanla aynıdır. */
    public Pageable toPageable(String primaryProperty, String secondaryProperty) {
        return PageRequest.of(page, size, Sort.by(direction(), primaryProperty, secondaryProperty));
    }
}
