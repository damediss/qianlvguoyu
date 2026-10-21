package com.guoyu.platform.trainingdepartment.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainUserInfoEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainUserInfoMapper;
import com.guoyu.platform.trainingdepartment.support.TrainWillingnessFormFiller.FillInput;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 开班申报：按班级花名册批量生成《培训意愿需求登记表》（官方模板填空）。
 */
@Component
@Slf4j
public class TrainWillingnessFormPackService {

  public static final String TEMPLATE_PATH =
      "templates/class-declare/nanning_training_willingness_form.docx";
  private static final String FORM_TITLE = "培训意愿需求登记表";

  private final TrainSignUpPeopleSelfMapper signUpPeopleMapper;
  private final PlatformUserDetailMapper platformUserDetailMapper;
  private final TrainUserInfoMapper trainUserInfoMapper;
  private final ExamWorkTypeMapper workTypeMapper;

  public TrainWillingnessFormPackService(
      TrainSignUpPeopleSelfMapper signUpPeopleMapper,
      PlatformUserDetailMapper platformUserDetailMapper,
      TrainUserInfoMapper trainUserInfoMapper,
      ExamWorkTypeMapper workTypeMapper) {
    this.signUpPeopleMapper = signUpPeopleMapper;
    this.platformUserDetailMapper = platformUserDetailMapper;
    this.trainUserInfoMapper = trainUserInfoMapper;
    this.workTypeMapper = workTypeMapper;
  }

  public record PackResult(
      byte[] zipBytes,
      int generated,
      int failed,
      List<String> notes,
      String workTypeTitle,
      Integer classLevel) {}

  public PackResult buildZip(
      TrainInfoClassEntity clazz,
      TrainInfoListEntity trainInfo,
      List<TrainPeopleListEntity> students) throws Exception {
    if (students == null || students.isEmpty()) {
      throw new IllegalStateException("该班级未添加任何学员");
    }
    byte[] template = loadTemplate();
    String workTypeTitle = resolveWorkTypeTitle(clazz, trainInfo);
    Integer classLevel = clazz.getWorkTypeLevel();
    if ((classLevel == null || classLevel <= 0) && trainInfo != null) {
      classLevel = trainInfo.getWorkTypeLevel();
    }

    ProfileSources sources = loadProfileSources(students);

    List<Map.Entry<String, byte[]>> entries = new ArrayList<>();
    Set<String> usedNames = new HashSet<>();
    List<String> notes = new ArrayList<>();
    int generated = 0;
    int failed = 0;

    for (TrainPeopleListEntity student : students) {
      try {
        FillInput input = buildInput(
            student,
            workTypeTitle,
            classLevel,
            clazz.getCompanyId() != null,
            sources);
        byte[] filled = TrainWillingnessFormFiller.fill(template, input);
        String fileName = uniqueName(
            sanitize(firstNonBlank(input.name(), "学员"))
                + (StringUtils.hasText(input.idCard()) ? "_" + input.idCard() : "")
                + "_"
                + FORM_TITLE
                + ".docx",
            usedNames);
        entries.add(Map.entry(fileName, filled));
        generated++;
      } catch (Exception ex) {
        failed++;
        String who = firstNonBlank(student.getRealname(), String.valueOf(student.getId()));
        notes.add(who + "：生成失败");
        log.warn("生成培训意愿需求登记表失败 peopleId={}", student.getId(), ex);
      }
    }

    if (entries.isEmpty()) {
      throw new IllegalStateException("未能生成任何意愿登记表，请检查学员资料");
    }

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(bos)) {
      String readme = FORM_TITLE + "\n"
          + "classId=" + clazz.getId() + "\n"
          + "classTitle=" + nullToEmpty(clazz.getTitle()) + "\n"
          + "count=" + entries.size() + "\n"
          + "generatedAt=" + LocalDateTime.now() + "\n"
          + "note=已预填基本信息/人员类别/就业与意愿句；承诺抄写与签字盖章请线下完成。\n";
      zos.putNextEntry(new ZipEntry("README.txt"));
      zos.write(readme.getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
      for (Map.Entry<String, byte[]> e : entries) {
        zos.putNextEntry(new ZipEntry(FORM_TITLE + "/" + e.getKey()));
        zos.write(e.getValue());
        zos.closeEntry();
      }
    }
    return new PackResult(bos.toByteArray(), generated, failed, notes, workTypeTitle, classLevel);
  }

