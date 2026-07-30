<template>
	<view class="content">
		<view class="content">
			<view style="height: 40px;"></view>
			<view class="exam-box">
				<view class="exam-title">{{currentIndex+1}}/{{examList.length}}</view>
				<view class="exam-item scrollbar-none" v-if="examList.length>0">
				<view class="color-73a9fa">
					<text v-if="examList[currentIndex].type=='multi'">多选题</text>
					<text v-if="examList[currentIndex].type=='single'">单选题</text>
					<text v-if="examList[currentIndex].type=='judge'">判断题</text>
				</view>
					<view class="pad-T10 pad-B10 font-s16">
						<uv-parse :content="examList[currentIndex].content"></uv-parse>
					</view>
					<view class="exam-answer">
					<template v-if="examList[currentIndex].type=='multi'">
						<template v-for="(item,index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10" :class="{'bg-73a9fa color-ff':examList[currentIndex]['user_answer'].includes(item.content)}">
								<view class="mar-L20">{{item.tag}}、</view>
								<view class="mar-L10"><uv-parse :content="item.content+''"></uv-parse></view>
							</view>
						</template>
					</template>
					<template v-else>
						<template v-for="(item,index) in examList[currentIndex].answer">
							<view class="exam-answer-item border-92 mar-B10" :class="{'bg-73a9fa color-ff':examList[currentIndex]['user_answer']==item.content}">
								<view class="mar-L20">{{item.tag}}、</view>
								<view class="mar-L10"><uv-parse :content="item.content+''"></uv-parse></view>
							</view>
						</template>
					</template>
					</view>
					<view class="color-red">正确答案:{{examList[currentIndex].isright}}</view>
					<view class="mar-T10 mar-B10">我的答案:{{examList[currentIndex].mytag}}</view>
				</view>
			<view class="exam-btns">
				<view class="mar-L20">
					<text v-if="currentIndex==0" class="btn-none color-92">上一题</text>
					<text v-else class="bg-73a9fa color-ff" @click="doBack">上一题</text>
				</view>
				<view class="mar-R20">
					<text v-if="currentIndex==examList.length-1" class="btn-none color-92">下一题</text>
					<text v-else class="bg-73a9fa color-ff" @click="doNext">下一题</text>
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
				course_id:0,
				exam_id:0,
				paper_id:0,
				examList:[],
				currentIndex:0
			}
		},
		onLoad(options) {
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log(item);
				if(item.course_id){
					this.course_id = item.course_id;
				}
				if(item.exam_id){
					this.exam_id = item.exam_id;
				}
				if(item.paper_id){
					this.paper_id = item.paper_id;
				}
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
					data:{exam_id:this.exam_id,paper_id:this.paper_id},
					url:"/apppromote/wrongdetail"
				});
				uni.hideLoading();
				console.log('promote wronglist',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.list){
					this.examList = res.data.list;
				}
			},
			doBack(){
				if(this.currentIndex>0){
					this.currentIndex--;
				}
			},
			doNext(){
				if(this.currentIndex<this.examList.length-1){
					this.currentIndex++;
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #73a9fa;
	}
	.content{
	}
	.exam-box{
		margin-left: 40rpx;
		margin-right: 40rpx;
		padding-bottom: 15px;
		background-color: #fff;
		border-radius: 12rpx;
		position: relative;
	}
	.exam-time{
		position: absolute;
		top: -30px;
		left: 285rpx;
		right: 285rpx;
		z-index: 9999;
		width: 80px;
		height: 80px;
		line-height: 80px;
		background-color: #fff;
		border-radius: 50px;
		text-align: center;
	}
	.exam-title{
		height: 50px;
		line-height: 50px;
		padding: 0 20rpx;
	}
	.exam-item{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-top: 5px;
		max-height: 500px;
		overflow-y: auto;
		font-size: 14px;
	}
	.exam-answer-item{
		display: flex;
		align-items: center;
		padding: 4px 0;
		height: 40px;
		border-radius: 22px;
	}
	.exam-btns{
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.exam-btns text{
		padding: 5px 10px;
		border-radius: 8rpx;
	}
	.btn-none{
		background-color: #ccc;
	}
	.exam-submit{
		margin-top: 15px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		height: 40px;
		line-height: 40px;
		line-height: 40px;
		border-radius: 20px;
		text-align: center;
		background-color: #73a9fa;
		color: #fff;
	}
</style>
