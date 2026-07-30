<template>
	<view class="content bg-f8">
		<view class="header bg-f8 " :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb text-C">竞赛详情</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<view class="notification-cards">
				<view class="notification-layout dis-flex">
					<view class="notification-card large-card competition-notice-bg dis-flex-hv radius-12 shadow-md"
						@click="goToNotification('competition')">
						<text class="card-text color-ff font-s16 font-wb">大赛通知</text>
					</view>

					<view class="right-cards dis-flex flex-column">
						<view class="notification-card small-card training-notice-bg dis-flex-hv radius-12 shadow-md"
							@click="goToNotification('training')">
							<text class="card-text color-ff font-s16 font-wb">赛前培训通知</text>
						</view>
						<view class="notification-card small-card referee-notice-bg dis-flex-hv radius-12 shadow-md"
							@click="goToNotification('referee')">
							<text class="card-text color-ff font-s16 font-wb">裁判培训通知</text>
						</view>
					</view>
				</view>
			</view>

			<view class="competition-info bg-ff radius-12 shadow-sm">
				<view class="competition-title font-s18 font-wb color-33">{{ competitionDetail.title }}</view>
				<view class="activity-date font-s14 color-66">活动日期: {{ competitionDetail.startDate }} - {{ competitionDetail.endDate }}</view>
			</view>

			<view class="section bg-ff radius-12 shadow-sm">
				<view class="section-title font-s16 font-wb color-33">赛事说明</view>
				<!-- <rich-text class="section-content" :nodes="competitionDetail.description"></rich-text> -->
				<uv-parse :content="competitionDetail.description"></uv-parse>
			</view>

			<view class="section bg-ff radius-12 shadow-sm">
				<view class="section-title font-s16 font-wb color-33">赛程介绍</view>
				<!-- <rich-text class="section-content" :nodes="competitionDetail.schedule"></rich-text> -->
				<uv-parse :content="competitionDetail.schedule"></uv-parse>
			</view>

			<view class="section bg-ff radius-12 shadow-sm">
				<view class="section-title font-s16 font-wb color-33">报名流程</view>
				<view class="registration-steps">
					<!-- <view class="step-item" v-for="(step, index) in competitionDetail.registrationSteps" :key="index">
						<rich-text class="step-text" :nodes="step"></rich-text>
					</view> -->
					<uv-parse :content="competitionDetail.process_content"></uv-parse>
				</view>
			</view>
		</view>

		<view class="bottom-button bg-00b6ff dis-flex-hv" @click="handleBottomButton">
			<text class="button-text color-ff font-s16 font-wb">
				{{ isRegistered ? '进入比赛' : '我要报名' }}
			</text>
		</view>
	</view>
</template>

