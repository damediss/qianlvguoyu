import { LockOutlined } from '@ant-design/icons';
import { Button, Form, Input, Modal, Select, Typography, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useEffect, useState } from 'react';
import { currentUser as fetchCurrentUser, fetchTenants, login } from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import type { CurrentUserVO } from '../types/api';
import { SliderCaptcha } from './SliderCaptcha';

const ASSESSOR_ROLE = 'CERTIFICATION_ASSESSOR';
const ASSESSOR_PERMISSION = 'certification.practical.score';
const COLLECTION_OPERATE_PERMISSION = 'certification.practical.collection.operate';

function isAssessorAccount(user?: CurrentUserVO) {
  return Boolean(
    user?.roles?.includes(ASSESSOR_ROLE)
    && user?.permissions?.includes(ASSESSOR_PERMISSION),
  );
}

function isCollectionOperator(user?: CurrentUserVO) {
  return Boolean(user?.permissions?.includes(COLLECTION_OPERATE_PERMISSION));
}

interface AuthGateProps {
  children: ReactNode;
  mode?: 'admin' | 'assessor' | 'collection';
}

export function AuthGate({ children, mode = 'admin' }: AuthGateProps) {
  const token = useAuthStore((state) => state.token);
  const currentUser = useAuthStore((state) => state.currentUser);
  const setToken = useAuthStore((state) => state.setToken);
  const setTenantId = useAuthStore((state) => state.setTenantId);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);
  const clearAuth = useAuthStore((state) => state.clearAuth);

  const [captchaVisible, setCaptchaVisible] = useState(false);
  const [pendingLogin, setPendingLogin] = useState<{ tenantCode: string; username: string; password: string } | null>(null);

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
      const err = userQuery.error as { response?: { status?: number } };
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
      if (mode === 'assessor' && !isAssessorAccount(data.user)) {
        message.error('当前账号无平板考评端权限');
        clearAuth();
        return;
      }
      if (mode === 'collection' && !isCollectionOperator(data.user)) {
        message.error('当前账号无平板信息采集端权限');
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
    if (mode === 'assessor' && resolvedUser && !isAssessorAccount(resolvedUser)) {
      message.error('当前账号无平板考评端权限');
      clearAuth();
      return;
    }
    if (mode === 'collection' && resolvedUser && !isCollectionOperator(resolvedUser)) {
      message.error('当前账号无平板信息采集端权限');
      clearAuth();
    }
  }, [clearAuth, mode, resolvedUser]);

  // Primary check: currentUser from store (set by login response on first login,
  // or by /api/auth/me on page refresh). This avoids blocking the UI on the
  // /api/auth/me round-trip after a fresh login.
  if (resolvedUser) {
    if (mode === 'assessor' && !isAssessorAccount(resolvedUser)) {
      return <div className="boot-screen">正在校验平板考评端权限...</div>;
    }
    if (mode === 'collection' && !isCollectionOperator(resolvedUser)) {
      return <div className="boot-screen">正在校验平板信息采集端权限...</div>;
    }
    return children;
  }

  if (token && userQuery.isLoading) {
    return <div className="boot-screen">正在载入平台权限...</div>;
  }

  return (
    <div className="login-screen">
      <div className="login-panel">
        <div className="login-title-row">
          <div className="login-mark">千铝</div>
          <div>
            <Typography.Title level={2}>千铝国昱云平台</Typography.Title>
            <Typography.Text>
              {mode === 'assessor'
                ? '平板考评端登录'
                : mode === 'collection'
                  ? '平板信息采集端登录'
                  : '管理端登录'}
            </Typography.Text>
          </div>
        </div>
        <Form
          layout="vertical"
          initialValues={mode === 'admin'
            ? { tenantCode: defaultTenantCode || 'demo', username: 'admin', password: '' }
            : { tenantCode: defaultTenantCode || 'demo' }}
          onFinish={(values) => {
            setPendingLogin(values);
            setCaptchaVisible(true);
          }}
        >
          <Form.Item label="租户" name="tenantCode" rules={[{ required: true, message: '请选择租户' }]}>
            <Select
              placeholder="选择租户"
              loading={tenantsQuery.isLoading}
              options={tenantsQuery.data?.map(t => ({ label: t.name, value: t.code }))}
            />
          </Form.Item>
          <Form.Item label="账号" name="username" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true }]}>
            <Input.Password prefix={<LockOutlined />} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={loginMutation.isPending} block>
            登录
          </Button>
        </Form>
      </div>

      <Modal
        open={captchaVisible}
        onCancel={() => {
          setCaptchaVisible(false);
          setPendingLogin(null);
        }}
        footer={null}
        title="安全验证"
        width={380}
        destroyOnHidden
        centered
      >
        <SliderCaptcha
          onSuccess={(captchaToken) => {
            setCaptchaVisible(false);
            if (pendingLogin) {
              loginMutation.mutate({ ...pendingLogin, captchaToken });
              setPendingLogin(null);
            }
          }}
          onClose={() => {
            setCaptchaVisible(false);
            setPendingLogin(null);
          }}
        />
      </Modal>
    </div>
  );
}
