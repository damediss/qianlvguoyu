package com.guoyu.platform.trainingdepartment.vo;

import java.util.List;

public record TrainTeacherBankImportResultVO(
    int total,
    int success,
    int created,
    int updated,
    int provisional,
    List<Item> failed,
    List<Item> warnings) {

  public record Item(Integer row, String name, String reason) {}
}
