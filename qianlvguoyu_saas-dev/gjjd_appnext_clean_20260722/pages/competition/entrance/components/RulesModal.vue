<template>
	<view class="modal-mask dis-flex-hv mask-dark" v-if="visible" @click="handleClose">
		<view class="modal-content bg-ff radius-16" @click.stop>
			<view class="modal-title font-s18 font-wb color-33">{{ title }}</view>
			<view class="modal-body">
				<view class="rule-text font-s15 color-66" v-for="(line, index) in contentLines" :key="index">
					{{ line }}
				</view>
			</view>
			<view class="modal-footer dis-flex">
				<view class="confirm-btn bg-00b6ff radius-8" @click="handleConfirm">
					<text class="font-s16 color-ff">{{ confirmText }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
export default {
	name: 'RulesModal',
	props: {
		visible: {
			type: Boolean,
			default: false
		},
		title: {
			type: String,
			default: '提示'
		},
		content: {
			type: String,
			default: ''
		},
		confirmText: {
			type: String,
			default: '知道了'
		}
	},
	computed: {
		contentLines() {
			if (!this.content) return [];
			return this.content.split('\n').filter(line => line.trim());
		}
	},
	methods: {
		handleClose() {
			this.$emit('close');
		},
		handleConfirm() {
			this.$emit('confirm');
		}
	}
}
</script>

<style scoped>
.modal-mask {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 10000;
	padding: 40rpx;
}

.modal-content {
	width: 100%;
	max-width: 550rpx;
	padding: 40rpx 30rpx 30rpx;
	animation: modalFadeIn 0.3s;
}

@keyframes modalFadeIn {
	from {
		opacity: 0;
		transform: scale(0.9);
	}

	to {
		opacity: 1;
		transform: scale(1);
	}
}

.modal-title {
	text-align: center;
	margin-bottom: 30rpx;
}

.modal-body {
	margin-bottom: 30rpx;
}

.rule-text {
	margin-bottom: 12rpx;
	line-height: 1.8;
}

.modal-footer {
	justify-content: flex-end;
	align-items: center;
}

.confirm-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	min-height: 64rpx;
	padding: 0 50rpx;
	transition: all 0.3s;
}

.confirm-btn:active {
	transform: scale(0.95);
	opacity: 0.8;
}
</style>