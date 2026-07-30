/**
 * API 响应处理工具
 * 用于统一处理后端返回的 Result 对象
 * 
 * 后端返回格式：
 * {
 *   code: 1,      // 1-成功，0-失败
 *   msg: "提示信息",
 *   data: {...}   // 返回数据
 * }
 */

/**
 * 响应码常量
 */
export const RESPONSE_CODE = {
  SUCCESS: 1,  // 成功
  ERROR: 0     // 失败
}

/**
 * 判断接口响应是否成功
 * @param {Object} res - 接口响应对象
 * @returns {boolean} - 是否成功
 * 
 * @example
 * const res = await someApi();
 * if (isSuccess(res)) {
 *   console.log('成功');
 * }
 */
export function isSuccess(res) {
  return res && res.code === RESPONSE_CODE.SUCCESS;
}

/**
 * 判断接口响应是否失败
 * @param {Object} res - 接口响应对象
 * @returns {boolean} - 是否失败
 * 
 * @example
 * const res = await someApi();
 * if (isError(res)) {
 *   uni.showToast({ title: res.msg, icon: 'none' });
 * }
 */
export function isError(res) {
  return res && res.code === RESPONSE_CODE.ERROR;
}

/**
 * 获取响应数据，如果失败则返回默认值
 * @param {Object} res - 接口响应对象
 * @param {*} defaultValue - 失败时返回的默认值
 * @returns {*} - 响应数据或默认值
 * 
 * @example
 * const list = getData(res, []);
 */
export function getData(res, defaultValue = null) {
  return isSuccess(res) ? res.data : defaultValue;
}

/**
 * 显示错误提示
 * @param {Object} res - 接口响应对象
 * @param {string} defaultMsg - 默认错误提示
 * 
 * @example
 * if (isError(res)) {
 *   showError(res, '操作失败');
 * }
 */
export function showError(res, defaultMsg = '操作失败') {
  const msg = res?.msg || defaultMsg;
  uni.showToast({
    title: msg,
    icon: 'none',
    duration: 2000
  });
}

/**
 * 显示成功提示
 * @param {Object} res - 接口响应对象
 * @param {string} defaultMsg - 默认成功提示
 * 
 * @example
 * if (isSuccess(res)) {
 *   showSuccess(res, '操作成功');
 * }
 */
export function showSuccess(res, defaultMsg = '操作成功') {
  const msg = res?.msg || defaultMsg;
  uni.showToast({
    title: msg,
    icon: 'success',
    duration: 1500
  });
}
