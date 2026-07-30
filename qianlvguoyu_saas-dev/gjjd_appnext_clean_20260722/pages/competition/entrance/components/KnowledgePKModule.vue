<template>
	<view class="section">
		<view class="section-title font-s16 font-wb color-00">知识PK</view>

		<view class="card-grid">
			<view class="card-item dis-flex-hv small-card green-bg-0 shadow-sm h-75 radius-32" @click="handleCheckin">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/race-1.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-547e71">签到</text>
			</view>

			<view class="card-item dis-flex-hv large-card blue-bg-2 shadow-sm h-75 radius-32" @click="handlePK">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/race-1.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-ff">个人竞技</text>
			</view>

			<view class="card-item dis-flex-hv small-card blue-bg-3 shadow-sm h-75 radius-32" @click="handleTeamPK">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/race-2.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-ff">组队竞技</text>
			</view>

			<view class="card-item dis-flex-hv small-card orange-bg-0 shadow-sm h-75 radius-32" @click="handleLevels">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/race-5.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-ff">答题闯关</text>
			</view>

			<view class="card-item dis-flex-hv full-width-card blue-bg-0 shadow-sm h-75 radius-32"
				@click="handleAdventure">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/race-5.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-00bfff">大冒险</text>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'KnowledgePKModule',
	methods: {
		handleCheckin() {
			this.$emit('navigate', 'checkin');
		},
		handlePK() {
			this.$emit('navigate', 'pk');
		},
		handleTeamPK() {
			this.$emit('navigate', 'teamPK');
		},
		handleLevels() {
			this.$emit('navigate', 'levels');
		},
		handleAdventure() {
			this.$emit('navigate', 'adventure');
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