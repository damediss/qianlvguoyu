import { describe, expect, it } from 'vitest';
import type { CompetitionStage } from '../src/types/competition';
import {
  autoLayoutStageGraph,
  buildStageSavePayload,
  canConnectStage,
  cleanStageNotes,
  toStageGraph,
  validateStageGraph,
} from '../src/pages/competition/stage-planning/stageGraphUtils';
import {
  readStageCanvasNotes,
  stageCanvasStorageKey,
  writeStageCanvasNotes,
} from '../src/pages/competition/stage-planning/stageCanvasStorage';
import { isBusinessStageNode } from '../src/types/competitionStagePlanning';

function stage(stageType: CompetitionStage['stageType'], sortOrder: number, id: number): CompetitionStage {
  return {
    id,
    eventId: 1001,
    name: stageType,
    stageType,
    sortOrder,
    status: 'draft',
    startAt: null,
    endAt: null,
    description: '说明',
  };
}

describe('competition stage graph utilities', () => {
  it('converts selected stages into stable nodes and adjacent edges while skipping stages', () => {
    const graph = toStageGraph([
      stage('preliminary', 20, 5001),
      stage('semifinal', 40, 5002),
      stage('final', 50, 5003),
    ]);

    expect(graph.nodes.filter(isBusinessStageNode).map((node) => node.id))
      .toEqual(['stage:preliminary', 'stage:semifinal', 'stage:final']);
    expect(graph.edges.filter((edge) => edge.data?.business).map((edge) => edge.id))
      .toEqual(['edge:preliminary:semifinal', 'edge:semifinal:final']);
  });

  it('lays business nodes horizontally by fixed stage order', () => {
    const graph = toStageGraph([
      stage('final', 50, 5003),
      stage('preliminary', 20, 5001),
    ]);
    const moved = autoLayoutStageGraph(graph.nodes);
    expect(moved.find((node) => node.id === 'stage:preliminary')?.position.x).toBe(120);
    expect(moved.find((node) => node.id === 'stage:final')?.position.x).toBe(380);
  });

  it('builds a backend payload without markers, coordinates or edges', () => {
    const graph = toStageGraph([stage('preliminary', 20, 5001)]);
    const payload = buildStageSavePayload(graph.nodes);
    expect(payload).toEqual({
      stages: [{
        id: '5001',
        stageType: 'preliminary',
        description: '说明',
      }],
    });
  });

  it('includes locally edited stage content in the draft payload', () => {
    const graph = toStageGraph([stage('preliminary', 20, 5001)]);
    const payload = buildStageSavePayload(graph.nodes.map((node) => node.id === 'stage:preliminary'
      ? { ...node, data: { ...node.data, contents: [{ contentType: 'theory' as const, weight: 100 }] } }
      : node));

    expect(payload.stages[0].contents).toEqual([{ contentType: 'theory', weight: 100 }]);
  });

  it('returns all blocking and warning validation issues', () => {
    const graph = toStageGraph([stage('preliminary', 20, 5001), stage('final', 50, 5002)]);
    const issues = validateStageGraph(
      graph.nodes.map((node) => node.id === 'stage:final'
        ? { ...node, data: { ...node.data, startAt: '2099-01-01T11:00:00', endAt: '2099-01-01T10:00:00', description: '' } }
        : node),
      graph.edges.filter((edge) => edge.data?.business === false || edge.id !== 'edge:preliminary:final'),
    );
    expect(issues.map((item) => item.code)).toEqual(expect.arrayContaining([
      'STAGE_EDGE_MISSING',
      'STAGE_TIME_RANGE_INVALID',
      'STAGE_DESCRIPTION_EMPTY',
    ]));
  });

  it('rejects stage times outside the competition time window', () => {
    const graph = toStageGraph([stage('preliminary', 20, 5001)]);
    const issues = validateStageGraph(
      graph.nodes.map((node) => node.id === 'stage:preliminary'
        ? {
          ...node,
          data: {
            ...node.data,
            startAt: '2026-08-01T08:59:59',
            endAt: '2026-08-10T18:00:01',
          },
        }
        : node),
      graph.edges,
      '2026-08-01T09:00:00',
      '2026-08-10T18:00:00',
    );

    expect(issues).toEqual(expect.arrayContaining([
      expect.objectContaining({
        code: 'STAGE_TIME_OUTSIDE_EVENT',
        message: '赛段开始时间不能早于赛事开始时间',
      }),
    ]));
    expect(issues).toEqual(expect.arrayContaining([
      expect.objectContaining({
        code: 'STAGE_TIME_OUTSIDE_EVENT',
        message: '赛段结束时间不能晚于赛事结束时间',
      }),
    ]));
  });

  it('rejects self, reverse, duplicate and branching connections', () => {
    const graph = toStageGraph([stage('preliminary', 20, 5001), stage('final', 50, 5002)]);
    expect(canConnectStage({ source: 'stage:final', target: 'stage:preliminary' }, graph.nodes, graph.edges)).toBe(false);
    expect(canConnectStage({ source: 'stage:preliminary', target: 'stage:preliminary' }, graph.nodes, graph.edges)).toBe(false);
    expect(canConnectStage({ source: 'stage:preliminary', target: 'stage:final' }, graph.nodes, graph.edges)).toBe(false);
  });

  it('serializes local notes and safely falls back for corrupt data', () => {
    writeStageCanvasNotes(1, 1001, { 'preliminary->final': '晋级说明' });
    expect(readStageCanvasNotes(1, 1001)).toEqual({ 'preliminary->final': '晋级说明' });
    localStorage.setItem(stageCanvasStorageKey(1, 1001), '{broken');
    expect(readStageCanvasNotes(1, 1001)).toEqual({});
  });

  it('keeps local notes separate for each work type and level', () => {
    writeStageCanvasNotes(1, 1001, { 'preliminary->final': '电工初级' }, '2001', '5');
    writeStageCanvasNotes(1, 1001, { 'preliminary->final': '电工中级' }, '2001', '4');

    expect(readStageCanvasNotes(1, 1001, '2001', '5')).toEqual({ 'preliminary->final': '电工初级' });
    expect(readStageCanvasNotes(1, 1001, '2001', '4')).toEqual({ 'preliminary->final': '电工中级' });
    expect(stageCanvasStorageKey(1, 1001, '2001', '5'))
      .not.toBe(stageCanvasStorageKey(1, 1001, '2002', '5'));
  });

  it('cleans notes for deleted edges', () => {
    const graph = toStageGraph([stage('preliminary', 20, 5001), stage('final', 50, 5002)]);
    const notes = cleanStageNotes(graph.nodes, graph.edges, { 'preliminary->final': '保留' });
    expect(notes).toEqual({ 'preliminary->final': '保留' });
    expect(cleanStageNotes(graph.nodes, graph.edges.filter((edge) => edge.id !== 'edge:preliminary:final'), {
      'preliminary->final': '悬空',
    })).toEqual({});
  });
});
