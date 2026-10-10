import fs from 'node:fs';
import path from 'node:path';
import { bcryptHash, sha256Hex, syntheticIdCard, syntheticMobile } from './crypto.mjs';
import { Kind, loadId } from './ids.mjs';
import { openMysql, upsertById, query, hasTable } from './mysql.mjs';

const ADMIN_BCRYPT = '$2y$10$NeoXS685SYXSeLFA.o1Wj.p6nqLS8RixLRcq7WRh4R1EN4kGNsDDW';
const PERMISSIONS = [
  ['competition:exam-session:view', '查看正式比赛', 'view'],
  ['competition:exam-session:prepare', '准备正式比赛', 'prepare'],
  ['competition:exam-session:start', '开始正式比赛', 'start'],
  ['competition:exam-session:force-submit', '强制交卷', 'force-submit'],
  ['competition:exam-session:force-offline', '清退', 'force-offline'],
  ['competition:exam-session:score', '评分', 'score'],
];

function nowSql() {
  return new Date().toISOString().slice(0, 19).replace('T', ' ');
}

function optionJson() {
  return JSON.stringify([
    { key: 'A', content: 'A' },
    { key: 'B', content: 'B' },
    { key: 'C', content: 'C' },
    { key: 'D', content: 'D' },
  ]);
}

