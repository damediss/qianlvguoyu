<template>
	<view class="content">
		<view v-if="list.length>0" class="exam-box mar-T10">
			<view>
				<template v-for="(item,i) in list">
					<view v-if="item.id == currentExamId">
						<view class="exam-box-content">
					<view>
						<span v-if="item.type == 'single'">单选题</span>
						<span v-if="item.type == 'multiple'">多选题</span>
						<span v-if="item.type == 'judge'">判断题</span>
					</view>
							<view>
								<uv-parse :content="item.content"></uv-parse>
							</view>
							<view>
								<template v-for="(ans,ai) in item.answer">
									<view v-if="ans.content" class="exam-answer pos-r" :class="{'answer-active':Number(ans.selected)>0}" @click="examAnswer(i,ai)">
										<span>{{ans.tag}}、{{ans.content}}</span>
									</view>
								</template>
							</view>
							<view v-if="item.isanswer>0" class="mar-B20 color-red">
								正确答案:{{item.right}}
							</view>
						</view>
					</view>
				</template>
			</view>
		<view class="exam-btns">
			<template v-if="list[currentExam].type == 'multiple'">
				<template v-if="currentExam>0">
					<view class="exam-btn-item bg-ff" v-if="list[currentExam].isanswer>0" @click="backExam()">上一题</view>
					<view class="exam-btn-item bg-dd" v-else>上一题</view>
				</template>
				<view class="exam-btn-item bg-dd" v-else>上一题</view>
				<template v-if="list[currentExam].isanswer>0">
					<view class="exam-btn-item bg-ff">{{currentExam+1}} / {{list.length}}</view>
				</template>
				<template v-else>
					<view class="exam-btn-item bg-ff" @click="onMultiple()">确定</view>
				</template>
				<template v-if="currentExam<list.length-1">
					<view class="exam-btn-item bg-ff" v-if="list[currentExam].isanswer>0" @click="nextExam()">下一题</view>
					<view class="exam-btn-item bg-dd" v-else>下一题</view>
				</template>
				<view class="exam-btn-item bg-dd" v-else>下一题</view>
			</template>
			<template v-else>
				<view class="exam-btn-item bg-ff" v-if="currentExam>0" @click="backExam()">上一题</view>
				<view class="exam-btn-item bg-dd" v-else>上一题</view>
				<view class="exam-btn-item bg-ff">{{currentExam+1}} / {{list.length}}</view>
				<view class="exam-btn-item bg-ff" v-if="currentExam<list.length-1" @click="nextExam()">下一题</view>
				<view class="exam-btn-item bg-dd" v-else>下一题</view>
			</template>
		</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg,cameraPermission} from "@/utils";
	export default {
		data() {
			return {
				id:0,
				filename:'',
				list:[],
				currentExam:0,
				currentExamId:0
			}
		},
		onLoad(options) {
			console.log("options",options);
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				if(item.id) this.id = item.id;
				if(item.filename) this.filename = item.filename;
			}
			this.getExams();
		},
		methods: {
			onMultiple(){
				if(this.list[this.currentExam].isanswer>0) return;
				this.list[this.currentExam].isanswer = 1;
			},
			examAnswer(i,ai){
				if(this.list[i]['isanswer']>0) return;
				if(this.list[i]['type'] == 'multiple'){
					this.list[i]['answer'][ai]['selected'] = !this.list[i]['answer'][ai]['selected'];
				}else{
					//排他
					for(let j=0;j<this.list[i]['answer'].length;j++){
						this.list[i]['answer'][j]['selected'] = -1;
					}
					this.list[i]['answer'][ai]['selected'] = 1;
					this.list[i]['isanswer'] = 1;
				}
			},
			backExam(){
				if(this.currentExam>0){
					this.currentExam--;
					this.currentExamId = this.list[this.currentExam].id;
				}else{
					uni.showToast({
						title:'已经是第一题了',
						icon:'none'
					});
				}
			},
			nextExam(){
				if(this.currentExam < this.list.length - 1){
					this.currentExam++;
					this.currentExamId = this.list[this.currentExam].id;
				}else{
					uni.showToast({
						title:'已经是最后一题了',
						icon:'none'
					});
				}
			},
			async getExams(){
				if(!this.$util.isLogin()){
					showMsg('this',{title:'请先登录'});
					return;
				}
				let res = await request({
					method:'POST',
					data:{id:this.id,filename:this.filename},
					url:"/appkejianexam/index"
				});
				//console.log('##res##',res);
				if(res?.data?.list){
					this.list = res.data.list;
					this.currentExamId = this.list[0].id;
				}
			}
		}
	}
