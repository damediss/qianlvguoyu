import assert from 'node:assert/strict';
import test from 'node:test';
import { findCompetitionMenuCode, isCompetitionPath } from '../src/routes/competitionRoute';

const menuTree = [{
  id: 1,
  parentId: 0,
  menuType: 'directory',
  code: 'competition_foundation',
  name: '竞赛管理',
  path: '/competition',
  component: '',
  permissionCode: '',
  icon: '',
  sortOrder: 1,
  visible: 1,
  status: 'enabled',
  children: [
    {
      id: 2,
      parentId: 1,
      menuType: 'page',
      code: 'competition_event',
      name: '赛事创建',
      path: '/competition/events',
      component: 'competition/CompetitionEventManagementPage',
      permissionCode: '',
      icon: '',
      sortOrder: 1,
      visible: 1,
      status: 'enabled',
      children: [],
    },
    {
      id: 3,
      parentId: 1,
      menuType: 'page',
      code: 'competition_stage',
      name: '赛段编排',
      path: '/competition/stages',
      component: 'competition/CompetitionStagePlanningPage',
      permissionCode: '',
      icon: '',
      sortOrder: 2,
      visible: 1,
      status: 'enabled',
      children: [],
    },
    {
      id: 4,
      parentId: 1,
      menuType: 'page',
      code: 'competition_practical_exam',
      name: '实操考试',
      path: '/competition/practical-exams',
      component: 'competition/CompetitionPracticalExamPage',
      permissionCode: '',
      icon: '',
      sortOrder: 3,
      visible: 1,
      status: 'enabled',
      children: [],
    },
  ],
}];

test('competition paths are recognized without a hard-coded child-menu whitelist', () => {
  assert.equal(isCompetitionPath('/competition/practical-exams'), true);
  assert.equal(isCompetitionPath('/competitionx/events'), false);
  assert.equal(findCompetitionMenuCode(menuTree, '/competition/practical-exams'), 'competition_practical_exam');
});

test('event detail URLs resolve to the event menu by longest path match', () => {
  assert.equal(findCompetitionMenuCode(menuTree, '/competition/events/1001/organizations'), 'competition_event');
});
