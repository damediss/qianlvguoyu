import type {
  CompetitionRankingEntry,
  CompetitionId,
  CompetitionRankingCandidate,
  CompetitionScoreSource,
  CompetitionScoreSourceType,
  CompetitionTotalScoreSnapshot,
} from '../../types/competition';
import type { PageResponse } from '../../types/api';

const REQUIRED_SOURCE_TYPES: CompetitionScoreSourceType[] = ['theory', 'simulation', 'practical'];

function decimalValue(value: number | string) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function compareDecimalDesc(
  left: number | string,
  right: number | string,
) {
  return decimalValue(right) - decimalValue(left);
}

export function findCalculationReadyContestants(
  sources: CompetitionScoreSource[],
): CompetitionId[] {
  const sourceTypesByContestant = new Map<string, Set<CompetitionScoreSourceType>>();

  sources
    .filter((source) => source.currentFlag === 1 && source.confirmationStatus === 'confirmed')
    .forEach((source) => {
      const contestantId = String(source.contestantMemberId);
      const sourceTypes = sourceTypesByContestant.get(contestantId) || new Set();
      sourceTypes.add(source.sourceType);
      sourceTypesByContestant.set(contestantId, sourceTypes);
    });

  return [...sourceTypesByContestant.entries()]
    .filter(([, sourceTypes]) => REQUIRED_SOURCE_TYPES.every((type) => sourceTypes.has(type)))
    .map(([contestantId]) => contestantId);
}

export function buildRankingCandidates(
  snapshots: CompetitionTotalScoreSnapshot[],
): CompetitionRankingCandidate[] {
  const currentConfirmedByContestant = new Map<string, CompetitionTotalScoreSnapshot>();

  snapshots
    .filter((snapshot) => snapshot.currentFlag === 1 && snapshot.confirmationStatus === 'confirmed')
    .forEach((snapshot) => {
      const contestantId = String(snapshot.contestantMemberId);
      const current = currentConfirmedByContestant.get(contestantId);
      if (!current || snapshot.calculationVersion > current.calculationVersion) {
        currentConfirmedByContestant.set(contestantId, snapshot);
      }
    });

  return [...currentConfirmedByContestant.values()]
    .sort((left, right) =>
      compareDecimalDesc(left.totalScore, right.totalScore)
      || String(left.contestantMemberId).localeCompare(String(right.contestantMemberId)))
    .map((snapshot) => ({
      contestantMemberId: snapshot.contestantMemberId,
      totalScoreSnapshotId: snapshot.id,
      contestantStatus: 'completed',
    }));
}

export function rankingEntryPageItems(
  page?: PageResponse<CompetitionRankingEntry>,
) {
  return page?.items || [];
}
