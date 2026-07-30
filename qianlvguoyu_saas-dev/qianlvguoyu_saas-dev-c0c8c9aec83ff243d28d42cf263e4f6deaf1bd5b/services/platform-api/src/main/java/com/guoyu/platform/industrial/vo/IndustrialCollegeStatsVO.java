package com.guoyu.platform.industrial.vo;

import java.util.List;

public record IndustrialCollegeStatsVO(
    long total,
    long recentCreated,
    List<TagCount> byTag
) {

  public record TagCount(String tag, long count) {
  }
}
