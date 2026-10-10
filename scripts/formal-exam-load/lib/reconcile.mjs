import { query } from './mysql.mjs';
import { outboxIntegrityFailures } from './seq-semantics.mjs';

export async function reconcile(connection, fixture, padSnapshots = []) {
  const failures = [];
  const notes = [];
  const sessionId = fixture.sessionId;
  const expected = fixture.pads.length || fixture.pads;

  const candidates = await query(
    connection,
    `SELECT status, COUNT(*) AS c FROM competition_formal_candidate_exam
     WHERE exam_session_id = ? AND deleted = 0 GROUP BY status`,
    [sessionId],
  );
  const byStatus = Object.fromEntries((candidates || []).map((row) => [row.status, Number(row.c)]));
  const total = Object.values(byStatus).reduce((sum, value) => sum + value, 0);
  if (total !== expected) {
    failures.push(`candidate total ${total} != expected ${expected}`);
  }

  const answers = await query(
    connection,
    `SELECT candidate_exam_id, question_snapshot_id, COUNT(*) AS c
     FROM competition_formal_candidate_answer
     WHERE exam_session_id = ? AND deleted = 0
     GROUP BY candidate_exam_id, question_snapshot_id
     HAVING COUNT(*) > 1`,
    [sessionId],
  );
  if (answers.length) {
    failures.push(`duplicate answers ${answers.length}`);
  }

  let versionMismatches = 0;
  let barrierPendingPads = 0;
  for (const pad of padSnapshots) {
    if (pad?.syncBarrier && !pad.syncBarrier.ok) {
      barrierPendingPads += 1;
    }
    if (!pad?.lastAcked) continue;
    for (const [questionId, version] of Object.entries(pad.lastAcked)) {
      const rows = await query(
        connection,
        `SELECT answer_version FROM competition_formal_candidate_answer
         WHERE candidate_exam_id = ? AND question_snapshot_id = ? AND deleted = 0 LIMIT 1`,
        [pad.candidateExamId, questionId],
      );
      const dbVersion = rows[0] ? Number(rows[0].answer_version) : 0;
      if (dbVersion !== Number(version)) {
        versionMismatches += 1;
      }
    }
  }
  if (barrierPendingPads) {
    failures.push(`answer sync barrier pending pads ${barrierPendingPads}`);
  }
  if (versionMismatches) {
    failures.push(`answerVersion mismatches ${versionMismatches}`);
  }

  const sessionRows = await query(
    connection,
    'SELECT status, submitted_total, candidate_total, actual_end_time, scoring_status FROM competition_formal_exam_session WHERE id = ?',
    [sessionId],
  );
  const session = sessionRows[0] || {};
  const terminal = ['submitted', 'auto_submitted', 'forced_submitted', 'absent'];
  const terminalCount = terminal.reduce((sum, status) => sum + (byStatus[status] || 0), 0);
  if (session.submitted_total != null && Number(session.submitted_total) !== terminalCount) {
    notes.push(`submitted_total=${session.submitted_total} vs terminal aggregate=${terminalCount}`);
  }
  if (session.status === 'finished' && !session.actual_end_time) {
    failures.push('finished session missing actual_end_time');
  }

  const outboxDropped = await query(
    connection,
    `SELECT COUNT(*) AS c FROM competition_formal_exam_outbox
     WHERE exam_session_id = ? AND status = 'dropped' AND deleted = 0`,
    [sessionId],
  );
  const dropped = Number(outboxDropped[0]?.c || 0);

  const seqDup = await query(
    connection,
    `SELECT seq, COUNT(*) AS c FROM competition_formal_exam_outbox
     WHERE exam_session_id = ? AND deleted = 0 GROUP BY seq HAVING COUNT(*) > 1`,
    [sessionId],
  );
  const eventDup = await query(
    connection,
    `SELECT event_id, COUNT(*) AS c FROM competition_formal_exam_outbox
     WHERE exam_session_id = ? AND deleted = 0 GROUP BY event_id HAVING COUNT(*) > 1`,
    [sessionId],
  );
  failures.push(...outboxIntegrityFailures({ dropped, seqDup, eventDup }));

  let outOfOrder = 0;
  let sequenceCollisions = 0;
  let missedRelevantEvents = 0;
  for (const pad of padSnapshots) {
    outOfOrder += Number(pad?.outOfOrder || 0);
    sequenceCollisions += Number(pad?.sequenceCollisions || 0);
    missedRelevantEvents += Number(pad?.missedRelevantEvents || 0);
  }
  if (outOfOrder) failures.push(`outOfOrder=${outOfOrder}`);
  if (sequenceCollisions) failures.push(`sequenceCollisions=${sequenceCollisions}`);
  if (missedRelevantEvents) failures.push(`missedRelevantEvents=${missedRelevantEvents}`);

  const commands = await query(
    connection,
    `SELECT status, COUNT(*) AS c FROM competition_formal_exam_access_command
     WHERE exam_session_id = ? AND deleted = 0 GROUP BY status`,
    [sessionId],
  );
  const commandByStatus = Object.fromEntries((commands || []).map((row) => [row.status, Number(row.c)]));

  const scoring = await query(
    connection,
    `SELECT result_status, COUNT(*) AS c FROM competition_formal_candidate_result
     WHERE exam_session_id = ? AND deleted = 0 GROUP BY result_status`,
    [sessionId],
  );

  return {
    ok: failures.length === 0,
    failures,
    notes,
    expectedCandidates: expected,
    candidateByStatus: byStatus,
    terminalCount,
    submittedTotal: session.submitted_total,
    sessionStatus: session.status,
    scoringStatus: session.scoring_status,
    actualEndTime: session.actual_end_time,
    outboxDropped: dropped,
    commandByStatus,
    scoringByStatus: Object.fromEntries((scoring || []).map((row) => [row.result_status, Number(row.c)])),
    versionMismatches,
  };
}
