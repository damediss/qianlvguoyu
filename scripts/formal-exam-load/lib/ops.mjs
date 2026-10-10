import { latencySummary, errorRate } from './percentile.mjs';

export function createOps() {
  const byName = new Map();
  const errors = [];

  function bucket(name) {
    if (!byName.has(name)) {
      byName.set(name, {
        samples: [],
        errors: 0,
        statuses: {},
        itemResults: {
          total: 0,
          accepted: 0,
          rejected: 0,
          byConflictCode: {},
          byOutcome: {},
        },
      });
    }
    return byName.get(name);
  }

  function recordItemResults(name, results) {
    const item = bucket(name);
    const rows = Array.isArray(results) ? results : [];
    for (const result of rows) {
      const accepted = Boolean(result?.accepted);
      const conflictCode = result?.conflictCode == null || result.conflictCode === ''
        ? 'NONE'
        : String(result.conflictCode);
      const outcome = `${accepted ? 'accepted' : 'rejected'}:${conflictCode}`;
      item.itemResults.total += 1;
      if (accepted) item.itemResults.accepted += 1;
      else item.itemResults.rejected += 1;
      item.itemResults.byConflictCode[conflictCode] =
        (item.itemResults.byConflictCode[conflictCode] || 0) + 1;
      item.itemResults.byOutcome[outcome] = (item.itemResults.byOutcome[outcome] || 0) + 1;
    }
  }

  return {
    record(name, result) {
      const item = bucket(name);
      item.samples.push(result.ms);
      const key = String(result.status);
      item.statuses[key] = (item.statuses[key] || 0) + 1;
      if (!result.ok) {
        item.errors += 1;
        if (errors.length < 50) {
          errors.push({
            operation: name,
            status: result.status,
            category: classify(result),
            ms: result.ms,
            message: safeMessage(result),
          });
        }
      }
    },
    snapshot() {
      const operations = {};
      for (const [name, item] of byName.entries()) {
        const summary = {
          ...latencySummary(item.samples),
          errors: item.errors,
          errorRate: errorRate(item.samples.length, item.errors),
          statuses: item.statuses,
        };
        if (item.itemResults.total > 0) {
          summary.itemResults = {
            total: item.itemResults.total,
            accepted: item.itemResults.accepted,
            rejected: item.itemResults.rejected,
            byConflictCode: { ...item.itemResults.byConflictCode },
            byOutcome: { ...item.itemResults.byOutcome },
          };
        }
        operations[name] = summary;
      }
      return { operations, errorSamples: errors };
    },
    recordItemResults,
  };
}

export function nextAnswerVersion(current) {
  const value = Number(current) || 0;
  return value + 1;
}

export function versionMap(questions, localVersions) {
  return questions.map((question) => ({
    questionSnapshotId: String(question.questionSnapshotId || question.id),
    version: localVersions[String(question.questionSnapshotId || question.id)] || 0,
  }));
}

function classify(result) {
  if (result.status === 0) return 'network';
  if (result.status === 401) return 'auth';
  if (result.status === 403) return 'forbidden';
  if (result.status === 409) return 'conflict';
  if (result.status === 503) return 'unavailable';
  if (result.status >= 500) return 'server';
  if (result.status >= 400) return 'client';
  return 'ok';
}

function safeMessage(result) {
  const body = result.body;
  if (!body) return result.error || '';
  const message = body.message || body.code || '';
  return String(message).slice(0, 180);
}
