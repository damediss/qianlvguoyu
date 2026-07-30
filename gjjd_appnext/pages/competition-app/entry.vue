<template>
  <view class="entry-page">
    <view class="brand"><text>竞赛选手端</text><text>企业内部技能竞赛</text></view>
    <view class="hero"><text class="hero-tag">赛事服务</text><text class="hero-title">扫码激活赛事资格</text><text class="hero-copy">请扫描计划总览中的赛事二维码，验证手机号后即可激活。</text></view>
    <view class="card"><view class="scan-mark">⌁</view><text class="card-title">扫一扫</text><text class="card-copy">使用赛事二维码进入对应赛事激活页</text><button class="primary" @click="scan">打开扫码</button></view>
    <view class="login-row"><text>已完成激活？</text><text class="login-link" @click="goLogin">扫码后密码登录</text></view>
  </view>
</template>

<script>
import { parseCompetitionPlayerEntry } from '@/utils/competition-entry'

export default {
  methods: {
    scan() {
      uni.scanCode({
        onlyFromCamera: true,
        success: ({ result }) => {
          const entryCode = parseCompetitionPlayerEntry(result)
          if (!entryCode) { uni.showToast({ title: '不是有效的赛事激活二维码', icon: 'none' }); return }
          uni.navigateTo({ url: `/pages/competition-app/login?entry=${encodeURIComponent(entryCode)}` })
        },
        fail: () => uni.showToast({ title: '扫码未完成，请检查相机权限后重试', icon: 'none' }),
      })
    },
    goLogin() {
      uni.navigateTo({ url: '/pages/competition-app/login' })
    },
  },
}
</script>

<style scoped>
.entry-page{min-height:100vh;padding:84rpx 32rpx;box-sizing:border-box;background:linear-gradient(180deg,#eff6ff 0,#f7f9fd 45%,#fff 100%);color:#102a56}.brand{display:flex;flex-direction:column;gap:12rpx;font-size:52rpx;font-weight:700}.brand text:last-child{font-size:27rpx;font-weight:400;color:#6e819e}.hero{display:flex;flex-direction:column;margin-top:52rpx;padding:38rpx;border-radius:28rpx;background:linear-gradient(135deg,#2864d7,#4388ef);color:#fff;box-shadow:0 16rpx 34rpx rgba(39,100,215,.22)}.hero-tag{align-self:flex-start;padding:7rpx 16rpx;border-radius:22rpx;background:rgba(255,255,255,.18);font-size:22rpx}.hero-title{margin-top:21rpx;font-size:40rpx;font-weight:700}.hero-copy{margin-top:16rpx;font-size:25rpx;line-height:1.7;opacity:.9}.card{display:flex;flex-direction:column;align-items:center;margin-top:30rpx;padding:50rpx 34rpx;border-radius:26rpx;background:#fff;box-shadow:0 12rpx 30rpx rgba(25,54,94,.08);text-align:center}.scan-mark{display:flex;align-items:center;justify-content:center;width:100rpx;height:100rpx;border-radius:50%;background:#eaf2ff;color:#1765df;font-size:66rpx}.card-title{margin-top:24rpx;font-size:35rpx;font-weight:700}.card-copy{margin-top:12rpx;font-size:25rpx;color:#71829b}.primary{width:100%;margin-top:34rpx;background:#1765df;color:#fff;border-radius:16rpx;font-size:30rpx}.login-row{display:flex;justify-content:center;gap:14rpx;margin-top:38rpx;font-size:25rpx;color:#7d8ba1}.login-link{color:#1765df}
</style>
