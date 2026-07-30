<template>
  <view class="login-page">
    <view class="brand"><text>竞赛选手端</text><text class="brand-sub">企业内部技能竞赛</text></view>

    <view v-if="phase === 'activation' && entryResolved" class="event-card">
      <text class="event-tag">赛事激活</text>
      <text class="event-name">{{ currentEvent.eventName }}</text>
      <text class="event-status">请验证企业名单中预留的手机号</text>
    </view>

    <view v-if="phase === 'login'" class="card">
      <view class="card-title">选手登录</view>
      <text class="hint">账号为手机号，初始密码为本人身份证后六位。</text>
      <input v-model.trim="mobile" class="input" type="number" maxlength="11" placeholder="请输入手机号" />
      <input v-model="password" class="input password" password placeholder="请输入密码" />
      <button class="primary" :loading="submitting" @click="login">登录</button>

      <view v-if="pendingActivation" class="pending-panel">
        <text class="pending-title">账号未激活</text>
        <text class="pending-copy">请扫描赛事二维码，完成短信验证后即可直接进入竞赛端。</text>
        <button class="primary activation-button" :loading="submitting" @click="scanActivation">扫码激活</button>
      </view>
    </view>

    <view v-else-if="entryResolved" class="card">
      <view class="card-title">短信验证</view>
      <text class="hint">验证成功后将自动登录并进入竞赛首页。</text>
      <input v-model.trim="mobile" class="input" type="number" maxlength="11" placeholder="手机号" disabled />
      <view class="code-row" v-if="challengeId">
        <input v-model.trim="code" class="input code" type="number" maxlength="6" placeholder="请输入 6 位短信验证码" />
        <button class="send ghost" :disabled="cooldown > 0" @click="sendCode">{{ cooldown ? `${cooldown}s后重发` : '重新发送' }}</button>
      </view>
      <button v-else class="primary" :loading="submitting" @click="sendCode">获取短信验证码</button>
      <button v-if="challengeId" class="primary" :loading="submitting" @click="verifyActivation">验证并登录</button>
      <text class="link" @click="backToLogin">返回账号密码登录</text>
    </view>

    <view v-else class="invalid">
      <text>正在验证赛事二维码…</text>
      <text v-if="entryError" class="error">{{ entryError }}</text>
      <text class="link" @click="backToLogin">返回账号密码登录</text>
    </view>
    <text class="privacy">手机号仅用于选手身份验证和赛事通知</text>
  </view>
</template>

<script>
import { getCurrentPlayer, playerLogin, resolvePlayerEntry, sendActivationCode, verifyActivation } from '@/api/competition-player'
import { parseCompetitionPlayerEntry } from '@/utils/competition-entry'
import { useCompetitionPlayerStore } from '@/store/competitionPlayer'

