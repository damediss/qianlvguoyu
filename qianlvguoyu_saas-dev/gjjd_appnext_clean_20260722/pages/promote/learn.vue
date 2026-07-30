<template>
	<view class="content bg-ff">
	<view class="header bg-ff border-ee" :style="'padding-top:'+$statusBarHeight+'px;'">
		<view class="header-box dis-flex-hsb dis-flex-vc">
			<view class="header-back dis-flex-hv" @click="goBack">
				<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
			</view>
			<view class="header-title font-s18 font-wb">学习</view>
			<view class="header-placeholder"></view>
		</view>
	</view>
		
	<view class="body" :style="'margin-top:'+(50+$statusBarHeight)+'px;'">
	<!-- 开始学习按钮 -->
	<view class="start-learning-btn w-25 h-60 radius-25 dis-flex-hv bg-00abf4 color-ff font-s14 font-wb" @click="startLearning">
		开始学习
	</view>
		
	<!-- 标签栏 -->
		<view class="tabs-container">
			<view class="tabs-scroll dis-flex border-f0">
				<view 
					class="tab-item font-s12 color-66"
						:class="{'active color-007aff font-wb border-007aff': currentTab === 'detail'}" 
						@click="switchTab('detail')"
				>
					详情
				</view>
				<view 
					class="tab-item font-s12 color-66" 
					:class="{'active color-007aff font-wb border-007aff': currentTab === 'regular'}" 
					@click="switchTab('regular')"
				>
					常规题
				</view>
				<view 
					class="tab-item font-s12 color-66" 
					:class="{'active color-007aff font-wb border-007aff': currentTab === 'game'}" 
					@click="switchTab('game')"
				>
					游戏题
				</view>
				<view 
					class="tab-item font-s12 color-66" 
					:class="{'active color-007aff font-wb border-007aff': currentTab === 'mock'}" 
					@click="switchTab('mock')"
				>
					模拟题
				</view>
					<view 
						class="tab-item font-s12 color-66" 
						:class="{'active color-007aff font-wb border-007aff': currentTab === 'wrong'}" 
						@click="switchTab('wrong')"
				>
					错题库
				</view>
					<view 
						class="tab-item font-s12 color-66" 
						:class="{'active color-007aff font-wb border-007aff': currentTab === 'homework'}" 
						@click="switchTab('homework')"
					>
						线下作业
					</view>
				</view>
			</view>
			
		<!-- 课程列表 -->
		<view class="course-list bg-ff">
			<view 
				class="course-item dis-flex-hsb dis-flex-vc border-f0" 
				v-for="(item, index) in flatCourseList" 
				:key="item.id"
				:class="[{'disabled color-99': item.disabled}, 'level-' + item.level]"
				@click="selectCourse(item)"
			>
				<view class="course-text font-s12 color-33">{{item.title}}</view>
				<view class="course-arrow color-cc font-s10" v-if="!item.disabled && item.children && item.children.length > 0" @click.stop="toggleExpand(item)">
					<i class="gjjd-iconfont" :class="item.expanded ? 'icon-xiangshang' : 'icon-xiangxia'"></i>
				</view>
			</view>
		</view>
		</view>
	
<!-- 底部错题库按钮 -->
<view class="bottom-button dis-flex-hv bg-00abf4" @click="goToWrongBank">
	<image src="/static/images/error.png" class="bottom-button-icon" mode="aspectFit"></image>
	<view class="bottom-button-text color-84ff00 font-s18 font-wb">错题库</view>
</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				currentTab: 'regular', // 默认选中常规题
				courseList: [
					{
						id: 1,
						title: '第1章11',
						level: 1,
						disabled: false,
						expanded: true,
						children: [
							{
								id: 2,
								title: '1.1节',
								level: 2,
								disabled: false,
								expanded: true,
								children: [
									{
										id: 3,
										title: '1.1.1节',
										level: 3,
										disabled: false,
										expanded: true,
										children: [
											{
												id: 4,
												title: '1.1.1.1章(无课时)',
												level: 4,
												disabled: true,
												expanded: false,
												children: []
											}
										]
									}
								]
							}
						]
					}
				]
			}
		},
		computed: {
			flatCourseList() {
				const result = [];
				const flatten = (items, parentExpanded = true) => {
					items.forEach(item => {
						if (parentExpanded) {
							result.push(item);
							if (item.children && item.children.length > 0) {
								flatten(item.children, item.expanded);
							}
						}
					});
				};
				flatten(this.courseList);
				return result;
			}
		},
		onLoad() {
			//
		},
		onShow() {
			//
		},
		methods: {
			goBack() {
				uni.navigateBack();
			},
			startLearning() {
				uni.showToast({
					title: '开始学习',
					icon: 'none'
				});
			},
			switchTab(tab) {
				this.currentTab = tab;
				// 这里可以根据不同的tab加载不同的课程数据
				console.log('切换到标签', tab);
			},
			selectCourse(item) {
				if (item.disabled) {
					uni.showToast({
						title: '该课程暂不可用',
						icon: 'none'
					});
					return;
				}
				uni.showToast({
					title: `选择了课程 ${item.title}`,
					icon: 'none'
				});
			},
			goToWrongBank() {
				navTo('/pages/promote/wrongList');
			},
			toggleExpand(item) {
				item.expanded = !item.expanded;
			}
		}
	}
</script>

<style>
	page {
		background-color: #fff;
	}
	
	.content {
		min-height: 100vh;
	}
	
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
	
	.header-back {
		width: 60rpx;
		height: 50px;
	}
	
	.header-title {
		flex: 1;
		text-align: center;
	}
	
	.header-placeholder {
		width: 60rpx;
	}
	
	.body {
		padding: 20rpx;
		padding-bottom: 80rpx;
	}
	
	.start-learning-btn {
		margin: 150rpx auto;
	}
	
	.tabs-container {
		margin-bottom: 10rpx;
	}
	
	.tabs-scroll {
		overflow-x: auto;
		white-space: nowrap;
	}
	
	.tab-item {
		flex-shrink: 0;
		padding: 10rpx 20rpx;
		position: relative;
		border-bottom: 2rpx solid transparent;
	}
	
	.course-item {
		padding: 15rpx 20rpx;
		position: relative;
		min-height: 40rpx;
	}
	
	.course-text {
		flex: 1;
		white-space: nowrap;
		overflow: visible;
		text-overflow: unset;
	}
	
	.course-arrow {
		flex-shrink: 0;
		margin-left: 15rpx;
	}
	
	/* 层级缩进 */
	.course-item.level-1 {
		padding-left: 20rpx;
	}
	
	.course-item.level-2 {
		padding-left: 60rpx;
	}
	
	.course-item.level-3 {
		padding-left: 100rpx;
	}
	
	.course-item.level-4 {
		padding-left: 140rpx;
	}
	
	.bottom-button {
		position: fixed;
		bottom: 20rpx;
		left: 20rpx;
		right: 20rpx;
		height: 100rpx;
		border-radius: 50rpx;
		padding: 0 40rpx;
		z-index: 9999;
	}
	
	.bottom-button-icon {
		width: 40rpx;
		height: 40rpx;
		margin-right: 16rpx;
	}
</style>
