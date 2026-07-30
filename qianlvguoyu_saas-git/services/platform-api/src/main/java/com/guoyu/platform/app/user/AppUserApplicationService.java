package com.guoyu.platform.app.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.sms.AppSmsException;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserDetailEntity;
import com.guoyu.platform.trainingdepartment.entity.PlatformUserEntity;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserDetailMapper;
import com.guoyu.platform.trainingdepartment.mapper.PlatformUserMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 对齐 PHP AppuserController */
@Service
public class AppUserApplicationService {

  private final PlatformUserMapper userMapper;
  private final PlatformUserDetailMapper detailMapper;
  private final String kefuPhone;
  private final String agreementUrl;
  private final String privacyUrl;
  private final String cdnUrl;

  public AppUserApplicationService(
      PlatformUserMapper userMapper,
      PlatformUserDetailMapper detailMapper,
      @Value("${platform.app.kefu-phone:18977081413}") String kefuPhone,
      @Value("${platform.app.agreement-url:}") String agreementUrl,
      @Value("${platform.app.privacy-url:}") String privacyUrl,
      @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl) {
    this.userMapper = userMapper;
    this.detailMapper = detailMapper;
    this.kefuPhone = kefuPhone;
    this.agreementUrl = agreementUrl;
    this.privacyUrl = privacyUrl;
    this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
  }

