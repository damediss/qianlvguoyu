<template>
	<view>
		<view style="padding: 0 20rpx;">
			<qiun-data-charts type="line" :chartData="chartLine"/>
		</view>
		<view style="padding: 0 20rpx;">
			<qiun-data-charts type="column" :chartData="chartColumn"/>
		</view>
		<view style="padding: 0 20rpx;">
			<qiun-data-charts type="column" :chartData="chartColumn" :eopts="columneopts" :echartsApp="true"/>
		</view>
		<view style="padding: 0 20rpx;">
			<qiun-data-charts type="pie" :chartData="chartPie"/>
		</view>
		<view style="padding: 0 20rpx;">
			<qiun-data-charts type="area" :chartData="chartArea"/>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				chartLine:{
					"categories": ["2016", "2017", "2018", "2019", "2020", "2021"],
					"series": [{
						"name": "成交量A",
						"data": [35, 8, 25, 37, 4, 20]
					}, {
						"name": "成交量B",
						"data": [70, 40, 65, 100, 44, 68]
					}, {
						"name": "成交量C",
						"data": [100, 80, 95, 150, 112, 132]
					}]
				},
				chartColumn:{
					"categories": ["2016", "2017", "2018", "2019", "2020", "2021"],
					"series": [{
						"name": "目标值",
						"data": [35, 36, 31, 33, 13, 34]
					}, {
						"name": "完成量",
						"data": [18, 27, 21, 24, 6, 28]
					}]
				},
				chartPie:{
					"series": [{
						"data": [
						{
							"name": "一班",
							"value": 50
						}, {
							"name": "二班",
							"value": 30
						}, {
							"name": "三班",
							"value": 20
						}, {
							"name": "四班",
							"value": 18
						}, {
							"name": "五班",
							"value": 8
						}
					  ]
					}]
				},
				chartArea:{
					"categories": ["2016", "2017", "2018", "2019", "2020", "2021"],
					"series": [{
						"name": "成交量A",
						"data": [35, 8, 25, 37, 4, 20]
					}, {
						"name": "成交量B",
						"data": [70, 40, 65, 100, 44, 68]
					}, {
						"name": "成交量C",
						"data": [100, 80, 95, 150, 112, 132]
					}]
				},
				columneopts:{
					grid: {
						left: '3%',
						right: '4%',
						bottom: 30,
						top:10,
						containLabel: true
					},
					xAxis: {
						type: 'value',
						boundaryGap: [0, 0.01],
						axisLine: {
							show: false
						},
						axisTick: {
							show: false
						}
					},
					yAxis: {
						type: 'category',
						data: []
					},
					//series模板，会覆盖至chartData中的series中的每一个数组内
					seriesTemplate:{
						"label": {
							"show": true,
							"color": "#666666",
							"position": 'right'
						}
					}
				}
			}
		},
		methods: {
			//使用教程
			//https://ext.dcloud.net.cn/plugin?id=271
			//https://gitee.com/uCharts/uCharts
		}
	}
</script>

<style>

</style>