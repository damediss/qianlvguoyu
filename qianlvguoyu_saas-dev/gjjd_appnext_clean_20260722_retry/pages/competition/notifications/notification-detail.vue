<template>
	<view class="content bg-ff">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back w-60 h-100 dis-flex-hv" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb w-full text-C">通知详情</view>
				<view class="header-right w-60"></view>
			</view>
		</view>

		<!-- 占位元素，高度与 header 一致 -->
		<view class="header-placeholder" :style="'height:' + (50 + $statusBarHeight) + 'px;'"></view>

		<view class="body" style="padding-bottom: calc(env(safe-area-inset-bottom) + 60rpx);">
			<view class="notification-detail bg-ff">
				<view class="notification-title font-s20 font-wb color-33 mar-B20">{{ notificationDetail.title }}</view>
				<view class="notification-time font-s14 color-99 mar-B40">{{ notificationDetail.time }}</view>
				<view class="notification-content mar-T40">
					<rich-text class="content-text color-33" :nodes="notificationDetail.content"></rich-text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getNotificationDetail } from "@/api/competition";
import { isSuccess, showError } from '@/utils/response';

export default {
	data() {
		return {
			notificationId: null,
			notificationDetail: {
				title: '加载中...',
				time: '',
				content: ''
			}
		}
	},

	onLoad(options) {
		let params = {};

		if (options.item) {
			// 从 item 中解析参数
			try {
				params = JSON.parse(decodeURIComponent(options.item));
			} catch (e) {
				console.error('解析路由参数失败:', e);
				params = {};
			}
		} else {
			params = options;
		}

		// 获取通知ID
		if (params.id) {
			this.notificationId = parseInt(params.id);
			this.loadNotificationDetail();
		} else {
			uni.showToast({
				title: '参数错误',
				icon: 'none'
			});
			setTimeout(() => {
				uni.navigateBack();
			}, 1500);
		}
	},

	methods: {
		goBack() {
			uni.navigateBack();
		},

		/**
		 * 加载通知详情
		 */
		async loadNotificationDetail() {
			uni.showLoading({
				title: '加载中...'
			});

			try {
				// 调用 API 获取通知详情
				const res = await getNotificationDetail(this.notificationId);

				if (isSuccess(res) && res.data) {
					this.notificationDetail = {
						title: res.data.title || '',
						time: this.formatTime(res.data.createTime),
						content: res.data.content || '暂无内容'
					};
				} else {
					showError(res, '加载失败');
				}
			} catch (error) {
				console.error('加载通知详情失败:', error);
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				});
			} finally {
				uni.hideLoading();
			}
		},

		/**
		 * 格式化时间
		 */
		formatTime(timeStr) {
			if (!timeStr) return '';
			// 格式化为 YYYY-MM-DD HH:mm:ss
			return timeStr.replace('T', ' ').substring(0, 19);
		}
	}
}
</script>

<style scoped>
.content {
	min-height: 100vh;
}

.header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 9999;
}

.header-box {
	height: 100rpx;
	padding: 0 20rpx;
}

.body {
	padding: 40rpx 40rpx 40rpx 40rpx;
}

.content-text {
	line-height: 1.6;
	font-size: 32rpx;
}

.content-text>>>p {
	margin: 10rpx 0;
	line-height: 1.6;
}

.content-text>>>img {
	max-width: 100%;
	height: auto;
	display: block;
	margin: 10rpx 0;
}
</style>