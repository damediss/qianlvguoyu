<template>
	<view class="content">
		<view class="row-item">
			<view class="avatar">
				<image src="/static/images/logo.jpg"></image>
			</view>
			<view class="mar-L10">
				<view class="font-s14">姓名</view>
				<view class="mar-T5 item-content font-s14">内容内容内容内容</view>
			</view>
		</view>
		<uni-popup ref="ask" type="bottom" :is-mask-click="false">
			<view class="popup">
				<view class="popup-header text-C">
					<text class="font-wb">我要提问</text>
					<text class="popup-close" @click="askClose"><i class="gjjd-iconfont icon-shanchuguanbiquxiaowubiankuang"></i></text>
				</view>
				<view class="popup-body">
					<view>
						<template v-for="(item,i) in tags">
						<text class="ask-tag mar-R10" :class="currentTag==i?'bg-73a9fa color-ff':'color-73a9fa'" @click="onTags(i)">{{item.title}}</text>
						</template>
					</view>
			<view class="mar-T10">
				<textarea class="textarea" placeholder="请输入内容"></textarea>
			</view>
				</view>
				<view class="popup-footer">
					<view class="bg-73a9fa color-ff">提交</view>
				</view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				tags:[{title:'成本核算'},{title:'生成服务'},{title:'质检化验'},{title:'人员管理'}],
				currentTag:-1
			}
		},
		onNavigationBarButtonTap(e){
			console.log(e);
			if(e.code && e.code == 'ask'){
				console.log("ask");
				this.$refs.ask.open();
			}
		},
		methods: {
			onTags(i){
				this.currentTag = i;
			},
			askClose(){
				this.currentTag = -1;
				this.$refs.ask.close();
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-item{
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
		display: flex;
		align-items: center;
	}
	.ask-tag{
		border:1rpx solid #73a9fa;
		padding: 3px 10rpx;
		border-radius: 4rpx;
		font-size:12px;
	}
	.avatar{
		width: 60px;
		height: 60px;
	}
	.avatar image{
		width: 60px;
		height: 60px;
		border-radius: 30px;
	}
	.item-content{
		padding: 2px 6rpx;
		border-radius: 8rpx;
		background-color: #73a9fa;
		color: #fff;
	}
	.textarea{
		width: 670rpx;
		min-height: 60px;
		border: 1rpx solid #929292;
		border-radius: 12rpx;
		padding: 10px;
	}
	.popup{
		width: 750rpx;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		background-color: #fff;
	}
	.popup-header{
		padding-top: 10px;
		position: relative;
	}
	.popup-close{
		position: absolute;
		top:5px;
		right: 20rpx;
	}
	.popup-body{
		padding: 10px 20rpx;
	}
	.popup-footer{
		padding-top: 10px;
		padding-bottom: 10px;
	}
	.popup-footer view{
		margin: 0 20rpx;
		height: 40px;
		line-height: 40px;
		text-align: center;
		border-radius: 20px;
	}
</style>
