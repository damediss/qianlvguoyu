import { apiClient, unwrap } from './client';

export interface WeKnoraEmbedToken {
  token: string;
  expiresIn: number;
}

export function getWeKnoraEmbedToken() {
  return unwrap<WeKnoraEmbedToken>(
    apiClient.get('/ai/weknora/embed-token', { silentError: true }),
  );
}
