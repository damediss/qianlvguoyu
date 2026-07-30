<template>
	<view class="content">
		<view style="position:fixed;top:0;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">拨打电话授权提示</view>
			<view style="margin-top: 10px;">为了实现电话客服、联系顾客、联系商家等功能，需要访问您的拨打电话权限，您如果拒绝开启，将无法使用上述功能。</view>
		</view>
		<view class="row-box">
			<view class="row-item" @click="toWebView(link.agreement)">
				<view class="dis-flex-vc">
					<i class="gjjd-iconfont icon-shezhi"></i>
					<text class="mar-L10 font-s12">用户协议</text>
				</view>
				<view class="text-R">
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>
			<view class="row-item" @click="toWebView(link.privacy)">
				<view class="dis-flex-vc">
					<i class="gjjd-iconfont icon-shezhi"></i>
					<text class="mar-L10 font-s12">隐私政策</text>
				</view>
				<view class="text-R">
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>
			<view class="row-item" @click="accountDis">
				<view class="dis-flex-vc">
					<i class="gjjd-iconfont icon-shezhi"></i>
					<text class="mar-L10 font-s12">账号注销</text>
				</view>
				<view class="text-R">
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>
			<view class="row-item" @click="jubao">
				<view class="dis-flex-vc">
					<i class="gjjd-iconfont icon-shezhi"></i>
					<text class="mar-L10 font-s12">举报</text>
				</view>
				<view class="text-R">
					<i class="gjjd-iconfont icon-xiangyou"></i>
				</view>
			</view>
		<view class="row-item" @click="checkVersion">
			<view class="dis-flex-vc">
				<i class="gjjd-iconfont icon-shezhi"></i>
				<text class="mar-L10 font-s12">检查版本</text>
			</view>
			<view class="text-R">
				<i class="gjjd-iconfont icon-xiangyou"></i>
			</view>
		</view>
	</view>
	<view class="row-version">
		<view>
			<text>版本号:</text>
			<text>v{{version}}</text>
			<text>.</text>
			<text>{{versionCode}}</text>
		</view>
	</view>
	<view class="mar-T10 font-s14 text-C">版权所有 广西国昱数字科技有限公司</view>
	</view>
	<!-- 更新下载进度条 -->
	<view v-if="upgrade_progress" class="upgrade-progress">
		<view style="background-color: #fff;padding: 20rpx;border-radius: 6rpx;">{{upgrade_progress}}</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg,checkUpgrade} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip:false,
				link:{
					agreement:'',
					privacy:''
				},
				config:{
					phone:'18977081413'
				},
				version:'',
				versionCode:'',
				upgrade_progress:''
			}
		},
		onLoad() {
			// #ifdef APP-PLUS
			plus.runtime.getProperty(plus.runtime.appid, async (widgetInfo) => {
				this.version = `${widgetInfo.version}`;
				this.versionCode = `${widgetInfo.versionCode}`
			})
			// #endif
			this.getData();
		},
		onBackPress() {
			if(this.upgrade_progress){
				return true;
			}
		},
		methods: {
			async getData(){
				let res = await request({
					method:'POST',
					data:{},
					url:"/appuser/index"
				});
				console.log("getData",res);
				if(res?.data?.link) this.link = res.data.link;
			},
			toWebView(src){
				navTo('/pages/public/webview',{src});
			},
			accountDis(){
				uni.showModal({
				title: '提示',
				content: '确认注销账号吗？',
				success:async (rs)=>{
						if(rs.confirm){
							let res = await request({
								method:'POST',
								data:{},
								url:"/appuser/accountdis"
							});
							console.log('accountDis',res);
							showMsg('this',{title:res.msg});
							if(res.code){
								return;
							}
							uni.removeStorageSync('itoken');
							let tid = setTimeout(()=>{
								rectTo('/pages/index/index');
							},1500);
						}
					}
				});
			},
			jubao(){
				uni.showActionSheet({
					itemList: ['举报电话', '留言'],
					success: (res) => {
						if(res.tapIndex>0){
							uni.navigateTo({
								url:'/pages/user/jubao'
							});
						}else{
							makeCall(this.config.phone,this);
						}
					}
				});
			},
			checkVersion(){
				checkUpgrade({userId:0},this,(e)=>{
					console.log('***',e);
				});
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-box{
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10px 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.row-item{
		display: flex;
		align-items: center;
		justify-content: space-between;
		height: 44px;
		border-bottom: 1rpx solid #ddd;
	}
	.row-version{
		height: 44px;
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.row-version view{
		width: 70%;
		height: 44px;
		line-height: 44px;
		background-color: #dedede;
		border-radius: 22px;
		color: #626262;
		text-align: center;
		font-size: 14px;
	}
	.upgrade-progress{
		position: fixed;
		top: 0;
		bottom: 0;
		left: 0;
		right: 0;
		z-index: 9999;
		background-color: rgba(0, 0, 0, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
	}
</style>
