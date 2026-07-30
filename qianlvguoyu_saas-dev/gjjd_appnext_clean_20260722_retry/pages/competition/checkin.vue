<template>
	<view class="content bg-f8">
		<view class="header bg-ff" :style="'padding-top:' + $statusBarHeight + 'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv w-60" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb text-C" @longpress="handleResetCheckin">签到</view>
				<view class="header-placeholder w-60"></view>
			</view>
		</view>

		<view class="body" :style="'margin-top:' + (50 + $statusBarHeight) + 'px;'">
			<view class="checkin-info dis-flex">
				<view class="info-item dis-flex">
					<text class="info-label font-s15 color-66">您已连续签到</text>
					<text class="info-value font-s20 font-wb color-red">{{ continuousDays }}</text>
					<text class="info-unit font-s15 color-66">天</text>
				</view>
				<view class="info-item dis-flex">
					<text class="info-label font-s15 color-66">您有</text>
					<text class="info-value font-s20 font-wb color-ff9f40">{{ points }}</text>
					<text class="info-unit font-s15 color-66">积分</text>
				</view>
			</view>

			<view class="calendar-container bg-ff shadow-sm radius-16">
				<view class="month-selector dis-flex-hsb">
					<view class="month-btn" @click="changeMonth(-1)">
						<text class="font-s16 color-66">上个月</text>
					</view>
					<view class="current-month font-s18 font-wb color-33 text-C">{{ currentYear }}年{{ currentMonth }}月
					</view>
					<view class="month-btn" @click="changeMonth(1)">
						<text class="font-s16 color-66">下个月</text>
					</view>
				</view>

				<view class="week-header">
					<view class="week-item font-s14 color-99">日</view>
					<view class="week-item font-s14 color-99">一</view>
					<view class="week-item font-s14 color-99">二</view>
					<view class="week-item font-s14 color-99">三</view>
					<view class="week-item font-s14 color-99">四</view>
					<view class="week-item font-s14 color-99">五</view>
					<view class="week-item font-s14 color-99">六</view>
				</view>

				<view class="date-grid">
					<view v-for="(day, index) in calendarDays" :key="index"
						class="date-item dis-flex-hv radius-full transition-all" :class="{
							'is-other-month': day.isOtherMonth,
							'is-today bg-50e3c2': day.isToday,
							'is-checked': day.isChecked
						}" @click="handleDateClick(day)">
						<text class="date-text font-s16"
							:class="day.isToday ? 'color-ff' : (day.isOtherMonth ? 'color-cc' : 'color-33')">
							{{ day.day }}
						</text>
					</view>
				</view>
			</view>

			<view class="checkin-btn-container" v-if="!hasCheckedToday">
				<view class="checkin-btn bg-00b6ff radius-32 shadow-md dis-flex-hv w-full h-100 transition-all"
					@click="handleCheckin">
					<text class="font-s18 font-wb color-ff">签到</text>
				</view>
			</view>

			<view class="checkin-records" v-if="checkinRecords.length > 0">
				<view class="records-title font-s16 font-wb color-33">签到记录</view>
				<view class="records-list dis-flex flex-column">
					<view class="record-item dis-flex bg-ff shadow-sm radius-16"
						v-for="(record, index) in checkinRecords" :key="index">
						<text class="record-text font-s15 color-66">您在</text>
						<text class="record-time font-s15 color-33">{{ record.time }}</text>
						<text class="record-text font-s15 color-66">进行了签到获得了</text>
						<text class="record-points font-s15 font-wb color-ff9f40">{{ record.points }}</text>
						<text class="record-text font-s15 color-66">积分</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import { checkIn, getCheckinStatistics, getCheckInHistory, resetTodayCheckin } from "@/api/competition";
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
			matchId: null,
			continuousDays: 0,
			points: 0,
			currentYear: 2025,
			currentMonth: 10,
			calendarDays: [],
			checkedDates: [],
			hasCheckedToday: false,
			checkinRecords: []
		}
	},
	onLoad(options) {
		if (options && options.matchId) {
			this.matchId = parseInt(options.matchId);
		}

		if (!this.matchId) {
			uni.showToast({
				title: '竞赛ID不存在，即将返回',
				icon: 'none',
				duration: 2000
			});
			setTimeout(() => {
				uni.navigateBack();
			}, 2000);
			return;
		}

		this.initDate();
		this.loadCheckinData();
		this.loadCheckinHistory();
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},

		initDate() {
			const now = new Date();
			this.currentYear = now.getFullYear();
			this.currentMonth = now.getMonth() + 1;
		},

		formatDate(date) {
			const year = date.getFullYear();
			const month = String(date.getMonth() + 1).padStart(2, '0');
			const day = String(date.getDate()).padStart(2, '0');
			return `${year}-${month}-${day}`;
		},

		formatTime(date) {
			const hours = String(date.getHours()).padStart(2, '0');
			const minutes = String(date.getMinutes()).padStart(2, '0');
			const seconds = String(date.getSeconds()).padStart(2, '0');
			return `${hours}:${minutes}:${seconds}`;
		},

		generateCalendar() {
			const year = this.currentYear;
			const month = this.currentMonth;

			const firstDay = new Date(year, month - 1, 1);
			const lastDay = new Date(year, month, 0);
			const daysInMonth = lastDay.getDate();
			const firstDayOfWeek = firstDay.getDay();
			const prevMonthLastDay = new Date(year, month - 1, 0).getDate();

			const days = [];
			const today = new Date();
			const todayStr = this.formatDate(today);

			for (let i = firstDayOfWeek - 1; i >= 0; i--) {
				const day = prevMonthLastDay - i;
				const dateStr = this.formatDate(new Date(year, month - 2, day));
				days.push({
					day: day,
					isOtherMonth: true,
					isToday: false,
					isChecked: this.checkedDates.includes(dateStr)
				});
			}

			for (let i = 1; i <= daysInMonth; i++) {
				const dateStr = this.formatDate(new Date(year, month - 1, i));
				const isToday = dateStr === todayStr;
				days.push({
					day: i,
					isOtherMonth: false,
					isToday: isToday,
					isChecked: this.checkedDates.includes(dateStr)
				});
			}

			const remainingDays = 42 - days.length;
			for (let i = 1; i <= remainingDays; i++) {
				const dateStr = this.formatDate(new Date(year, month, i));
				days.push({
					day: i,
					isOtherMonth: true,
					isToday: false,
					isChecked: this.checkedDates.includes(dateStr)
				});
			}

			this.calendarDays = days;
		},

		changeMonth(delta) {
			this.currentMonth += delta;
			if (this.currentMonth > 12) {
				this.currentMonth = 1;
				this.currentYear++;
			} else if (this.currentMonth < 1) {
				this.currentMonth = 12;
				this.currentYear--;
			}
			this.generateCalendar();
		},

		handleDateClick(day) {
			if (day.isOtherMonth) {
				return;
			}

			const clickedDate = `${this.currentYear}-${String(this.currentMonth).padStart(2, '0')}-${String(day.day).padStart(2, '0')}`;

			if (day.isToday) {
				if (this.hasCheckedToday) {
					const todayRecord = this.checkinRecords.find(r => r.date === clickedDate);
					if (todayRecord) {
						uni.showToast({
							title: `已于${todayRecord.time}签到，获得${todayRecord.points}积分`,
							icon: 'none',
							duration: 2000
						});
					}
				} else {
					uni.showToast({
						title: '请点击下方签到按钮进行签到',
						icon: 'none'
					});
				}
				return;
			}

			if (day.isChecked) {
				const record = this.checkinRecords.find(r => r.date === clickedDate);
				if (record) {
					uni.showToast({
						title: `${clickedDate.substring(5)}签到时间：${record.time}，获得${record.points}积分`,
						icon: 'none',
						duration: 2500
					});
				} else {
					uni.showToast({
						title: `${clickedDate.substring(5)}已签到`,
						icon: 'none'
					});
				}
			} else {
				const today = this.formatDate(new Date());
				if (clickedDate < today) {
					uni.showToast({
						title: '不能补签历史日期',
						icon: 'none'
					});
				} else {
					uni.showToast({
						title: '不能提前签到',
						icon: 'none'
					});
				}
			}
		},

		async handleCheckin() {
			if (this.hasCheckedToday) {
				uni.showToast({
					title: '今天已经签到过了',
					icon: 'none'
				});
				return;
			}

			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				uni.showLoading({ title: '签到中...' });

				const res = await checkIn({
					userId: this.userStore.userId,
					matchId: this.matchId
				});

				uni.hideLoading();

				if (isSuccess(res) && res.data) {
					const earnedPoints = res.data.totalPoint || 0;

					uni.showToast({
						title: `签到+${earnedPoints}` + `积分`,
						icon: 'success',
						duration: 2000
					});

					await this.loadCheckinData();
					await this.loadCheckinHistory();
				} else {
					showError(res, '签到失败');
				}
			} catch (error) {
				uni.hideLoading();
				console.error('签到失败:', error);
				uni.showToast({
					title: error.message || '签到失败，请重试',
					icon: 'none'
				});
			}
		},

		async loadCheckinData() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				const res = await getCheckinStatistics({
					userId: this.userStore.userId,
					matchId: this.matchId
				});

				if (isSuccess(res) && res.data) {
					this.continuousDays = res.data.continuousDays || 0;
					this.hasCheckedToday = res.data.hasCheckedToday || false;
					this.points = res.data.totalPoints || 0;
					this.generateCalendar();
				}
			} catch (error) {
				console.error('加载签到统计数据失败:', error);
				uni.showToast({
					title: '加载签到数据失败',
					icon: 'none'
				});
			}
		},

		async loadCheckinHistory() {
			if (!this.userStore.checkLogin()) {
				return;
			}

			try {
				const res = await getCheckInHistory({
					userId: this.userStore.userId,
					matchId: this.matchId,
					limit: 100
				});

				if (isSuccess(res) && res.data) {
					this.checkedDates = res.data.signDates || [];

					if (res.data.signLogs && res.data.signLogs.length > 0) {
						this.checkinRecords = res.data.signLogs.map(log => {
							const createTime = new Date(log.createTime);
							return {
								time: this.formatTime(createTime),
								points: log.point || 0,
								date: this.formatDate(createTime)
							};
						});
					}

					this.generateCalendar();
				}
			} catch (error) {
				console.error('加载签到历史失败:', error);
				uni.showToast({
					title: '加载签到历史失败',
					icon: 'none'
				});
			}
		},

		async handleResetCheckin() {
			uni.showModal({
				title: '开发测试',
				content: '确定要重置今日签到记录吗？重置后可以再次签到测试。',
				success: async (res) => {
					if (res.confirm) {
						try {
							uni.showLoading({ title: '重置中...' });

							const response = await resetTodayCheckin({
								userId: this.userStore.userId,
								matchId: this.matchId
							});

							uni.hideLoading();

							if (isSuccess(response)) {
								uni.showToast({ title: '重置成功', icon: 'success', duration: 2000 });
								await this.loadCheckinData();
								await this.loadCheckinHistory();
							} else {
								showError(response, '重置失败');
							}
						} catch (error) {
							uni.hideLoading();
							console.error('重置今日签到失败:', error);
							uni.showToast({ title: '重置失败，请重试', icon: 'none' });
						}
					}
				}
			});
		}
	}
}
</script>

