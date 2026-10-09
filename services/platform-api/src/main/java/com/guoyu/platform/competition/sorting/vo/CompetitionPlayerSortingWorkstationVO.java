package com.guoyu.platform.competition.sorting.vo;

import java.util.List;

/** 一个固定工位及其绑定的子工位编号。抽中工位时，子工位一并带出。 */
public record CompetitionPlayerSortingWorkstationVO(
    String code,
    List<String> subCodes) {
}
