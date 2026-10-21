export function emptyFormalAnswer() {
    return { selectedOptionIds: [], blanks: [] };
}
export function sameAnswerPayload(left, right, leftMarked, rightMarked) {
    if (leftMarked !== rightMarked)
        return false;
    const leftIds = [...(left?.selectedOptionIds || [])].sort();
    const rightIds = [...(right?.selectedOptionIds || [])].sort();
    if (leftIds.join('\0') !== rightIds.join('\0'))
        return false;
    const leftBlanks = left?.blanks || [];
    const rightBlanks = right?.blanks || [];
    if (leftBlanks.length !== rightBlanks.length)
        return false;
    return leftBlanks.every((item, index) => (item || '') === (rightBlanks[index] || ''));
}
export function fromServerAnswer(sessionId, candidateExamId, saved) {
    const now = new Date().toISOString();
    return {
        sessionId: String(sessionId),
        candidateExamId: String(candidateExamId),
        questionSnapshotId: String(saved.questionSnapshotId),
        answer: {
            selectedOptionIds: saved.answer?.selectedOptionIds || [],
            blanks: saved.answer?.blanks || [],
        },
        marked: Boolean(saved.marked),
        version: saved.version || 0,
        serverVersion: saved.version || 0,
        syncStatus: 'SYNCED',
        updatedAt: now,
        lastSyncAt: now,
    };
}
export function mergeServerAndLocal(input) {
    const sessionId = String(input.sessionId);
    const candidateExamId = String(input.candidateExamId);
    const questionSnapshotId = String(input.questionSnapshotId);
    const server = input.server || null;
    const local = input.local
        ? {
            ...input.local,
            sessionId,
            candidateExamId,
            questionSnapshotId: String(input.local.questionSnapshotId),
        }
        : null;
    if (!server && !local) {
        return { record: null, reason: 'empty' };
    }
    if (server && !local) {
        return { record: fromServerAnswer(sessionId, candidateExamId, server), reason: 'server-only' };
    }
    if (local && !server) {
        if (local.syncStatus === 'SYNCED' && local.version > 0) {
            return {
                record: { ...local, syncStatus: 'PENDING', lastError: undefined },
                reason: 'local-only',
            };
        }
        return { record: local, reason: 'local-only' };
    }
    if (!server || !local) {
        return { record: local, reason: 'local-only' };
    }
    const serverVersion = server.version || 0;
    if (local.version > serverVersion) {
        return {
            record: {
                ...local,
                serverVersion,
                syncStatus: local.syncStatus === 'CONFLICT' ? 'CONFLICT' : 'PENDING',
                lastError: local.syncStatus === 'CONFLICT' ? local.lastError : undefined,
            },
            reason: 'local-newer',
        };
    }
    if (local.version < serverVersion) {
        return {
            record: fromServerAnswer(sessionId, candidateExamId, server),
            reason: 'server-newer',
        };
    }
    const serverAnswer = {
        selectedOptionIds: server.answer?.selectedOptionIds || [],
        blanks: server.answer?.blanks || [],
    };
    if (sameAnswerPayload(local.answer, serverAnswer, local.marked, Boolean(server.marked))) {
        return {
            record: {
                ...fromServerAnswer(sessionId, candidateExamId, server),
                answer: local.answer,
            },
            reason: 'same-synced',
        };
    }
    return {
        record: {
            ...local,
            serverVersion,
            syncStatus: 'CONFLICT',
            lastError: 'VERSION_PAYLOAD_CONFLICT',
        },
        reason: 'conflict',
    };
}
