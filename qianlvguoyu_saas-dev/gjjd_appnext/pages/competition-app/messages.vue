<template>
  <PlayerPageShell :title="selectedMessage ? selectedMessage.title : '消息通知'" subtitle="公告由平台可靠投递后生成">
    <view v-if="selectedMessage" class="detail">
      <view class="detail-top">
        <text class="back" @click="closeDetail">‹ 返回消息列表</text>
        <text class="type-badge" :class="`type-${noticeType(selectedMessage)}`">{{ noticeTypeLabel(selectedMessage) }}</text>
      </view>
      <text class="detail-time">发布于 {{ formatTime(selectedMessage.publishedAt || selectedMessage.receivedAt) }}</text>

      <view v-if="noticeType(selectedMessage) === 'pdf'" class="pdf-card">
        <view class="pdf-icon">PDF</view>
        <view class="pdf-info">
          <text class="pdf-name">{{ pdfAsset?.originalName || '赛事文件' }}</text>
          <text class="pdf-meta">{{ formatSize(pdfAsset?.sizeBytes) }}</text>
        </view>
        <button class="preview-button" :loading="pdfLoading" @click="previewPdf">打开文件</button>
      </view>
      <text v-if="noticeType(selectedMessage) === 'pdf' && selectedMessage.content" class="pdf-intro">{{ selectedMessage.content }}</text>

      <view v-else class="markdown-content">
        <rich-text :nodes="markdownNodes" />
      </view>
    </view>

    <view v-else-if="messages.length" class="list">
      <view v-for="message in messages" :key="message.messageId" class="card" @click="openMessage(message)">
        <view class="card-main">
          <view class="title-row">
            <text class="title">{{ message.title }}</text>
            <text class="type-badge" :class="`type-${noticeType(message)}`">{{ noticeTypeLabel(message) }}</text>
          </view>
          <text class="content">{{ messageSummary(message) }}</text>
          <text class="time">{{ formatTime(message.publishedAt || message.receivedAt) }}</text>
        </view>
        <text v-if="!message.readAt" class="dot"></text>
      </view>
    </view>
    <view v-else class="empty">暂无消息通知</view>
  </PlayerPageShell>
</template>

<script>
import { getPlayerMessageAsset, getPlayerMessages, markPlayerMessageRead } from '@/api/competition-player'
import PlayerPageShell from './components/PlayerPageShell.vue'
import { guardCompetitionPlayerRoute } from '@/utils/competition-route-guard'
import { buildCompetitionNoticeMarkdownNodes } from '@/utils/competition-notice-renderer'

