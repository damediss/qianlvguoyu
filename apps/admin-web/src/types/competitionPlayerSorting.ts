import type { CompetitionId, CompetitionStagePaperAssignmentPlayer } from './competition';

export interface CompetitionPlayerSortingTask {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  stageName?: string | null;
  groupCount: number;
  workstationCount: number;
  playersPerGroup: number;
  playerCount: number;
  status: 'created' | 'sorted' | string;
  createdAt?: string | null;
  assignments?: CompetitionPlayerSortingAssignment[];
}

export interface CompetitionPlayerSortingAssignment {
  contestantProfileId: CompetitionId;
  contestantNo?: string | null;
  name?: string | null;
  mobile?: string | null;
  groupNo: number;
  groupOrder: number;
  workstation: string;
}

export interface CompetitionPlayerSortingTaskCreateRequest {
  groupCount: number;
  workstationCount: number;
  playersPerGroup: number;
  contestantProfileIds: CompetitionId[];
}

export interface CompetitionPlayerSortingTaskSelectionRequest {
  contestantProfileIds: CompetitionId[];
}

export type CompetitionPlayerSortingCandidate = CompetitionStagePaperAssignmentPlayer;