export async function seedLoadData(options) {
  const pads = Math.max(1, Number(options.pads) || 10);
  const consoles = Math.max(1, Number(options.consoles) || 1);
  const questions = Math.max(1, Number(options.questions) || 8);
  const durationSeconds = Math.max(30, Number(options.examDurationSec) || 300);
  const tenantId = String(options.tenantId || process.env.LOAD_TENANT_ID || '10159');
  const tenantCode = options.tenantCode || process.env.LOAD_TENANT_CODE || 'loadtest';
  const eventCode = options.eventCode || `load-${Date.now()}`;
  const playerPassword = options.playerPassword || process.env.LOAD_PLAYER_PASSWORD || 'LoadTest#123';
  const consolePassword = options.consolePassword || process.env.LOAD_CONSOLE_PASSWORD || 'Admin@123456';
  const playerHash = bcryptHash(playerPassword);
  const created = nowSql();
  const sessionId = loadId(Kind.SESSION, 1);
  const connection = await openMysql(options.env);
  const fixture = {
    tenantId,
    tenantCode,
    eventCode,
    pads,
    consoles,
    questions,
    durationSeconds,
    sessionStatus: 'ready',
    generatedAt: new Date().toISOString(),
  };

  try {
    await connection.beginTransaction();
    await deletePreviousLoad(connection, { tenantId, sessionId });
    await upsertById(connection, 'sys_tenant', {
      id: tenantId,
      code: tenantCode,
      name: '正式考试压测租户',
      status: 'enabled',
      contact_name: 'load',
      contact_phone: null,
      remark: 'isolated loadtest tenant',
      created_at: created,
      updated_at: created,
      deleted: 0,
    });
    const orgId = loadId(Kind.ORG, 1);
    await upsertById(connection, 'sys_org', {
      id: orgId,
      tenant_id: tenantId,
      parent_id: 0,
      org_type: 'company',
      code: 'load-org',
      name: '压测企业',
      sort_order: 1,
      status: 'enabled',
      created_at: created,
      updated_at: created,
      created_by: 1,
      updated_by: 1,
      deleted: 0,
    });

    const roleId = loadId(Kind.ROLE, 1);
    await upsertById(connection, 'sys_role', {
      id: roleId,
      tenant_id: tenantId,
      code: 'PLATFORM_ADMIN',
      name: '压测控制台',
      role_type: 'platform',
      status: 'enabled',
      remark: 'formal-exam load console',
      created_at: created,
      updated_at: created,
      created_by: 1,
      updated_by: 1,
      deleted: 0,
    });
    const menuId = loadId(Kind.MENU, 1);
    await upsertById(connection, 'sys_menu', {
      id: menuId,
      tenant_id: tenantId,
      parent_id: 0,
      menu_type: 'page',
      code: 'load_formal_exam',
      name: '压测正式考',
      path: '/competition/formal-exam',
      component: 'competition/FormalExam',
      permission_code: 'competition:exam-session:view',
      icon: null,
      sort_order: 1,
      visible: 1,
      status: 'enabled',
      created_at: created,
      updated_at: created,
      created_by: 1,
      updated_by: 1,
      deleted: 0,
    });
    const permissionIds = [];
    for (let i = 0; i < PERMISSIONS.length; i += 1) {
      const [code, name, action] = PERMISSIONS[i];
      const permissionId = loadId(Kind.PERMISSION, i + 1);
      permissionIds.push(permissionId);
      await upsertById(connection, 'sys_permission', {
        id: permissionId,
        tenant_id: tenantId,
        menu_id: menuId,
        code,
        name,
        action,
        status: 'enabled',
        created_at: created,
        updated_at: created,
        created_by: 1,
        updated_by: 1,
        deleted: 0,
      });
      await upsertById(connection, 'sys_role_permission', {
        id: loadId(Kind.ROLE_PERMISSION, i + 1),
        tenant_id: tenantId,
        role_id: roleId,
        permission_id: permissionId,
        created_at: created,
        updated_at: created,
        created_by: 1,
        updated_by: 1,
        deleted: 0,
      });
    }

    fixture.consolesAccounts = [];
    for (let i = 1; i <= consoles; i += 1) {
      const userId = loadId(Kind.USER, i);
      const username = `load-console-${i}`;
      await upsertById(connection, 'sys_user', {
        id: userId,
        tenant_id: tenantId,
        org_id: orgId,
        username,
        password_hash: ADMIN_BCRYPT,
        display_name: `压测控制台${i}`,
        user_type: 'admin',
        phone: null,
        email: null,
        avatar_file_id: null,
        status: 'enabled',
        last_login_at: null,
        created_at: created,
        updated_at: created,
        created_by: 1,
        updated_by: 1,
        deleted: 0,
      });
      await upsertById(connection, 'sys_user_role', {
        id: loadId(Kind.USER_ROLE, i),
        tenant_id: tenantId,
        user_id: userId,
        role_id: roleId,
        created_at: created,
        updated_at: created,
        created_by: 1,
        updated_by: 1,
        deleted: 0,
      });
      fixture.consolesAccounts.push({ username, userId });
    }

    const eventId = loadId(Kind.EVENT, 1);
    const stageId = loadId(Kind.STAGE, 1);
    const workTypeId = loadId(Kind.WORK_TYPE, 1);
    const levelId = loadId(Kind.LEVEL, 1);
    await upsertById(connection, 'competition_event', {
      id: eventId,
      tenant_id: tenantId,
      name: `压测赛事 ${eventCode}`,
      code: eventCode,
      player_entry_code: `pe-${eventCode}`.slice(0, 64),
      scope: 'platform',
      owner_org_id: orgId,
      status: 'published',
      stage_plan_status: 'confirmed',
      registration_audit_required: 0,
      start_at: created,
      end_at: created,
      description: 'isolated formal-exam load event',
      created_by: 1,
      created_at: created,
      updated_by: 1,
      updated_at: created,
      deleted: 0,
    });
    await upsertById(connection, 'competition_event_work_type', {
      id: workTypeId,
      tenant_id: tenantId,
      event_id: eventId,
      work_type_id: loadId(Kind.WORK_TYPE, 99),
      display_name: '压测工种',
      status: 'active',
      sort_order: 1,
      stage_plan_status: 'confirmed',
      created_by: 1,
      updated_by: 1,
      created_at: created,
      updated_at: created,
      deleted: 0,
    });
    await upsertById(connection, 'competition_event_work_type_level', {
      id: levelId,
      tenant_id: tenantId,
      event_id: eventId,
      event_work_type_id: workTypeId,
      level_code: 'L4',
      level_name: '四级',
      source_level_code: 'L4',
      source_level_text: '四级',
      reconciliation_status: 'matched',
      stage_plan_status: 'confirmed',
      created_by: 1,
      updated_by: 1,
      created_at: created,
      updated_at: created,
      deleted: 0,
    });
    await upsertById(connection, 'competition_stage', {
      id: stageId,
      tenant_id: tenantId,
      event_id: eventId,
      event_work_type_id: workTypeId,
      event_work_type_level_code: 'L4',
      name: '理论压测赛段',
      stage_type: 'theory',
      sort_order: 1,
      start_at: created,
      end_at: created,
      status: 'ready',
      description: 'load',
      created_by: 1,
      created_at: created,
      updated_by: 1,
      updated_at: created,
      deleted: 0,
    });
    if (await hasTable(connection, 'competition_event_work_type_stage')) {
      await upsertById(connection, 'competition_event_work_type_stage', {
        id: loadId(Kind.STAGE, 2),
        tenant_id: tenantId,
        event_id: eventId,
        event_work_type_id: workTypeId,
        stage_id: stageId,
        enabled: 1,
        sort_order: 1,
        legacy_rule: 1,
        created_by: 1,
        updated_by: 1,
        created_at: created,
        updated_at: created,
        deleted: 0,
      });
    }

    const paperId = loadId(Kind.PAPER, 1);
    const sourcePaperId = loadId(Kind.PAPER, 2);
    await upsertById(connection, 'competition_formal_exam_session', {
      id: sessionId,
      tenant_id: tenantId,
      event_id: eventId,
      stage_id: stageId,
      event_work_type_id: workTypeId,
      event_work_type_level_id: levelId,
      exam_type: 'theory',
      status: 'ready',
      duration_seconds: durationSeconds,
      planned_start_time: created,
      actual_start_time: null,
      actual_end_time: null,
      candidate_total: pads,
      submitted_total: 0,
      occupancy_token: 0,
      scoring_status: 'not_started',
      created_by: 1,
      updated_by: 1,
      created_at: created,
      updated_at: created,
      deleted: 0,
    });
    await upsertById(connection, 'competition_formal_exam_paper_snapshot', {
      id: paperId,
      tenant_id: tenantId,
      exam_session_id: sessionId,
      source_paper_id: sourcePaperId,
      paper_name: '压测试卷',
      paper_code: 'LOAD-PAPER',
      total_score: questions,
      question_count: questions,
      duration_minutes: Math.ceil(durationSeconds / 60),
      snapshot_version: 1,
      created_by: 1,
      updated_by: 1,
      created_at: created,
      updated_at: created,
      deleted: 0,
    });
    const questionIds = [];
    for (let i = 1; i <= questions; i += 1) {
      const questionId = loadId(Kind.QUESTION, i);
      questionIds.push(questionId);
      await upsertById(connection, 'competition_formal_exam_question_snapshot', {
        id: questionId,
        tenant_id: tenantId,
        exam_session_id: sessionId,
        paper_snapshot_id: paperId,
        source_question_id: loadId(Kind.QUESTION, 1000 + i),
        question_order: i,
        question_type: 'single',
        stem: `压测题 ${i}`,
        score: 1,
        options_json: optionJson(),
        correct_answers_json: JSON.stringify(['A']),
        scoring_rule_code: 'choice-exact',
        scoring_rule_version: 1,
        created_by: 1,
        updated_by: 1,
        created_at: created,
        updated_at: created,
        deleted: 0,
      });
    }

    const batchId = loadId(Kind.BATCH, 1);
    fixture.pads = [];
    for (let i = 1; i <= pads; i += 1) {
      const playerId = loadId(Kind.PLAYER, i);
      const memberId = loadId(Kind.MEMBER, i);
      const rosterId = loadId(Kind.ROSTER, i);
      const registrationId = loadId(Kind.REGISTRATION, i);
      const profileId = loadId(Kind.PROFILE, i);
      const candidateId = loadId(Kind.CANDIDATE, i);
      const mobile = syntheticMobile(i);
      const idCard = syntheticIdCard(i);
      await upsertById(connection, 'competition_player_identity', {
        id: playerId,
        tenant_id: tenantId,
        id_card_ciphertext: idCard,
        id_card_lookup_hash: sha256Hex(idCard),
        mobile_ciphertext: mobile,
        mobile_lookup_hash: sha256Hex(mobile),
        crypto_key_version: 'plain-v1',
        password_hash: playerHash,
        password_initialized_at: created,
        status: 'active',
        activated_at: created,
        created_by: 1,
        updated_by: 1,
        created_at: created,
        updated_at: created,
        deleted: 0,
      });
      await upsertById(connection, 'competition_event_member', {
        id: memberId,
        tenant_id: tenantId,
        event_id: eventId,
        person_id: playerId,
        role_type: 'contestant',
        status: 'active',
        joined_at: created,
        created_by: 1,
        updated_by: 1,
        created_at: created,
        updated_at: created,
        deleted: 0,
      });
      await upsertById(connection, 'competition_roster_item', {
        id: rosterId,
        tenant_id: tenantId,
        batch_id: batchId,
        employee_no: `PAD${String(i).padStart(5, '0')}`,
        player_id: playerId,
        org_id: orgId,
        org_name: '压测企业',
        event_work_type_id: workTypeId,
        event_work_type_level_id: levelId,
        source_row_no: i,
        name: `Pad${i}`,
        work_type: '压测工种',
        level: '四级',
        validation_status: 'valid',
        created_by: 1,
        created_at: created,
        updated_by: 1,
        updated_at: created,
        deleted: 0,
      });
      await upsertById(connection, 'competition_registration', {
        id: registrationId,
        tenant_id: tenantId,
        event_id: eventId,
        org_id: orgId,
        roster_item_id: rosterId,
        player_id: playerId,
        member_id: memberId,
        event_work_type_id: workTypeId,
        source: 'enterprise_import',
        status: 'eligible_activated',
        created_by: 1,
        created_at: created,
        updated_by: 1,
        updated_at: created,
        deleted: 0,
      });
      await upsertById(connection, 'competition_contestant_profile', {
        id: profileId,
        tenant_id: tenantId,
        member_id: memberId,
        registration_id: registrationId,
        contestant_no: `C${String(i).padStart(5, '0')}`,
        group_code: 'load',
        current_stage_id: stageId,
        event_work_type_id: workTypeId,
        event_work_type_level_id: levelId,
        work_type_match_status: 'matched',
        promotion_status: 'pending',
        total_score: 0,
        created_by: 1,
        updated_by: 1,
        created_at: created,
        updated_at: created,
        deleted: 0,
      });
      await upsertById(connection, 'competition_formal_candidate_exam', {
        id: candidateId,
        tenant_id: tenantId,
        exam_session_id: sessionId,
        contestant_profile_id: profileId,
        contestant_member_id: memberId,
        registration_id: registrationId,
        player_id: playerId,
        candidate_no: `C${String(i).padStart(5, '0')}`,
        candidate_name: `Pad${i}`,
        paper_snapshot_id: paperId,
        source_paper_id: sourcePaperId,
        paper_code: 'LOAD-PAPER',
        paper_name: '压测试卷',
        status: 'waiting_enter',
        answered_count: 0,
        total_question_count: questions,
        created_by: 1,
        updated_by: 1,
        created_at: created,
        updated_at: created,
        deleted: 0,
      });
      fixture.pads.push({
        index: i,
        playerId,
        account: mobile,
        deviceId: `load-pad-${i}`,
        candidateExamId: candidateId,
        candidateNo: `C${String(i).padStart(5, '0')}`,
      });
    }

    fixture.sessionId = sessionId;
    fixture.eventId = eventId;
    fixture.stageId = stageId;
    fixture.paperId = paperId;
    fixture.questionIds = questionIds;
    fixture.consolePasswordHint = 'env LOAD_CONSOLE_PASSWORD';
    fixture.playerPasswordHint = 'env LOAD_PLAYER_PASSWORD';
    const checks = await consistencyCheck(connection, fixture);
    if (!checks.ok) {
      throw new Error(`seed consistency failed: ${checks.failures.join('; ')}`);
    }
    await connection.commit();
    fixture.checks = checks;
    fixture.playerPassword = playerPassword;
    fixture.consolePassword = consolePassword;
    return fixture;
  } catch (error) {
    try { await connection.rollback(); } catch { /* ignore */ }
    throw error;
  } finally {
    await connection.end();
  }
}

