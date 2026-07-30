<template><PlayerPageShell title="赛事详情"><view class="card">赛事规则、赛段和选手资格将在接口接入后展示。</view></PlayerPageShell></template>
<script>import PlayerPageShell from './components/PlayerPageShell.vue'; import { guardCompetitionPlayerRoute } from '@/utils/competition-route-guard'; export default { components:{PlayerPageShell}, onLoad(){ guardCompetitionPlayerRoute() } }</script>
<style scoped>.card{padding:32rpx;background:#fff;border-radius:20rpx}</style>
