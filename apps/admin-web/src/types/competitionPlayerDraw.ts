import type { CompetitionId, CompetitionStagePaperAssignmentPlayer } from './competition';

export interface CompetitionPlayerDrawTask {
  id: CompetitionId;
  eventId: CompetitionId;
  stageId: CompetitionId;
  stageName?: string | null;
  playerCount: number;
  drawnCount: number;
  status: 'created' | 'distributed' | 'drawing' | 'drawn' | string;
  createdAt?: string | null;
  slots?: CompetitionPlayerDrawSlot[];
}

export interface CompetitionPlayerDrawSlot {
  contestantProfileId: CompetitionId;
  contestantNo?: string | null;
  name?: string | null;
  orgName?: string | null;
  mobile?: string | null;
  drawOrder: number;
  appearanceOrder?: number | null;
}

export interface CompetitionPlayerDrawTaskSelectionRequest {
  contestantProfileIds: CompetitionId[];
}

export type CompetitionPlayerDrawCandidate = CompetitionStagePaperAssignmentPlayer;
