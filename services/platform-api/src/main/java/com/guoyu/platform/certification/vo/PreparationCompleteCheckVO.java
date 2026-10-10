package com.guoyu.platform.certification.vo;

import java.util.List;

public record PreparationCompleteCheckVO(boolean passed, List<PreparationCheckItemVO> items) {
}
