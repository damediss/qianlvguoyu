package com.guoyu.platform.industrial.service;

import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.industrial.dto.ProjectResourceQueryReq;
import com.guoyu.platform.industrial.vo.ProjectResourceVO;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectResourceApplicationService {

  private static final String DATE = "2025-06-10";
  private static final String SOURCE = "千铝工投";
  private static final String PASS_STATE = "通过";
  private static final String RUNNING_STATE = "进行中";
  private static final String BASE_NAME = "铝电解槽操作工";
  private static final String PREVIEW_URL =
      "https://cdn.guoyu.local/upload/common/20250810/40303771284689834d679802.pptx";
  private static final List<String> LEVELS = List.of("高级技师", "技师", "高级", "中级", "初级");
  private static final List<String> TYPES = List.of("course", "teach", "exam", "train", "criterion");
  private static final Map<String, String> TYPE_PREFIX = Map.of(
      "course", "course",
      "teach", "teach",
      "exam", "exam",
      "train", "train",
      "criterion", "criterion"
  );

  @Transactional(readOnly = true)
  public PageResponse<ProjectResourceVO> page(String type, ProjectResourceQueryReq req) {
    String normalizedType = normalizeType(type);
    List<ProjectResourceVO> filtered = baseData(normalizedType).stream()
        .filter(matches(req))
        .toList();

    int fromIndex = (int) Math.min((req.getPage() - 1) * req.getPageSize(), filtered.size());
    int toIndex = (int) Math.min(fromIndex + req.getPageSize(), filtered.size());
    return new PageResponse<>(filtered.subList(fromIndex, toIndex), req.getPage(), req.getPageSize(), filtered.size());
  }

  private Predicate<ProjectResourceVO> matches(ProjectResourceQueryReq req) {
    return item -> containsAny(item, req.getKeyword())
        && matchesText(item.state(), req.getState())
        && matchesText(item.source(), req.getSource())
        && matchesText(item.company(), req.getCompany());
  }

  private boolean containsAny(ProjectResourceVO item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String normalized = keyword.toLowerCase(Locale.ROOT);
    return contains(item.name(), normalized)
        || contains(item.state(), normalized)
        || contains(item.source(), normalized)
        || contains(item.company(), normalized);
  }

  private boolean matchesText(String value, String expected) {
    if (!StringUtils.hasText(expected)) {
      return true;
    }
    return contains(value, expected.toLowerCase(Locale.ROOT));
  }

  private boolean contains(String value, String expected) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(expected);
  }

  private List<ProjectResourceVO> baseData(String type) {
    return switch (type) {
      case "course", "teach" -> withPreview(type);
      case "exam" -> IntStream.range(0, 10)
          .mapToObj(index -> item(type, BASE_NAME + level(index), PASS_STATE, SOURCE, null, null, null, index))
          .toList();
      case "train" -> IntStream.range(0, 10)
          .mapToObj(index -> item(type, BASE_NAME + level(index) + "实训中心", RUNNING_STATE, null, SOURCE, null, null, index))
          .toList();
      case "criterion" -> IntStream.range(0, 10)
          .mapToObj(index -> item(type, BASE_NAME, RUNNING_STATE, null, SOURCE, null, null, index))
          .toList();
      default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "开发工程资源类型不正确");
    };
  }

  private List<ProjectResourceVO> withPreview(String type) {
    return IntStream.range(0, 10)
        .mapToObj(index -> item(type, BASE_NAME + level(index), PASS_STATE, SOURCE, null, PREVIEW_URL, "pptx", index))
        .toList();
  }

  private String level(int index) {
    return LEVELS.get(index % LEVELS.size());
  }

  private ProjectResourceVO item(
      String type,
      String name,
      String state,
      String source,
      String company,
      String preview,
      String fileType,
      int index
  ) {
    int hash = Math.abs((type + name + state + index).hashCode());
    return new ProjectResourceVO(
        TYPE_PREFIX.get(type) + "-" + hash,
        type,
        DATE,
        name,
        state,
        source,
        company,
        preview,
        fileType
    );
  }

  private String normalizeType(String type) {
    if (!TYPES.contains(type)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "开发工程资源类型不正确");
    }
    return type;
  }
}
