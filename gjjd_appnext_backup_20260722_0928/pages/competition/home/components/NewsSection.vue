<template>
	<view class="competition-info">
		<view class="info-container bg-ff border-t-f0 border-b-f0">
			<view class="section-header dis-flex-hsb dis-flex-vc border-b-f0" @click="handleClickMore">
				<text class="section-title font-s16 font-wb color-33">竞赛资讯</text>
				<i class="gjjd-iconfont icon-xiangyou font-s14 color-99"></i>
			</view>
			
			<view class="info-list bg-ff">
				<view 
					class="info-item border-b-f0" 
					v-for="item in newsList" 
					:key="item.id"
					@click="handleClickItem(item)"
				>
					<view class="info-content dis-flex-vc">
						<text class="info-text font-s14 color-33">{{item.title}}</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'NewsSection',
	props: {
		newsList: {
			type: Array,
			default: () => []
		}
	},
	methods: {
		handleClickMore() {
			this.$emit('click-more');
		},
		handleClickItem(item) {
			this.$emit('click-item', item);
		}
	}
}
</script>

<style scoped>
.competition-info {
	margin-top: 20rpx;
}

.info-container {
	position: relative;
}

.section-header {
	padding: 20rpx 20rpx;
	margin-bottom: 0;
}

.info-list {
	overflow: hidden;
}

.info-item {
	padding: 25rpx 20rpx;
}

.info-item:last-child {
	border-bottom: none;
}

.info-text {
	line-height: 1.2;
	font-weight: normal;
}
</style>