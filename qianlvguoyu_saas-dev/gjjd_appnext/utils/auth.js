/**
 * Token 认证共享模块
 * 提供 token 校验、静默刷新、失效拦截、WebSocket 重绑等能力
 * 供 utils/index.js 和 utils/java.js 共同引入
 *
 * 合并自：本地版（retryQueue + refreshFailCount + Pinia 同步 + redirectToLogin 替换 navigateTo）
 *        远程版（silentLogin 独立函数 + deviceId/deviceInfo + getCachedPassword）
 */

import util from "@/utils/util.js";
import { useUserStore } from '@/store/user';
import { getDeviceId, getDeviceInfo } from '@/utils/device.js';

// ======================== 状态 ========================

/** 内存中短暂保留的密码（App 重启即丢，不持久化） */
export let cachedPassword = null;

/** 是否正在重登中（防并发重入） */
let isRefreshing = false;

/** 等待重登成功的请求队列 */
let retryQueue = [];

/** 连续失败计数 */
let refreshFailCount = 0;

/** 连续失败上限 */
const MAX_REFRESH_FAIL = 3;

// ======================== 内存密码管理 ========================

/**
 * 设置内存中的缓存密码（登录成功后调用，App 重启即丢）
 * @param {string} password
 */
export function setCachedPassword(password) {
  cachedPassword = password;
}

/**
 * 读取内存中的缓存密码
 * @returns {string|null}
 */
export function getCachedPassword() {
  return cachedPassword;
}

// ======================== 工具函数 ========================

/**
 * 校验 token 是否合法
 * @param {string} token
 * @returns {boolean}
 */
export function isValidToken(token) {
  return token && typeof token === 'string' && token.length >= 10;
}

/**
 * 判断是否"被踢下线"（与区分自然过期）
 * 被踢下线的提示不静默重登，避免两端互踢拉锯
 * @param {string} msg 后端返回的 msg
 * @returns {boolean}
 */
export function isKickedOut(msg) {
  if (!msg || typeof msg !== 'string') return false;
  return /其他设备|已.*登录|踢|下线/.test(msg);
}

/**
 * URL 里的 istate 参数替换为新 token
 * @param {string} url 原始 URL
 * @param {string} newToken 新 token
 * @returns {string}
 */
export function updateTokenInUrl(url, newToken) {
  if (!url) return url;
  // 如果 URL 里没有 istate，直接追加
  if (url.indexOf('istate=') === -1) {
    return url + (url.indexOf('?') === -1 ? '?' : '&') + 'istate=' + newToken;
  }
  return url.replace(/istate=[^&]*/, `istate=${newToken}`);
}

/**
 * 清除本地凭证（内存 + storage + Pinia store）
 */
export function clearLocalCredentials() {
  cachedPassword = null;
  const store = useUserStore();
  store.logout();
}

/**
 * 读取本地缓存的登录凭证
 * @returns {{ phone: string|null, password: string|null }}
 */
export function getLocalCredentials() {
  const phone = uni.getStorageSync('iusername');
  return {
    phone: phone || null,
    password: cachedPassword || null
  };
}

// ======================== 跳转 ========================

/**
 * 跳转到登录页（用 redirectTo 替换远程版的 navigateTo，防栈溢出）
 */
export function redirectToLogin() {
  // 已经在登录页则不重复跳转
  const pages = getCurrentPages();
  if (pages.length > 0) {
    const currentPage = pages[pages.length - 1];
    if (currentPage && currentPage.route === 'pages/auth/login') {
      return;
    }
  }
  uni.redirectTo({ url: '/pages/auth/login' });
}

/**
 * 跳转到登录页 + 弹设备未授权 Modal（用于 code:2）
 * 修复：业务页先弹 Modal，确认后再跳转，避免用户不知道被踢原因
 * @param {string} msg 后端返回的提示
 */
export function redirectToLoginWithDeviceModal(msg) {
  const pages = getCurrentPages();
  const onLoginPage = pages.length > 0 &&
    pages[pages.length - 1].route === 'pages/auth/login';

  uni.showModal({
    title: '设备未授权',
    content: msg || '当前设备未授权，请联系管理员审批解绑',
    showCancel: false,
    success: () => {
      if (!onLoginPage) {
        uni.redirectTo({ url: '/pages/auth/login' });
      }
    }
  });
}

