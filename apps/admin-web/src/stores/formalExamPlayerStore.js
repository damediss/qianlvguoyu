import { create } from 'zustand';
const TOKEN_KEY = 'formal-exam-access-token';
const TENANT_KEY = 'formal-exam-tenant-id';
const PLAYER_KEY = 'formal-exam-player-id';
const ACCESS_SESSION_KEY = 'formal-exam-access-session-id';
const GENERATION_KEY = 'formal-exam-access-generation';
const DEVICE_KEY = 'formal-exam-device-id';
const EXAM_SESSION_KEY = 'formal-exam-exam-session-id';
function read(key) {
    try {
        return window.localStorage.getItem(key) || undefined;
    }
    catch {
        return undefined;
    }
}
function write(key, value) {
    try {
        if (!value) {
            window.localStorage.removeItem(key);
            return;
        }
        window.localStorage.setItem(key, value);
    }
    catch {
        /* ignore quota / private mode */
    }
}
export function getOrCreateFormalExamDeviceId() {
    let deviceId = read(DEVICE_KEY);
    if (!deviceId) {
        deviceId = globalThis.crypto?.randomUUID?.() || `dev-${Date.now()}`;
        write(DEVICE_KEY, deviceId);
    }
    return deviceId;
}
export const useFormalExamPlayerStore = create((set) => ({
    token: read(TOKEN_KEY),
    tenantId: read(TENANT_KEY),
    playerId: read(PLAYER_KEY),
    accessSessionId: read(ACCESS_SESSION_KEY),
    generation: Number.parseInt(read(GENERATION_KEY) || '', 10) || undefined,
    examSessionId: read(EXAM_SESSION_KEY),
    setSession: ({ token, tenantId, playerId, accessSessionId, generation, deviceId, examSessionId }) => {
        write(TOKEN_KEY, token);
        write(TENANT_KEY, tenantId);
        write(PLAYER_KEY, playerId);
        write(ACCESS_SESSION_KEY, accessSessionId);
        write(GENERATION_KEY, generation == null ? undefined : String(generation));
        write(EXAM_SESSION_KEY, examSessionId);
        if (deviceId)
            write(DEVICE_KEY, deviceId);
        set({ token, tenantId, playerId, accessSessionId, generation, examSessionId });
    },
    clear: () => {
        write(TOKEN_KEY);
        write(TENANT_KEY);
        write(PLAYER_KEY);
        write(ACCESS_SESSION_KEY);
        write(GENERATION_KEY);
        write(EXAM_SESSION_KEY);
        set({
            token: undefined,
            tenantId: undefined,
            playerId: undefined,
            accessSessionId: undefined,
            generation: undefined,
            examSessionId: undefined,
        });
    },
}));
