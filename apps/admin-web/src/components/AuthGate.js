import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { LockOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Typography, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { currentUser as fetchCurrentUser, fetchTenants, login } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import { SliderCaptcha } from './SliderCaptcha';
const ASSESSOR_ROLE = 'CERTIFICATION_ASSESSOR';
const ASSESSOR_PERMISSION = 'certification.practical.score';
const DEFENSE_REVIEW_PERMISSION = 'certification.defense.review';
const COLLECTION_OPERATE_PERMISSION = 'certification.practical.collection.operate';
const COMPETITION_JUDGE_PERMISSION = 'competition:practical-judge:score';
function isAssessorAccount(user) {
    return Boolean(user?.roles?.includes(ASSESSOR_ROLE)
        && (user?.permissions?.includes(ASSESSOR_PERMISSION)
            || user?.permissions?.includes(DEFENSE_REVIEW_PERMISSION)));
}
function isCollectionOperator(user) {
    return Boolean(user?.permissions?.includes(COLLECTION_OPERATE_PERMISSION));
}
function isCompetitionJudge(user) {
    return Boolean(user?.permissions?.includes(COMPETITION_JUDGE_PERMISSION));
}
export function AuthGate({ children, mode = 'admin' }) {
    const token = useAuthStore((state) => state.token);
    const currentUser = useAuthStore((state) => state.currentUser);
    const setToken = useAuthStore((state) => state.setToken);
    const setTenantId = useAuthStore((state) => state.setTenantId);
    const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const [captchaVisible, setCaptchaVisible] = useState(false);
    const [pendingLogin, setPendingLogin] = useState(null);
    const userQuery = useQuery({
        queryKey: ['current-user', token],
        queryFn: fetchCurrentUser,
        enabled: Boolean(token),
        retry: false,
    });
    // Sync /api/auth/me result to store (used on page refresh)
    useEffect(() => {
        if (userQuery.data) {
            setCurrentUser(userQuery.data);
        }
    }, [setCurrentUser, userQuery.data]);
    // Silently clear expired token when /api/auth/me returns 401
    useEffect(() => {
        if (userQuery.isError && token && userQuery.error) {
            const err = userQuery.error;
            if (err?.response?.status === 401) {
                setToken(undefined);
                setCurrentUser(undefined);
            }
        }
    }, [userQuery.isError, userQuery.error, token, setToken, setCurrentUser]);
    const loginMutation = useMutation({
        mutationFn: login,
        onSuccess: (data) => {
            setToken(data.accessToken);
            setTenantId(data.tenantId);
            setCurrentUser(data.user);
            if ((mode === 'assessor' || mode === 'defense') && !isAssessorAccount(data.user)) {
                message.error(mode === 'defense' ? '当前账号无考评员综合答辩端权限' : '当前账号无平板考评端权限');
                clearAuth();
                return;
            }
            if (mode === 'collection' && !isCollectionOperator(data.user)) {
                message.error('当前账号无平板信息采集端权限');
                clearAuth();
                return;
            }
            if (mode === 'competitionJudge' && !isCompetitionJudge(data.user)) {
                message.error('当前账号无竞赛实操裁判端权限');
                clearAuth();
                return;
            }
            if (mode === 'admin' && isAssessorAccount(data.user)) {
                message.success('登录成功');
                window.location.replace('/assessor-scoring');
                return;
            }
            message.success('登录成功');
        },
    });
    // 获取租户列表
    const tenantsQuery = useQuery({
        queryKey: ['tenants'],
        queryFn: fetchTenants,
    });
    // 默认选中"千铝工投"租户
    const defaultTenantCode = tenantsQuery.data?.find(t => t.name.includes('千铝工投'))?.code
        || tenantsQuery.data?.[0]?.code;
    const resolvedUser = currentUser || userQuery.data;
    useEffect(() => {
        if ((mode === 'assessor' || mode === 'defense') && resolvedUser && !isAssessorAccount(resolvedUser)) {
            message.error(mode === 'defense' ? '当前账号无考评员综合答辩端权限' : '当前账号无平板考评端权限');
            clearAuth();
            return;
        }
        if (mode === 'collection' && resolvedUser && !isCollectionOperator(resolvedUser)) {
            message.error('当前账号无平板信息采集端权限');
            clearAuth();
        }
        if (mode === 'competitionJudge' && resolvedUser && !isCompetitionJudge(resolvedUser)) {
            message.error('当前账号无竞赛实操裁判端权限');
            clearAuth();
        }
    }, [clearAuth, mode, resolvedUser]);
    // Primary check: currentUser from store (set by login response on first login,
    // or by /api/auth/me on page refresh). This avoids blocking the UI on the
    // /api/auth/me round-trip after a fresh login.
    if (resolvedUser) {
        if ((mode === 'assessor' || mode === 'defense') && !isAssessorAccount(resolvedUser)) {
            return _jsx("div", { className: "boot-screen", children: "\u6B63\u5728\u6821\u9A8C\u8003\u8BC4\u5458\u6743\u9650..." });
        }
        if (mode === 'collection' && !isCollectionOperator(resolvedUser)) {
            return _jsx("div", { className: "boot-screen", children: "\u6B63\u5728\u6821\u9A8C\u5E73\u677F\u4FE1\u606F\u91C7\u96C6\u7AEF\u6743\u9650..." });
        }
        if (mode === 'competitionJudge' && !isCompetitionJudge(resolvedUser)) {
            return _jsx("div", { className: "boot-screen", children: "\u6B63\u5728\u6821\u9A8C\u7ADE\u8D5B\u88C1\u5224\u6743\u9650..." });
        }
        return children;
    }
    if (token && userQuery.isLoading) {
        return _jsx("div", { className: "boot-screen", children: "\u6B63\u5728\u8F7D\u5165\u5E73\u53F0\u6743\u9650..." });
    }
    return (_jsxs("div", { className: "login-screen", children: [_jsxs("div", { className: `login-panel${mode === 'defense' ? ' login-panel-phone' : ''}`, children: [_jsxs("div", { className: "login-title-row", children: [_jsx("div", { className: "login-mark", children: "\u5343\u94DD" }), _jsxs("div", { children: [_jsx(Typography.Title, { level: 2, children: "\u5343\u94DD\u56FD\u6631\u4E91\u5E73\u53F0" }), _jsx(Typography.Text, { children: mode === 'assessor'
                                            ? '平板考评端登录'
                                            : mode === 'defense'
                                                ? '考评员综合答辩端登录'
                                                : mode === 'collection'
                                                    ? '平板信息采集端登录'
                                                    : mode === 'competitionJudge'
                                                        ? '竞赛实操裁判端登录'
                                                        : '管理端登录' })] })] }), _jsxs(Form, { layout: "vertical", initialValues: mode === 'admin'
                            ? { tenantCode: defaultTenantCode || 'demo', username: 'admin', password: '' }
                            : { tenantCode: defaultTenantCode || 'demo' }, onFinish: (values) => {
                            setPendingLogin(values);
                            setCaptchaVisible(true);
                        }, children: [_jsx(Form.Item, { label: "\u79DF\u6237", name: "tenantCode", rules: [{ required: true, message: '请选择租户' }], children: _jsx(Select, { placeholder: "\u9009\u62E9\u79DF\u6237", loading: tenantsQuery.isLoading, options: tenantsQuery.data?.map(t => ({ label: t.name, value: t.code })) }) }), _jsx(Form.Item, { label: "\u8D26\u53F7", name: "username", rules: [{ required: true }], children: _jsx(Input, {}) }), _jsx(Form.Item, { label: "\u5BC6\u7801", name: "password", rules: [{ required: true }], children: _jsx(Input.Password, { prefix: _jsx(LockOutlined, {}) }) }), _jsx(Button, { type: "primary", htmlType: "submit", loading: loginMutation.isPending, block: true, children: "\u767B\u5F55" })] })] }), _jsx(Modal, { open: captchaVisible, onCancel: () => {
                    setCaptchaVisible(false);
                    setPendingLogin(null);
                }, footer: null, title: "\u5B89\u5168\u9A8C\u8BC1", width: 380, destroyOnHidden: true, centered: true, children: _jsx(SliderCaptcha, { onSuccess: (captchaToken) => {
                        setCaptchaVisible(false);
                        if (pendingLogin) {
                            loginMutation.mutate({ ...pendingLogin, captchaToken });
                            setPendingLogin(null);
                        }
                    }, onClose: () => {
                        setCaptchaVisible(false);
                        setPendingLogin(null);
                    } }) })] }));
}
