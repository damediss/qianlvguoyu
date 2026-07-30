<template>
	<view class="content">
		<view class="show-permission-tip" :style="'top:'+(50+$statusBarHeight)+'px;'" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机信息授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启，将无法使用上述功能。</view>
		</view>
		<!-- 导航栏 -->
		<view class="header bg-292931" :style="'padding-top:'+$statusBarHeight+'px;'">
			<view class="header-title">{{appTitle}}</view>
			<!-- Tabs -->
			<view class="tabs">
				<template v-for="(item,i) in tabs">
					<view class="tabs-item" @click="onTabs(i)">
						<view class="tabs-text" :class="{'tabs-active':i==currentTab}">{{item.title}}</view>
					</view>
				</template>
			</view>
		</view>
		<!-- height:'+($winHeight-($statusBarHeight+60+55+3+60))+'px; -->
		<view class="body" :style="'padding-top:'+($statusBarHeight+60+55+10)+'px;'">
			<view v-if="tabs[currentTab].tag=='tuijian'" class="tuijian">
				<!-- 轮播图 -->
				<view class="lunbo">
					<swiper v-if="homeData.lunbo.length>0" class="lunbo-swiper" circular :indicator-dots="true" :autoplay="true" :interval="2000" :duration="500">
						<swiper-item v-for="item in homeData.lunbo">
							<view class="lunbo-pic">
								<image :src="item.img"></image>
							</view>
						</swiper-item>
					</swiper>
				</view>
				<!-- 导航标签 -->
				<view class="navtag">
					<view class="navtag-item">
						<view><image src="/static/images/phn11.jpg"></image></view>
						<view class="text-C">技能认定</view>
					</view>
					<view class="navtag-item">
						<view><image src="/static/images/phn11.jpg"></image></view>
						<view class="text-C">在线学习</view>
					</view>
					<view class="navtag-item">
						<view><image src="/static/images/phn11.jpg"></image></view>
						<view class="text-C">技能大赛</view>
					</view>
					<view class="navtag-item">
						<view><image src="/static/images/phn11.jpg"></image></view>
						<view class="text-C">证书查询</view>
					</view>
				</view>
				<view class="row-box mar-B20" v-if="homeData?.article?.tuijian?.ren">
					<view class="color-FFDC06 mar-B10">{{homeData.article.tuijian.ren.ctitle}}</view>
					<template v-for="(item,i) in homeData.article.tuijian.ren.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view> 
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>
					</template>
				</view>
				<view class="row-box mar-B20" v-if="homeData?.article?.tuijian?.exammatch">
					<view class="color-FFDC06 mar-B10">{{homeData.article.tuijian.exammatch.ctitle}}</view>
					<template v-for="(item,i) in homeData.article.tuijian.exammatch.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view>
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>
					</template>
				</view>
			</view>
			<view v-if="tabs[currentTab].tag=='zhengce'">
				<view class="row-box mar-B20" v-if="homeData?.article?.zhengce">
					<view class="color-FFDC06 mar-B10">{{homeData.article.zhengce.ctitle}}</view>
					<!-- 列表 -->
					<template v-for="(item,i) in homeData.article.zhengce.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view>
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>
					</template>
				</view>
			</view>
			<view v-if="tabs[currentTab].tag=='news'">
				<view class="row-box mar-B20" v-if="homeData?.article?.news">
					<view class="color-FFDC06 mar-B10">{{homeData.article.news.ctitle}}</view>
					<!-- 列表 -->
					<template v-for="(item,i) in homeData.article.news.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view>
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>
					</template>
				</view>
			</view>
			<view v-if="tabs[currentTab].tag=='platform'">
				<view class="row-box mar-B20" v-if="homeData?.article?.platform">
					<view class="color-FFDC06 mar-B10">{{homeData.article.platform.ctitle}}</view>
					<!-- 列表 -->
					<template v-for="(item,i) in homeData.article.platform.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view>
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>
					</template>
				</view>
			</view>
			<view v-if="tabs[currentTab].tag=='share'">
				<view class="row-box mar-B20" v-if="homeData?.article?.share">
					<view class="color-FFDC06 mar-B10">{{homeData.article.share.ctitle}}</view>
					<!-- 列表 -->
					<template v-for="(item,i) in homeData.article.share.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view>
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>	
					</template>
				</view>
			</view>
			<view v-if="tabs[currentTab].tag=='liangdian'">
				<view class="row-box mar-B20" v-if="homeData?.article?.liangdian">
					<view class="color-FFDC06 mar-B10">{{homeData.article.liangdian.ctitle}}</view>
					<!-- 列表 -->
					<template v-for="(item,i) in homeData.article.liangdian.list">
						<view class="line-292931 mar-T10 mar-B10" v-if="i>0"></view>
						<view @click="toArticle(item.id)">
							<view class="mar-B10">{{item.title}}</view>
							<view class="font-s12 mar-B10">{{item.desc}}</view>
							<template v-if="item.img.length == 6">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-3" :src="item.img[3]"></image>
									<image class="img-news-3" :src="item.img[4]"></image>
									<image class="img-news-3" :src="item.img[5]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 5">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[3]"></image>
									<image class="img-news-2" :src="item.img[4]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 4">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
								<view class="dis-flex-hsb">
									<image class="img-news-2" :src="item.img[2]"></image>
									<image class="img-news-2" :src="item.img[3]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 3">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-3" :src="item.img[0]"></image>
									<image class="img-news-3" :src="item.img[1]"></image>
									<image class="img-news-3" :src="item.img[2]"></image>
								</view>
							</template>
							<template v-else-if="item.img.length == 2">
								<view class="dis-flex-hsb mar-B10">
									<image class="img-news-2" :src="item.img[0]"></image>
									<image class="img-news-2" :src="item.img[1]"></image>
								</view>
							</template>
							<template v-else>
								<image class="img-news-1 mar-B10" src="/static/images/balance-bg.png"></image>
							</template>
						</view>
					</template>
				</view>	
			</view>
		</view>
		<!-- 更新下载进度条 -->
		<view v-if="upgrade_progress" class="upgrade-progress" @touchmove.stop.prevent="()=>{}">
			<view style="background-color: #fff;padding: 20rpx;border-radius: 6rpx;">{{upgrade_progress}}</view>
		</view>
	</view>