export default {
  components: { PlayerPageShell },
  data: () => ({
    messages: [],
    selectedMessage: null,
    imageUrls: {},
    assetResources: [],
    pdfLoading: false,
  }),
  computed: {
    markdownNodes() {
      return buildCompetitionNoticeMarkdownNodes(this.selectedMessage?.content, this.imageUrls)
    },
    pdfAsset() {
      return (this.selectedMessage?.assets || []).find((asset) => asset.assetType === 'pdf')
    },
  },
  onShow() {
    if (guardCompetitionPlayerRoute() && !this.selectedMessage) this.loadMessages()
  },
  onUnload() {
    this.releaseAssets()
  },
  methods: {
    async loadMessages() {
      try {
        this.messages = (await getPlayerMessages()).items || []
      } catch (error) {
        uni.showToast({ title: error.message || '消息加载失败', icon: 'none' })
      }
    },
    noticeType(message) {
      return message?.contentType === 'pdf' ? 'pdf' : 'markdown'
    },
    noticeTypeLabel(message) {
      return this.noticeType(message) === 'pdf' ? 'PDF 赛事文件' : '图文公告'
    },
    messageSummary(message) {
      if (this.noticeType(message) === 'pdf') {
        const asset = (message.assets || []).find((item) => item.assetType === 'pdf')
        return message.content || `附件：${asset?.originalName || 'PDF 赛事文件'}`
      }
      return String(message.content || '').replace(/!\[[^\]]*\]\(notice-asset:\/\/\d+\)/g, '【图片】')
    },
    async openMessage(message) {
      this.selectedMessage = message
      await this.markRead(message)
      if (this.noticeType(message) === 'markdown') await this.loadInlineImages(message)
    },
    async markRead(message) {
      if (message.readAt) return
      try {
        await markPlayerMessageRead(message.messageId)
        message.readAt = new Date().toISOString()
      } catch (error) {
        uni.showToast({ title: error.message || '消息状态更新失败', icon: 'none' })
      }
    },
    async loadInlineImages(message) {
      this.releaseAssets()
      const imageAssets = (message.assets || []).filter((asset) => asset.assetType === 'inline_image')
      const loaded = await Promise.all(imageAssets.map(async (asset) => {
        try {
          const resource = await getPlayerMessageAsset(message.messageId, asset.assetId)
          return { assetId: String(asset.assetId), resource }
        } catch (error) {
          return null
        }
      }))
      const imageUrls = {}
      loaded.filter(Boolean).forEach(({ assetId, resource }) => {
        imageUrls[assetId] = resource.url
        this.assetResources.push(resource)
      })
      this.imageUrls = imageUrls
    },
    async previewPdf() {
      if (!this.pdfAsset || this.pdfLoading) return
      this.pdfLoading = true
      // #ifdef H5
      const externalWindow = window.open('', '_blank')
      // #endif
      try {
        const resource = await getPlayerMessageAsset(
          this.selectedMessage.messageId,
          this.pdfAsset.assetId,
        )
        // #ifdef H5
        if (externalWindow) externalWindow.location.href = resource.url
        else window.location.href = resource.url
        // Blob URL must remain alive long enough for the external browser viewer to load it.
        setTimeout(() => resource.revoke?.(), 5 * 60 * 1000)
        // #endif
        // #ifndef H5
        uni.openDocument({
          filePath: resource.url,
          showMenu: true,
          fail: () => uni.showToast({ title: '本机无法打开该 PDF 文件', icon: 'none' }),
        })
        // #endif
      } catch (error) {
        // #ifdef H5
        if (externalWindow) externalWindow.close()
        // #endif
        uni.showToast({ title: error.message || 'PDF 打开失败', icon: 'none' })
      } finally {
        this.pdfLoading = false
      }
    },
    closeDetail() {
      this.releaseAssets()
      this.imageUrls = {}
      this.selectedMessage = null
    },
    releaseAssets() {
      this.assetResources.forEach((resource) => resource?.revoke?.())
      this.assetResources = []
    },
    formatTime(value) {
      return value ? String(value).replace('T', ' ').slice(0, 16) : ''
    },
    formatSize(value) {
      const size = Number(value)
      if (!Number.isFinite(size) || size <= 0) return ''
      return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.ceil(size / 1024)} KB`
    },
  },
}
</script>

<style scoped>
.list{display:flex;flex-direction:column;gap:18rpx}.card{display:flex;justify-content:space-between;gap:18rpx;padding:30rpx;background:#fff;border-radius:20rpx}.card-main{display:flex;flex:1;flex-direction:column;gap:12rpx;min-width:0}.title-row{display:flex;align-items:center;gap:12rpx}.title{overflow:hidden;flex:1;font-size:30rpx;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.type-badge{flex:none;padding:4rpx 12rpx;border-radius:20rpx;font-size:20rpx}.type-markdown{color:#2164c9;background:#eaf3ff}.type-pdf{color:#b74324;background:#fff0ed}.content{display:-webkit-box;overflow:hidden;color:#6e8099;font-size:25rpx;line-height:1.5;-webkit-box-orient:vertical;-webkit-line-clamp:2}.time,.detail-time{font-size:22rpx;color:#9ba8ba}.dot{width:15rpx;height:15rpx;margin-top:12rpx;border-radius:50%;background:#ea5c57}.empty{padding:48rpx;text-align:center;color:#8492a7;background:#fff;border-radius:20rpx}.detail{padding:2rpx 2rpx 30rpx}.detail-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:18rpx}.back{font-size:25rpx;color:#2672dc}.detail-time{display:block;margin-bottom:30rpx}.markdown-content{padding:30rpx;border-radius:20rpx;background:#fff}.pdf-card{display:flex;align-items:center;gap:20rpx;padding:26rpx;border:1px solid #ffd8d0;border-radius:20rpx;background:#fff7f5}.pdf-icon{padding:16rpx 12rpx;border-radius:10rpx;background:#e75137;color:#fff;font-size:22rpx;font-weight:700}.pdf-info{display:flex;flex:1;flex-direction:column;gap:8rpx;min-width:0}.pdf-name{overflow:hidden;color:#2b3850;font-size:27rpx;font-weight:700;text-overflow:ellipsis;white-space:nowrap}.pdf-meta{font-size:22rpx;color:#8f9caf}.preview-button{margin:0;padding:0 16rpx;border:0;border-radius:24rpx;background:#e75137;color:#fff;font-size:22rpx;line-height:56rpx}.pdf-intro{display:block;margin-top:22rpx;padding:26rpx;border-radius:20rpx;background:#fff;color:#344a6b;font-size:27rpx;line-height:1.75;white-space:pre-wrap}
</style>
