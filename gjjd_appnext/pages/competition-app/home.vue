<template>
  <view class="page">
    <view class="top">
      <view>
        <text class="title">首页</text>
        <text class="player-account">{{ playerLabel }}</text>
      </view>
      <text class="message" @click="go('/pages/competition-app/messages')">
        通知{{ unreadCount ? `(${unreadCount})` : '' }}
      </text>
    </view>

    <view class="event-card">
      <text class="event-status">
        {{ currentEvent.registrationStatus === 'eligible_activated' ? '进行中 · 已激活' : '我的赛事' }}
      </text>
      <text class="event-name">{{ currentEvent.eventName || '暂无可参加赛事' }}</text>
      <text class="event-tip">当前共有 {{ eventCount }} 场有效赛事，可进入“我的赛事”查看。</text>
    </view>

    <text class="section">快捷入口</text>
    <view class="shortcuts">
      <view @click="go('/pages/competition-app/events')"><b>赛</b><text>我的赛事</text></view>
      <view @click="go('/pages/competition-app/tasks')"><b>任</b><text>赛事任务</text></view>
      <view @click="go('/pages/competition-app/messages')"><b>信</b><text>消息通知</text></view>
      <view @click="go('/pages/competition-app/profile')"><b>我</b><text>我的</text></view>
    </view>

    <view class="section-row">
      <text class="section">最近公告</text>
      <text class="more" @click="go('/pages/competition-app/messages')">查看全部</text>
    </view>
    <view v-if="recentMessages.length" class="notice-list">
      <view
        v-for="messageItem in recentMessages"
        :key="messageItem.messageId"
        class="notice-card"
        @click="go('/pages/competition-app/messages')"
      >
        <view class="notice-main">
          <view class="notice-title-row">
            <text class="notice-title">{{ messageItem.title }}</text>
            <text class="notice-type" :class="messageItem.contentType === 'pdf' ? 'notice-type--pdf' : ''">{{ messageItem.contentType === 'pdf' ? 'PDF 文件' : '图文' }}</text>
            <text v-if="!messageItem.readAt" class="unread-dot"></text>
          </view>
          <text class="notice-content">{{ messageSummary(messageItem) }}</text>
          <text class="notice-time">{{ formatTime(messageItem.publishedAt || messageItem.receivedAt) }}</text>
        </view>
      </view>
    </view>
    <view v-else class="empty">
      <text>暂无赛事公告</text>
      <text>平台发布与你当前赛事相关的公告后，将在这里展示。</text>
    </view>
  </view>
</template>

<script>
import { getCurrentPlayer, getPlayerMessages, getPlayerUnreadCount } from '@/api/competition-player'
import { guardCompetitionPlayerRoute } from '@/utils/competition-route-guard'
import { useCompetitionPlayerStore } from '@/store/competitionPlayer'

export default {
  data: () => ({
    unreadCount: 0,
    recentMessages: [],
  }),
  computed: {
    playerStore() {
      return useCompetitionPlayerStore()
    },
    currentEvent() {
      return this.playerStore.currentEvent || {}
    },
    eventCount() {
      return this.playerStore.events?.length || 0
    },
    playerLabel() {
      return this.playerStore.playerInfo?.maskedMobile
        ? `选手账号 ${this.playerStore.playerInfo.maskedMobile}`
        : '竞赛选手'
    },
  },
  onShow() {
    if (guardCompetitionPlayerRoute()) this.load()
  },
  methods: {
    async load() {
      try {
        const [profile, unreadCount, messagePage] = await Promise.all([
          getCurrentPlayer(),
          getPlayerUnreadCount(),
          getPlayerMessages(1, 5),
        ])
        this.playerStore.setProfile(profile)
        this.unreadCount = Number(unreadCount) || 0
        this.recentMessages = messagePage?.items || []
      } catch (error) {
        uni.showToast({ title: error.message || '首页加载失败', icon: 'none' })
      }
    },
    go(url) {
      uni.navigateTo({ url })
    },
    messageSummary(message) {
      if (message?.contentType === 'pdf') {
        const asset = (message.assets || []).find((item) => item.assetType === 'pdf')
        return message.content || `附件：${asset?.originalName || 'PDF 赛事文件'}`
      }
      return String(message?.content || '').replace(/!\[[^\]]*\]\(notice-asset:\/\/\d+\)/g, '【图片】')
    },
    formatTime(value) {
      return value ? String(value).replace('T', ' ').slice(0, 16) : ''
    },
  },
}
</script>

<style scoped>
.page{min-height:100vh;padding:46rpx 28rpx;background:#f5f8fd;box-sizing:border-box;color:#102a56}
.top{display:flex;justify-content:space-between;align-items:flex-start}
.top>view{display:flex;flex-direction:column;gap:8rpx}
.title{font-size:42rpx;font-weight:700}
.player-account{font-size:23rpx;color:#71829b}
.message{padding:12rpx 18rpx;border-radius:24rpx;background:#eaf2ff;font-size:25rpx;color:#1765df}
.event-card{display:flex;flex-direction:column;margin-top:32rpx;padding:34rpx;border-radius:26rpx;background:linear-gradient(135deg,#2864d7,#4388ef);color:#fff;box-shadow:0 14rpx 30rpx rgba(39,100,215,.18)}
.event-status{align-self:flex-start;padding:6rpx 16rpx;border-radius:22rpx;background:rgba(255,255,255,.18);font-size:22rpx}
.event-name{margin-top:20rpx;font-size:39rpx;font-weight:700}
.event-tip{margin-top:18rpx;font-size:23rpx;opacity:.86}
.section{display:block;margin:38rpx 0 18rpx;font-size:31rpx;font-weight:700}
.shortcuts{display:flex;gap:14rpx}
.shortcuts view{display:flex;flex:1;flex-direction:column;align-items:center;gap:10rpx;padding:22rpx 6rpx;border-radius:18rpx;background:#fff;font-size:22rpx}
.shortcuts b{display:flex;width:50rpx;height:50rpx;align-items:center;justify-content:center;border-radius:50%;background:#eaf2ff;color:#1765df}
.section-row{display:flex;align-items:center;justify-content:space-between}
.section-row .section{margin-bottom:18rpx}
.more{margin-top:22rpx;font-size:24rpx;color:#1765df}
.notice-list{display:flex;flex-direction:column;gap:16rpx}
.notice-card{display:flex;padding:26rpx;border-radius:18rpx;background:#fff}
.notice-main{display:flex;flex:1;flex-direction:column;gap:11rpx;min-width:0}
.notice-title-row{display:flex;align-items:center;gap:12rpx}
.notice-title{overflow:hidden;flex:1;font-size:28rpx;font-weight:700;text-overflow:ellipsis;white-space:nowrap}
.notice-type{flex:none;padding:3rpx 10rpx;border-radius:18rpx;background:#eaf3ff;color:#2164c9;font-size:19rpx}.notice-type--pdf{background:#fff0ed;color:#b74324}
.unread-dot{width:14rpx;height:14rpx;border-radius:50%;background:#ea5c57}
.notice-content{display:-webkit-box;overflow:hidden;font-size:24rpx;line-height:1.5;color:#667994;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.notice-time{font-size:21rpx;color:#9ba8ba}
.empty{display:flex;flex-direction:column;gap:12rpx;padding:30rpx;border-radius:18rpx;background:#fff;color:#667994}
.empty text:first-child{font-size:28rpx;font-weight:700;color:#233d67}
.empty text:last-child{font-size:24rpx;line-height:1.6}
</style>
