package com.guoyu.platform.trainingdepartment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionCreateReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionQueryReq;
import com.guoyu.platform.trainingdepartment.dto.TrainSolutionUpdateReq;
import com.guoyu.platform.trainingdepartment.entity.TrainSolutionEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainSolutionMapper;
import com.guoyu.platform.trainingdepartment.service.TrainSolutionService;
import com.guoyu.platform.trainingdepartment.vo.TrainSolutionVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 方案管理服务实现
 */
@Service
public class TrainSolutionServiceImpl implements TrainSolutionService {

    private final TrainSolutionMapper mapper;

    public TrainSolutionServiceImpl(TrainSolutionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainSolutionVO> page(TrainSolutionQueryReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        LambdaQueryWrapper<TrainSolutionEntity> wrapper = new LambdaQueryWrapper<TrainSolutionEntity>()
                .eq(TrainSolutionEntity::getTenantId, tenantId)
                .eq(TrainSolutionEntity::getDeleted, 0)
                .orderByDesc(TrainSolutionEntity::getCreateTime);
        if (StringUtils.hasText(req.getKeyword())) {
            wrapper.and(w -> w
                    .like(TrainSolutionEntity::getTitle, req.getKeyword())
                    .or()
                    .like(TrainSolutionEntity::getCompanyName, req.getKeyword()));
        }
        if (req.getStatus() != null) {
            wrapper.eq(TrainSolutionEntity::getStatus, req.getStatus());
        }
        if (req.getTrainInfoId() != null) {
            wrapper.eq(TrainSolutionEntity::getTrainInfoId, req.getTrainInfoId());
        }
        if (req.getCompanyId() != null) {
            wrapper.eq(TrainSolutionEntity::getCompanyId, req.getCompanyId());
        }
        Page<TrainSolutionEntity> page = mapper.selectPage(new Page<>(req.getPage(), req.getPageSize()), wrapper);
        return new PageResponse<>(page.getRecords().stream().map(this::toVO).toList(),
                page.getCurrent(), page.getSize(), page.getTotal());
    }

    @Override
    @Transactional(readOnly = true)
    public TrainSolutionVO getById(Long id) {
        return toVO(getExisting(id));
    }

    @Override
    @Transactional
    public TrainSolutionVO create(TrainSolutionCreateReq req) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        Long userId = SecurityContext.currentUser().getUserId();
        TrainSolutionEntity entity = new TrainSolutionEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setTrainInfoId(req.getTrainInfoId());
        entity.setCompanyId(req.getCompanyId());
        entity.setCompanyName(req.getCompanyName());
        entity.setTitle(req.getTitle());
        entity.setNationPolicy(req.getNationPolicy());
        entity.setIndustrySituationAnalysis(req.getIndustrySituationAnalysis());
        entity.setCompanyDevelopmentAnalysis(req.getCompanyDevelopmentAnalysis());
        entity.setResearchDataAnalysis(req.getResearchDataAnalysis());
        entity.setProjectDevelopmentIdeas(req.getProjectDevelopmentIdeas());
        entity.setMainObjectives(req.getMainObjectives());
        entity.setMainInitiatives(req.getMainInitiatives());
        entity.setDevelopmentPlan(req.getDevelopmentPlan());
        entity.setDivision(req.getDivision());
        entity.setRisk(req.getRisk());
        entity.setCommunication(req.getCommunication());
        entity.setCheckpoint(req.getCheckpoint());
        entity.setResults(req.getResults());
        entity.setTeacherIntro(req.getTeacherIntro());
        entity.setRequestAnalysis(req.getRequestAnalysis());
        entity.setTrainContent(req.getTrainContent());
        entity.setTrainMethod(req.getTrainMethod());
        entity.setCourse(req.getCourse());
        entity.setStatus(req.getStatus());
        entity.setSortNumber(req.getSortNumber());
        entity.setContent(req.getContent());
        entity.setFilePath(req.getFilePath());
        entity.setRejectReason(req.getRejectReason());
        entity.setDeleted(0);
        mapper.insert(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public TrainSolutionVO update(Long id, TrainSolutionUpdateReq req) {
        TrainSolutionEntity entity = getExisting(id);
        if (req.getTrainInfoId() != null) {
            entity.setTrainInfoId(req.getTrainInfoId());
        }
        if (req.getCompanyId() != null) {
            entity.setCompanyId(req.getCompanyId());
        }
        if (req.getCompanyName() != null) {
            entity.setCompanyName(req.getCompanyName());
        }
        if (req.getTitle() != null) {
            entity.setTitle(req.getTitle());
        }
        if (req.getNationPolicy() != null) {
            entity.setNationPolicy(req.getNationPolicy());
        }
        if (req.getIndustrySituationAnalysis() != null) {
            entity.setIndustrySituationAnalysis(req.getIndustrySituationAnalysis());
        }
        if (req.getCompanyDevelopmentAnalysis() != null) {
            entity.setCompanyDevelopmentAnalysis(req.getCompanyDevelopmentAnalysis());
        }
        if (req.getResearchDataAnalysis() != null) {
            entity.setResearchDataAnalysis(req.getResearchDataAnalysis());
        }
        if (req.getProjectDevelopmentIdeas() != null) {
            entity.setProjectDevelopmentIdeas(req.getProjectDevelopmentIdeas());
        }
        if (req.getMainObjectives() != null) {
            entity.setMainObjectives(req.getMainObjectives());
        }
        if (req.getMainInitiatives() != null) {
            entity.setMainInitiatives(req.getMainInitiatives());
        }
        if (req.getDevelopmentPlan() != null) {
            entity.setDevelopmentPlan(req.getDevelopmentPlan());
        }
        if (req.getDivision() != null) {
            entity.setDivision(req.getDivision());
        }
        if (req.getRisk() != null) {
            entity.setRisk(req.getRisk());
        }
        if (req.getCommunication() != null) {
            entity.setCommunication(req.getCommunication());
        }
        if (req.getCheckpoint() != null) {
            entity.setCheckpoint(req.getCheckpoint());
        }
        if (req.getResults() != null) {
            entity.setResults(req.getResults());
        }
        if (req.getTeacherIntro() != null) {
            entity.setTeacherIntro(req.getTeacherIntro());
        }
        if (req.getRequestAnalysis() != null) {
            entity.setRequestAnalysis(req.getRequestAnalysis());
        }
        if (req.getTrainContent() != null) {
            entity.setTrainContent(req.getTrainContent());
        }
        if (req.getTrainMethod() != null) {
            entity.setTrainMethod(req.getTrainMethod());
        }
        if (req.getCourse() != null) {
            entity.setCourse(req.getCourse());
        }
        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }
        if (req.getSortNumber() != null) {
            entity.setSortNumber(req.getSortNumber());
        }
        if (req.getContent() != null) {
            entity.setContent(req.getContent());
        }
        if (req.getFilePath() != null) {
            entity.setFilePath(req.getFilePath());
        }
        if (req.getRejectReason() != null) {
            entity.setRejectReason(req.getRejectReason());
        }
        mapper.updateById(entity);
        return toVO(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TrainSolutionEntity entity = getExisting(id);
        entity.setDeleted(1);
        mapper.updateById(entity);
    }

    private TrainSolutionEntity getExisting(Long id) {
        Long tenantId = SecurityContext.currentUser().getTenantId();
        TrainSolutionEntity entity = mapper.selectOne(new LambdaQueryWrapper<TrainSolutionEntity>()
                .eq(TrainSolutionEntity::getTenantId, tenantId)
                .eq(TrainSolutionEntity::getId, id)
                .eq(TrainSolutionEntity::getDeleted, 0));
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Record not found");
        }
        return entity;
    }

    private TrainSolutionVO toVO(TrainSolutionEntity entity) {
        return new TrainSolutionVO(
                entity.getId(),
                entity.getTrainInfoId(),
                entity.getCompanyId(),
                entity.getCompanyName(),
                entity.getTitle(),
                entity.getNationPolicy(),
                entity.getIndustrySituationAnalysis(),
                entity.getCompanyDevelopmentAnalysis(),
                entity.getResearchDataAnalysis(),
                entity.getProjectDevelopmentIdeas(),
                entity.getMainObjectives(),
                entity.getMainInitiatives(),
                entity.getDevelopmentPlan(),
                entity.getDivision(),
                entity.getRisk(),
                entity.getCommunication(),
                entity.getCheckpoint(),
                entity.getResults(),
                entity.getTeacherIntro(),
                entity.getRequestAnalysis(),
                entity.getTrainContent(),
                entity.getTrainMethod(),
                entity.getCourse(),
                entity.getStatus(),
                entity.getSortNumber(),
                entity.getTenantId(),
                entity.getCreateTime(),
                entity.getUserId(),
                entity.getContent(),
                entity.getFilePath(),
                entity.getRejectReason()
        );
    }
}