<style scoped>
.content {
	min-height: 100vh;
	padding-bottom: 30rpx;
}

.header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 9999;
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.header-title {
	flex: 1;
}

.body {
	padding: 20rpx;
}

.checkin-info {
	justify-content: space-between;
	margin-bottom: 30rpx;
	padding: 0 20rpx;
}

.info-item {
	align-items: baseline;
	gap: 8rpx;
}

.calendar-container {
	padding: 30rpx 20rpx;
	margin-bottom: 30rpx;
}

.month-selector {
	margin-bottom: 30rpx;
	padding: 0 10rpx;
}

.month-btn {
	padding: 10rpx 20rpx;
}

.current-month {
	flex: 1;
}

.week-header {
	display: grid;
	grid-template-columns: repeat(7, 1fr);
	margin-bottom: 20rpx;
}

.week-item {
	text-align: center;
	padding: 10rpx 0;
}

.date-grid {
	display: grid;
	grid-template-columns: repeat(7, 1fr);
	gap: 10rpx;
}

.date-item {
	aspect-ratio: 1;
	position: relative;
}

.date-item:not(.is-other-month):active {
	transform: scale(0.9);
	background-color: rgba(0, 191, 255, 0.1);
}

.date-item.is-other-month {
	opacity: 0.3;
}

.date-item.is-checked::after {
	content: '';
	position: absolute;
	bottom: 5rpx;
	left: 50%;
	transform: translateX(-50%);
	width: 8rpx;
	height: 8rpx;
	background-color: #00bfff;
	border-radius: 50%;
}

.date-item.is-today.is-checked::after {
	background-color: #fff;
}

.date-text {
	z-index: 2;
}

.checkin-btn-container {
	padding: 0 30rpx;
}

.checkin-btn:active {
	transform: scale(0.98);
}

.checkin-btn.disabled {
	background: linear-gradient(135deg, #ccc 0%, #999 100%);
	opacity: 0.6;
}

.checkin-records {
	margin-top: 30rpx;
	padding: 0 20rpx;
}

.records-title {
	margin-bottom: 20rpx;
	padding-left: 10rpx;
}

.records-list {
	gap: 15rpx;
}

.record-item {
	padding: 20rpx 25rpx;
	flex-wrap: wrap;
	align-items: baseline;
	line-height: 1.6;
}

.record-text,
.record-time,
.record-points {
	margin-right: 5rpx;
}

.record-time {
	font-weight: bold;
}
</style>