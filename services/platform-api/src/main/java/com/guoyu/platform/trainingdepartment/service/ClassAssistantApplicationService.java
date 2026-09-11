package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.coach.client.WeKnoraClient;
import com.guoyu.platform.app.coach.client.WeKnoraClient.WeKnoraChatResult;
import com.guoyu.platform.app.coach.config.WeKnoraProperties;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * AI 小培 / 班级助理（通道 D）—— 规则意图 + 只读 Tool + WeKnora 知识库问答。
 * <p>学员侧人格为「小培」：学习编排走 Tool（数字必真）；自由提问走 WeKnora「AI助教_智能问答」。
 * 不落会话表；写操作仅返回深链引导。
 */
@Service
public class ClassAssistantApplicationService {

  private static final Logger log = LoggerFactory.getLogger(ClassAssistantApplicationService.class);

  private static final String ROLE_STUDENT = "student";

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
  private final ClassReviewSyncApplicationService reviewSyncService;
  private final ReviewPlanApplicationService reviewPlanService;
  private final PreciseLearnDashboardApplicationService preciseLearnDashboardService;
  private final WeKnoraClient weKnoraClient;
  private final WeKnoraProperties weKnoraProperties;

  public ClassAssistantApplicationService(
      TrainClassRoleApplicationService roleService,
      TrainScheduleApplicationService scheduleService,
      TrainFollowTaskApplicationService taskService,
      TrainFollowTaskMapper taskMapper,
      TrainFollowTaskItemMapper itemMapper,
      TrainInfoClassMapper classMapper,
      ClassReviewSyncApplicationService reviewSyncService,
      ReviewPlanApplicationService reviewPlanService,
      PreciseLearnDashboardApplicationService preciseLearnDashboardService,
      WeKnoraClient weKnoraClient,
      WeKnoraProperties weKnoraProperties) {
    this.roleService = roleService;
    this.scheduleService = scheduleService;
    this.taskService = taskService;
    this.taskMapper = taskMapper;
    this.itemMapper = itemMapper;
    this.classMapper = classMapper;
    this.reviewSyncService = reviewSyncService;
    this.reviewPlanService = reviewPlanService;
    this.preciseLearnDashboardService = preciseLearnDashboardService;
    this.weKnoraClient = weKnoraClient;
    this.weKnoraProperties = weKnoraProperties;
  }

  /** 可能触发同步学 overview 懒创建计划，不可标 readOnly。 */
  public Map<String, Object> bootstrap(Long platformUserId, Long classId) {
    String primary = roleService.primaryRoleOfMember(platformUserId, classId);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    boolean studentMode = isStudentMode(primary);

    List<Map<String, Object>> chips = new ArrayList<>();
    if (studentMode) {
      chips.add(chip("what_now", "现在做什么"));
      chips.add(chip("reward_now", "完成得什么"));
      chips.add(chip("reward_future", "未来得什么"));
      chips.add(chip("learn_detail", "学习详情"));
      chips.add(chip("go_sync", "该刷哪"));
      chips.add(chip("ask_kb", "知识问答"));
      chips.add(chip("schedule_today", "今日安排"));
      chips.add(chip("my_homework", "我的作业"));
      chips.add(chip("help", "能做什么"));
    } else {
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
    }

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("classId", classId);
    out.put("classTitle", clazz == null ? null : clazz.getTitle());
    out.put("primaryRole", primary);
    out.put("persona", studentMode ? "xiaopei" : "class_assistant");
    out.put("chips", chips);

    SyncSnapshot sync = studentMode ? loadSyncSnapshot(platformUserId, classId) : null;
    if (studentMode) {
      out.put("welcome", "你好，我是小培～进度类问题我查真实数据；其它知识问题我会查「"
          + weKnoraProperties.xiaopeiKnowledgeBaseName() + "」知识库来答。");
      out.put("hint", "点快捷指令看今日任务/收益；随便问问考点/概念会走知识库。练题请点「该刷哪」，我不在聊天里答题。");
      out.put("summary", buildSummaryCard(sync, clazz));
      out.put("kbEnabled", weKnoraProperties.isApiReady());
      out.put("kbName", weKnoraProperties.xiaopeiKnowledgeBaseName());
    } else {
      out.put("welcome", "我是本班 AI 助理（规则版）。可点下方快捷指令，或输入「今日安排 / 我的作业 / 谁没交」。"
          + "班务数字来自系统查询；不替代批改与认定考试。");
      out.put("hint", "学员侧请用「AI小培」；深度刷题可用「AI精准学」。");
      out.put("summary", null);
      out.put("kbEnabled", false);
      out.put("kbName", null);
    }
    return out;
  }

