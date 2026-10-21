export const REQUIRED_SOURCE_TYPES = [
    'theory',
    'simulation',
    'practical',
];
export function stageContentFormulaReady(contents) {
    const enabled = (contents || []).filter((item) => item.status === 'enabled');
    if (enabled.length === 0)
        return false;
    const total = enabled.reduce((sum, item) => sum + Number(item.weight || 0), 0);
    return (enabled.every((item) => Number(item.weight) > 0) &&
        Math.abs(total - 100) < 0.005);
}
export function enabledScoreSourceTypes(contents) {
    const types = (contents || [])
        .filter((item) => item.status === 'enabled')
        .map((item) => item.contentType)
        .filter((type) => type === 'theory' || type === 'simulation' || type === 'practical');
    return types.filter((type, index) => types.indexOf(type) === index);
}
function decimalValue(value) {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
}
function compareDecimalDesc(left, right) {
    return decimalValue(right) - decimalValue(left);
}
export function findCalculationReadyContestants(sources, requiredSourceTypes = REQUIRED_SOURCE_TYPES) {
    const sourceTypesByContestant = new Map();
    sources
        .filter((source) => source.currentFlag === 1 && source.confirmationStatus === 'confirmed')
        .forEach((source) => {
        const contestantId = String(source.contestantMemberId);
        const sourceTypes = sourceTypesByContestant.get(contestantId) || new Set();
        sourceTypes.add(source.sourceType);
        sourceTypesByContestant.set(contestantId, sourceTypes);
    });
    return [...sourceTypesByContestant.entries()]
        .filter(([, sourceTypes]) => requiredSourceTypes.every((type) => sourceTypes.has(type)))
        .map(([contestantId]) => contestantId);
}
export function buildRankingCandidates(snapshots) {
    const currentConfirmedByContestant = new Map();
    snapshots
        .filter((snapshot) => snapshot.currentFlag === 1 &&
        snapshot.confirmationStatus === 'confirmed')
        .forEach((snapshot) => {
        const contestantId = String(snapshot.contestantMemberId);
        const current = currentConfirmedByContestant.get(contestantId);
        if (!current ||
            snapshot.calculationVersion > current.calculationVersion) {
            currentConfirmedByContestant.set(contestantId, snapshot);
        }
    });
    return [...currentConfirmedByContestant.values()]
        .sort((left, right) => compareDecimalDesc(left.totalScore, right.totalScore) ||
        String(left.contestantMemberId).localeCompare(String(right.contestantMemberId)))
        .map((snapshot) => ({
        contestantMemberId: snapshot.contestantMemberId,
        totalScoreSnapshotId: snapshot.id,
        contestantStatus: 'completed',
    }));
}
export function rankingEntryPageItems(page) {
    return page?.items || [];
}
