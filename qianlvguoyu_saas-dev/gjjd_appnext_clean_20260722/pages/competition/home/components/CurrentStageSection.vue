<template>
	<view class="current-stage bg-ff border-t-f0 border-b-f0">
		<view class="current-stage-header dis-flex-hsb dis-flex-vc">
			<text class="section-title font-s16 font-wb color-33">当前赛段</text>
		</view>
		
		<view class="stage-card bg-ff border-797979 radius-16" @click="handleViewDetail">
			<view class="stage-content">
				<text class="stage-name font-s12 color-33">{{currentStageData.stageName}}</text>
				<text class="stage-date font-s12 color-92">{{currentStageData.startDate}}至{{currentStageData.endDate}}</text>
			</view>
			<view class="view-button bg-00abf4">
				<text class="view-text color-ff font-s12">查看</text>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'CurrentStageSection',
	props: {
		currentStageData: {
			type: Object,
			default: () => ({
				stageName: '',
				startDate: '',
				endDate: ''
			})
		}
	},
	methods: {
		handleViewDetail() {
			this.$emit('view-detail');
		}
	}
}
</script>

<style scoped>
.current-stage {
	margin: 20rpx 0;
	padding: 20rpx;
}

.current-stage-header {
	margin-bottom: 20rpx;
}

.stage-card {
	display: flex;
	align-items: center;
	gap: 20rpx;
	padding: 24rpx;
}

.stage-content {
	display: flex;
	flex: 1;
	min-width: 0;
	align-items: center;
	gap: 20rpx;
}

.stage-name {
	flex: 1;
	min-width: 0;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.stage-date {
	flex-shrink: 0;
	white-space: nowrap;
}

.view-button {
	flex-shrink: 0;
	border-radius: 30rpx;
	padding: 0 40rpx;
	height: 66rpx;
	display: flex;
	align-items: center;
	justify-content: center;
}
</style>