  private FillInput buildInput(
      TrainPeopleListEntity student,
      String workTypeTitle,
      Integer classLevel,
      boolean classHasCompany,
      ProfileSources sources) {
    String idCard = normalizeIdCard(student.getIdCard());
    TrainSignUpPeopleSelfEntity signUp = sources.resolveSignUp(student);
    PlatformUserDetailEntity detail = sources.resolveDetail(student);
    TrainUserInfoEntity userInfo = sources.resolveUserInfo(student);

    // 资料优先报名人员/平台详情（与「报名人员编辑」一致），花名册仅作兜底
    String name = firstNonBlank(
        signUp != null ? signUp.getName() : null,
        detail != null ? detail.getRealname() : null,
        student.getRealname(),
        "学员");
    String phone = firstNonBlank(
        signUp != null ? signUp.getPhone() : null,
        detail != null ? detail.getPhone() : null,
        student.getPhone());
    String address = firstNonBlank(
        signUp != null ? signUp.getAddress() : null,
        detail != null ? detail.getAddress() : null);
    String nation = firstNonBlank(
        signUp != null ? signUp.getNation() : null,
        detail != null ? detail.getNation() : null);
    if (StringUtils.hasText(nation) && !nation.endsWith("族") && nation.length() <= 5) {
      nation = nation + "族";
    }
    if (!StringUtils.hasText(idCard)) {
      idCard = firstNonBlank(
          signUp != null ? normalizeIdCard(signUp.getIdCardNumber()) : null,
          detail != null ? normalizeIdCard(detail.getIdcard()) : null);
    }

    String gender = resolveGender(signUp, detail, idCard);

    String age = "";
    if (signUp != null && signUp.getAge() != null && signUp.getAge() > 0) {
      age = String.valueOf(signUp.getAge());
    }
    if (!StringUtils.hasText(age)) {
      age = TrainWillingnessFormFiller.ageFromIdCard(idCard, LocalDate.now());
    }

    String categoryHint = firstNonBlank(
        userInfo != null ? userInfo.getPersonType() : null,
        userInfo != null ? userInfo.getPersonSupplementary() : null,
        signUp != null ? signUp.getHouseType() : null,
        detail != null ? detail.getHouseType() : null,
        signUp != null ? signUp.getComments() : null);

    String companyName = firstNonBlank(signUp != null ? signUp.getCompanyName() : null);
    String jobPost = firstNonBlank(
        signUp != null ? signUp.getJob() : null,
        signUp != null ? signUp.getJog() : null,
        workTypeTitle);
    boolean employed = StringUtils.hasText(companyName)
        || StringUtils.hasText(signUp != null ? signUp.getJob() : null)
        || StringUtils.hasText(signUp != null ? signUp.getJog() : null)
        || (StringUtils.hasText(categoryHint)
            && (categoryHint.contains("在岗") || categoryHint.contains("企业")))
        || classHasCompany;

    String willingnessWorkType = firstNonBlank(
        signUp != null ? signUp.getReqWorkTypeName() : null,
        signUp != null ? signUp.getWorkTypeName() : null,
        workTypeTitle);

    return new FillInput(
        name,
        gender,
        nation,
        age,
        idCard,
        phone,
        address,
        categoryHint,
        employed,
        employed ? jobPost : "",
        false,
        "",
        willingnessWorkType,
        classLevel);
  }

