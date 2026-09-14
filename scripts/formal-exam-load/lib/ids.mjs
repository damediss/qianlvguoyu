const ID_BASE = 9100000000000000000n;

export function loadId(kind, n) {
  return (ID_BASE + BigInt(kind) * 100000000n + BigInt(n)).toString();
}

export function asIdString(value) {
  if (value == null) return '';
  if (typeof value === 'bigint') return value.toString();
  if (typeof value === 'number') return Math.trunc(value).toString();
  return String(value);
}

export const Kind = {
  TENANT: 1,
  ORG: 2,
  USER: 3,
  ROLE: 4,
  MENU: 5,
  PERMISSION: 6,
  USER_ROLE: 7,
  ROLE_PERMISSION: 8,
  EVENT: 10,
  STAGE: 11,
  WORK_TYPE: 12,
  LEVEL: 13,
  BATCH: 14,
  PLAYER: 20,
  MEMBER: 21,
  ROSTER: 22,
  REGISTRATION: 23,
  PROFILE: 24,
  SESSION: 30,
  PAPER: 31,
  QUESTION: 32,
  CANDIDATE: 33,
};