  public Map<String, Object> chat(Long platformUserId, Long classId, Map<String, Object> body) {
    String primary = roleService.primaryRoleOfMember(platformUserId, classId);
    boolean studentMode = isStudentMode(primary);
    String text = body == null || body.get("text") == null ? "" : String.valueOf(body.get("text")).trim();
    String intent = body != null && body.get("intent") != null
        ? String.valueOf(body.get("intent")).trim()
        : detectIntent(text, studentMode);

    // 学员自由输入未命中规则意图 → 走知识库
    if ("unknown".equals(intent) && studentMode && StringUtils.hasText(text)) {
      intent = "ask_kb";
    }

    return switch (intent) {
      case "what_now" -> replyWhatNow(platformUserId, classId, primary);
      case "reward_now" -> replyRewardNow(platformUserId, classId, primary);
      case "reward_future" -> replyRewardFuture(platformUserId, classId, primary);
      case "learn_detail" -> replyLearnDetail(platformUserId, classId, primary);
      case "go_sync" -> replyGoSync(platformUserId, classId, primary);
      case "ask_kb" -> replyAskKb(classId, primary, text, body);
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

  // ------------------------------------------------------------------
  // 小培意图
  // ------------------------------------------------------------------

  private Map<String, Object> replyWhatNow(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();

    if (!sync.configured()) {
      replies.add(textReply("本班还没开通同步学计划，我没法按日进度给你派活。你可以先看今日课表和课堂作业；开通后我就能告诉你今天该刷多少题。"));
      actions.add(nav("今日安排", "/pages/class/schedule?classId=" + classId));
      actions.add(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId));
      return pack("what_now", primary, replies, cards, actions);
    }
    if (!sync.entryVisible()) {
      String reason = StringUtils.hasText(sync.reasonMessage())
          ? sync.reasonMessage()
          : "同步学入口暂不可用";
      replies.add(textReply("同步学已配置，但现在进不去：" + reason + "。在此之前，可以先完成课堂作业或查看课表。"));
      actions.add(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId));
      actions.add(nav("今日安排", "/pages/class/schedule?classId=" + classId));
      return pack("what_now", primary, replies, cards, actions);
    }
    if (sync.overview() == null) {
      replies.add(textReply("同步学已开通，但暂时读不到今日任务。请稍后重试，或直接打开同步练习。"));
      actions.add(syncNav(sync));
      return pack("what_now", primary, replies, cards, actions);
    }

    Map<String, Object> ov = sync.overview();
    String stageName = str(ov.get("stageName"), "当前阶段");
    int dayIndex = toInt(ov.get("dayIndex"), 0);
    int totalDays = toInt(ov.get("totalDays"), 0);
    @SuppressWarnings("unchecked")
    Map<String, Object> today = ov.get("today") instanceof Map ? (Map<String, Object>) ov.get("today") : Map.of();
    int newTarget = toInt(today.get("newTarget"), 0);
    int newDone = toInt(today.get("newDone"), 0);
    int reviewTarget = toInt(today.get("reviewTarget"), 0);
    int reviewDone = toInt(today.get("reviewDone"), 0);
    int newLeft = Math.max(0, newTarget - newDone);
    int reviewLeft = Math.max(0, reviewTarget - reviewDone);

    StringBuilder sb = new StringBuilder();
    sb.append("你现在在同步学「").append(stageName).append("」第 ")
        .append(dayIndex).append("/").append(totalDays).append(" 天。");
    if (newLeft + reviewLeft <= 0) {
      sb.append("今日新题与复盘都已完成");
      Object mock = today.get("mock");
      if (mock instanceof Map<?, ?> m && m.get("status") != null) {
        sb.append("；今日还有模考任务（状态：").append(m.get("status")).append("）");
      }
      sb.append("。可以点下方继续练，或明天再来。");
    } else {
      sb.append("今天还差：新题 ").append(newLeft).append(" 道");
      if (reviewLeft > 0) {
        sb.append("、错题复盘 ").append(reviewLeft).append(" 道");
      }
      sb.append("。建议先完成今日任务。");
    }
    replies.add(textReply(sb.toString()));

    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "summary_card");
    card.put("stageName", stageName);
    card.put("dayIndex", dayIndex);
    card.put("totalDays", totalDays);
    card.put("newDone", newDone);
    card.put("newTarget", newTarget);
    card.put("reviewDone", reviewDone);
    card.put("reviewTarget", reviewTarget);
    cards.add(card);

