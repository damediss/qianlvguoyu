<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-vc" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">社群问答</view>
				<view class="header-placeholder"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 20) + 'px;'">
			<!-- 空状态提示 -->
			<view v-if="!loading && qaList.length === 0" class="empty-state dis-flex-hv flex-column pad-60">
				<image src="/static/images/empty.png" class="empty-icon w-200 h-200 mar-B30" mode="aspectFit"></image>
				<text class="empty-text font-s18 color-99">暂无问答内容</text>
			</view>

			<view v-for="(item, index) in qaList" :key="index"
				class="qa-item bg-ff radius-16 shadow-sm mar-B20 overflow-hidden">
				<view class="qa-header dis-flex pad-20" @click="toggleExpand(index)">
					<view class="qa-avatar w-80 h-80 bg-e0 radius-full flex-shrink-0 mar-R20"></view>
					<view class="qa-info">
						<text class="qa-title font-s16 color-33 mar-B10 line-h-15">{{ item.title }}</text>
						<view class="qa-meta dis-flex gap-15">
							<text class="meta-text font-s12 color-99">来源:{{ item.source }}</text>
							<text class="meta-text font-s12 color-99">分类:{{ item.category }}</text>
							<text class="meta-text font-s12 color-99">回答问题:{{ item.answerCount }}个</text>
							<text class="meta-text font-s12 color-99">浏览:{{ item.viewCount }}次</text>
						</view>
					</view>
					<view class="expand-icon mar-L10">
						<i class="gjjd-iconfont font-s16 color-99"
							:class="item.expanded ? 'icon-xiangshang' : 'icon-xiangxia'"></i>
					</view>
				</view>

				<view v-if="item.expanded" class="qa-content pad-20">
					<text class="content-text font-s15 color-33 line-h-18 mar-B20">{{ item.content }}</text>
					<view class="btn-wrapper dis-flex-he">
						<view class="answer-btn bg-00b6ff radius-16 dis-flex-hv transition-fast"
							@click="goToAnswer(item)">
							<text class="font-s15 font-wb color-ff">去回答</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { getQAList } from '@/api/competition';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess, showError } from '@/utils/response';

export default {
	setup() {
		const competitionStore = useCompetitionStore()
		return { competitionStore }
	},
	data() {
		return {
			matchId: null,
			qaList: [],
			loading: false
		}
	},
	onLoad(options) {
		// 优先从URL参数获取matchId，否则从store获取
		if (options.matchId) {
			this.matchId = parseInt(options.matchId);
		} else if (this.competitionStore.currentMatchId) {
			this.matchId = this.competitionStore.currentMatchId;
		}

		if (!this.matchId) {
			uni.showToast({ title: '竞赛ID不存在', icon: 'none' });
			setTimeout(() => uni.navigateBack(), 1500);
			return;
		}

		this.loadQAList();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		// 加载问答列表
		async loadQAList() {
			if (this.loading) return;

			this.loading = true;
			uni.showLoading({
				title: '加载中...'
			});

			try {
				const res = await getQAList({ matchId: this.matchId });
				if (isSuccess(res) && res.data && res.data.list) {
					this.qaList = res.data.list.map(item => ({
						...item,
						expanded: false // 默认收起
					}));
				} else {
					showError(res, '获取问答列表失败');
				}
			} catch (error) {
				console.error('【社区问答】获取问答列表失败：', error);
				uni.showToast({
					title: '网络错误，请稍后重试',
					icon: 'none'
				});
			} finally {
				this.loading = false;
				uni.hideLoading();
			}
		},

		toggleExpand(index) {
			this.qaList[index].expanded = !this.qaList[index].expanded;
		},

		goToAnswer(item) {
			uni.navigateTo({
				url: `/pages/competition/community/qa-submit?id=${item.id}&title=${encodeURIComponent(item.title)}`
			});
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
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.header-box {
	height: 50px;
	padding: 0 30rpx;
}

.header-back {
	width: 60rpx;
	height: 60rpx;
}

.header-title {
	flex: 1;
}

.header-placeholder {
	width: 60rpx;
}

.body {
	padding: 20rpx;
}

.qa-header {
	align-items: flex-start;
	cursor: pointer;
}

.qa-info {
	flex: 1;
}

.qa-title {
	display: block;
}

.qa-meta {
	flex-wrap: wrap;
}

.expand-icon {
	padding: 5rpx;
}

.qa-content {
	border-top: 1rpx solid #f0f0f0;
}

.content-text {
	display: block;
}

.answer-btn {
	padding: 12rpx 50rpx;
	min-width: 150rpx;
}

.answer-btn:active {
	transform: scale(0.95);
	opacity: 0.9;
}

.empty-state {
	min-height: 60vh;
}

.empty-icon {
	opacity: 0.6;
}
</style>