  private static String resolveGender(
      TrainSignUpPeopleSelfEntity signUp, PlatformUserDetailEntity detail, String idCard) {
    if (signUp != null && StringUtils.hasText(signUp.getGender())) {
      String g = signUp.getGender().trim();
      if ("女".equals(g) || "1".equals(g)) {
        return "女";
      }
      if ("男".equals(g) || "0".equals(g) || "2".equals(g)) {
        // 报名 self：常见「男/女」；偶发 0/1。平台 detail 另用 gender>0=女
        return "男".equals(g) || "0".equals(g) ? "男" : ("2".equals(g) ? "女" : g);
      }
      return g;
    }
    if (detail != null && detail.getGender() != null) {
      return detail.getGender() > 0 ? "女" : "男";
    }
    return TrainWillingnessFormFiller.genderFromIdCard(idCard);
  }

  private String resolveWorkTypeTitle(TrainInfoClassEntity clazz, TrainInfoListEntity trainInfo) {
    Long workTypeId = clazz.getWorkTypeId();
    if (workTypeId == null && trainInfo != null) {
      workTypeId = trainInfo.getWorkTypeId();
    }
    if (workTypeId == null) {
      return "";
    }
    ExamWorkTypeEntity wt = workTypeMapper.selectById(workTypeId);
    return wt != null ? nullToEmpty(wt.getTitle()) : "";
  }

  private ProfileSources loadProfileSources(List<TrainPeopleListEntity> students) {
    List<String> idCards = idCardsOf(students);
    Set<Long> userIds = userIdsOf(students);

    Map<String, TrainSignUpPeopleSelfEntity> signUpByIdCard = new HashMap<>();
    Map<Long, TrainSignUpPeopleSelfEntity> signUpByUserId = new HashMap<>();
    if (!idCards.isEmpty() || !userIds.isEmpty()) {
      LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> q =
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0);
      q.and(w -> {
        if (!idCards.isEmpty()) {
          w.in(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCards);
        }
        if (!userIds.isEmpty()) {
          if (!idCards.isEmpty()) {
            w.or();
          }
          w.and(u -> u.in(TrainSignUpPeopleSelfEntity::getPlatformUserId, userIds)
              .or()
              .in(TrainSignUpPeopleSelfEntity::getUserId, userIds));
        }
      });
      for (TrainSignUpPeopleSelfEntity row : signUpPeopleMapper.selectList(q)) {
        indexSignUp(signUpByIdCard, signUpByUserId, row);
      }
    }

    Map<String, PlatformUserDetailEntity> detailByIdCard = new HashMap<>();
    Map<Long, PlatformUserDetailEntity> detailByUserId = new HashMap<>();
    if (!idCards.isEmpty() || !userIds.isEmpty()) {
      LambdaQueryWrapper<PlatformUserDetailEntity> q =
          new LambdaQueryWrapper<PlatformUserDetailEntity>()
              .eq(PlatformUserDetailEntity::getDeleted, 0);
      q.and(w -> {
        if (!idCards.isEmpty()) {
          w.in(PlatformUserDetailEntity::getIdcard, idCards);
        }
        if (!userIds.isEmpty()) {
          if (!idCards.isEmpty()) {
            w.or();
          }
          w.in(PlatformUserDetailEntity::getUserId, userIds);
        }
      });
      for (PlatformUserDetailEntity row : platformUserDetailMapper.selectList(q)) {
        indexDetail(detailByIdCard, detailByUserId, row);
      }
    }

    Map<Long, TrainUserInfoEntity> userInfoByUserId = new LinkedHashMap<>();
    if (!userIds.isEmpty()) {
      try {
        List<TrainUserInfoEntity> rows = trainUserInfoMapper.selectList(
            new LambdaQueryWrapper<TrainUserInfoEntity>()
                .in(TrainUserInfoEntity::getUserId, userIds)
                .eq(TrainUserInfoEntity::getDeleted, 0));
        for (TrainUserInfoEntity row : rows) {
          if (row.getUserId() != null) {
            userInfoByUserId.putIfAbsent(row.getUserId(), row);
          }
        }
      } catch (Exception ex) {
        log.debug("加载 train_user_info 人员类别失败，将使用默认勾选: {}", ex.getMessage());
      }
    }

