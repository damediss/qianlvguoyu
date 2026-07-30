<template>
	<view class="content bg-ff">
	<view class="header bg-ff border-b-ee" :style="'padding-top:'+$statusBarHeight+'px;'">
		<view class="header-box dis-flex-hsb dis-flex-vc">
			<view class="header-back dis-flex-hv w-60" @click="goBack">
				<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
			</view>
			<view class="header-title text-C"></view>
			<view class="header-share dis-flex-hv w-60" @click="shareToWeChat">
				<image src="/static/images/share.png" class="share-icon"></image>
			</view>
		</view>
	</view>
		
		<view class="body" :style="'margin-top:'+(50+$statusBarHeight)+'px;'">
	<view class="article-title font-s20 font-wb color-33">{{competitionInfo.title}}</view>
	
	<view class="article-meta dis-flex-vc border-b-f0">
		<text class="article-tag font-s12 color-73a9fa" @click="goToInfoList">#{{competitionInfo.category}}#</text>
		<text class="article-date font-s12 color-99">{{competitionInfo.date}}</text>
			<text class="article-views font-s12 color-99">{{competitionInfo.views}}浏览</text>
			<view class="article-collect-wrapper dis-flex-vc">
				<view class="heart-icon-container dis-flex-hv" @click="toggleCollect">
					<text class="heart-icon" :class="isCollected ? 'heart-collected' : 'heart-uncollected'">{{isCollected ? '♥' : '♡'}}</text>
				</view>
				<text class="font-s12 color-99">收藏</text>
			</view>
		</view>
			
		<view class="article-content">
			<rich-text class="article-text" :nodes="competitionInfo.content"></rich-text>
		</view>
		</view>
	</view>
</template>

<script>
import {showMsg} from "@/utils";
import {getCompetitionInfoDetail} from "@/api/competition";
import { isSuccess, showError } from '@/utils/response';

export default {
	data() {
		return {
			competitionId: null,
			isCollected: false,
			competitionInfo: {
				id: 0,
				title: '加载中...',
				date: '',
				views: 0,
				category: '竞赛资讯',
				content: ''
			}
		}
	},
	onLoad(options) {
		if (options.id) {
			this.competitionId = parseInt(options.id);
			this.loadCompetitionDetail();
			this.loadCollectStatus();
		} else if (options.item) {
			try {
				const item = JSON.parse(decodeURIComponent(options.item));
				this.competitionId = item.id;
				this.loadCompetitionDetail();
				this.loadCollectStatus();
			} catch (e) {
				uni.showToast({ title: '参数解析失败', icon: 'none' });
			}
		} else {
			uni.showToast({ title: '参数错误', icon: 'none' });
			setTimeout(() => {
				uni.navigateBack();
			}, 1500);
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},
		
		goToInfoList() {
			uni.navigateTo({
				url: '/pages/competition/info/list'
			});
		},
		
		toggleCollect() {
			this.isCollected = !this.isCollected;
			this.saveCollectStatus();
			if (this.isCollected) {
				showMsg('this', {title: '收藏成功'});
			} else {
				showMsg('this', {title: '取消收藏'});
			}
		},
		
		loadCollectStatus() {
			const collectKey = `competition_collect_${this.competitionId}`;
			const savedStatus = uni.getStorageSync(collectKey);
			if (savedStatus !== null && savedStatus !== undefined && savedStatus !== '') {
				this.isCollected = savedStatus;
			}
		},
		
		saveCollectStatus() {
			const collectKey = `competition_collect_${this.competitionId}`;
			uni.setStorageSync(collectKey, this.isCollected);
		},
		
		shareToWeChat() {
			uni.share({
				provider: 'weixin',
				scene: 'WXSceneSession',
				type: 0,
				href: '',
				title: this.competitionInfo.title,
				summary: this.competitionInfo.content,
				imageUrl: '',
				success: (res) => {
					uni.showToast({
						title: '分享成功',
						icon: 'success'
					});
				},
				fail: (err) => {
					uni.showToast({
						title: '分享失败',
						icon: 'none'
					});
				}
			});
		},
		
	async loadCompetitionDetail() {
		uni.showLoading({
			title: '加载中...'
		});
		
		try {
			const res = await getCompetitionInfoDetail(this.competitionId);
			
			if (isSuccess(res) && res.data) {
				this.competitionInfo = {
					id: res.data.id,
					title: res.data.title,
					date: res.data.publishDate,
					views: res.data.views,
					category: '竞赛资讯',
					content: res.data.content || '暂无内容'
				};
			} else {
				showError(res, '数据加载失败');
			}
		} catch (error) {
			console.error('加载资讯详情失败：', error);
			uni.showToast({
				title: '加载失败，请重试',
				icon: 'none'
			});
		} finally {
			uni.hideLoading();
		}
	}
	}
}
</script>

<style scoped>
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

.share-icon {
	width: 40rpx;
	height: 40rpx;
}

.body {
	padding: 20rpx;
}

.article-title {
	line-height: 1.4;
	margin-bottom: 20rpx;
}

.article-meta {
	margin-bottom: 30rpx;
	padding-bottom: 20rpx;
}

.article-tag {
	margin-right: 20rpx;
	cursor: pointer;
}

.article-date {
	margin-right: 20rpx;
}

.article-views {
	margin-right: 20rpx;
}

.article-collect-wrapper {
	margin-left: 20rpx;
}

.heart-icon-container {
	position: relative;
	width: 24rpx;
	height: 24rpx;
	margin-right: 8rpx;
}

.heart-icon {
	font-size: 32rpx;
	position: absolute;
	top: 50%;
	left: 50%;
	transform: translate(-50%, -50%);
	line-height: 1;
}

.heart-collected {
	color: #007AFF;
}

.heart-uncollected {
	color: #ccc;
}

.article-content {
	margin-top: 20rpx;
}

.article-text {
	line-height: 1.6;
	font-size: 16px;
	color: #333333;
	word-break: break-all;
}

.article-text >>> p {
	margin: 10rpx 0;
	line-height: 1.6;
}

.article-text >>> img {
	max-width: 100%;
	height: auto;
	display: block;
	margin: 10rpx 0;
}
</style>