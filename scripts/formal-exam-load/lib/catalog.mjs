const PAD_SCENARIOS = [100, 500, 1000, 3000];
const CONSOLE_SCENARIOS = [1, 5, 10];

export function planPadsAndConsoles(pads, consoles) {
  const statusIntervalMs = 4750;
  const connectedPollMs = 90_000;
  const answerEverySec = 10;
  const reconnectItems = 20;
  const autoSubmitChunk = 100;
  const autoSubmitMaxChunksPerTick = 5;
  return {
    pads,
    consoles,
    playerWsConnections: pads,
    consoleWsConnections: consoles,
    playerStatusPerMinDisconnected: Math.round(pads * (60_000 / statusIntervalMs)),
    playerStatusPerMinConnected: Math.round(pads * (60_000 / connectedPollMs)),
    playerContextOnce: pads,
    playerAnswerActionsPerMin: pads * (60 / answerEverySec),
    playerSyncBatchesPerMinApprox: Math.round((pads * (60 / answerEverySec)) / 20),
    reconnectTwentyAnswerBurst: {
      pads,
      itemsPerPad: reconnectItems,
      httpPosts: pads,
      expectedTransactions: pads,
    },
    unifiedDeadline: {
      sessions: 1,
      candidates: pads,
      waitingToAbsentSetBased: true,
      runningChunkSize: autoSubmitChunk,
      maxChunksPerTick: autoSubmitMaxChunksPerTick,
      ticksToDrainRunning: Math.ceil(pads / (autoSubmitChunk * autoSubmitMaxChunksPerTick)),
    },
    adminHttpWhenWsDownPerMin: consoles * 12,
    adminHttpWhenWsUpPerMin: consoles * 0.5,
    notes: [
      'Answers/paper/submit stay on HTTP. WS is status/control/ACK only.',
      'Connected pads should poll GET /status every 60–120s, not every 4s.',
      'Do not broadcast a 3000-row candidate array on WS.',
      'Reconnect burst is one POST /answers/sync of 20 items per pad, one DB TX.',
      'Unified deadline uses claim + chunk auto-submit; scoring is a separate scheduler.',
    ],
  };
}

export function phase6Catalog() {
  return {
    '1-pads-and-consoles': PAD_SCENARIOS.flatMap((pads) =>
      CONSOLE_SCENARIOS.map((consoles) => planPadsAndConsoles(pads, consoles))),
    '2-ws-steady-3000': {
      playerSockets: 3000,
      consoleSockets: 10,
      heartbeatSeconds: 20,
      presenceTtlSeconds: 60,
      ticketTtlSeconds: 45,
      measured: false,
    },
    '3-answer-action-every-10s': {
      pads: 3000,
      actionsPerMin: 3000 * 6,
      path: 'POST /competition/formal-exam/player/sessions/{id}/answers/sync',
      measured: false,
    },
    '4-offline-5min-then-20-item-burst': {
      pads: 3000,
      offlineSeconds: 300,
      itemsPerPad: 20,
      httpPosts: 3000,
      expectedTransactions: 3000,
      thunderingHerdMitigation: 'client full-jitter reconnect; server batch sync one TX',
      measured: false,
    },
    '5-unified-deadline-auto-submit': {
      candidates: 3000,
      claimLeaseSeconds: 25,
      chunkSize: 100,
      maxChunksPerTick: 5,
      schedulerDelayMillis: 5000,
      measured: false,
    },
  };
}
