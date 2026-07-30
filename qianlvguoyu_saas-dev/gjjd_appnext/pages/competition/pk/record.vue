<template>
	<view class="content bg-f8">
		<view class="header bg-ff shadow-sm" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-vc w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">答题详情</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 20) + 'px;'">
			<view v-if="records.length > 0">
				<view v-for="(record, index) in records" :key="index" class="record-item bg-ff radius-16 shadow-sm">
					<view class="record-header dis-flex-hsb border-b-f0">
						<text class="level-title font-s16 font-wb color-33">{{ record.levelName }}</text>
						<text class="record-status font-s14 font-wb"
							:class="record.correct ? 'color-00b6ff' : 'color-ff6b6b'">
							{{ record.correct ? '正确' : '错误' }}
						</text>
					</view>

					<view class="question-content">
						<text class="question-label font-s14 font-wb color-66">题目</text>
						<text class="question-text font-s15 color-33">{{ record.question }}</text>
					</view>

					<view class="score-info">
						<text class="score-text font-s16 font-wb"
							:class="record.correct ? 'color-00b6ff' : 'color-ff6b6b'">
							{{ record.correct ? '+' : '-' }}{{ record.score }}积分
						</text>
					</view>
				</view>
			</view>

			<!-- 加载完成但无数据时显示空状态 -->
			<view v-else-if="loaded" class="empty-state dis-flex-hv flex-column">
				<text class="empty-text font-s15 color-99">暂无答题记录</text>
			</view>
		</view>
	</view>
</template>

<script>
import { getAdventureHistory } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	data() {
		return {
			records: [],
			loaded: false // 标记是否加载完成
		}
	},
	computed: {
		matchId() {
			return this.competitionStore.currentMatchId;
		}
	},
	onLoad() {
		if (!this.competitionStore.checkMatchId()) return;
		this.loadRecords();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		/**
		 * 从后端API加载答题记录
		 */
		async loadRecords() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				uni.showLoading({ title: '加载中...' });

				const res = await getAdventureHistory({
					matchId: this.matchId,
					userId: this.userStore.userId
				});

				uni.hideLoading();
				this.loaded = true; // 标记加载完成

				// 后端 Result 类定义：1-成功，0-失败
				if (res.code === 1 && res.data) {
					this.records = (res.data || []).map(item => {
						return {
							levelName: item.checkpointTitle,
							question: item.content,
							correct: item.isCorrect,
							score: Math.abs(item.getPoint || 1),
							timestamp: item.createTime ? new Date(item.createTime).getTime() : Date.now()
						};
					});
				} else {
					uni.showToast({
						title: res.msg || '加载失败',
						icon: 'none'
					});
				}
			} catch (error) {
				console.error('加载答题记录失败:', error);
				uni.hideLoading();
				this.loaded = true; // 即使失败也标记加载完成
				uni.showToast({
					title: '加载失败，请重试',
					icon: 'none'
				});
			}
		},

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
	z-index: 100;
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.header-title {
	flex: 1;
}

.body {
	padding: 0 20rpx 20rpx 20rpx;
}

.record-item {
	margin-bottom: 20rpx;
	padding: 25rpx 30rpx;
}

.record-header {
	margin-bottom: 20rpx;
	padding-bottom: 20rpx;
}

.question-content {
	margin-bottom: 20rpx;
}

.question-label {
	display: block;
	margin-bottom: 10rpx;
}

.question-text {
	display: block;
	line-height: 1.6;
}

.score-info {
	display: flex;
	justify-content: flex-end;
}

.empty-state {
	min-height: 60vh;
}
</style>