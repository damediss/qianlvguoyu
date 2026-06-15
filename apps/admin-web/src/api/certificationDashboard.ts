import type {
  CertificationDashboard,
  CertificationDashboardQuery,
} from '../types/certificationDashboard';
import { apiClient, unwrap } from './client';

export function getCertificationDashboard(params: CertificationDashboardQuery) {
  return unwrap<CertificationDashboard>(
    apiClient.get('/certification/dashboard', { params }),
  );
}
