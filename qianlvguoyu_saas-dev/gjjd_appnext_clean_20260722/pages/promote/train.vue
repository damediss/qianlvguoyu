<template>
	<view class="content">
		<view :style="'top:'+($statusBarHeight+5)+'px;'" style="position:fixed;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启,将无法使用上述功能。</view>
		</view>
		<view class="nav-back" :style="'top:'+($statusBarHeight+5)+'px;'" @click="toBack">
			<i class="gjjd-iconfont icon-xiangzuo font-s20 color-ff"></i>
		</view>
		<view class="page-head">
			<view :style="'height: '+$statusBarHeight+'px;'"><!--状态栏占位--></view>
			<view class="video-box">
				<video v-if="activeTag=='study'" style="width: 99%;"
				id="video_demo"
				autoplay
				:src="videoUrl"
				:controls="true"
				:show-fullscreen-btn="false"
				@ended="playEnd"></video>
				<view v-else class="cover">
					<image :src="info.cover" model="widthFix" style="width: 100%;"></image>
				</view>
			</view>
			<view class="tabs">
				<view class="tabs-item dis-flex-hv" v-for="tab in tabs">
					<view @click="onTabs(tab)">
						<view>{{tab.title}}</view>
						<view class="dis-flex-hv" v-if="tab.tag == activeTag">
							<view class="tabs-active"></view>
						</view>
					</view>
				</view>
			</view>
		</view>
		<view :style="'height:'+($statusBarHeight+230+44)+'px;'"><!--page-head占位--></view>
		<view v-if="activeTag=='detail'" class="tabs-detail">
			<view class="mar-B10">{{info.class.title}}</view>
			<!-- <view>工种:{{info.class.work_type}}</view>
			<view>人数:{{info.class.nums}}</view>
			<view>开始日期:{{info.date}}</view> -->
			<view class="mar-B10">
				<text class="color-92 mar-R20">工种:{{info.class.work_type}}</text>
				<text class="color-92 mar-R20">{{info.class.nums}}人学习</text>
				<text class="color-92">开始日期:{{info.date}}</text>
			</view>
			<view class="mar-B10">{{info.class.desc}}</view>
			<view class="train-row">
				<view class="dis-flex">
					<view class="teacher-avatar"><image :src="info.teacher.avatar"></image></view>
					<view class="teacher-info">
						<view class="teacher-name mar-B10">
							{{info.teacher.realname}}
							<text class="teacher-tag">{{info.teacher.major}}</text>
						</view>
						<view class="row-tags mar-B10" v-if="info?.teacher?.tags">
							<template v-for="(ta,ti) in info.teacher.tags">
								<text class="color-ff" :class="ti>0?'mar-L20 '+ta.bgclass:ta.bgclass">{{ta.title}}</text>
							</template>
						</view>
						<view class="teacher-desc color-92">{{info.teacher.desc}}</view>
					</view>
				</view>
			</view>
			<view class="train-row">
				{{info.desc}}
			</view>
			<view class="train-row">
				<view class="mar-B10">课程推荐</view>
				<view class="dis-flex mar-B10" v-for="(item) in kcTuijians">
					<view class="kejian-cover"><image :src="item.cover"></image></view>
					<view class="kejian-info">
						<view class="kejian-name mar-B10">
							{{item.title}}
						</view>
						<view class="row-tags mar-B10" v-if="item.tags.length>0">
							<template v-for="(ta,ti) in item.tags">
								<text class="color-ff" :class="ti>0?'mar-L20 '+ta.bgclass:ta.bgclass">{{ta.title}}</text>
							</template>
						</view>
						<view class="mar-B10 color-92">{{item.desc}}</view>
						<view class="dis-flex-hsb font-s14">
							<view class="color-red">￥<text>{{item.price}}</text></view>
							<view>
								<i class="gjjd-iconfont icon-dianhua"></i>
								<text class="color-92">{{item.kfTel}}</text>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>
		<view v-if="activeTag=='study'" class="tabs-study">
			<view v-for="(item,i) in info.kejian" class="study-item mar-B20" :class="{'border-73a9fa':i==kjIndex}">
				<view class="study-title border-b-e0" @click="onStudyShow(i)">
					{{item.sno}} {{item.title}}
					<text class="study-show-btn"><i class="gjjd-iconfont" :class="item.showAll?'icon-xiangxia':'icon-xiangshang'"></i></text>
				</view>
				<view v-if="item.showAll" style="padding: 0 30rpx;">
					<view class="dis-flex-hsb border-b-e0 pad-T20 pad-B20" @click="onPlay(i)">
						<view>
							<text class="study-tag">视频</text>
							<text class="mar-L10">{{item.title}}</text>
						</view>
						<view><i class="gjjd-iconfont icon-shipin1 color-92"></i></view>
					</view>
					<view class="dis-flex-hsb border-b-e0 pad-T20 pad-B20 mar-T20" @click="onRead(i)">
						<view>
							<text class="study-tag">文档</text>
							<text class="mar-L10">{{item.title}}</text>
						</view>
						<view><i class="gjjd-iconfont icon-yanjing color-92"></i></view>
					</view>
					<view class="mar-T20" @click="toShareTab">
						<text class="study-tag">讨论</text>
						<text class="mar-L10">讨论</text>
					</view>
				</view>
			</view>
		</view>
		<view v-if="activeTag=='exam'" class="tabs-exam">
			<view v-for="(item,i) in info.kejian" class="exam-it mar-B20 border-b-e0" @click="openExam(item,i)">
				<view class="w-120 text-C"><i class="gjjd-iconfont icon-bianji font-s30"></i></view>
				<view class="w-full">{{item.xuhao}}、{{item.title}}</view>
				<view class="kejian-exam-count mar-T5 font-s12" @click.stop="toExamLog(item)">练习次数({{item.examcount}})</view>
			</view>
		</view>
		<view v-if="activeTag=='share'" class="tabs-share">
			<view class="share-types mar-B20">
				<view class="w-200 bg-73a9fa color-ff text-C" :class="{'border-f44336':commentType==0}" @click="onCommentType(0)">综合讨论区</view>
				<view class="w-200 bg-fed912 color-ff text-C" :class="{'border-f44336':commentType==1}" @click="onCommentType(1)">老师答疑区</view>
				<view class="w-200 bg-4caf50 color-ff text-C" :class="{'border-f44336':commentType==2}" @click="onCommentType(2)">课堂交流区</view>
			</view>
			<view class="share-list">
				<view class="share-list-item" v-for="(item,i) in comments">
					<view class="share-avatar">
						<image v-if="item.avatar" :src="item.avatar"></image>
						<image v-else src="/static/images/logo.jpg"></image>
					</view>
					<view class="share-info">
						<view class="share-user">
							{{item.realname}}
							<text class="share-times">{{item.times}}</text>
						</view>
						<view class="mar-B20">
							<uni-rate size="18" :value="item.rates" @change="onRateChange($event,item.id)" /> 
						</view>
						<view v-if="item.text_type>0"class="mar-B20">
							<view v-if="item.imgs.length==3" class="share-pic-3">
								<image :src="item.imgs[0]" model="widthFix"></image>
								<image :src="item.imgs[1]" model="widthFix"></image>
								<image :src="item.imgs[2]" model="widthFix"></image>
							</view>
							<view v-if="item.imgs.length==2" class="share-pic-2">
								<image :src="item.imgs[0]" model="widthFix"></image>
								<image :src="item.imgs[1]" model="widthFix"></image>
							</view>
							<view v-if="item.imgs.length==1" class="share-pic-1">
								<image :src="item.imgs[0]" model="widthFix"></image>
							</view>
						</view>
						<view v-else class="mar-B20 color-62">
							{{item.text_content}}
							<text class="share-reply-btn" @click="onReply(i,-1)">回复</text>
						</view>
						<view class="share-reply-list">
							<view class="share-reply mar-B20" v-for="(ritem,ri) in item.replys" @click="onReply(i,ri)">
								<text>{{ritem.realname}}</text> 
								<text v-if="ritem.reply_id!=item.parent_id">回复{{ritem.reply_user}}:</text>
								<text v-else>回复:</text>
								<text>{{ritem.text_content}}</text>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>
		<view style="height:40px;"><!--page-footer占位--></view>
		<view class="page-footer">
			<view class="mar-L20 mar-R20 text-C">
				<text v-if="activeTag=='share'" class="comment-write bg-73a9fa color-ff" @click="onReply(-1,-1)">写评论</text>
				<text v-else>共{{info.kejian.length}}节</text>
			</view>
		</view>
		<!-- 课件图文模式 -->
		<uni-popup ref="popupKejian" type="bottom" :mask-click="false">
			<view class="kejian-tuwen">
				<view class="kejian-content">
					<view class="kejian-content-box scrollbar-none::-webkit-scrollbar" :style="'height:'+(($winHeight*2/3) - 50)+'px;'">
						<uv-parse :content="tuwenContent"></uv-parse>
					</view>
				</view>
				<view class="mar-T20 text-C">
					<text class="do-xiti mar-R20" @click="onKejianXiti()">做习题</text>
					<text class="video-close" @click="onKejianXitiClose()">关闭</text>
				</view>
			</view>
		</uni-popup>
		<!-- 课后练习弹窗 -->
		<uni-popup ref="popupExam" type="center" :mask-click="false">
			<view class="exam-box">
				<view class="exam-title">{{ examIndex + 1 }}/{{ examList.length }}</view>
				<view class="exam-item scrollbar-none" v-if="examList.length > 0">
					<view class="color-73a9fa">
						<text v-if="examList[examIndex].type == 'multi'">多选题</text>
						<text v-if="examList[examIndex].type == 'single'">单选题</text>
						<text v-if="examList[examIndex].type == 'judge'">判断题</text>
					</view>
					<view class="pad-T10 pad-B10 font-s16">
						<uv-parse :content="examList[examIndex].content"></uv-parse>
					</view>
					<view class="exam-answer">
						<template v-if="examList[examIndex].type == 'multi'">
							<template v-for="(item, index) in examList[examIndex].answer">
								<view class="exam-answer-item border-92 mar-B10"
									:class="{ 'bg-73a9fa color-ff': examList[examIndex]['user_answer'].includes(item.tag) }"
									@click="multiAnswer(index)">
									<view class="mar-L20">{{ item.tag }}、</view>
									<view class="mar-L10"><uv-parse :content="item.content + ''"></uv-parse></view>
								</view>
							</template>
						</template>
						<template v-else>
							<template v-for="(item, index) in examList[examIndex].answer">
								<view class="exam-answer-item border-92 mar-B10"
									:class="{ 'bg-73a9fa color-ff': examList[examIndex]['user_answer'] == item.tag }"
									@click="selectedAnswer(index)">
									<view class="mar-L20">{{ item.tag }}、</view>
									<view class="mar-L10"><uv-parse :content="item.content + ''"></uv-parse></view>
								</view>
							</template>
						</template>
					</view>
				</view>
				<view class="exam-btns">
					<view class="mar-L20">
						<text v-if="examIndex == 0" class="btn-none color-92">上一题</text>
						<text v-else class="bg-73a9fa color-ff" @click="doBack">上一题</text>
					</view>
					<view class="mar-R20">
						<text class="bg-73a9fa color-ff" @click="doNext">{{ examIndex == examList.length - 1 ? '查看结果' : '下一题'}}</text>
					</view>
				</view>
			</view>
			<view class="text-C mar-T20">
				<i class="gjjd-iconfont icon-round_delete color-ff font-s30" @click="onKejianClose"></i>
			</view>
		</uni-popup>
		<!-- 练习结果弹窗 -->
		<uni-popup ref="popupExamlog" type="bottom" :mask-click="false">
			<view class="exam-log">
				<view style="height: 30px;line-height:30px;text-align: right;">
					<text class="mar-R10" @click="examlogClose">关闭</text>
				</view>
				<view class="pad-L10 pad-R10">
					<view class="dis-flex-hsb">
						<view class="w-full">做题日期</view>
						<view class="w-80">题量</view>
						<view class="w-80 text-R">得分</view>
					</view>
				</view>
				<view :style="'height:'+(($winHeight*2/3)-150)+'px;'" style="overflow-y: auto;scrollbar-none::-webkit-scrollbar">
				<view class="pad-L10 pad-R10" :class="i%2>0?'bg-73a9fa':'bg-a8c8ff'" v-for="(item,i) in examlogs">
					<view class="dis-flex-hsb">
						<view class="w-full">{{item.date}}</view>
						<view class="w-80">{{item.nums}}</view>
						<view class="w-80 text-R">{{item.score}}</view>
					</view>
				</view>
				</view>
			</view>
		</uni-popup>
		<!-- 回复弹窗 -->
		<uni-popup ref="popupComment" type="bottom" :mask-click="false">
			<view class="popup-comment">
				<view class="comment-head">
					<text class="mar-R10" @click="popupCommentClose">关闭</text>
				</view>
				<view class="comment-input">
					<textarea v-model="commentReply" placeholder="请输入回复内容"></textarea>
				</view>
				<view class="comment-tools">
					<view>
						<i class="gjjd-iconfont icon-tupian2 font-s30" @click="commentPic"></i>
					</view>
					<view class="comment-send" @click="commentSend($event)">发送</view>
				</view>
				<view v-if="isKeyboard" style="height: 60px;"></view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
	import {request, uploads, navTo, showMsg} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip:false,
				id:0,
				activeTag:'detail',
				tabs:[
					{title:'课程介绍',tag:'detail'},
					{title:'课程大纲',tag:'study'},
					{title:'课后作业',tag:'exam'},
					{title:'讨论',tag:'share'}
				],
				info:{
					id:0,
					cover:'',
					class:{
						id:0,
						title:'',
						work_type:'',
						nums:0
					},
					teacher:{
						teacher_id:0,
						realname:'',
						tags:[],
						avatar:'',
						desc:'',
						major:''
					},
					kejian:[]
				},
				videoUrl:'',
				currentVideo:null,
				kjIndex:0,
				commentType:0,
				commentIndex:-1,
				commentreIndex:-1,
				commentReply:'',
				isKeyboard:0,
				comments:[],
				commentPage:1,
				kcTuijians:[],
				examSubmit:0,
				examkjIndex:-1,
				examList:[],
				examIndex:0,
				examlogs:[],
				tuwenContent:'',
				tuwenTimes:0,
				tuwenTimeid:0
			}
		},
		onLoad(options) {
			if(options.id){
				this.id = options.id;
				this.getData();
			}
		},
		mounted() {
			uni.onKeyboardHeightChange(res => {
				console.log("KeyboardHeightChange",res.height);
				if(res.height>0){
					this.isKeyboard = 1;
				}else{
					this.isKeyboard = 0;
				}
			});
		},
		onUnload() {
			this.examList = [];
			if(this.tuwenTimeid>0){
				clearInterval(this.tuwenTimeid);
				this.tuwenTimeid = 0;
				this.tuwenTimes = 0;
			}
		},
		methods: {
			toBack(){
				uni.navigateBack();
			},
			async getData(){
				let res = await request({
					method:'POST',
					data:{id:this.id},
					url:"/promote/train-detail"
				});
				console.log("promote train detail",res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
				}
				if(res?.data?.info){
					this.info = res.data.info;
				}
				if(res?.data?.kcTuijian){
					this.kcTuijians = res.data.kcTuijian;//
				}
			},
			async getComments(flag=false){
				if(flag){
					this.comments = [];
					this.commentPage = 1;
				}
				let res = await request({
					method:'POST',
					data:{types:this.commentType,page:this.commentPage,tag:'train'},
					url:"/promote/comment-list"
				});
				console.log("get Comments",res);
				if(res?.data?.list){
					if(res.data.list.length>0){
						this.comments = [...this.comments,...res.data.list];
						this.commentPage++;
					}
				}
			},
			onTabs(tab){
				if(tab.tag == this.activeTag){
					return;
				}
				this.activeTag = tab.tag;
				if(this.activeTag == 'study'){
					this.videoUrl = this.info.kejian[this.kjIndex].video;
					this.currentVideo = uni.createVideoContext("video_demo",this);
				}else{
					this.videoUrl = '';
					if(this.activeTag == 'share'){
						this.getComments(true);
					}
				}
			},
			toShareTab(){
				this.activeTag = 'share';
				this.videoUrl = '';
				this.getComments(true);
			},
			onStudyShow(i){
				if(this.info.kejian[i].showAll){
					this.info.kejian[i].showAll = 0;
				}else{
					this.info.kejian[i].showAll = 1;
				}
			},
			onPlay(i){
				//if(i == this.kjIndex) return;
				this.currentVideo.stop();//停止当前播放
				this.currentVideo.seek(0);
				this.kjIndex = i;
				this.videoUrl = this.info.kejian[this.kjIndex].video;
				this.currentVideo.play();//播放当前
			},
			playEnd(e){
				console.log("play end",e);
				this.videoUrl = "";
				this.tuwenContent = "";
				//显示习题
				this.openExam(this.info.kejian[this.kjIndex],this.kjIndex);
			},
			onRead(i){
				//if(i == this.kjIndex) return;
				this.kjIndex = i;
				this.currentVideo.stop();//停止当前播放
				this.currentVideo.seek(0);
				this.videoUrl = "";//this.info.kejian[i].video;
				let item = this.info.kejian[i];
				console.log("toKejian",item);
				this.tuwenContent = item.content;
				this.tuwenTimes = 0;
				if(this.tuwenTimeid>0){
					clearInterval(this.tuwenTimeid);
					this.tuwenTimeid = 0;
				}
				this.tuwenTimeid = setInterval(()=>{
					this.tuwenTimes++;
				},1000);
				this.examList = item.exams;
				this.$refs.popupKejian.open();
			},
			onKejianXiti(){
				if(this.tuwenTimeid>0){
					if(this.tuwenTimes<60*3){
						showMsg('this',{title:'您当前学习时长小于3分钟,还不能做题'});
						return;
					}
					clearInterval(this.tuwenTimeid);
					this.tuwenTimeid = 0;
				}
				this.videoUrl = "";
				this.tuwenContent = "";
				//显示习题
				this.openExam(this.info.kejian[this.kjIndex],this.kjIndex);
			},
			onKejianXitiClose(){
				this.isExam = 0;
				this.tuwenTimes = 0;
				this.tuwenContent = "";
				this.videoUrl = "";
				this.examList = [];
				this.currentIndex = 0;
				this.$refs.popupKejian.close();
				if(this.tuwenTimeid>0){
					clearInterval(this.tuwenTimeid);
					this.tuwenTimeid = 0;
				}
			},
			onCommentType(type){
				this.commentType = type;
			},
			onReply(i,ri){
				this.commentIndex = i;
				this.commentreIndex = ri;
				this.$refs.popupComment.open();
			},
			popupCommentClose(){
				this.commentIndex = -1;
				this.commentreIndex = -1;
				this.commentReply = '';
				this.$refs.popupComment.close();
			},
			async  commentPic(){
				if(!(this.commentIndex==-1 && this.commentreIndex==-1)){
					showMsg('this',{title:'回复评论不能发布图片'});
					return;
				}
				let res = await uploads({limit:3},this);
				console.log('pic',res);
				if(res.length>0){
					this.commentSend({},res);
				}
			},
			async commentSend(e,imgs=[]){
				let text_type = 0;
				if(imgs.length>0) text_type = 1;
				if(text_type == 0 && !this.commentReply){
					showMsg('this',{title:'请填写评论'});
					return;
				}
				let param = {
					types:this.commentType,
					text_type,
					text_content:this.commentReply,
					imgs,
					parent_id:0,
					reply_id:0,
					reply_user_id:0,
					reply_user:'',
					tag:'train'
				}
				if(!(this.commentIndex==-1 && this.commentreIndex==-1)){
					let comment = this.comments[this.commentIndex];
					param['parent_id'] = comment.id;
					if(this.commentreIndex>-1){
						let reply = this.comments[this.commentIndex].replys[this.commentreIndex];
						param['reply_id'] = reply.id;
						param['reply_user_id'] = reply.platform_user_id;
						param['reply_user'] = reply.platform_user;
					}
				}
				let res = await request({
					method:'POST',
					data:param,
					url:"/promote/comment-save"
				});
				console.log("comment send",res);
				showMsg("this",{title:res.msg});
				if(res?.code){
					return;
				}
				if(res?.data?.info){
					if(!(this.commentIndex==-1 && this.commentreIndex==-1)){
						this.comments[this.commentIndex].replys.push(res.data.info);
					}else{//写评论
						let newComment = [];
						newComment.push(res.data.info);
						this.comments = [...newComment,...this.comments];
					}
				}
				this.popupCommentClose();
				/*let tid = setTimeout(()=>{
					this.getComments(true);
				},2000);*/
			},
			async onRateChange(e,id=0){
				console.log(e,id);
				let rates = Number(e.value);
				if(rates>5) rates = 5;
				if(rates<0) rates = 0;
				if(id == 0) return;
				let res = await request({
					method:'POST',
					data:{id,rates},
					url:"/promote/comment-rate-save"
				});
				console.log("rates change",res);
			},
			openExam(item,index){
				this.examkjIndex = index;
				this.examList = item.exams;
				this.$refs.popupExam.open();
			},
			onKejianClose(){
				this.examList = [];
				this.examIndex = 0;
				this.examkjIndex = -1;
				this.$refs.popupExam.close();
			},
			multiAnswer(index) {
				if (this.examList[this.examIndex]['user_answer'].includes(this.examList[this.examIndex]['answer'][index]['tag'])) {
					for (let i in this.examList[this.examIndex]['user_answer']) {
						if (this.examList[this.examIndex]['user_answer'][i] == this.examList[this.examIndex]['answer'][index]['tag']) {
							this.examList[this.examIndex]['user_answer'].splice(i, 1);
							break;
						}
					}
				} else {
					this.examList[this.examIndex]['user_answer'].push(this.examList[this.examIndex]['answer'][index]['tag']);
				}
			},
			selectedAnswer(index) {
				console.log("****="+index)
				this.examList[this.examIndex]['user_answer'] = this.examList[this.examIndex]['answer'][index]['tag'];
			},
			doBack() {
				if (this.examIndex > 0) {
					this.examIndex--;
				}
			},
			async doNext() {
				if (this.examList[this.examIndex].type == 'multi' && this.examList[this.examIndex]['user_answer'].length == 0) {
					showMsg('this', { title: '请选择答案' });
					return;
				}
				if (this.examList[this.examIndex].type != 'multi' && this.examList[this.examIndex]['user_answer'] == '') {
					showMsg('this', { title: '请选择答案' });
					return;
				}
				if (this.examIndex < this.examList.length - 1) {
					this.examIndex++;
				}else{
					if(this.examSubmit) return;
					this.examSubmit = 1;
					console.log("练习结果",this.examList);
					let rightCount = 0;
					for(let i in this.examList){
						let type = this.examList[i]['type'];
						let answer = this.examList[i]['user_answer'];
						let obj = this.examList[i].answer;
						let isright = 1;
						if(type == 'multi'){
							for(let j in obj){
								if(!obj[j].isCorrect && answer.includes(obj[j]['tag'])){
									isright = 0;
								}
							}
						}else{
							for(let j in obj){
								if(obj[j].isCorrect && answer!=obj[j]['tag']){
									isright = 0;
								}
							}
						}
						if(isright) rightCount++;
					}
					//计算本次正确率
					console.log(rightCount+","+this.examList.length)
					let rate = (rightCount/this.examList.length)*100;
					rate = rate.toFixed(0);
					showMsg("this",{title:'本次得分:'+rate});
					uni.showLoading({
						title:'正在提交成绩...'
					});
					let res = await request({
						method:'POST',
						data:{exams:this.examList,score:rate},
						url:"/promote/train-detail-exam-log"
					});
					uni.hideLoading();
					this.examSubmit = 0;
					this.info.kejian[this.examkjIndex].examcount++;
					this.toExamLog(this.info.kejian[this.examkjIndex]);//显示结果
					console.log("promote train detail",res);
					this.onKejianClose();
				}
			},
			async toExamLog(item){
				console.log("ExamLog",item);
				let ck_id = item.id;
				let res = await request({
					method:'POST',
					data:{ck_id},
					url:"/promote/train-detail-exam-log-list"
				});
				console.log("promote train detail logs",res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg});
				}
				if(res?.data?.list){
					this.examlogs = res.data.list;
				}
				this.$refs.popupExamlog.open();
			},
			examlogClose(){
				this.examlogs = [];
				this.$refs.popupExamlog.close();
			},
		}
	}
