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
  status: 'created' | 'ordered' | 'drawn' | 'sorted' | string;
  createdAt?: string | null;
  assignments?: CompetitionPlayerSortingAssignment[];
  groupingMode?: 'company' | 'balanced' | string | null;
  workstationLabel?: string | null;
  workstationCodes?: string[] | null;
  roleOptions?: string[] | null;
  members?: CompetitionPlayerSortingMember[];
  subWorkstationLabel?: string | null;
  subWorkstationCodes?: string[] | null;
  stations?: CompetitionPlayerSortingStation[] | null;
  groupOrderShuffled?: boolean | null;
}

export interface CompetitionPlayerSortingStation {
  code: string;
  subCodes: string[];
}

export interface CompetitionPlayerSortingMember {
  contestantProfileId: CompetitionId;
  orgName?: string | null;
  role?: string | null;
  groupNo?: number | null;
  groupName?: string | null;
  drawOrder?: number | null;
}

export interface CompetitionPlayerSortingAssignment {
  contestantProfileId: CompetitionId;
  contestantNo?: string | null;
  name?: string | null;
  mobile?: string | null;
  groupNo: number;
  groupOrder: number;
  workstation?: string | null;
  groupLeader?: boolean | null;
  orgName?: string | null;
  groupName?: string | null;
  role?: string | null;
  subWorkstation?: string | null;
}

export interface CompetitionPlayerSortingTaskCreateRequest {
  contestantProfileIds: CompetitionId[];
}

export interface CompetitionPlayerSortingTaskSelectionRequest {
  contestantProfileIds: CompetitionId[];
}

export interface CompetitionPlayerSortingGroupsRequest {
  groups: Array<{ groupName: string; contestantProfileIds: CompetitionId[] }>;
}

export interface CompetitionPlayerSortingRolesRequest {
  roleOptions: string[];
  members: Array<{ contestantProfileId: CompetitionId; role: string }>;
}

export interface CompetitionPlayerSortingWorkstationsRequest {
  label: string;
  subLabel: string;
  stations: CompetitionPlayerSortingStation[];
}

export interface CompetitionPlayerSortingReveal {
  task: CompetitionPlayerSortingTask;
  contestantProfileId?: string | null;
  name?: string | null;
  groupName?: string | null;
  orgName?: string | null;
  drawOrder?: number | null;
  leader?: boolean | null;
  workstationLabel?: string | null;
  workstation?: string | null;
  subWorkstationLabel?: string | null;
  subWorkstation?: string | null;
  groupCompleted: boolean;
  allCompleted: boolean;
}

export type CompetitionPlayerSortingCandidate = CompetitionStagePaperAssignmentPlayer;
