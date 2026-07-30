<template>
	<view class="content">
		<view class="body">
			<uv-parse :content="content"></uv-parse>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				id:0,
				content:''
			}
		},
		onLoad(options) {
			console.log("team pingfen",options)
			if(options.id){
				this.id = options.id;
				this.getData();
			}
		},
		methods: {
			async getData(){
				let res = await request({
					method:'POST',
					data:{id:this.id},
					url:"/appteam/pingfen"
				});
				console.log('team pingfen content',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.pingfen){
					this.content = res.data.pingfen;
				}
			}
		}
	}
</script>

<style>
	.body{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-top: 10px;
	}
</style>