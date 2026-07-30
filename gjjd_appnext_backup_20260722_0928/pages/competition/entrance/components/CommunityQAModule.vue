<template>
	<view class="section">
		<view class="section-title font-s16 font-wb color-00">社群问答</view>

		<view class="card-item dis-flex-hv full-width-card green-bg-0 shadow-sm h-75 radius-32" @click="handleClick">
			<view class="card-pattern"></view>
			<view class="card-icon dis-flex-hv">
				<image src="/static/images/s-3.png" style="width: 25px; height: 25px;"></image>
			</view>
			<text class="card-text font-s16 font-wb color-547e71">进入</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'CommunityQAModule',
	methods: {
		handleClick() {
			this.$emit('show-rules', 'communityQA');
		}
	}
}
</script>

<style scoped>
@import '../styles/cards.css';

.section {
	margin-bottom: 30rpx;
}

.section-title {
	padding: 0 0 20rpx 0;
	margin-bottom: 10rpx;
}
</style>