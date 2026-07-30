<template>
	<view>
		<view v-if="exams.length>0">
			<view class="row-box mar-B10" v-for="(exam,i) in exams">
				<view class="font-wb">{{exam.title}}</view>
				<view class="">题量:{{exam.nums}}</view>
				<view>状态:{{exam.status}}</view>
				<text class="text-audit" @click="onAudit(i)">去审核</text>
			</view>
		</view>
		<view v-else class="text-C dis-flex-hv" style="height: 100px;">
			<view v-if="loading">
				<view><i class="gjjd-iconfont icon-record color-92 font-s30"></i></view>
				<view><text class="color-92">暂无审核任务</text></view>
			</view>
		</view>
		<uni-popup ref="audit" type="center" :mask-click="false">
			<view class="popup-audit">
				<view class="popup-head text-C font-wb">{{current.title}} ({{current.nums}})</view>
				<view class="popup-body scrollbar-none">
					<view class="dis-flex mar-B10">
						<view class="w-140">题型:</view>
						<view class="w-full">{{currentExam.exam_type}}</view>
					</view>
					<view class="dis-flex mar-B10">
						<view class="w-140">题干:</view>
						<view class="w-full">
							<textarea class="textarea" v-model="currentExam.exam_content"></textarea>
						</view>
					</view>
					<template v-if="currentExam.exam_type == '判断题'">
						<view class="dis-flex mar-B10">
							<view class="w-140">参考答案:</view>
							<view class="w-full">
								<radio-group @change="onPanduan">
									<radio value="A" :checked="currentExam.exam_answer=='A'" class="mar-L20 mar-R20">对</radio>
									<radio value="B" :checked="currentExam.exam_answer=='B'">错</radio>
								</radio-group>
							</view>
						</view>
					</template>
					<template v-else>
						<view class="dis-flex mar-B10">
							<view class="w-140 w-input">A</view>
							<view class="w-full">
								<input class="input" v-model="currentExam.exam_a" />
							</view>
						</view>
						<view class="dis-flex mar-B10">
							<view class="w-140 w-input">B</view>
							<view class="w-full">
								<input class="input" v-model="currentExam.exam_b" />
							</view>
						</view>
						<view class="dis-flex mar-B10">
							<view class="w-140 w-input">C</view>
							<view class="w-full">
								<input class="input" v-model="currentExam.exam_c" />
							</view>
						</view>
						<view class="dis-flex mar-B10" v-if="currentExam.exam_d">
							<view class="w-140 w-input">D</view>
							<view class="w-full">
								<input class="input" v-model="currentExam.exam_d" />
							</view>
						</view>
						<template v-if="currentExam.exam_type == '多选题'">
							<view class="dis-flex mar-B10" v-if="currentExam.exam_e">
								<view class="w-140 w-input">E</view>
								<view class="w-full">
									<input class="input" v-model="currentExam.exam_e" />
								</view>
							</view>
							<view class="dis-flex mar-B10" v-if="currentExam.exam_f">
								<view class="w-140 w-input">F</view>
								<view class="w-full">
									<input class="input" v-model="currentExam.exam_f" />
								</view>
							</view>
							<view class="dis-flex mar-B10" v-if="currentExam.exam_g">
								<view class="w-140 w-input">G</view>
								<view class="w-full">
									<input class="input" v-model="currentExam.exam_g" />
								</view>
							</view>
							<view class="dis-flex mar-B10" v-if="currentExam.exam_h">
								<view class="w-140 w-input">H</view>
								<view class="w-full">
									<input class="input" v-model="currentExam.exam_h" />
								</view>
							</view>
						</template>
						<view class="dis-flex mar-B10">
							<view class="w-140 w-input">参考答案</view>
							<view class="w-full">
								<input class="input" v-model="currentExam.exam_answer" />
							</view>
						</view>
					</template>
				</view>
				<view class="exam-sumit" @click="examSubmit()">提交</view>
				<view class="mar-T20 text-C font-s12 color-red"><text>注:提交成功后会自动显示下一题</text></view>
			</view>
			<view class="mar-T20 text-C">
				<text class="color-ff" @click="onAuditClose()">关闭</text>
			</view>
		</uni-popup>
	</view>
</template>