    actions.add(syncNav(sync));
    return pack("what_now", primary, replies, cards, actions);
  }

  private Map<String, Object> replyRewardNow(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    if (sync.overview() == null) {
      replies.add(textReply(rewardNowFallback()));
      if (sync.entryVisible()) {
        actions.add(syncNav(sync));
      } else {
        actions.add(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId));
      }
      return pack("reward_now", primary, replies, List.of(), actions);
    }

    Map<String, Object> ov = sync.overview();
    int stage = toInt(ov.get("stage"), 1);
    String stageName = str(ov.get("stageName"), "当前阶段");
    @SuppressWarnings("unchecked")
    Map<String, Object> today = ov.get("today") instanceof Map ? (Map<String, Object>) ov.get("today") : Map.of();
    int newLeft = Math.max(0, toInt(today.get("newTarget"), 0) - toInt(today.get("newDone"), 0));
    int reviewLeft = Math.max(0, toInt(today.get("reviewTarget"), 0) - toInt(today.get("reviewDone"), 0));

    replies.add(textReply(rewardNowByStage(stage, stageName, newLeft, reviewLeft)));
    if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    return pack("reward_now", primary, replies, List.of(), actions);
  }

  private Map<String, Object> replyRewardFuture(Long platformUserId, Long classId, String primary) {
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    String classTitle = clazz == null ? "本班" : Objects.toString(clazz.getTitle(), "本班");
    Integer level = clazz == null ? null : clazz.getWorkTypeLevel();
    String examDate = sync.overview() == null ? null : str(sync.overview().get("examDate"), null);
    String examTitle = sync.examTitle();

    StringBuilder sb = new StringBuilder();
    sb.append("学完这条路径，目标是顺利参加认定考试并拿到相应证书/资格。");
    sb.append("班级：").append(classTitle).append("。");
    if (level != null && level > 0) {
      sb.append("工种等级：").append(level).append(" 级。");
    }
    if (StringUtils.hasText(examTitle)) {
      sb.append("同步学对应考试计划：").append(examTitle).append("。");
    }
    if (StringUtils.hasText(examDate)) {
      sb.append("计划考期：").append(examDate).append("。");
    } else {
      sb.append("具体考期以班级/教务通知为准。");
    }
    sb.append("小培只读展示路径节点，认定结果以官方考试与发证为准。");
    replies.add(textReply(sb.toString()));

    if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    return pack("reward_future", primary, replies, List.of(), actions);
  }

  private Map<String, Object> replyLearnDetail(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    StringBuilder sb = new StringBuilder();
    if (sync.overview() != null) {
      Map<String, Object> ov = sync.overview();
      @SuppressWarnings("unchecked")
      Map<String, Object> progress = ov.get("progress") instanceof Map
          ? (Map<String, Object>) ov.get("progress") : Map.of();
      @SuppressWarnings("unchecked")
      Map<String, Object> wrong = ov.get("wrongStats") instanceof Map
          ? (Map<String, Object>) ov.get("wrongStats") : Map.of();
      sb.append("同步学进度：已覆盖 ")
          .append(progress.getOrDefault("doneQuestions", 0)).append("/")
          .append(progress.getOrDefault("targetQuestions", 0))
          .append(" 题（覆盖率 ").append(progress.getOrDefault("coveragePercent", 0)).append("%），")
          .append("正确率 ").append(progress.getOrDefault("accuracy", 0)).append("%。");
      sb.append("错题卡：红 ").append(wrong.getOrDefault("red", 0))
          .append(" / 黄 ").append(wrong.getOrDefault("yellow", 0))
          .append(" / 已清 ").append(wrong.getOrDefault("green", 0))
          .append("，星标 ").append(wrong.getOrDefault("star", 0)).append("。");
      sb.append("阶段「").append(str(ov.get("stageName"), "-")).append("」第 ")
          .append(ov.getOrDefault("dayIndex", 0)).append("/")
          .append(ov.getOrDefault("totalDays", 0)).append(" 天。");
    } else if (sync.configured()) {
      sb.append("同步学已配置，但还读不到你的进度明细（可能尚未开练或入口受限）。");
    } else {
      sb.append("本班暂无同步学进度。");
    }

    // 精准学错题摘要（只读）
    try {
      Map<String, Object> dash = preciseLearnDashboardService.dashboard(platformUserId, classId);
      @SuppressWarnings("unchecked")
      Map<String, Object> wrongOverview = dash.get("wrongOverview") instanceof Map
          ? (Map<String, Object>) dash.get("wrongOverview") : null;
      if (wrongOverview != null) {
        @SuppressWarnings("unchecked")
        Map<String, Object> special = wrongOverview.get("special") instanceof Map
            ? (Map<String, Object>) wrongOverview.get("special") : Map.of();
        int book = toInt(special.get("wrongBookCount"), 0);
        if (book <= 0) {
          book = toInt(special.get("totalWrong"), 0);
        }
        if (book > 0) {
          sb.append(" 专项错题本约 ").append(book).append(" 题");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> priority = wrongOverview.get("priority") instanceof Map
            ? (Map<String, Object>) wrongOverview.get("priority") : null;
        if (priority != null && priority.get("hint") != null
            && StringUtils.hasText(String.valueOf(priority.get("hint")))) {
          sb.append("；").append(priority.get("hint"));
        }
        if (book > 0 || (priority != null && priority.get("hint") != null)) {
          sb.append("。");
        }
      }
    } catch (Exception e) {
      log.debug("[小培] 精准学摘要跳过: {}", e.getMessage());
    }

    if (sb.length() == 0) {
      sb.append("暂时没有可展示的学情，完成几轮练习后再来问我。");
    }
    replies.add(textReply(sb.toString()));

    if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    return pack("learn_detail", primary, replies, cards, actions);
  }

  private Map<String, Object> replyGoSync(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    if (!sync.configured()) {
      replies.add(textReply("本班还没开通同步学，没法直接带你去日练。可先做课堂作业，或去 AI 精准学看错题薄弱点；开通同步学后「该刷哪」会优先带你去同步练。"));
      actions.add(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId));
      actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
      return pack("go_sync", primary, replies, List.of(), actions);
    }
    if (!sync.entryVisible()) {
      replies.add(textReply("同步学入口暂不可用："
          + (StringUtils.hasText(sync.reasonMessage()) ? sync.reasonMessage() : "请联系教务")
          + "。在此期间可用 AI 精准学练薄弱点。"));
      actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
      return pack("go_sync", primary, replies, List.of(), actions);
    }

    String tip = "接下来建议继续同步学日练";
    if (sync.overview() != null) {
      tip += "（" + str(sync.overview().get("stageName"), "当前阶段")
          + " 第 " + sync.overview().getOrDefault("dayIndex", "-") + " 天）";
    }
    tip += "。点下面打开同步练习，我不在聊天里出题。";
    replies.add(textReply(tip));
    actions.add(syncNav(sync));
    return pack("go_sync", primary, replies, List.of(), actions);
  }

  /**
   * 自由知识问答 → WeKnora「AI助教_智能问答」（或配置的 KB）。
   * 进度数字类问题仍应走 Tool 意图；本路径不做答卷、不改写个人学情数字。
   */
  private Map<String, Object> replyAskKb(
      Long classId, String primary, String text, Map<String, Object> body) {
    String kbName = weKnoraProperties.xiaopeiKnowledgeBaseName();
    if (!StringUtils.hasText(text)) {
      return pack("ask_kb", primary,
          List.of(textReply("把想问的考点、概念或复习问题发给我，我会查询知识库「" + kbName + "」来回答。"
              + "若要问今天练什么/进度，请点「现在做什么」或「学习详情」。")),
          List.of(), List.of());
    }
    if (!weKnoraProperties.isApiReady()) {
      return pack("ask_kb", primary,
          List.of(textReply("知识库问答暂未开通（WeKnora API Key 未配置）。进度类问题仍可用快捷指令。")),
          List.of(), List.of());
    }

    try {
      String sessionId = body != null && body.get("sessionId") != null
          ? String.valueOf(body.get("sessionId")).trim() : "";
      if (!StringUtils.hasText(sessionId)) {
        sessionId = weKnoraClient.createSession(
            "AI小培 · 班级" + classId,
            "学员学习伙伴，知识库=" + kbName);
      }

      List<String> kbIds = resolveXiaopeiKbIds();
      String agentId = weKnoraProperties.resolveXiaopeiAgentId();
      String query = """
          你是学习伙伴「小培」。请基于知识库回答学员问题，语气简洁友好。
          不要编造学员个人进度数字（天数、正确率、错题数等）；若问题涉及个人进度，请提醒学员点「现在做什么」或「学习详情」。
          学员问题：%s
          """.formatted(text);

      WeKnoraChatResult result = weKnoraClient.knowledgeChat(
          sessionId, query, kbIds, agentId, List.of());

      List<Map<String, Object>> replies = new ArrayList<>();
      replies.add(textReply(result.answer()));
      List<Map<String, Object>> cards = new ArrayList<>();
      if (result.references() != null && !result.references().isEmpty()) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("type", "kb_references");
        card.put("items", result.references().stream().limit(5).map(ref -> {
          Map<String, Object> line = new LinkedHashMap<>();
          line.put("title", Objects.toString(ref.get("title"), ""));
          line.put("filename", Objects.toString(ref.get("filename"), ""));
          line.put("score", ref.get("score"));
          return line;
        }).toList());
        cards.add(card);
      }
      Map<String, Object> out = pack("ask_kb", primary, replies, cards, List.of());
      out.put("sessionId", sessionId);
      out.put("kbName", kbName);
      out.put("source", "weknora");
      return out;
    } catch (Exception e) {
      log.warn("[小培] WeKnora 问答失败 classId={}: {}", classId, e.getMessage());
      return pack("ask_kb", primary,
          List.of(textReply("知识库暂时繁忙：" + shortenErr(e.getMessage())
              + "。请稍后再试；进度问题可用下方快捷指令。")),
          List.of(), List.of());
    }
  }

  private List<String> resolveXiaopeiKbIds() {
    List<String> ids = weKnoraProperties.xiaopeiKnowledgeBaseIdList();
    if (!ids.isEmpty()) {
      return ids;
    }
    String name = weKnoraProperties.xiaopeiKnowledgeBaseName();
    if (!StringUtils.hasText(name)) {
      return List.of();
    }
    return weKnoraClient.resolveKnowledgeBaseIdsByName(name);
  }

  private static String shortenErr(String msg) {
    if (!StringUtils.hasText(msg)) {
      return "请稍后重试";
    }
    String s = msg.trim();
    return s.length() > 80 ? s.substring(0, 80) + "…" : s;
  }

  // ------------------------------------------------------------------
  // 原班务意图
  // ------------------------------------------------------------------

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
          List.of(textReply("进度汇总仅对班长/班主任/教务/主讲开放。你可查看自己的课堂作业，或问小培「学习详情」。")),
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
    replies.add(textReply(lines.isEmpty() ? "本班暂无作业任务。" : ("最近 " + lines.size() + " 条作业进度：")));
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "progress_list");
    card.put("items", lines);
    return pack("progress", primary, replies, List.of(card),
        List.of(nav("作业进度", "/pages/class/homeworkProgress?classId=" + classId)));
  }

  private Map<String, Object> replyUnsubmitted(Long classId, String primary) {
    if (!STAFF_ROLES.contains(primary)) {
      return pack("unsubmitted", primary,
          List.of(textReply("「谁没交」仅班务角色可用。")),
          List.of(), List.of());
    }
    TrainFollowTaskEntity task = taskMapper.selectOne(new LambdaQueryWrapper<TrainFollowTaskEntity>()
        .eq(TrainFollowTaskEntity::getClassId, classId)
        .eq(TrainFollowTaskEntity::getDeleted, 0)
        .eq(TrainFollowTaskEntity::getStatus, "published")
        .orderByDesc(TrainFollowTaskEntity::getCreatedAt)
        .last("LIMIT 1"));
    if (task == null) {
      return pack("unsubmitted", primary,
          List.of(textReply("暂无进行中的已发布作业。")),
          List.of(),
          List.of(nav("作业进度", "/pages/class/homeworkProgress?classId=" + classId)));
    }
    List<TrainFollowTaskItemEntity> pending = itemMapper.selectList(new LambdaQueryWrapper<TrainFollowTaskItemEntity>()
        .eq(TrainFollowTaskItemEntity::getTaskId, task.getId())
        .eq(TrainFollowTaskItemEntity::getDeleted, 0)
        .notIn(TrainFollowTaskItemEntity::getStatus, "submitted", "graded")
        .last("LIMIT 40"));
    List<String> names = pending.stream()
        .map(i -> StringUtils.hasText(i.getUserName()) ? i.getUserName() : ("#" + i.getPeopleId()))
        .distinct()
        .limit(20)
        .collect(Collectors.toList());
    String copy = names.isEmpty()
        ? ("《" + task.getTitle() + "》目前没有未交名单。")
        : ("同学们好，请尽快提交课堂作业《" + task.getTitle() + "》。未交：" + String.join("、", names));
    List<Map<String, Object>> replies = new ArrayList<>();
    replies.add(textReply(names.isEmpty()
        ? ("《" + task.getTitle() + "》暂无未交。")
        : ("《" + task.getTitle() + "》未交 " + names.size() + " 人（仅姓名，不含联系方式）。")));
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "unsubmitted_list");
    card.put("names", names);
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
    String tip = isStudentMode(primary)
        ? "我是小培：现在做什么、完成/未来收益、学习详情、该刷哪（同步学）；"
            + "其它考点/概念可直接问，我会查知识库「" + weKnoraProperties.xiaopeiKnowledgeBaseName() + "」。"
            + "不会在聊天里直接答题。"
        : "我可以：今日安排、本班进度、谁没交、调课说明、引导抽题布置、跳转 AI 精准学。";
    return pack("help", primary, List.of(textReply(tip)), List.of(), List.of());
  }

  private Map<String, Object> replyUnknown(String primary) {
    String tip = isStudentMode(primary)
        ? "暂未理解。可点快捷指令，或直接输入知识问题（会查知识库）；也可试：现在做什么 / 该刷哪。"
        : "暂未理解。可点快捷指令，或试：今日安排 / 我的作业 / 谁没交 / 该刷哪。";
    return pack("unknown", primary, List.of(textReply(tip)), List.of(), List.of());
  }

  // ------------------------------------------------------------------
  // Sync Tool
  // ------------------------------------------------------------------

  private SyncSnapshot loadSyncSnapshot(Long platformUserId, Long classId) {
    Map<String, Object> resolve;
    try {
      resolve = reviewSyncService.resolveForAppMember(classId, platformUserId);
    } catch (Exception e) {
      log.warn("[小培] resolve 同步学失败 classId={}: {}", classId, e.getMessage());
      return SyncSnapshot.empty();
    }
    boolean configured = Boolean.TRUE.equals(resolve.get("configured"));
    boolean entryVisible = Boolean.TRUE.equals(resolve.get("entryVisible"));
    Long examId = toLong(resolve.get("examId"));
    String examTitle = str(resolve.get("examTitle"), null);
    String reasonMessage = str(resolve.get("reasonMessage"), null);
    Map<String, Object> overview = null;
    if (configured && examId != null && examId > 0 && entryVisible) {
      try {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
          tenantId = TenantContextHolder.DEFAULT_TENANT_ID;
        }
        overview = reviewPlanService.overview(platformUserId, examId, tenantId);
      } catch (Exception e) {
        log.warn("[小培] overview 失败 classId={} examId={}: {}", classId, examId, e.getMessage());
      }
    }
    return new SyncSnapshot(configured, entryVisible, examId, examTitle, reasonMessage, overview);
  }

  private Map<String, Object> buildSummaryCard(SyncSnapshot sync, TrainInfoClassEntity clazz) {
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "summary_card");
    card.put("classTitle", clazz == null ? null : clazz.getTitle());
    if (sync == null || !sync.configured()) {
      card.put("syncStatus", "not_configured");
      card.put("title", "同步学未开通");
      card.put("subtitle", "可先看课表与作业；开通后我会按日进度提醒你");
      return card;
    }
    if (!sync.entryVisible()) {
      card.put("syncStatus", "blocked");
      card.put("title", "同步学暂不可进");
      card.put("subtitle", StringUtils.hasText(sync.reasonMessage()) ? sync.reasonMessage() : "请联系教务");
      return card;
    }
    if (sync.overview() == null) {
      card.put("syncStatus", "ready");
      card.put("title", "同步学已开通");
      card.put("subtitle", "点「现在做什么」看今日任务");
      return card;
    }
    Map<String, Object> ov = sync.overview();
    @SuppressWarnings("unchecked")
    Map<String, Object> today = ov.get("today") instanceof Map ? (Map<String, Object>) ov.get("today") : Map.of();
    card.put("syncStatus", "active");
    card.put("title", str(ov.get("stageName"), "同步学") + " · 第 "
        + ov.getOrDefault("dayIndex", "-") + "/" + ov.getOrDefault("totalDays", "-") + " 天");
    card.put("subtitle", "今日新题 " + today.getOrDefault("newDone", 0) + "/"
        + today.getOrDefault("newTarget", 0)
        + " · 复盘 " + today.getOrDefault("reviewDone", 0) + "/"
        + today.getOrDefault("reviewTarget", 0));
    card.put("stageName", ov.get("stageName"));
    card.put("dayIndex", ov.get("dayIndex"));
    card.put("totalDays", ov.get("totalDays"));
    card.put("newDone", today.get("newDone"));
    card.put("newTarget", today.get("newTarget"));
    card.put("reviewDone", today.get("reviewDone"));
    card.put("reviewTarget", today.get("reviewTarget"));
    return card;
  }

  private static String rewardNowByStage(int stage, String stageName, int newLeft, int reviewLeft) {
    String base = switch (stage) {
      case 1 -> "做完今日「" + stageName + "」任务，等于把必做池再铺厚一层：覆盖率上升、生题变熟题，后面定向爆破会轻松很多。";
      case 2 -> "做完今日「" + stageName + "」任务，重点是把红黄错题卡压下去：即时收益是薄弱点变少、星标题被清算。";
      default -> "做完今日「" + stageName + "」任务，等于多一次仿真手感：估分更贴近考场节奏，查漏补缺更有数。";
    };
    if (newLeft + reviewLeft > 0) {
      base += " 你今天还剩新题 " + newLeft + "、复盘 " + reviewLeft + "，做完就能落袋为安。";
    } else {
      base += " 今日清单已清完，保持节奏即可。";
    }
    return base;
  }

  private static String rewardNowFallback() {
    return "完成今日学习的即时收益：把该覆盖的知识点练过、把错题复盘掉，进度条和正确率会如实上涨。"
        + "开通同步学后，我可以按你当前阶段说得更具体。";
  }

  private static Map<String, Object> syncNav(SyncSnapshot sync) {
    if (sync.examId() == null) {
      return nav("打开同步练习", "/pages/exam/syncHome");
    }
    return nav("打开同步练习", "/pages/exam/syncHome?exam_id=" + sync.examId());
  }

  private static boolean isStudentMode(String primary) {
    return ROLE_STUDENT.equals(primary) || TrainClassRoleApplicationService.MONITOR.equals(primary);
  }

  private static String detectIntent(String text, boolean studentMode) {
    if (!StringUtils.hasText(text)) {
      return "help";
    }
    String t = text.toLowerCase(Locale.ROOT);

    if (studentMode) {
      if (containsAny(t, "现在做", "该干", "干啥", "干嘛", "做什么", "今日任务", "今天练")) {
        return "what_now";
      }
      if (containsAny(t, "做完", "有什么用", "有啥用", "收益", "完成得")) {
        return "reward_now";
      }
      if (containsAny(t, "未来", "学完", "证书", "认定", "考期", "能拿到")) {
        return "reward_future";
      }
      if (containsAny(t, "学得", "学习详情", "怎么样了", "学情", "进度如何")) {
        return "learn_detail";
      }
      if (containsAny(t, "该刷", "刷哪", "接下来刷", "去练", "同步学", "同步练")) {
        return "go_sync";
      }
      if (containsAny(t, "知识库", "知识问答", "什么是", "怎么理解", "讲一下", "解释一下")) {
        return "ask_kb";
      }
      if (containsAny(t, "精准")) {
        return "go_precise";
      }
    }

    if (containsAny(t, "课表", "今日安排", "上课安排")) {
      return "schedule_today";
    }
    if (containsAny(t, "作业", "待办", "我的题")) {
      return "my_homework";
    }
    if (containsAny(t, "没交", "未交", "催交", "谁没")) {
      return "unsubmitted";
    }
    if (containsAny(t, "进度", "完成率", "汇总") && !studentMode) {
      return "progress";
    }
    if (containsAny(t, "调课")) {
      return "change_faq";
    }
    if (containsAny(t, "刷", "薄弱", "精准", "错题", "推荐")) {
      return studentMode ? "go_sync" : "go_precise";
    }
    if (containsAny(t, "抽题", "布置")) {
      return "draw_hint";
    }
    if (containsAny(t, "帮助", "能做", "功能")) {
      return "help";
    }
    // 学员口语兜底：今天/安排 仍可落到课表；干啥已在上方
    if (studentMode && containsAny(t, "今天", "安排", "上课")) {
      return "schedule_today";
    }
    if (!studentMode && containsAny(t, "课表", "今日", "今天", "安排", "干啥", "上课")) {
      return "schedule_today";
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

  private static String str(Object v, String dft) {
    if (v == null) {
      return dft;
    }
    String s = String.valueOf(v).trim();
    return s.isEmpty() || "null".equals(s) ? dft : s;
  }

  private static int toInt(Object v, int dft) {
    if (v == null) {
      return dft;
    }
    if (v instanceof Number n) {
      return n.intValue();
    }
    try {
      return Integer.parseInt(String.valueOf(v).trim());
    } catch (Exception e) {
      return dft;
    }
  }

  private static Long toLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      String s = String.valueOf(v).trim();
      if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
        return null;
      }
      return Long.parseLong(s);
    } catch (Exception e) {
      return null;
    }
  }

  private record SyncSnapshot(
      boolean configured,
      boolean entryVisible,
      Long examId,
      String examTitle,
      String reasonMessage,
      Map<String, Object> overview) {
    static SyncSnapshot empty() {
      return new SyncSnapshot(false, false, null, null, null, null);
    }
  }
}
