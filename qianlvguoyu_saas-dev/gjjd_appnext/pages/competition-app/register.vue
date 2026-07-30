<template><PlayerPageShell title="赛事报名"><view class="card">报名表单和资格校验留待 COMP-002 API 实现。</view></PlayerPageShell></template>
<script>import PlayerPageShell from './components/PlayerPageShell.vue'; import { guardCompetitionPlayerRoute } from '@/utils/competition-route-guard'; export default { components:{PlayerPageShell}, onLoad(){ guardCompetitionPlayerRoute() } }</script>
<style scoped>.card{padding:32rpx;background:#fff;border-radius:20rpx}</style>
