<template>
	<view class="content">
		<view class="tabs">
			<view class="tabs-item" v-for="(item,i) in tabs">
				<view @click="onTabs(i)">
					<view class="text-C mar-B5">{{item.title}}</view>
					<view class="dis-flex-hv">
						<view class="tabs-active" v-if="tabIndex==i"></view>
					</view>
				</view>
			</view>
		</view>
		<view class="list">
			<view class="list-item" v-for="(item,i) in list" @click="toDetail(item.id)">
				<view class="item-info">
					<view class="info-top">
						<view>{{item.title}}</view>
						<view class="color-92 font-s12">{{item.title_code}}</view>
					</view>
					<view class="info-bottom color-92 font-s12">{{item.member}}成员在学习</view>
				</view>
				<view class="item-pic">
					<image :src="item.ablum" mode="heightFix"></image>
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
				tabIndex:0,
				tabs:[
					{title:'我的团队',type:'myteam',icon:''},
					{title:'技能大师工作室',type:'master',icon:''}
				],
				list:[]
			}
		},
		onLoad() {
			this.getList();
		},
		onShow() {
			this.$util.checkLogin();
		},
		methods: {
			onTabs(i){
				if(this.loading) return;
				console.log('i='+i);
				if(i != this.tabIndex){
					this.tabIndex = i;
					this.list = [];
					this.getList();
				}
			},
			toDetail(id){
				navTo("/pages/team/detail",{id});
			},
			async getList(){
				let res = await request({
					method:'POST',
					data:{type:this.tabs[this.tabIndex].type},
					url:"/appteam/index"
				});
				console.log('team list',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.list){
					this.list = res.data.list;
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.tabs{
		height: 44px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		background-color: #fff;
	}
	.tabs-item{
		width: 50%;
		padding: 5px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.tabs-active{
		width: 60%;
		height: 4px;
		background-color: #73a9fa;
		border-radius: 2px;
	}
	.list{
		width: 710rpx;
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
	}
	.list-item{
		width: 670rpx;
		padding: 20rpx;
		background-color: #fff;
		border-radius: 10rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.item-info{
		margin-right: 10rpx;
		flex: 1;
	}
	.info-top{
		height: 70px;
	}
	.info-bottom{
		height: 30px;
		line-height: 30px;
	}
	.item-pic{
		width: 300rpx;
		height: 100px;
		border-radius: 10rpx;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: end;
	}
	.item-pic image{
		height: 100px;
	}
	.footer{
		position: fixed;
		bottom: 10px;
		left: 0;
		right: 0;
	}
	.popup{
		width: 750rpx;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		background-color: #fff;
	}
	.popup-header{
		padding-top: 10px;
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