  public Map<String, Object> infoByIdCard(String idcard) {
    if (!StringUtils.hasText(idcard)) {
      throw new AppSmsException(-1, "证件号为空");
    }
    PlatformUserDetailEntity detail = detailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getIdcard, idcard.trim())
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (detail == null) {
      throw new AppSmsException(-1, "该身份证号还未注册");
    }
    Map<String, Object> info = new LinkedHashMap<>();
    info.put("id", detail.getId());
    info.put("phone", detail.getPhone());
    info.put("idcard", detail.getIdcard());
    info.put("realname", detail.getRealname());
    return info;
  }

  /** 登录用户信息（我的页面头部） */
  public Map<String, Object> userInfo(Long userId) {
    if (userId == null || userId <= 0) {
      throw new AppSmsException(-1, "未登录");
    }
    PlatformUserEntity user = userMapper.selectById(userId);
    if (user == null || user.getDeleted() != 0) {
      throw new AppSmsException(-1, "用户不存在");
    }
    PlatformUserDetailEntity detail = detailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getUserId, userId)
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (detail == null && StringUtils.hasText(user.getIdcard())) {
      detail = detailMapper.selectOne(
          new LambdaQueryWrapper<PlatformUserDetailEntity>()
              .eq(PlatformUserDetailEntity::getIdcard, user.getIdcard())
              .eq(PlatformUserDetailEntity::getDeleted, 0)
              .last("LIMIT 1"));
    }

    String realname = StringUtils.hasText(user.getRealname())
        ? user.getRealname()
        : (detail != null && StringUtils.hasText(detail.getRealname()) ? detail.getRealname() : user.getPhone());

    Map<String, Object> userMap = new LinkedHashMap<>();
    userMap.put("id", user.getId());
    userMap.put("realname", realname);
    userMap.put("phone", user.getPhone());
    userMap.put("idcard", user.getIdcard());
    userMap.put("expert_id", user.getExpertId() != null ? user.getExpertId() : 0);
    userMap.put("orgId", user.getOrgId());
    userMap.put("companyId", user.getCompanyId());
    userMap.put("point", 0);
    userMap.put("wealthValue", 0);
    userMap.put("valueValuation", 0);
    userMap.put("wk_level", detail != null && detail.getEducation() != null ? detail.getEducation() : "");
    userMap.put("addtime", user.getAddtime());
    userMap.put("logintime", user.getLogintime());

    Map<String, Object> detailMap = new LinkedHashMap<>();
    if (detail != null) {
      detailMap.put("id", detail.getId());
      detailMap.put("phone", detail.getPhone());
      detailMap.put("idcard", detail.getIdcard());
      detailMap.put("realname", detail.getRealname());
      detailMap.put("gender", detail.getGender());
      detailMap.put("education", detail.getEducation());
      detailMap.put("address", detail.getAddress());
      detailMap.put("idcardHeader", detail.getIdcardHeader());
    }

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("user", userMap);
    data.put("detail", detailMap);
    return data;
  }

  /** 我的页面配置 */
  public Map<String, Object> myPageData(Long userId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("phone", kefuPhone);

    Map<String, String> link = new LinkedHashMap<>();
    link.put("agreement", agreementUrl);
    link.put("privacy", privacyUrl);
    data.put("link", link);

    List<Map<String, Object>> navs = new ArrayList<>();
    navs.add(nav("签到", "icon-qiandao", "/pages/user/sign"));
    navs.add(nav("消息中心", "icon-xiaoxi1", "/pages/user/message"));
    data.put("navs", navs);

    List<Map<String, Object>> menus = new ArrayList<>();
    menus.add(menu("联系客服", "icon-kefu", "/pages/user/kefu"));
    menus.add(menu("设置", "icon-shezhi", "/pages/user/setting"));
    data.put("menus", menus);

    if (userId != null && userId > 0) {
      PlatformUserEntity user = userMapper.selectById(userId);
      boolean isExpert = user != null && user.getDeleted() == 0
          && user.getExpertId() != null && user.getExpertId() > 0;
      data.put("showExpertTask", isExpert);
    } else {
      data.put("showExpertTask", false);
    }
    return data;
  }

  /** 用户资料详情（编辑页回显） */
  public Map<String, Object> detail(Long userId) {
    if (userId == null || userId <= 0) {
      throw new AppSmsException(-1, "未登录");
    }
    PlatformUserEntity user = userMapper.selectById(userId);
    if (user == null || user.getDeleted() != 0) {
      throw new AppSmsException(-1, "用户不存在");
    }
    PlatformUserDetailEntity d = findDetail(user);
    Map<String, Object> userDetail = new LinkedHashMap<>();
    if (d != null) {
      userDetail.put("id", d.getId());
      userDetail.put("phone", d.getPhone());
      userDetail.put("realname", d.getRealname());
      userDetail.put("idcard", d.getIdcard());
      userDetail.put("gender", d.getGender());
      userDetail.put("nation", d.getNation());
      userDetail.put("political", d.getPolitical());
      userDetail.put("address", d.getAddress());
      userDetail.put("house_type", d.getHouseType());
      userDetail.put("idcard_header", toAbsoluteUrl(d.getIdcardHeader()));
      userDetail.put("idcard_front", d.getIdcardFront());
      userDetail.put("idcard_back", d.getIdcardBack());
      userDetail.put("idcard_front_show", toAbsoluteUrl(d.getIdcardFront()));
      userDetail.put("idcard_back_show", toAbsoluteUrl(d.getIdcardBack()));
      userDetail.put("school", d.getSchool());
      userDetail.put("education", d.getEducation());
      userDetail.put("major", d.getMajor());
      userDetail.put("school_no", d.getSchoolNo());
      userDetail.put("in_school_date", d.getInSchoolDate());
      userDetail.put("graduate_date", d.getGraduateDate());
      userDetail.put("school_paper", toAbsoluteUrl(d.getSchoolPaper()));
      userDetail.put("school_paper_has", StringUtils.hasText(d.getSchoolPaper()) ? 1 : 0);
      userDetail.put("work_time", d.getWorkTime());
      userDetail.put("title_type", d.getTitleType());
    } else {
      userDetail.put("phone", user.getPhone());
      userDetail.put("realname", user.getRealname());
      userDetail.put("idcard", user.getIdcard());
      userDetail.put("title_type", "无");
      userDetail.put("school_paper_has", 0);
    }
    return Map.of("userDetail", userDetail);
  }

  /** 保存用户资料 */
  @Transactional
  public Map<String, Object> detailSave(Long userId, Map<String, Object> form) {
    if (userId == null || userId <= 0) {
      throw new AppSmsException(-1, "未登录");
    }
    if (form == null || form.isEmpty()) {
      throw new AppSmsException(-1, "参数不合法");
    }
    PlatformUserEntity user = userMapper.selectById(userId);
    if (user == null || user.getDeleted() != 0) {
      throw new AppSmsException(-1, "用户不存在");
    }

    form.remove("idcard_front_show");
    form.remove("idcard_back_show");
    form.remove("school_paper_has");

    Long detailId = toLong(form.get("id"));
    PlatformUserDetailEntity d;
    boolean isNew = false;
    if (detailId != null && detailId > 0) {
      d = detailMapper.selectById(detailId);
      if (d == null || (d.getDeleted() != null && d.getDeleted() == 1)) {
        throw new AppSmsException(-1, "资料记录不存在");
      }
    } else {
      d = findDetail(user);
      if (d == null) {
        d = new PlatformUserDetailEntity();
        d.setUserId(userId);
        d.setPhone(user.getPhone());
        d.setIdcard(user.getIdcard());
        d.setDeleted(0);
        isNew = true;
      }
    }

    applyForm(d, form);
    if (StringUtils.hasText(d.getRealname()) && !d.getRealname().equals(user.getRealname())) {
      user.setRealname(d.getRealname());
      userMapper.updateById(user);
    }

    boolean ok = isNew ? detailMapper.insert(d) > 0 : detailMapper.updateById(d) > 0;
    if (!ok) {
      throw new AppSmsException(-1, "保存失败");
    }
    return Map.of("id", d.getId());
  }

  private PlatformUserDetailEntity findDetail(PlatformUserEntity user) {
    PlatformUserDetailEntity d = detailMapper.selectOne(
        new LambdaQueryWrapper<PlatformUserDetailEntity>()
            .eq(PlatformUserDetailEntity::getUserId, user.getId())
            .eq(PlatformUserDetailEntity::getDeleted, 0)
            .last("LIMIT 1"));
    if (d == null && StringUtils.hasText(user.getIdcard())) {
      d = detailMapper.selectOne(
          new LambdaQueryWrapper<PlatformUserDetailEntity>()
              .eq(PlatformUserDetailEntity::getIdcard, user.getIdcard())
              .eq(PlatformUserDetailEntity::getDeleted, 0)
              .last("LIMIT 1"));
    }
    return d;
  }

  private static void applyForm(PlatformUserDetailEntity d, Map<String, Object> form) {
    if (form.containsKey("realname")) d.setRealname(str(form.get("realname")));
    if (form.containsKey("phone")) d.setPhone(str(form.get("phone")));
    if (form.containsKey("idcard")) d.setIdcard(str(form.get("idcard")));
    if (form.containsKey("gender")) d.setGender(toInt(form.get("gender")));
    if (form.containsKey("nation")) d.setNation(str(form.get("nation")));
    if (form.containsKey("political")) d.setPolitical(str(form.get("political")));
    if (form.containsKey("address")) d.setAddress(str(form.get("address")));
    if (form.containsKey("house_type")) d.setHouseType(str(form.get("house_type")));
    if (form.containsKey("idcard_header")) d.setIdcardHeader(str(form.get("idcard_header")));
    if (form.containsKey("idcard_front")) d.setIdcardFront(str(form.get("idcard_front")));
    if (form.containsKey("idcard_back")) d.setIdcardBack(str(form.get("idcard_back")));
    if (form.containsKey("school")) d.setSchool(str(form.get("school")));
    if (form.containsKey("education")) d.setEducation(str(form.get("education")));
    if (form.containsKey("major")) d.setMajor(str(form.get("major")));
    if (form.containsKey("school_no")) d.setSchoolNo(str(form.get("school_no")));
    if (form.containsKey("in_school_date")) d.setInSchoolDate(str(form.get("in_school_date")));
    if (form.containsKey("graduate_date")) d.setGraduateDate(str(form.get("graduate_date")));
    if (form.containsKey("school_paper")) d.setSchoolPaper(str(form.get("school_paper")));
    if (form.containsKey("work_time")) d.setWorkTime(str(form.get("work_time")));
    if (form.containsKey("title_type")) d.setTitleType(str(form.get("title_type")));
  }

  private String toAbsoluteUrl(String path) {
    if (!StringUtils.hasText(path)) return path;
    if (path.startsWith("http://") || path.startsWith("https://")) return path;
    String normalized = path.startsWith("/") ? path.substring(1) : path;
    return cdnUrl + "/" + normalized;
  }

  private static Map<String, Object> nav(String title, String icon, String page) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("title", title);
    item.put("icon", icon);
    item.put("page", page);
    return item;
  }

  private static Map<String, Object> menu(String title, String icon, String page) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("title", title);
    item.put("icon", icon);
    item.put("page", page);
    return item;
  }

  private static String str(Object o) {
    return o == null ? null : String.valueOf(o).trim();
  }

  private static Integer toInt(Object o) {
    if (o == null) return null;
    try { return Integer.parseInt(String.valueOf(o).trim()); }
    catch (NumberFormatException e) { return null; }
  }

  private static Long toLong(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.longValue();
    try { return Long.parseLong(String.valueOf(o).trim()); }
    catch (NumberFormatException e) { return null; }
  }
}
