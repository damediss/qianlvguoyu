import type { Page } from '@playwright/test';

export type StageFixtureItem = {
  id?: string;
  stageType: string;
  startAt?: string;
  endAt?: string;
  description?: string;
};

type ApiEnvelope<T> = {
  code: string;
  message?: string;
  data: T;
};

const apiBaseURL = process.env.STAGE_E2E_API_BASE_URL || 'http://127.0.0.1:8080/api';

export function stageE2EEnabled() {
  return Boolean(
    process.env.STAGE_E2E_EVENT_ID
      && process.env.STAGE_E2E_TENANT_CODE
      && process.env.STAGE_E2E_USERNAME
      && process.env.STAGE_E2E_PASSWORD
      && process.env.STAGE_E2E_CAPTCHA_X,
  );
}

export function stageE2EEventId() {
  return process.env.STAGE_E2E_EVENT_ID!;
}

async function authHeaders(page: Page) {
  return page.evaluate(() => ({
    authorization: localStorage.getItem('guoyu_admin_access_token'),
    tenantId: localStorage.getItem('guoyu_admin_tenant_id'),
  }));
}

async function api<T>(page: Page, path: string, init: Parameters<Page['request']['fetch']>[1] = {}) {
  const auth = await authHeaders(page);
  const response = await page.request.fetch(`${apiBaseURL}${path}`, {
    ...init,
    headers: {
      Authorization: auth.authorization ? `Bearer ${auth.authorization}` : '',
      'X-Tenant-Id': auth.tenantId || '',
      'X-Request-Id': `stage-e2e-${Date.now()}`,
      ...(init.headers || {}),
    },
  });
  const body = await response.json() as ApiEnvelope<T>;
  if (!response.ok() || body.code !== '0') {
    throw new Error(`stage fixture API failed: ${response.status()} ${body.message || response.statusText()}`);
  }
  return body.data;
}

export async function getEvent(page: Page, eventId: string) {
  return api<{ id: string; status: string }>(page, `/competition/events/${eventId}`);
}

export async function getStages(page: Page, eventId: string) {
  const response = await api<{ items: Array<StageFixtureItem & { id: string; status: string }>; total: number }>(
    page,
    `/competition/events/${eventId}/stages?page=1&pageSize=100`,
  );
  return response.items;
}

export async function saveStages(page: Page, eventId: string, stages: StageFixtureItem[]) {
  return api<Array<StageFixtureItem & { id: string; sortOrder: number }>>(
    page,
    `/competition/events/${eventId}/stages/draft`,
    {
      method: 'PUT',
      data: { stages },
      headers: { 'Content-Type': 'application/json' },
    },
  );
}

export function toFixtureItems(stages: Array<StageFixtureItem & { id: string }>): StageFixtureItem[] {
  return stages.map((stage) => ({
    id: String(stage.id),
    stageType: stage.stageType,
    startAt: stage.startAt || undefined,
    endAt: stage.endAt || undefined,
    description: stage.description || undefined,
  }));
}

export async function prepareDraftFixture(page: Page, eventId: string) {
  const event = await getEvent(page, eventId);
  if (event.status !== 'draft') {
    throw new Error(`stage E2E fixture requires a draft event, got ${event.status}`);
  }
  const originalStages = await getStages(page, eventId);
  if (originalStages.length === 0) {
    throw new Error('stage E2E fixture requires one or more existing draft stages so teardown can restore exactly');
  }
  if (originalStages.some((stage) => stage.status !== 'draft')) {
    throw new Error('stage E2E fixture requires all existing stages to be draft');
  }
  const initialStage: StageFixtureItem = {
    stageType: 'preliminary',
    description: 'E2E initial stage',
  };
  await saveStages(page, eventId, [initialStage]);
  return {
    originalStages: toFixtureItems(originalStages),
    restoreStages: toFixtureItems(originalStages),
  };
}
