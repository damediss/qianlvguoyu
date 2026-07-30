package com.guoyu.platform.competition.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;
import java.math.BigDecimal;

@TableName("competition_ranking_entry")
public class CompetitionRankingEntryEntity extends BaseEntity {

  private Long rankingSnapshotId;
  private Long contestantMemberId;
  private Long totalScoreSnapshotId;
  private BigDecimal totalScore;
  private Integer displayRank;
  private String tieKey;
  private Integer tiedFlag;
  private String contestantStatus;
  private Integer sortOrder;

  public Long getRankingSnapshotId() { return rankingSnapshotId; }
  public void setRankingSnapshotId(Long rankingSnapshotId) {
    this.rankingSnapshotId = rankingSnapshotId;
  }
  public Long getContestantMemberId() { return contestantMemberId; }
  public void setContestantMemberId(Long contestantMemberId) {
    this.contestantMemberId = contestantMemberId;
  }
  public Long getTotalScoreSnapshotId() { return totalScoreSnapshotId; }
  public void setTotalScoreSnapshotId(Long totalScoreSnapshotId) {
    this.totalScoreSnapshotId = totalScoreSnapshotId;
  }
  public BigDecimal getTotalScore() { return totalScore; }
  public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
  public Integer getDisplayRank() { return displayRank; }
  public void setDisplayRank(Integer displayRank) { this.displayRank = displayRank; }
  public String getTieKey() { return tieKey; }
  public void setTieKey(String tieKey) { this.tieKey = tieKey; }
  public Integer getTiedFlag() { return tiedFlag; }
  public void setTiedFlag(Integer tiedFlag) { this.tiedFlag = tiedFlag; }
  public String getContestantStatus() { return contestantStatus; }
  public void setContestantStatus(String contestantStatus) {
    this.contestantStatus = contestantStatus;
  }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
