<template>
	<view class="content bg-f8">
		<view class="header bg-f8 " :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 text-C">竞赛</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<SearchBar @search="handleSearch" />

			<RegistrationSection v-if="isLoggedIn" :registrationData="registrationData"
				@click-detail="goToRegistrationDetail" @click-card="goToRegistrationCard" 
				@click-ongoing="handleClickOngoing" />

			<StyleDisplaySection :styleDisplayList="styleDisplayList" @click-more="goToStyleDisplay"
				@click-item="goToStyleDetail" />

			<ParticipationSection v-if="isLoggedIn" :participationData="participationData" />

			<CurrentStageSection v-if="isLoggedIn && currentStageData && currentStageData.stageName" 
				:currentStageData="currentStageData"
				@view-detail="goToStageDetail" />

			<NewsSection :newsList="competitionInfoList" @click-more="goToCompetitionInfo"
				@click-item="goToInfoDetail" />
		</view>

		<view class="home-icon-container bg-7fd5f9 dis-flex-hv radius-full shadow-lg" @click="goToPromote">
			<uni-icons type="home" size="24" color="#fff"></uni-icons>
		</view>
	</view>
</template>

<script>
import { ref } from 'vue';
import { navTo, showMsg } from "@/utils";
import {
	getCompetitionHome,
	searchCompetition,
	checkRegistration
} from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess } from '@/utils/response';
import SearchBar from './components/SearchBar.vue';
import RegistrationSection from './components/RegistrationSection.vue';
import StyleDisplaySection from './components/StyleDisplaySection.vue';
import ParticipationSection from './components/ParticipationSection.vue';
import CurrentStageSection from './components/CurrentStageSection.vue';
import NewsSection from './components/NewsSection.vue';

export default {
	setup() {
		const userStore = useUserStore();
		const competitionStore = useCompetitionStore();
		const isLoggedIn = ref(false);

		return {
			userStore,
			competitionStore,
			isLoggedIn
		}
	},
	components: {
		SearchBar,
		RegistrationSection,
		StyleDisplaySection,
		ParticipationSection,
		CurrentStageSection,
		NewsSection
	},
	computed: {
		registrationData() {
			return this.competitionStore.registrationData || {}
		},
		participationData() {
			return this.competitionStore.participationData || {}
		},
		currentStageData() {
			return this.competitionStore.currentStageData || {}
		},
		styleDisplayList() {
			return this.competitionStore.styleDisplayList || []
		},
		competitionInfoList() {
			return (this.competitionStore.competitionInfoList || []).slice(0, 3)
		}
	},
	onLoad() {
		this.checkLoginStatus();
		this.loadCompetitionData();
	},
	methods: {
		/**
		 * 检查登录状态
		 */
		checkLoginStatus() {
			this.isLoggedIn = this.userStore.isLoggedIn;
		},

		/**
		 * 加载竞赛主页数据
		 */
		async loadCompetitionData() {
			// 获取当前登录用户ID
			// const userId = this.userStore.userInfo?.id || null;
			const userId = this.userStore.userId;  // 直接用userId字段

			
			// 使用 store 的 action 加载数据,传入userId
			await this.competitionStore.fetchCompetitionHome(userId);
		},

		goBack() {
			uni.navigateBack();
		},

		goToPromote() {
			uni.switchTab({
				url: '/pages/promote/index'
			});
		},

		async handleSearch(keyword) {
			try {
				const res = await searchCompetition(keyword);
				if (isSuccess(res)) {
					uni.showToast({
						title: '搜索成功',
						icon: 'success'
					});
				}
			} catch (error) {
				uni.showToast({
					title: '搜索失败',
					icon: 'none'
				});
			}
		},


		goToRegistrationDetail() {
			const id = this.registrationData.cardData?.id;
			if (!id) {
				uni.showToast({
					title: '竞赛ID不存在',
					icon: 'none'
				});
				return;
			}
			navTo('/pages/competition/home/detail', { id });
		},

		goToRegistrationCard() {
			const id = this.registrationData.cardData?.id;
			if (!id) {
				uni.showToast({
					title: '竞赛ID不存在',
					icon: 'none'
				});
				return;
			}
			navTo('/pages/competition/home/detail', { id });
		},

		/**
		 * 处理"进行中"Tab点击事件
		 * 检查报名状态后跳转
		 */
		async handleClickOngoing() {
			const competitionId = this.registrationData.cardData?.id;
			
			if (!competitionId) {
				uni.showToast({ title: '暂无进行中的竞赛', icon: 'none' });
				return;
			}
			
			try {
				// 检查报名状态
				const res = await checkRegistration(this.userStore.userId, competitionId);
				if (isSuccess(res) && res.data) {
					// 已报名，跳转到竞赛入口
					uni.navigateTo({
						url: `/pages/competition/entrance/index?competitionId=${competitionId}`
					});
				} else {
					uni.showToast({ title: '请先报名该竞赛', icon: 'none' });
				}
			} catch (error) {
				console.error('检查报名状态失败:', error);
				uni.showToast({ title: '检查报名状态失败', icon: 'none' });
			}
		},


		goToStyleDisplay() {
		},


		goToStyleDetail(item) {
		},

		goToStageDetail() {
			const competitionId = this.registrationData.cardData?.id;

			if (!competitionId) {
				uni.showToast({
					title: '竞赛信息未加载，请稍后重试',
					icon: 'none'
				});
				return;
			}

			uni.navigateTo({
				url: `/pages/competition/entrance/index?competitionId=${competitionId}`
			});
		},

		goToCompetitionInfo() {
			navTo('/pages/competition/info/list');
		},

		goToInfoDetail(item) {
			navTo('/pages/competition/info/detail', {
				id: item.id
			});
		}
	}
}
</script>

<style scoped>
@import './styles/home.css';

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
	padding: 0;
	margin: 0;
}

.home-icon-container {
	position: fixed;
	bottom: 30rpx;
	right: 30rpx;
	width: 80rpx;
	height: 80rpx;
	z-index: 100;
}
</style>