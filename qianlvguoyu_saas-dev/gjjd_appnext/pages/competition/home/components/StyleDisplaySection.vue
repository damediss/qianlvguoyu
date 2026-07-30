<template>
	<view class="style-display bg-ff border-t-f0 border-b-f0">
		<view class="section-header dis-flex-hsb dis-flex-vc" @click="handleClickMore">
			<text class="section-title font-s16 font-wb color-33">风采展示</text>
			<i class="gjjd-iconfont icon-xiangyou font-s14 color-99"></i>
		</view>
			
		<view class="display-card" v-if="styleDisplayList && styleDisplayList.length > 0">
			<swiper 
				class="display-swiper" 
				:indicator-dots="true" 
				:autoplay="true" 
				:interval="3000" 
				:duration="500"
				indicator-color="rgba(0,0,0,0.3)"
				indicator-active-color="#73a9fa"
			>
				<swiper-item v-for="(item, index) in styleDisplayList" :key="index">
					<view class="display-item" @click="handleClickItem(item)">
						<image :src="item.image" mode="aspectFill" class="display-image"></image>
						<view class="display-caption color-ff font-s14">{{item.caption}}</view>
					</view>
				</swiper-item>
			</swiper>
		</view>
		
		<view v-else class="no-data">
			<text class="font-s14 color-99">暂无风采展示</text>
		</view>
	</view>
</template>

<script>
export default {
	name: 'StyleDisplaySection',
	props: {
		styleDisplayList: {
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
.style-display {
	margin-top: 20rpx;
	overflow: hidden;
}

.section-header {
	margin-bottom: 20rpx;
	padding: 20rpx 20rpx 0 20rpx;
}

.display-card {
	overflow: hidden;
	margin: 0 20rpx 20rpx 20rpx;
}

.display-swiper {
	height: 350rpx;
}

.display-item {
	position: relative;
	height: 100%;
}

.display-image {
	width: 100%;
	height: 100%;
}

.display-caption {
	position: absolute;
	bottom: 0;
	left: 0;
	right: 0;
	background: linear-gradient(transparent, rgba(0,0,0,0.7));
	padding: 40rpx 20rpx 20rpx;
	line-height: 1.4;
}

.no-data {
	padding: 60rpx 0;
	text-align: center;
}
</style>