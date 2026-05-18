import { LockOutlined } from '@ant-design/icons';
import { Button, Form, Input, Typography, message } from 'antd';
import { useMutation, useQuery } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { currentUser, login } from '../api/auth';
import { useAuthStore } from '../stores/authStore';

interface AuthGateProps {
  children: ReactNode;
}

export function AuthGate({ children }: AuthGateProps) {
  const token = useAuthStore((state) => state.token);
  const currentUser = useAuthStore((state) => state.currentUser);
  const setToken = useAuthStore((state) => state.setToken);
  const setTenantId = useAuthStore((state) => state.setTenantId);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

  const userQuery = useQuery({
    queryKey: ['current-user', token],
    queryFn: currentUser,
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
      message.success('登录成功');
    },
  });

  // Primary check: currentUser from store (set by login response on first login,
  // or by /api/auth/me on page refresh). This avoids blocking the UI on the
  // /api/auth/me round-trip after a fresh login.
  if (currentUser || userQuery.data) {
    return children;
  }

  if (token && userQuery.isLoading) {
    return <div className="boot-screen">正在载入平台权限...</div>;
  }

  return (
    <div className="login-screen">
      <div className="login-panel">
        <div className="login-title-row">
          <div className="login-mark">国</div>
          <div>
            <Typography.Title level={2}>千铅国旺云平台</Typography.Title>
            <Typography.Text>管理端登录</Typography.Text>
          </div>
        </div>
        <Form
          layout="vertical"
          initialValues={{ tenantCode: 'demo', username: 'admin', password: 'Admin@123456' }}
          onFinish={(values) => loginMutation.mutate(values)}
        >
          <Form.Item label="租户编码" name="tenantCode" rules={[{ required: true }]}>
            <Input />
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
    </div>
  );
}
