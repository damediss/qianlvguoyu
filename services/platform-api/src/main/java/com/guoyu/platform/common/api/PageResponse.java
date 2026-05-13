package com.guoyu.platform.common.api;

import java.util.List;

public record PageResponse<T>(List<T> items, long page, long pageSize, long total) {

  public static <T> PageResponse<T> empty(long page, long pageSize) {
    return new PageResponse<>(List.of(), page, pageSize, 0);
  }
}
