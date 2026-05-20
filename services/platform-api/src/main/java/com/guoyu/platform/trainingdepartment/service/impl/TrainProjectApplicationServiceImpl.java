package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.entity.TrainProjectEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainProjectTableEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainProjectMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainProjectTableMapper;
import com.guoyu.platform.trainingdepartment.service.TrainProjectApplicationService;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectTableVO;
import com.guoyu.platform.trainingdepartment.vo.TrainProjectVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TrainProjectApplicationServiceImpl implements TrainProjectApplicationService {

    private static final String STAGE_DRAFT = "DRAFT";
    private static final String STAGE_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STAGE_APPROVED = "APPROVED";
    private static final String STAGE_PREPARING = "PREPARING";
    private static final String STAGE_EXECUTING = "EXECUTING";
    private static final String STAGE_CERTIFYING = "CERTIFYING";
    private static final String STAGE_CLOSING = "CLOSING";
    private static final String STAGE_ARCHIVED = "ARCHIVED";

    /** Legal forward transitions: key = current stage, value = allowed next stages */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            STAGE_APPROVED, Set.of(STAGE_PREPARING),
            STAGE_PREPARING, Set.of(STAGE_EXECUTING),
            STAGE_EXECUTING, Set.of(STAGE_CERTIFYING),
            STAGE_CERTIFYING, Set.of(STAGE_CLOSING),
            STAGE_CLOSING, Set.of(STAGE_ARCHIVED)
    );

    private static final String[] TABLE_NAMES = {
            "项目总体计划表",
            "培训人员台账表",
            "课程与课件管理表",
            "师资与排课管理表",
            "学员材料审核表",
            "考勤与过程记录表",
            "考核与认定申报表",
            "获证学员花名册表",
            "补贴材料与拨款跟踪表",
            "项目结算与总结表"
    };

    private final TrainProjectMapper projectMapper;
    private final TrainProjectTableMapper tableMapper;

    public TrainProjectApplicationServiceImpl(TrainProjectMapper projectMapper,
                                              TrainProjectTableMapper tableMapper) {
        this.projectMapper = projectMapper;
        this.tableMapper = tableMapper;
    }

    @Override
    @Transactional
    public TrainProjectVO launch(Long id) {
        TrainProjectEntity entity = getExisting(id);
        String stage = entity.getCurrentStage();
        if (stage != null && !stage.isEmpty() && !STAGE_DRAFT.equals(stage)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有草稿状态的项目才能发起审批");
        }
        entity.setCurrentStage(STAGE_PENDING_APPROVAL);
        entity.setStatus(1);
        projectMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainProjectVO approve(Long id) {
        TrainProjectEntity entity = getExisting(id);
        if (!STAGE_PENDING_APPROVAL.equals(entity.getCurrentStage())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待审批状态的项目才能审批通过");
        }
        entity.setCurrentStage(STAGE_APPROVED);
        entity.setStatus(2);
        projectMapper.updateById(entity);
        generateManagementTables(entity.getId(), entity.getTenantId());
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainProjectVO reject(Long id, String reason) {
        TrainProjectEntity entity = getExisting(id);
        if (!STAGE_PENDING_APPROVAL.equals(entity.getCurrentStage())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有待审批状态的项目才能驳回");
        }
        entity.setCurrentStage(STAGE_DRAFT);
        entity.setStatus(0);
        String desc = entity.getDescription();
        String appendText = "[驳回原因] " + reason;
        entity.setDescription(desc == null || desc.isEmpty() ? appendText : desc + "\n" + appendText);
        projectMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainProjectVO transit(Long id, String targetStage) {
        TrainProjectEntity entity = getExisting(id);
        String current = entity.getCurrentStage();
        Set<String> allowed = TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(targetStage)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "不允许从 " + current + " 转换到 " + targetStage);
        }
        entity.setCurrentStage(targetStage);
        projectMapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrainProjectTableVO> getProjectTables(Long projectId) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainProjectTableEntity> wrapper = new LambdaQueryWrapper<TrainProjectTableEntity>()
                .eq(TrainProjectTableEntity::getProjectId, projectId)
                .eq(TrainProjectTableEntity::getTenantId, tenantId)
                .eq(TrainProjectTableEntity::getDeleted, 0)
                .orderByAsc(TrainProjectTableEntity::getTableType);
        List<TrainProjectTableEntity> entities = tableMapper.selectList(wrapper);
        return entities.stream().map(this::toTableVO).toList();
    }

    private void generateManagementTables(Long projectId, Long tenantId) {
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < TABLE_NAMES.length; i++) {
            TrainProjectTableEntity table = new TrainProjectTableEntity();
            table.setProjectId(projectId);
            table.setTableType(i + 1);
            table.setTableName(TABLE_NAMES[i]);
            table.setStatus(0);
            table.setSortNumber(i + 1);
            table.setTenantId(tenantId);
            table.setCreateTime(now);
            table.setUpdateTime(now);
            table.setDeleted(0);
            tableMapper.insert(table);
        }
    }

    private TrainProjectEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainProjectEntity entity = projectMapper.selectOne(new LambdaQueryWrapper<TrainProjectEntity>()
                .eq(TrainProjectEntity::getTenantId, tenantId)
                .eq(TrainProjectEntity::getId, id)
                .eq(TrainProjectEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return entity;
    }

    private TrainProjectVO toVO(TrainProjectEntity entity) {
        return new TrainProjectVO(
                entity.getId(),
                entity.getProjectName(),
                entity.getCompanyId(),
                entity.getCompanyName(),
                entity.getManagerId(),
                entity.getManagerName(),
                entity.getPlanStart(),
                entity.getPlanEnd(),
                entity.getCurrentStage(),
                entity.getProgress(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private TrainProjectTableVO toTableVO(TrainProjectTableEntity entity) {
        return new TrainProjectTableVO(
                entity.getId(),
                entity.getProjectId(),
                entity.getTableType(),
                entity.getTableName(),
                entity.getContent(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getFileId(),
                entity.getOwnerId(),
                entity.getOwnerName(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }
}
