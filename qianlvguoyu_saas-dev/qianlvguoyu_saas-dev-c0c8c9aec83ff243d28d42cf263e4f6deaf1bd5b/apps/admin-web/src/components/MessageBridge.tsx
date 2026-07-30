import { App } from 'antd';
import { useEffect } from 'react';
import { bindAppMessage } from '../utils/appMessage';

export function MessageBridge() {
  const { message } = App.useApp();

  useEffect(() => {
    bindAppMessage(message);
  }, [message]);

  return null;
}
