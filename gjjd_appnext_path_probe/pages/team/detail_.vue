<template>
	<view class="content">
		<view :style="'top:'+($statusBarHeight+5)+'px;'" style="position:fixed;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启,将无法使用上述功能。</view>
		</view>
		<view class="navs" :style="'top:'+$statusBarHeight+'px;'">
			<i @click="toBack" class="gjjd-iconfont icon-xiangzuo font-s20 font-wb color-ff"></i>
			<text class="mar-L20 font-wb color-ff">{{master.title}}</text>
		</view>
		<view class="top">
			<image v-if="albums.length>0" class="top-bg" :src="albums[0]['pic']"></image>
			<view class="pinfen dis-flex-hv">
				<view @click="toPingfen">
					<view class="mar-B5"><i class="gjjd-iconfont icon-shoucang font-s40"></i></view>
					<view>工作室评分</view>
				</view>
			</view>
		</view>
		<view class="body" :style="'height:'+bHeight+'px;'">
			<view class="body-top">
				<view style="height: 30px;line-height: 30px;">{{master.title}}</view>
				<view class="member-box dis-flex mar-T10 font-s12">
					<view class="member dis-flex">
						<template v-for="(item,i) in members">
							<image :src="item.avatar" :style="i>0?'margin-left:-20rpx;':''"></image>
						</template>
					</view>
					<view class="mar-L20">
						<text>{{members.length}}</text>成员在学习
						<text class="color-73a9fa hide">能力评分:0分</text>
					</view>
				</view>
				<view class="tabs dis-flex-vc mar-T10">
					<template v-for="(item,i) in tabs">
						<view class="tabs-item dis-flex-hv mar-R10" v-if="(['intro','outcome','album'].includes(item.type))||(['member','task','cuiqu','share'].includes(item.type) && isMember>0)">
							<view @click="onTabs(i)">
								<view class="text-C mar-B5">{{item.title}}</view>
								<view class="dis-flex-hv">
									<view class="tabs-active" v-if="tabIndex==i"></view>
								</view>
							</view>
						</view>
					</template>
				</view>
			</view>
			<view class="body-list">
				<view v-if="tabType == 'intro'" class="list-content" :style="'height:'+lHeight+'px;'">
					<swiper class="swiper" circular :indicator-dots="true" :autoplay="true" v-if="detail.introAlbums.length>0">
						<swiper-item v-for="pic in detail.introAlbums">
							<view class="swiper-item">
								<image :src="pic" mode="widthFix"></image>
							</view>
						</swiper-item>
					</swiper>
					<view class="jianjie">
						<uv-parse :content="detail.intro"></uv-parse>
					</view>
				</view>
				<view v-if="tabType == 'outcome'" class="list-content" :style="'height:'+lHeight+'px;'">
					<!-- 专利申报 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">专利申报</view>
						<view>
							<uni-grid :column="3" :square="false" :highlight="false" @change="onGridChange($event,'patents')">
								<uni-grid-item v-for="(item,index) in outcomes.patents" :index="index" :key="index">
									<view class="grid-item-box">
										<text class="text">{{item.title}}</text>
									</view>
								</uni-grid-item>
							</uni-grid>
						</view>
					</view>
					<!-- 获奖情况 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">获奖情况</view>
						<view>
							<uni-grid :column="3" :square="false" :highlight="false" @change="onGridChange($event,'rewards')">
								<uni-grid-item v-for="(item,index) in outcomes.rewards" :index="index" :key="index">
									<view class="grid-item-box">
										<text class="text">{{item.title}}</text>
									</view>
								</uni-grid-item>
							</uni-grid>
						</view>
					</view>
					<!-- 技能专家团队 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">技能专家团队</view>
						<view>
							<view class="expert-row border-b-e0" v-for="(item,index) in outcomes.experts">
								<view class="expert-avatar"><image></image></view>
								<view class="expert-info">
									<view class="mar-B10">姓名:{{item.realname}}</view>
									<view class="mar-B10">职称:{{item.zhicheng}}</view>
									<view class="mar-B10">研究方向:{{item.desc}}</view>
								</view>
							</view>
						</view>
					</view>
					<!-- 文件资料展示 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">文件资料展示</view>
						<view>
							<uni-grid :column="3" :square="false" :highlight="false" @change="onGridChange($event,'files')">
								<uni-grid-item v-for="(item,index) in outcomes.files" :index="index" :key="index">
									<view class="grid-item-box">
										<text class="text">{{item.title}}</text>
									</view>
								</uni-grid-item>
							</uni-grid>
						</view>
					</view>
					<!-- 论文发表情况 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">论文发表情况</view>
						<view>
							<uni-grid :column="3" :square="false" :highlight="false" @change="onGridChange($event,'thesis')">
								<uni-grid-item v-for="(item,index) in outcomes.thesis" :index="index" :key="index">
									<view class="grid-item-box">
										<text class="text">{{item.title}}</text>
									</view>
								</uni-grid-item>
							</uni-grid>
						</view>
					</view>
					<!-- 计划执行记录 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">计划执行记录</view>
						<view>
							<uni-grid :column="3" :square="false" :highlight="false" @change="onGridChange($event,'doplans')">
								<uni-grid-item v-for="(item,index) in outcomes.doplans" :index="index" :key="index">
									<view class="grid-item-box">
										<text class="text">{{item.title}}</text>
									</view>
								</uni-grid-item>
							</uni-grid>
						</view>
					</view>
					<!-- 经营管理情况 -->
					<view class="outcome-row mar-B20">
						<view class="text-C font-wb color-73a9fa mar-B20">经营管理情况</view>
						<view>
							<uni-grid :column="3" :square="false" :highlight="false" @change="onGridChange($event,'business')">
								<uni-grid-item v-for="(item,index) in outcomes.business" :index="index" :key="index">
									<view class="grid-item-box">
										<text class="text">{{item.title}}</text>
									</view>
								</uni-grid-item>
							</uni-grid>
						</view>
					</view>
				</view>
				<view v-if="tabType == 'member'" class="list-content" :style="'height:'+lHeight+'px;'">
					<view class="member-list">
						<view class="member-item" v-for="item in members">
							<view class="member-avatar"><image :src="item.avatar"></image></view>
							<view class="member-info">{{item.realname}}</view>
						</view>
					</view>
				</view>
				<view v-if="tabType == 'task'" class="list-content" :style="'height:'+lHeight+'px;'">
					<view class="task-list">
						<view class="task-item" v-for="item in tasks">
							<view class="task-cover"><image :src="item.cover"></image></view>
							<view class="task-info">
								<view class="mar-B5">{{item.title}}</view>
								<view class="color-73a9fa font-s12">{{item.tag}}</view>
							</view>
						</view>
					</view>
				</view>
				<view v-if="tabType == 'album'" class="album-list dis-flex" :style="'max-height:'+lHeight+'px;'">
					<template v-if="albums.length>0">
						<view class="album-item" v-for="(item,i) in albums">
							<view class="album-pic dis-flex-hv">
								<image :src="item.pic" mode="aspectFit" @click="onPreviewImage(i,albums,false)"></image>
							</view>
						</view>
					</template>
					<view v-else class="mar-L20 mar-R20">
						暂无相册
					</view>
				</view>
				<view v-if="tabType == 'cuiqu'" class="list-content" :style="'height:'+lHeight+'px;'">
					<view class="cuiqu-item" v-for="(item,i) in cuiqus">
						<view class="cuiqu-content bg-fed912">
							{{item.content}}
							<text class="cuiqu-rw" v-if="item.iswrite>0" @click="onrwChange(i)">
								<i class="gjjd-iconfont icon-yanjing"></i>
							</text>
							<text class="cuiqu-rw" v-else @click="onrwChange(i)">
								<i class="gjjd-iconfont icon-bianji"></i>
							</text>
						</view>
						<view class="cuiqu-answer" v-if="item.iswrite>0">
							<view class="cuiqu-textarea">
								<textarea class="cuiqu-input" v-model="cuiquContent" placeholder="请用10-500字回答"></textarea>
								<text class="cuiqu-send" @click.stop="onCuiquSubmit(item.id)">提交</text>
							</view>
							<view class="cuiqu-pics">
								<view class="cuiqu-img" v-for="(pic,index) in cuiquPics" :class="{'mar-right15':index==0 || (index>0 && (index+1)%3)>0}">
									<image :src="pic.url"></image>
								</view>
								<view v-if="cuiquPics.length<3" class="cuiqu-upload" @click="onCuiquUpload">
									<view class="text-C">
										<view><i class="gjjd-iconfont icon-xinzeng font-s26"></i></view>
										<view>添加图片</view>
									</view>
								</view>
							</view>
						</view>
						<view v-else>
							<view class="cuiqu-answer border-b-e0" v-for="ans in item.list">
								<view class="cuiqu-textarea">
									{{ans.content}}
								</view>
								<view class="cuiqu-pics">
									<view class="cuiqu-img" v-for="(pic,index) in ans.pics" :class="{'mar-right15':index==0 || (index>0 && (index+1)%3)>0}">
										<image :src="pic"></image>
									</view>
								</view>
							</view>
						</view>
					</view>
				</view>
				<view v-if="tabType == 'share'" class="list-content" :style="'height:'+lHeight+'px;'">
					<template v-if="comments.length>0">
						<view class="share-item mar-B20" v-for="(item,i) in comments">
							<view class="share-avatar">
								<image v-if="item.avatar" :src="item.avatar"></image>
								<image v-else src="/static/images/logo.jpg"></image>
							</view>
							<view class="share-info">
								<view class="dis-flex-hsb mar-B10" @click.stop="onReply(i,-1)">
									<view>
										<view>{{item.realname}}</view>
										<view class="color-92 font-s14">{{item.times}}</view>
									</view>
									<view class="dis-flex">
										<uni-rate :size="18" :value="item.rates" @change="onRateChange($event,item.id)"></uni-rate>
										<text class="font-s14 mar-L10">{{item.rates}}</text>
									</view>
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
								<view v-else class="share-content mar-B20">
									{{item.text_content}}
									<view class="share-reply-btn" @click.stop="onReply(i,-1)"><i class="gjjd-iconfont icon-xiaoxi"></i></view>
								</view>
								<view class="share-replys">
									<view v-for="(ritem,ri) in item.replys" class="reply-item mar-B10" @click.stop="onReply(i,ri)">
										<text>{{ritem.realname}}</text>
										<text v-if="ritem.reply_id!=item.parent_id">回复{{ritem.reply_user}}:</text>
										<text v-else>回复:</text>
										<text>{{ritem.text_content}}</text>
									</view>
								</view>
							</view>
						</view>
					</template>
					<view v-else class="mar-L20 mar-R20">
						暂无讨论
					</view>
					<view class="share-write">
						<i class="gjjd-iconfont icon-xinzeng font-s40" @click.stop="onReply(-1,-1)"></i>
					</view>
				</view>
			</view>
		</view>
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
	import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip:false,
				bHeight:600,
				lHeight:600,
				tabs:[
					{title:'简介',type:'intro',icon:''},
					{title:'成果',type:'outcome',icon:''},
					{title:'成员',type:'member',icon:''},
					{title:'任务',type:'task',icon:''},
					{title:'相册',type:'album',icon:''},
					{title:'萃取',type:'cuiqu',icon:''},
					{title:'讨论',type:'share',icon:''}
				],
				tabIndex:0,
				tabType:'intro',
				id:0,
				master:{},
				detail:{},
				outcomes:{patents:[],rewards:[],experts:[],files:[],thesis:[],doplans:[],business:[]},
				isMember:0,
				members:[],
				tasks:[],
				albums:[],
				cuiqus:[],
				cuiIndex:-1,
				cuiquContent:'',
				cuiquPics:[],
				commentType:0,
				commentIndex:-1,
				commentreIndex:-1,
				commentReply:'',
				isKeyboard:0,
				comments:[],
				commentPage:1
			}
		},
		onLoad(options) {
			console.log("team detail",options)
			this.bHeight = this.$winHeight - 180;
			this.lHeight = this.bHeight - 20 - 44 - 30 - 30 - 20;
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
		methods: {
			toBack(){
				uni.navigateBack();
			},
			onTabs(i){
				if(this.loading) return;
				console.log('i='+i);
				if(i != this.tabIndex){
					this.tabIndex = i;
					this.tabType = this.tabs[i].type;
					if(this.tabType == 'share'){
						this.getComments();
					}
				}
			},
			toPingfen(){
				navTo("/pages/team/pingfen",{id:this.id});
			},
			async getData(){
				let res = await request({
					method:'POST',
					data:{id:this.id},
					url:"/appteam/detail"
				});
				console.log('team list',res);
				if(res.code){
					let msg = '请求失败';
					if(res?.msg && res.msg){
						msg = res.msg;
					}
					showMsg('this',{title:msg})
				}
				if(res?.data?.master){
					this.master = res.data.master;
				}
				if(res?.data?.detail){
					this.detail = res.data.detail;
				}
				if(res?.data?.outcomes){
					this.outcomes = res.data.outcomes;
				}
				if(res?.data?.isMember){
					this.isMember = res.data.isMember;
				}
				if(res?.data?.members){
					this.members = res.data.members;
				}
				if(res?.data?.tasks){
					this.tasks = res.data.tasks;
				}
				if(res?.data?.albums){
					this.albums = res.data.albums;
				}
				if(res?.data?.cuiqus){
					this.cuiqus = res.data.cuiqus;
				}
			},
			onPreviewImage(i,lists=[],isarray=true){
				if(lists.length == 0) return;
				let urls = isarray?lists:[];
				if(!isarray){
					urls = [];
					for(let j in lists){
						urls.push(lists[j].pic);
					}
				}
				uni.previewImage({
					current:i,
					urls:urls
				});
			},
			onGridChange(e){
				console.log('onGridChange',e);
			},
			rwcuiquInit(){
				this.cuiqus[this.cuiIndex].iswrite = 0;
				this.cuiquPics = [];
				this.cuiquContent = '';
				this.cuiIndex = -1;
			},
			onrwChange(i){
				if(this.cuiqus[i].iswrite){
					this.rwcuiquInit();
				}else{
					this.cuiIndex = i;
					this.cuiqus[i].iswrite = 1;
				}
			},
			async onCuiquUpload(e){
				console.log('onCuiquUpload',e);
				//this.cuiquPics.push({pic:'',url:'https://cdn.app.gjjd168.com/'});
				let res = await uploads({limit:1},this);
				console.log('pic',res);
				if(res[0]){
					this.cuiquPics.push({pic:res[0],url:'https://cdn.app.gjjd168.com/'+res[0]});
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
			async onCuiquSubmit(id=0){
				let pics = [];
				if(this.cuiquPics.length>0){
					for(let i in this.cuiquPics){
						pics.push(this.cuiquPics[i].pic);
					}
				}
				let param = {
					id,
					master_id:this.id,
					content:this.cuiquContent,
					pics,
				};
				let res = await request({
					method:'POST',
					data:param,
					url:"/appteam/cuiqusave"
				});
				console.log("cuiqu send",res);
				showMsg("this",{title:res.msg});
				if(res?.code){
					return;
				}
				if(res?.data?.info){
					this.cuiqus[this.cuiIndex].iswrite = 0;
					this.cuiqus[this.cuiIndex].list.push(res.data.info);
					this.rwcuiquInit();
				}
			},
			async commentPic(){
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
			async getComments(flag=false){
				if(flag){
					this.comments = [];
					this.commentPage = 1;
				}
				let res = await request({
					method:'POST',
					data:{types:this.commentType,page:this.commentPage,tag:'master',tag_id:this.id},
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
					tag:'master',
					tag_id:this.id
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
			}
		}
	}
</script>

<style>
	page{
		background-color: blue;
	}
	.navs{
		position: fixed;
		left: 20rpx;
		right: 20rpx;
		z-index: 9999;
		height: 40px;
		line-height: 40px;
	}
	.top{
		height: 200px;
		background-color: #ddd;
		overflow: hidden;
		position: relative;
	}
	.top-bg{
		position: absolute;
		z-index: 0;
		width: 750rpx;
		height: 200px;
	}
	.pinfen{
		position: absolute;
		z-index: 99;
		bottom: 60px;
		right: 30rpx;
		width: 120rpx;
		height: 120rpx;
		font-size: 12px;
		text-align: center;
		color: #fff;
	}
	.member{
		max-width: 180rpx;
		height: 30px;
		overflow-x: auto;
	}
	.member image{
		width: 50rpx;
		height: 50rpx;
		border-radius: 25rpx;
		flex-shrink: 0;
	}
	.body{
		position: fixed;
		top: 180px;
		left: 0;
		right: 0;
		z-index: 99;
		height: 120px;
		background-color: #f8f8f8;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
		/* overflow-y:auto; */
	}
	.body-top{
		padding: 10px;
		background-color: #fff;
		border-top-left-radius: 20rpx;
		border-top-right-radius: 20rpx;
	}
	.body-list{
		margin-top: 10px;
	}
	.tabs{
		height: 44px;
		justify-content: start;
		background-color: #fff;
	}
	.tabs-item{
		padding: 5px;
	}
	.tabs-active{
		width: 60%;
		height: 4px;
		background-color: #73a9fa;
		border-radius: 2px;
	}
	.list-content{
		padding: 0 20rpx;
		overflow-y: auto;
	}
	.swiper{
		width: 710rpx;
		height: 200px;
	}
	.swiper-item{
		width: 710rpx;
	}
	.swiper-item image{
		width: 100%;
	}
	.jianjie{
		max-height: 90px;
		overflow: hidden;
	}
	.outcome-row{
		background-color: #fff;
		border-radius: 8rpx;
		padding: 10px 5px;
	}
	.expert-row{
		display: flex;
		align-items: center;
		justify-content: space-around;
	}
	.expert-avatar{
		width: 120rpx;
		height: 120rpx;
	}
	.expert-avatar image{
		width: 120rpx;
		height: 120rpx;
		border-radius: 60rpx;
		background-color: #ddd;
	}
	.expert-info{
		flex:1;
		padding: 5px;
	}
	.member-list{
		
	}
	.member-item{
		display: flex;
		align-items: center;
		padding: 10px 0;
		border-bottom:1rpx solid #e0e0e0;
	}
	.member-avatar{
		width: 120rpx;
		height: 120rpx;
	}
	.member-avatar image{
		width: 120rpx;
		height: 120rpx;
		background-color: #ddd;
		border-radius: 60rpx;
	}
	.member-info{
		flex: 1;
		padding: 5px;
	}
	.task-list{
		
	}
	.task-item{
		display: flex;
		padding: 10px 0;
		border-bottom:1rpx solid #e0e0e0;
	}
	.task-cover{
		width: 120rpx;
		height: 120rpx;
	}
	.task-cover image{
		width: 120rpx;
		height: 120rpx;
		background-color: #ddd;
		border-radius: 10rpx;
	}
	.task-info{
		flex: 1;
		padding: 5px;
	}
	.album-list{
		width: 750rpx;
		align-items: start;
		justify-content: start;
		flex-wrap: wrap;
		overflow-y: auto;
	}
	.album-item{
		width: 230rpx;
		height: 230rpx;
		margin-bottom: 10px;
		padding: 0 10rpx;
	}
	.album-pic{
		width: 230rpx;
		height: 230rpx;
		background-color: #fff;
		border-radius: 12rpx;
	}
	.album-pic image{
		width: 200rpx;
		height: 200rpx;
	}
	.cuiqu-item{
		
	}
	.cuiqu-content{
		margin-bottom: 5px;
		padding: 5px 0;
		text-align: center;
		position: relative;
	}
	.cuiqu-rw{
		position: absolute;
		bottom: 0;
		right: 5px;
		z-index: 99;
	}
	.cuiqu-answer{
		padding: 5px;
		background-color: #fff ;
	}
	.cuiqu-textarea{
		position: relative;
	}
	.cuiqu-input{
		width: 690rpx;
		border: 1rpx solid #e0e0e0;
		border-radius: 6rpx;
		margin-bottom: 4px;
	}
	.cuiqu-send{
		position: absolute;
		bottom: 5px;
		right: 10rpx;
		z-index: 99;
		padding: 2px 12rpx;
		border-radius: 8rpx;
		background-color: #73a9fa;
		color: #fff;
		font-size: 14px;
	}
	.cuiqu-pics{
		display: flex;
		align-items: flex-end;
		flex-wrap: wrap;
	}
	.cuiqu-img{
		width: 220rpx;
		height: 220rpx;
		margin-bottom: 5px;
		flex-shrink: 0;/*防止项目缩小*/
	}
	.cuiqu-img image{
		width: 220rpx;
		height: 220rpx;
		background-color: #e0e0e0;
		border-radius: 8rpx;
	}
	.cuiqu-upload{
		width: 160rpx;
		height: 160rpx;
		margin-bottom: 5px;
		display: flex;
		align-items: center;
		justify-content: center;
		border: 1rpx solid #e0e0e0;
	}
	.share-item{
		display: flex;
		padding: 5px 0;
		border-bottom: 1rpx solid #e0e0e0;
	}
	.share-avatar{
		width: 80rpx;
		height: 80rpx;
	}
	.share-avatar image{
		width: 80rpx;
		height: 80rpx;
		background-color: #e0e0e0;
		border-radius: 40rpx;
	}
	.share-info{
		flex: 1;
		padding: 4px 0;
		margin-left: 20rpx;
	}
	.share-content{
		position: relative;
		padding: 5px 0;
	}
	.share-write{
		position: fixed;
		bottom: 60px;
		right: 30px;
		z-index: 999;
	}
	.share-reply-btn{
		position: absolute;
		bottom: -5px;
		right: 10rpx;
		z-index: 99;
	}
	.reply-item{
		margin-right: 40rpx;
		padding: 10px 10rpx;
		background-color: #e0e0e0;
		border-radius: 8rpx;
		color: #626262;
		font-size: 12px;
	}
	.grid-item-box {
		flex: 1;
		/* #ifndef APP-NVUE */
		display: flex;
		/* #endif */
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 15px 0;
	}
	.mar-right15{
		margin-right: 15rpx;
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
</style>
