import { apiClient, unwrap } from './client';
export function getCertificationDashboard(params) {
    return unwrap(apiClient.get('/certification/dashboard', { params }));
}
