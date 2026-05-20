package com.easyfinance.shared.application;

public record PageQuery(int page, int size) {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    public static PageQuery of(Integer page, Integer size) {
        int resolvedPage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int resolvedSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return new PageQuery(resolvedPage, resolvedSize);
    }
}

