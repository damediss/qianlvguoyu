<template>
	<view class="content">
		<!-- 统计 -->
		<view class="huizong-box">
			<view class="huizong-search bg-292931 dis-flex-hv" :style="'padding-top:'+$statusBarHeight+'px;'">
				<view class="huizong-search-box color-ff">
					<text class="huizong-search-icon gjjd-iconfont icon-sousuobeifen2"></text>
					<marquee behavior="scroll" direction="left" scrollamount="2">南宁市,南南铝,大型企业等</marquee>
				</view>
			</view>
			<view style="height: 50px;"><!--huizong-search占位符--></view>
			<view class="dis-flex-hsb mar-B10 huizong-lr">
				<view class="huizong-lr-item bg-3b82f6 dis-flex-hv">
					<view>
						<view class="mar-B10">产业工人数量:</view>
						<view class="font-s28 font-wb">1000人</view>
					</view>
				</view>
				<view class="huizong-lr-item bg-50e3c2 dis-flex-hv">
					<view>
						<view class="font-s12">技能人数占产业工人比例:</view>
						<view class="font-wb">
							<qiun-data-charts type="pie" :chartData="bowuguan.chartPie1"/>
						</view>
					</view>
				</view>
			</view>
			<view class="dis-flex-hsb mar-B10 huizong-lr">
				<view class="huizong-lr-item bg-3b82f6 dis-flex-hv">
					<view>
						<view class="mar-B10">产业工人数量:</view>
						<view class="font-s28 font-wb">1000人</view>
					</view>
				</view>
				<view class="huizong-lr-item bg-50e3c2 dis-flex-hv">
					<view>
						<view class="font-s12">技能人数占产业工人比例:</view>
						<view class="font-wb">
							<qiun-data-charts type="pie" :chartData="bowuguan.chartPie1"/>
						</view>
					</view>
				</view>
			</view>
			<view class="dis-flex-hsb mar-B10 huizong-lr">
				<view class="huizong-lr-item bg-3b82f6 dis-flex-hv">
					<view>
						<view class="mar-B10">产业工人数量:</view>
						<view class="font-s28 font-wb">1000人</view>
					</view>
				</view>
				<view class="huizong-lr-item bg-50e3c2 dis-flex-hv">
					<view>
						<view class="font-s12">技能人数占产业工人比例:</view>
						<view class="font-wb">
							<qiun-data-charts type="pie" :chartData="bowuguan.chartPie1"/>
						</view>
					</view>
				</view>
			</view>
			<view class="mar-B10">
				<view>技能人才历年变化趋势</view>
				<view class="bg-ff pad-10 radius-12">
					<qiun-data-charts type="line" :chartData="bowuguan.chartLine1"/>
				</view>
			</view>
			<view>产业链职业工种大全及技能人才数量</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00">全部职业工种 <text class="mar-L20">总计86个</text></view>
				<view class="dis-flex-hsb pad-L20 pad-R20">
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">工种数量</view>
						<view class="color-00">86</view>
					</view>
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">产业工人</view>
						<view class="color-00">31</view>
					</view>
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">技能人才</view>
						<view class="color-00">21</view>
					</view>
				</view>
				<view>
					<qiun-data-charts type="column" :chartData="bowuguan.chartColumn1"/>
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00">可认职业工种 <text class="mar-L20">总计86个</text></view>
				<view class="dis-flex-hsb pad-L20 pad-R20">
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">工种数量</view>
						<view class="color-00">86</view>
					</view>
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">产业工人</view>
						<view class="color-00">31</view>
					</view>
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">技能人才</view>
						<view class="color-00">21</view>
					</view>
				</view>
				<view>
					<qiun-data-charts type="column" :chartData="bowuguan.chartColumn2"/>
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00">新开发职业工种 <text class="mar-L20">总计61个</text></view>
				<view class="dis-flex-hsb pad-L20 pad-R20">
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">工种数量</view>
						<view class="color-00">61</view>
					</view>
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">产业工人</view>
						<view class="color-00">40</view>
					</view>
					<view style="width: 200rpx;border-radius: 6rpx;padding: 10px 0;" class="bg-e0 text-C">
						<view class="color-cc">技能人才</view>
						<view class="color-00">21</view>
					</view>
				</view>
				<view>
					<qiun-data-charts type="column" :chartData="bowuguan.chartColumn3"/>
				</view>
			</view>
			<view>产业链职业工种课程完善度</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00 mar-B10">可认定职业课程 <text class="mar-L20 color-0066ff tag-text">总计256门</text></view>
				<view class="dis-flex-hsb text-C mar-B20">
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">对应工种</view>
						<view class="color-00 font-s20 font-wb">65</view>
					</view>
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">课程数量</view>
						<view class="color-00 font-s20 font-wb">256</view>
					</view>
				</view>
				<view>
					<progress :percent="50" border-radius="1" stroke-width="3" />
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00 mar-B10">待开发课程 <text class="mar-L20 color-0066ff tag-text">总计72门</text></view>
				<view class="dis-flex-hsb text-C mar-B20">
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">对应工种</view>
						<view class="color-00 font-s20 font-wb">21</view>
					</view>
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">规划课程数量</view>
						<view class="color-00 font-s20 font-wb">72</view>
					</view>
				</view>
				<view>
					<progress :percent="30" border-radius="1" stroke-width="3" />
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00">各类工种-课程匹配度对比</view>
				<view>
					<qiun-data-charts type="column" :chartData="bowuguan.chartColumn4"/>
				</view>
			</view>
			<!-- 产业链工种题库完善度 -->
			<view>产业链工种题库完善度</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00 mar-B10">可认定职业工种题库 <text class="mar-L20 color-0066ff tag-text">总计218套</text></view>
				<view class="dis-flex-hsb text-C mar-B20">
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">对应工种</view>
						<view class="color-00 font-s20 font-wb">65</view>
					</view>
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">题库数量</view>
						<view class="color-00 font-s20 font-wb">256</view>
					</view>
				</view>
				<view>
					<progress :percent="80" border-radius="1" stroke-width="3" />
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00 mar-B10">待开发标准工种题库 <text class="mar-L20 color-0066ff tag-text">总计68套</text></view>
				<view class="dis-flex-hsb text-C mar-B20">
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">对应工种</view>
						<view class="color-00 font-s20 font-wb">21</view>
					</view>
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">规划题库数</view>
						<view class="color-00 font-s20 font-wb">68</view>
					</view>
				</view>
				<view>
					<progress :percent="40" stroke-width="3" />
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view>
					<qiun-data-charts type="pie" :chartData="bowuguan.chartPie2"/>
				</view>
			</view>
			<!-- 产业链工种实训平台配套情况 -->
			<view>产业链工种实训平台配套情况</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00 mar-B10">可认定职业工种实训平台 <text class="mar-L20 color-0066ff tag-text">总计62个</text></view>
				<view class="dis-flex-hsb text-C mar-B20">
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">对应工种数</view>
						<view class="color-00 font-s20 font-wb">62</view>
					</view>
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">平台数量</view>
						<view class="color-00 font-s20 font-wb">65</view>
					</view>
				</view>
				<view>
					<progress :percent="80" border-radius="1" stroke-width="3" />
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view class="color-00 mar-B10">待开发标准工种实训平台 <text class="mar-L20 color-0066ff tag-text">总计16个</text></view>
				<view class="dis-flex-hsb text-C mar-B20">
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">对应工种数</view>
						<view class="color-00 font-s20 font-wb">16</view>
					</view>
					<view class="w-300 bg-e0 pad-T20 pad-B20 radius-8">
						<view class="color-00">规划平台数</view>
						<view class="color-00 font-s20 font-wb">21</view>
					</view>
				</view>
				<view>
					<progress :percent="40" stroke-width="3" />
				</view>
			</view>
			<view class="mar-B10 bg-ff pad-20 radius-12">
				<view>
					<qiun-data-charts type="ring" :opts="{title:{name:'',color:'#2fc25b',fontSize:35},subtitle:{name:'',color:'#666666',fontSize:25},legend:{position: 'bottom'},extra:{ring:{ringWidth: 10,centerColor:'#fff'}}}" :chartData="bowuguan.chartsDataRing1"/>
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
				bowuguan:{
					chartPie1:{
						"series": [{
							"data": [
							{
								"name": "技能工人",
								"value": 50
							}, {
								"name": "非技能工人",
								"value": 90
							}
						  ]
						}]
					},
					chartLine1:{
						"categories": ["2021", "2022", "2023", "2024", "2025"],
						"series": [{
							"name": "技能人数",
							"data": [8, 25, 37, 4, 20]
						}, {
							"name": "高技人才",
							"data": [40, 65, 100, 44, 68]
						}, {
							"name": "高技领军人才",
							"data": [80, 95, 150, 112, 132]
						}]
					},
					chartColumn1:{
						"categories": ["全部工种"],
						"series": [{
							"name": "产业人工",
							"data": [31]
						}, {
							"name": "技能人才",
							"data": [21]
						}]
					},
					chartColumn2:{
						"categories": ["可认定工种"],
						"series": [{
							"name": "产业人工",
							"data": [31]
						}, {
							"name": "技能人才",
							"data": [21]
						}]
					},
					chartColumn3:{
						"categories": ["待开发工种"],
						"series": [{
							"name": "产业人工",
							"data": [40]
						}, {
							"name": "技能人才",
							"data": [11]
						}]
					},
					chartColumn4:{
						"categories": [""],
						"series": [{
							"name": "可认定课程",
							"data": [40]
						}, {
							"name": "待开发课程",
							"data": [11]
						}]
					},
					chartPie2:{
						"series": [{
							"data": [
							{
								"name": "可认定课程",
								"value": 90
							}, {
								"name": "待开发课程",
								"value": 50
							}
						  ]
						}]
					},
					chartsDataRing1:{
						"series": [{
							"data": [
							{
								"name": "可认定",
								"value": 50
							},
							{
								"name": "待开发",
								"value": 30
							}
						  ]
						}]
					}
				}
			}
		},
		methods: {
			
		}
	}
</script>

<style>
	page{
		background-color: #292931;
	}
	.huizong-box{
		width: 710rpx;
		padding-left: 20rpx;
		padding-right: 20rpx;
		padding-bottom: 5px;
	}
	.huizong-search{;
		height: 50px;
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 99;
		padding-left: 20rpx;
		padding-right: 20rpx;
	}
	.huizong-search-box{
		position: relative;
		width: 650rpx;
		height: 40px;
		line-height: 40px;
		border: 1rpx solid #e0e0e0;
		border-radius: 8rpx;
		padding-left: 60rpx
	}
	.huizong-search-icon{
		display: block;
		position: absolute;
		top: 0;
		left: 0;
		z-index: 99;
		width: 60rpx;
		height: 40px;
		line-height: 40px;
		text-align: center;
		font-size: 30px;
	}
	.huizong-lr{
		width: 710rpx;
		height: 300px;
	}
	.huizong-lr-item{
		width: 355rpx;
		text-align: center;
	}
	.tag-text{
		background-color: rgba(173, 200, 244, 0.5);
		padding: 2px 14rpx;
		border-radius: 8rpx;
		font-size: 12px;
	}
</style>