export async function writeFixture(fixture, filePath) {
  const target = filePath || path.join(process.cwd(), '.run', 'fixture.json');
  fs.mkdirSync(path.dirname(target), { recursive: true });
  const stored = { ...fixture };
  fs.writeFileSync(target, `${JSON.stringify(stored, null, 2)}\n`, 'utf8');
  return target;
}

export async function consistencyCheck(connection, fixture) {
  const failures = [];
  const candidates = await query(
    connection,
    'SELECT COUNT(*) AS c FROM competition_formal_candidate_exam WHERE exam_session_id = ? AND deleted = 0',
    [fixture.sessionId],
  );
  const expectedPads = Array.isArray(fixture.pads) ? fixture.pads.length : Number(fixture.pads);
  if (Number(candidates[0]?.c) !== expectedPads) {
    failures.push(`candidate count ${candidates[0]?.c} != ${expectedPads}`);
  }
  const questions = await query(
    connection,
    'SELECT COUNT(*) AS c FROM competition_formal_exam_question_snapshot WHERE exam_session_id = ? AND deleted = 0',
    [fixture.sessionId],
  );
  if (Number(questions[0]?.c) !== fixture.questions) {
    failures.push(`question count ${questions[0]?.c} != ${fixture.questions}`);
  }
  const players = await query(
    connection,
    `SELECT COUNT(*) AS c FROM competition_player_identity
     WHERE tenant_id = ? AND id >= ? AND deleted = 0`,
    [fixture.tenantId, loadId(Kind.PLAYER, 1)],
  );
  if (Number(players[0]?.c) !== expectedPads) {
    failures.push(`player count ${players[0]?.c} != ${expectedPads}`);
  }
  const registrations = await query(
    connection,
    'SELECT COUNT(*) AS c FROM competition_registration WHERE event_id = ? AND player_id IS NOT NULL AND deleted = 0',
    [fixture.eventId],
  );
  if (Number(registrations[0]?.c) !== expectedPads) {
    failures.push(`registration count ${registrations[0]?.c} != ${expectedPads}`);
  }
  return { ok: failures.length === 0, failures };
}

