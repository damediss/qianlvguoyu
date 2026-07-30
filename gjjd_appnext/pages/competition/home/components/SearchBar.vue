<template>
	<view class="search-bar dis-flex-vc bg-ff border-b-f0">
		<view class="search-input-wrapper dis-flex-vc bg-f5 radius-25">
			<i class="gjjd-iconfont icon-sousuo search-icon font-s14 color-99"></i>
			<input 
				class="search-input font-s14 color-33" 
				v-model="keyword" 
				placeholder="请输入搜索内容"
				@confirm="handleSearch"
			/>
		</view>
		<view class="search-btn font-s16 color-66" @click="handleSearch">搜索</view>
	</view>
</template>

<script>
export default {
	name: 'SearchBar',
	data() {
		return {
			keyword: ''
		}
	},
	methods: {
		handleSearch() {
			if (!this.keyword.trim()) {
				uni.showToast({
					title: '请输入搜索内容',
					icon: 'none'
				});
				return;
			}
			this.$emit('search', this.keyword);
		}
	}
}
</script>

<style scoped>
.search-bar {
	padding: 15rpx 20rpx;
}

.search-input-wrapper {
	flex: 1;
	padding: 0 20rpx;
	height: 70rpx;
	margin-right: 20rpx;
}

.search-icon {
	margin-right: 10rpx;
}

.search-input {
	flex: 1;
}

.search-btn {
	padding: 0 20rpx;
	height: 70rpx;
	line-height: 70rpx;
}
</style>