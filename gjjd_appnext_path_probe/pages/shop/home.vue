<template>
	<view class="content">
		<view class="list-box color-ff" :style="'padding-top:'+($statusBarHeight+5)+'px;'">
			<view class="row-box mar-B20">
				<view class="color-FFDC06 mar-B10">我的奖励与荣誉</view>
				<!-- 列表 -->
				<view class="mar-B10">技能积分：860</view>
				<view class="font-s12 mar-B10">通过课程学习、考核、打卡等方式累计获得860积分，可兑换实训设备使用时长、高端课程学习资格等奖励。</view>
				<view class="dis-flex-hsb mar-B10">
					<image class="img-news-1" src="/static/images/balance-bg.png"></image>
				</view>
				<view class="line-292931 mar-T20 mar-B20"></view>
				<view class="mar-B10">月度学习之星</view>
				<view class="font-s12 mar-B10">2026年1月获评月度学习之星，奖励积分200分+专业书籍一套，学习时长和考核成绩均位列平台前10%。</view>
				<view class="dis-flex-hsb mar-B10">
					<image class="img-news-2" src="/static/images/balance-bg.png"></image>
					<image class="img-news-2" src="/static/images/balance-bg.png"></image>
				</view>
				<view class="line-292931 mar-T20 mar-B20"></view>
				<view class="mar-B10">竞赛获奖证书</view>
				<view class="font-s12 mar-B10">2025年广西有色金属行业技能竞赛铝加工组三等奖，获奖金5000元+高级工认定加分资格。</view>
				<view class="dis-flex-hsb mar-B10">
					<image class="img-news-3" src="/static/images/balance-bg.png"></image>
					<image class="img-news-3" src="/static/images/balance-bg.png"></image>
					<image class="img-news-3" src="/static/images/balance-bg.png"></image>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				article:{}
			}
		},
		onShow() {
			this.getPageData();
		},
		methods: {
			async getPageData(){
				let that = this;
				let res = await request({
					method:'POST',
					data:{},
					url:"/appreward/index"
				});
				console.log(res);
				if(res?.data?.article){
					this.article = res.data.article;
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #292931;
	}
	.list-box{
		width: 710rpx;
		padding-top: 10px;
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.row-box{
		width: 670rpx;
		padding: 10px 20rpx;
		background-color: #343440;
		border-radius: 12rpx;
	}
	.line-292931{
		height: 1px;
		background-color: #292931;
	}
	.img-news-1{
		width: 670rpx;
		height: 120px;
		border-radius: 8rpx;
	}
	.img-news-2{
		width: 330rpx;
		height: 90px;
		border-radius: 8rpx;
	}
	.img-news-3{
		width: 200rpx;
		height: 90px;
		border-radius: 8rpx;
	}
</style>