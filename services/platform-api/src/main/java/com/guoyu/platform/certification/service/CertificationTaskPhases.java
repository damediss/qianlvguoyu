package com.guoyu.platform.certification.service;

import java.util.List;
import java.util.Set;

/**
 * 任务阶段（certification_task.current_phase）枚举。
 * 任务阶段按四步推进：任务创建 -> 认定筹备 -> 认定归档 -> 认定完成。
 *
 * @see CertificationTaskStatuses
 */
public final class CertificationTaskPhases {

  public static final String TASK_CREATION = "task_creation";
  public static final String PREPARATION = "preparation";
  public static final String ARCHIVE = "archive";
  public static final String CERTIFICATION_COMPLETED = "certification_completed";

  public static final Set<String> PHASES = Set.of(
      TASK_CREATION,
      PREPARATION,
      ARCHIVE,
      CERTIFICATION_COMPLETED
  );

  public static final List<String> ORDER = List.of(
      TASK_CREATION,
      PREPARATION,
      ARCHIVE,
      CERTIFICATION_COMPLETED
  );

  public static String phaseName(String phase) {
    return switch (phase == null ? "" : phase) {
      case TASK_CREATION -> "任务创建阶段";
      case PREPARATION -> "认定筹备阶段";
      case ARCHIVE -> "认定归档阶段";
      case CERTIFICATION_COMPLETED -> "认定完成";
      default -> phase;
    };
  }

  public static boolean isValid(String phase) {
    return phase != null && PHASES.contains(phase);
  }

  public static int orderOf(String phase) {
    int idx = ORDER.indexOf(phase);
    return idx < 0 ? 0 : idx + 1;
  }

  private CertificationTaskPhases() {
  }
}