// ======================== WebSocket ========================

/**
 * 刷新后重新绑定 WebSocket
 * 注意：request 作为参数传入，避免循环依赖！
 * @param {Function} requestFn request 函数
 */
export function rebindWebSocket(requestFn) {
  const clientId = uni.getStorageSync('clientId');
  if (!clientId) return;
  // 用 skipAuth：bind 不需要用户态 token（URL 里会带 itoken 如果有的话）
  requestFn({
    url: '/appauth/bind',
    method: 'POST',
    skipAuth: true,
    data: { clientid: clientId }
  }).then(rs => {
    console.log('刷新后重新绑 WS', rs);
  }).catch(err => {
    console.log('刷新后绑 WS 失败', err);
  });
}

// ======================== 核心：静默登录 ========================

/**
 * 用缓存的手机号 + 内存密码静默登录（远程版提取为独立函数）
 * 注意：request 作为参数传入，避免循环依赖！
 * - 成功 → 写入 token + 同步用户信息 → 返回 { token: string }
 * - code:2 设备未授权 → 清凭证 + 跳登录页 + 弹 Modal → 返回 { error: 'DEVICE_UNAUTHORIZED', msg }
 * - 其他失败 → 返回 { error: '...' }
 * @param {Function} requestFn request 函数
 * @returns {Promise<{token: string}|{error: string, msg?: string}>}
 */
export async function silentLogin(requestFn) {
  const phone = uni.getStorageSync('iusername');
  if (!phone || !cachedPassword) {
    return { error: 'NO_CREDENTIALS' };
  }
  try {
    // 合并远程版优点：携带 deviceId + deviceInfo（设备绑定校验需要）
    const res = await requestFn({
      method: 'POST',
      data: {
        phone,
        password: cachedPassword,
        loginType: 'pwd',
        deviceId: getDeviceId(),
        deviceInfo: getDeviceInfo()
      },
      url: '/appauth/login',
      skipAuth: true
    });

    // 后端返回 {code:0, msg:"成功", data:{token:"xxx", user:{...}}}
    if (!res) return { error: 'REQUEST_FAILED' };

    // code:2 设备未授权 → 不重登，跳登录页 + 设备 Modal
    if (res.code === 2) {
      clearLocalCredentials();
      redirectToLoginWithDeviceModal(res.msg);
      return { error: 'DEVICE_UNAUTHORIZED', msg: res.msg };
    }

    // 其他错误（code != 0）
    if (res.code) {
      console.warn('silentLogin failed', res.msg);
      return { error: 'LOGIN_FAILED', msg: res.msg };
    }

    // 校验 token 非空（控制点 ②）
    const token = res.data && res.data.token;
    if (!isValidToken(token)) {
      return { error: 'INVALID_TOKEN' };
    }

    // 写入 token（通过 Pinia store 同步）
    setLocalToken(token);

    // 同步用户信息（如果后端返回了 user，也通过 store 同步）
    if (res.data.user && res.data.user.id) {
      const store = useUserStore();
      store.setUserId(res.data.user.id);
      store.setUserInfo(res.data.user);
    }

    return { token };
  } catch (e) {
    console.error('silentLogin error', e);
    return { error: 'EXCEPTION' };
  }
}

// ======================== 核心：确保有 token ========================

/**
 * 确保有 token（发请求前调用）
 * 注意：request 作为参数传入，避免循环依赖！
 * - 有有效 token → 直接返回
 * - 无有效 token → 尝试用缓存密码静默刷新
 * - 刷新成功 → 返回新 token
 * - 刷新失败 → 清凭证 → 非设备未授权时跳登录页 → 返回 null
 * @param {Function} requestFn request 函数
 * @returns {Promise<string|null>}
 */
export async function ensureToken(requestFn) {
  const token = uni.getStorageSync('itoken');
  if (isValidToken(token)) return token;

  // 无有效 token，尝试静默刷新
  const result = await silentLogin(requestFn);
  if (result.error) {
    clearLocalCredentials();
    // 设备未授权已由 silentLogin 处理弹窗+跳转，这里不再重复跳转
    if (result.error !== 'DEVICE_UNAUTHORIZED') {
      redirectToLogin();
    }
    return null;
  }
  return result.token;
}

