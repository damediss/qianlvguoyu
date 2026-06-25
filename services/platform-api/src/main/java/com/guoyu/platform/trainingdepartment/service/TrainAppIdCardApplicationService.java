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

/** App 证件照上传与 OCR 迁入（文件落 CDN，对接百度 OCR 识别） */
@Service
public class TrainAppIdCardApplicationService {

  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  private final PlatformUserDetailMapper detailMapper;
  private final BaiduOcrService baiduOcrService;
  private final String cdnPath;
  private final String cdnUrl;
  private final String fileLocalRoot;

  public TrainAppIdCardApplicationService(
      TrainSignUpPeopleSelfMapper signUpPeopleMapper,
      PlatformUserDetailMapper detailMapper,
      BaiduOcrService baiduOcrService,
      @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
      @Value("${platform.file.local-root:D:/phpApp/qianlvguoyu_saas/storage/files}") String fileLocalRoot) {
    this.signUpPeopleMapper = signUpPeopleMapper;
    this.detailMapper = detailMapper;
    this.baiduOcrService = baiduOcrService;
    this.cdnPath = cdnPath;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    this.fileLocalRoot = fileLocalRoot;
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

  @Transactional
  public Map<String, Object> ocr(TrainIdCardOcrReq req) {
    String pic = req.getPic();
    String url;
    String relativePath;

    if (StringUtils.hasText(pic) && (pic.startsWith("http://") || pic.startsWith("https://"))) {
      url = pic;
      if (pic.startsWith(cdnUrl + "/")) {
        relativePath = pic.substring(cdnUrl.length() + 1);
      } else {
        relativePath = pic;
      }
    } else {
      relativePath = pic;
      url = cdnUrl + "/" + pic.replace("\\", "/");
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

    Map<String, Object> result = new LinkedHashMap<>();
    String type = req.getType();

    if ("front".equals(type)) {
      String localFilePath = resolveLocalFilePath(relativePath);
      Map<String, Object> ocrData = baiduOcrService.recognizeIdCard(localFilePath, url, "front");
      if (ocrData != null && !ocrData.isEmpty()) {
        String idcardNumber = (String) ocrData.get("id_card_number");
        if (!StringUtils.hasText(idcardNumber)) {
          idcardNumber = req.getIdcard();
        }

        Map<String, Object> cardInfo = new LinkedHashMap<>();
        cardInfo.put("realname", ocrData.get("realname"));
        cardInfo.put("nation", ocrData.get("nation"));
        cardInfo.put("address", ocrData.get("address"));
        cardInfo.put("id_card_number", idcardNumber);
        cardInfo.put("birthday", ocrData.get("birthday"));
        cardInfo.put("gender", ocrData.get("gender"));
        cardInfo.put("age", ocrData.get("age"));
        cardInfo.put("id_card_front", url);

        if (StringUtils.hasText(idcardNumber)) {
          updateDetailFront(idcardNumber, url, (String) ocrData.get("realname"),
              (String) ocrData.get("nation"), (String) ocrData.get("address"),
              (String) ocrData.get("birthday"), (String) ocrData.get("gender"));
        }

        result.put("idcard", cardInfo);
      } else {
        result.put("idcard", Map.of("id_card_number", req.getIdcard(), "id_card_front", url));
      }

    } else if ("back".equals(type)) {
      if (!StringUtils.hasText(req.getIdcard())) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号不能为空");
      }
      String localFilePath = resolveLocalFilePath(relativePath);
      Map<String, Object> ocrData = baiduOcrService.recognizeIdCard(localFilePath, url, "back");
      if (ocrData != null && !ocrData.isEmpty()) {
        updateDetailBack(req.getIdcard(), url,
            (String) ocrData.get("valid_date_start"),
            (String) ocrData.get("valid_date_end"));
      }
      result.put("id_card_back", url);

    } else if ("header".equals(type)) {
      if (StringUtils.hasText(req.getIdcard())) {
        updateDetailHeader(req.getIdcard(), url);
        if (req.getSignUpId() != null) {
          updateSignUpPhoto(req.getSignUpId(), req.getIdcard(), url);
        }
      }
      result.put("id_card_header", url);
    }

    return result;
  }

  /**
   * 解析本地文件路径：先尝试 fileLocalRoot（新文件存储），再回退 cdnPath（旧 CDN 目录）
   */
  private String resolveLocalFilePath(String relativePath) {
    Path newPath = Paths.get(fileLocalRoot).resolve(relativePath.replace("/", java.io.File.separator));
    if (Files.exists(newPath)) {
      return newPath.toString();
    }
    Path oldPath = Paths.get(cdnPath).resolve(relativePath.replace("/", java.io.File.separator));
    if (Files.exists(oldPath)) {
      return oldPath.toString();
    }
    return newPath.toString();
  }

  private void updateDetailFront(String idcard, String frontUrl, String realname,
                                  String nation, String address, String birthday, String gender) {
    PlatformUserDetailEntity detail = detailMapper.selectOne(new LambdaQueryWrapper<PlatformUserDetailEntity>()
        .eq(PlatformUserDetailEntity::getIdcard, idcard)
        .eq(PlatformUserDetailEntity::getDeleted, 0));
    if (detail != null) {
      detail.setIdcardFront(frontUrl);
      if (StringUtils.hasText(realname)) {
        detail.setRealname(realname);
      }
      if (StringUtils.hasText(nation)) {
        detail.setNation(nation);
      }
      if (StringUtils.hasText(address)) {
        detail.setAddress(address);
      }
      if (StringUtils.hasText(birthday)) {
        detail.setBirthday(birthday);
      }
      if (StringUtils.hasText(gender)) {
        detail.setGender("女".equals(gender) ? 1 : 0);
      }
      detailMapper.updateById(detail);
    }
  }

  private void updateDetailBack(String idcard, String backUrl, String validDateStart, String validDateEnd) {
    PlatformUserDetailEntity detail = detailMapper.selectOne(new LambdaQueryWrapper<PlatformUserDetailEntity>()
        .eq(PlatformUserDetailEntity::getIdcard, idcard)
        .eq(PlatformUserDetailEntity::getDeleted, 0));
    if (detail != null) {
      detail.setIdcardBack(backUrl);
      if (StringUtils.hasText(validDateStart)) {
        detail.setIdcardStartDate(validDateStart);
      }
      if (StringUtils.hasText(validDateEnd)) {
        detail.setIdcardEndDate(validDateEnd);
      }
      detailMapper.updateById(detail);
    }
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

  /**
   * 文件 OCR（对齐 PHP fileorcAction）：上传文件后返回 URL 供前端回显
   */
  public Map<String, Object> fileOcr(String pic, String type, String idcard) {
    if (!StringUtils.hasText(pic)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "图片为空");
    }
    if (!StringUtils.hasText(idcard)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号为空");
    }

    String url;
    if (pic.startsWith("http://") || pic.startsWith("https://")) {
      url = pic;
    } else {
      url = cdnUrl + "/" + pic.replace("\\", "/");
    }

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("pic", url);
    if ("header".equals(type)) {
      String file = "idCard/headerOrigin/" + idcard + "_" + type + ".jpg";
      result.put("id_card_header", file);
    }
    return result;
  }
}
