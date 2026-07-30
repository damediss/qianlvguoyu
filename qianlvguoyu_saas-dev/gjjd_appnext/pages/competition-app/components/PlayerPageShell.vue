<template>
  <view class="player-page">
    <view class="hero">
      <text class="title">{{ title }}</text>
      <text class="subtitle">{{ subtitle }}</text>
    </view>
    <view class="content"><slot /></view>
  </view>
</template>

<script>
export default {
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, default: '竞赛选手端 MVP 页面骨架' },
  },
}
</script>

<style scoped>
.player-page { min-height: 100vh; background: #f5f7fb; color: #1f2937; }
.hero { padding: 56rpx 36rpx; background: linear-gradient(135deg, #155eef, #3b82f6); color: #fff; }
.title { display: block; font-size: 42rpx; font-weight: 700; }
.subtitle { display: block; margin-top: 12rpx; font-size: 26rpx; opacity: .85; }
.content { padding: 32rpx; }
</style>
