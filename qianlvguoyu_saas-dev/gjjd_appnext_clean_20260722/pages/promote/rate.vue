<template>
	<view class="content">
		<view class="header">
			<picker @change="bindcourseChange" :value="cindex" :range="courseList">
				<view class="header-picker">
					<view class="header-left">{{course}}</view>
					<view class="header-right text-R"><i class="gjjd-iconfont icon-xiangxia"></i></view>
				</view>
			</picker>
		</view>
		<view class="body" :style="'height:'+($winHeight-$statusBarHeight-44-44-10)+'px;'">
			<view class="body-icon">
				<view class="body-icon-circle">
					<image class="body-icon-bg" src="/static/images/cr.png"></image>
					<view class="body-icon-text font-s14">{{realname}}</view>
				</view>
			</view>
			<view class="row-list">
				<view class="row-item">
					<view class="text-C">能力</view>
					<view class="text-C">评价</view>
				</view>
				<view class="row-box scrollbar-none" :style="'height:'+($winHeight-$statusBarHeight-44-44-10-50-44-10)+'px;'">
					<view class="row-item border-top-73a9fa" v-for="item in list">
						<view class="item-left text-L">{{item.title}}</view>
						<view class="item-right text-R">{{item.result}}</view>
					</view>
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
				listAll:{},
				list:[],
				cindex:0,
				courseAll:[],
				courseList:[],
				course:'',
				course_id:0,
				realname:''
			}
		},
		onLoad() {
			this.getList(false);
		},
		methods: {
			bindcourseChange(e){
				this.cindex = Number(e.detail.value);
				this.course = this.courseAll[this.cindex].title;
				this.course_id = this.courseAll[this.cindex].id;
				if(Object.values(this.listAll).length>0){
					this.list = this.listAll[this.course_id];
				}
			},
			async getList(){
				uni.showLoading({
					title:'数据加载�?..'
				});
				let res = await request({
					method:'POST',
					data:{},
					url:"/apppromote/rate"
				});
				uni.hideLoading();
				console.log('promote rate getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				if(res?.data?.realname){
					this.realname = res.data.realname;
				}
				if(res?.data?.list){
					this.listAll = res.data.list;
				}
				if(res?.data?.course){
					this.courseAll = res.data.course;
					this.courseList = [];
					for(let i in this.courseAll){
						this.courseList.push(this.courseAll[i].title);
					}
					this.course = this.courseAll[this.cindex].title;
					this.course_id = this.courseAll[this.cindex].id;
					if(Object.values(this.listAll).length>0){
						this.list = this.listAll[this.course_id];
					}
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.header{
		padding: 0 20rpx;
		background-color: #fff;
	}
	.header-picker{
		height: 44px;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.header-left{
		width: 70%;
	}
	.header-right{
		width: 28%;
	}
	.body{
		background-color: #73a9fa;
		position: relative;
	}
	.row-list{
		position: absolute;
		top: 140px;
		z-index: 99;
	}
	.body-icon{
		display: flex;
		justify-content: center;
		align-items: center;
		height: 120px;
		padding-top: 40px;
	}
	.body-icon-circle{
		position: relative;
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		width: 150px;
		height: 150px;
		border-radius: 50%;
	}
	.body-icon-bg {
		position: absolute;
		left: 0%;
		top: 0rpx;
		z-index: 1;
		width: 100%;
		height: 100%;
	}
	.body-icon-text{
		position: relative;
		bottom: 10px;
		z-index: 2;
		text-align: center;
	}
	.border-top-73a9fa{
		border-top: 1rpx solid #73a9fa;
	}
	.row-item{
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 0 20rpx;
		background-color: #fff;
	}
	.item-left{
		width: 70%;
	}
	.item-right{
		width: 28%;
	}
	.row-box{
		overflow-y: auto;
	}
</style>
