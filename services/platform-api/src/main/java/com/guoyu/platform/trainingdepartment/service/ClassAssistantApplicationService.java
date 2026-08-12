package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.vo.FollowStudentTaskVO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * AI 班级助理（通道 D）—— A-P0/A-P1：规则意图 + 只读 Tool。
 * <p>不落会话表、不调 LLM，避免影响现网；写操作仅返回深链引导。
 */
@Service
public class ClassAssistantApplicationService {

  private static final Set<String> STAFF_ROLES = Set.of(
      TrainClassRoleApplicationService.LEAD_TEACHER,
      TrainClassRoleApplicationService.ASSISTANT,
      TrainClassRoleApplicationService.HEAD_TEACHER,
      TrainClassRoleApplicationService.FOLLOW_STAFF,
      TrainClassRoleApplicationService.MONITOR);

  private static final Set<String> TEACHER_DRAW_ROLES = Set.of(
      TrainClassRoleApplicationService.LEAD_TEACHER,
      TrainClassRoleApplicationService.ASSISTANT);

  private final TrainClassRoleApplicationService roleService;
  private final TrainScheduleApplicationService scheduleService;
  private final TrainFollowTaskApplicationService taskService;
  private final TrainFollowTaskMapper taskMapper;
  private final TrainFollowTaskItemMapper itemMapper;
  private final TrainInfoClassMapper classMapper;

  public ClassAssistantApplicationService(
      TrainClassRoleApplicationService roleService,
      TrainScheduleApplicationService scheduleService,
      TrainFollowTaskApplicationService taskService,
      TrainFollowTaskMapper taskMapper,
      TrainFollowTaskItemMapper itemMapper,
      TrainInfoClassMapper classMapper) {
    this.roleService = roleService;
    this.scheduleService = scheduleService;
    this.taskService = taskService;
    this.taskMapper = taskMapper;
    this.itemMapper = itemMapper;
    this.classMapper = classMapper;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> bootstrap(Long platformUserId, Long classId) {
    String primary = roleService.primaryRoleOfMember(platformUserId, classId);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);

    List<Map<String, Object>> chips = new ArrayList<>();
    chips.add(chip("schedule_today", "今日安排"));
    chips.add(chip("my_homework", "我的作业"));
    chips.add(chip("go_precise", "该刷哪"));
    chips.add(chip("change_faq", "调课说明"));
    if (STAFF_ROLES.contains(primary)) {
      chips.add(chip("progress", "本班进度"));
      chips.add(chip("unsubmitted", "谁没交"));
    }
    if (TEACHER_DRAW_ROLES.contains(primary)) {
      chips.add(chip("draw_hint", "帮我抽题"));
    }
    chips.add(chip("help", "能做什么"));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("classTitle", clazz == null ? null : clazz.getTitle());
    out.put("primaryRole", primary);
    out.put("chips", chips);
    out.put("welcome", "我是本班 AI 助理（规则版）。可点下方快捷指令，或输入「今日安排 / 我的作业 / 谁没交」。"
        + "班务数字来自系统查询；不替代批改与认定考试。");
    out.put("hint", "深度刷题请用「AI精准学」；考前冲刺请用「同步练习」。");
    return out;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> chat(Long platformUserId, Long classId, Map<String, Object> body) {
    String primary = roleService.primaryRoleOfMember(platformUserId, classId);
    String text = body == null || body.get("text") == null ? "" : String.valueOf(body.get("text")).trim();
    String intent = body != null && body.get("intent") != null
        ? String.valueOf(body.get("intent")).trim()
        : detectIntent(text);

    return switch (intent) {
      case "schedule_today" -> replySchedule(classId, primary);
      case "my_homework" -> replyMyHomework(platformUserId, classId, primary);
      case "progress" -> replyProgress(classId, primary);
      case "unsubmitted" -> replyUnsubmitted(classId, primary);
      case "change_faq" -> replyChangeFaq(classId);
      case "go_precise" -> replyPrecise(classId);
      case "draw_hint" -> replyDrawHint(classId, primary);
      case "help" -> replyHelp(primary);
      default -> replyUnknown(primary);
    };
  }

  private Map<String, Object> replySchedule(Long classId, String primary) {
    List<Map<String, Object>> schedule = scheduleService.getByClassIdForApp(classId);
    LocalDate today = LocalDate.now();
    List<Map<String, Object>> todayRows = schedule.stream()
        .filter(row -> {
          Object d = row.get("scheduleDate");
          if (d == null) {
            return false;
          }
          String s = String.valueOf(d);
          return s.startsWith(today.toString());
        })
        .limit(8)
        .toList();

    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();
    if (todayRows.isEmpty()) {
      replies.add(textReply("今天没有排课记录。可打开课表查看近期安排。"));
    } else {
      replies.add(textReply("今天共 " + todayRows.size() + " 条课次："));
      Map<String, Object> card = new LinkedHashMap<>();
      card.put("type", "schedule_list");
      card.put("items", todayRows.stream().map(r -> {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("title", Objects.toString(r.get("courseName"), "课次"));
        line.put("time", Objects.toString(r.get("startTime"), "") + "-" + Objects.toString(r.get("endTime"), ""));
        line.put("date", Objects.toString(r.get("scheduleDate"), ""));
        return line;
      }).toList());
      cards.add(card);
    }
    actions.add(nav("打开课表", "/pages/class/schedule?classId=" + classId));
    return pack("schedule_today", primary, replies, cards, actions);
  }

  private Map<String, Object> replyMyHomework(Long platformUserId, Long classId, String primary) {
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();
    if (STAFF_ROLES.contains(primary) && !Objects.equals(primary, TrainClassRoleApplicationService.MONITOR)) {
      replies.add(textReply("老师请查看「作业进度」管理布置与完成情况；学员待办请让学员本人打开「课堂作业」。"));
      actions.add(nav("作业进度", "/pages/class/homeworkProgress?classId=" + classId));
      if (TEACHER_DRAW_ROLES.contains(primary)) {
        actions.add(nav("布置作业", "/pages/class/homeworkAssign?classId=" + classId));
      }
      return pack("my_homework", primary, replies, cards, actions);
    }
    List<FollowStudentTaskVO> tasks = taskService.listStudentTasks(platformUserId, classId);
    replies.add(textReply(tasks.isEmpty()
        ? "当前没有下发给你的课堂作业。"
        : ("你有 " + tasks.size() + " 条作业记录，请到课堂作业页查看待办与结果。")));
    actions.add(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId));
    return pack("my_homework", primary, replies, cards, actions);
  }

