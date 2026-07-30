<template>
	<view class="content bg-ff">
		<!-- 顶部导航和标签栏 -->
		<PageHeader :title="'竞赛'" :currentTab="currentTab" :statusBarHeight="$statusBarHeight" @back="goBack"
			@tab-change="switchTab" />

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<!-- 竞赛活动内容区域 -->
			<view class="content-area">
				<!-- 知识PK模块 -->
				<KnowledgePKModule @navigate="handleNavigate" />

				<!-- 线上学习模块 -->
				<OnlineLearningModule @show-rules="showRules" />

				<!-- 社群问答模块 -->
				<CommunityQAModule @show-rules="showRules" />

				<!-- 用户积分显示 -->
				<UserScorePanel :userScore="userScore" @click="goToScoreDetail" />

				<!-- 竞赛列表展示 -->
				<CompetitionList :competitionList="competitionList" :currentCompetitionId="competitionId"
					@select="handleSelectCompetition" />
			</view>
		</view>

		<view class="home-icon-container bg-7fd5f9 shadow-lg radius-full dis-flex-hv" @click="goToHome">
			<uni-icons type="home" size="28" color="#fff"></uni-icons>
		</view>

		<RulesModal :visible="modalConfig.visible" :title="modalConfig.title" :content="modalConfig.content"
			:confirmText="modalConfig.confirmText" @confirm="handleModalConfirm" @close="closeModal" />
	</view>
</template>

