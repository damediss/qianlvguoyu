export function parseFormalExamSeq(seq) {
  if (seq == null || seq === '') return null;
  const value = typeof seq === 'number' ? seq : Number(seq);
  if (!Number.isFinite(value) || !Number.isInteger(value) || value < 1) return null;
  if (value > Number.MAX_SAFE_INTEGER) return null;
  return value;
}

export function classifyFormalExamRealtimeEvent(input) {
  const seen = input.seen || new Set();
  const eventId = input.eventId;
  if (eventId && seen.has(eventId)) {
    return { action: 'duplicate', lastSeq: input.lastSeq ?? null };
  }
  if (eventId) seen.add(eventId);
  const seq = parseFormalExamSeq(input.seq);
  if (seq == null) {
    return { action: 'skip-seq', lastSeq: input.lastSeq ?? null };
  }
  const lastSeq = input.lastSeq ?? null;
  if (lastSeq == null || seq > lastSeq) {
    return { action: 'accept', lastSeq: seq, jump: lastSeq != null && seq > lastSeq + 1 };
  }
  if (seq === lastSeq) {
    return { action: 'collision', lastSeq };
  }
  return { action: 'out-of-order', lastSeq };
}

export function mergeFormalExamRealtimeEvents(catchUp = [], buffered = []) {
  return [...catchUp, ...buffered].sort((left, right) => {
    const leftSeq = parseFormalExamSeq(left?.seq);
    const rightSeq = parseFormalExamSeq(right?.seq);
    if (leftSeq == null && rightSeq == null) {
      return String(left?.eventId || '').localeCompare(String(right?.eventId || ''));
    }
    if (leftSeq == null) return 1;
    if (rightSeq == null) return -1;
    if (leftSeq !== rightSeq) return leftSeq - rightSeq;
    return String(left?.eventId || '').localeCompare(String(right?.eventId || ''));
  });
}

export function relevantToPad(envelope, accessSessionId) {
  const target = envelope?.target;
  if (!target || target === 'examSession') return true;
  if (target === 'accessSession') {
    return String(envelope.accessSessionId || '') === String(accessSessionId || '');
  }
  return false;
}

export function outboxIntegrityFailures({ dropped = 0, seqDup = [], eventDup = [] } = {}) {
  const failures = [];
  if (dropped) failures.push(`outbox dropped=${dropped}`);
  if (eventDup.length) failures.push(`outbox event_id collisions ${eventDup.length}`);
  if (seqDup.length) failures.push(`outbox seq duplicates ${seqDup.length}`);
  return failures;
}
