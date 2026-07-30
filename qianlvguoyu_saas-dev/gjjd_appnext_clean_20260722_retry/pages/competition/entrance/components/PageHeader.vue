<template>
	<view>

		<view class="header bg-f8" :style="'padding-top:' + statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="handleBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb text-C">{{ title }}</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="tabs-container dis-flex bg-ff " :style="'top:' + (50 + statusBarHeight) + 'px;'">
			<view class="tab-item dis-flex flex-column dis-flex-hv" :class="{ 'active': currentTab === 'registration' }"
				@click="handleTabChange('registration')">
				<text class="tab-text font-s16"
					:class="currentTab === 'registration' ? 'color-33 font-wb' : 'color-99'">报名</text>
				<view class="tab-line bg-00b6ff radius-3" v-if="currentTab === 'registration'"></view>
			</view>
			<view class="tab-item dis-flex flex-column dis-flex-hv" :class="{ 'active': currentTab === 'ongoing' }"
				@click="handleTabChange('ongoing')">
				<text class="tab-text font-s16"
					:class="currentTab === 'ongoing' ? 'color-33 font-wb' : 'color-99'">进行中</text>
				<view class="tab-line bg-00b6ff radius-3" v-if="currentTab === 'ongoing'"></view>
			</view>
			<view class="tab-item dis-flex flex-column dis-flex-hv" :class="{ 'active': currentTab === 'ranking' }"
				@click="handleTabChange('ranking')">
				<text class="tab-text font-s16"
					:class="currentTab === 'ranking' ? 'color-33 font-wb' : 'color-99'">排行榜</text>
				<view class="tab-line bg-00b6ff radius-3" v-if="currentTab === 'ranking'"></view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'PageHeader',
	props: {
		title: {
			type: String,
			default: '竞赛'
		},
		currentTab: {
			type: String,
			default: 'ongoing'
		},
		statusBarHeight: {
			type: Number,
			default: 0
		}
	},
	methods: {
		handleBack() {
			this.$emit('back');
		},
		handleTabChange(tab) {
			this.$emit('tab-change', tab);
		}
	}
}
</script>

<style scoped>
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

.tabs-container {
	padding: 0 30rpx;
	position: fixed;
	left: 0;
	right: 0;
	z-index: 99;
}

.tab-item {
	flex: 1;
	padding: 10rpx 0;
	position: relative;
	transition: all 0.2s ease;
}

.tab-item:active {
	transform: scale(0.95);
	opacity: 0.7;
}

.tab-text {
	margin-bottom: 8rpx;
	transition: all 0.3s ease;
}

.tab-line {
	position: absolute;
	bottom: 3rpx;
	width: 60rpx;
	height: 4rpx;
}
</style>