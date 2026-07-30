<template>
	<view class="score-section dis-flex-hsb dis-flex-vc bg-ff shadow-sm radius-32" @click="handleClick">
		<view class="score-content dis-flex-vc">
			<text class="score-label font-s15 color-66">积分：</text>
			<text class="score-value font-s20 font-wb color-33">{{ userScore }}</text>
		</view>
		<i class="gjjd-iconfont icon-xiangyou font-s16 color-99"></i>
	</view>
</template>

<script>
export default {
	name: 'UserScorePanel',
	props: {
		userScore: {
			type: Number,
			default: 0
		}
	},
	methods: {
		handleClick() {
			this.$emit('click');
		}
	}
}
</script>

<style scoped>
.score-section {
	padding: 25rpx 30rpx;
	margin: 20rpx 0 30rpx 0;
	transition: opacity 0.2s;
}

.score-section:active {
	opacity: 0.7;
}

.score-value {
	margin-left: 10rpx;
}
</style>