</template>

<script>
	import {request,uploads,rectTo,navTo,showMsg,cameraPermission,checkUpgrade} from "@/utils";
	export default {
		data() {
			return {
				showpermissionTip:false,
				appTitle:'工匠基地平台',
				tabs:[
					{title:'发现',tag:'tuijian'},
					{title:'政策',tag:'zhengce'},
					{title:'新闻',tag:'news'},
					{title:'平台',tag:'platform'},
					{title:'分享',tag:'share'},
					{title:'亮点',tag:'liangdian'}
				],
				currentTab:0,
				homeData:{
					lunbo:[{img:'./static/images/logo.jpg'},{img:'./static/images/brushing.jpg'}],
					article:{}
				},
				upgrade_progress:''
			}
		},
		onLoad() {
			checkUpgrade({userId:0},this,(e)=>{
				console.log('***',e);
			});
			this.getTabsData();
		},
		onShow() {
			this.getPageData();
			if(this.upgrade_progress){
				uni.addInterceptor('switchTab',{
					invoke(e){
						//拦截前触发
						console.log('invoke',e)
						return false;
					},
					success(e){
						//成功回调拦截
						console.log('success',e)
						return false;
					}
				});
			}else{
				var tu=0;
				var timeId = setInterval(()=>{
					if(tu>10){
						clearInterval(timeId);
						tu=0;
					}else{
						tu++;
						if(this.upgrade_progress){
							clearInterval(timeId);
							tu=0;
							uni.addInterceptor('switchTab',{
								invoke(e){
									//拦截前触发
									console.log('invoke',e)
									return false;
								},
								success(e){
									//成功回调拦截
									console.log('success',e)
									return false;
								}
							});
						}
					}
				},1000);
			}
		},
		onPullDownRefresh() {
			setTimeout(function () {
				uni.stopPullDownRefresh();
			}, 1000);
		},
		onNavigationBarButtonTap(e){
			console.log(e);
			if(e.code && e.code == ''){
				//
			}
		},
		methods: {
			toSearch(){
				navTo('/pages/index/search');
			},
			scan(){
				cameraPermission((e)=>{
					if(e>0){
						uni.scanCode({
							onlyFromCamera: true,
								success: (res)=>{
									console.log('条码类型：' + res.scanType);
									console.log('条码内容：' + res.result);
									if(res.result){
									if(res.result.includes('/pages/promote/survey')){
										const formId = res.result.split('=')[1]
										uni.navigateTo({
											url: `/pages/promote/survey?formId=${formId}`
										});
									}else if(res.result.includes('/pages/user/kjexam')){
										uni.navigateTo({
											url:'/pages/user/kjexam'
										});
									}else if(res.result.includes('/#/')){
										let array = res.result.split('#');
										let path1 = array[1] && array[1].includes('pages/registration/detail?id=');
										let path2 = array[1] && array[1].includes('pages/signUp/self?id=');
										if(path1 || path2){
											let param = array[1].split('=');
											uni.navigateTo({
												url:'/pages/registration/detail?id='+param[1]
											});
										}
									}
								}
							}
						});
					}
				},this);
			},
			onTabs(i){
				if(i==this.currentTab) return;
				this.currentTab = i;
				this.currentScrollView = this.tabs[i].tag;
			},
			toMore(tag){
				return;
				navTo('/pages/index/category',{tag});
			},
			toArticle(id,tag){
				if(tag == 'share'){
					//
				}else{
					navTo('/pages/public/article',{article_id:id});
				}
			},
			async getTabsData(){
				let that = this;
				let res = await request({
					method:'POST',
					data:{},
					url:"/apphome/tabs"
				});
				console.log(res);
				if(res?.data?.tabs){
					this.tabs = res.data.tabs;
				}
			},
			async getPageData(){
				let that = this;
				let res = await request({
					method:'POST',
					data:{tag:this.tabs[this.currentTab].tag},
					url:"/apphome/home"
				});
				console.log(res);
				if(res?.data?.home){
					this.homeData = res.data.home;
				}
			}
		}
	}
