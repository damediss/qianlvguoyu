const DB_NAME = 'guoyu-assessor-scoring';
const STORE_NAME = 'drafts';
const DB_VERSION = 1;
function openDatabase() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                db.createObjectStore(STORE_NAME, { keyPath: 'key' });
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    });
}
export function assessorDraftKey(userId, taskId, revision) {
    return `${userId}:${taskId}:${revision}`;
}
export async function loadAssessorDraft(key) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const request = db.transaction(STORE_NAME, 'readonly').objectStore(STORE_NAME).get(key);
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error);
    }).finally(() => db.close());
}
export async function saveAssessorDraft(draft) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(STORE_NAME, 'readwrite');
        transaction.objectStore(STORE_NAME).put(draft);
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error);
    }).finally(() => db.close());
}
export async function deleteAssessorDraft(key) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const transaction = db.transaction(STORE_NAME, 'readwrite');
        transaction.objectStore(STORE_NAME).delete(key);
        transaction.oncomplete = () => resolve();
        transaction.onerror = () => reject(transaction.error);
    }).finally(() => db.close());
}