  private Map<String, Object> replyProgress(Long classId, String primary) {
    if (!STAFF_ROLES.contains(primary)) {
      return pack("progress", primary,
          List.of(textReply("进度汇总仅对班长/班主任/教务/主讲开放。你可查看自己的课堂作业。")),
          List.of(),
          List.of(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId)));
    }
    List<TrainFollowTaskEntity> tasks = taskMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .in(TrainFollowTaskEntity::getStatus, "published", "closed", "draft")
        .orderByDesc(TrainFollowTaskEntity::getCreatedAt)
        .last("LIMIT 10"));
    List<Map<String, Object>> lines = new ArrayList<>();
    for (TrainFollowTaskEntity t : tasks) {
      long total = itemMapper.selectCount(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
          .eq(TrainFollowTaskItemEntity::getTaskId, t.getId())
          .eq(TrainFollowTaskItemEntity::getDeleted, 0));
      long submitted = itemMapper.selectCount(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
          .eq(TrainFollowTaskItemEntity::getTaskId, t.getId())
          .eq(TrainFollowTaskItemEntity::getDeleted, 0)
          .in(TrainFollowTaskItemEntity::getStatus, "submitted", "graded"));
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("title", t.getTitle());
      row.put("status", t.getStatus());
      row.put("submitted", submitted);
      row.put("total", total);
      lines.add(row);
    }
    List<Map<String, Object>> replies = new ArrayList<>();
    replies.add(textReply(lines.isEmpty() ? "本班暂无作业任务。" : ("最近 " + lines.size() + " 条作业进度如下：")));
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "progress_list");
    card.put("items", lines);
    return pack("progress", primary, replies, List.of(card),
        List.of(nav("作业进度", "/pages/class/homeworkProgress?classId=" + classId)));
  }

  private Map<String, Object> replyUnsubmitted(Long classId, String primary) {
    if (!STAFF_ROLES.contains(primary)) {
      return pack("unsubmitted", primary,
          List.of(textReply("未交名单仅对班务角色开放。")),
          List.of(), List.of());
    }
    TrainFollowTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .eq(TrainFollowTaskEntity::getStatus, "published")
        .orderByDesc(TrainFollowTaskEntity::getPublishedAt)
        .last("LIMIT 1"));
    if (task == null) {
      return pack("unsubmitted", primary,
          List.of(textReply("当前没有已发布的作业。")),
          List.of(),
          List.of(nav("作业进度", "/pages/class/homeworkProgress?classId=" + classId)));
    }
    List<TrainFollowTaskItemEntity> pending = itemMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
        .eq(TrainFollowTaskItemEntity::getTaskId, task.getId())
        .eq(TrainFollowTaskItemEntity::getDeleted, 0)
        .eq(TrainFollowTaskItemEntity::getStatus, "pending")
        .orderByAsc(TrainFollowTaskItemEntity::getUserName)
        .last("LIMIT 50"));
    List<String> names = pending.stream()
        .map(i -> StringUtils.hasText(i.getUserName()) ? i.getUserName() : ("#" + i.getPeopleId()))
        .collect(Collectors.toList());
    String copy = "【催交提醒】《" + task.getTitle() + "》尚未提交的同学请尽快完成：" + String.join("、", names);
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "unsubmitted_list");
    card.put("taskId", task.getId());
    card.put("taskTitle", task.getTitle());
    card.put("names", names);
    card.put("copyText", copy);

    List<Map<String, Object>> replies = new ArrayList<>();
    replies.add(textReply(names.isEmpty()
        ? ("《" + task.getTitle() + "》目前无人待交。")
        : ("《" + task.getTitle() + "》未交 " + names.size() + " 人（仅姓名，不含联系方式）。")));
    List<Map<String, Object>> actions = new ArrayList<>();
    actions.add(Map.of("type", "copy_text", "label", "复制催交文案", "payload", copy));
    actions.add(nav("作业进度", "/pages/class/homeworkProgress?classId=" + classId));
    return pack("unsubmitted", primary, replies, List.of(card), actions);
  }

  private Map<String, Object> replyChangeFaq(Long classId) {
    String faq = "调课说明：主讲/助教/班主任可在 App「申请调课」提交，不能直接改课表；"
        + "管理端审批通过后课表才会变更。可在「我的调课申请」查看进度。";
    return pack("change_faq", "member",
        List.of(textReply(faq)),
        List.of(),
        List.of(
            nav("申请调课", "/pages/class/scheduleChangeApply?classId=" + classId),
            nav("我的申请", "/pages/class/scheduleChangeMine?classId=" + classId)));
  }

  private Map<String, Object> replyPrecise(Long classId) {
    return pack("go_precise", "member",
        List.of(textReply("建议先看薄弱点与错题，再到 AI 精准学继续练。班级刷题请带上本班上下文。")),
        List.of(),
        List.of(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId)));
  }

  private Map<String, Object> replyDrawHint(Long classId, String primary) {
    if (!TEACHER_DRAW_ROLES.contains(primary)) {
      return pack("draw_hint", primary,
          List.of(textReply("智能抽题布置仅主讲/助教可用。")),
          List.of(), List.of());
    }
    return pack("draw_hint", primary,
        List.of(textReply("请打开布置页：选科目/知识点后使用「智能抽题」，确认后可存草稿或发布。助理不会静默改作业。")),
        List.of(),
        List.of(nav("去布置并抽题", "/pages/class/homeworkAssign?classId=" + classId)));
  }

  private Map<String, Object> replyHelp(String primary) {
    String tip = STAFF_ROLES.contains(primary)
        ? "我可以：今日安排、本班进度、谁没交、调课说明、引导抽题布置、跳转 AI 精准学。"
        : "我可以：今日安排、我的作业、调课说明、跳转 AI 精准学。考前请走提升-同步练习。";
    return pack("help", primary, List.of(textReply(tip)), List.of(), List.of());
  }

  private Map<String, Object> replyUnknown(String primary) {
    return pack("unknown", primary,
        List.of(textReply("暂未理解。可点快捷指令，或试：今日安排 / 我的作业 / 谁没交 / 该刷哪。")),
        List.of(), List.of());
  }

  private static String detectIntent(String text) {
    if (!StringUtils.hasText(text)) {
      return "help";
    }
    String t = text.toLowerCase(Locale.ROOT);
    if (containsAny(t, "课表", "今日", "今天", "安排", "干啥", "上课")) {
      return "schedule_today";
    }
    if (containsAny(t, "作业", "待办", "我的题")) {
      return "my_homework";
    }
    if (containsAny(t, "没交", "未交", "催交", "谁没")) {
      return "unsubmitted";
    }
    if (containsAny(t, "进度", "完成率", "汇总")) {
      return "progress";
    }
    if (containsAny(t, "调课")) {
      return "change_faq";
    }
    if (containsAny(t, "刷", "薄弱", "精准", "错题", "推荐")) {
      return "go_precise";
    }
    if (containsAny(t, "抽题", "布置")) {
      return "draw_hint";
    }
    if (containsAny(t, "帮助", "能做", "功能")) {
      return "help";
    }
    return "unknown";
  }

  private static boolean containsAny(String text, String... keys) {
    for (String k : keys) {
      if (text.contains(k)) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, Object> chip(String intent, String label) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("intent", intent);
    m.put("label", label);
    return m;
  }

  private static Map<String, Object> textReply(String content) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "text");
    m.put("content", content);
    return m;
  }

  private static Map<String, Object> nav(String label, String url) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", "navigate");
    m.put("label", label);
    m.put("url", url);
    return m;
  }

  private static Map<String, Object> pack(
      String intent,
      String primaryRole,
      List<Map<String, Object>> replies,
      List<Map<String, Object>> cards,
      List<Map<String, Object>> actions) {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("intent", intent);
    out.put("primaryRole", primaryRole);
    out.put("replies", replies);
    out.put("cards", cards);
    out.put("actions", actions);
    return out;
  }
}
