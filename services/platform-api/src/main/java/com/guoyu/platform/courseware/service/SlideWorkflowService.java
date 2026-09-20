package com.guoyu.platform.courseware.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guoyu.platform.courseware.domain.CoursewareLayout;
import com.guoyu.platform.courseware.domain.CoursewareModels.AssembledSlide;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideGenInput;
import com.guoyu.platform.courseware.domain.CoursewareModels.SlideWorkflowResult;
import com.guoyu.platform.courseware.domain.SlideAssembler;
import com.guoyu.platform.courseware.domain.SlideValidator;
import com.guoyu.platform.courseware.domain.StructureIssue;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 单页生成自纠环(方案 §8.1):生成 → 确定性校验 →(值得修)修复 1 轮。
 * JSON 契约失败视同一轮修复(输出零信任);修复只是把问题文本回喂 prompt 重生成。
 */
@Service
public class SlideWorkflowService {

  /** 最多修复 1 轮(方案 §8.1) */
  static final int MAX_REPAIR_ROUNDS = 1;

  private final CoursewareAiService aiService;
  private final ObjectMapper objectMapper;

  public SlideWorkflowService(CoursewareAiService aiService, ObjectMapper objectMapper) {
    this.aiService = aiService;
    this.objectMapper = objectMapper;
  }

  /**
   * 执行单页工作流。LLM 调用异常(FaqLlmException 等)直接向上抛,由编排层计页失败。
   */
  public SlideWorkflowResult execute(SlideGenInput input, Long tenantId, Long projectId,
                                     Long operatorId, Long slideId) {
    CoursewareLayout.Layout layout = CoursewareLayout.require(input.layoutId());
    List<String> issues = List.of();
    int round = 0;
    while (true) {
      SlideGenInput attemptInput = input.withIssues(issues);
      String content = aiService.callSlide(tenantId, projectId, slideId, operatorId,
          CoursewarePrompts.slideSystem(layout, objectMapper),
          CoursewarePrompts.slideUser(attemptInput, objectMapper));

      List<StructureIssue> roundIssues = new ArrayList<>();
      SlideAssembler.ParsedDraft draft;
      try {
        draft = SlideAssembler.parseDraft(content, objectMapper);
      } catch (IllegalArgumentException e) {
        // JSON 契约失败 = 一轮修复(输出零信任,方案 §12)
        roundIssues.add(StructureIssue.error(null, "输出不是合法 JSON: " + e.getMessage()));
        if (round >= MAX_REPAIR_ROUNDS) {
          throw new IllegalStateException("单页生成两轮输出均不合法: " + e.getMessage());
        }
        issues = describe(roundIssues);
        round++;
        continue;
      }

      AssembledSlide assembled = SlideAssembler.assemble(draft.rawBlocks(), layout);
      roundIssues.addAll(assembled.issues());
      roundIssues.addAll(SlideValidator.validate(assembled.blocks(), layout, input.pageRole(),
          input.contentDensity()));

      boolean repairWorthy = roundIssues.stream().anyMatch(StructureIssue::isRepairWorthy);
      if (!repairWorthy || round >= MAX_REPAIR_ROUNDS) {
        return new SlideWorkflowResult(assembled.blocks(), draft.speakerNotes(), roundIssues,
            round);
      }
      issues = describe(roundIssues);
      round++;
    }
  }

  /** 问题 → 修复提示文本(槽位 xx:消息 / 本页:消息) */
  static List<String> describe(List<StructureIssue> issues) {
    List<String> out = new ArrayList<>();
    for (StructureIssue issue : issues) {
      String scope = issue.slotId() != null ? "槽位 " + issue.slotId() : "本页";
      out.add(scope + ":" + issue.message());
    }
    return out;
  }
}
