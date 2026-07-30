<template>
	<view class="content">
		<template v-if="messages.length>0">
			<view class="row-item mar-B10" v-for="(msg,i) in messages">
				<view>
					<view class="font-wb font-s14">{{msg.title}}</view>
					<view class="color-62">
						<uv-parse :content="msg.content"></uv-parse>
					</view>
					<view class="color-92 font-s14 mar-T5">{{msg.create_time}}</view>
				</view>
				<view>
					<!-- <i class="gjjd-iconfont icon-xiangyou font-wb"></i> -->
				</view>
			</view>
		</template>
		<template v-else>
			<view class="text-C color-62">您当前还没有消息</view>
		</template>
	</view>
</template>

<script>
	import {request,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				messages:[]
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
			}
			this.getData();
		},
		onShow() {
			//this.getData();
		},
		methods: {
	async getData(){
		uni.showLoading({
			title:'数据加载中...'
		});
				let res = await request({
					method:'POST',
					data:{},
					url:"/appmessage/index"
				});
				uni.hideLoading();
				console.log('messages getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.messages){
					this.messages = res.data.messages;
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-item{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 20rpx;
		background-color: #fff;
		border-radius: 12rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
</style>
