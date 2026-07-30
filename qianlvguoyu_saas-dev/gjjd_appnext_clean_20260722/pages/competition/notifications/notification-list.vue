<template>
	<view class="content bg-f5">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back w-60 h-100 dis-flex-hv" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb w-full text-C">{{ pageTitle }}</view>
				<view class="header-right w-60"></view>
			</view>
		</view>

		<!-- 占位元素，高度与 header 一致 -->
		<view class="header-placeholder" :style="'height:' + (50 + $statusBarHeight) + 'px;'"></view>

		<view class="body" style="padding-bottom: calc(env(safe-area-inset-bottom) + 40rpx);">
			<!-- 通知列表 -->
			<view v-if="notificationList.length > 0" class="notification-list dis-flex flex-column gap-20">
				<view v-for="item in notificationList" :key="item.id"
					class="notification-item bg-ff radius-12 shadow-sm pad-30 transition-all"
					@click="goToNotificationDetail(item)">
					<view class="item-title font-s16 font-wb color-33 mar-B15">{{ item.title }}</view>
					<view class="item-time font-s12 color-99">{{ item.time }}</view>
				</view>
			</view>

			<!-- 加载完成但无数据时显示空状态 -->
			<view v-else-if="loaded" class="empty-state dis-flex-hv flex-column">
				<text class="empty-text font-s18 color-99">暂无通知</text>
			</view>
		</view>
	</view>
</template>

<script>
import { navTo } from "@/utils";
import { getNotificationList } from "@/api/competition";
import { isSuccess, showError } from '@/utils/response';

export default {
	data() {
		return {
			type: 0,
			pageTitle: '通知列表',
			matchId: null,
			notificationList: [],
			loaded: false // 标记是否加载完成
		}
	},

	onLoad(options) {
		// 解析参数：支持两种方式
		// 1. 直接传递：{ type: "1", title: "xxx" }
		// 2. item方式：{ item: "{\"type\":1,\"title\":\"xxx\"}" }
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

		// 解析 type 和 title
		const type = params.type !== undefined ? parseInt(params.type) : 0;
		const title = params.title || '通知列表';
		const matchId = params.matchId !== undefined ? Number(params.matchId) : null;
		this.matchId = matchId;
		// 设置实例变量
		this.type = type;
		this.pageTitle = title;

		// 加载通知列表
		this.loadNotificationList(type);
	},

	methods: {
		goBack() {
			uni.navigateBack();
		},

		goToNotificationDetail(item) {
			navTo('/pages/competition/notifications/notification-detail', {
				id: item.id
			});
		},

		/**
		 * 加载通知列表
		 * @param {number} type - 通知类型（可选，如果不传则使用 this.type）
		 */
		async loadNotificationList(type) {
			// 如果传入了参数，优先使用参数；否则使用 this.type
			const requestType = type !== undefined ? type : this.type;

			uni.showLoading({
				title: '加载中...'
			});

			try {
				// 调用 API 获取指定类型的通知
				const res = await getNotificationList({
					type: requestType,
					matchId: this.matchId,
					page: 1,
					limit: 10
				});

				this.loaded = true; // 标记加载完成

				if (isSuccess(res) && res.data) {
					// 格式化数据
					this.notificationList = res.data.list.map(item => ({
						id: item.id,
						title: item.title,
						time: this.formatTime(item.createTime)
					}));
				} else {
					showError(res, '加载失败');
				}
			} catch (error) {
				this.loaded = true; // 即使失败也标记加载完成
				console.error('加载通知列表失败:', error);
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
			// 格式化为 YYYY-MM-DD HH:mm
			return timeStr.replace('T', ' ').substring(0, 16);
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
	padding: 40rpx 20rpx 20rpx 20rpx;
}

.notification-item:active {
	opacity: 0.8;
	transform: scale(0.98);
}

.item-title {
	line-height: 1.5;
}

.item-time {
	line-height: 1.4;
}

.empty-state {
	min-height: 60vh;
}

.empty-text {
	line-height: 1.5;
}
</style>