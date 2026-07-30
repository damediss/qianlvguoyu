<template>
	<view class="content">
		<template v-if="certs.length>0">
			<view class="row-item" v-for="(cert,i) in certs">
				<view class="cert-pic">
					<image :src="cert.url" mode="widthFix"></image>
					<view class="cert-down">下载证书</view>
				</view>
			</view>
		</template>
		<template v-else>
			<view class="text-C color-62">您当前还没有证书</view>
		</template>
	</view>
</template>

<script>
	import {request,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				certs:[]
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
			}
			this.getData();
		},
		methods: {
			async getData(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{},
					url:"/appuser/certs"
				});
				uni.hideLoading();
				console.log('certs getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.certs){
					this.certs = res.data.certs;
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
		background-color: #fff;
		padding: 10px 20rpx;
		margin-bottom: 10px;
	}
	.cert-pic{
		background-color: #ddd;
		min-height: 120px;
		border-radius: 12rpx;
		position: relative;
	}
	.cert-pic image{
		width: 710rpx;
		border-radius: 12rpx;
	}
	.cert-down{
		width: 120rpx;
		padding: 5px 0;
		background-color: #73a9fa;
		border-radius: 12rpx;
		font-size: 12px;
		color: #fff;
		text-align: center;
		position: absolute;
		left: 10rpx;
		bottom: 5px;
	}
</style>
