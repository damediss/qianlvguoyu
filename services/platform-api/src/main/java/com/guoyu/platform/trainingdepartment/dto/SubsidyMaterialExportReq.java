package com.guoyu.platform.trainingdepartment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SubsidyMaterialExportReq(@NotEmpty List<Long> instanceIds) {}
