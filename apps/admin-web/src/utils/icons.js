import { jsx as _jsx } from "react/jsx-runtime";
import * as Icons from '@ant-design/icons';
/** Curated icon list (~44 icons used across the project seed data + common ones). */
export const CURATED_ICONS = [
    // From seed data migrations
    'DashboardOutlined',
    'SettingOutlined',
    'BookOutlined',
    'ScheduleOutlined',
    'BranchesOutlined',
    'RobotOutlined',
    'DatabaseOutlined',
    'SafetyCertificateOutlined',
    'SearchOutlined',
    'SafetyOutlined',
    'FileSearchOutlined',
    'ProjectOutlined',
    'TableOutlined',
    'ExperimentOutlined',
    'ApiOutlined',
    'AuditOutlined',
    'UserOutlined',
    'TeamOutlined',
    'SolutionOutlined',
    'FolderOutlined',
    'DollarOutlined',
    'ClockCircleOutlined',
    'BankOutlined',
    'GroupOutlined',
    'FileTextOutlined',
    'TagsOutlined',
    'FormOutlined',
    'UserAddOutlined',
    'ScanOutlined',
    'FileOutlined',
    'ApartmentOutlined',
    'ProfileOutlined',
    'IdcardOutlined',
    'ContactsOutlined',
    'LockOutlined',
    'ReadOutlined',
    'CheckCircleOutlined',
    'SwapOutlined',
    'AppstoreOutlined',
    'OrderedListOutlined',
    // Additional common icons
    'HomeOutlined',
    'PlusCircleOutlined',
    'EditOutlined',
    'DeleteOutlined',
    'NotificationOutlined',
];
/** Map icon string name (e.g. "BookOutlined") to a React component at runtime. */
export function iconFromName(name) {
    if (!name)
        return null;
    const IconComp = Icons[name];
    return IconComp ? _jsx(IconComp, {}) : null;
}
