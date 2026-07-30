<template>
	<view class="page bg-f8">
		<view class="header bg-ff" :style="'padding-top:' + statusBarHeight + 'px;'">
			<view class="header-inner dis-flex-hv">
				<text class="header-title font-s18 font-wb color-33">人员信息采集</text>
			</view>
		</view>

		<view v-if="loading" class="loading-wrap dis-flex-hv">
			<text class="color-99 font-s14">加载中...</text>
		</view>

		<view v-else-if="loadError" class="error-wrap dis-flex-hv">
			<text class="color-99 font-s14">{{ loadError }}</text>
		</view>

		<scroll-view v-else scroll-y class="body" :style="'margin-top:' + (50 + statusBarHeight) + 'px;'">
			<view class="info-card bg-ff">
				<view class="info-title font-s18 font-wb color-33">{{ collectionInfo.title }}</view>
				<view class="info-sub font-s14 color-99 mar-T10">调研企业：{{ collectionInfo.researchOrgTitle }}</view>
				<view class="info-sub font-s14 color-99 mar-T6">单位名称：{{ collectionInfo.companyName }}</view>
			</view>

			<template v-for="group in orderedGroups" :key="group.key">
				<view v-if="group.key !== 'certificate' && groupFields(group.key).length" class="form-card bg-ff mar-T20">
					<view class="section-title font-s16 font-wb color-33">{{ group.label }}</view>
					<template v-for="field in groupFields(group.key)" :key="field.key">
						<view v-if="field.key === 'name'" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33 required">{{ field.config.label }}</text>
							<input class="form-input font-s14 color-66" v-model="form.name" placeholder="请输入姓名" />
						</view>
						<view v-else-if="field.key === 'idCardNumber'" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: field.config.required }">{{ field.config.label }}</text>
							<input class="form-input font-s14 color-66" v-model="form.idCardNumber" placeholder="请输入身份证号码" @blur="onIdCardBlur" />
						</view>
						<view v-else-if="field.key === 'companyName'" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33">{{ field.config.label }}</text>
							<text class="form-value font-s14 color-66">{{ form.companyName }}</text>
						</view>
						<view v-else-if="field.config.inputType === 'date'" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: field.config.required }">{{ field.config.label }}</text>
							<picker mode="date" fields="day" :value="normalizePickerDate(form[field.key])" :start="startDate" :end="endDate" @change="onDateChange(field.key, $event)">
								<view class="picker-value dis-flex-vc">
									<text class="font-s14" :class="form[field.key] ? 'color-66' : 'color-99'">{{ form[field.key] || ('请选择' + field.config.label) }}</text>
									<text class="picker-arrow color-99">›</text>
								</view>
							</picker>
						</view>
						<view v-else-if="field.config.inputType === 'select'" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: field.config.required }">{{ field.config.label }}</text>
							<picker v-if="shouldUseSelectPicker(field.config, form[field.key]) && !isCustomInputField(field.key)" mode="selector" :range="getSelectPickerOptions(field.config, form[field.key])" :value="pickerIndex(field.key)" @change="onPickerChange(field.key, $event)">
								<view class="picker-value dis-flex-vc">
									<text class="font-s14" :class="form[field.key] ? 'color-66' : 'color-99'">{{ form[field.key] || '请选择' }}</text>
									<text class="picker-arrow color-99">›</text>
								</view>
							</picker>
							<input v-else class="form-input font-s14 color-66" v-model="form[field.key]" :placeholder="'请输入' + field.config.label" />
						</view>
						<view v-else class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: field.config.required }">{{ field.config.label }}</text>
							<input class="form-input font-s14 color-66" v-model="form[field.key]" :placeholder="'请输入' + field.config.label" />
						</view>
					</template>
				</view>

				<view v-else-if="group.key === 'certificate' && showCertificateSection" class="form-card bg-ff mar-T20">
					<view class="section-title dis-flex-hsb dis-flex-vc">
						<text class="font-s16 font-wb color-33">{{ group.label }}</text>
						<text class="add-cert font-s14 color-73a9fa" @click="addCertificate">+ 添加</text>
					</view>
					<view v-for="(cert, index) in form.certificates" :key="index" class="cert-block">
						<view class="cert-header dis-flex-hsb dis-flex-vc">
							<text class="font-s14 color-66">证书 {{ index + 1 }}</text>
							<text v-if="form.certificates.length > 1" class="font-s12 color-f56" @click="removeCertificate(index)">删除</text>
						</view>
						<view v-if="isCertFieldVisible('certLevel')" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: certFieldConfig('certLevel').required }">{{ certFieldConfig('certLevel').label }}</text>
							<picker v-if="shouldUseSelectPicker(certFieldConfig('certLevel')) && !isCustomInputField('certLevel')" mode="selector" :range="getSelectPickerOptions(certFieldConfig('certLevel'))" :value="levelIndex(cert.level)" @change="onLevelChange(index, $event)">
								<view class="picker-value dis-flex-vc">
									<text class="font-s14" :class="cert.level ? 'color-66' : 'color-99'">{{ cert.level || '请选择等级' }}</text>
									<text class="picker-arrow color-99">›</text>
								</view>
							</picker>
							<input v-else class="form-input font-s14 color-66" v-model="cert.level" placeholder="请输入证书等级" />
						</view>
						<view v-if="isCertFieldVisible('certNo')" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: certFieldConfig('certNo').required }">{{ certFieldConfig('certNo').label }}</text>
							<input class="form-input font-s14 color-66" v-model="cert.certNo" placeholder="请输入证书编号" />
						</view>
						<view v-if="isCertFieldVisible('certIssueDate')" class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-label font-s14 color-33" :class="{ required: certFieldConfig('certIssueDate').required }">{{ certFieldConfig('certIssueDate').label }}</text>
							<picker mode="date" fields="day" :value="normalizePickerDate(cert.issueDate)" :start="startDate" :end="endDate" @change="onCertIssueDateChange(index, $event)">
								<view class="picker-value dis-flex-vc">
									<text class="font-s14" :class="cert.issueDate ? 'color-66' : 'color-99'">{{ cert.issueDate || '请选择发证日期' }}</text>
									<text class="picker-arrow color-99">›</text>
								</view>
							</picker>
						</view>
					</view>
				</view>
			</template>

			<view class="submit-wrap">
				<view class="submit-btn dis-flex-hv bg-73a9fa" :class="{ disabled: submitting }" @click="handleSubmit">
					<text class="color-ff font-s16 font-wb">{{ hasExisting ? '更新信息' : '提交信息' }}</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import {
	getResearchCollectionDetail,
	findMyResearchCollection,
	submitResearchCollection
} from '@/api/research-collect.js';
import {
	isSaasSuccess as isSuccess,
	getSaasData as getData,
	getSaasMsg as getMsg,
	showSaasError as showError,
	showSaasSuccess as showSuccess
} from '@/utils/saasResponse.js';
import {
	buildEmptyForm,
	CUSTOM_SELECT_OPTION,
	getOrderedGroups,
	getOrderedVisibleFields,
	getSelectPickerOptions,
	isCertificateFieldKey,
	normalizeCollectionFieldOptions,
	shouldUseSelectPicker,
	validatePersonFormValues
} from '@/utils/researchCollectionSchema.js';

