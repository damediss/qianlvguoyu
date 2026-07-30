import type { MessageInstance } from 'antd/es/message/interface';
import { message as staticMessage } from 'antd';

let messageApi: MessageInstance | undefined;

export function bindAppMessage(api: MessageInstance) {
  messageApi = api;
}

export const appMessage = {
  success: (content: string) => (messageApi ?? staticMessage).success(content),
  error: (content: string) => (messageApi ?? staticMessage).error(content),
  warning: (content: string) => (messageApi ?? staticMessage).warning(content),
  info: (content: string) => (messageApi ?? staticMessage).info(content),
};
