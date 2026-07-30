import { request } from "@/utils/java.js";


export const getCompetitionHome = (userId) => {
	return request({
		method: 'POST',
		data: userId ? { userId } : {},
		url: '/appcompetition/home'
	}).then(res => {
		return res;
	});
};

export const getCompetitionDetail = (id) => {
	return request({
		method: 'POST',
		data: { id },
		url: '/appcompetition/detail'
	});
};

export const getCompetitionInfoList = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/info'
	});
};

export const searchCompetition = (keyword) => {
	return request({
		method: 'POST',
		data: { keyword },
		url: '/appcompetition/search'
	});
};

export const getCompetitionInfoDetail = (id) => {
	return request({
		method: 'POST',
		data: { id },
		url: '/appcompetition/infoDetail'
	});
};


export const getNotificationList = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/notificationList'
	});
};

export const getNotificationDetail = (id) => {
	return request({
		method: 'POST',
		data: { id },
		url: '/appcompetition/notificationDetail'
	});
};

export const getEntrance = (competitionId, userId) => {
	return request({
		method: 'POST',
		data: {
			competitionId: competitionId,
			userId: userId
		},
		url: '/appcompetition/entrance'
	});
};


export const checkIn = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/checkin'
	});
};

export const getCheckInHistory = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/checkinHistory'
	});
};

export const getCheckinStatistics = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/checkinStats'
	});
};

export const resetTodayCheckin = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/resetTodayCheckin'
	});
};

export const getPointDetail = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/pointDetail'
	});
};

export const getPKQuestions = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/pk/questions'
	});
};


export const submitPKResult = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/pk/submit'
	});
};

export const getTeamPKInfo = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/teampk/info'
	});
};

export const getAdventureInfo = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/adventure/info'
	});
};

export const getAdventureQuestions = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/adventure/questions'
	});
};

export const submitAdventureAnswers = (data) => {
	return request({
		method: 'POST',
		data: data,
		url: '/appcompetition/adventure/submit'
	});
};


export const getAdventureHistory = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/adventure/history'
	});
};

export const getBigAdventureInfo = (params = {}) => {
	return request({
		method: 'POST',
		data: { ...params, type: 1 },
		url: '/appcompetition/adventure/info'
	});
};

export const getBigAdventureQuestions = (params = {}) => {
	return request({
		method: 'POST',
		data: { ...params, type: 1 },
		url: '/appcompetition/adventure/questions'
	});
};

export const submitBigAdventureAnswers = (data) => {
	return request({
		method: 'POST',
		data: { ...data, type: 1 },
		url: '/appcompetition/adventure/submit',
	});
};

export const getBigAdventureHistory = (params = {}) => {
	return request({
		method: 'POST',
		data: { ...params, type: 1 },
		url: '/appcompetition/adventure/history'
	});
};


export const getCoursewareList = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/courseware/list'
	});
};


export const getCoursewareDetail = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/courseware/detail'
	});
};


export const recordCoursewareStudy = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/courseware/record'
	});
};

export const getArticleList = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/article/list'
	});
};


export const getArticleDetail = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/article/detail'
	});
};


export const recordArticleStudy = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/article/record'
	});
};


export const getWrongQuestions = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/wrongquestions/list'
	});
};


export const getFragmentQuestions = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/fragment/questions'
	});
};


export const getMockExamConfig = (matchId) => {
	return request({
		method: 'POST',
		data: { matchId },
		url: '/appcompetition/mockexam/config'
	});
};


export const getMockExamQuestions = (matchId) => {
	return request({
		method: 'POST',
		data: { matchId },
		url: '/appcompetition/mockexam/questions'
	});
};


export const submitMockExam = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/mockexam/submit'
	});
};


export const getQAList = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/qa/list'
	});
};


export const getQADetail = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/qa/detail'
	});
};


export const submitQAReply = (params = {}) => {
	return request({
		method: 'POST',
		data: params,
		url: '/appcompetition/qa/submit'
	});
};


export const checkRegistration = (userId, matchId) => {
	return request({
		method: 'POST',
		data: { userId, matchId },
		url: '/appcompetition/checkRegistration'
	});
};


export const getCompanyList = () => {
	return request({
		method: 'POST',
		data: {},
		url: '/appcompetition/company/list'
	});
};


export const submitRegistration = (data) => {
	return request({
		method: 'POST',
		data,
		url: '/appcompetition/register'
	});
};

export const getPKTest = (params = {}) => {
	return request({
		method: 'GET',
		data: params,
		url: '/appcompetition/pk/test'
	});
};