export default {
	data() {
		return {
			token: '',
			statusBarHeight: 0,
			loading: true,
			loadError: '',
			submitting: false,
			hasExisting: false,
			collectionInfo: {
				title: '',
				researchOrgTitle: '',
				companyName: '',
				fieldOptions: null
			},
			form: buildEmptyForm(''),
			customInputFields: {}
		};
	},
	computed: {
		startDate() {
			return this.getDate('start');
		},
		endDate() {
			return this.getDate('end');
		},
		fieldOptions() {
			return this.collectionInfo.fieldOptions;
		},
		orderedGroups() {
			return getOrderedGroups(this.fieldOptions);
		},
		showCertificateSection() {
			return getOrderedVisibleFields(this.fieldOptions, 'certificate').some((f) => isCertificateFieldKey(f.key));
		}
	},
	onLoad(options) {
		this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0;
		this.token = (options && options.token) ? decodeURIComponent(options.token) : '';
		if (!this.token) {
			this.loading = false;
			this.loadError = '链接无效，请重新扫码进入';
			return;
		}
		this.loadDetail();
	},
	methods: {
		groupFields(groupKey) {
			return getOrderedVisibleFields(this.fieldOptions, groupKey).filter((f) => !isCertificateFieldKey(f.key));
		},
		isCertFieldVisible(key) {
			return getOrderedVisibleFields(this.fieldOptions, 'certificate').some((f) => f.key === key);
		},
		certFieldConfig(key) {
			const field = getOrderedVisibleFields(this.fieldOptions, 'certificate').find((f) => f.key === key);
			return field ? field.config : { label: key, required: false, options: [] };
		},
		shouldUseSelectPicker,
		getSelectPickerOptions,
		isCustomInputField(key) {
			return !!this.customInputFields[key];
		},
		getFieldConfig(key) {
			const field = getOrderedVisibleFields(this.fieldOptions).find((f) => f.key === key);
			return field ? field.config : null;
		},
		syncCustomInputFields() {
			// 仅在选择「其他（手动输入）」时切换手输，不因历史值自动降级为文本框
			this.customInputFields = { ...this.customInputFields };
		},
		getDate(type) {
			const date = new Date();
			let year = date.getFullYear();
			let month = date.getMonth() + 1;
			let day = date.getDate();
			if (type === 'start') year -= 50;
			else if (type === 'end') year += 1;
			month = month > 9 ? month : '0' + month;
			day = day > 9 ? day : '0' + day;
			return `${year}-${month}-${day}`;
		},
		normalizePickerDate(value) {
			if (!value) return '';
			if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
			if (/^\d{4}-\d{2}$/.test(value)) return `${value}-01`;
			return value;
		},
		onDateChange(key, e) {
			this.form[key] = e.detail.value || '';
		},
		onCertIssueDateChange(index, e) {
			this.form.certificates[index].issueDate = e.detail.value || '';
		},
		pickerIndex(key) {
			const config = this.getFieldConfig(key);
			const options = getSelectPickerOptions(config, this.form[key]);
			const index = options.indexOf(this.form[key]);
			return index >= 0 ? index : 0;
		},
		onPickerChange(key, e) {
			const config = this.getFieldConfig(key);
			const options = getSelectPickerOptions(config, this.form[key]);
			const selected = options[Number(e.detail.value)] || '';
			if (config && config.allowCustom && selected === CUSTOM_SELECT_OPTION) {
				this.customInputFields[key] = true;
				this.form[key] = '';
				return;
			}
			this.customInputFields[key] = false;
			this.form[key] = selected;
		},
		levelIndex(level) {
			const options = getSelectPickerOptions(this.certFieldConfig('certLevel'));
			const index = options.indexOf(level);
			return index >= 0 ? index : 0;
		},
		onLevelChange(index, e) {
			const config = this.certFieldConfig('certLevel');
			const options = getSelectPickerOptions(config);
			const selected = options[Number(e.detail.value)] || '';
			if (config.allowCustom && selected === CUSTOM_SELECT_OPTION) {
				this.customInputFields.certLevel = true;
				this.form.certificates[index].level = '';
				return;
			}
			this.customInputFields.certLevel = false;
			this.form.certificates[index].level = selected;
		},
		addCertificate() {
			this.form.certificates.push({ level: '', certNo: '', issueDate: '' });
		},
		removeCertificate(index) {
			this.form.certificates.splice(index, 1);
		},
		async loadDetail() {
			this.loading = true;
			this.loadError = '';
			try {
				const res = await getResearchCollectionDetail(this.token);
				if (!res) {
					this.loadError = '无法连接采集服务，请检查网络或稍后重试';
					return;
				}
				if (!isSuccess(res)) {
					this.loadError = getMsg(res) || '采集信息加载失败';
					return;
				}
				const data = getData(res) || {};
				if (data.active === false) {
					this.loadError = '采集尚未开启或已结束';
					return;
				}
				this.collectionInfo = {
					title: data.title || '人员信息采集',
					researchOrgTitle: data.researchOrgTitle || '',
					companyName: data.companyName || '',
					fieldOptions: normalizeCollectionFieldOptions(data.fieldOptions)
				};
				this.form.companyName = data.companyName || '';
				this.syncCustomInputFields();
			} catch (e) {
				console.log('[collect] loadDetail error', e);
				this.loadError = '网络异常，请稍后重试';
			} finally {
				this.loading = false;
			}
		},
		fillFormFromPerson(person) {
			this.form = {
				name: person.name || '',
				idCardNumber: person.idCardNumber || '',
				phone: person.phone || '',
				companyName: person.companyName || this.collectionInfo.companyName,
				major: person.major || '',
				education: person.education || '',
				workTime: person.workTime || '',
				workTypeName: person.workTypeName || '',
				department: person.department || '',
				job: person.job || '',
				workDateStart: person.workDateStart || '',
				titleType: person.titleType || '',
				titleMajor: person.titleMajor || '',
				titleCertDate: person.titleCertDate || '',
				certificates: (person.certificates && person.certificates.length)
					? person.certificates.map((c) => ({
						level: c.level || '',
						certNo: c.certNo || '',
						issueDate: c.issueDate || ''
					}))
					: [{ level: '', certNo: '', issueDate: '' }]
			};
			this.hasExisting = true;
			this.syncCustomInputFields();
		},
		async onIdCardBlur() {
			const idCard = (this.form.idCardNumber || '').trim();
			if (!/^\d{17}[\dXx]$/.test(idCard)) return;
			try {
				const res = await findMyResearchCollection(this.token, idCard);
				if (!isSuccess(res)) return;
				const person = getData(res);
				if (person && person.id) {
					this.fillFormFromPerson(person);
					uni.showToast({ title: '已加载您的历史信息', icon: 'none' });
				} else {
					this.hasExisting = false;
				}
			} catch (e) {
				// ignore
			}
		},
		buildPayload() {
			const certificates = (this.form.certificates || [])
				.filter((c) => (c.level || '').trim() || (c.certNo || '').trim() || (c.issueDate || '').trim())
				.map((c) => ({
					level: (c.level || '').trim(),
					certNo: (c.certNo || '').trim(),
					issueDate: (c.issueDate || '').trim()
				}));
			return {
				name: (this.form.name || '').trim(),
				idCardNumber: (this.form.idCardNumber || '').trim(),
				phone: (this.form.phone || '').trim(),
				companyName: (this.form.companyName || '').trim(),
				major: (this.form.major || '').trim(),
				education: (this.form.education || '').trim(),
				workTime: (this.form.workTime || '').trim(),
				workTypeName: (this.form.workTypeName || '').trim(),
				department: (this.form.department || '').trim(),
				job: (this.form.job || '').trim(),
				workDateStart: (this.form.workDateStart || '').trim(),
				titleType: (this.form.titleType || '').trim(),
				titleMajor: (this.form.titleMajor || '').trim(),
				titleCertDate: (this.form.titleCertDate || '').trim(),
				certificates
			};
		},
		async handleSubmit() {
			if (this.submitting) return;
			const err = validatePersonFormValues(this.form, this.fieldOptions);
			if (err) {
				uni.showToast({ title: err, icon: 'none' });
				return;
			}
			this.submitting = true;
			try {
				const res = await submitResearchCollection(this.token, this.buildPayload());
				if (!isSuccess(res)) {
					showError(res, '提交失败');
					return;
				}
				const result = getData(res);
				showSuccess(res, (result && result.message) || '提交成功');
				this.hasExisting = !!(result && result.updated);
			} catch (e) {
				uni.showToast({ title: '提交失败，请重试', icon: 'none' });
			} finally {
				this.submitting = false;
			}
		}
	}
};
</script>

