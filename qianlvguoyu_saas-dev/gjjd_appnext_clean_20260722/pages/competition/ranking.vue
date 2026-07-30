<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb text-C">排行榜</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<view class="ranking-list dis-flex flex-column">
				<view class="ranking-item dis-flex-vc bg-ff shadow-sm radius-24 transition-all"
					v-for="(item, index) in rankingList" :key="item.id" :class="{ 'top-three': index < 3 }">
					<view class="rank-number dis-flex-hv">
						<view v-if="index === 0" class="trophy-wrapper dis-flex-hv">
							<image src="/static/images/rank-1.png" class="trophy-image"></image>
						</view>
						<view v-else-if="index === 1" class="trophy-wrapper dis-flex-hv">
							<image src="/static/images/rank-2.png" class="trophy-image"></image>
						</view>
						<view v-else-if="index === 2" class="trophy-wrapper dis-flex-hv">
							<image src="/static/images/rank-3.png" class="trophy-image"></image>
						</view>
						<text v-else class="rank-normal font-s16 color-33">{{ index + 1 }}</text>
					</view>

					<view class="user-name font-s16 color-33">{{ item.name }}</view>

					<view class="score dis-flex">
						<text class="score-value font-s18 font-wb color-red">{{ item.score }}</text>
						<text class="score-unit font-s14 color-red">分</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { request } from "@/utils/java.js";

export default {
	data() {
		return {
			matchId: null,
			rankingList: []
		}
	},
	onLoad(options) {
		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		}
		this.loadRankingData();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		async loadRankingData() {
			try {
				const res = await request({
					method: 'POST',
					data: {
						matchId: this.matchId
					},
					url: '/appcompetition/ranking'
				});
				if (res?.data?.list) {
					this.rankingList = res.data.list;
				}
			} catch (error) {
				console.error('加载排行榜失败:', error);
				uni.showToast({ title: '加载排行榜失败', icon: 'none' });
			}
		}
	}
}
</script>

<style scoped>
.content {
	min-height: 100vh;
	padding-bottom: 40rpx;
}

.header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 9999;
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.body {
	padding: 20rpx;
}

.ranking-list {
	gap: 15rpx;
}

.ranking-item {
	padding: 25rpx 30rpx;
}

.ranking-item.top-three {
	background: linear-gradient(135deg, #ffffff 0%, #f8f8f8 100%);
}

.ranking-item:active {
	transform: scale(0.98);
}

.rank-number {
	width: 80rpx;
}

.trophy-wrapper {
	position: relative;
	width: 60rpx;
	height: 60rpx;
}

.trophy-image {
	width: 48rpx;
	height: 48rpx;
}

.rank-normal {
	width: 60rpx;
	text-align: center;
}

.user-name {
	flex: 1;
	margin-left: 20rpx;
}

.score {
	align-items: baseline;
	gap: 4rpx;
}

.score-value {
	font-weight: bold;
}
</style>