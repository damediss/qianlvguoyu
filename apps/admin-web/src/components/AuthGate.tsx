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
  const setToken = useAuthStore((state) => state.setToken);
  const setCurrentUser = useAuthStore((state) => state.setCurrentUser);

  const userQuery = useQuery({
    queryKey: ['current-user', token],
    queryFn: currentUser,
    enabled: Boolean(token),
    retry: false,
  });

  useEffect(() => {
    if (userQuery.data) {
      setCurrentUser(userQuery.data);
    }
  }, [setCurrentUser, userQuery.data]);

  const loginMutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setToken(data.accessToken);
      message.success('登录成功');
    },
  });

  if (userQuery.data) {
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
            <Typography.Title level={2}>千铝国昱云平台</Typography.Title>
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