</script>

<style>
	.text-C{
		text-align: center;
	}
	.bg-ff{
		background-color: #fff;
	}
	.bg-dd{
		background-color: #ddd;
	}
	.bg-f9d52b{
		background-color: #f9d52b;
	}
	.bg-73a9fa{
		background-color: #73a9fa;
	}
	.bg-0a3c88{
		background-color: #0a3c88;
	}
	.color-ff{
		color: #fff;
	}
	.color-62{
		color: #626262;
	}
	.color-red{
		color: red;
	}
	.mar-L20{
		margin-left: 20rpx;
	}
	.mar-R10{
		margin-right: 10rpx;
	}
	.mar-T10{
		margin-top: 10px;
	}
	.mar-B10{
		margin-bottom: 10px;
	}
	.mar-B20{
		margin-bottom: 20px;
	}
	.pos-r{
		position: relative;
	}
	.is-correct{
		position: absolute;
		right: 4px;
		color: red;
	}
	.content{
		padding-bottom: 10px;
		position: relative;
	}
	.exam-result{
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin: 0 20rpx;
		height: 30px;
	}
	.exam-result-c{
		color: #73a9fa;
	}
	.exam-wrong{
		margin-top: 10px;
	}
	.exam-times{
		height: 30px;
		line-height: 30px;
		padding: 0 20rpx;
	}
	.exam-box{
		padding: 10px 0;
	}
	.exam-box-content{
		overflow-y: auto;
		padding: 0 20rpx;
		width: 670rpx;
	}
	.exam-answer{
		margin: 20px 0;
		padding: 10px 20rpx;
		background-color: #fff;
		border-radius: 8rpx;
		color: #000;
	}
	.answer-active{
		background-color: #0a3c88;
		color:#fff
	}
	.exam-btns {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin: 0 20rpx;
	}
	.exam-btn-item{
		/* background-color: #fff; */
		border-radius: 12rpx;
		height: 30px;
		line-height: 30px;
		padding: 0 20rpx;
		color: #626262;
	}
	.exam-box{
		width: 710rpx;
		margin: 0 20rpx;
		background-color: #33aafc;
		border-radius: 12rpx;
	}
	.exam-submit{
		margin-top: 10px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		border-radius: 20px;
		height: 40px;
		line-height: 40px;
		padding: 0 20rpx;
		text-align: center;
	}
	.exam-dir{
		position: absolute;
		top: 0;
		bottom: 0;
		left: 0;
		right: 0;
		z-index: 999;
		background-color: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: end;
	}
	.exam-dir-box{
		padding: 10px;
		height: 500px;
		background-color: #fff;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		overflow-y: auto;
	}
	.exam-dir-close{
		padding: 5px 10rpx;
		text-align: right;
	}
	.exam-dir-close text{
		border:1rpx solid red;
		border-radius: 6rpx;
		padding: 2px 8rpx;
		color: red;
	}
	.exam-dir-row{
		display: flex;
		flex-wrap: wrap;
	}
	.exam-dir-item{
		padding: 2px 20rpx;
		border: 1rpx solid #73a9fa;
		border-radius: 4rpx;
		margin-bottom: 10px;
	}
</style>