// ======================== 核心：Token 失效处理 ========================

/**
 * 设置本地 token（带校验，通过 Pinia store 同步）
 * @param {string} token
 * @returns {boolean} 是否成功写入
 */
export function setLocalToken(token) {
  if (!isValidToken(token)) return false;
  const store = useUserStore();
  store.setToken(token);
  return true;
}

/**
 * 标记登录成功（写入 token + 存储用户名 + 缓存密码到内存）
 * @param {object} userData
 * @param {string} userData.token 新 token
 * @param {object} userData.user 用户信息
 * @param {string} userData.user.phone 手机号
 * @param {string} password 密码（缓存到内存）
 */
export function markLoginSuccess(userData, password) {
  if (!isValidToken(userData.token)) {
    console.error('markLoginSuccess: token 无效', userData.token);
    return false;
  }
  setLocalToken(userData.token);
  uni.setStorageSync('iusername', userData.user?.phone || '');
  if (password) {
    cachedPassword = password;
  }
  refreshFailCount = 0;
  return true;
}

/**
 * Token 失效处理
 * 注意：request 作为参数传入，避免循环依赖！
 * @param {object} originalConfig 原始请求配置
 * @param {string} originalUrl 完整 URL
 * @param {Function} requestFn request 函数
 * @returns {Promise<any|false>} 重试结果，false 表示失败
 */
export async function handleTokenExpired(originalConfig, originalUrl, requestFn) {
  // 连续失败次数兜底
  if (refreshFailCount >= MAX_REFRESH_FAIL) {
    clearLocalCredentials();
    redirectToLogin();
    return false;
  }

  // 已在重登中 → 排队
  if (isRefreshing) {
    return new Promise(resolve => {
      retryQueue.push(async () => {
        const newToken = uni.getStorageSync('itoken');
        if (isValidToken(newToken)) {
          const newUrl = updateTokenInUrl(originalUrl, newToken);
          try {
            const result = await requestFn({ ...originalConfig, _url: newUrl });
            resolve(result);
          } catch (e) {
            resolve(false);
          }
        } else {
          resolve(false);
        }
      });
    });
  }

  // 开始重登
  isRefreshing = true;

  try {
    // 调用独立 silentLogin（含 code:2 设备未授权处理）
    const result = await silentLogin(requestFn);

    // 静默登录失败（含 code:2 已处理跳转）
    if (result.error) {
      refreshFailCount++;
      isRefreshing = false;
      clearLocalCredentials();              // 先清凭证，再唤醒队列，避免回调拿旧 token 重试
      const queue = retryQueue;
      retryQueue = [];
      queue.forEach(cb => cb());            // 唤醒队列，回调 resolve(false)，避免 Promise 悬挂

      // 设备未授权已由 silentLogin 处理弹窗+跳转，这里不再重复跳转
      if (result.error !== 'DEVICE_UNAUTHORIZED') {
        const pages = getCurrentPages();
        const currentPage = pages[pages.length - 1];
        if (!currentPage || currentPage.route !== 'pages/auth/login') {
          redirectToLogin();
        }
      }
      return false;
    }

    const newToken = result.token;

    // 成功：重置失败计数，重绑 WS
    refreshFailCount = 0;
    rebindWebSocket(requestFn);

    // 更新原请求 URL，重试
    const newUrl = updateTokenInUrl(originalUrl, newToken);
    isRefreshing = false;

    // 唤醒队列中所有等待的请求
    retryQueue.forEach(cb => cb());
    retryQueue = [];

    // 重试原请求
    return await requestFn({ ...originalConfig, _url: newUrl });

  } catch (err) {
    refreshFailCount++;
    clearLocalCredentials();
    isRefreshing = false;
    const queue = retryQueue;
    retryQueue = [];
    queue.forEach(cb => cb());              // 唤醒队列，避免 Promise 悬挂
    redirectToLogin();
    return false;
  }
}

/**
 * 检查响应是否需要拦截（token 失效）
 * @param {object} data 响应 data
 * @returns {boolean}
 */
export function isLoginExpired(data) {
  return data && data.code === -1 && /登陆过期|登录过期/.test(data.msg || '');
}
