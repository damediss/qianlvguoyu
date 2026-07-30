<template>
	<view class="content bg-f8">
		<view class="header bg-ff border-b-ee" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-vc dis-flex-hsb">
				<view class="header-back dis-flex-vc" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20 color-33"></i>
				</view>
				<view class="header-title font-s18 font-wb color-33 text-C">社群问答提交</view>
				<view class="header-placeholder"></view>
			</view>
		</view>

		<view class="body" :style="'padding-top:' + (50 + $statusBarHeight + 10) + 'px;'">
			<view class="form-section mar-B40">
				<text class="form-label font-s15 color-33 mar-B20 pad-T10">回答内容:</text>
				<textarea class="answer-textarea bg-ff radius-16 pad-25 line-h-18 w-full-percent"
					v-model="answerContent" placeholder="请输入您的回答..." :maxlength="500"></textarea>
			</view>

			<view class="submit-btn bg-00b6ff radius-32 text-C transition-fast w-full-percent pad-20"
				@click="submitAnswer">
				<text class="font-s16 font-wb color-ff">提交</text>
			</view>
		</view>
	</view>
</template>

<script>
import { submitQAReply } from '@/api/competition';
import { useUserStore } from '@/store/user';
import { useCompetitionStore } from '@/store/competition';
import { isSuccess, showError } from '@/utils/response';

export default {
	setup() {
		const userStore = useUserStore()
		const competitionStore = useCompetitionStore()
		return { userStore, competitionStore }
	},
	data() {
		return {
			qaId: '',
			qaTitle: '',
			answerContent: '',
			submitting: false
		}
	},
	onLoad(options) {
		this.qaId = options.id || '';
		this.qaTitle = decodeURIComponent(options.title || '');
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		async submitAnswer() {
			if (!this.answerContent.trim()) {
				uni.showToast({ title: '请输入回答内容', icon: 'none' });
				return;
			}
			
			if (!this.userStore.checkLogin()) {
				return;
			}

			if (this.submitting) return;
			this.submitting = true;
			uni.showLoading({ title: '提交中...', mask: true });

			try {
				const res = await submitQAReply({
					wendaId: parseInt(this.qaId),
					userId: this.userStore.userId,
					content: this.answerContent
				});

				uni.hideLoading();

				if (isSuccess(res)) {
					uni.showToast({ title: '提交成功', icon: 'success', duration: 1500 });
					setTimeout(() => uni.navigateBack(), 1500);
				} else {
					showError(res, '提交失败');
					this.submitting = false;
				}
			} catch (error) {
				uni.hideLoading();
				console.error('【社区问答】提交失败：', error);
				uni.showToast({ title: '网络错误，请稍后重试', icon: 'none' });
				this.submitting = false;
			}
		}
	}
}
</script>

<style scoped>
.content {
	min-height: 100vh;
}

.header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 9999;
	box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.05);
}

.header-box {
	height: 50px;
	padding: 0 30rpx;
}

.header-back {
	width: 60rpx;
	height: 60rpx;
}

.header-title {
	flex: 1;
}

.header-placeholder {
	width: 60rpx;
}

.body {
	padding: 20rpx;
}

.form-label {
	display: block;
}

.answer-textarea {
	min-height: 300rpx;
	font-size: 30rpx;
	box-sizing: border-box;
	border: 1rpx solid #e0e0e0;
}

.submit-btn {
	box-shadow: 0 4rpx 12rpx rgba(0, 182, 255, 0.3);
	box-sizing: border-box;
}

.submit-btn:active {
	transform: scale(0.98);
	opacity: 0.9;
}
</style>