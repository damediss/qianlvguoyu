package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.dto.TrainIdCardOcrReq;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** App 证件照上传与 OCR 迁入（文件落 CDN，OCR 预留百度对接） */
@Service
public class TrainAppIdCardApplicationService {

  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  private final PlatformUserDetailMapper detailMapper;
  private final String cdnPath;
  private final String cdnUrl;

  public TrainAppIdCardApplicationService(
      TrainSignUpPeopleSelfMapper signUpPeopleMapper,
      PlatformUserDetailMapper detailMapper,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl) {
    this.signUpPeopleMapper = signUpPeopleMapper;
    this.detailMapper = detailMapper;
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
  }

  @Transactional
  public Map<String, Object> uploadHeader(Long signUpId, String idcard, MultipartFile file) {
    if (!StringUtils.hasText(idcard) || file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "参数不完整");
    }
    String normalizedId = idcard.trim();
    Path targetDir = Paths.get(cdnPath).resolve("idCard/headerOrigin");
    try {
      Files.createDirectories(targetDir);
      Path target = targetDir.resolve(normalizedId + "_header.jpg");
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
      String url = cdnUrl + "/idCard/headerOrigin/" + normalizedId + "_header.jpg";
      updateDetailHeader(normalizedId, url);
      if (signUpId != null) {
        updateSignUpPhoto(signUpId, normalizedId, url);
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("idcard", normalizedId);
      result.put("headerUrl", url);
      result.put("pic", "idCard/headerOrigin/" + normalizedId + "_header.jpg");
      return result;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL_ERROR, "证件照上传失败");
    }
  }

  @Transactional(readOnly = true)
  public Map<String, Object> ocr(TrainIdCardOcrReq req) {
    Path picPath = Paths.get(cdnPath).resolve(req.getPic());
    if (!Files.exists(picPath)) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
    }
    if (req.getSignUpId() != null) {
      TrainSignUpPeopleSelfEntity people = signUpPeopleMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, req.getSignUpId())
              .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, req.getIdcard())
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
      if (people == null) {
        throw new BusinessException(ErrorCode.NOT_FOUND, "报名记录不存在");
      }
    }
    String url = cdnUrl + "/" + req.getPic().replace("\\", "/");
    Map<String, Object> cardInfo = new LinkedHashMap<>();
    cardInfo.put("id_card_number", req.getIdcard());
    cardInfo.put("type", req.getType());
    cardInfo.put("pic", req.getPic());
    cardInfo.put("picUrl", url);
    if ("header".equals(req.getType())) {
      updateDetailHeader(req.getIdcard(), url);
      if (req.getSignUpId() != null) {
        updateSignUpPhoto(req.getSignUpId(), req.getIdcard(), url);
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("idcard", cardInfo);
    result.put("ocrReady", false);
    result.put("message", "文件已挂载 CDN；百度 OCR 密钥配置后可在此接口扩展识别");
    return result;
  }

  private void updateDetailHeader(String idcard, String url) {
    PlatformUserDetailEntity detail = detailMapper.selectOne(new LambdaQueryWrapper<PlatformUserDetailEntity>()
        .eq(PlatformUserDetailEntity::getIdcard, idcard)
        .eq(PlatformUserDetailEntity::getDeleted, 0));
    if (detail != null) {
      detail.setIdcardHeader(url);
      detailMapper.updateById(detail);
    }
  }

  private void updateSignUpPhoto(Long signUpId, String idcard, String url) {
    TrainSignUpPeopleSelfEntity people = signUpPeopleMapper.selectOne(
        new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
            .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUpId)
            .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idcard)
            .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0));
    if (people != null) {
      people.setPhoto(url);
      signUpPeopleMapper.updateById(people);
    }
  }
}
