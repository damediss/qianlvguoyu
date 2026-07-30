import { request } from "@/utils";

/**
 * 提升模块相关API接口
 */

// 获取提升页面数据
export const getPromoteData = () => {
	return request({
		method: 'POST',
		data: {},
		url: '/apppromote/index'
	});
};

// 获取培训任务列表
export const getTrainingList = () => {
	return request({
		method: 'POST',
		data: {},
		url: '/apppromote/trainingList'
	});
};

// 获取培训计划列表
export const getTrainingPlanList = () => {
	return request({
		method: 'POST',
		data: {},
		url: '/apppromote/trainingPlanList'
	});
};

// 获取竞赛任务列表
export const getCompetitionTaskList = () => {
	return request({
		method: 'POST',
		data: {},
		url: '/apppromote/competitionTaskList'
	});
};
