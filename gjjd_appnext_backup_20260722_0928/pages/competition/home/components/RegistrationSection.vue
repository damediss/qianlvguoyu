<template>
	<view class="registration-section bg-ff border-t-f0 border-b-f0">
		<view class="section-tabs dis-flex-vc">
			<view 
				class="tab-item dis-flex flex-column dis-flex-hv" 
				:class="{'active': currentTab === 'registration'}"
				@click="switchTab('registration')"
			>
				<text class="tab-text font-s18 font-wb" :class="currentTab === 'registration' ? 'color-33' : 'color-99'">报名</text>
				<view v-if="currentTab === 'registration'" class="tab-line bg-00b6ff"></view>
			</view>
			<view 
				class="tab-item dis-flex flex-column dis-flex-hv mar-L40" 
				:class="{'active': currentTab === 'ongoing'}"
				@click="switchTab('ongoing')"
			>
				<text class="tab-text font-s18 font-wb" :class="currentTab === 'ongoing' ? 'color-33' : 'color-99'">进行中</text>
				<view v-if="currentTab === 'ongoing'" class="tab-line bg-00b6ff"></view>
			</view>
			<view v-if="registrationData.status" class="status-tag bg-ff8800 color-ff font-s12 radius-4" style="margin-left: 10rpx; padding: 4rpx 12rpx;">
				{{registrationData.status}}
			</view>
		</view>
				
		<view class="registration-content">
			<view class="registration-list-item dis-flex-hsb dis-flex-vc bg-ff border-b-f0" @click="handleClickDetail">
				<view class="registration-item-text font-s14 color-33">{{registrationData.listItem}}</view>
				<i class="gjjd-iconfont icon-xiangyou font-s12 color-99"></i>
			</view>
			<view 
				v-if="registrationData.cover" 
				class="registration-card radius-12" 
				@click="handleClickCard" 
				:style="'background-image: url(' + registrationData.cover + '); background-size: cover; background-position: center;'"
			>
				<view class="registration-card-content">
					<view class="registration-icon">
						<i class="gjjd-iconfont color-ff" :class="registrationData.cardData.icon"></i>
					</view>
				</view>
				<view v-if="registrationData.cardData.subText" class="registration-overlay">
					<text class="registration-sub-text color-ff">{{registrationData.cardData.subText}}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'RegistrationSection',
	props: {
		registrationData: {
			type: Object,
			default: () => ({
				listItem: '',
				status: '',
				cover: '',
				cardData: {
					id: 0,
					icon: '',
					mainText: '',
					subText: ''
				}
			})
		}
	},
	data() {
		return {
			currentTab: 'registration'
		}
	},
	methods: {
		switchTab(tab) {
			if (tab === 'ongoing') {
				// 通知父组件处理"进行中"点击事件
				this.$emit('click-ongoing');
			} else {
				this.currentTab = tab;
			}
		},
		handleClickDetail() {
			this.$emit('click-detail');
		},
		handleClickCard() {
			this.$emit('click-card');
		}
	}
}
</script>

<style scoped>
.registration-section {
	margin-top: 20rpx;
	overflow: hidden;
}

.section-tabs {
	position: relative;
	padding: 20rpx 20rpx 0 20rpx;
}

.tab-item {
	position: relative;
	padding: 10rpx 0;
}

.tab-text {
	margin-bottom: 8rpx;
}

.tab-line {
	width: 60rpx;
	height: 4rpx;
	border-radius: 2rpx;
}

.registration-content {
	padding: 0 20rpx 20rpx 20rpx;
}

.registration-list-item {
	padding: 20rpx 0;
	margin-bottom: 20rpx;
}

.registration-item-text {
	flex: 1;
}

.registration-card {
	position: relative;
	overflow: hidden;
	height: 550rpx;
}

.registration-card-content {
	position: relative;
	height: 100%;
	display: flex;
	flex-direction: column;
	justify-content: center;
	align-items: center;
	padding: 50rpx 40rpx 120rpx;
	text-align: center;
}

.registration-icon i {
	font-size: 100rpx;
	text-shadow: 0 2rpx 8rpx rgba(0,0,0,0.3);
}

.registration-overlay {
	position: absolute;
	bottom: 20rpx;
	left: 0;
	right: 0;
	background: rgba(240, 240, 240, 0.6);
	padding: 20rpx 30rpx;
	display: flex;
	align-items: center;
	justify-content: flex-start;
}

.registration-sub-text {
	font-size: 26rpx;
	color: #333333;
	line-height: 1.4;
	text-align: left;
}
</style>