<script>
	import {request,uploads,makeCall,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				loading:0,
				role:0,
				exams:[],
				index:-1,
				current:{title:'',nums:0},
				examDetails:[],
				currentExam:{
					id:0,
					exam_type:'',
					exam_conent:'',
					exam_a:'',
					exam_b:'',
					exam_c:'',
					exam_d:'',
					exam_e:'',
					exam_f:'',
					exam_g:'',
					exam_h:'',
					exam_answer:'',
					isblod:0
				},
				isSubmit:0
			}
		},
		onLoad(options) {
			//console.log("options",options);
			if(options.item){
				let item = JSON.parse(decodeURIComponent(options.item));
				console.log("item",item);
				this.role = item.role;
				this.getData();
			}
		},
		onShow() {
			
		},
		methods: {
			async getData(){
				this.loading = 0;
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:{role:this.role},
					url:"/apptask/exam"
				});
				uni.hideLoading();
				this.loading = 1;
				console.log(res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.exams){
					this.exams = res.data.exams;
				}
			},
			async onAudit(index){
				this.index = index;
				this.current = this.exams[index];
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:{task_id:this.current.id,role:this.role},
					url:"/apptask/examdetail"
				});
				uni.hideLoading();
				console.log(res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				if(res?.data?.auditcount){}
				if(res?.data?.list && res.data.list.length>0){
					this.examDetails = res.data.list;
					this.currentExam = this.examDetails[0];
					this.$refs.audit.open();
				}else{
					showMsg('this',{title:'当前没有要审的题了'});
				}
			},
			async examSubmit(){
				if(this.isSubmit) return;
				this.isSubmit = 1;
				this.currentExam.role = this.role;
				/*console.log("*****",this.currentExam);
				return;*/
				uni.showLoading({
					title:'数据加载中'
				});
				let res = await request({
					method:'POST',
					data:this.currentExam,
					url:"/apptask/taskexamsubmit"
				});
				console.log(res);
				if(res.code){
					this.isSubmit = 0;
					uni.hideLoading();
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
					return;
				}
				showMsg('this',{title:'成功'});
				let tid = setTimeout(()=>{
					clearTimeout(tid);
					uni.hideLoading();
					this.examDetails.splice(0,1);
					this.exams[this.index].nums--;
					if(this.examDetails.length>0){
						this.currentExam = this.examDetails[0];
						this.isSubmit = 0;
					}else{
						this.isSubmit = 0;
						this.onAuditClose();
					}
				},1500);
			},
			onPanduan(e){
				this.currentExam.exam_answer = e.detail.value;
			},
			onAuditClose(){
				this.index = -1;
				this.current = {title:'',nums:0};
				this.examDetails = [];
				this.currentExam = {
					id:0,
					exam_type:'',
					exam_conent:'',
					exam_a:'',
					exam_b:'',
					exam_c:'',
					exam_d:'',
					exam_e:'',
					exam_f:'',
					exam_g:'',
					exam_h:'',
					exam_answer:'',
					isblod:0,
				};
				this.$refs.audit.close();
			}
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.row-box{
		width: 690rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding: 10rpx;
		background-color: #fff;
		border-radius: 12rpx;
		position: relative;
	}
	.text-audit{
		position: absolute;
		top: 4px;
		right: 10rpx;
		padding: 4px 20rpx;
		color: #fff;
		background-color: #4ec6f6;
		border-radius: 6rpx;
		font-size: 12px;
	}
	.popup-audit{
		width: 690rpx;
		background-color: #fff;
		border-radius: 12rpx;
		padding-bottom: 10px;
	}
	.popup-head{
		padding-top: 10px;
	}
	.popup-body{
		max-height: 400px;
		padding: 10px;
		overflow-y: auto;
	}
	.textarea{
		width: 90%;
		height: 80px;
		padding: 4px 6px;
		border: 1rpx solid #dedede;
		border-radius: 8rpx;
	}
	.input{
		width: 90%;
		height: 30px;
		line-height: 30px;
		padding: 4px 6px;
		border: 1rpx solid #dedede;
		border-radius: 8rpx;
	}
	.w-input{
		height: 30px;
		line-height: 30px;
	}
	.exam-sumit{
		height: 44px;
		line-height: 44px;
		margin-top:10px;
		margin-left: 40rpx;
		margin-right: 40rpx;
		font-size: 16px;
		font-weight: bold;
		text-align: center;
		color: #fff;
		background-color: #4ec6f6;
		border-radius: 22px;
	}
</style>