    return new ProfileSources(
        signUpByIdCard, signUpByUserId, detailByIdCard, detailByUserId, userInfoByUserId);
  }

  private static void indexSignUp(
      Map<String, TrainSignUpPeopleSelfEntity> byIdCard,
      Map<Long, TrainSignUpPeopleSelfEntity> byUserId,
      TrainSignUpPeopleSelfEntity row) {
    String key = normalizeIdCard(row.getIdCardNumber());
    if (StringUtils.hasText(key)) {
      byIdCard.merge(key, row, TrainWillingnessFormPackService::preferRicherSignUp);
    }
    if (row.getPlatformUserId() != null) {
      byUserId.merge(row.getPlatformUserId(), row, TrainWillingnessFormPackService::preferRicherSignUp);
    }
    if (row.getUserId() != null) {
      byUserId.merge(row.getUserId(), row, TrainWillingnessFormPackService::preferRicherSignUp);
    }
  }

  private static void indexDetail(
      Map<String, PlatformUserDetailEntity> byIdCard,
      Map<Long, PlatformUserDetailEntity> byUserId,
      PlatformUserDetailEntity row) {
    String key = normalizeIdCard(row.getIdcard());
    if (StringUtils.hasText(key)) {
      byIdCard.merge(key, row, TrainWillingnessFormPackService::preferRicherDetail);
    }
    if (row.getUserId() != null) {
      byUserId.merge(row.getUserId(), row, TrainWillingnessFormPackService::preferRicherDetail);
    }
  }

  /** 同身份证多条报名时，优先保留地址/民族等资料更完整的一条。 */
  static TrainSignUpPeopleSelfEntity preferRicherSignUp(
      TrainSignUpPeopleSelfEntity a, TrainSignUpPeopleSelfEntity b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return signUpRichness(b) > signUpRichness(a) ? b : a;
  }

  static PlatformUserDetailEntity preferRicherDetail(
      PlatformUserDetailEntity a, PlatformUserDetailEntity b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    return detailRichness(b) > detailRichness(a) ? b : a;
  }

  private static int signUpRichness(TrainSignUpPeopleSelfEntity e) {
    if (e == null) {
      return -1;
    }
    int score = 0;
    if (StringUtils.hasText(e.getAddress())) {
      score += 8;
    }
    if (StringUtils.hasText(e.getNation())) {
      score += 2;
    }
    if (StringUtils.hasText(e.getPhone())) {
      score += 1;
    }
    if (StringUtils.hasText(e.getName())) {
      score += 1;
    }
    if (StringUtils.hasText(e.getHouseType())) {
      score += 1;
    }
    return score;
  }

  private static int detailRichness(PlatformUserDetailEntity e) {
    if (e == null) {
      return -1;
    }
    int score = 0;
    if (StringUtils.hasText(e.getAddress())) {
      score += 8;
    }
    if (StringUtils.hasText(e.getNation())) {
      score += 2;
    }
    if (StringUtils.hasText(e.getPhone())) {
      score += 1;
    }
    if (StringUtils.hasText(e.getRealname())) {
      score += 1;
    }
    return score;
  }

  private static List<String> idCardsOf(List<TrainPeopleListEntity> students) {
    List<String> idCards = new ArrayList<>();
    for (TrainPeopleListEntity s : students) {
      if (!StringUtils.hasText(s.getIdCard())) {
        continue;
      }
      String raw = s.getIdCard().trim();
      idCards.add(raw);
      String normalized = normalizeIdCard(raw);
      if (StringUtils.hasText(normalized) && !normalized.equals(raw)) {
        idCards.add(normalized);
      }
    }
    return idCards.stream().distinct().toList();
  }

  private static Set<Long> userIdsOf(List<TrainPeopleListEntity> students) {
    Set<Long> userIds = new HashSet<>();
    for (TrainPeopleListEntity s : students) {
      if (s.getUserId() != null) {
        userIds.add(s.getUserId());
      }
      if (s.getPlatformUserId() != null) {
        userIds.add(s.getPlatformUserId());
      }
    }
    return userIds;
  }

  /** 身份证归一：去空白、末位 x→X，避免匹配丢失。 */
  static String normalizeIdCard(String idCard) {
    if (!StringUtils.hasText(idCard)) {
      return "";
    }
    return idCard.trim().toUpperCase();
  }

  private record ProfileSources(
      Map<String, TrainSignUpPeopleSelfEntity> signUpByIdCard,
      Map<Long, TrainSignUpPeopleSelfEntity> signUpByUserId,
      Map<String, PlatformUserDetailEntity> detailByIdCard,
      Map<Long, PlatformUserDetailEntity> detailByUserId,
      Map<Long, TrainUserInfoEntity> userInfoByUserId) {

    TrainSignUpPeopleSelfEntity resolveSignUp(TrainPeopleListEntity student) {
      String idCard = normalizeIdCard(student.getIdCard());
      TrainSignUpPeopleSelfEntity byCard =
          StringUtils.hasText(idCard) ? signUpByIdCard.get(idCard) : null;
      TrainSignUpPeopleSelfEntity byPlatform =
          student.getPlatformUserId() != null ? signUpByUserId.get(student.getPlatformUserId()) : null;
      TrainSignUpPeopleSelfEntity byUser =
          student.getUserId() != null ? signUpByUserId.get(student.getUserId()) : null;
      TrainSignUpPeopleSelfEntity best = byCard;
      best = preferRicherSignUp(best, byPlatform);
      best = preferRicherSignUp(best, byUser);
      return best;
    }

    PlatformUserDetailEntity resolveDetail(TrainPeopleListEntity student) {
      String idCard = normalizeIdCard(student.getIdCard());
      PlatformUserDetailEntity byCard =
          StringUtils.hasText(idCard) ? detailByIdCard.get(idCard) : null;
      PlatformUserDetailEntity byPlatform =
          student.getPlatformUserId() != null ? detailByUserId.get(student.getPlatformUserId()) : null;
      PlatformUserDetailEntity byUser =
          student.getUserId() != null ? detailByUserId.get(student.getUserId()) : null;
      PlatformUserDetailEntity best = byCard;
      best = preferRicherDetail(best, byPlatform);
      best = preferRicherDetail(best, byUser);
      return best;
    }

    TrainUserInfoEntity resolveUserInfo(TrainPeopleListEntity student) {
      if (student.getUserId() != null) {
        TrainUserInfoEntity hit = userInfoByUserId.get(student.getUserId());
        if (hit != null) {
          return hit;
        }
      }
      if (student.getPlatformUserId() != null) {
        return userInfoByUserId.get(student.getPlatformUserId());
      }
      return null;
    }
  }

  private byte[] loadTemplate() throws Exception {
    ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
    if (!resource.exists()) {
      throw new IllegalStateException("意愿登记表模板不存在: " + TEMPLATE_PATH);
    }
    try (InputStream in = resource.getInputStream()) {
      return in.readAllBytes();
    }
  }

  private static String uniqueName(String name, Set<String> used) {
    if (used.add(name)) {
      return name;
    }
    int i = 2;
    while (true) {
      String n = name.replace(".docx", "_" + i + ".docx");
      if (used.add(n)) {
        return n;
      }
      i++;
    }
  }

  private static String sanitize(String name) {
    if (!StringUtils.hasText(name)) {
      return "学员";
    }
    return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
  }

  private static String firstNonBlank(String... values) {
    for (String v : values) {
      if (StringUtils.hasText(v)) {
        return v.trim();
      }
    }
    return "";
  }

  private static String trim(String s) {
    return s == null ? "" : s.trim();
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
