package com.guoyu.platform.exam.zhenti.vo;

import java.util.List;

/**
 * A1 App 端真题清单出参（records/total/page/size 与 R1 同口径）。
 *
 * <p>{@code list} 为过渡兼容字段：已发版 App（2026-10-18 版）仍读 data.list，读不到会误显
 * 空态；与 records 同引用，下版 App 改为读 records 后必须删除该字段（方案 R1 统一口径）。
 */
public record ZhentiAppListVO(
    List<ZhentiItemVO> records,
    long total,
    long page,
    long size,
    @Deprecated List<ZhentiItemVO> list
) {

  /** 常规构造：list 与 records 同引用（过渡别名）。 */
  public ZhentiAppListVO(List<ZhentiItemVO> records, long total, long page, long size) {
    this(records, total, page, size, records);
  }
}
