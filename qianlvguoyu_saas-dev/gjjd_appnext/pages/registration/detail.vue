<template>
	<view class="content bg-f8" :class="{'no-scroll': isShowForm}">
		<view :style="'top:'+($statusBarHeight+44+20)+'px;'" style="position:fixed;left:20rpx;right:20rpx;z-index:9999;background-color:rgba(0, 0, 0, 0.6);padding: 10rpx;color:#fff;border-radius:8rpx;" v-if="showpermissionTip">
			<view style="font-weight: bold;">相册、相机授权提示</view>
			<view style="margin-top: 10px;">为了实现扫码功能、相关材料上传，需要访问您的相册、相机权限，您如果拒绝开启,将无法使用上述功能。</view>
		</view>
		<view class="header bg-ff" :style="'padding-top:'+$statusBarHeight+'px;'">
			<view class="header-box dis-flex-hsb dis-flex-vc">
				<view class="header-back dis-flex-hv" @click="goBack">
					<i class="gjjd-iconfont icon-xiangzuo font-s20"></i>
				</view>
				<view class="header-title font-s18 font-wb">自主报名预约</view>
				<view class="header-placeholder"></view>
			</view>
		</view>
		
		<view class="body" :style="'margin-top:'+(50+$statusBarHeight)+'px;'" :class="{'no-scroll': isShowForm}">
			<!-- 项目信息卡片 -->
			<view class="company-cover dis-flex-hc">
				<image mode="widthFix" :src="registrationInfo.cover"></image>
			</view>
			<view class="info-card bg-ff">
				<view class="company-title">
					<view class="company-name font-s18 font-wb color-33">{{registrationInfo.company}}</view>
					<view class="company-subtitle font-s14 color-99" v-if="registrationInfo.subtitle">{{registrationInfo.subtitle}}</view>
				</view>
			</view>
				<view class="info-section">
					<view class="info-title font-s16 font-wb color-33">{{registrationInfo.title}}</view>
					
					<view class="info-details">
						<view class="info-item">
							<view class="info-label font-s14 color-99">简介</view>
						<view class="info-value font-s14 color-66">
							<uv-parse :content="registrationInfo.intro"></uv-parse>
						</view>
						</view>
						
						<view class="info-item">
							<view class="info-label font-s14 color-99">培训时间</view>
						<view class="info-value font-s14 color-66">
							<uv-parse :content="registrationInfo.train_time"></uv-parse>
						</view>
						</view>
						
						<view class="info-item">
							<view class="info-label font-s14 color-99">考试培训时间</view>
						<view class="info-value font-s14 color-66">
							<uv-parse :content="registrationInfo.exam_time"></uv-parse>
						</view>
						</view>
					</view>
				</view>
			</view>
			
			<!-- 填写按钮 -->
		<view v-if="infoStatus" class="submit-btn dis-flex-hv bg-7fd5f9">
			<text class="color-ff font-s16 font-wb">您已报名</text>
		</view>
		<view v-else class="submit-btn dis-flex-hv bg-73a9fa" @click="showFormPopup">
			<text class="color-ff font-s16 font-wb">点击填写</text>
		</view>
		
		<!-- 表单弹窗 -->
		<view class="form-popup" :class="{'show': isShowForm}" @click="closeForm" @touchmove.stop.prevent>
			<view class="form-content dis-flex flex-column bg-ff" @click.stop @touchmove.stop>
				<view class="form-header dis-flex-hsb dis-flex-vc">
					<view class="form-title font-s18 font-wb color-33">预约报名</view>
					<view class="form-close dis-flex-hv" @click="closeForm">
						<text class="close-text color-66">×</text>
					</view>
				</view>
				<view v-if="!itoken">
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">身份证号</text>
						<input class="form-row-input font-s14 color-66" v-model="formData.idCardNumber" placeholder="请输入身份证号" @input="onIdcard" />
					</view>
					<template v-if="isAuth">
						<view class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-row-label font-s14 color-33">手机号</text>
							<input class="form-row-input font-s14 color-66" v-model="formData.phone" placeholder="请输入手机号" />
						</view>
						<view class="form-row dis-flex-hsb dis-flex-vc">
							<text class="form-row-label font-s14 color-33">验证码</text>
							<input class="form-row-input font-s14 color-66" v-model="smscode" placeholder="请输入验证码" />
							<text @click="getCode" class="mar-L20 bg-73a9fa color-ff font-s12" style="padding: 4px 10rpx;border-radius: 4rpx;">{{smsText}}</text>
						</view>
						<view class="idcard-yz mar-T10" @click="checkPhone">登陆</view>
					</template>
					<view v-else class="idcard-yz mar-T10" @click="checkIdCard">验证</view>
				</view>
				<scroll-view v-else class="form-body" scroll-y @touchmove.stop>
					<!-- 手机号 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">手机号</text>
						<text class="form-row-value font-s14 color-66">{{formData.phone}}</text>
					</view>
					
					<!-- 身份证正面 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<view>
						<text class="form-row-label font-s14 color-33">身份证正面</text>
							<view class="form-row-tip font-s12 color-73a9fa mar-T10" @click="picExample({title:'身份证正面示例',text:0,src:'idcard_front_example.png',width:243,heigh:153})">查看样例/提示</view>
						</view>
						<view class="form-row-right dis-flex-vc" @click="uploadIdCardFront">
							<text class="form-row-upload font-s14 color-66">{{formData.id_card_front ? '已上传' : '点击上传身份证正面'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 身份证反面 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<view>
						<text class="form-row-label font-s14 color-33">身份证反面</text>
							<view class="form-row-tip font-s12 color-73a9fa mar-T10" @click="picExample({title:'身份证反面示例',text:0,src:'idcard_back_example.png',width:243,heigh:153})">查看样例/提示</view>
						</view>
						<view class="form-row-right dis-flex-vc" @click="uploadIdCardBack">
							<text class="form-row-upload font-s14 color-66">{{formData.id_card_back ? '已上传' : '点击上传身份证反面'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 姓名 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">姓名</text>
						<input class="form-row-input font-s14 color-66" disabled v-model="formData.name" placeholder="请上传身份证正面识别" />
					</view>
					
					<!-- 身份证号 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<view>
						<text class="form-row-label font-s14 color-33">身份证号</text>
							<view class="form-row-tip font-s12 color-73a9fa mar-T10" @click="picExample({title:'身份号示例',text:1,src:'450101202510313621'})">查看样例/提示</view>
						</view>
						<view class="form-row-right dis-flex-vc">
							<input class="form-row-input font-s14 color-66" disabled v-model="formData.id_card_number" placeholder="请上传身份证正面识别" />
						</view>
					</view>
					
					<!-- 身份证地址 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">身份证地址</text>
						<input class="form-row-input font-s14 color-66" v-model="formData.address" placeholder="请上传身份证正面识别" />
					</view>
					
					<!-- 年龄 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">年龄</text>
						<input class="form-row-input font-s14 color-66" disabled v-model="formData.age" type="number" placeholder="请上传身份证正面识别" />
					</view>
					
					<!-- 民族 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">民族</text>
						<input class="form-row-input font-s14 color-66" disabled v-model="formData.nation" placeholder="请上传身份证正面识别" />
					</view>
					
					<!-- 一寸白底证件照 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<view>
						<text class="form-row-label font-s14 color-33">一寸白底证件照</text>
							<view class="form-row-tip font-s12 color-73a9fa mar-T10" @click="picExample({title:'一寸白底证件照示例',text:0,src:'idcard_header_example.png',width:295,heigh:413})">查看样例/提示</view>
						</view>
						<view class="form-row-right dis-flex-vc" @click="uploadIdCardHeader">
							<text class="form-row-value font-s14 color-66">{{formData.id_card_header ? '已上传' : '请上传一寸白底证件照'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>

					<!-- 政治面貌 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="selectPoliticalStatus">
						<text class="form-row-label font-s14 color-33">政治面貌</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.political || '群众'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 户口性质 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="selectHouseholdType">
						<text class="form-row-label font-s14 color-33">户口性质</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.house_type || '点击选择户口性质'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 毕业院校 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">毕业院校</text>
						<input class="form-row-input font-s14 color-66" v-model="formData.school" placeholder="请输入毕业院校" />
					</view>
					
					<!-- 学历 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="selectEducation">
						<text class="form-row-label font-s14 color-33">学历</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.education || '小学'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 所学专业 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">所学专业</text>
						<input class="form-row-input font-s14 color-66" v-model="formData.major" placeholder="请输入所学专业" />
					</view>
					
					<!-- 是否有毕业证 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">是否有毕业证</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-switch-text font-s14 color-66">{{formData.school_paper_has ? '是' : '否'}}</text>
							<switch :checked="formData.school_paper_has" @change="onSchoolPaper" color="#73a9fa" />
						</view>
					</view>
					
					<!-- 毕业证编号 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" v-if="formData.school_paper_has">
						<text class="form-row-label font-s14 color-33">毕业证编号</text>
						<input class="form-row-input font-s14 color-66" v-model="formData.school_no" placeholder="请输入毕业证编号" />
					</view>
					
					<!-- 毕业证 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" v-if="formData.school_paper_has" @click="uploadImage('school_paper')">
						<text class="form-row-label font-s14 color-33">毕业证照片</text>
						<view class="form-row-righ dis-flex-vct">
							<text class="form-row-value font-s14 color-66">{{formData.school_paper ? '已上传' : ''}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 入学日期 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">入学日期</text>
						<view class="form-row-right dis-flex-vc">
							<picker mode="date" fields="day" :value="formData.in_school_date" :start="startDate" :end="endDate" @change="bindDateChange($event,'in_school_date')">
								<view class="font-s14 color-66">{{formData.in_school_date || '点击选择入学日期'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 毕业日期 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">毕业日期</text>
						<view class="form-row-right dis-flex-vc">
							<picker mode="date" fields="day" :value="formData.graduate_date" :start="startDate" :end="endDate" @change="bindDateChange($event,'graduate_date')">
								<view class="font-s14 color-66">{{formData.graduate_date || '点击选择毕业日期'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 从业开始日期 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">从业开始日期</text>
						<view class="form-row-right dis-flex-vc">
							<picker mode="date" fields="day" :value="formData.work_time" :start="startDate" :end="endDate" @change="bindDateChange($event,'work_time')">
								<view class="font-s14 color-66">{{formData.work_time || '点击选择从业开始日期'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 本公司入职日期 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">本公司入职日期</text>
						<view class="form-row-right dis-flex-vc">
							<picker mode="date" fields="day" :value="formData.work_date_start" :start="startDate" :end="endDate" @change="bindDateChange($event,'work_date_start')">
								<view class="font-s14 color-66">{{formData.work_date_start || '点击选择本公司入职日期'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 预约工种 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" style="display: none;">
						<text class="form-row-label font-s14 color-33">预约工种</text>
						<view class="form-row-right dis-flex-vc">
							<picker @columnchange="changeReqWorkType" @cancel="cancelReqWorkType" mode="multiSelector" :value="[reqWorkIndex,reqWorkIndex1]" :range="reqWorkArray">
								<view class="font-s14 color-66">{{formData.req_work_type_name || '点击选择预约工种'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 预约工种等级 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="selectReqWorkTypeLevel" style="display: none;">
						<text class="form-row-label font-s14 color-33">预约工种等级</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{optionsWorkTypeLevel[formData.req_level] || '点击选择预约工种等级'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 是否有上一等级证书 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">是否有上一等级证书</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-switch-text font-s14 color-66">{{formData.work_type_paper_has ? '是' : '否'}}</text>
							<switch :checked="formData.work_type_paper_has" @change="onWorkTypePaper" color="#73a9fa" />
						</view>
					</view>
					
					<!-- 上一等级证书 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" v-if="formData.work_type_paper_has" @click="uploadImage('work_type_paper')">
						<text class="form-row-label font-s14 color-33">上一等级证书</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.work_type_paper ? '已上传' : '点击上传上一等级证书'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 职称 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="selectJobTitle">
						<text class="form-row-label font-s14 color-33">职称</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.title_type || '点击选择职称'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 职称证书 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" v-if="['助理工程师', '工程师', '高级工程师', '其他'].includes(formData.title_type)" @click="uploadImage('title_paper')">
						<text class="form-row-label font-s14 color-33">职称证书</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.title_paper ? '已上传' : '点击上传职称证书'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 工作经历 -->
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">工作经历</text>
						<view class="form-row-right dis-flex-vc" v-if="formData.work_exp.length==0" @click="$refs.workExp.open()">
							<text class="form-row-value font-s14 color-66">点击添加工作经历</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
						<view style="display: flex;align-items: center;justify-content: space-between;" v-else>
							<view class="text-R">
								<view v-for="(exp,i) in formData.work_exp">
									<view>{{exp.startDate}}~{{exp.endDate}}</view>
									<view>{{exp.company}},{{exp.position}}</view>
									<view>{{exp.proveName}},{{exp.provePhone}}</view>
									<view>
										<i class="gjjd-iconfont icon-bianji" @click="workExpEdit(i)"></i>
										<i class="gjjd-iconfont icon-shanchu font-s20 mar-L20" @click="workExpDel(i)"></i>
									</view>
								</view>
							</view>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 劳动合同第一页 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="uploadContract('contact_first_page')">
						<text class="form-row-label font-s14 color-33">劳动合同第一页</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.contact_first_page ? '已上传' : '点击上传劳动合同第一页'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 劳动合同期限页 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="uploadContract('contact_expire_page')">
						<text class="form-row-label font-s14 color-33">劳动合同期限页</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.contact_expire_page ? '已上传' : '点击上传劳动合同期限页'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 劳动合同盖章页三页 -->
					<view class="form-row dis-flex-hsb dis-flex-vc" @click="uploadContract('contact_sign_page')">
						<text class="form-row-label font-s14 color-33">劳动合同盖章页三页</text>
						<view class="form-row-right dis-flex-vc">
							<text class="form-row-value font-s14 color-66">{{formData.contact_sign_page ? '已上传' : '点击上传劳动合同盖章页'}}</text>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					
					<!-- 提交按钮在滚动区域内 -->
					<view class="form-submit-wrapper">
						<view class="form-submit-btn bg-73a9fa" @click="submitForm">
							<text class="color-ff font-s16 font-wb">提交</text>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>
		<uni-popup ref="workExp" type="bottom" :is-mask-click="false">
			<view class="popup">
				<view class="popup-header text-C">
					<text class="popup-close" @click="workexpClose"><i class="gjjd-iconfont icon-shanchuguanbiquxiaowubiankuang"></i></text>
					<view class="mar-T30 pad-L20 pad-R20 font-s14 color-66 text-L">温馨提示：请按实填写，仅需填写本单位的工作经历，否则可能影响材料制作</view>
				</view>
				<view class="popup-body">
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">工作单位</text>
						<input class="form-row-input font-s14 color-66" v-model="workexp.company" placeholder="请输入工作单位" />
					</view>
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">岗位</text>
						<input class="form-row-input font-s14 color-66" v-model="workexp.position" placeholder="请输入岗位" />
					</view>
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">开始日期</text>
						<view class="form-row-right dis-flex-vc">
							<picker mode="date" fields="day" :value="workexp.startDate" :start="startDate" :end="endDate" @change="bindDateChangeExp($event,'startDate')">
								<view class="font-s14 color-66">{{workexp.startDate || '点击选择开始日期'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">结束日期</text>
						<view class="form-row-right dis-flex-vc">
							<picker mode="date" fields="day" :value="workexp.endDate" :start="startDate" :end="endDate" @change="bindDateChangeExp($event,'endDate')">
								<view class="font-s14 color-66">{{workexp.endDate || '点击选择结束日期'}}</view>
							</picker>
							<i class="gjjd-iconfont icon-xiangyou color-99"></i>
						</view>
					</view>
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">证明人</text>
						<input class="form-row-input font-s14 color-66" v-model="workexp.proveName" placeholder="请输入证明人" />
					</view>
					<view class="form-row dis-flex-hsb dis-flex-vc">
						<text class="form-row-label font-s14 color-33">证明人联系方式</text>
						<input class="form-row-input font-s14 color-66" v-model="workexp.provePhone" placeholder="请输入证明人联系方式" />
					</view>
					<view @click="workextSubmit" class="mar-T10 bg-73a9fa color-ff text-C" style="height: 40px;line-height: 40px;border-radius: 20px;">提交</view>
				</view>
			</view>
		</uni-popup>
		<uni-popup ref="tips" type="center" :is-mask-click="false">
			<view class="popup-tips">
				<view class="mar-T10 pad-T10 text-C">{{pic_example_obj.title}}</view>
				<view class="popup-tips-pic dis-flex-hv">
					<text v-if="pic_example_obj.text">{{pic_example_obj.src}}</text>
					<image v-else :src="'/static/images/'+pic_example_obj.src" :style="{width:pic_example_obj.width+'rpx',height:pic_example_obj.heigh+'rpx'}"></image>
				</view>
				<view class="popup-tips-close">
					<i class="gjjd-iconfont icon-round_delete font-s24 color-ff" @click="picExampleClose()"></i>
				</view>
			</view>
		</uni-popup>
	</view>
</template>

<script>
import {request,uploads,rectTo,navTo,showMsg} from "@/utils";
import { getDeviceId, getDeviceInfo } from '@/utils/device';
import { useUserStore } from '@/store/user';
export default {
	setup() {
		const userStore = useUserStore()
		return {
			userStore
		}
	},
	data() {
		return {
			showpermissionTip:false,
			infoStatus:0,
			isShowForm: false,
			itoken:'',
			isAuth:'',
			id:0,
			smscode:'',
			smsText:'获取验证码',
			timeid:0,
			registrationInfo: {},
			reqWorkIndex:0,
			reqWorkIndex1:0,
			workList:{},
			workListID:{},
			reqWorkArray:[
				['无'],
				['无'],
			],
			reqWorkColumn:[
				['无'],
				['无']
			],
			optionsWorkTypeLevel:["五级/初级工", "四级中级工", "三级高级工", "二级/技师", "一级/高级技师"],
			formData: {
				id:0,
				sign_up_id: null,
				platform_user_id:0,
				phone: "",
				id_card_front: null,
				id_card_back: null,
				name: "",
				id_card_number: "",
				address: null,
				//age: null,
				nation: null,
				id_card_header: null,
				house_type: null,
				political: null,
				
				school: null,
				education: null,
				major: null,
				school_paper_has:0,
				school_paper: null,
				school_no: null,
				in_school_date: '',
				graduate_date: null,
				
				work_time: null,
				work_date_start: null,
				work_date_end: null,
				req_work_type_id: null,
				req_work_type_name: null,
				req_level: null,
				work_type_paper_has: 0,
				work_type_paper: null,
				level: null,
				work_type_id: null,
				title_type: null,
				title_paper: null,
				work_exp: [],
				contact_first_page: null,
				contact_expire_page: null,
				contact_sign_page: null
			},
			workexp:{
				company:'',
				position:'',
				startDate:'',
				endDate:'',
				proveName:'',
				provePhone:''
			},
			pic_example_title:'',
			pic_example_istext:0,
			pic_example_src:''
		}
	},
	onLoad(options) {
		console.log('接收参数:', options);
		this.itoken = uni.getStorageSync('itoken');
		if(options.id){
			this.id = options.id;
			this.formData.sign_up_id = options.id;
			this.getData();
		}
	},
	onShow() {
		// 移除登录检查，允许用户查看项目详情
		// this.$util.checkLogin();
	},
	computed: {
		startDate() {
			return this.getDate('start');
		},
		endDate() {
			return this.getDate('end');
		}
	},
	methods: {
		goBack() {
			uni.navigateBack();
		},
		onIdcard(e){
			let idcard = e.detail.value;
			//console.log('len='+idcard.length);
			if(idcard.length == 18){
				this.isAuth = '';
			}
		},
		picExample(obj){
			this.pic_example_obj = obj;
			this.$refs.tips.open();
		},
		picExampleClose(){
			this.$refs.tips.close();
			this.pic_example_obj = {title:'',text:0,src:'',width:0,height:0};
		},
		getDate(type) {
			const date = new Date();
			let year = date.getFullYear();
			let month = date.getMonth() + 1;
			let day = date.getDate();

			if (type === 'start') {
				year = year - 30;
			} else if (type === 'end') {
				year = year + 10;
			}
			month = month > 9 ? month : '0' + month;
			day = day > 9 ? day : '0' + day;
			return `${year}-${month}-${day}`;
		},
		bindDateChange(e,field){
			console.log(e,field);
			let date = e.detail.value;
			this.formData[field] = date;
		},
		bindDateChangeExp(e,field){
			let date = e.detail.value;
			this.workexp[field] = date;
		},
		workExpEdit(i){
			this.workexp = this.formData.work_exp[i];
			this.$refs.workExp.open();
		},
		workExpDel(i){
			this.formData.work_exp = [];
		},
		workexpClose(){
			this.workexp = {
				company:'',
				position:'',
				startDate:'',
				endDate:'',
				proveName:'',
				provePhone:''
			}
			this.$refs.workExp.close();
		},
		workextSubmit(){
			/*if(this.formData.work_exp.length>0){
				showMsg('this',{title:'最多添加一项'});
				return;
			}*/
			if(!this.workexp.position){
				showMsg('this',{title:'请添加岗位'});
				return;
			}
			if(!this.workexp.startDate){
				showMsg('this',{title:'请添加开始日期'});
				return;
			}
			if(!this.workexp.endDate){
				showMsg('this',{title:'请添加结束日期'});
				return;
			}
			if(!this.workexp.proveName){
				showMsg('this',{title:'请添加证明人'});
				return;
			}
			if(!this.workexp.provePhone){
				showMsg('this',{title:'请添加证明人联系方式'});
				return;
			}
			//this.formData.work_exp.push(this.workexp);
			this.formData.work_exp = [this.workexp];
			this.workexpClose();
		},
		showFormPopup() {
			this.isShowForm = true;
		},
		closeForm() {
			this.isShowForm = false;
		},
		async getCode(){
			let that = this;
			if(!this.formData.phone){
				showMsg('this',{title:'请输入手机号'});
				return;
			}
			if(this.timeid>0) return;
			//获取验证码
			let rs = await request({
				method:'POST',
				data:{phone:this.formData.phone},
				url:"/appsms/send"
			});
			if(rs.code){
				showMsg('this',{title:'短信验证码获失败'});
				return;
			}
			let t = 60;
			this.timeid = setInterval(()=>{
				if(t>0){
					t--;
					that.smsText = t+'秒后可重新获取';
				}else{
					clearInterval(that.timeid);
					that.timeid = 0;
					that.smsText = '获取验证码';
				}
			},1000);
		},
		async getData(){
			console.log("selfinfo id",this.id);
			let res = await request({
				method:'POST',
				data:{id:this.id},
				url:"/appregistration/selfinfo"
			});
			console.log("selfinfo",res);
			if(res?.data?.info){
				this.registrationInfo = res.data.info;
			}
			if(res?.data?.formData){
				this.formData = res.data.formData;
				//console.log(this.formData);
				if(this.formData?.status){
					this.infoStatus = this.formData.status;
				}
			}
			this.workList = res.data.workList;
			this.workListID = res.data.workListID;
			this.createWorkTypeArray(0);
			
		},
		async checkIdCard(){
			if(!this.formData.idCardNumber){
				showMsg('this',{title:'请输入身份证号'});
				return;
			}
			let res = await request({
				method:'POST',
				data:{idcard:this.formData.idCardNumber},
				url:"/appuser/infobyidcard"
			});
			console.log("checkIdCard",res);
			if(res?.code){
				showMsg('this',{title:'该身份证号还未注册'});
				this.isAuth = 'reg';
				return;
			}
			let info = res.data.info;
			this.formData.phone = info.phone;
			this.isAuth = 'login';
		},
		async checkPhone(){
			if(!this.formData.phone){
				showMsg('this',{title:'请输入手机号'});
				return;
			}
			let res = await request({
				method:'POST',
				data:{phone:this.formData.phone,code:this.smscode},
				url:"/appauth/checkcode"
			});
			console.log("checkPhone",res);
			if(res?.code){
				showMsg('this',{title:res.msg});
				return;
			}
			this.setLogin();
		},
		async setLogin(){
			let res = await request({
				method:'POST',
				data:{
					phone:this.formData.phone,
					idcard:this.formData.idCardNumber,
					isAuth:this.isAuth,
					deviceId:getDeviceId(),
					deviceInfo:getDeviceInfo()
				},
				url:"/appauth/setlogin"
			});
			console.log("setLogin",res);
			if(res?.code === 2){
				showMsg('this',{title:res.msg || '当前设备未授权，请联系管理员审批解绑'});
				return;
			}
			if(res?.code){
				showMsg('this',{title:res.msg});
				return;
			}
			uni.setStorageSync('itoken',res.data.token);
			const payload = {};
			payload.token = res.data.token;
			payload.user = res.data.user || {};
			payload.user.phone = this.formData.phone;
			this.userStore.login(payload);
			//绑定websocket clientid
			request({
				method:'POST',
				data:{clientid:this.userStore.clientId},
				url:"/appauth/bind"
			}).then((rs)=>{
				console.log('绑定结果'+uni.getStorageSync('clientId'),rs);
			});
			this.itoken = res.data.token;
			this.getData();
		},
		async uploadIdCardFront() {
			let res = await uploads({limit:1},this);
			console.log('uploadIdCardFront',res);
			if(res[0]){
				uni.showLoading({title: '身份证识别中'});
				let rs = await request({
					method:'POST',
					data:{pic:res[0],type:'front'},
					url:"/appuser/idcardorc"
				});
				uni.hideLoading();
				console.log('idcard orc',rs);
				if(rs.code){
					showMsg('this',{title:'识别失败'});
				}
				let card = rs.data.idcard;
				this.formData.name = card.realname;
				this.formData.id_card_number = card.id_card_number;
				this.formData.nation = card.nation;
				this.formData.address = card.address;
				this.formData.id_card_front = card.id_card_front;
				this.formData.age = card.age;
			}else{
				showMsg('this',{title:'上传失败'});
			}
		},
		async uploadIdCardBack() {
			if(!this.formData.id_card_number){
				showMsg('this',{title:'请先上传正面'});
				return;
			}
			let res = await uploads({limit:1},this);
			console.log('uploadIdCardBack',res);
			if(res[0]){
				uni.showLoading({title: '身份证识别中'});
				let rs = await request({
					method:'POST',
					data:{pic:res[0],type:'back',idcard:this.formData.id_card_number},
					url:"/appuser/idcardorc"
				});
				uni.hideLoading();
				console.log('idcard orc',rs);
				if(rs.code){
					showMsg('this',{title:'识别失败'});
				}
				this.formData.id_card_back = rs.data.id_card_back;
			}else{
				showMsg('this',{title:'上传失败'});
			}
		},
		async uploadIdCardHeader(){
			if(!this.formData.id_card_number){
				showMsg('this',{title:'请先上传正面'});
				return;
			}
			let res = await uploads({limit:1},this);
			console.log('uploadIdCardHeader',res);
			if(res[0]){
				uni.showLoading({title: '处理中'});
				let rs = await request({
					method:'POST',
					data:{pic:res[0],type:'header',idcard:this.formData.id_card_number},
					url:"/appuser/idcardorc"
				});
				uni.hideLoading();
				console.log('idcard orc',rs);
				if(rs.code){
					showMsg('this',{title:'处理失败'});
				}
				this.formData.id_card_header = rs.data.id_card_header;
				this.formData.id_card_header2 = rs.data.id_card_header;
			}else{
				showMsg('this',{title:'上传失败'});
			}
		},
		onSchoolPaper(e) {
			this.formData.school_paper_has = Number(e.detail.value);
		},
		selectEducation() {
			const options = ['小学', '初中', '普通高中', '技工学校', '职业高中', '中等专科', '大学专科', '大学本科','硕士研究生','博士研究生','其他'];
			uni.showActionSheet({
				itemList: options,
				success: (res) => {
					this.formData.education = options[res.tapIndex];
				}
			});
		},
		async uploadImage(field) {
			let res = await uploads({limit:1},this);
			if(res[0]){
				this.formData[field] = res[0];
			}else{
				showMsg('this',{title:'上传失败'});
			}
		},
		selectPoliticalStatus() {
			const options = ['群众', '中共党员', '中共预备党员', '共青团员', '民主党派'];
			uni.showActionSheet({
				itemList: options,
				success: (res) => {
					this.formData.political = options[res.tapIndex];
				}
			});
		},
		selectHouseholdType() {
			const options = ['农业户口', '非农业户口', '居民户口'];
			uni.showActionSheet({
				itemList: options,
				success: (res) => {
					this.formData.house_type = options[res.tapIndex];
				}
			});
		},
		createWorkTypeArray(i){
			let worklist = Object.values(this.workList);
			this.reqWorkArray[0] = ['无',...worklist];
			let workids = Object.keys(this.workList);
			if(i>0){
				let colunm = Object.values(this.workListID[workids[i-1]]);
				this.reqWorkArray[1] = ['无',...colunm];
			}
		},
		changeReqWorkType(e){
			//console.log('****',e);
			let column = e.detail.column;
			let value = e.detail.value;
			if(column == 0){
				//this.reqWorkArray[1] = this.reqWorkColumn[value];
				this.createWorkTypeArray(value);
				this.reqWorkIndex = value;
				this.reqWorkIndex1 = 0;
			}else{
				let workids = Object.keys(this.workList);
				let parentid = workids[this.reqWorkIndex-1];
				console.log(this.reqWorkIndex,'parentid='+parentid);
				let colunm = Object.keys(this.workListID[parentid]);
				this.formData.req_work_type_id = colunm[value-1];
				this.formData.req_work_type_name = this.reqWorkArray[1][value];
				this.reqWorkIndex1 = value;
				console.log(this.formData.req_work_type_id+'='+this.formData.req_work_type_name);
			}
		},
		cancelReqWorkType(e){
			this.reqWorkArray[1] = this.reqWorkColumn[0];
			this.formData.req_work_type_id = 0;
			this.formData.req_work_type_name = '';
		},
		selectJobType() {
			const options = ['电工', '焊工', '钳工', '车工', '铣工', '其他'];
			uni.showActionSheet({
				itemList: options,
				success: (res) => {
					this.formData.jobType = options[res.tapIndex];
				}
			});
		},
		selectReqWorkTypeLevel() {
			uni.showActionSheet({
				itemList: this.optionsWorkTypeLevel,
				success: (res) => {
					this.formData.req_level = res.tapIndex;
				}
			});
		},
		onWorkTypePaper(e) {
			this.formData.work_type_paper_has = Number(e.detail.value);
		},
		selectJobTitle() {
			const options = ['无', '助理工程师', '工程师', '高级工程师', '其他'];
			uni.showActionSheet({
				itemList: options,
				success: (res) => {
					this.formData.title_type = options[res.tapIndex];
				}
			});
		},
		addWorkExperience() {
			uni.showModal({
				title: '添加工作经历',
				content: '工作经历功能开发中，敬请期待',
				showCancel: false
			});
		},
		async uploadContract(field) {
			if(!this.formData.id_card_number){
				showMsg('this',{title:'请先上传正面'});
				return;
			}
			let res = await uploads({limit:1},this);
			console.log('uploadContract',res);
			if(res[0]){
				uni.showLoading({title: '处理中'});
				let rs = await request({
					method:'POST',
					data:{pic:res[0],type:field,idcard:this.formData.id_card_number,signupid:this.id},
					url:"/appuser/fileorc"
				});
				uni.hideLoading();
				console.log('uploadContract orc',rs);
				if(rs.code){
					showMsg('this',{title:'处理失败'});
				}
				this.formData[field] = rs.data.pic;
			}else{
				showMsg('this',{title:'上传失败'});
			}
		},
		async submitForm() {
			// 表单验证
			if (!this.id) {
				showMsg('this', {title: '报名计划不存在'});
				return;
			}
			if (!this.formData.phone) {
				showMsg('this', {title: '请输入手机号'});
				return;
			}
			if (!this.formData.name) {
				showMsg('this', {title: '请输入姓名'});
				return;
			}
			if (!this.formData.id_card_number) {
				showMsg('this', {title: '请输入身份证号'});
				return;
			}
			if(!this.formData.school_paper_has){
				this.formData.major = '';
				this.formData.school_paper = '';
				this.formData.school_no = '';
			}
			if(!this.formData.work_type_paper_has){
				this.formData.work_type_paper = '';
			}
			
			const res = await request({
				method: 'POST',
				url: '/appregistration/submit',
					data: {
					formData:this.formData,
					id: this.id
				}
			});
			console.log('sign up self submit',res);

			// ★ v3：只在失败时弹 Toast，成功时由 Modal 或 closeForm 提供反馈
			if (res.code) {
				showMsg('this', {title: res.msg || '提交失败'});
				return;
			}
			this.formData = res.data.formData;
			this.infoStatus = res.data.status;

			if(res.data.tips && res.data.tips.length>0){
				// ★ v3：status=0，草稿已保存，但必填项未填齐
				let text = "您的报名信息已成功提交！\n\n以下信息建议您尽快补充，以便加快审核进度：\n\n";
				for(let j in res.data.tips){
					text += (Number(j)+1)+"、"+res.data.tips[j]+"\n";
				}
				text += "\n您可在「自主预约报名」中随时补充未填信息。";
				uni.showModal({
					title:'保存成功',
					content:text,
					showCancel:false
				});
			}else{
				// ★ v3：status=1，全部填完，正式提交成功
				showMsg('this', {title: '提交成功'});
				this.closeForm();
			}
		}
	}
}
</script>

<style>
.content {
	min-height: 100vh;
}

.content.no-scroll {
	overflow: hidden;
	height: 100vh;
}

.body.no-scroll {
	overflow: hidden;
	height: calc(100vh - 50px);
}


.header {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	z-index: 9999;
	border-bottom: 1rpx solid #eee;
}

.header-box {
	height: 50px;
	padding: 0 20rpx;
}

.header-back {
	width: 60rpx;
	height: 50px;
}

.header-title {
	flex: 1;
	text-align: center;
}

.header-placeholder {
	width: 60rpx;
}

.body {
	padding: 20rpx;
}
.company-cover{
	width: 710rpx;
	margin-bottom: 10px;
}
.company-cover image{
	width: 710rpx;
	border-radius: 12rpx;
}
.info-card {
	border-radius: 12rpx;
	padding: 40rpx 30rpx;
	margin-bottom: 40rpx;
}
.company-title {
	text-align: center;
	margin-bottom: 40rpx;
}

.company-name {
	font-size: 48px;
	line-height: 1.2;
}

.company-subtitle {
	font-size: 32px;
	margin-top: 8rpx;
}

.info-section {
	margin-top: 30rpx;
	padding-bottom: 60px;
}

.info-title {
	margin-bottom: 30rpx;
}

.info-details {
	margin-top: 20rpx;
}

.info-item {
	margin-bottom: 20rpx;
	padding-bottom: 20rpx;
	border-bottom: 1rpx solid #f0f0f0;
}

.info-item:last-child {
	border-bottom: none;
	margin-bottom: 0;
	padding-bottom: 0;
}

.info-label {
	margin-bottom: 8rpx;
}

.submit-btn {
	position: fixed;
	bottom: 20rpx;
	left: 20rpx;
	right: 20rpx;
	height: 100rpx;
	border-radius: 50rpx;
}

/* 表单弹窗 */
.idcard-yz{
	margin: 10px 40rpx;
	height: 40px;
	line-height: 40px;
	text-align: center;
	color: #fff;
	background-color: #73a9fa;
	border-radius: 20px;
}
.form-popup {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: 0;
	background-color: rgba(0, 0, 0, 0.5);
	z-index: 9;
	display: none;
}

.form-popup.show {
	display: flex;
	align-items: flex-end;  /* ✅ 特殊属性flex-end，保留 */
}

.form-content {
	width: 100%;
	height: 85vh;
	border-radius: 20rpx 20rpx 0 0;
	animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
	from {
		transform: translateY(100%);
	}
	to {
		transform: translateY(0);
	}
}

.form-header {
	padding: 30rpx 30rpx;
	border-bottom: 1rpx solid #f0f0f0;
}

.form-close {
	width: 60rpx;
	height: 60rpx;
	background-color: transparent;
	border-radius: 50%;
}

.close-text {
	font-size: 24px;
	font-weight: bold;
	line-height: 1;
}

.form-body {
	flex: 1;
	height: calc(85vh - 100rpx);
	padding: 0;
}

.form-row {
	padding: 30rpx;
	border-bottom: 1rpx solid #f0f0f0;
}

.form-row-label {
	flex-shrink: 0;
}

.form-row-value {
	text-align: right;
}

.form-row-right {
	flex: 1;
	justify-content: flex-end;  /* ✅ 特殊属性flex-end，保留 */
}

.form-row-upload {
	margin-right: 10rpx;
}

.form-row-tip {
	margin-right: 10rpx;
}

.form-row-switch-text {
	margin-right: 20rpx;
}

.form-row-input {
	flex: 1;
	text-align: right;
}

.form-submit-wrapper {
	padding: 30rpx;
	padding-bottom: calc(50rpx + env(safe-area-inset-bottom));
}

.form-submit-btn {
	height: 100rpx;
	line-height: 100rpx;
	border-radius: 50rpx;
	box-shadow: 0 4rpx 12rpx rgba(79, 172, 254, 0.3);
	text-align: center;
}

.popup{
	width: 750rpx;
	border-top-left-radius: 20rpx;
	border-top-right-radius: 20rpx;
	background-color: #fff;
}
.popup-header{
	padding-top: 10px;
	position: relative;
}
.popup-close{
	position: absolute;
	top:5px;
	right: 20rpx;
}
.popup-body{
	padding: 10px 20rpx;
}
.popup-tips{
	width: 600rpx;
	background-color: #fff;
	border-radius: 12rpx;
	position: relative;
}
.popup-tips-pic{
	width: 600rpx;
	margin-top: 10px;
	padding-bottom: 10px;
}
.popup-tips-pic image{
	border: 1rpx solid #000;
}
.popup-tips-close{
	position: absolute;
	bottom: -60px;
	left: 0;
	right: 0;
	z-index: 999;
	height: 80px;
	line-height: 80px;
	text-align: center;
}
</style>