export default {
  data: () => ({
    phase: 'login', entryResolved: false, entryError: '', currentEvent: {}, mobile: '', password: '',
    code: '', challengeId: null, cooldown: 0, submitting: false, timer: null, pendingActivation: false,
  }),
  onLoad(options) {
    this.mobile = options?.mobile || options?.account || ''
    if (options?.entry) {
      this.resolveEntry(options.entry)
    }
  },
  onUnload() { if (this.timer) clearInterval(this.timer) },
  methods: {
    validMobile() {
      if (!/^1\d{10}$/.test(this.mobile)) {
        uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
        return false
      }
      return true
    },
    async login() {
      if (!this.validMobile()) return
      if (!this.password || this.password.length < 6) {
        uni.showToast({ title: '请输入正确的密码', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        const result = await playerLogin({ account: this.mobile, password: this.password })
        if (result?.status === 'pending_activation') {
          this.pendingActivation = true
          uni.showToast({ title: '账号未激活，请扫码激活', icon: 'none' })
          return
        }
        if (result?.status !== 'active' || !result.session?.token) {
          throw new Error('登录结果无效，请重试')
        }
        await this.completeLogin(result.session)
      } catch (error) {
        this.pendingActivation = false
        uni.showToast({ title: error.message || '账号或密码错误', icon: 'none' })
      } finally {
        this.submitting = false
      }
    },
    scanActivation() {
      if (!this.validMobile()) return
      uni.scanCode({
        onlyFromCamera: true,
        success: ({ result }) => {
          const entryCode = parseCompetitionPlayerEntry(result)
          if (!entryCode) {
            uni.showToast({ title: '不是有效的赛事激活二维码', icon: 'none' })
            return
          }
          this.phase = 'activation'
          this.entryResolved = false
          this.entryError = ''
          this.challengeId = null
          this.code = ''
          this.resolveEntry(entryCode)
        },
        fail: () => uni.showToast({ title: '扫码未完成，请检查相机权限后重试', icon: 'none' }),
      })
    },
    async resolveEntry(entryCode) {
      try {
        const entry = await resolvePlayerEntry(entryCode)
        useCompetitionPlayerStore().setEntry({ entryCode, ...entry })
        this.currentEvent = entry
        this.entryResolved = true
      } catch (error) {
        this.entryError = error.message || '赛事二维码无效或赛事已关闭'
      }
    },
    async sendCode() {
      if (!this.validMobile() || this.cooldown || !this.entryResolved) return
      this.submitting = true
      try {
        const data = await sendActivationCode({ entryCode: useCompetitionPlayerStore().entryCode, mobile: this.mobile })
        this.challengeId = data.challengeId
        this.startCooldown()
        uni.showToast({ title: '验证码已发送，请注意查收', icon: 'none' })
      } catch (error) {
        uni.showToast({ title: error.message || '验证码发送失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    },
    startCooldown() {
      this.cooldown = 60
      if (this.timer) clearInterval(this.timer)
      this.timer = setInterval(() => {
        this.cooldown -= 1
        if (this.cooldown <= 0) clearInterval(this.timer)
      }, 1000)
    },
    async verifyActivation() {
      if (!/^\d{6}$/.test(this.code)) {
        uni.showToast({ title: '请输入 6 位验证码', icon: 'none' })
        return
      }
      this.submitting = true
      try {
        const session = await verifyActivation({
          entryCode: useCompetitionPlayerStore().entryCode,
          mobile: this.mobile,
          challengeId: this.challengeId,
          code: this.code,
        })
        await this.completeLogin(session)
      } catch (error) {
        uni.showToast({ title: error.message || '激活失败', icon: 'none' })
      } finally {
        this.submitting = false
      }
    },
    async completeLogin(session) {
      if (!session?.token || !session?.tenantId) throw new Error('未获取到有效的竞赛登录会话')
      const store = useCompetitionPlayerStore()
      store.setSession({
        playerToken: session.token,
        playerInfo: { playerId: session.playerId, status: 'active' },
        tenantId: session.tenantId,
      })
      store.setProfile(await getCurrentPlayer())
      uni.reLaunch({ url: '/pages/competition-app/home' })
    },
    backToLogin() {
      this.phase = 'login'
      this.entryResolved = false
      this.entryError = ''
      this.challengeId = null
      this.code = ''
    },
  },
}
</script>

<style scoped>
.login-page{min-height:100vh;padding:84rpx 32rpx 44rpx;box-sizing:border-box;background:linear-gradient(180deg,#eff6ff 0,#f7f9fd 42%,#fff 100%)}.brand{display:flex;flex-direction:column;font-size:52rpx;font-weight:700;color:#102a56}.brand-sub{margin-top:12rpx;font-size:27rpx;font-weight:400;color:#6e819e}.event-card{display:flex;flex-direction:column;margin-top:46rpx;padding:34rpx;border-radius:26rpx;background:linear-gradient(135deg,#2864d7,#4388ef);color:#fff;box-shadow:0 16rpx 34rpx rgba(39,100,215,.22)}.event-tag{align-self:flex-start;padding:6rpx 16rpx;border-radius:22rpx;background:rgba(255,255,255,.18);font-size:22rpx}.event-name{margin-top:20rpx;font-size:38rpx;font-weight:700}.event-status{margin-top:12rpx;font-size:25rpx;opacity:.9}.card{margin-top:46rpx;padding:34rpx;border-radius:26rpx;background:#fff;box-shadow:0 12rpx 30rpx rgba(25,54,94,.08)}.event-card+.card{margin-top:26rpx}.card-title{font-size:34rpx;font-weight:700;color:#19345f}.hint{display:block;margin:20rpx 0;font-size:25rpx;line-height:1.6;color:#71829b}.input{height:92rpx;padding:0 24rpx;border-radius:16rpx;background:#f4f7fb;font-size:30rpx}.password{margin-top:16rpx}.code-row{display:flex;gap:16rpx;margin-top:16rpx}.code{flex:1}.send{width:188rpx!important;margin:0!important;font-size:24rpx!important}.primary{margin-top:22rpx;background:#1765df;color:#fff;border-radius:16rpx;font-size:30rpx}.ghost{background:#edf4ff!important;color:#1765df!important}.pending-panel{margin-top:30rpx;padding-top:26rpx;border-top:1rpx solid #e7eef8}.pending-title{display:block;font-size:29rpx;font-weight:600;color:#d8683e}.pending-copy{display:block;margin-top:10rpx;font-size:24rpx;line-height:1.6;color:#71829b}.activation-button{margin-top:18rpx}.invalid{display:flex;flex-direction:column;gap:20rpx;margin-top:64rpx;text-align:center;color:#6d7b91}.error{color:#ec5b53}.link{display:block;margin-top:26rpx;text-align:center;font-size:25rpx;color:#1765df}.privacy{display:block;margin-top:44rpx;text-align:center;font-size:22rpx;color:#9aa7ba}
</style>
