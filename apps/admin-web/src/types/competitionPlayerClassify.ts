import type { CompetitionId, CompetitionStagePaperAssignmentPlayer } from './competition';

export interface CompetitionPlayerClassifyTask {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  stageName?: string | null;
  typeCount: number;
  playerCount: number;
  status: 'classified' | string;
  createdAt?: string | null;
  typeNames?: string[] | null;
  typeSizes?: number[] | null;
  assignments?: CompetitionPlayerClassifyAssignment[];
}

export interface CompetitionPlayerClassifyAssignment {
  contestantProfileId: CompetitionId;
  contestantNo?: string | null;
  name?: string | null;
  orgName?: string | null;
  typeIndex: number;
  typeName: string;
  orderInType: number;
}

export interface CompetitionPlayerClassifyCreateRequest {
  typeNames: string[];
  contestantProfileIds: Array<string | number>;
}

export type CompetitionPlayerClassifyCandidate = CompetitionStagePaperAssignmentPlayer;
