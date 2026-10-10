package com.guoyu.platform.trainingdepartment.support;

import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * 申报补贴人员口径：仅「已获证」学员进入材料生成。
 * 与毕业典礼证书回填一致：有证书文件 / 技能等级证 / 非 GC- 假号。
 */
public final class SubsidyCertifiedPeopleSupport {

  private SubsidyCertifiedPeopleSupport() {}

  /** 是否视为已回填真实证书（可申报） */
  public static boolean isCertified(TrainCertificateEntity c) {
    if (c == null) {
      return false;
    }
    if (StringUtils.hasText(c.getFilePath())) {
      return true;
    }
    String type = c.getCertificateType();
    if ("skill_level_cert".equalsIgnoreCase(type) || "renshe".equalsIgnoreCase(type)) {
      return true;
    }
    String no = c.getCertificateNo();
    return StringUtils.hasText(no) && !no.startsWith("GC-");
  }

  public static List<TrainPeopleListEntity> filterCertified(
      List<TrainPeopleListEntity> students, Map<Long, TrainCertificateEntity> certByPeopleId) {
    if (students == null || students.isEmpty()) {
      return List.of();
    }
    Map<Long, TrainCertificateEntity> certs =
        certByPeopleId == null ? Map.of() : certByPeopleId;
    List<TrainPeopleListEntity> out = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      if (s == null || s.getId() == null) {
        continue;
      }
      if (isCertified(certs.get(s.getId()))) {
        out.add(s);
      }
    }
    return out;
  }

  public static int countCertified(
      List<TrainPeopleListEntity> students, Map<Long, TrainCertificateEntity> certByPeopleId) {
    return filterCertified(students, certByPeopleId).size();
  }
}
