package com.guoyu.platform.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.exam.entity.ExamOperatePaperDetailEntity;
import com.guoyu.platform.exam.entity.ExamOperatePaperEntity;
import com.guoyu.platform.exam.entity.ExamPaperDetailEntity;
import com.guoyu.platform.exam.entity.ExamPaperEntity;
import com.guoyu.platform.exam.mapper.ExamOperatePaperDetailMapper;
import com.guoyu.platform.exam.mapper.ExamOperatePaperMapper;
import com.guoyu.platform.exam.mapper.ExamPaperDetailMapper;
import com.guoyu.platform.exam.mapper.ExamPaperMapper;
import com.guoyu.platform.exam.vo.ExamOperatePaperVO;
import com.guoyu.platform.exam.vo.ExamPaperDetailVO;
import com.guoyu.platform.exam.vo.ExamPaperVO;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class ExamPaperApplicationService {

  private final ExamPaperMapper paperMapper;
  private final ExamPaperDetailMapper paperDetailMapper;
  private final ExamOperatePaperMapper operatePaperMapper;
  private final ExamOperatePaperDetailMapper operatePaperDetailMapper;

  public ExamPaperApplicationService(
      ExamPaperMapper paperMapper,
      ExamPaperDetailMapper paperDetailMapper,
      ExamOperatePaperMapper operatePaperMapper,
      ExamOperatePaperDetailMapper operatePaperDetailMapper) {
    this.paperMapper = paperMapper;
    this.paperDetailMapper = paperDetailMapper;
    this.operatePaperMapper = operatePaperMapper;
    this.operatePaperDetailMapper = operatePaperDetailMapper;
  }

  @Transactional(readOnly = true)
  public ExamPaperVO getPaperWithDetails(Integer paperId) {
    ExamPaperEntity paper = paperMapper.selectById(paperId);
    if (paper == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "试卷不存在");
    }

    ExamPaperVO vo = ExamPaperVO.from(paper);

    List<ExamPaperDetailEntity> details = paperDetailMapper.selectList(
        new LambdaQueryWrapper<ExamPaperDetailEntity>()
            .eq(ExamPaperDetailEntity::getPaperId, paperId)
            .orderByAsc(ExamPaperDetailEntity::getSortNumber)
            .orderByAsc(ExamPaperDetailEntity::getId));

    // 暂不设到 VO 中，后续可扩展
    return vo;
  }

  @Transactional(readOnly = true)
  public List<ExamPaperDetailVO> getPaperDetails(Integer paperId) {
    List<ExamPaperDetailEntity> details = paperDetailMapper.selectList(
        new LambdaQueryWrapper<ExamPaperDetailEntity>()
            .eq(ExamPaperDetailEntity::getPaperId, paperId)
            .orderByAsc(ExamPaperDetailEntity::getSortNumber)
            .orderByAsc(ExamPaperDetailEntity::getId));
    return details.stream().map(ExamPaperDetailVO::from).toList();
  }

  @Transactional(readOnly = true)
  public ExamOperatePaperVO getOperatePaperWithDetails(Integer paperId) {
    ExamOperatePaperEntity paper = operatePaperMapper.selectById(paperId);
    if (paper == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "实操试卷不存在");
    }
    return ExamOperatePaperVO.from(paper);
  }

  @Transactional(readOnly = true)
  public List<ExamOperatePaperDetailEntity> getOperatePaperDetails(Integer paperId) {
    return operatePaperDetailMapper.selectList(
        new LambdaQueryWrapper<ExamOperatePaperDetailEntity>()
            .eq(ExamOperatePaperDetailEntity::getPaperId, paperId)
            .orderByAsc(ExamOperatePaperDetailEntity::getSortNumber)
            .orderByAsc(ExamOperatePaperDetailEntity::getId));
  }

  @Transactional
  public void deletePaper(Integer paperId) {
    paperMapper.selectById(paperId);
    paperDetailMapper.delete(new LambdaQueryWrapper<ExamPaperDetailEntity>()
        .eq(ExamPaperDetailEntity::getPaperId, paperId));
    paperMapper.deleteById(paperId);
  }

  @Transactional
  public void deleteOperatePaper(Integer paperId) {
    operatePaperMapper.selectById(paperId);
    operatePaperDetailMapper.delete(new LambdaQueryWrapper<ExamOperatePaperDetailEntity>()
        .eq(ExamOperatePaperDetailEntity::getPaperId, paperId));
    operatePaperMapper.deleteById(paperId);
  }
}
