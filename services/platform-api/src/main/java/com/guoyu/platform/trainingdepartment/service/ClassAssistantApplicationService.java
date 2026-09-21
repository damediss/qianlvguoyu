package com.guoyu.platform.trainingdepartment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.app.coach.client.WeKnoraClient;
import com.guoyu.platform.app.coach.client.WeKnoraClient.WeKnoraChatResult;
import com.guoyu.platform.app.coach.config.WeKnoraProperties;
import com.guoyu.platform.app.exam.service.QuestionResolveAssistService;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.exam.entity.ExamWorkTypeEntity;
import com.guoyu.platform.exam.mapper.ExamWorkTypeMapper;
import com.guoyu.platform.exam.reviewsync.service.ReviewPlanApplicationService;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainFollowTaskItemEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainInfoClassEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpPeopleSelfEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainSignUpSelfEntity;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskItemMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainFollowTaskMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainInfoClassMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainPeopleListMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpPeopleSelfMapper;
import com.guoyu.platform.trainingdepartment.mapper.TrainSignUpSelfMapper;
import com.guoyu.platform.trainingdepartment.support.SignUpLevelSupport;
import com.guoyu.platform.trainingdepartment.support.SignUpPeopleDefaultTemplatePackService;
import com.guoyu.platform.trainingdepartment.vo.FollowStudentTaskVO;
import java.math.BigDecimal;
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
  private final QuestionResolveAssistService questionResolveAssistService;
  private final ExamWorkTypeMapper examWorkTypeMapper;
  private final TrainPeopleListMapper peopleListMapper;
  private final TrainSignUpSelfMapper signUpSelfMapper;
  private final TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper;

  /** 单次 bootstrap/chat 请求内复用，避免重复 resolve/overview/dashboard */
  private final ThreadLocal<Map<String, SyncSnapshot>> syncCache = ThreadLocal.withInitial(LinkedHashMap::new);
  private final ThreadLocal<Map<String, ReadinessMetrics>> metricsCache = ThreadLocal.withInitial(LinkedHashMap::new);
  private final ThreadLocal<Map<String, StudentClassProfile>> profileCache = ThreadLocal.withInitial(LinkedHashMap::new);

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
      WeKnoraProperties weKnoraProperties,
      QuestionResolveAssistService questionResolveAssistService,
      ExamWorkTypeMapper examWorkTypeMapper,
      TrainPeopleListMapper peopleListMapper,
      TrainSignUpSelfMapper signUpSelfMapper,
      TrainSignUpPeopleSelfMapper signUpPeopleSelfMapper) {
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
    this.questionResolveAssistService = questionResolveAssistService;
    this.examWorkTypeMapper = examWorkTypeMapper;
    this.peopleListMapper = peopleListMapper;
    this.signUpSelfMapper = signUpSelfMapper;
    this.signUpPeopleSelfMapper = signUpPeopleSelfMapper;
  }

  /** 可能触发同步学 overview 懒创建计划，不可标 readOnly。 */
  public Map<String, Object> bootstrap(Long platformUserId, Long classId) {
    try {
      return doBootstrap(platformUserId, classId);
    } finally {
      clearRequestCache();
    }
  }

  private Map<String, Object> doBootstrap(Long platformUserId, Long classId) {
    String primary = roleService.primaryRoleOfMember(platformUserId, classId);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    boolean studentMode = isStudentMode(primary);

    List<Map<String, Object>> chips = new ArrayList<>();
    if (studentMode) {
      chips.add(chip("exam_readiness", "能不能考过"));
      chips.add(chip("what_now", "现在做什么"));
      chips.add(chip("go_sync", "该刷哪"));
      // 文案是「我有多少错题」，intent 仍用 learn_detail，兼容未部署 wrong_stats 的线上包
      chips.add(chip("learn_detail", "我有多少错题"));
      chips.add(chip("learn_detail", "学习详情"));
      chips.add(chip("my_homework", "作业与模考"));
      chips.add(chip("weak_points", "我的薄弱点"));
      chips.add(chip("explain_question", "讲讲这题"));
      chips.add(chip("reward_future", "考证路径"));
      chips.add(chip("ask_kb", "知识问答"));
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
      ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
      StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
      Map<String, Object> learning = buildLearningSnapshot(platformUserId, classId, sync, cert, profile);
      out.put("welcome", "你好，我是小培。上方是你的学情快照；也可直接问能不能考过、作业模考、考什么证。");
      out.put("hint", "点「快捷」或点学情卡看详情；数字只来自真实刷题与班级任务记录。");
      out.put("summary", null);
      out.put("learningSnapshot", learning);
      out.put("kbEnabled", weKnoraProperties.isApiReady());
      out.put("kbName", weKnoraProperties.xiaopeiKnowledgeBaseName());
      out.put("certContext", cert.toMap());
      out.put("classProfile", profile.toMap());
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
    try {
      return doChat(platformUserId, classId, body);
    } finally {
      clearRequestCache();
    }
  }

  private Map<String, Object> doChat(Long platformUserId, Long classId, Map<String, Object> body) {
    String primary = roleService.primaryRoleOfMember(platformUserId, classId);
    boolean studentMode = isStudentMode(primary);
    String text = body == null || body.get("text") == null ? "" : String.valueOf(body.get("text")).trim();
    // 展示给用户的文案也可能在 intent 旁的 label 里（兼容前端只传 intent 的 chips）
    String intentHint = body != null && body.get("intent") != null
        ? String.valueOf(body.get("intent")).trim() : "";
    String intent = StringUtils.hasText(intentHint) ? intentHint : detectIntent(text, studentMode);

    // 硬纠正：能力介绍类问句绝不能走 what_now（含「你都可以做什么」）
    if (looksLikeCapabilityQuestion(text) || looksLikeCapabilityQuestion(intentHint)) {
      intent = "help";
    }
    // 若前端误传 what_now，但原文是能力问句，已在上方纠正；再防空 text + 错误 intent
    if ("what_now".equals(intent) && StringUtils.hasText(text) && looksLikeCapabilityQuestion(text)) {
      intent = "help";
    }
    // 硬纠正：问错题数量/情况绝不能走 go_sync / what_now / ask_kb（如「我有多少错题」）
    if (studentMode && (looksLikeWrongStatsQuestion(text) || looksLikeWrongStatsQuestion(intentHint))) {
      intent = containsAny(
          (StringUtils.hasText(text) ? text : intentHint).toLowerCase(Locale.ROOT), "薄弱")
          ? "weak_points" : "wrong_stats";
    }

    // 场景页带 questionId + 讲题口语 → explain（勿用宽泛「这题/讲解」，避免误伤）
    if (studentMode && body != null && body.get("questionId") != null
        && ("explain_question".equals(intent) || looksLikeExplainCurrentQuestion(text))) {
      intent = "explain_question";
    }

    // 粘贴长题干 → 讲题/知识库
    if (studentMode && "unknown".equals(intent) && looksLikePastedQuestion(text)) {
      intent = body != null && body.get("questionId") != null ? "explain_question" : "ask_kb";
    }

    // 学员自由输入未命中规则意图 → 走知识库
    if ("unknown".equals(intent) && studentMode && StringUtils.hasText(text)) {
      intent = "ask_kb";
    }

    // 最终锁定：错题数量问句绝不能进知识库
    if (studentMode && looksLikeWrongStatsQuestion(text)) {
      intent = "wrong_stats";
    }
    // 最终锁定：作业/模考/完成了哪些任务 → 真数据，禁止进知识库拒答
    if (studentMode && looksLikeHomeworkTaskQuestion(text)) {
      intent = "my_homework";
    }
    // 口语随意问：软分流到 Tool / 知识库，减少「听不懂就拒答」
    if (studentMode && StringUtils.hasText(text)
        && ("ask_kb".equals(intent) || "unknown".equals(intent) || "help".equals(intent)
            || "reward_now".equals(intent) || "reward_future".equals(intent))) {
      String soft = softRouteStudentFreeForm(text);
      if (StringUtils.hasText(soft)) {
        intent = soft;
      }
    }
    // 最终锁定：做题页问「讲这题」必须走当前题，禁止进知识库讲成别的题
    if (studentMode && body != null && body.get("questionId") != null
        && looksLikeExplainCurrentQuestion(text)) {
      intent = "explain_question";
    }
    // 考证类：走班务真数据 + KB；禁止被「有什么用」锁在 reward_now
    if (studentMode && looksLikeCertQuestion(text)) {
      intent = "ask_kb";
    }

    log.info("[小培] chat classId={} intent={} text={}", classId, intent,
        text.length() > 40 ? text.substring(0, 40) + "…" : text);

    return switch (intent) {
      case "what_now" -> replyWhatNow(platformUserId, classId, primary);
      case "reward_now" -> replyRewardNow(platformUserId, classId, primary);
      case "reward_future" -> replyRewardFuture(platformUserId, classId, primary);
      case "learn_detail" -> replyLearnDetail(platformUserId, classId, primary, text);
      case "wrong_stats" -> replyWrongStats(platformUserId, classId, primary);
      case "exam_readiness" -> replyExamReadiness(platformUserId, classId, primary);
      case "weak_points" -> replyWeakPoints(platformUserId, classId, primary);
      case "go_sync" -> replyGoSync(platformUserId, classId, primary);
      case "explain_question" -> replyExplainQuestion(platformUserId, classId, primary, body);
      case "ask_kb" -> replyAskKb(platformUserId, classId, primary, text, body);
      case "schedule_today" -> replySchedule(classId, primary);
      case "my_homework" -> replyMyHomework(platformUserId, classId, primary, text);
      case "progress" -> replyProgress(classId, primary);
      case "unsubmitted" -> replyUnsubmitted(classId, primary);
      case "change_faq" -> replyChangeFaq(classId);
      case "go_precise" -> replyPrecise(classId);
      case "draw_hint" -> replyDrawHint(classId, primary);
      case "help" -> replyHelp(primary);
      default -> replyUnknown(primary);
    };
  }

  private void clearRequestCache() {
    syncCache.remove();
    metricsCache.remove();
    profileCache.remove();
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

  private Map<String, Object> replyLearnDetail(
      Long platformUserId, Long classId, String primary, String askText) {
    if (looksLikeWrongStatsQuestion(askText)) {
      return replyWrongStats(platformUserId, classId, primary);
    }
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
    StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
    ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    replies.add(textReply(formatLearningBoard(cert, m, sync, profile, true)));
    cards.add(buildLearningSnapshot(platformUserId, classId, sync, cert, profile));

    if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    if (profile.homework().pending() > 0 || profile.mock().pending() > 0) {
      actions.add(nav("课堂作业/模考", "/pages/class/homeworkTodo?classId=" + classId));
    }
    return pack("learn_detail", primary, replies, cards, actions);
  }

  /** 专答「我有多少错题」：先给数量，不绕进度长文、不进知识库。 */
  private Map<String, Object> replyWrongStats(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);
    int green = 0;
    int star = 0;
    Integer doneQ = null;
    Integer targetQ = null;
    Integer coverage = m.coveragePercent();
    Integer accuracy = m.accuracy();
    if (sync != null && sync.overview() != null) {
      @SuppressWarnings("unchecked")
      Map<String, Object> wrong = sync.overview().get("wrongStats") instanceof Map
          ? (Map<String, Object>) sync.overview().get("wrongStats") : Map.of();
      green = toInt(wrong.get("green"), 0);
      star = toInt(wrong.get("star"), 0);
      @SuppressWarnings("unchecked")
      Map<String, Object> progress = sync.overview().get("progress") instanceof Map
          ? (Map<String, Object>) sync.overview().get("progress") : Map.of();
      int d = toInt(progress.get("doneQuestions"), -1);
      int t = toInt(progress.get("targetQuestions"), -1);
      if (d >= 0) {
        doneQ = d;
      }
      if (t >= 0) {
        targetQ = t;
      }
    }

    StringBuilder sb = new StringBuilder();
    sb.append("你问的是错题数量，按本班真实刷题记录：\n\n");
    sb.append("1. 专项/班级错题本：约 ").append(m.wrongBookCount()).append(" 题\n");
    sb.append("2. 同步学错题卡：红 ").append(m.red())
        .append(" · 黄 ").append(m.yellow())
        .append(" · 已清 ").append(green)
        .append(" · 星标 ").append(star).append("\n");
    if (!m.weakTitles().isEmpty()) {
      sb.append("3. 薄弱优先：")
          .append(String.join("、", m.weakTitles().stream().limit(3).toList()))
          .append("\n");
    }
    if (!m.recentWrongStems().isEmpty()) {
      sb.append("\n近期错题：")
          .append(String.join("；", m.recentWrongStems().stream().limit(3).toList()))
          .append("\n");
    }
    if (doneQ != null && targetQ != null) {
      sb.append("\n（同步学覆盖 ").append(doneQ).append("/").append(targetQ).append(" 题");
      if (coverage != null) {
        sb.append("，覆盖率 ").append(coverage).append("%");
      }
      if (accuracy != null) {
        sb.append("，正确率 ").append(accuracy).append("%");
      }
      sb.append("，仅供参考）");
    }
    sb.append("\n\n清错题可去 AI 精准学；继续日练可打开同步练习。");

    List<Map<String, Object>> actions = new ArrayList<>();
    actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    if (sync != null && sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    List<Map<String, Object>> cards = new ArrayList<>();
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
    StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
    cards.add(buildLearningSnapshot(platformUserId, classId, sync, cert, profile));
    return pack("wrong_stats", primary, List.of(textReply(sb.toString().trim())), cards, actions);
  }

  /**
   * 报考可行性：真数据看板 + 明确结论 + 温和建议（默认阈值，可后续配置化）。
   */
  private Map<String, Object> replyExamReadiness(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
    StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
    ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);

    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    StringBuilder board = new StringBuilder();
    board.append(formatLearningBoard(cert, m, sync, profile, true));
    board.append("\n\n");

    String verdict;
    String advice;
    String level;
    if (m.accuracy() == null && m.coveragePercent() == null && m.wrongBookCount() <= 0
        && m.specialAnswered() <= 0) {
      level = "insufficient";
      verdict = "【结论】样本还不够，暂时无法判断能不能考过。";
      advice = "建议先完成几天同步学日练或精准学强化，再回来问我；我会用真实正确率与薄弱点给你结论。";
    } else if (m.ready()) {
      level = "ready";
      verdict = "【结论】建议报考：按当前真数据，整体比较稳妥。";
      advice = "温和建议：考前仍把红黄错题与薄弱点过一遍，保持手感即可。认定结果以官方考试为准，小培只做学习侧参考。";
    } else if (m.borderline()) {
      level = "practice";
      verdict = "【结论】再练一段时间再考更稳。";
      advice = "温和建议：优先攻薄弱点与错题本，把正确率往 80% 推、薄弱点压到 3 个以内。需要的话我可以陪你讲题或带你去练。";
    } else {
      level = "not_ready";
      verdict = "【结论】暂不建议马上报考。";
      advice = "温和建议：先把基础覆盖与错题清算做扎实，别急着上考场。你每练完一轮，我可以重新评估。";
    }

    replies.add(textReply(board + verdict + "\n" + advice));

    Map<String, Object> card = buildLearningSnapshot(platformUserId, classId, sync, cert, profile);
    card.put("type", "readiness_card");
    card.put("level", level);
    cards.add(card);

    if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    return pack("exam_readiness", primary, replies, cards, actions);
  }

  private Map<String, Object> replyWeakPoints(Long platformUserId, Long classId, String primary) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);
    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();

    if (m.weakTitles().isEmpty() && m.wrongBookCount() <= 0) {
      replies.add(textReply("目前还没有汇总到明显薄弱点。先多练几轮同步学或精准学，我就能按真实错题给你一对一补。"));
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("根据你的刷题记录，当前需要重点补的有：\n");
      int i = 1;
      for (String title : m.weakTitles()) {
        sb.append(i++).append(". ").append(title).append("\n");
        if (i > 8) {
          break;
        }
      }
      if (m.wrongBookCount() > 0) {
        sb.append("错题本约 ").append(m.wrongBookCount()).append(" 题。");
      }
      sb.append("\n你可以：① 直接把某道题/考点发我讲解；② 去精准学做同类强化；③ 继续同步学清算红黄卡。");
      replies.add(textReply(sb.toString().trim()));
    }
    if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    return pack("weak_points", primary, replies, List.of(), actions);
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
   * 讲题链：优先题库/草稿解析（peek），缺解析再 ensureResolve；默认不调 WeKnora 扩写以提速。
   * body: questionId（库内 id，仅查题）, questionNo/questionTotal（列表序号，仅展示）
   */
  private Map<String, Object> replyExplainQuestion(
      Long platformUserId, Long classId, String primary, Map<String, Object> body) {
    Long questionId = body == null ? null : toLong(body.get("questionId"));
    if (questionId == null || questionId <= 0) {
      return pack("explain_question", primary,
          List.of(textReply("请在同步学答题页打开小培，并点「讲讲这题」；我需要当前题目才能讲解。")),
          List.of(), List.of());
    }
    String listLabel = formatQuestionListLabel(body);

    List<Map<String, Object>> replies = new ArrayList<>();
    List<Map<String, Object>> cards = new ArrayList<>();
    List<Map<String, Object>> actions = new ArrayList<>();
    String resolveText = null;
    String source = "empty";
    String stem = null;

    try {
      int qidInt = Math.toIntExact(questionId);
      Map<String, Object> resolved = questionResolveAssistService.peekResolve(qidInt);
      resolveText = resolved.get("resolve") == null ? null : String.valueOf(resolved.get("resolve")).trim();
      source = resolved.get("source") == null ? "empty" : String.valueOf(resolved.get("source"));
      if (resolved.get("stem") != null) {
        stem = String.valueOf(resolved.get("stem"));
      }
      // 题库无解析时再触发 AI 生成（较慢）；有解析则直接返回
      if (!StringUtils.hasText(resolveText)) {
        resolved = questionResolveAssistService.ensureResolve(qidInt);
        resolveText = resolved.get("resolve") == null ? null : String.valueOf(resolved.get("resolve")).trim();
        source = resolved.get("source") == null ? "empty" : String.valueOf(resolved.get("source"));
        if (!StringUtils.hasText(stem) && resolved.get("stem") != null) {
          stem = String.valueOf(resolved.get("stem"));
        }
      }
    } catch (ArithmeticException ex) {
      log.warn("[小培] questionId 超出 int：{}", questionId);
    } catch (Exception e) {
      log.warn("[小培] 讲题解析失败 qid={}: {}", questionId, e.getMessage());
    }

    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "explain_card");
    card.put("questionId", String.valueOf(questionId));
    card.put("questionLabel", listLabel);
    Integer qNo = body == null ? null : toIntOrNull(body.get("questionNo"));
    Integer qTotal = body == null ? null : toIntOrNull(body.get("questionTotal"));
    if (qNo != null) {
      card.put("questionNo", qNo);
    }
    if (qTotal != null) {
      card.put("questionTotal", qTotal);
    }
    card.put("source", source);
    if (StringUtils.hasText(stem)) {
      String plain = stripHtmlPlain(stem);
      if (StringUtils.hasText(plain)) {
        card.put("stem", plain.length() > 120 ? plain.substring(0, 120) + "…" : plain);
      }
    }
    cards.add(card);

    if (StringUtils.hasText(resolveText)) {
      replies.add(textReply("【官方/题库解析】（" + listLabel + "）\n" + resolveText));
    } else {
      replies.add(textReply("题库暂无完整解析。下面仅围绕" + listLabel
          + "讲解，不会换成知识库里的其它题目。你可以粘贴题干，我按考点讲解。"));
    }

    // 按场景决定是否给深链：刷题页不再塞「打开同步练习 / 精准学」
    appendSceneAwarePracticeActions(actions, classId, body);
    Map<String, Object> out = pack("explain_question", primary, replies, cards, actions);
    out.put("source", "resolve+" + source);
    return out;
  }

  /** 答题页列表序号文案，如「第 3/20 题」；无序号时回退「当前题」。 */
  private static String formatQuestionListLabel(Map<String, Object> body) {
    Integer no = body == null ? null : toIntOrNull(body.get("questionNo"));
    Integer total = body == null ? null : toIntOrNull(body.get("questionTotal"));
    if (no != null && no > 0) {
      if (total != null && total > 0) {
        return "第 " + no + "/" + total + " 题";
      }
      return "第 " + no + " 题";
    }
    return "当前题";
  }

  private static Integer toIntOrNull(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.intValue();
    }
    try {
      String s = String.valueOf(v).trim();
      if (!StringUtils.hasText(s)) {
        return null;
      }
      return Integer.parseInt(s);
    } catch (Exception e) {
      return null;
    }
  }

  /** 去掉检索日志。 */
  private static String sanitizeExplainAnswer(String answer, String listLabel) {
    if (!StringUtils.hasText(answer)) {
      return answer;
    }
    String a = answer.replaceAll("(?im)^\\s*Keyword search.*$", "").trim();
    if (StringUtils.hasText(listLabel) && a.contains("题目：") && !a.contains(listLabel)
        && a.matches("(?s).*题号\\s*\\d{4,}.*")) {
      return "（请对照答题页" + listLabel + "）\n" + a;
    }
    return a;
  }

  /** 题干富文本转纯文本，避免前端露出 &lt;p&gt; 标签。 */
  private static String stripHtmlPlain(String raw) {
    if (!StringUtils.hasText(raw)) {
      return "";
    }
    return raw
        .replaceAll("(?is)<br\\s*/?>", "\n")
        .replaceAll("(?is)</p>", "\n")
        .replaceAll("(?is)<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
        .replaceAll("\\n{3,}", "\n\n")
        .trim();
  }

  /**
   * 自由知识问答 → WeKnora。
   * 个人进度数字只允许来自下方 Tool 快照；考点/考证政策走知识库；可带图片。
   */
  private Map<String, Object> replyAskKb(
      Long platformUserId, Long classId, String primary, String text, Map<String, Object> body) {
    String kbName = weKnoraProperties.xiaopeiKnowledgeBaseName();
    if (!StringUtils.hasText(text)) {
      return pack("ask_kb", primary,
          List.of(textReply("直接把问题发给我就行：考点、概念、考证政策，或粘贴整道题。问进度/能不能考过也可以。")),
          List.of(), List.of());
    }
    // 做题页说「讲这题」→ 强制当前题 Tool，禁止知识库讲成别的题
    if (body != null && body.get("questionId") != null && looksLikeExplainCurrentQuestion(text)) {
      return replyExplainQuestion(platformUserId, classId, primary, body);
    }
    if (looksLikeExamReadiness(text)) {
      return replyExamReadiness(platformUserId, classId, primary);
    }
    // 错题数量：强制 Tool 短答，禁止进 WeKnora 揉成进度摘要
    if (looksLikeWrongStatsQuestion(text)) {
      return replyWrongStats(platformUserId, classId, primary);
    }
    // 「考什么证」：本班真数据优先，再可选知识库补充（禁止库覆盖工种/级别）
    if (looksLikeCertQuestion(text)) {
      return replyCertWithKbSupplement(platformUserId, classId, primary, text, body);
    }
    // 作业/模考/完成了哪些任务：强制 Tool，禁止进 WeKnora 被当成闲聊拒答
    if (looksLikeHomeworkTaskQuestion(text)) {
      return replyMyHomework(platformUserId, classId, primary, text);
    }
    // 含正确率/通过率等个人指标：强制走 Tool，禁止进 WeKnora 说「库里没有」
    if (looksLikeProgressQuestion(text)) {
      if (containsAny(text.toLowerCase(Locale.ROOT), "通过率", "及格率", "能不能考", "能不能过")) {
        return replyExamReadiness(platformUserId, classId, primary);
      }
      return replyLearnDetail(platformUserId, classId, primary, text);
    }
    // 口语软分流（与 doChat 一致，防止 chip 误传 ask_kb）
    String soft = softRouteStudentFreeForm(text);
    if ("exam_readiness".equals(soft)) {
      return replyExamReadiness(platformUserId, classId, primary);
    }
    if ("learn_detail".equals(soft)) {
      return replyLearnDetail(platformUserId, classId, primary, text);
    }
    if ("what_now".equals(soft)) {
      return replyWhatNow(platformUserId, classId, primary);
    }
    if ("my_homework".equals(soft)) {
      return replyMyHomework(platformUserId, classId, primary, text);
    }
    if ("weak_points".equals(soft)) {
      return replyWeakPoints(platformUserId, classId, primary);
    }
    // 闲聊/时事/通用百科：直接拒答并引导回学习，避免胡编
    if (looksLikeOffTopicChitchat(text)) {
      return replyOutOfScope(primary, classId, body);
    }
    if (!weKnoraProperties.isApiReady()) {
      // 考证类无知识库时，至少回班务真数据
      if (looksLikeCertQuestion(text)) {
        return replyCertFactsOnly(platformUserId, classId, primary);
      }
      // 随意问仍可用本班快照作答（进度/作业/报考）
      if (looksLikeInScopeFreeQuestion(text) || looksLikeLearningTopic(text)) {
        List<Map<String, Object>> actions = new ArrayList<>();
        appendSceneAwarePracticeActions(actions, classId, body);
        return pack("ask_kb", primary,
            List.of(textReply(buildFreeFormSnapshotFallback(platformUserId, classId, text)
                + "\n\n（知识库暂未开通，考点细则待开通后再补充）")),
            List.of(), actions);
      }
      return pack("ask_kb", primary,
          List.of(textReply("知识库问答暂未开通（WeKnora API Key 未配置）。进度与「能不能考过」仍可用真实刷题数据回答。")),
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
      Map<String, Object> ctxBody = body == null ? new LinkedHashMap<>() : new LinkedHashMap<>(body);
      ctxBody.put("text", text);
      // 自由问答默认轻量上下文：避免每次都拉 dashboard/作业拖慢首包；个人进度类已在上方走 Tool
      String context = buildLearnerContextForLlm(platformUserId, classId, ctxBody);
      List<String> images = parseImageDataUrls(body);
      boolean certQ = looksLikeCertQuestion(text);
      boolean pasted = looksLikePastedQuestion(text);
      String query = """
          你是学习伙伴「小培」，辅导本班学习、考证与题库相关问题。
          学员可能口语随意提问：先理解真实意图，再作答；口语不等于闲聊。
          【学员上下文快照】（个人进度/作业/报考数字只能用这里的；没有就说暂无，禁止编造）
          %s
          【回答规则】
          1. 先判断意图：进度/能不能考过/错题/作业模考/考证/考点概念/讲题/其它。
          2. 个人进度、正确率、通过率、错题、作业、模考、报考工种等级：只依据快照，3～8 句直接说清并给下一步；禁止说「知识库没有」「去点某某按钮」。
          3. 「考什么证 / 怎么拿证 / 考完还能考什么」：先复述快照本班真数据；再结合知识库「%s」补充；库没有就诚实说。
          4. 考点、概念、工艺、安全规程：优先依据知识库检索；检索不到就明确说「知识库暂未收录」，禁止用通识编造工种知识点。
          5. 附带图片或粘贴题干：先审题再讲考点与易错点。
          6. 闲聊、时事、娱乐、天气、股票、星座、旅游、与本班学习无关的百科：不要答实质内容，只说明你是学习伙伴并引导回学习问题。
          7. 不要让学员「先点快捷指令」才给答案。
          8. %s
          学员问题：%s
          """.formatted(
          context,
          kbName,
          certQ ? "本题偏考证路径，优先班务真数据 + 知识库政策。"
              : (pasted ? "本题是粘贴/拍照题目，按讲题辅导来答。"
                  : "自由口语问答：能用快照就先用快照，再用知识库补考点。"),
          text);

      WeKnoraChatResult result = weKnoraClient.knowledgeChat(
          sessionId, query, kbIds, agentId, images, true);

      boolean learningAdjacent = pasted
          || (images != null && !images.isEmpty())
          || looksLikeLearningTopic(text)
          || looksLikeInScopeFreeQuestion(text);
      boolean kbHit = hasKbReferenceHit(result.references());
      // 仅明确闲聊才拒答；口语学习问题即使库未命中也允许用快照/诚实提示作答
      if (!kbHit && !learningAdjacent && looksLikeOffTopicChitchat(text)) {
        Map<String, Object> refuse = replyOutOfScope(primary, classId, body);
        refuse.put("sessionId", sessionId);
        refuse.put("kbName", kbName);
        refuse.put("source", "scope_guard");
        return refuse;
      }
      if (!kbHit && !learningAdjacent) {
        // 不像学习也不像明确闲聊：给轻量引导，仍附带一句可用快照能答的方向
        Map<String, Object> refuse = replyOutOfScope(primary, classId, body);
        refuse.put("sessionId", sessionId);
        refuse.put("kbName", kbName);
        refuse.put("source", "scope_guard");
        return refuse;
      }

      List<Map<String, Object>> replies = new ArrayList<>();
      String answer = sanitizeXiaopeiKbAnswer(result.answer());
      if (!StringUtils.hasText(answer)) {
        answer = buildFreeFormSnapshotFallback(platformUserId, classId, text);
      } else if (!kbHit && learningAdjacent) {
        if (!answer.contains("知识库") && !answer.contains("暂未") && !answer.contains("没有找到")
            && !answer.contains("快照") && !answer.contains("真数据")) {
          answer = "下面结合你的本班学习记录作答；考点细则若未命中知识库，请以教材与老师讲解为准：\n" + answer;
        }
      }
      replies.add(textReply(answer));
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
      List<Map<String, Object>> actions = new ArrayList<>();
      appendSceneAwarePracticeActions(actions, classId, body);
      Map<String, Object> out = pack("ask_kb", primary, replies, cards, actions);
      out.put("sessionId", sessionId);
      out.put("kbName", kbName);
      out.put("source", "weknora");
      return out;
    } catch (Exception e) {
      log.warn("[小培] WeKnora 问答失败 classId={}: {}", classId, e.getMessage());
      if (looksLikeCertQuestion(text)) {
        return replyCertFactsOnly(platformUserId, classId, primary);
      }
      return pack("ask_kb", primary,
          List.of(textReply("知识库暂时繁忙：" + shortenErr(e.getMessage())
              + "。进度问题可问「我学到哪里了」；报考问题可问「能不能考过」。")),
          List.of(), List.of());
    }
  }

  /** 去掉 WeKnora Agent 的 Tool/检索调试碎片。 */
  private static String sanitizeXiaopeiKbAnswer(String answer) {
    if (!StringUtils.hasText(answer)) {
      return answer;
    }
    String a = answer
        .replaceAll("(?im)^\\s*Tool completed.*$", "")
        .replaceAll("(?im)^\\s*Keyword search.*$", "")
        .replaceAll("(?is)\\(grep_results;.*?\\)", "")
        .replaceAll("(?is)payload omitted from history[^\\n]*", "")
        .replaceAll("(?m)^[ \\t]*\\n", "\n")
        .trim();
    if (!StringUtils.hasText(a)
        || a.regionMatches(true, 0, "Tool completed", 0, "Tool completed".length())
        || a.contains("grep_results")) {
      return "";
    }
    return a;
  }

  private Map<String, Object> replyCertFactsOnly(Long platformUserId, Long classId, String primary) {
    return replyCertWithKbSupplement(platformUserId, classId, primary, "考什么证", null);
  }

  /**
   * 考证问答：先报本班配置真数据，再调 WeKnora 补充申报/晋级说明。
   * 知识库不得改写工种名称与级别；库不可用时仅回真数据。
   */
  private Map<String, Object> replyCertWithKbSupplement(
      Long platformUserId, Long classId, String primary, String text, Map<String, Object> body) {
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
    String facts = buildCertFactsBlock(cert);

    List<Map<String, Object>> actions = new ArrayList<>();
    appendSceneAwarePracticeActions(actions, classId, body);
    if (actions.isEmpty() && sync.entryVisible()) {
      actions.add(syncNav(sync));
    }

    if (!weKnoraProperties.isApiReady()) {
      String only = facts
          + "\n\n「考证有什么用 / 怎么拿证 / 考完还能考什么」等细则，待知识库开通后补充；"
          + "目前以本班配置为准。技能等级证书一般用于证明岗位技能水平，具体评聘与政策以教务和官方认定为准。";
      return pack("ask_kb", primary, List.of(textReply(only)), List.of(), actions);
    }

    String kbName = weKnoraProperties.xiaopeiKnowledgeBaseName();
    try {
      String sessionId = body != null && body.get("sessionId") != null
          ? String.valueOf(body.get("sessionId")).trim() : "";
      if (!StringUtils.hasText(sessionId)) {
        sessionId = weKnoraClient.createSession(
            "AI小培·考证·班级" + classId,
            "考证真数据优先，知识库仅补充申报路径");
      }
      String ask = StringUtils.hasText(text) ? text.trim() : "我现在考的是什么证？";
      String query = """
          你是学习伙伴「小培」。下面【本班真数据】由教务系统给出，绝对正确。
          【硬性约束】
          1. 禁止修改、否定、改写【本班真数据】里的班级、报考工种、报考等级（如「二级/技师」「一级/高级技师」）、考试计划、考期。
          2. 禁止根据课表/方案文档把学员报考等级笼统改成「技师方向」等其它表述来覆盖真数据；等级以「报考等级」行为准。
          3. 学员若问「考这个证有什么用 / 考证干什么 / 有啥好处」：先点明本班报考的工种与等级，再结合知识库说明该证书常见用途（就业、评聘、晋升、技能认定等）；库没有就诚实说政策细则以官方为准，并给 2～3 句通用学习向用途，禁止编造具体补贴金额或未核实政策。
          4. 只补充：申报条件怎么理解、怎么拿证、考完下一级常见路径、证书用途；库没有就一句话说明暂无细则。
          5. 禁止回答成「今日刷题/扫荡任务有什么用」；本题是考证用途，不是同步学即时收益。
          6. 不要输出 Tool/Keyword search 等调试日志；控制在 3～8 句。
          【本班真数据】
          %s
          知识库名称：%s
          学员问题：%s
          """.formatted(facts, kbName, ask);

      WeKnoraChatResult result = weKnoraClient.knowledgeChat(
          sessionId,
          query,
          resolveXiaopeiKbIds(),
          weKnoraProperties.resolveXiaopeiAgentId(),
          List.of(),
          true);

      String supplement = sanitizeXiaopeiKbAnswer(result.answer());
      List<Map<String, Object>> replies = new ArrayList<>();
      if (StringUtils.hasText(supplement)) {
        replies.add(textReply(facts
            + "\n\n【申报与规划补充】（来自知识库，仅作说明，不改写上方本班配置）\n"
            + supplement));
      } else {
        replies.add(textReply(facts
            + "\n\n知识库暂未检索到可补充的申报说明；上方本班配置仍有效。具体以教务与官方认定为准。"));
      }

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
      Map<String, Object> out = pack("ask_kb", primary, replies, cards, actions);
      out.put("sessionId", sessionId);
      out.put("kbName", kbName);
      out.put("source", "cert_facts+kb");
      return out;
    } catch (Exception e) {
      log.warn("[小培] 考证知识库补充失败 classId={}: {}", classId, e.getMessage());
      String fallback = facts
          + "\n\n知识库暂时繁忙，先以本班配置为准。你可以稍后再问「怎么拿证 / 考完还能考什么」。";
      Map<String, Object> out = pack("ask_kb", primary, List.of(textReply(fallback)), List.of(), actions);
      out.put("source", "cert_facts");
      return out;
    }
  }

  private static String buildCertFactsBlock(ClassCertFacts cert) {
    StringBuilder sb = new StringBuilder();
    sb.append("先说你的报考真信息：\n");
    sb.append("班级：").append(Objects.toString(cert.classTitle(), "本班")).append("\n");
    if (StringUtils.hasText(cert.workTypeTitle())) {
      sb.append("报考工种/证书：").append(cert.workTypeTitle()).append("\n");
    } else {
      sb.append("报考工种/证书：暂未配置\n");
    }
    if (StringUtils.hasText(cert.applyLevelLabel())) {
      sb.append("报考等级：").append(cert.applyLevelLabel());
      if ("person".equals(cert.levelSource())) {
        sb.append("（来自报名信息）");
      } else if ("class".equals(cert.levelSource())) {
        sb.append("（来自班级预约）");
      }
      sb.append("\n");
    } else {
      sb.append("报考等级：暂未配置\n");
    }
    if (StringUtils.hasText(cert.heldLevelLabel())) {
      sb.append("现持证书等级：").append(cert.heldLevelLabel()).append("\n");
    }
    if (StringUtils.hasText(cert.examTitle())) {
      sb.append("同步学考试计划：").append(cert.examTitle()).append("\n");
    }
    if (StringUtils.hasText(cert.examDate())) {
      sb.append("计划考期：").append(cert.examDate()).append("\n");
    }
    return sb.toString().trim();
  }

  private String buildLearnerContextForLlm(Long platformUserId, Long classId, Map<String, Object> body) {
    String text = body == null || body.get("text") == null ? "" : String.valueOf(body.get("text"));
    // 个人指标才拉 dashboard/作业；考证只附轻量工种等级，避免自由问每次等 2～5 秒前置查询
    boolean needFullMetrics = looksLikeProgressQuestion(text)
        || looksLikeExamReadiness(text)
        || looksLikeWrongStatsQuestion(text)
        || looksLikeHomeworkTaskQuestion(text);
    boolean needCertSlim = looksLikeCertQuestion(text);
    TrainInfoClassEntity clazz = classMapper.selectById(classId);
    StringBuilder sb = new StringBuilder();
    if (needFullMetrics) {
      SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
      ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
      ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);
      sb.append("班级=").append(Objects.toString(cert.classTitle(), "-"));
      sb.append("；报考工种=").append(Objects.toString(cert.workTypeTitle(), "未配置"));
      if (StringUtils.hasText(cert.applyLevelLabel())) {
        sb.append("；报考等级=").append(cert.applyLevelLabel());
      } else if (cert.workTypeLevel() != null && cert.workTypeLevel() > 0) {
        sb.append("；等级=").append(cert.workTypeLevel());
      }
      sb.append("；考试计划=").append(Objects.toString(cert.examTitle(), "-"));
      sb.append("；考期=").append(Objects.toString(cert.examDate(), "未设置"));
      sb.append("。");
      StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
      sb.append(formatTaskChannelLine("课堂作业", profile.homework())).append("；");
      sb.append(formatTaskChannelLine("模拟考", profile.mock())).append("。");
      sb.append(buildProgressFactsForLlm(sync));
      sb.append(" 薄弱点约 ").append(m.weakCount()).append(" 个");
      if (!m.weakTitles().isEmpty()) {
        sb.append("：").append(String.join("、", m.weakTitles().stream().limit(6).toList()));
      }
      sb.append("；错题本约 ").append(m.wrongBookCount()).append("。");
    } else if (needCertSlim) {
      // 不拉 sync overview，只查报考工种等级，缩短考证问答前置耗时
      ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, null);
      sb.append("班级=").append(Objects.toString(cert.classTitle(), "-"));
      sb.append("；报考工种=").append(Objects.toString(cert.workTypeTitle(), "未配置"));
      if (StringUtils.hasText(cert.applyLevelLabel())) {
        sb.append("；报考等级=").append(cert.applyLevelLabel());
      }
      sb.append("。本题为考证相关问答，未附带完整刷题看板。");
    } else {
      String classTitle = clazz == null ? "-" : Objects.toString(clazz.getTitle(), "-");
      sb.append("班级=").append(classTitle).append("。本题为考点/概念问答，个人进度数字未附带。");
    }
    if (body != null && body.get("questionId") != null) {
      sb.append(" 当前答题页题目 questionId=").append(body.get("questionId"));
      // 快路径：只 peek 题干，不触发 AI 生成解析
      try {
        Long qid = toLong(body.get("questionId"));
        if (qid != null && qid > 0) {
          Map<String, Object> peeked = questionResolveAssistService.peekResolve(Math.toIntExact(qid));
          Object stemObj = peeked == null ? null : peeked.get("stem");
          if (stemObj != null && StringUtils.hasText(String.valueOf(stemObj))) {
            String stem = stripHtmlPlain(String.valueOf(stemObj));
            if (stem.length() > 160) {
              stem = stem.substring(0, 160) + "…";
            }
            sb.append("；当前题干摘要=").append(stem);
          }
        }
      } catch (Exception e) {
        log.debug("[小培] 上下文附带题干失败: {}", e.getMessage());
      }
      sb.append("。若学员说「讲这题/这道题」，必须讲上述当前题，禁止换成知识库其它题。");
    }
    if (body != null && body.get("scene") != null) {
      sb.append(" 场景=").append(body.get("scene")).append("。");
    }
    return sb.toString();
  }

  private String buildProgressFactsForLlm(SyncSnapshot sync) {
    if (sync == null || !sync.configured()) {
      return "本班未开通同步学；无个人日进度数字。";
    }
    if (!sync.entryVisible()) {
      return "同步学已配置但入口不可用：" + Objects.toString(sync.reasonMessage(), "请联系教务") + "。";
    }
    if (sync.overview() == null) {
      return "同步学已开通，但尚未读到 overview 进度。";
    }
    Map<String, Object> ov = sync.overview();
    @SuppressWarnings("unchecked")
    Map<String, Object> today = ov.get("today") instanceof Map ? (Map<String, Object>) ov.get("today") : Map.of();
    @SuppressWarnings("unchecked")
    Map<String, Object> progress = ov.get("progress") instanceof Map
        ? (Map<String, Object>) ov.get("progress") : Map.of();
    @SuppressWarnings("unchecked")
    Map<String, Object> wrong = ov.get("wrongStats") instanceof Map
        ? (Map<String, Object>) ov.get("wrongStats") : Map.of();
    return "考试计划=" + Objects.toString(sync.examTitle(), "-")
        + "；阶段=" + ov.getOrDefault("stageName", "-")
        + "；第 " + ov.getOrDefault("dayIndex", "-") + "/" + ov.getOrDefault("totalDays", "-") + " 天"
        + "；考期=" + ov.getOrDefault("examDate", "未设置")
        + "；今日新题 " + today.getOrDefault("newDone", 0) + "/" + today.getOrDefault("newTarget", 0)
        + "；今日复盘 " + today.getOrDefault("reviewDone", 0) + "/" + today.getOrDefault("reviewTarget", 0)
        + "；覆盖 " + progress.getOrDefault("doneQuestions", 0) + "/"
        + progress.getOrDefault("targetQuestions", 0)
        + "（覆盖率 " + progress.getOrDefault("coveragePercent", 0) + "%）"
        + "；正确率 " + progress.getOrDefault("accuracy", 0) + "%"
        + "；错题卡 红" + wrong.getOrDefault("red", 0)
        + "/黄" + wrong.getOrDefault("yellow", 0)
        + "/已清" + wrong.getOrDefault("green", 0)
        + "/星标" + wrong.getOrDefault("star", 0) + "。";
  }

  private static boolean looksLikeProgressQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    if (looksLikeWrongStatsQuestion(t)) {
      return true;
    }
    return containsAny(t,
        "学到哪", "学到哪里", "到哪了", "到哪儿", "学习进度", "学情",
        "第几天", "学到第", "进度", "覆盖率", "正确率", "通过率", "及格率",
        "练到哪", "复习到哪", "现在学",
        "没掌握", "未掌握", "没有掌握", "薄弱点",
        "答对多少", "做对多少", "准确率");
  }

  /** 做题页「讲这题」口语（勿用过宽的「这题/讲解」，避免误伤错题统计） */
  private static boolean looksLikeExplainCurrentQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    if (looksLikeWrongStatsQuestion(text)) {
      return false;
    }
    String t = text.trim();
    return containsAny(t,
        "讲这题", "讲讲这题", "讲讲这道", "讲讲当前", "讲一下这题", "讲一下这道",
        "这道题怎么", "这题怎么做", "这题怎么答", "这题解析", "解析这题", "讲解这题",
        "当前题怎么", "当前这题", "帮我讲这", "帮我讲讲这", "这道题讲", "讲讲题目");
  }

  /** 「我有多少错题 / 错题情况」→ 学情真数据，禁止当成「该刷哪」或知识库摘要 */
  private static boolean looksLikeWrongStatsQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String raw = text.toLowerCase(Locale.ROOT);
    String t = raw.replaceAll("[\\s\\p{Punct}？?！!。．.、，,·:：;；]", "");
    if (containsAny(t,
        "错题多少", "多少错题", "有多少错题", "有几道错", "有几题错", "错了多少",
        "错题情况", "错题本", "我的错题", "错题数", "几道错题", "几题错了",
        "红黄卡", "红卡多少", "黄卡多少", "错题卡", "还有多少错", "错题有多少", "有多少错")) {
      return true;
    }
    // 「错」+「题」+ 数量问法（兼容中间夹字/标点）
    if (raw.contains("错") && raw.contains("题")
        && containsAny(raw, "多少", "几个", "几道", "几题", "情况", "统计", "数量", "有没有", "查看", "看看")
        && !containsAny(t, "错题练", "练错题", "刷错题", "去做错题", "去练错题")) {
      return true;
    }
    return t.contains("错题") && (t.contains("我") || t.contains("有"))
        && !containsAny(t, "错题练", "练错题", "刷错题", "去做错题", "去练错题");
  }

  private static boolean looksLikeExamReadiness(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    if (containsAny(t, "能不能考", "能不能过", "能考过", "考得过", "考的过", "过过吗",
        "建议报考", "适合报考", "可以报考", "准备好了吗", "准备好报考",
        "敢不敢考", "要不要报名", "报考吗", "能过吗", "过吗")) {
      return true;
    }
    // 「通过率怎么样 / 我的通过率」→ 走真数据评估，禁止进知识库瞎聊
    return containsAny(t, "通过率", "及格率")
        && (containsAny(t, "怎么样", "如何", "好不好", "怎样", "啥样", "高不高", "我的")
            || t.length() <= 16);
  }

  private static boolean looksLikeCertQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    if (containsAny(t,
        "考什么证", "考啥证", "什么证", "哪种证", "哪个证", "这个证", "考这个证",
        "怎么拿证", "如何拿证", "怎么取证", "如何取证", "怎么获得",
        "考完还能", "还能考什么", "还能考啥", "考证路径", "取证条件",
        "报名条件", "报考条件", "认定条件",
        "证有什么用", "证有啥用", "考证有什么用", "考证有啥用",
        "拿证有什么用", "拿证有啥用", "证书有什么用", "证书有啥用",
        "为什么考证", "考证干什么", "考证干嘛", "考证有啥好处", "拿证有啥好处",
        "考证意义", "证书作用", "考证作用", "值不值得考", "值得考证吗")) {
      return true;
    }
    // 「证/证书/考证」+ 用途/好处口语
    return containsAny(t, "证", "证书", "考证", "取证", "认定")
        && containsAny(t, "有什么用", "有啥用", "干什么", "干嘛", "好处", "意义", "作用",
            "值不值", "值得吗", "有没有用", "有用吗");
  }

  /** 做完今日刷题/任务有什么即时收益——勿与「考证有什么用」混淆。 */
  private static boolean looksLikeTaskRewardQuestion(String text) {
    if (!StringUtils.hasText(text) || looksLikeCertQuestion(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    if (containsAny(t, "证", "证书", "考证", "取证", "认定", "报名")) {
      return false;
    }
    if (containsAny(t, "做完有", "刷完有", "练完有", "完成得什么", "完成得啥", "即时收益",
        "做完有什么用", "刷完有什么用", "练完有什么用", "做完有啥用")) {
      return true;
    }
    // 裸「有什么用」仅在明确指向今日任务/刷题时成立
    return containsAny(t, "有什么用", "有啥用")
        && containsAny(t, "做完", "刷完", "练完", "今天", "今日", "任务", "扫荡", "复盘", "新题");
  }

  private static boolean looksLikePastedQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.trim();
    if (t.length() < 40) {
      return false;
    }
    return containsAny(t, "下列", "以下", "正确的是", "错误的是", "应选择", "单选", "多选", "判断题")
        || (t.length() >= 80 && (t.contains("？") || t.contains("?") || t.contains("A.") || t.contains("A、")));
  }

  /** 闲聊/时事/通用百科：不进知识库，避免胡答。 */
  private static boolean looksLikeOffTopicChitchat(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    if (looksLikeLearningTopic(text)
        || looksLikePastedQuestion(text)
        || looksLikeProgressQuestion(text)
        || looksLikeWrongStatsQuestion(text)
        || looksLikeExamReadiness(text)
        || looksLikeCertQuestion(text)
        || looksLikeHomeworkTaskQuestion(text)
        || looksLikeExplainCurrentQuestion(text)
        || looksLikeCapabilityQuestion(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    return containsAny(t,
        "天气", "气温", "下雨吗", "下雨嘛", "几度",
        "股票", "基金", "比特币", "房价",
        "今日新闻", "热点新闻", "八卦", "娱乐圈", "明星",
        "讲个笑话", "讲笑话", "说个段子", "陪我聊", "随便聊聊", "你无聊吗",
        "今天星期几", "现在几点", "什么星座", "算命", "运势",
        "世界杯", "奥运会谁", "比赛谁赢", "比分多少",
        "怎么做菜", "食谱", "外卖推荐", "旅游攻略",
        "写首诗", "写小说", "编个故事", "谈恋爱",
        "特朗普", "拜登", "谁是总统", "比特币涨");
  }

  /** 像学习/考证/工种问题（允许进知识库；库未命中时诚实提示）。 */
  private static boolean looksLikeLearningTopic(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    return containsAny(t,
        "考点", "考试", "考证", "证书", "工种", "认定", "取证", "报名",
        "题", "解析", "答案", "选项", "单选", "多选", "判断",
        "刷题", "错题", "薄弱", "正确率", "通过率", "同步学", "精准学",
        "作业", "模考", "模拟考", "任务", "待办", "交作业",
        "工艺", "规程", "标准", "安全", "操作", "故障", "处理",
        "原理", "设备", "电压", "电流", "温度", "焙烧", "电解", "阳极",
        "知识", "概念", "名词", "什么意思", "怎么理解", "为何", "为什么选",
        "教材", "讲义", "实训", "技能等级", "学习", "复习", "练习", "备考");
  }

  /**
   * 口语随意问但仍像「本班学习相关」：允许走知识库/快照作答，避免误拒。
   * 明确闲聊仍由 {@link #looksLikeOffTopicChitchat} 拦截。
   */
  private static boolean looksLikeInScopeFreeQuestion(String text) {
    if (!StringUtils.hasText(text) || looksLikeOffTopicChitchat(text)) {
      return false;
    }
    if (looksLikeLearningTopic(text) || looksLikeProgressQuestion(text)
        || looksLikeHomeworkTaskQuestion(text) || looksLikeCertQuestion(text)
        || looksLikeExamReadiness(text) || looksLikeWrongStatsQuestion(text)) {
      return true;
    }
    String t = text.trim().toLowerCase(Locale.ROOT);
    if (t.length() > 160) {
      return true;
    }
    if (containsAny(t, "吗", "呢", "？", "?", "怎么", "如何", "为什么", "为啥", "啥", "什么",
        "哪些", "那些", "多少", "能不能", "可不可以", "怎么办", "咋办", "帮我", "告诉我",
        "想问", "请问", "请教", "解释", "说说", "讲讲")) {
      return true;
    }
    return containsAny(t, "我", "本班", "我们班", "这班", "老师", "同学", "小培") && t.length() <= 100;
  }

  /**
   * 学员口语软分流：把随意问句映射到已有 Tool 意图；无法判断则返回 null（走知识库）。
   */
  private static String softRouteStudentFreeForm(String text) {
    if (!StringUtils.hasText(text) || looksLikeOffTopicChitchat(text)) {
      return null;
    }
    if (looksLikeCapabilityQuestion(text) || looksLikePastedQuestion(text)) {
      return null;
    }
    String t = text.toLowerCase(Locale.ROOT);
    // 考证用途必须最先：避免落到刷题收益/学情
    if (looksLikeCertQuestion(t)) {
      return "ask_kb";
    }
    if (looksLikeHomeworkTaskQuestion(t) || looksLikeCompletedTasksQuestion(t)
        || containsAny(t, "还有啥没", "还缺啥", "没做完", "剩下的", "未完成", "待完成")) {
      return "my_homework";
    }
    if (looksLikeExamReadiness(t) || containsAny(t,
        "行不行", "够不够", "稳不稳", "有没有戏", "把握大不大", "能过吗", "过得了吗",
        "考得过吗", "有把握吗", "悬不悬")) {
      return "exam_readiness";
    }
    if (looksLikeWrongStatsQuestion(t)) {
      return containsAny(t, "薄弱") ? "weak_points" : "wrong_stats";
    }
    if (containsAny(t, "薄弱", "不会的点", "短板")) {
      return "weak_points";
    }
    if (containsAny(t, "现在做什么", "现在做啥", "接下来", "然后呢", "下一步", "该干嘛",
        "先干啥", "优先做什么", "优先干啥", "今天练啥", "今天刷啥")) {
      return "what_now";
    }
    if (looksLikeProgressQuestion(t) || containsAny(t,
        "怎么样了", "咋样了", "如何了", "情况怎样", "帮我看看", "看看我",
        "分析一下我", "评估一下", "学得怎样", "练得怎样", "整体怎样", "整体如何")) {
      return "learn_detail";
    }
    return null;
  }

  /** 知识库空答时：用本班真数据给一句可用回复，避免只会拒答。 */
  private String buildFreeFormSnapshotFallback(Long platformUserId, Long classId, String text) {
    try {
      SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
      TrainInfoClassEntity clazz = classMapper.selectById(classId);
      ClassCertFacts cert = loadClassCertFacts(platformUserId, classId, clazz, sync);
      StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
      ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);
      StringBuilder sb = new StringBuilder();
      sb.append("知识库暂未直接命中该问法，先按你的本班记录说明：\n");
      if (StringUtils.hasText(cert.workTypeTitle())) {
        sb.append("报考：").append(cert.workTypeTitle());
        if (StringUtils.hasText(cert.applyLevelLabel())) {
          sb.append(" · ").append(cert.applyLevelLabel());
        }
        sb.append("\n");
      }
      if (m.accuracy() != null) {
        sb.append("练习正确率约 ").append(m.accuracy()).append("%（").append(m.accuracySource()).append("）\n");
      }
      sb.append("错题本约 ").append(m.wrongBookCount()).append(" 题；薄弱约 ").append(m.weakCount()).append(" 个\n");
      sb.append(formatTaskChannelLine("课堂作业", profile.homework())).append("\n");
      sb.append(formatTaskChannelLine("模拟考", profile.mock())).append("\n");
      sb.append("若问的是具体考点，可换关键词或粘贴题干；进度/作业也可直接问「能不能考过」「我完成了哪些任务」。");
      if (StringUtils.hasText(text) && text.length() <= 40) {
        sb.append("（你刚问的是：").append(text.trim()).append("）");
      }
      return sb.toString().trim();
    } catch (Exception e) {
      return "知识库里暂时没找到直接相关说明。你可以换个考点关键词，或问「能不能考过 / 我有多少错题 / 我完成了哪些任务」。";
    }
  }

  /** 课堂作业 / 模拟考 / 「完成了哪些任务」类：走班务真数据，不进知识库拒答。 */
  private static boolean looksLikeHomeworkTaskQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    if (containsAny(t, "模拟考", "模拟考试", "模考", "作业", "待办", "我的题",
        "作业成绩", "模考成绩", "课堂作业", "练习任务")) {
      return true;
    }
    if (containsAny(t,
        "完成了哪些", "完成了那些", "都完成了", "做完了哪些", "做完了那些",
        "交了哪些", "交了那些", "还有哪些任务", "还有什么任务", "我的任务",
        "哪些任务", "那些任务", "什么任务", "啥任务", "任务完成", "完成任务")) {
      return true;
    }
    // 「任务」+ 完成/待办语义
    return containsAny(t, "任务")
        && containsAny(t, "完成", "做完", "交了", "提交", "哪些", "那些", "待办", "未交", "没交", "还有");
  }

  /** 偏「已完成清单」而非总览。 */
  private static boolean looksLikeCompletedTasksQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.toLowerCase(Locale.ROOT);
    return containsAny(t,
        "完成了哪些", "完成了那些", "都完成了", "做完了哪些", "做完了那些",
        "交了哪些", "交了那些", "哪些完成", "那些完成", "完成了什么", "做完了什么",
        "完成了啥", "做完了啥", "已经完成", "已完成");
  }

  private static void appendCompletedTaskSection(
      StringBuilder sb, String label, TaskChannelSummary ch) {
    sb.append(label).append("：");
    if (ch == null || ch.total() <= 0) {
      sb.append("暂无下发\n");
      return;
    }
    if (ch.done() <= 0) {
      sb.append("暂无已交（待办 ").append(ch.pending()).append("）\n");
      return;
    }
    sb.append("已完成 ").append(ch.done()).append("/").append(ch.total());
    if (ch.passed() > 0) {
      sb.append("，通过 ").append(ch.passed());
    }
    sb.append("\n");
    for (String line : ch.doneLines().stream().limit(8).toList()) {
      sb.append("· ").append(line).append("\n");
    }
  }

  private static boolean hasKbReferenceHit(List<Map<String, Object>> references) {
    return references != null && !references.isEmpty();
  }

  private Map<String, Object> replyOutOfScope(String primary, Long classId, Map<String, Object> body) {
    String tip = "我是学习伙伴小培，主要帮你：学习进度、作业模考、错题、讲题、考证路径和本班考点。"
        + "你可以随意用口语问，例如「我学得怎么样了」「还有啥没做」「这个考点啥意思」。"
        + "闲聊、时事或与学习无关的百科问题我答不准，也不乱编。"
        + "也可以问「能不能考过」「我有多少错题」「讲讲这题」，或直接粘贴题干。";
    List<Map<String, Object>> actions = new ArrayList<>();
    appendSceneAwarePracticeActions(actions, classId, body);
    Map<String, Object> out = pack("out_of_scope", primary, List.of(textReply(tip)), List.of(), actions);
    out.put("source", "scope_guard");
    return out;
  }

  /**
   * 场景感知深链：已在同步刷题/同步首页则不再推「打开同步练习」；
   * 刷题中不推「打开精准学」（避免打断做题）；已在精准学首页则不推精准学。
   */
  private static void appendSceneAwarePracticeActions(
      List<Map<String, Object>> actions, Long classId, Map<String, Object> body) {
    if (actions == null) {
      return;
    }
    String scene = body == null || body.get("scene") == null
        ? "" : String.valueOf(body.get("scene")).trim();
    boolean onSyncExam = "sync_exam".equals(scene);
    boolean onSyncHome = "sync_home".equals(scene);
    boolean onPrecise = "precise_home".equals(scene);
    Long examId = body == null ? null : toLong(body.get("examId"));

    if (!onSyncExam && !onSyncHome) {
      if (examId != null && examId > 0) {
        actions.add(nav("打开同步练习", "/pages/exam/syncHome?exam_id=" + examId));
      } else {
        actions.add(nav("打开同步练习", "/pages/exam/syncHome"));
      }
    }
    if (!onSyncExam && !onPrecise && classId != null) {
      actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    }
  }

  /**
   * 加载考证真数据：优先学员报名表 {@code req_work_type / req_level}，缺省回退班级预约工种等级。
   */
  private ClassCertFacts loadClassCertFacts(
      Long platformUserId, Long classId, TrainInfoClassEntity clazz, SyncSnapshot sync) {
    String classTitle = clazz == null ? null : clazz.getTitle();
    Integer classLevel = clazz == null ? null : clazz.getWorkTypeLevel();
    Long classWorkTypeId = clazz == null ? null : clazz.getWorkTypeId();
    String classWorkTypeTitle = null;
    if (classWorkTypeId != null && classWorkTypeId > 0) {
      try {
        ExamWorkTypeEntity wt = examWorkTypeMapper.selectById(classWorkTypeId.intValue());
        if (wt != null && StringUtils.hasText(wt.getTitle())) {
          classWorkTypeTitle = wt.getTitle();
        }
      } catch (Exception e) {
        log.debug("[小培] 工种标题查询跳过: {}", e.getMessage());
      }
    }

    TrainSignUpPeopleSelfEntity person = loadStudentSignUpPerson(platformUserId, classId);
    Integer personReqLevel = person == null ? null : person.getReqLevel();
    Long personReqWorkTypeId = person == null ? null : person.getReqWorkTypeId();
    String personReqWorkTypeName = person == null ? null : person.getReqWorkTypeName();

    Integer reqIndex = SignUpPeopleDefaultTemplatePackService.resolveReqLevelIndex(
        personReqLevel, personReqWorkTypeId, personReqWorkTypeName, classWorkTypeId, classLevel);
    String applyLevelLabel = SignUpLevelSupport.toLabel(reqIndex);
    String workTypeTitle = SignUpPeopleDefaultTemplatePackService.resolveReqWorkTypeName(
        personReqWorkTypeName, classWorkTypeTitle);
    if (!StringUtils.hasText(workTypeTitle)) {
      workTypeTitle = classWorkTypeTitle;
    }

    String levelSource = "unknown";
    if (person != null && reqIndex != null) {
      Integer classReq = SignUpPeopleDefaultTemplatePackService.classLevelToReqIndex(classLevel);
      boolean personExplicit = personReqLevel != null && personReqLevel > 0 && personReqLevel <= 4;
      boolean personOwnWork = (personReqWorkTypeId != null && personReqWorkTypeId > 0)
          || StringUtils.hasText(personReqWorkTypeName);
      if (personExplicit || (personOwnWork && !Objects.equals(reqIndex, classReq))) {
        levelSource = "person";
      } else if (classReq != null) {
        levelSource = "class";
      } else {
        levelSource = "person";
      }
    } else if (reqIndex != null) {
      levelSource = "class";
    }

    // 对外仍保留班级口径 1–5，便于前端兼容；展示优先用 applyLevelLabel
    Integer workTypeLevel = reqIndex != null ? (5 - reqIndex) : classLevel;
    String heldLevelLabel = SignUpPeopleDefaultTemplatePackService.optionalCurrentLevelLabel(person);

    String examTitle = sync == null ? null : sync.examTitle();
    String examDate = null;
    if (sync != null && sync.overview() != null) {
      examDate = str(sync.overview().get("examDate"), null);
    }
    return new ClassCertFacts(
        classTitle, workTypeTitle, workTypeLevel, applyLevelLabel, levelSource, heldLevelLabel,
        examTitle, examDate);
  }

  /** 按班级花名册 + 信息收集表定位学员报考行。 */
  private TrainSignUpPeopleSelfEntity loadStudentSignUpPerson(Long platformUserId, Long classId) {
    if (classId == null || classId <= 0) {
      return null;
    }
    try {
      TrainPeopleListEntity roster = null;
      if (platformUserId != null && platformUserId > 0) {
        roster = peopleListMapper.selectOne(
            new LambdaQueryWrapper<TrainPeopleListEntity>()
                .eq(TrainPeopleListEntity::getTrainClassId, classId)
                .eq(TrainPeopleListEntity::getPlatformUserId, platformUserId)
                .eq(TrainPeopleListEntity::getType, "student")
                .eq(TrainPeopleListEntity::getDeleted, 0)
                .orderByDesc(TrainPeopleListEntity::getId)
                .last("LIMIT 1"));
      }

      TrainSignUpSelfEntity signUp = signUpSelfMapper.selectOne(
          new LambdaQueryWrapper<TrainSignUpSelfEntity>()
              .eq(TrainSignUpSelfEntity::getTrainClassId, classId)
              .eq(TrainSignUpSelfEntity::getDeleted, 0)
              .orderByDesc(TrainSignUpSelfEntity::getId)
              .last("LIMIT 1"));
      if (signUp == null && roster != null && roster.getTrainId() != null && roster.getTrainId() > 0) {
        signUp = signUpSelfMapper.selectOne(
            new LambdaQueryWrapper<TrainSignUpSelfEntity>()
                .eq(TrainSignUpSelfEntity::getTrainInfoId, roster.getTrainId())
                .eq(TrainSignUpSelfEntity::getDeleted, 0)
                .orderByDesc(TrainSignUpSelfEntity::getId)
                .last("LIMIT 1"));
      }
      if (signUp == null) {
        return null;
      }

      LambdaQueryWrapper<TrainSignUpPeopleSelfEntity> peopleQ =
          new LambdaQueryWrapper<TrainSignUpPeopleSelfEntity>()
              .eq(TrainSignUpPeopleSelfEntity::getSignUpId, signUp.getId())
              .eq(TrainSignUpPeopleSelfEntity::getDeleted, 0);
      if (platformUserId != null && platformUserId > 0
          && roster != null && StringUtils.hasText(roster.getIdCard())) {
        final String idCard = roster.getIdCard().trim();
        peopleQ.and(w -> w
            .eq(TrainSignUpPeopleSelfEntity::getPlatformUserId, platformUserId)
            .or()
            .eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, idCard));
      } else if (platformUserId != null && platformUserId > 0) {
        peopleQ.eq(TrainSignUpPeopleSelfEntity::getPlatformUserId, platformUserId);
      } else if (roster != null && StringUtils.hasText(roster.getIdCard())) {
        peopleQ.eq(TrainSignUpPeopleSelfEntity::getIdCardNumber, roster.getIdCard().trim());
      } else {
        return null;
      }
      return signUpPeopleSelfMapper.selectOne(
          peopleQ.orderByDesc(TrainSignUpPeopleSelfEntity::getId).last("LIMIT 1"));
    } catch (Exception e) {
      log.debug("[小培] 学员报考信息查询跳过: {}", e.getMessage());
      return null;
    }
  }

  /** 打开半屏即可看到的学情快照（真数据）。 */
  private Map<String, Object> buildLearningSnapshot(
      Long platformUserId, Long classId, SyncSnapshot sync, ClassCertFacts cert,
      StudentClassProfile profile) {
    ReadinessMetrics m = collectReadinessMetrics(platformUserId, classId, sync);
    if (profile == null) {
      profile = loadStudentClassProfile(platformUserId, classId);
    }
    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "learning_snapshot");
    card.put("title", "我的学情");
    StringBuilder sub = new StringBuilder();
    if (m.accuracy() != null) {
      sub.append("练习正确率 ").append(m.accuracy()).append("%（").append(m.accuracySource()).append("）");
    } else {
      sub.append("暂无足够正确率样本");
    }
    sub.append(" · 错题本 ").append(m.wrongBookCount()).append(" · 薄弱 ").append(m.weakCount());
    if (profile.homework().total() > 0 || profile.mock().total() > 0) {
      sub.append(" · 作业待办 ").append(profile.homework().pending())
          .append(" · 模考待办 ").append(profile.mock().pending());
    }
    card.put("subtitle", sub.toString());
    card.put("accuracy", m.accuracy());
    card.put("accuracySource", m.accuracySource());
    card.put("passTendency", m.accuracy());
    card.put("coveragePercent", m.coveragePercent());
    card.put("wrongBookCount", m.wrongBookCount());
    card.put("weakCount", m.weakCount());
    card.put("weakTitles", m.weakTitles());
    card.put("red", m.red());
    card.put("yellow", m.yellow());
    card.put("weekAnswered", m.weekAnswered());
    card.put("weekWrongRate", m.weekWrongRate());
    card.put("specialAnswered", m.specialAnswered());
    card.put("specialWrongRate", m.specialWrongRate());
    card.put("dayIndex", m.dayIndex());
    card.put("totalDays", m.totalDays());
    card.put("recentWrongs", m.recentWrongStems());
    card.put("homeworkPending", profile.homework().pending());
    card.put("homeworkDone", profile.homework().done());
    card.put("mockPending", profile.mock().pending());
    card.put("mockDone", profile.mock().done());
    card.put("mockPassed", profile.mock().passed());
    card.put("lines", learningSnapshotLines(m, cert, profile));
    return card;
  }

  private static List<String> learningSnapshotLines(
      ReadinessMetrics m, ClassCertFacts cert, StudentClassProfile profile) {
    List<String> lines = new ArrayList<>();
    if (m.accuracy() != null) {
      lines.add("练习正确率/通过倾向 " + m.accuracy() + "%（" + m.accuracySource() + "）");
    } else {
      lines.add("练习正确率：暂无足够样本");
    }
    if (m.specialAnswered() > 0 && m.specialWrongRate() != null) {
      lines.add("专项作答 " + m.specialAnswered() + " 题，错误率 " + m.specialWrongRate() + "%");
    }
    if (m.coveragePercent() != null) {
      lines.add("同步学覆盖率 " + m.coveragePercent() + "%");
    }
    if (m.dayIndex() != null && m.totalDays() != null && m.totalDays() > 0) {
      lines.add("同步学第 " + m.dayIndex() + "/" + m.totalDays() + " 天");
    }
    lines.add("错题本 " + m.wrongBookCount() + " 题 · 红卡 " + m.red() + " · 黄卡 " + m.yellow());
    if (!m.weakTitles().isEmpty()) {
      lines.add("薄弱：" + String.join("、", m.weakTitles().stream().limit(4).toList()));
    }
    if (!m.recentWrongStems().isEmpty()) {
      lines.add("近期错题：" + String.join("；", m.recentWrongStems().stream().limit(3).toList()));
    }
    if (cert != null && StringUtils.hasText(cert.workTypeTitle())) {
      lines.add("报考：" + cert.workTypeTitle()
          + (StringUtils.hasText(cert.applyLevelLabel()) ? " · " + cert.applyLevelLabel() : ""));
    }
    if (profile != null) {
      lines.add(formatTaskChannelLine("课堂作业", profile.homework()));
      lines.add(formatTaskChannelLine("模拟考", profile.mock()));
      if (profile.examScore() != null || profile.theoryScore() != null || profile.realScore() != null) {
        lines.add(formatOfficialScoresLine(profile));
      }
    }
    return lines;
  }

  private String formatLearningBoard(
      ClassCertFacts cert, ReadinessMetrics m, SyncSnapshot sync,
      StudentClassProfile profile, boolean withHeader) {
    StringBuilder board = new StringBuilder();
    if (withHeader) {
      board.append("以下数字全部来自你本人的班级与刷题记录（不是知识库估算）：\n\n");
      board.append("【学情看板】\n");
    }
    if (profile != null && StringUtils.hasText(profile.realname())) {
      board.append("学员：").append(profile.realname()).append("\n");
    }
    if (cert != null && StringUtils.hasText(cert.workTypeTitle())) {
      board.append("报考方向：").append(cert.workTypeTitle());
      if (StringUtils.hasText(cert.applyLevelLabel())) {
        board.append(" · ").append(cert.applyLevelLabel());
      } else if (cert.workTypeLevel() != null && cert.workTypeLevel() > 0) {
        board.append("（").append(cert.workTypeLevel()).append(" 级）");
      }
      board.append("\n");
    }
    if (cert != null && StringUtils.hasText(cert.examTitle())) {
      board.append("考试计划：").append(cert.examTitle()).append("\n");
    }
    if (m.accuracy() != null) {
      board.append("练习正确率/通过倾向：").append(m.accuracy()).append("%（来源：")
          .append(m.accuracySource()).append("）\n");
    } else {
      board.append("练习正确率/通过倾向：暂无足够刷题样本\n");
    }
    if (m.specialAnswered() > 0) {
      board.append("专项/班级练习：作答 ").append(m.specialAnswered()).append(" 题");
      if (m.specialWrongRate() != null) {
        board.append("，错误率 ").append(m.specialWrongRate()).append("%");
      }
      board.append("\n");
    }
    if (m.coveragePercent() != null) {
      board.append("同步学覆盖率：").append(m.coveragePercent()).append("%\n");
    }
    if (m.dayIndex() != null && m.totalDays() != null && m.totalDays() > 0) {
      board.append("同步学日进度：第 ").append(m.dayIndex()).append("/").append(m.totalDays()).append(" 天\n");
    }
    board.append("错题情况：错题本 ").append(m.wrongBookCount()).append(" 题");
    board.append("；同步学错题卡 红 ").append(m.red()).append(" / 黄 ").append(m.yellow()).append("\n");
    board.append("薄弱知识点：约 ").append(m.weakCount()).append(" 个");
    if (!m.weakTitles().isEmpty()) {
      board.append("（").append(String.join("、", m.weakTitles().stream().limit(6).toList())).append("）");
    }
    board.append("\n");
    if (m.weekAnswered() != null && m.weekAnswered() > 0) {
      board.append("近 7 日作答 ").append(m.weekAnswered()).append(" 题");
      if (m.weekWrongRate() != null) {
        board.append("，错误率 ").append(m.weekWrongRate()).append("%");
      }
      board.append("\n");
    }
    if (profile != null) {
      board.append(formatTaskChannelLine("课堂作业", profile.homework())).append("\n");
      board.append(formatTaskChannelLine("模拟考", profile.mock())).append("\n");
      if (profile.examScore() != null || profile.theoryScore() != null || profile.realScore() != null
          || profile.normalScore() != null) {
        board.append(formatOfficialScoresLine(profile)).append("\n");
      }
      if (profile.graduateStatus() != null) {
        board.append("结业状态：").append(graduateStatusLabel(profile.graduateStatus())).append("\n");
      }
    }
    if (!m.recentWrongStems().isEmpty()) {
      board.append("近期错题题干：\n");
      int i = 1;
      for (String stem : m.recentWrongStems()) {
        board.append(i++).append(". ").append(stem).append("\n");
        if (i > 5) {
          break;
        }
      }
    }
    if (sync != null && !sync.configured()) {
      board.append("（本班暂未绑定同步学计划，以上以专项/精准学与班级任务数据为主）\n");
    }
    return board.toString().trim();
  }

  /**
   * 聚合学员在本班的花名册、课堂作业、模拟考等真数据（请求内缓存）。
   */
  private StudentClassProfile loadStudentClassProfile(Long platformUserId, Long classId) {
    String key = platformUserId + ":" + classId;
    Map<String, StudentClassProfile> cache = profileCache.get();
    StudentClassProfile hit = cache.get(key);
    if (hit != null) {
      return hit;
    }
    String realname = null;
    String phone = null;
    BigDecimal examScore = null;
    BigDecimal theoryScore = null;
    BigDecimal realScore = null;
    BigDecimal normalScore = null;
    Integer graduateStatus = null;
    try {
      if (platformUserId != null && platformUserId > 0 && classId != null && classId > 0) {
        TrainPeopleListEntity roster = peopleListMapper.selectOne(
            new LambdaQueryWrapper<TrainPeopleListEntity>()
                .eq(TrainPeopleListEntity::getTrainClassId, classId)
                .eq(TrainPeopleListEntity::getPlatformUserId, platformUserId)
                .eq(TrainPeopleListEntity::getType, "student")
                .eq(TrainPeopleListEntity::getDeleted, 0)
                .orderByDesc(TrainPeopleListEntity::getId)
                .last("LIMIT 1"));
        if (roster != null) {
          realname = roster.getRealname();
          phone = roster.getPhone();
          examScore = roster.getExamScore();
          theoryScore = roster.getTheoryScore();
          realScore = roster.getRealScore();
          normalScore = roster.getNormalScore();
          graduateStatus = roster.getGraduateStatus();
        }
      }
    } catch (Exception e) {
      log.debug("[小培] 花名册查询跳过: {}", e.getMessage());
    }

    TaskChannelSummary homework = TaskChannelSummary.empty();
    TaskChannelSummary mock = TaskChannelSummary.empty();
    try {
      // 一次拉取 assignment+mock，避免两次 listStudentTasks 往返
      List<FollowStudentTaskVO> all = taskService.listStudentTasks(platformUserId, classId, "all");
      List<FollowStudentTaskVO> hw = new ArrayList<>();
      List<FollowStudentTaskVO> mocks = new ArrayList<>();
      if (all != null) {
        for (FollowStudentTaskVO t : all) {
          if (t == null) {
            continue;
          }
          if (TrainFollowTaskApplicationService.TYPE_MOCK.equals(t.taskType())) {
            mocks.add(t);
          } else {
            hw.add(t);
          }
        }
      }
      homework = summarizeTaskChannel(hw);
      mock = summarizeTaskChannel(mocks);
    } catch (Exception e) {
      log.debug("[小培] 作业/模考查询跳过: {}", e.getMessage());
    }

    StudentClassProfile profile = new StudentClassProfile(
        realname, phone, examScore, theoryScore, realScore, normalScore, graduateStatus,
        homework, mock);
    cache.put(key, profile);
    return profile;
  }

  private static TaskChannelSummary summarizeTaskChannel(List<FollowStudentTaskVO> tasks) {
    if (tasks == null || tasks.isEmpty()) {
      return TaskChannelSummary.empty();
    }
    int pending = 0;
    int done = 0;
    int passed = 0;
    List<String> pendingTitles = new ArrayList<>();
    List<String> doneLines = new ArrayList<>();
    for (FollowStudentTaskVO t : tasks) {
      boolean isDone = TrainFollowTaskApplicationService.ITEM_SUBMITTED.equals(t.itemStatus())
          || TrainFollowTaskApplicationService.ITEM_GRADED.equals(t.itemStatus());
      if (isDone) {
        done++;
        if (Boolean.TRUE.equals(t.passed())) {
          passed++;
        }
        StringBuilder line = new StringBuilder(shortTaskTitle(t.title()));
        if (t.score() != null) {
          line.append(" ").append(t.score().stripTrailingZeros().toPlainString()).append("分");
        }
        if (t.passed() != null) {
          line.append(Boolean.TRUE.equals(t.passed()) ? "·通过" : "·未通过");
        } else if (StringUtils.hasText(t.itemStatusName())) {
          line.append("·").append(t.itemStatusName());
        }
        doneLines.add(line.toString());
      } else {
        pending++;
        if (StringUtils.hasText(t.title()) && pendingTitles.size() < 8) {
          pendingTitles.add(shortTaskTitle(t.title()));
        }
      }
    }
    return new TaskChannelSummary(tasks.size(), pending, done, passed, pendingTitles, doneLines);
  }

  /** 作业标题常含长技能描述，展示时截断以免气泡刷屏。 */
  private static String shortTaskTitle(String title) {
    if (!StringUtils.hasText(title)) {
      return "未命名";
    }
    String t = title.trim().replaceAll("[\\s\\u00A0]+", " ");
    if (t.length() <= 28) {
      return t;
    }
    return t.substring(0, 28) + "…";
  }

  private static String formatTaskChannelLine(String label, TaskChannelSummary ch) {
    if (ch == null || ch.total() <= 0) {
      return label + "：暂无下发";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(label).append("：共 ").append(ch.total())
        .append(" · 已交 ").append(ch.done())
        .append(" · 待办 ").append(ch.pending());
    if (ch.done() > 0) {
      sb.append(" · 通过 ").append(ch.passed());
    }
    if (!ch.pendingTitles().isEmpty()) {
      sb.append("；待办：").append(String.join("、", ch.pendingTitles().stream().limit(4).toList()));
    } else if (!ch.doneLines().isEmpty()) {
      sb.append("；最近：").append(String.join("；", ch.doneLines().stream().limit(3).toList()));
    }
    return sb.toString();
  }

  private static String formatOfficialScoresLine(StudentClassProfile profile) {
    List<String> parts = new ArrayList<>();
    if (profile.theoryScore() != null) {
      parts.add("理论 " + profile.theoryScore().stripTrailingZeros().toPlainString());
    }
    if (profile.realScore() != null) {
      parts.add("实操 " + profile.realScore().stripTrailingZeros().toPlainString());
    }
    if (profile.examScore() != null) {
      parts.add("综合/考试 " + profile.examScore().stripTrailingZeros().toPlainString());
    }
    if (profile.normalScore() != null) {
      parts.add("平时 " + profile.normalScore().stripTrailingZeros().toPlainString());
    }
    return parts.isEmpty() ? "成绩：暂无" : ("班级成绩：" + String.join(" · ", parts));
  }

  private static String graduateStatusLabel(Integer status) {
    if (status == null) {
      return "未知";
    }
    return switch (status) {
      case 1 -> "已结业";
      case 2 -> "不合格";
      default -> "未结业";
    };
  }

  private ReadinessMetrics collectReadinessMetrics(Long platformUserId, Long classId, SyncSnapshot sync) {
    String key = platformUserId + ":" + classId;
    Map<String, ReadinessMetrics> cache = metricsCache.get();
    ReadinessMetrics hit = cache.get(key);
    if (hit != null) {
      return hit;
    }
    Integer accuracy = null;
    String accuracySource = "暂无";
    Integer coveragePercent = null;
    Integer dayIndex = null;
    Integer totalDays = null;
    int wrongBookCount = 0;
    Integer weekAnswered = null;
    Integer weekWrongRate = null;
    int specialAnswered = 0;
    Integer specialWrongRate = null;
    int red = 0;
    int yellow = 0;
    List<String> weakTitles = new ArrayList<>();
    List<String> recentWrongStems = new ArrayList<>();

    if (sync != null && sync.overview() != null) {
      Map<String, Object> ov = sync.overview();
      dayIndex = toInt(ov.get("dayIndex"), 0);
      totalDays = toInt(ov.get("totalDays"), 0);
      @SuppressWarnings("unchecked")
      Map<String, Object> progress = ov.get("progress") instanceof Map
          ? (Map<String, Object>) ov.get("progress") : Map.of();
      @SuppressWarnings("unchecked")
      Map<String, Object> wrong = ov.get("wrongStats") instanceof Map
          ? (Map<String, Object>) ov.get("wrongStats") : Map.of();
      red = toInt(wrong.get("red"), 0);
      yellow = toInt(wrong.get("yellow"), 0);
      int doneQ = toInt(progress.get("doneQuestions"), 0);
      int acc = toInt(progress.get("accuracy"), -1);
      if (doneQ > 0 && acc >= 0) {
        accuracy = acc;
        accuracySource = "同步学进度";
      }
      int cov = toInt(progress.get("coveragePercent"), -1);
      if (cov >= 0) {
        coveragePercent = cov;
      }
    }

    try {
      Map<String, Object> dash = preciseLearnDashboardService.dashboard(platformUserId, classId);
      @SuppressWarnings("unchecked")
      Map<String, Object> wrongOverview = dash.get("wrongOverview") instanceof Map
          ? (Map<String, Object>) dash.get("wrongOverview") : null;
      if (wrongOverview != null) {
        @SuppressWarnings("unchecked")
        Map<String, Object> special = wrongOverview.get("special") instanceof Map
            ? (Map<String, Object>) wrongOverview.get("special") : Map.of();
        wrongBookCount = toInt(special.get("wrongBookCount"), 0);
        if (wrongBookCount <= 0) {
          wrongBookCount = toInt(special.get("totalWrong"), 0);
        }
        specialAnswered = toInt(special.get("totalAnswered"), 0);
        double swr = toDoubleObj(special.get("wrongRate"), -1);
        if (specialAnswered > 0 && swr >= 0) {
          specialWrongRate = (int) Math.round(swr);
          int specialAcc = Math.max(0, 100 - specialWrongRate);
          if (accuracy == null || (accuracy == 0 && specialAnswered >= 3)) {
            accuracy = specialAcc;
            accuracySource = "专项/班级刷题";
          }
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> effect = wrongOverview.get("effect") instanceof Map
            ? (Map<String, Object>) wrongOverview.get("effect") : Map.of();
        weekAnswered = toInt(effect.get("weekAnswered"), 0);
        double wwr = toDoubleObj(effect.get("weekWrongRate"), -1);
        if (wwr >= 0) {
          weekWrongRate = (int) Math.round(wwr);
        }
        if (weekAnswered != null && weekAnswered >= 5 && weekWrongRate != null) {
          accuracy = Math.max(0, 100 - weekWrongRate);
          accuracySource = "近7日练习";
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byKnowledge = wrongOverview.get("byKnowledge") instanceof List
            ? (List<Map<String, Object>>) wrongOverview.get("byKnowledge") : List.of();
        for (Map<String, Object> row : byKnowledge) {
          String level = str(row.get("level"), "");
          int rate = toInt(row.get("wrongRate"), 0);
          String title = str(row.get("title"), null);
          if (!StringUtils.hasText(title)) {
            continue;
          }
          if ("薄弱".equals(level) || rate >= 40) {
            weakTitles.add(title);
          }
          if (weakTitles.size() >= 10) {
            break;
          }
        }
        if (weakTitles.isEmpty()) {
          for (Map<String, Object> row : byKnowledge) {
            String title = str(row.get("title"), null);
            if (StringUtils.hasText(title)) {
              weakTitles.add(title);
            }
            if (weakTitles.size() >= 8) {
              break;
            }
          }
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recent = wrongOverview.get("recentWrongs") instanceof List
            ? (List<Map<String, Object>>) wrongOverview.get("recentWrongs") : List.of();
        for (Map<String, Object> q : recent) {
          String stem = str(q.get("stem"), null);
          if (!StringUtils.hasText(stem)) {
            continue;
          }
          String kname = str(q.get("knowledgeName"), "");
          recentWrongStems.add(StringUtils.hasText(kname) ? ("【" + kname + "】" + stem) : stem);
          if (recentWrongStems.size() >= 5) {
            break;
          }
        }
      }
    } catch (Exception e) {
      log.warn("[小培] 学情指标采集失败 classId={}: {}", classId, e.getMessage());
    }

    int weakCount = weakTitles.size();
    boolean ready = accuracy != null && accuracy >= 80 && weakCount <= 3
        && (coveragePercent == null || coveragePercent >= 70
        || (dayIndex != null && totalDays != null && totalDays > 0 && dayIndex * 100 / totalDays >= 70));
    boolean borderline = !ready && (
        (accuracy != null && accuracy >= 60)
            || (weakCount >= 4 && weakCount <= 8)
            || (coveragePercent != null && coveragePercent >= 40)
            || wrongBookCount > 0);
    ReadinessMetrics metrics = new ReadinessMetrics(
        accuracy, accuracySource, coveragePercent, dayIndex, totalDays,
        weakCount, weakTitles, wrongBookCount, weekAnswered, weekWrongRate,
        specialAnswered, specialWrongRate, red, yellow, recentWrongStems,
        ready, borderline && !ready);
    cache.put(key, metrics);
    return metrics;
  }

  private static double toDoubleObj(Object v, double dft) {
    if (v == null) {
      return dft;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(v).trim());
    } catch (Exception e) {
      return dft;
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

  private static List<String> parseImageDataUrls(Map<String, Object> body) {
    if (body == null || body.get("images") == null) {
      return List.of();
    }
    Object raw = body.get("images");
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<String> images = new ArrayList<>();
    for (Object item : list) {
      if (item == null) {
        continue;
      }
      if (item instanceof Map<?, ?> map) {
        Object data = map.get("data");
        if (data != null && StringUtils.hasText(String.valueOf(data))) {
          images.add(String.valueOf(data).trim());
        }
        continue;
      }
      String dataUrl = String.valueOf(item).trim();
      if (StringUtils.hasText(dataUrl)) {
        images.add(dataUrl);
      }
    }
    return images;
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

  private Map<String, Object> replyMyHomework(
      Long platformUserId, Long classId, String primary, String text) {
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

    StudentClassProfile profile = loadStudentClassProfile(platformUserId, classId);
    SyncSnapshot sync = loadSyncSnapshot(platformUserId, classId);
    ReadinessMetrics metrics = collectReadinessMetrics(platformUserId, classId, sync);
    boolean askCompleted = looksLikeCompletedTasksQuestion(text);
    StringBuilder sb = new StringBuilder();
    if (StringUtils.hasText(profile.realname())) {
      sb.append(profile.realname()).append("，");
    }
    if (askCompleted) {
      sb.append("你已完成的班级任务如下（真数据）：\n\n");
      appendCompletedTaskSection(sb, "课堂作业", profile.homework());
      appendCompletedTaskSection(sb, "模拟考", profile.mock());
      int doneTotal = profile.homework().done() + profile.mock().done();
      if (doneTotal <= 0) {
        sb.append("目前还没有已交记录。");
        if (profile.homework().pending() + profile.mock().pending() > 0) {
          sb.append("待办还剩 ")
              .append(profile.homework().pending() + profile.mock().pending())
              .append(" 项。");
        } else if (profile.homework().total() + profile.mock().total() <= 0) {
          sb.append("本班暂未给你下发课堂作业或模拟考。");
        }
      } else {
        sb.append("\n合计已完成 ").append(doneTotal).append(" 项");
        int pending = profile.homework().pending() + profile.mock().pending();
        if (pending > 0) {
          sb.append("，还有 ").append(pending).append(" 项待办");
        }
        sb.append("。");
      }
    } else {
      sb.append("你在本班的作业与模拟考情况如下（真数据）：\n\n");
      sb.append(formatTaskChannelLine("课堂作业", profile.homework())).append("\n");
      sb.append(formatTaskChannelLine("模拟考", profile.mock())).append("\n");
      if (profile.examScore() != null || profile.theoryScore() != null || profile.realScore() != null
          || profile.normalScore() != null) {
        sb.append(formatOfficialScoresLine(profile)).append("\n");
      }
    }
    sb.append("\n\n").append(buildTaskLearningAdvice(profile, metrics, sync));
    replies.add(textReply(sb.toString().trim()));

    Map<String, Object> card = new LinkedHashMap<>();
    card.put("type", "task_profile");
    card.put("title", "作业与模考");
    card.put("homework", profile.homework().toMap());
    card.put("mock", profile.mock().toMap());
    Map<String, Object> scores = new LinkedHashMap<>();
    scores.put("examScore", profile.examScore());
    scores.put("theoryScore", profile.theoryScore());
    scores.put("realScore", profile.realScore());
    scores.put("normalScore", profile.normalScore());
    scores.put("graduateStatus", profile.graduateStatus());
    card.put("scores", scores);
    cards.add(card);

    actions.add(nav("课堂作业", "/pages/class/homeworkTodo?classId=" + classId));
    if (profile.mock().total() > 0) {
      actions.add(nav("模拟考", "/pages/class/homeworkTodo?classId=" + classId + "&taskType=mock"));
    }
    if (metrics.wrongBookCount() > 0 || !metrics.weakTitles().isEmpty()) {
      actions.add(nav("打开 AI 精准学", "/pages/exam/preciseHome?classId=" + classId));
    } else if (sync.entryVisible()) {
      actions.add(syncNav(sync));
    }
    return pack("my_homework", primary, replies, cards, actions);
  }

  /**
   * 基于作业/模考完成与学情真数据给可执行学习建议（非知识库空话）。
   */
  private static String buildTaskLearningAdvice(
      StudentClassProfile profile, ReadinessMetrics m, SyncSnapshot sync) {
    List<String> tips = new ArrayList<>();
    TaskChannelSummary hw = profile.homework();
    TaskChannelSummary mock = profile.mock();
    int hwPending = hw.pending();
    int mockPending = mock.pending();
    int totalTasks = hw.total() + mock.total();

    if (totalTasks <= 0) {
      tips.add("本班暂无作业/模考时，按同步学日进度刷题，或去精准学清错题，保持手感。");
    } else {
      if (hwPending > 0) {
        String pendingHint = hw.pendingTitles().isEmpty()
            ? ""
            : "（如：" + String.join("、", hw.pendingTitles().stream().limit(2).toList()) + "）";
        tips.add("先把剩余 " + hwPending + " 项课堂作业交完" + pendingHint + "，再安排模考或加练。");
      }
      if (hw.done() > 0 && hw.passed() < hw.done()) {
        tips.add("已交作业里有未通过项，打开解析对照错因，把同类题再练一轮后再往下走。");
      }
      if (channelHasZeroScorePass(hw) || channelHasZeroScorePass(mock)) {
        tips.add("有任务得分很低（含 0 分）却显示通过：建议回看该卷解析，确认知识点真会了，避免「交了但没学会」。");
      }
      if (hwPending == 0 && mockPending > 0) {
        tips.add("作业已齐，下一步完成 " + mockPending + " 场待办模拟考；尽量按正式考节奏限时作答。");
      }
      if (mock.done() > 0 && mock.passed() < mock.done()) {
        tips.add("模拟考有未通过，先补薄弱点与错题，再重考同一套或同类卷。");
      }
      if (hw.total() >= 4 && hw.done() * 2 < hw.total()) {
        tips.add("作业完成进度不到一半，本周优先把待办清到过半，再加大刷题量。");
      }
      if (hwPending == 0 && mockPending == 0 && hw.done() + mock.done() > 0) {
        if (m != null && m.wrongBookCount() > 0) {
          tips.add("班级任务已交齐；错题本还有 " + m.wrongBookCount() + " 题，建议去精准学清红黄卡巩固。");
        } else if (m != null && !m.weakTitles().isEmpty()) {
          tips.add("班级任务已交齐；薄弱点「"
              + String.join("、", m.weakTitles().stream().limit(3).toList())
              + "」可做专项练。");
        } else if (sync != null && sync.entryVisible()) {
          tips.add("班级任务已交齐；继续按同步学日进度刷题，把正确率稳住再冲认定。");
        } else {
          tips.add("班级任务已交齐；可定期回看作业/模考解析，或去精准学做薄弱专项。");
        }
      }
    }

    if (m != null && m.accuracy() != null && m.accuracy() < 70
        && (hw.done() > 0 || mock.done() > 0)) {
      tips.add("当前练习正确率约 " + m.accuracy() + "%，任务虽在推进，建议每天固定清一轮错题再做新题。");
    }

    if (tips.isEmpty()) {
      tips.add("按「待办作业 → 模拟考 → 错题巩固」顺序推进即可。");
    }

    StringBuilder out = new StringBuilder("【学习建议】（依据上方真数据）\n");
    int i = 1;
    for (String tip : tips) {
      if (i > 4) {
        break;
      }
      out.append(i++).append(". ").append(tip).append("\n");
    }
    return out.toString().trim();
  }

  private static boolean channelHasZeroScorePass(TaskChannelSummary ch) {
    if (ch == null || ch.doneLines() == null) {
      return false;
    }
    for (String line : ch.doneLines()) {
      if (line != null && line.contains("0分") && line.contains("通过")) {
        return true;
      }
    }
    return false;
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
        ? "我是小培，可以直接聊：\n"
            + "· 随意口语问：学得怎么样、还有啥没做、能不能考过\n"
            + "· 作业与模考：待办、得分、通过情况与学习建议\n"
            + "· 讲题：当前题、错题、粘贴或拍照\n"
            + "· 考证 / 考点：本班配置真数据 + 知识库补充\n"
            + "点「快捷」可展开常用入口；我不会在聊天里替你答题，但可以讲题和指路去练。"
        : "我可以：今日安排、本班进度、谁没交、调课说明、引导抽题布置、跳转 AI 精准学。";
    return pack("help", primary, List.of(textReply(tip)), List.of(), List.of());
  }

  private Map<String, Object> replyUnknown(String primary) {
    String tip = isStudentMode(primary)
        ? "我没太听懂。可以直接打字问，或点「快捷」：能不能考过 / 该刷哪 / 讲讲这题。"
        : "暂未理解。可点快捷指令，或试：今日安排 / 我的作业 / 谁没交 / 该刷哪。";
    return pack("unknown", primary, List.of(textReply(tip)), List.of(), List.of());
  }

  // ------------------------------------------------------------------
  // Sync Tool
  // ------------------------------------------------------------------

  private SyncSnapshot loadSyncSnapshot(Long platformUserId, Long classId) {
    String key = platformUserId + ":" + classId;
    Map<String, SyncSnapshot> cache = syncCache.get();
    SyncSnapshot cached = cache.get(key);
    if (cached != null) {
      return cached;
    }
    Map<String, Object> resolve;
    try {
      resolve = reviewSyncService.resolveForAppMember(classId, platformUserId);
    } catch (Exception e) {
      log.warn("[小培] resolve 同步学失败 classId={}: {}", classId, e.getMessage());
      SyncSnapshot empty = SyncSnapshot.empty();
      cache.put(key, empty);
      return empty;
    }
    boolean configured = Boolean.TRUE.equals(resolve.get("configured"));
    boolean entryVisible = Boolean.TRUE.equals(resolve.get("entryVisible"));
    Long examId = toLong(resolve.get("examId"));
    String examTitle = str(resolve.get("examTitle"), null);
    String reasonMessage = str(resolve.get("reasonMessage"), null);
    Map<String, Object> overview = null;
    // 只要绑了 exam，就尽量读 overview（即便入口灰度未开，学情问答仍可用真数据）
    if (configured && examId != null && examId > 0) {
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
    SyncSnapshot snap = new SyncSnapshot(configured, entryVisible, examId, examTitle, reasonMessage, overview);
    cache.put(key, snap);
    return snap;
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
    // 带 classId 便于小培续聊上下文（App syncHome 已支持）
    return nav("打开同步练习",
        "/pages/exam/syncHome?exam_id=" + sync.examId());
  }

  private static boolean isStudentMode(String primary) {
    return ROLE_STUDENT.equals(primary) || TrainClassRoleApplicationService.MONITOR.equals(primary);
  }

  private static String detectIntent(String text, boolean studentMode) {
    if (!StringUtils.hasText(text)) {
      return "help";
    }
    String t = text.toLowerCase(Locale.ROOT);

    // 能力介绍优先：避免「你都可以做什么」被「做什么」误判成今日任务
    if (looksLikeCapabilityQuestion(t)) {
      return "help";
    }

    if (studentMode) {
      if (looksLikeExamReadiness(t)) {
        return "exam_readiness";
      }
      // 错题数量/情况优先：避免「我有多少错题」落到 go_sync / 知识库
      if (looksLikeWrongStatsQuestion(t)) {
        return containsAny(t, "薄弱") ? "weak_points" : "wrong_stats";
      }
      // 作业/模考/完成了哪些任务：优先于「做完有什么用」等收益口语
      if (looksLikeHomeworkTaskQuestion(t)) {
        return "my_homework";
      }
      // 进度口语优先：避免「学到哪里了 / 通过率」被当成知识库闲聊
      if (containsAny(t, "学到哪", "学到哪里", "到哪了", "到哪儿", "学习进度", "学情",
          "第几天", "学到第", "练到哪", "复习到哪", "现在学", "进度如何",
          "学得怎么样", "学习详情", "覆盖率", "正确率", "通过率", "及格率",
          "没掌握", "未掌握", "没有掌握", "薄弱点", "薄弱知识", "准确率",
          "班级情况", "我的情况", "在班", "本班情况")) {
        if (containsAny(t, "薄弱")) {
          return "weak_points";
        }
        if (containsAny(t, "通过率", "及格率")) {
          return "exam_readiness";
        }
        return "learn_detail";
      }
      // 今日任务：必须带「现在/今天/今日/该」等，禁止裸匹配「做什么」
      if (containsAny(t, "现在做什么", "现在做啥", "现在干啥", "现在干嘛",
          "今天做什么", "今天做啥", "今天干啥", "今天练", "今天刷",
          "今日任务", "今日做什么", "该干什么", "该做啥", "该干啥",
          "接下来做什么", "接下来干啥")
          || (containsAny(t, "现在做", "该干", "该做") && !containsAny(t, "你", "能", "会", "都"))) {
        return "what_now";
      }
      // 考证用途/路径优先：禁止「有什么用」误进刷题即时收益
      if (looksLikeCertQuestion(t)) {
        return "ask_kb";
      }
      if (looksLikeTaskRewardQuestion(t)) {
        return "reward_now";
      }
      if (containsAny(t, "未来", "学完能", "认定", "考期", "能拿到") && !containsAny(t, "考什么", "怎么拿", "还能考", "这个证", "有什么用", "有啥用")) {
        return "reward_future";
      }
      if (containsAny(t, "进度") && !containsAny(t, "本班进度", "作业进度")) {
        return "learn_detail";
      }
      if (containsAny(t, "讲这题", "讲讲这", "怎么讲", "讲解一下", "解析一下")) {
        return "explain_question";
      }
      if (containsAny(t, "该刷", "刷哪", "接下来刷", "去练", "同步学", "同步练")) {
        return "go_sync";
      }
      if (containsAny(t, "知识库", "知识问答", "什么是", "怎么理解", "讲一下", "解释一下")) {
        return "ask_kb";
      }
      if (containsAny(t, "精准学")) {
        return "go_precise";
      }
    }

    if (containsAny(t, "课表", "今日安排", "上课安排")) {
      return "schedule_today";
    }
    if (looksLikeHomeworkTaskQuestion(t)) {
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
    if (containsAny(t, "刷哪", "去刷", "错题练", "推荐练")) {
      return studentMode ? "go_sync" : "go_precise";
    }
    if (containsAny(t, "抽题", "布置")) {
      return "draw_hint";
    }
    if (containsAny(t, "帮助", "能做", "功能")) {
      return "help";
    }
    // 学员口语兜底：今天/安排 仍可落到课表；干啥已在上方
    if (studentMode && containsAny(t, "今天", "安排", "上课") && !containsAny(t, "做什么", "干啥")) {
      return "schedule_today";
    }
    if (!studentMode && containsAny(t, "课表", "今日", "今天", "安排", "干啥", "上课")) {
      return "schedule_today";
    }
    return "unknown";
  }

  /** 「你能做什么 / 有哪些功能」类 → help，勿进 what_now */
  private static boolean looksLikeCapabilityQuestion(String text) {
    if (!StringUtils.hasText(text)) {
      return false;
    }
    String t = text.trim();
    // 去掉空白与常见标点再匹配
    String compact = t.replaceAll("[\\s\\p{Punct}？?！!。．.、，,]", "");
    if (containsAny(compact,
        "能做什么", "都可以做", "可以做什么", "你会什么", "你会做",
        "有什么功能", "有哪些功能", "都能做", "会什么", "能干啥",
        "你是谁", "介绍一下你", "你是干什么", "你是干嘛", "怎么用你",
        "你会干啥", "你能帮", "帮我什么", "你可以做什么", "你都能做")) {
      return true;
    }
    // 「你…做什么/干啥」且带能/会/都 → 能力介绍，不是今日任务
    return compact.contains("你")
        && containsAny(compact, "做什么", "做甚么", "干啥", "干嘛")
        && containsAny(compact, "能", "会", "都", "可以");
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

  private record ClassCertFacts(
      String classTitle,
      String workTypeTitle,
      Integer workTypeLevel,
      String applyLevelLabel,
      String levelSource,
      String heldLevelLabel,
      String examTitle,
      String examDate) {
    Map<String, Object> toMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("classTitle", classTitle);
      m.put("workTypeTitle", workTypeTitle);
      m.put("workTypeLevel", workTypeLevel);
      m.put("applyLevelLabel", applyLevelLabel);
      m.put("levelSource", levelSource);
      m.put("heldLevelLabel", heldLevelLabel);
      m.put("examTitle", examTitle);
      m.put("examDate", examDate);
      return m;
    }
  }

  private record TaskChannelSummary(
      int total,
      int pending,
      int done,
      int passed,
      List<String> pendingTitles,
      List<String> doneLines) {
    static TaskChannelSummary empty() {
      return new TaskChannelSummary(0, 0, 0, 0, List.of(), List.of());
    }

    Map<String, Object> toMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("total", total);
      m.put("pending", pending);
      m.put("done", done);
      m.put("passed", passed);
      m.put("pendingTitles", pendingTitles);
      m.put("doneLines", doneLines);
      return m;
    }
  }

  /** 学员在本班的花名册成绩 + 作业/模考汇总。 */
  private record StudentClassProfile(
      String realname,
      String phone,
      BigDecimal examScore,
      BigDecimal theoryScore,
      BigDecimal realScore,
      BigDecimal normalScore,
      Integer graduateStatus,
      TaskChannelSummary homework,
      TaskChannelSummary mock) {
    Map<String, Object> toMap() {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("realname", realname);
      m.put("phone", phone);
      m.put("examScore", examScore);
      m.put("theoryScore", theoryScore);
      m.put("realScore", realScore);
      m.put("normalScore", normalScore);
      m.put("graduateStatus", graduateStatus);
      m.put("homework", homework == null ? TaskChannelSummary.empty().toMap() : homework.toMap());
      m.put("mock", mock == null ? TaskChannelSummary.empty().toMap() : mock.toMap());
      return m;
    }
  }

  private record ReadinessMetrics(
      Integer accuracy,
      String accuracySource,
      Integer coveragePercent,
      Integer dayIndex,
      Integer totalDays,
      int weakCount,
      List<String> weakTitles,
      int wrongBookCount,
      Integer weekAnswered,
      Integer weekWrongRate,
      int specialAnswered,
      Integer specialWrongRate,
      int red,
      int yellow,
      List<String> recentWrongStems,
      boolean ready,
      boolean borderline) {}

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
