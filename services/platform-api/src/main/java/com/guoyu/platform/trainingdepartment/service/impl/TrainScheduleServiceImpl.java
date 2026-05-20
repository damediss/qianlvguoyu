package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainScheduleEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainScheduleMapper;
import com.guoyu.platform.trainingdepartment.service.TrainScheduleService;
import com.guoyu.platform.trainingdepartment.vo.TrainScheduleConflictVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TrainScheduleServiceImpl implements TrainScheduleService {

    private final TrainScheduleMapper mapper;

    public TrainScheduleServiceImpl(TrainScheduleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainScheduleConflictVO> checkConflicts(Long classId) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        // Query all schedules for the same tenant (cross-class conflict detection)
        LambdaQueryWrapper<TrainScheduleEntity> wrapper = new LambdaQueryWrapper<TrainScheduleEntity>()
                .eq(TrainScheduleEntity::getTenantId, tenantId)
                .eq(TrainScheduleEntity::getDeleted, 0);
        List<TrainScheduleEntity> allSchedules = mapper.selectList(wrapper);

        List<TrainScheduleConflictVO> conflicts = new ArrayList<>();

        for (int i = 0; i < allSchedules.size(); i++) {
            for (int j = i + 1; j < allSchedules.size(); j++) {
                TrainScheduleEntity s1 = allSchedules.get(i);
                TrainScheduleEntity s2 = allSchedules.get(j);

                // At least one schedule must belong to the queried class
                if (!Objects.equals(s1.getClassId(), classId) && !Objects.equals(s2.getClassId(), classId)) {
                    continue;
                }

                // Must be on the same day
                if (!isSameDay(s1, s2)) {
                    continue;
                }

                // Check teacher time overlap
                if (s1.getTeacherId() != null && s2.getTeacherId() != null
                        && Objects.equals(s1.getTeacherId(), s2.getTeacherId())
                        && hasTimeOverlap(s1, s2)) {
                    conflicts.add(new TrainScheduleConflictVO(
                            s1.getId(), s2.getId(),
                            "TEACHER_CONFLICT",
                            "教师 " + s1.getTeacherName() + " 在 " + s1.getScheduleDate()
                                    + " " + s1.getStartTime() + "-" + s1.getEndTime()
                                    + " 与 " + s2.getStartTime() + "-" + s2.getEndTime() + " 存在时间冲突"
                    ));
                }

                // Check room time overlap
                if (s1.getRoomName() != null && s2.getRoomName() != null
                        && Objects.equals(s1.getRoomName(), s2.getRoomName())
                        && hasTimeOverlap(s1, s2)) {
                    conflicts.add(new TrainScheduleConflictVO(
                            s1.getId(), s2.getId(),
                            "ROOM_CONFLICT",
                            "教室 " + s1.getRoomName() + " 在 " + s1.getScheduleDate()
                                    + " " + s1.getStartTime() + "-" + s1.getEndTime()
                                    + " 与 " + s2.getStartTime() + "-" + s2.getEndTime() + " 存在时间冲突"
                    ));
                }
            }
        }
        return conflicts;
    }

    private boolean isSameDay(TrainScheduleEntity s1, TrainScheduleEntity s2) {
        if (s1.getScheduleDate() != null && s2.getScheduleDate() != null) {
            return Objects.equals(s1.getScheduleDate(), s2.getScheduleDate());
        }
        // Fall back to dayOfWeek comparison if scheduleDate is not set
        return s1.getDayOfWeek() != null && Objects.equals(s1.getDayOfWeek(), s2.getDayOfWeek());
    }

    private boolean hasTimeOverlap(TrainScheduleEntity s1, TrainScheduleEntity s2) {
        if (s1.getStartTime() == null || s1.getEndTime() == null
                || s2.getStartTime() == null || s2.getEndTime() == null) {
            return false;
        }
        // Overlap exists when one starts before the other ends and vice versa
        return s1.getStartTime().compareTo(s2.getEndTime()) < 0
                && s2.getStartTime().compareTo(s1.getEndTime()) < 0;
    }
}