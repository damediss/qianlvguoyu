/* 第二版补充：统一动态页面中的赛段业务名称。 */
function normalizeStageTerms(root){
  const replacements=[['基础能力提升赛','初赛阶段'],['基础赛段','初赛阶段'],['基础赛','初赛阶段'],['专项技能挑战赛','复赛阶段'],['专项赛','复赛阶段']];
  const walker=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);
  const nodes=[];
  while(walker.nextNode())nodes.push(walker.currentNode);
  nodes.forEach(node=>replacements.forEach(([from,to])=>{node.nodeValue=node.nodeValue.replaceAll(from,to)}));
}

function renderSuper(page){
  const content=document.getElementById('superContent');
  if(!content)return;
  const map={dashboard:()=>dashboard(),wizard:()=>competitionWizard(),competitions:()=>tablePage('competitions'),stages:()=>stagesPage(),units:()=>tablePage('units'),users:()=>tablePage('users'),checkin:()=>tablePage('checkin'),learning:()=>tablePage('learning'),gate:()=>tablePage('gate'),points:()=>tablePage('points'),ranking,promotion:()=>promotion(false)};
  content.innerHTML=(map[page]||map.dashboard)();
  normalizeStageTerms(content);
  document.getElementById('crumb').textContent=labels[page]||labels.dashboard;
  bindControls();
}

function renderEnterprise(page){
  const content=document.getElementById('enterpriseContent');
  if(!content)return;
  const map={dashboard:()=>dashboard(true),users:()=>tablePage('users',true),checkin:()=>tablePage('checkin',true),learning:()=>tablePage('learning',true),gate:()=>tablePage('gate',true),points:()=>tablePage('points',true),details:()=>tablePage('details',true),promotion:()=>promotion(true)};
  content.innerHTML=(map[page]||map.dashboard)();
  normalizeStageTerms(content);
  document.getElementById('crumb').textContent=labels[page]||labels.dashboard;
  bindControls();
}
