package com.guoyu.platform.textbook.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** C3 批量分配/改派请求（方案 §6.1）：单事务、全量预检、1~50 章。ID 均字符串。 */
public record TextbookAssignmentBatchReq(
    @NotEmpty @Size(max = 50) @Valid List<Item> items) {

  public record Item(
      @NotBlank String chapterId,
      @NotBlank String writerId,
      @NotBlank String reviewerId,
      /** yyyy-MM-dd，可空 = 不限 */
      String dueDate,
      @Size(max = 500) String instruction,
      /** 数据库从未有该章分配时为 null；已有（含 active=0）必须传当前版本，防改派竞态 */
      Integer assignmentVersion) {
  }
}
