package com.campus.assistant.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 统一分页返回：{ list, total, page, size }
 */
@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(List<T> list, long total, long page, long size) {
        return new PageResult<>(list, total, page, size);
    }
}
