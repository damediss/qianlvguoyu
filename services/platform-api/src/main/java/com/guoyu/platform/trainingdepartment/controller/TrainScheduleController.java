package com.guoyu.platform.trainingdepartment.controller;

import com.guoyu.platform.common.api.ApiResponse;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleService;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleConflictVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 培训排课管理接口
 */
@Validated
@RestController
@RequestMapping("/training")
public class TrainScheduleController {

    private final TrainScheduleService service;

    public TrainScheduleController(TrainScheduleService service) {
        this.service = service;
    }

  /** 检查排课冲突 */
    @GetMapping("/schedules/conflicts")
    @PreAuthorize("hasAuthority('train:class:view')")
    public ApiResponse<List<TrainScheduleConflictVO>> checkConflicts(@RequestParam Long classId) {
        return ApiResponse.ok(service.checkConflicts(classId));
    }
}
