export function arg(name, fallback, argv = process.argv) {
  const index = argv.indexOf(name);
  if (index < 0 || index === argv.length - 1) return fallback;
  return argv[index + 1];
}

export function hasFlag(name, argv = process.argv) {
  return argv.includes(name);
}

export function parseInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.trunc(parsed) : fallback;
}

export function parseArgs(argv = process.argv) {
  const command = argv[2] && !argv[2].startsWith('-') ? argv[2] : 'catalog';
  return {
    command,
    live: hasFlag('--live', argv) || command === 'live',
    mode: arg('--mode', command === 'live' ? 'exam' : 'health', argv),
    scenario: arg('--scenario', 'smoke', argv),
    pads: parseInteger(arg('--pads', arg('--scenario', '10', argv), argv), 10),
    consoles: parseInteger(arg('--consoles', '1', argv), 1),
    questions: parseInteger(arg('--questions', '8', argv), 8),
    durationSec: parseInteger(arg('--duration-sec', '300', argv), 300),
    rampSec: parseInteger(arg('--ramp-sec', '30', argv), 30),
    shardIndex: parseInteger(arg('--shard-index', '0', argv), 0),
    shardCount: parseInteger(arg('--shard-count', '1', argv), 1),
    seed: parseInteger(arg('--seed', String(Date.now() % 1_000_000), argv), 1),
    examDurationSec: parseInteger(arg('--exam-duration-sec', '300', argv), 300),
    tenants: parseInteger(arg('--tenants', '1', argv), 1),
    eventCode: arg('--event-code', '', argv),
    baseUrl: arg('--base-url', 'http://127.0.0.1:18080/api', argv),
    fixture: arg('--fixture', '', argv),
    outputDir: arg('--output-dir', '', argv),
    sessionId: arg('--session-id', process.env.FORMAL_EXAM_LOAD_SESSION_ID || '', argv),
    account: arg('--account', process.env.FORMAL_EXAM_LOAD_ACCOUNT || '', argv),
    password: arg('--password', process.env.FORMAL_EXAM_LOAD_PASSWORD || '', argv),
    writeAnswers: hasFlag('--write-answers', argv),
    startSession: !hasFlag('--no-start-session', argv),
    nodes: parseInteger(arg('--nodes', '1', argv), 1),
  };
}

export function scenarioPreset(name) {
  const presets = {
    smoke: { pads: 10, consoles: 1, durationSec: 300, rampSec: 20, questions: 8, examDurationSec: 240 },
    L1: { pads: 100, consoles: 1, durationSec: 600, rampSec: 40, questions: 60, examDurationSec: 540 },
    l1: { pads: 100, consoles: 1, durationSec: 600, rampSec: 40, questions: 60, examDurationSec: 540 },
    L2: { pads: 500, consoles: 5, durationSec: 900, rampSec: 90, questions: 60, examDurationSec: 840 },
    l2: { pads: 500, consoles: 5, durationSec: 900, rampSec: 90, questions: 60, examDurationSec: 840 },
    L3: { pads: 1000, consoles: 5, durationSec: 1200, rampSec: 120, questions: 100, examDurationSec: 1140 },
    l3: { pads: 1000, consoles: 5, durationSec: 1200, rampSec: 120, questions: 100, examDurationSec: 1140 },
    L4: { pads: 3000, consoles: 10, durationSec: 1800, rampSec: 180, questions: 100, examDurationSec: 1740 },
    l4: { pads: 3000, consoles: 10, durationSec: 1800, rampSec: 180, questions: 100, examDurationSec: 1740 },
    soak: { pads: 1000, consoles: 5, durationSec: 7200, rampSec: 120, questions: 60, examDurationSec: 7140 },
    'ws-gap-catchup': { pads: 10, consoles: 1, durationSec: 150, rampSec: 10, questions: 8, examDurationSec: 120 },
  };
  return presets[name] || null;
}

export function applyScenario(options, argv = process.argv) {
  const preset = scenarioPreset(options.scenario);
  if (!preset) return options;
  const numericScenario = Number(options.scenario);
  const padsFromNumericName = Number.isFinite(numericScenario) && numericScenario >= 10
    ? numericScenario
    : preset.pads;
  return {
    ...options,
    pads: hasFlag('--pads', argv) ? options.pads : padsFromNumericName,
    consoles: hasFlag('--consoles', argv) ? options.consoles : preset.consoles,
    durationSec: hasFlag('--duration-sec', argv) ? options.durationSec : preset.durationSec,
    rampSec: hasFlag('--ramp-sec', argv) ? options.rampSec : preset.rampSec,
    questions: hasFlag('--questions', argv) ? options.questions : preset.questions,
    examDurationSec: hasFlag('--exam-duration-sec', argv) ? options.examDurationSec : preset.examDurationSec,
  };
}