</script>

<style>
	page{
		background-color: #f8f8f8;
	}
	.nav-back{
		position: fixed;
		left: 10px;
		z-index: 999;
		width: 80rpx;
		height: 80rpx;
		line-height: 80rpx;
		background-color: rgba(0, 0, 0, 0.5);
		border-radius: 40rpx;
		text-align: center;
	}
	.page-head{
		background-color: #3f3f4a;
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 9;
	}
	.page-footer{
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		z-index: 9;
		height: 40px;
		line-height: 40px;
		background-color: #f8f8f8;
	}
	.video-box{
		height: 230px;
	}
	.cover{
		height: 230px;
		overflow: hidden;
	}
	.tabs{
		background-color: #f8f8f8;
		height: 44px;
		padding: 0 10rpx;
		display: flex;
		white-space: nowrap;
		overflow-x: auto;
	}
	.tabs-item{
		margin-right: 20rpx;
	}
	.tabs-active{
		width: 80%;
		height: 4px;
		background-color: #ff6b6b;
		border-radius: 2px;
	}
	.tabs-content{
		width: 730rpx;
		padding: 0 10rpx;
		overflow-y: auto;
		/* border: 1rpx solid red; */
	}
	.tabs-detail{
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.train-row{
		border-top: 15px solid #f8f8f8;
		background-color: #fff;
		padding: 5px 20rpx;
	}
	.teacher-avatar{
		width: 100rpx;
		height: 100rpx;
	}
	.teacher-avatar image{
		width: 100rpx;
		height: 100rpx;
		background-color: #ddd;
		border-radius: 8rpx;
	}
	.teacher-info{
		flex: 1;
		margin-left: 20rpx;
	}
	.teacher-name{
		position: relative;
	}
	.teacher-tag{
		position: absolute;
		top: 0;
		right: 0;
		border:1rpx solid #929292;
		border-radius: 6rpx;
		padding: 3px 12rpx;
		font-size: 12px;
		color: #929292;
	}
	.kejian-cover{
		width: 160rpx;
		height: 100px;
	}
	.kejian-cover image{
		width: 160rpx;
		height: 100px;
		background-color: #ddd;
		border-radius: 8rpx;
	}
	.kejian-info{
		flex: 1;
		margin-left: 20rpx;
	}
	.tabs-study{
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.study-item{
		padding: 5px 0;
	}
	.study-title{
		position: relative;
	}
	.study-show-btn{
		position: absolute;
		right: 0;
		top: 0;
	}
	.study-tag{
		padding: 3px 16rpx;
		border: 1rpx solid #e0e0e0;
		color: #e0e0e0;
		border-radius: 8rpx;
		font-size: 12px;
	}
	.tabs-exam{
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.exam-it{
		display: flex;
		padding: 5px 0;
		position: relative;
	}
	.kejian-exam-count{
		position: absolute;
		top: 3px;
		right: 0;
		z-index: 99;
		background-color: #fff;
		border-radius: 6rpx;
		padding: 2px 6px;
	}
	.tabs-share{
		padding-top: 5px;
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.share-types{
		height: 60px;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.share-types view{
		height: 60px;
		line-height: 60px;
	}
	.share-list-item{
		display: flex;
		padding-top: 5px;
		border-bottom: 1px solid #e0e0e0;
	}
	.share-avatar{
		width: 100rpx;
		height: 100rpx;
	}
	.share-avatar image{
		width: 100rpx;
		height: 100rpx;
		background-color: #3b82f6;
		border-radius: 40px;
	}
	.share-info{
		flex:1;
		margin-left: 20rpx;
	}
	.share-user{
		position: relative;
	}
	.share-times{
		position: absolute;
		top: 0;
		right: 0;
		color: #929292;
		font-size: 12px;
	}
	.share-pic-1 image{
		flex: 1;
		height: 80px;
	}
	.share-pic-2,.share-pic-3{
		display: flex;
		justify-content: space-between;
	}
	.share-pic-2 image{
		width: 260rpx;
		height: 80px;
	}
	.share-pic-3 image{
		width: 190rpx;
		height: 80px;
	}
	.share-reply-list{}
	.share-reply{
		padding: 10px 10rpx;
		background-color: #e0e0e0;
		border-radius: 8rpx;
		color: #626262;
		font-size: 12px;
	}
	.share-reply-btn{
		border: 1rpx solid #626262;
		border-radius: 4rpx;
		padding: 1px 2px;
		font-size: 12px;
	}
	.comment-write{
		padding: 3px 12rpx;
		border-radius: 6rpx;
	}
	.row-tags text{
		padding: 3px 12rpx;
		font-size: 12px;
		border-radius: 6rpx;
	}
	.popup-comment{
		background-color: #fff;
		padding-bottom: 20px;/*安全区域*/
	}
	.comment-head{
		padding: 0 10px;
		height: 30px;
		line-height:30px;
		text-align: right;
	}
	.comment-input{
		padding: 5px 10px;
	}
	.comment-input textarea{
		background-color: #e0e0e0;
		border-radius: 8rpx;
		width: 670rpx;
		height: 50px;
		padding: 5px 10px;
	}
	.comment-tools{
		padding: 5px 10px;
		display: flex;
		justify-content: space-between;
	}
	.comment-send{
		width: 100rpx;
		height: 30px;
		line-height: 30px;
		border:1rpx solid #e0e0e0;
		border-radius: 15px;
		text-align: center;
		font-size: 14px;
	}
	.kejian-tuwen{
		width: 750rpx;
		background-color: #fff;
		border-top-left-radius: 12rpx;
		border-top-right-radius: 12rpx;
	}
	.kejian-content{
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-top: 5px;
	}
	.kejian-content-box{
		overflow-y: auto;
		border: 1rpx solid #0099ff;
		padding: 3px 5px;
	}
	.do-xiti{
		padding: 2px 6rpx;
		background-color: #0099ff;
		border-radius: 4rpx;
		color: #fff;
	}
	.exam-box {
		width: 710rpx;
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-bottom: 15px;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.exam-title {
		height: 50px;
		line-height: 50px;
		padding: 0 20rpx;
	}
	.exam-item {
		margin-left: 20rpx;
		margin-right: 20rpx;
		padding-top: 5px;
		max-height: 500px;
		overflow-y: auto;
		font-size: 14px;
	}
	.exam-answer-item {
		display: flex;
		align-items: center;
		padding: 4px 0;
		min-height: 40px;
		border-radius: 22px;
	}
	.exam-btns {
		margin-top: 10px;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.exam-btns text {
		padding: 5px 10px;
		border-radius: 8rpx;
	}
	.btn-none {
		background-color: #ccc;
	}
	.exam-log{
		width: 750rpx;
		background-color: #fff;
		border-top-left-radius: 12rpx;
		border-top-right-radius: 12rpx;
	}
</style>