<style scoped>
.page { min-height: 100vh; }
.header { position: fixed; top: 0; left: 0; right: 0; z-index: 100; border-bottom: 1rpx solid #f0f0f0; }
.header-inner { height: 50px; }
.header-title { text-align: center; }
.body { height: calc(100vh - 50px); padding: 20rpx 20rpx 40rpx; box-sizing: border-box; }
.loading-wrap, .error-wrap { min-height: 60vh; padding-top: 200rpx; }
.info-card, .form-card { border-radius: 16rpx; padding: 30rpx; }
.info-title, .info-sub { line-height: 1.5; }
.section-title { margin-bottom: 10rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f5f5f5; }
.form-row { padding: 24rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.form-label { flex-shrink: 0; width: 240rpx; }
.form-label.required::before { content: '*'; color: #f56c6c; margin-right: 4rpx; }
.form-input { flex: 1; text-align: right; }
.form-value { flex: 1; text-align: right; }
.picker-value { flex: 1; justify-content: flex-end; }
.picker-arrow { margin-left: 8rpx; font-size: 32rpx; line-height: 1; }
.cert-block { margin-top: 16rpx; padding-top: 8rpx; border-top: 1rpx dashed #eee; }
.cert-header { padding: 12rpx 0; }
.add-cert { padding: 8rpx 0; }
.submit-wrap { padding: 40rpx 0 calc(40rpx + env(safe-area-inset-bottom)); }
.submit-btn { height: 96rpx; border-radius: 48rpx; box-shadow: 0 4rpx 12rpx rgba(115, 169, 250, 0.35); }
.submit-btn.disabled { opacity: 0.6; }
.mar-T6 { margin-top: 6rpx; }
.mar-T10 { margin-top: 10rpx; }
.mar-T20 { margin-top: 20rpx; }
.color-f56 { color: #f56c6c; }
</style>