<script>
import { navTo } from "@/utils";
import { getEntrance } from "@/api/competition";
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess, showError } from '@/utils/response';
import PageHeader from './components/PageHeader.vue';
import KnowledgePKModule from './components/KnowledgePKModule.vue';
import OnlineLearningModule from './components/OnlineLearningModule.vue';
import CommunityQAModule from './components/CommunityQAModule.vue';
import UserScorePanel from './components/UserScorePanel.vue';
import CompetitionList from './components/CompetitionList.vue';
import RulesModal from './components/RulesModal.vue';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	components: {
		PageHeader,
		KnowledgePKModule,
		OnlineLearningModule,
		CommunityQAModule,
		UserScorePanel,
		CompetitionList,
		RulesModal
	},
	data() {
		return {
			competitionId: null,
			currentTab: 'ongoing',
			userScore: 0,
			competitionList: [],
			navigating: false,
			modalConfig: {
				visible: false,
				title: '',
				content: '',
				confirmText: '知道了',
				type: ''
			}
		}
	},
	onLoad(options) {
		// 只接受标准 URL 参数，不再支持 item 参数
		if (options.competitionId) {
			this.competitionId = parseInt(options.competitionId);
		}

		// 验证 competitionId 是否有效
		if (!this.competitionId || isNaN(this.competitionId)) {
			uni.showToast({
				title: '竞赛参数无效',
				icon: 'none'
			});
			return;
		}

		this.loadEntranceData();
	},

	onShow() {
		this.navigating = false;
		if (this.competitionId && this.userScore !== undefined) {
			this.refreshUserScore();
		}
	},

	methods: {
		goBack() {
			uni.navigateBack();
		},

		async loadEntranceData() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			uni.showLoading({ title: '加载中...' });
			try {
				const res = await getEntrance(this.competitionId, this.userStore.userId);
				if (isSuccess(res) && res.data) {
					// 同步更新 Store 中的 matchId，确保其他页面使用正确的竞赛ID
					this.competitionStore.setMatchId(this.competitionId);

					this.userScore = res.data.userScore || 0;
					if (res.data.competitionList && res.data.competitionList.length > 0) {
						this.competitionList = res.data.competitionList
							.map(item => ({
								id: item.id,
								title: item.title || '未命名竞赛',
								status: this.getCompetitionStatus(item),
								tag: item.id === this.competitionId ? '当前竞赛' : '其他竞赛',
								startDate: this.formatDate(item.startDate),
								endDate: this.formatDate(item.endDate),
								progress: this.calculateProgress(item),
								_endDate: new Date(item.endDate)
							}))
							.sort((a, b) => {
								if (a.id === this.competitionId) return -1;
								if (b.id === this.competitionId) return 1;
								return a._endDate - b._endDate;
							});
					}
				} else {
					showError(res, '加载失败');
				}
			} catch (error) {
				console.error('加载竞赛入口数据失败:', error);
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			} finally {
				uni.hideLoading();
			}
		},

		async refreshUserScore() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				const res = await getEntrance(this.competitionId, this.userStore.userId);
				if (isSuccess(res) && res.data) {
					this.userScore = res.data.userScore || 0;
				}
			} catch (error) {
				console.error('刷新积分失败:', error);
			}
		},

		getCompetitionStatus(competition) {
			const now = new Date();
			const startDate = new Date(competition.startDate);
			const endDate = new Date(competition.endDate);

			// 将结束日期设置为当天的23:59:59
			endDate.setHours(23, 59, 59, 999);

			if (now < startDate) return '未开始';
			else if (now > endDate) return '已结束';
			else return '进行中';
		},

		calculateProgress(competition) {
			const now = new Date();
			const startDate = new Date(competition.startDate);
			const endDate = new Date(competition.endDate);
			if (now < startDate) return 0;
			else if (now > endDate) return 100;
			else {
				const total = endDate - startDate;
				const elapsed = now - startDate;
				return Math.round((elapsed / total) * 100);
			}
		},

		formatDate(dateStr) {
			if (!dateStr) return '';
			return dateStr.substring(0, 10);
		},

		switchTab(tab) {
			if (tab === 'registration') {
				uni.showLoading({ title: '加载中...', mask: true });
				setTimeout(() => {
					uni.hideLoading();
					uni.navigateTo({
						url: '/pages/competition/home/index',
						animationType: 'slide-in-right',
						animationDuration: 300
					});
				}, 200);
				return;
			}
			if (tab === 'ranking') {
				uni.showLoading({ title: '加载中...', mask: true });
				setTimeout(() => {
					uni.hideLoading();
					uni.navigateTo({
						url: `/pages/competition/ranking?matchId=${this.competitionId}`,
						animationType: 'slide-in-right',
						animationDuration: 300
					});
				}, 200);
				return;
			}
			this.currentTab = tab;
		},

		goToScoreDetail() {
			if (this.navigating) {
				return;
			}
			if (!this.competitionId) {
				uni.showToast({ title: '竞赛ID不存在', icon: 'none' });
				return;
			}
			this.navigating = true;
			uni.navigateTo({
				url: `/pages/competition/score-detail?matchId=${this.competitionId}`,
				success: () => {
					setTimeout(() => { this.navigating = false; }, 500);
				},
				fail: () => {
					this.navigating = false;
				}
			});
		},

		goToHome() {
			uni.switchTab({ url: '/pages/index/index' });
		},

		handleNavigate(type) {
			const routes = {
				checkin: `/pages/competition/checkin?matchId=${this.competitionId}`,
				pk: 'showPKRules',
				teamPK: `/pages/competition/pk/team-pk?matchId=${this.competitionId}`,
				levels: `/pages/competition/pk/levels?matchId=${this.competitionId}`,
				adventure: '/pages/competition/adventure/adventure-levels'
			};

			if (type === 'pk') {
				this.showPKRules();
			} else if (routes[type]) {
				uni.showLoading({ title: '加载中...', mask: true });
				setTimeout(() => {
					uni.hideLoading();
					uni.navigateTo({
						url: routes[type],
						animationType: 'slide-in-right',
						animationDuration: 300
					});
				}, 200);
			}
		},

		showPKRules() {
			if (!this.competitionId) {
				uni.showToast({
					title: '竞赛ID不存在',
					icon: 'none'
				});
				return;
			}
			// 直接显示规则弹窗，不预加载题目
			this.showModal('pk', '说明', '个人竞技规则：\n1、系统随机两人进行比拼\n2、答题正确多者为胜，答题正确一样多，用时少者为胜', '确定');
		},

		showRules(type) {
			const rulesMap = {
				courseware: { title: '友情提示', content: '规则说明:\n阅读指定课件，按阅读时长获得相应积分', confirmText: '知道了' },
				article: { title: '友情提示', content: '规则说明:\n阅读指定文章，按阅读时长获得相应积分', confirmText: '知道了' },
				wrongQuestion: { title: '友情提示', content: '规则说明:\n错题学习是指查看自己曾经做错的题和多数人曾经做错的题', confirmText: '知道了' },
				fragment: { title: '友情提示', content: '规则说明:\n碎片化学习是指参赛人员每天利用空闲时间做参赛工种的相关习题', confirmText: '知道了' },
				mockExam: { title: '友情提示', content: '规则说明:\n模拟考试是指初赛期间完成参赛工种的模拟考试', confirmText: '知道了' },
				communityQA: { title: '友情提示', content: '规则说明:\n社群问答是指专家给出参赛相关问题，参赛者解答相应问题提交后，由专家评审获得相应积分', confirmText: '知道了' }
			};
			const rule = rulesMap[type];
			if (rule) {
				this.showModal(type, rule.title, rule.content, rule.confirmText);
			}
		},

		showModal(type, title, content, confirmText = '知道了') {
			this.modalConfig = {
				visible: true,
				title,
				content,
				confirmText,
				type
			};
		},

		closeModal() {
			this.modalConfig.visible = false;
		},

		handleModalConfirm() {
			const type = this.modalConfig.type;
			this.closeModal();

			const routes = {
				pk: '/pages/competition/pk/pk',
				courseware: `/pages/competition/learning/content-list?type=courseware&matchId=${this.competitionId}`,
				article: `/pages/competition/learning/content-list?type=article&matchId=${this.competitionId}`,
				wrongQuestion: '/pages/competition/learning/wrong-list',
				fragment: '/pages/competition/learning/fragment-practice',
				mockExam: `/pages/competition/learning/exam-mock?matchId=${this.competitionId}&paperId=110`,
				communityQA: `/pages/competition/community/community-qa?matchId=${this.competitionId}`
			};

			if (routes[type]) {
				uni.navigateTo({
					url: routes[type],
					animationType: 'slide-in-right',
					animationDuration: 300
				});
			}
		},

		// 切换到其他竞赛
		handleSelectCompetition(competitionId) {
			if (competitionId === this.competitionId) {
				return;
			}

			uni.showLoading({ title: '切换中...', mask: true });
			setTimeout(() => {
				uni.hideLoading();
				// 使用 redirectTo 替换当前页面，避免页面栈过深
				uni.redirectTo({
					url: `/pages/competition/entrance/index?competitionId=${competitionId}`,
					animationType: 'fade-in',
					animationDuration: 200
				});
			}, 200);
		}
	}
}
</script>

<style scoped>
.content {
	min-height: 100vh;
	padding-bottom: 150rpx;
}

.body {
	padding-bottom: 140rpx;
}

.content-area {
	padding: 30rpx 20rpx;
	padding-top: 80rpx;
}

.home-icon-container {
	position: fixed;
	bottom: 50rpx;
	right: 35rpx;
	width: 110rpx;
	height: 110rpx;
	z-index: 100;
	transition: all 0.3s;
}

.home-icon-container:active {
	transform: scale(0.92);
	box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.2);
}
</style>