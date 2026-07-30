<template>
	<view class="content">
	<view class="top">
		<view class="font-s14">
			<text>您已连续签到</text>
			<text class="mar-L10 mar-R10 color-red">{{userDetail.signDay}}</text>
			<text>天</text>
		</view>
		<view class="font-s16">
			<text>您有</text>
			<text class="mar-L10 mar-R10 color-red">{{userDetail.point}}</text>
			<text>积分</text>
		</view>
	</view>
		<view class="calendar">
			<uni-calendar v-if="showCalendar" :selected="selected" :showMonth="false" @change="changeCalendar" @monthSwitch="monthSwitch"></uni-calendar>
		</view>
		<view class="btn-sign" @click="onSign">签到</view>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				userDetail:{signDay:0,point:0},
				showCalendar:true,
				selected: [],
				date:''
			}
		},
		onLoad(options) {
			this.date = this.$util.dateFormat();
			this.getData();
		},
		methods: {
			async getData(){
				let res = await request({
					method:'POST',
					data:{},
					url:"/appuser/signinfo"
				});
				console.log("getData",res);//
				if(res?.data?.userDetail){
					this.userDetail = res.data.userDetail;
				}
				if(res?.data?.signlog){
					this.selected = res.data.signlog;
				}
			},
			changeCalendar(e) {
				console.log('change 返回:', e)
				this.date = e.fulldate;
			},
			monthSwitch(e) {
				console.log('monthSwitchs 返回:', e);
			},
			async onSign(){
				let that = this;
				let res = await request({
					method:'POST',
					data:{date:this.date,matchId:0},
					url:"/appuser/sign"
				});
				console.log("getData",res);
				showMsg('this',{title:res.msg});
				if(res?.code){
					return;
				}
				if(res?.data?.point) this.userDetail.point = res.data.point;
				if(res?.data?.sign_day) this.userDetail.signDay = res.data.sign_day;
				if(this.selected.length>0){
					for(let i in this.selected){
						if(this.selected[i].date == this.date){
							return;
						}
					}
				}
				this.selected.push({
					date: this.date,
					info: '已签'
				});
				console.log(this.selected);
				this.showCalendar = false;
				this.$nextTick(() => {
				  this.showCalendar = true;
				});
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.top{
		height: 44px;
		padding: 0 20rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		background-color: #fff;
	}
	.calendar{
		margin: 10px 20rpx;
		padding: 10px 0;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.btn-sign{
		margin: 10px 20rpx;
		background-color: #73a9fa;
		border-radius: 22px;
		height: 44px;
		line-height: 44px;
		text-align: center;
		color: #fff;
	}
</style>
