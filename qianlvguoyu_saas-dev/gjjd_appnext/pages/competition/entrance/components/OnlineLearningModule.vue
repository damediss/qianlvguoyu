<template>
	<view class="section">
		<view class="section-title font-s16 font-wb color-00">线上学习</view>

		<view class="card-grid">
			<view class="card-item dis-flex-hv small-card green-bg-0 shadow-sm h-75 radius-32"
				@click="handleCourseware">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/train.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-547e71">阅读课件</text>
			</view>

			<view class="card-item dis-flex-hv large-card blue-bg-2 shadow-sm h-75 radius-32" @click="handleArticle">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/train.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-ff">阅读文章</text>
			</view>

			<view class="card-item dis-flex-hv small-card blue-bg-3 shadow-sm h-75 radius-32"
				@click="handleWrongQuestion">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/train.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-ff">错题学习</text>
			</view>

			<view class="card-item dis-flex-hv small-card blue-bg-0 shadow-sm h-75 radius-32" @click="handleFragment">
				<view class="card-pattern"></view>
				<view class="card-icon">
					<image src="/static/images/train.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-00bfff">碎片化学习</text>
			</view>

			<view class="card-item dis-flex-hv full-width-card orange-bg-0 shadow-sm h-75 radius-32"
				@click="handleMockExam">
				<view class="card-pattern"></view>
				<view class="card-icon dis-flex-hv">
					<image src="/static/images/s-4.png" style="width: 25px; height: 25px;"></image>
				</view>
				<text class="card-text font-s16 font-wb color-ff">模拟考试</text>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'OnlineLearningModule',
	methods: {
		handleCourseware() {
			this.$emit('show-rules', 'courseware');
		},
		handleArticle() {
			this.$emit('show-rules', 'article');
		},
		handleWrongQuestion() {
			this.$emit('show-rules', 'wrongQuestion');
		},
		handleFragment() {
			this.$emit('show-rules', 'fragment');
		},
		handleMockExam() {
			this.$emit('show-rules', 'mockExam');
		}
	}
}
</script>

<style scoped>
@import '../styles/cards.css';

.section {
	margin-bottom: 30rpx;
}

.section-title {
	padding: 0 0 20rpx 0;
	margin-bottom: 10rpx;
}
</style>