</script>

<style>
	page{
		background-color: #292931;
	}
	.content{}
	.show-permission-tip{
		position:fixed;
		left:20rpx;
		right:20rpx;
		z-index:9999;
		background-color:rgba(0, 0, 0, 0.6);
		padding: 10rpx;
		color:#fff;
		border-radius:8rpx;
	}
	.upgrade-progress{
		position: fixed;
		top: 0;
		bottom: 0;
		left: 0;
		right: 0;
		z-index: 9999;
		background-color: rgba(0, 0, 0, 0.6);
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.header{
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 9999;
	}
	.header-title{
		height: 60px;
		line-height: 60px;
		text-align: center;
		background-color: #343440;
		color: #FFDC06;
		font-size: 18px;
		font-weight: bold;
	}
	.tabs{
		width: 710rpx;
		height: 50px;
		margin-top: 5px;
		margin-left: 20rpx;
		margin-right: 20rpx;
		/* overflow-x: auto; */
		display: flex;
		align-items: center;
		justify-content: space-between;
		flex-wrap: nowrap;/*不换行*/
		background-color: #343440;
	}
	.tabs-item{
		flex-shrink: 0;/*防止项目缩小*/
		padding-top: 5px;
		padding-bottom: 5px;
		padding-left: 10rpx;
		padding-right: 10rpx;
	}
	.tabs-text{
		background-color: #292931;
		border-radius: 8px;
		padding: 4px 8px;
		text-align: center;
		color: #fff;
	}
	.tabs-current{
		height: 4px;
		margin: 0 15rpx;
	}
	.tabs-active{
		color: #FFDC06;
	}
	.body{
		padding-left: 20rpx;
		padding-right: 20rpx;
		color: #fff;
		/* border-bottom: 1rpx solid #fff; */
		overflow-y: auto;
	}
	.lunbo{
		width: 710rpx;
		height: 160px;
		background-color: #343440;
		border-radius: 10rpx;
	}
	.lunbo-swiper{
		width: 710rpx;
		height: 160px;
	}
	.lunbo-pic{
		height: 160px;
	}
	.lunbo-pic image{
		width: 710rpx;
		height: 160px;
		border-radius: 10rpx;
	}
	.navtag{
		width: 710rpx;
		padding-top: 10px;
		padding-bottom: 10px;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}
	.navtag-item{
		width: 150rpx;
	}
	.navtag-item image{
		width: 150rpx;
		height: 150rpx;
		border-radius: 8rpx;
	}
	.row-box{
		width: 670rpx;
		padding: 10px 20rpx;
		background-color: #343440;
		border-radius: 12rpx;
	}
	.line-292931{
		height: 1px;
		background-color: #292931;
	}
	.imgage-fencai{
		width: 670rpx;
		height: 200px;
		border-radius: 12rpx;
	}
	.img-news-1{
		width: 670rpx;
		height: 120px;
		border-radius: 8rpx;
	}
	.img-news-2{
		width: 330rpx;
		height: 90px;
		border-radius: 8rpx;
	}
	.img-news-3{
		width: 200rpx;
		height: 90px;
		border-radius: 8rpx;
	}
</style>