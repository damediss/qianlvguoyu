import type { Page } from '@playwright/test';

type LocalStage = {
  id: string;
  stageType: string;
  sortOrder: number;
  status: 'draft';
  startAt: string | null;
  endAt: string | null;
  description: string;
};

const stageOrder: Record<string, number> = {
  qualification: 10,
  preliminary: 20,
  quarterfinal: 30,
  semifinal: 40,
  final: 50,
};

const user = {
  id: 1,
  tenantId: 1,
  username: 'fixture-admin',
  displayName: '本地测试管理员',
  userType: 'platform',
  roles: ['PLATFORM_ADMIN'],
  permissions: [
    'competition:event:view',
    'competition:stage:view',
    'competition:stage:manage',
  ],
};

function response(data: unknown) {
  return {
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code: '0', message: 'OK', data, traceId: 'local-stage-fixture' }),
  };
}

function menuTree() {
  return [{
    id: 1,
    parentId: 0,
    menuType: 'directory',
    code: 'competition_event',
    name: '赛事',
    path: '/competition/events',
    component: '',
    permissionCode: '',
    icon: 'TrophyOutlined',
    sortOrder: 1,
    children: [{
      id: 2,
      parentId: 1,
      menuType: 'page',
      code: 'competition_stage',
      name: '赛段编排',
      path: '/competition/stages',
      component: '',
      permissionCode: 'competition:stage:view',
      icon: 'BranchesOutlined',
      sortOrder: 1,
      children: [],
    }],
  }];
}

export async function installLocalCompetitionStageFixture(page: Page) {
  let stages: LocalStage[] = [{
    id: '9001',
    stageType: 'preliminary',
    sortOrder: 20,
    status: 'draft',
    startAt: null,
    endAt: null,
    description: '本地夹具初始赛段',
  }];
  let nextId = 9002;
  const event = {
    id: '9000',
    code: 'LOCAL-STAGE-E2E',
    name: '本地赛段画布验收赛事',
    scope: 'platform',
    status: 'draft',
    stagePlanStatus: 'draft',
    startAt: '2026-08-01T00:00:00',
    endAt: '2026-12-31T23:59:59',
    registrationAuditRequired: 0,
    description: '仅用于 Playwright 本地夹具',
  };

  await page.route('**/api/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api/, '');
    if (path === '/auth/tenants' && request.method() === 'GET') {
      await route.fulfill(response([{ code: 'fixture', name: '本地测试租户' }]));
      return;
    }
    if (path === '/auth/captcha/generate' && request.method() === 'GET') {
      const transparentPng = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=';
      await route.fulfill(response({ captchaId: 'local-captcha', bgImage: `data:image/png;base64,${transparentPng}`, sliderImage: `data:image/png;base64,${transparentPng}`, sliderY: 40 }));
      return;
    }
    if (path === '/auth/captcha/verify' && request.method() === 'POST') {
      await route.fulfill(response({ success: true, captchaToken: 'local-captcha-token', message: null }));
      return;
    }
    if (path === '/auth/login' && request.method() === 'POST') {
      await route.fulfill(response({ accessToken: 'local-stage-token', refreshToken: 'local-stage-refresh', expiresIn: 3600, tenantId: 1, user }));
      return;
    }
    if (path === '/auth/me' && request.method() === 'GET') {
      await route.fulfill(response(user));
      return;
    }
    if (path === '/auth/menus' && request.method() === 'GET') {
      await route.fulfill(response(menuTree()));
      return;
    }
    if (path === '/competition/events' && request.method() === 'GET') {
      await route.fulfill(response({ items: [event], page: 1, pageSize: 100, total: 1 }));
      return;
    }
    if (path === `/competition/events/${event.id}` && request.method() === 'GET') {
      await route.fulfill(response(event));
      return;
    }
    if (path === `/competition/events/${event.id}/stages` && request.method() === 'GET') {
      await route.fulfill(response({ items: stages, page: 1, pageSize: 100, total: stages.length }));
      return;
    }
    if (path === `/competition/events/${event.id}/stages/draft` && request.method() === 'PUT') {
      const body = request.postDataJSON() as { stages: Array<{ id?: string; stageType: string; startAt?: string; endAt?: string; description?: string }> };
      stages = body.stages.map((item) => ({
        id: item.id || String(nextId++),
        stageType: item.stageType,
        sortOrder: stageOrder[item.stageType],
        status: 'draft',
        startAt: item.startAt || null,
        endAt: item.endAt || null,
        description: item.description || '',
      })).sort((a, b) => a.sortOrder - b.sortOrder);
      await route.fulfill(response(stages));
      return;
    }
    if (path === `/competition/events/${event.id}/stages/confirm` && request.method() === 'POST') {
      event.stagePlanStatus = 'confirmed';
      await route.fulfill(response(event));
      return;
    }
    await route.continue();
  });

  return {
    eventId: event.id,
    getStages: () => stages,
  };
}
