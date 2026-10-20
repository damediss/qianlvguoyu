import { useCallback } from 'react';
import { pinyin } from 'pinyin-pro';
/**
 * Hook that auto-generates menu fields (code, path, component, permissionCode)
 * based on Chinese name input using pinyin conversion.
 */
export function usePinyinAutoGen(form, treeData) {
    const onNameChange = useCallback((name) => {
        if (!name || !name.trim())
            return;
        // Convert to pinyin array, no tones
        const segments = pinyin(name, { toneType: 'none', type: 'array' })
            .map((s) => s.toLowerCase())
            .filter((s) => /^[a-zA-Z]+$/.test(s));
        if (segments.length === 0)
            return;
        // code: snake_case (e.g., "yong_hu_guan_li")
        const code = segments.join('_');
        // path: kebab-case with / prefix (e.g., "/yong-hu-guan-li")
        const path = '/' + segments.join('-');
        // component: {parentCode}/PascalCasePage
        const pascalName = segments.map((s) => s.charAt(0).toUpperCase() + s.slice(1)).join('');
        const parentId = form.getFieldValue('parentId');
        const parentNode = findNodeById(treeData, parentId);
        const prefix = parentNode?.code || segments[0];
        const component = `${prefix}/${pascalName}Page`;
        // permissionCode: full pinyin joined lowercase + :view (e.g., "ziyuanguanli:view")
        const permissionCode = segments.join('') + ':view';
        form.setFieldsValue({ code, path, component, permissionCode });
    }, [form, treeData]);
    return { onNameChange };
}
/** Find a node in the tree by ID. */
function findNodeById(nodes, id) {
    if (id === undefined || id === null || id === 0 || id === '')
        return null;
    for (const node of nodes) {
        if (node.id === id)
            return node;
        if (node.children?.length) {
            const found = findNodeById(node.children, id);
            if (found)
                return found;
        }
    }
    return null;
}
