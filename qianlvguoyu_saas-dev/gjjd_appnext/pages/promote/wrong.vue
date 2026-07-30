<template>
	<view class="content">
		<view class="header">
			<view class="circle">
				<image class="bgimage" src="/static/images/cr.png"></image>
				<view class="num-text">
					<view class="mar-B10 color-00 font-wb">{{total}}</view>
					<view class="color-92">我的错题</view>
				</view>
			</view>
		</view>
		<view class="body scrollbar-none" :style="'height:'+($winHeight-$statusBarHeight-44-225-30)+'px;'">
			<view class="row-item">
				<view class="item-left color-92">习题名称</view>
				<view class="item-right text-R color-92">错题数量</view>
			</view>
			<view class="row-list" :style="'height:'+($winHeight-$statusBarHeight-44-225-30-30)+'px;'">
			<template v-for="item in listByCourse">
				<view class="row-item mar-B10" @click="toList(item)">
					<view class="item-left">{{item.title}}</view>
					<view class="item-right text-R">
						<text class="color-73a9fa">{{item.num}}</text>
						<i class="gjjd-iconfont icon-xiangyou color-92"></i>
					</view>
				</view>
			</template>
			</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				exam_id:0,
				total:0,
				listByCourse:[]
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.exam_id){
					this.exam_id = item.exam_id;
				}
				this.getData();
			}
		},
		methods: {
			async getData(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{exam_id:this.exam_id},
					url:"/apppromote/wronginfo"
				});
				uni.hideLoading();
				console.log('promote wrong getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.total){
					this.total = res.data.total;
				}
				if(res?.data?.list){
					this.listByCourse = res.data.list;
				}
			},
			toList(item){
				navTo("/pages/promote/wrongList",{exam_id:this.exam_id,paper_id:item.paper_id});
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.header{
		height: 225px;
		background-image: linear-gradient(#01a9ef, #9de2ff);
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.circle{
		position: relative;
		display: flex;
		flex-direction: column;
		justify-content: center;
		align-items: center;
		width: 160px;
		height: 160px;
		border-radius: 50%;
	}
	.bgimage {
	  position: absolute;
	  left: 0%;
	  top: 0rpx;
	  z-index: 1;
	  width: 100%;
	  height: 100%;
	}
	.num-text{
		position: relative;
		z-index: 9;
		text-align: center;
	}
	.body{
		margin-top: -30px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10px;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.row-list{
		overflow-y: auto;
	}
	.row-item{
		display: flex;
		justify-content: space-between;
		padding: 5px 0;
	}
	.item-left{
		width: 60%;
	}
	.item-right{
		width: 35%;
	}
</style>