async function deletePreviousLoad(connection, { tenantId, sessionId }) {
  // Runtime rows are created by MyBatis IdWorker (roughly 2e18), not by the
  // synthetic 9.1e18 seed IDs.  Delete them by their business scope first;
  // an ID floor alone silently leaves the previous round in the database.
  const sessionTables = [
    'competition_formal_candidate_question_score',
    'competition_formal_candidate_result',
    'competition_formal_candidate_answer',
    'competition_formal_exam_access_command',
    'competition_formal_exam_access_session',
    'competition_formal_exam_outbox',
    'competition_formal_exam_realtime_cursor',
    'competition_formal_exam_realtime_dispatch_cursor',
    'competition_formal_candidate_exam',
    'competition_formal_exam_question_snapshot',
    'competition_formal_exam_paper_snapshot',
    'competition_formal_exam_session',
  ];
  for (const table of sessionTables) {
    if (!(await hasTable(connection, table))) continue;
    if (table === 'competition_formal_exam_session') {
      await connection.execute(
        `DELETE FROM \`${table}\` WHERE tenant_id = ? AND id = ?`,
        [tenantId, sessionId],
      );
      continue;
    }
    await connection.execute(
      `DELETE FROM \`${table}\` WHERE tenant_id = ? AND exam_session_id = ?`,
      [tenantId, sessionId],
    );
  }

  // The remaining fixture graph uses deterministic synthetic IDs.  Keep this
  // cleanup tenant-scoped so a load run cannot touch another tenant's data.
  const tenantSyntheticTables = [
    'competition_contestant_profile',
    'competition_registration',
    'competition_roster_item',
    'competition_event_member',
    'competition_player_identity',
    'competition_event_work_type_stage',
    'competition_event_work_type_level',
    'competition_event_work_type',
    'competition_stage',
    'competition_event',
    'sys_role_permission',
    'sys_user_role',
    'sys_permission',
    'sys_menu',
    'sys_user',
    'sys_role',
    'sys_org',
  ];
  const minId = loadId(Kind.TENANT, 0);
  for (const table of tenantSyntheticTables) {
    if (!(await hasTable(connection, table))) continue;
    await connection.execute(
      `DELETE FROM \`${table}\` WHERE tenant_id = ? AND id >= ?`,
      [tenantId, minId],
    );
  }
  if (await hasTable(connection, 'sys_tenant')) {
    await connection.execute('DELETE FROM `sys_tenant` WHERE id >= ?', [minId]);
  }
}
