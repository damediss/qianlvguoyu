<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb text-C">竞赛资讯</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<view v-if="competitionInfoList.length > 0" class="info-list bg-ff radius-12">
				<view class="info-item border-b-f0" v-for="item in competitionInfoList" :key="item.id"
					@click="goToInfoDetail(item)">
					<text class="info-title font-s16 color-33 font-wb">{{ item.title }}</text>
					<view class="info-meta-row">
						<text class="info-date font-s12 color-99">{{ item.date }}</text>
						<text class="info-views font-s12 color-99">{{ item.views }}浏览</text>
					</view>
				</view>
			</view>

			<!-- 空状态 -->
			<view v-else-if="!loading" class="empty-state dis-flex-hv flex-column">
				<text class="empty-text font-s15 color-99">暂无资讯</text>
			</view>
		</view>
	</view>
</template>

<script>
import { navTo } from "@/utils";
import { getCompetitionInfoList } from "@/api/competition";
import { isSuccess, showError } from '@/utils/response';

export default {
	data() {
		return {
			competitionInfoList: [],
			page: 1,
			pageSize: 20,
			hasMore: true,
			loading: false
		}
	},
	onLoad() {
		this.loadCompetitionData();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		goToInfoDetail(item) {
			navTo('/pages/competition/info/detail', {
				id: item.id
			});
		},

		async loadCompetitionData() {
			this.loading = true;
			uni.showLoading({ title: '加载中...' });

			try {
				const res = await getCompetitionInfoList({
					page: this.page,
					pageSize: this.pageSize
				});

				if (isSuccess(res) && res.data) {
					this.competitionInfoList = res.data.list || [];
				} else {
					showError(res, '数据加载失败');
				}
			} catch (error) {
				console.error('加载资讯列表失败:', error);
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			} finally {
				this.loading = false;
				uni.hideLoading();
			}
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
	height: 50px;
	padding: 0 20rpx;
}

.body {
	padding: 20rpx;
}

.info-list {
	overflow: hidden;
}

.info-item {
	padding: 35rpx 30rpx;
	display: flex;
	flex-direction: column;
	gap: 15rpx;
}

.info-item:last-child {
	border-bottom: none;
}

.info-title {
	line-height: 1.6;
	word-break: break-all;
}

.info-meta-row {
	display: flex;
	align-items: center;
	gap: 20rpx;
}

.info-date {
	flex-shrink: 0;
}

.info-views {
	flex-shrink: 0;
}

.empty-state {
	min-height: 60vh;
}
</style>