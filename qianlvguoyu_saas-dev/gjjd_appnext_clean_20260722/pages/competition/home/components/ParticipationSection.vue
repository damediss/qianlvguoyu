<template>
	<view class="participation-status bg-ff border-t-f0 border-b-f0">
	<view class="participation-header dis-flex-hsb dis-flex-vc">
		<text class="section-title font-s16 font-wb color-33">参赛情况</text>
		<view class="registration-count-card dis-flex-vc">
			<text class="count-label color-ff font-s12">报名人数</text>
			<text class="count-number color-ff font-s12">
				<text class="number font-wb font-s18">{{participationData.registrationCount}}</text>人
			</text>
		</view>
	</view>
		
		<view class="participation-item dis-flex-hsb dis-flex-vc border-t-f5">
			<text class="competition-name font-s16 color-33">{{participationData.competitionName}}</text>
			<text class="participant-count font-s15 color-ff9f40">{{participationData.participantCount}}人</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'ParticipationSection',
	props: {
		participationData: {
			type: Object,
			default: () => ({
				registrationCount: 0,
				competitionName: '',
				participantCount: 0
			})
		}
	}
}
</script>

<style scoped>
.participation-status {
	margin: 20rpx 0;
	padding: 20rpx 0 20rpx 20rpx;
}

.participation-header {
	margin-bottom: 20rpx;
}

.registration-count-card {
	background: linear-gradient(90deg, #ff9f40 0%, #ff7b00 100%);
	padding: 12rpx 20rpx;
	min-width: 100rpx;
	width: 300rpx;
	height: 50rpx;
	margin-left: auto;
	justify-content: center;
	align-items: center;
}

.count-label {
	margin-right: 8rpx;
}

.count-number {
	display: flex;
	align-items: baseline;
	gap: 8rpx;
}

.participation-item {
	padding: 15rpx 0;
}

.participant-count {
	margin-right: 40rpx;
}
</style>