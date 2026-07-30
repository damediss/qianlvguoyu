<template>
	<view class="section">
		<view class="section-title font-s16 font-wb color-00">竞赛列表</view>

		<view class="competition-list">
			<view class="competition-item bg-ff shadow-sm radius-32" 
				:class="[item.tag === '当前竞赛' ? 'border-current' : 'border-other', 'clickable']" 
				v-for="item in competitionList"
				:key="item.id"
				@click="handleSelect(item)">
				<text class="competition-name font-s15 font-wb color-00">{{ item.title }}</text>
				<view class="competition-status">
					<view class="circle-progress radius-full dis-flex-hv"
						:style="'background: conic-gradient(#57d801 0%, #57d801 ' + (item.progress || 65) + '%, #e0e0e0 ' + (item.progress || 65) + '%, #e0e0e0 100%)'">
						<view class="circle-inner bg-ff radius-full dis-flex-hv">
							<text class="font-s12 font-wb color-57d801">{{ item.status }}</text>
						</view>
					</view>
				</view>
				<view class="competition-info dis-flex flex-column">
					<text class="competition-tag bg-fff3e0 font-s12 color-ff9f40 font-wb radius-32">{{ item.tag }}</text>
					<text class="competition-date font-s12 color-99">竞赛时间：{{ item.startDate }} ~ {{ item.endDate }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'CompetitionList',
	props: {
		competitionList: {
			type: Array,
			default: () => []
		},
		currentCompetitionId: {
			type: Number,
			default: null
		}
	},
	methods: {
		handleSelect(item) {
			// 如果点击的是当前竞赛，不做任何操作
			if (item.tag === '当前竞赛') {
				return;
			}
			this.$emit('select', item.id);
		}
	}
}
</script>

<style scoped>
.section {
	margin-bottom: 30rpx;
}

.section-title {
	padding: 0 0 20rpx 0;
	margin-bottom: 10rpx;
}

.competition-list {
	margin-top: 10rpx;
}

.competition-item {
	padding: 30rpx;
	margin-bottom: 15rpx;
	position: relative;
}

/* 当前竞赛 - 红色边框高亮 */
.border-current {
	border: 2px solid #e37c4f;
}

/* 其他竞赛 - 灰色边框 */
.border-other {
	border: 2px solid #e0e0e0;
}

.competition-name {
	display: block;
	padding-right: 140rpx;
	line-height: 1.5;
	margin-bottom: 15rpx;
}

.competition-status {
	position: absolute;
	right: 30rpx;
	top: 50%;
	transform: translateY(-50%);
	width: 120rpx;
	height: 120rpx;
}

.circle-progress {
	width: 120rpx;
	height: 120rpx;
	position: relative;
}

.circle-inner {
	width: 102rpx;
	height: 102rpx;
	text-align: center;
}

.competition-info {
	gap: 10rpx;
}

.competition-tag {
	padding: 6rpx 18rpx;
	align-self: flex-start;
	display: inline-block;
}

.competition-date {
	line-height: 1.6;
}

/* 可点击样式 */
.clickable {
	cursor: pointer;
	transition: all 0.2s ease;
}

.clickable:active {
	transform: scale(0.98);
	opacity: 0.9;
}

/* 非当前竞赛悬停提示 */
.border-other.clickable:active {
	border-color: #ff9f40;
}
</style>