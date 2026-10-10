package com.guoyu.platform.certification.collection.vo;

import java.util.List;

public record CollectionWorkbenchVO(
    CollectionTaskVO task,
    CollectionStatsVO stats,
    List<CandidateCollectionItemVO> candidates,
    List<QueueItemVO> queue,
    CandidateCollectionDetailVO currentCandidate
) {
}
