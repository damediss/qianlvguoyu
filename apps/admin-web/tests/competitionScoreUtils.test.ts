import assert from 'node:assert/strict';
import test from 'node:test';
import type {
  CompetitionScoreSource,
  CompetitionTotalScoreSnapshot,
} from '../src/types/competition';
import {
  buildRankingCandidates,
  findCalculationReadyContestants,
  rankingEntryPageItems,
} from '../src/pages/competition/competitionScoreUtils';

function scoreSource(
  contestantMemberId: string,
  sourceType: CompetitionScoreSource['sourceType'],
  confirmationStatus: CompetitionScoreSource['confirmationStatus'] = 'confirmed',
): CompetitionScoreSource {
  return {
    id: `${contestantMemberId}-${sourceType}`,
    eventId: '1',
    stageId: '2',
    eventWorkTypeId: '3',
    contestantMemberId,
    sourceType,
    rawScore: 80,
    confirmationStatus,
    sourceSystem: 'test',
    scoreRuleId: '4',
    revisionNo: 1,
    currentFlag: 1,
  };
}

function totalSnapshot(
  id: string,
  contestantMemberId: string,
  totalScore: number,
  practicalScore: number,
  simulationScore: number,
  theoryScore: number,
): CompetitionTotalScoreSnapshot {
  return {
    id,
    eventId: '1',
    stageId: '2',
    eventWorkTypeId: '3',
    contestantMemberId,
    scoreRuleId: '4',
    theorySourceId: '10',
    simulationSourceId: '11',
    practicalSourceId: '12',
    theoryScore,
    simulationScore,
    practicalScore,
    simulationPracticalScore: 80,
    theoryWeight: 40,
    simulationPracticalWeight: 60,
    totalScore,
    calculationVersion: 1,
    confirmationStatus: 'confirmed',
    currentFlag: 1,
  };
}

test('only contestants with three current confirmed sources are calculation-ready', () => {
  const sources = [
    scoreSource('100', 'theory'),
    scoreSource('100', 'simulation'),
    scoreSource('100', 'practical'),
    scoreSource('200', 'theory'),
    scoreSource('200', 'simulation', 'pending'),
    scoreSource('200', 'practical'),
  ];

  assert.deepEqual(findCalculationReadyContestants(sources), ['100']);
});

test('ranking candidates default to total score only and leave tie keys for explicit review', () => {
  const candidates = buildRankingCandidates([
    totalSnapshot('31', '300', 80, 90, 70, 80),
    totalSnapshot('11', '100', 90, 88, 85, 95),
    totalSnapshot('21', '200', 90, 90, 80, 95),
    totalSnapshot('41', '400', 90, 90, 80, 95),
  ]);

  assert.deepEqual(
    candidates.map((candidate) => candidate.contestantMemberId),
    ['100', '200', '400', '300'],
  );
  assert.ok(candidates.every((candidate) => candidate.tieKey === undefined));
});

test('pending, superseded and duplicate older total snapshots are excluded', () => {
  const older = totalSnapshot('51', '500', 70, 70, 70, 70);
  older.currentFlag = 0;
  const pending = totalSnapshot('61', '600', 99, 99, 99, 99);
  pending.confirmationStatus = 'pending';

  assert.deepEqual(
    buildRankingCandidates([older, pending]),
    [],
  );
});

test('ranking entries consume the backend PageResponse items field', () => {
  const entries = rankingEntryPageItems({
    items: [{
      id: '701',
      rankingSnapshotId: '700',
      contestantMemberId: '100',
      totalScoreSnapshotId: '11',
      totalScore: 90,
      displayRank: 1,
      tieKey: 'verified-key',
      tiedFlag: 0,
      contestantStatus: 'completed',
      sortOrder: 1,
    }],
    page: 1,
    pageSize: 500,
    total: 1,
  });

  assert.equal(entries.length, 1);
  assert.equal(entries[0].contestantMemberId, '100');
  assert.deepEqual(rankingEntryPageItems(undefined), []);
});
