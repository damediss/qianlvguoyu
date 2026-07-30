<template>
	<view class="content">
		<view class="row-box">
			<template v-if="simList.length>0">
				<view class="task-fuxi-item radius-12 mar-B10" :class="item.status>0?'border-4caf50':'border-62'" v-for="item in simList" :key="item.id" @click="toDetail(item)">
					<view class="font-wb">{{item.title}}</view>
					<view class="font-s12 color-92 mar-T5">
						得分:
						<text class="mar-L10">{{item.score}}</text>
					</view>
					<view class="font-s12 color-92 mar-T5">
						考试时间:
						<text class="mar-L10">{{item.exam_datetime}}</text>
					</view>
					<view class="font-s12 color-92 mar-T5">
						状态:
						<text v-if="item.status>0" class="mar-L10 color-4caf50">完成</text>
						<text v-else class="mar-L10">未完成</text>
						<text v-if="item.status>0" class="mar-L10 color-73a9fa" @click.stop="toReSim(item)">再次模拟</text>
					</view>
				</view>
			</template>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				paper_id:0,
				exam_id:0,
				num:0,
				simList:[],
				isFirst:1
			}
		},
		onLoad(options){
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.paper_id){
					this.paper_id = item.paper_id;
				}
				if(item.exam_id){
					this.exam_id = item.exam_id;
				}
				if(item.num){
					this.num = item.num;
				}
				this.getList();
			}
		},
		onShow(){
			if(!this.isFirst){
				this.getList();
			}
		},
		methods: {
			async getList(){
				uni.showLoading({
					title:'数据加载中...'
				});
				let res = await request({
					method:'POST',
					data:{exam_id:this.exam_id},
					url:"/appreview/simlist"
				});
				uni.hideLoading();
				console.log('simList getData',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('back',{title:msg});
				}
				if(res?.data?.list){
					this.simList = res.data.list;
				}
			},
			toDetail(item){
				this.isFirst = 0;
				if(item.status>0){
					let param = {
						paper_id:item.paper_id,
						exam_id:item.exam_id
					};
					navTo("/pages/exam/simDetail",param);
				}else{
					let param = {
						paper_id:item.paper_id,
						exam_id:item.exam_id
					};
					navTo("/pages/exam/sim",param);
				}
			},
			toReSim(item){
				this.isFirst = 0;
				let param = {
					paper_id:item.paper_id,
					exam_id:item.exam_id
				};
				navTo("/pages/exam/sim",param);
			}
		}
	}
</script>

<style>
	page{
		background-color: #f1f1f1;
	}
	.row-box{
		width: 710rpx;
		padding: 20rpx;
	}
	.task-fuxi-item{
		width: 670rpx;
		margin-top: 20rpx;
		padding: 30rpx 20rpx;
	}
</style>