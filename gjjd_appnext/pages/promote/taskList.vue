<template>
	<view class="content">
		<view class="search-box">
			<view @click="onSearch">
				<i class="gjjd-iconfont icon-sousuobeifen2 font-s40"></i>
			</view>
			<input class="search-input" v-model="keyword" placeholder="请输入关键字"/>
		</view>
		<view class="task-list">
			<template v-if="['review','exam'].includes(type)">
				<view class="task-item border-8e dis-flex-hv mar-B10" v-for="item in list" @click="resultLink(item)">
					<view><i class="gjjd-iconfont icon-renwu font-s30"></i></view>
					<view class="task-title">{{item.title}}</view>
				</view>
			</template>
			<template v-else>
				<view class="task-item dis-flex-hv mar-B10" v-for="item in list" @click="goToTrainingDetail(item)">
					<view class="task-cover">
						<image></image>
					</view>
					<view class="task-title">
						<view>{{item.classInfo.title}}</view>
						<view class="color-92">{{item.classInfo.desc}}</view>
					</view>
				</view>
			</template>
		</view>
	</view>
</template>

<script>
	import {request, navTo, showMsg} from "@/utils";
	export default {
		data() {
			return {
				titles:{train:'培训任务',review:'复习任务',exam:'考试任务',match:'竞赛任务'},
				type:'',
				list:[],
				keyword:'',
				page:1,
				loading:0,
				finish:0
			}
		},
		onLoad(options) {
			//console.log("task list",options);
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.type){
					this.type = item.type;
					if(this.titles[this.type]){
						uni.setNavigationBarTitle({
							title:this.titles[this.type]
						});
					}
					this.getData(true);
				}
			}
		},
		onReachBottom() {
			console.log("触底了");
			if(!this.finish && !this.loading){
				this.getData(false);
			}
		},
		methods: {
			async getData(flag = false){
				if(flag){
					this.list = [];
					this.page = 1;
					this.finish = 0;
				}
				if(this.finish) return;
				if(this.loading) return;
				this.loading = 1;
				uni.showLoading({
					title:'数据加载中...'
				});
				const res = await request({
					method:'POST',
					data:{type:this.type,page:this.page,keyword:this.keyword},
					url:"/apppromote/tasklist"
				});
				uni.hideLoading();
				this.loading = 0;
				console.log("promote task list",res);
				if(res?.data?.tasklist){
					if(res.data.tasklist.length>0){
						this.list = [...this.list,...res.data.tasklist];
						this.page++;
					}else{
						this.finish = 1;
					}
				}
			},
			resultLink(item){
				if(['review','exam'].includes(this.type)){
					if(item.type == 1) return;//线下
					let resultType = 0;
					if(this.type == 'review') resultType = 1;
					navTo('/pages/promote/exam',{paper_id:item.paper_id,exam_id:item.id,has_review:item.hasReview,resultType});
				}
			},
			goToTrainingDetail(item){
				console.log("training detail",item);
				navTo('/pages/promote/train', {id: item.id});
			},
			onSearch(){
				this.getData(true);
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.border-8e{
		border:1rpx solid #8e8e8e;
	}
	.search-box{
		margin-left: 50rpx;
		margin-right: 50rpx;
		height: 40px;
		background-color: #cfe3f7;
		border-radius: 20px;
		display: flex;
		align-items: center;
	}
	.search-input{
		flex: 1;
		height: 30px;
		line-height: 30px;
		margin-left: 10rpx;
		margin-right: 20rpx;
		text-align: left;
	}
	.task-list{
		padding-top: 10px;
		padding-bottom: 10px;
		padding-left: 30rpx;
		padding-right: 30rpx;
	}
	.task-item{
		padding-left: 10rpx;
		padding-right: 10rpx;
		height: 70px;
		border-radius: 8rpx;
	}
	.task-cover{
		width: 120rpx;
		height: 120rpx;
	}
	.task-cover image{
		width: 120rpx;
		height: 120rpx;
		background-color: #8e8e8e;
		border-radius: 12rpx;
	}
	.task-title{
		margin-left: 20rpx;
		flex: 1;
	}
</style>