<script>
import { request,uploads,rectTo,navTo,showMsg } from "@/utils";
import { isSuccess, showError, showSuccess } from "@/utils/response";
import {
	getCompetitionDetail,
	checkRegistration,
	submitRegistration
} from "@/api/competition";
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
			competitionId: null,
			competitionDetail: {
				title: '加载中...',
				startDate: '',
				endDate: '',
				description: '',
				schedule: '',
				registrationSteps: []
			},
			isRegistered: false
		}
	},
	onLoad(options) {
		if (options.id) {
			this.competitionId = parseInt(options.id);
			this.loadCompetitionDetail();
			this.checkRegistrationStatus();
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		goToNotification(type) {
			const notificationConfig = {
				'competition': { type: 0, title: '大赛通知' },
				'training': { type: 1, title: '赛前培训通知' },
				'referee': { type: 2, title: '裁判培训通知' }
			};

			const config = notificationConfig[type];
			if (config) {
				navTo('/pages/competition/notifications/notification-list', {
					type: config.type,
					title: config.title,
					matchId: this.competitionId
				});
			}
		},

		/**
		 * 检查报名状态
		 */
		async checkRegistrationStatus() {
			if (!this.userStore.checkLogin()) {
				this.isRegistered = false;
				return;
			}

			try {
				const res = await checkRegistration(this.userStore.userId, this.competitionId);
				if (isSuccess(res)) {
					this.isRegistered = res.data;
				}
			} catch (error) {
				console.error('检查报名状态失败:', error);
			}
		},

		/**
		 * 底部按钮点击事件
		 */
		handleBottomButton() {
			if (this.isRegistered) {
				this.goToEntrance();
			} else {
				this.showRegistrationForm();
			}
		},

		goToEntrance() {
			uni.navigateTo({
				url: `/pages/competition/entrance/index?competitionId=${this.competitionId}`
			});
		},

		/**
		 * 直接报名
		 */
		async showRegistrationForm() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			let username = '';
			let phone = '';
			const cachedUserInfo = uni.getStorageSync('userInfo');
			if (cachedUserInfo) {
				try {
					const user = typeof cachedUserInfo === 'string' ? JSON.parse(cachedUserInfo) : cachedUserInfo;
					username = user?.realname || user?.name || user?.username || '';
					phone = user?.phone || user?.mobile || user?.telephone || '';
				} catch (error) {
					console.warn('解析本地用户信息失败', error);
				}
			}
			if (!phone) {
				console.warn('本地缓存中未找到手机号');
			}

			uni.showLoading({ title: '报名中...' });
			let loadingVisible = true;

			try {
				const res = await submitRegistration({
					userId: this.userStore.userId,
					matchId: this.competitionId,
					username: username,
					phone: phone
				});

				if (loadingVisible) {
					uni.hideLoading();
					loadingVisible = false;
				}

				if (isSuccess(res)) {
					showSuccess(res, '报名成功！');
					this.isRegistered = true;
					setTimeout(() => {
						this.goToEntrance();
					}, 1500);
				} else {
					showError(res, '报名失败');
				}
			} catch (error) {
				if (loadingVisible) {
					uni.hideLoading();
					loadingVisible = false;
				}
				console.error('报名失败:', error);
				uni.showToast({ title: '网络错误，请重试', icon: 'none' });
			} finally {
				if (loadingVisible) {
					uni.hideLoading();
				}
			}
		},

		async loadCompetitionDetail() {
			uni.showLoading({
				title: '加载中...'
			});

			try {
				//let res = await getCompetitionDetail(this.competitionId);
				let res = await request({
					method:'POST',
					data:{matchId: this.competitionId},
					url:"/match/info"
				});
				//console.log("match res",res);
				if(res.code){
					showMsg('back',{title:res.msg});
					return;
				}
				res['code'] = 1;

				if (isSuccess(res) && res.data) {
					console.log("match info",res.data);
					this.competitionDetail = {
						title: res.data.title || '',
						startDate: res.data.startDate || '',
						endDate: res.data.endDate || '',
						description: res.data.description || '',
						schedule: res.data.schedule || '',
						registrationSteps: res.data.registrationSteps || [],
						process_content: res.data.process_content || ''
					};
				} else {
					showError(res, '数据加载失败');
				}
			} catch (error) {
				console.error('加载竞赛详情失败:', error);
				uni.showToast({ title: '加载失败，请重试', icon: 'none' });
			} finally {
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

.header-title {
	flex: 1;
}

.body {
	padding: 20rpx;
	padding-bottom: calc(env(safe-area-inset-bottom) + 120rpx);
}

.notification-cards {
	margin-bottom: 30rpx;
}

.notification-layout {
	gap: 20rpx;
	height: 410rpx;
}

.notification-card {
	position: relative;
	overflow: hidden;
}

.large-card {
	flex: 1;
	height: 100%;
}

.right-cards {
	flex: 1;
	gap: 20rpx;
}

.small-card {
	height: calc(50% - 10rpx);
}

.card-text {
	position: relative;
	z-index: 1;
}

.competition-notice-bg {
	background-image: url('/static/images/blue-bg-2.png');
	background-size: cover;
	background-position: center;
	background-repeat: no-repeat;
}

.training-notice-bg {
	background-image: url('/static/images/blue-bg-3.png');
	background-size: cover;
	background-position: center;
	background-repeat: no-repeat;
}

.referee-notice-bg {
	background-image: url('/static/images/orange-bg-0.png');
	background-size: cover;
	background-position: center;
	background-repeat: no-repeat;
}

.competition-info {
	padding: 30rpx;
	margin-bottom: 20rpx;
}

.competition-title {
	margin-bottom: 15rpx;
}

.section {
	padding: 30rpx;
	margin-bottom: 20rpx;
}

.section-title {
	margin-bottom: 20rpx;
}

.section-content {
	line-height: 1.6;
	font-size: 14px;
	color: #666666;
}

.section-content>>>p {
	margin: 10rpx 0;
	line-height: 1.6;
}

.section-content>>>img {
	max-width: 100%;
	height: auto;
	display: block;
	margin: 10rpx 0;
}

.section-content>>>ol,
.section-content>>>ul {
	padding-left: 30rpx;
	margin: 10rpx 0;
}

.section-content>>>li {
	margin: 5rpx 0;
}

.registration-steps {
	margin-top: 20rpx;
}

.step-item {
	margin-bottom: 15rpx;
}

.step-item:last-child {
	margin-bottom: 0;
}

.step-text {
	line-height: 1.6;
	font-size: 14px;
	color: #666666;
}

.step-text>>>p {
	margin: 5rpx 0;
	line-height: 1.4;
}

.step-text>>>ol,
.step-text>>>ul {
	padding-left: 20rpx;
	margin: 5rpx 0;
}

.step-text>>>li {
	margin: 3rpx 0;
}

.bottom-button {
	position: fixed;
	bottom: 30rpx;
	left: 40rpx;
	right: 40rpx;
	height: 90rpx;
	z-index: 100;
	border-radius: 45rpx;
}
</style>