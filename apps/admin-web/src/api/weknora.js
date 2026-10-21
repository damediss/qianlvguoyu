import { apiClient, unwrap } from './client';
export function getWeKnoraEmbedToken() {
    return unwrap(apiClient.get('/ai/weknora/embed-token', { silentError: true }));
}
