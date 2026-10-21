/** 将课程管理工种树转换为有效叶子工种选项，值使用稳定 ID，标签显示完整路径。 */
export function flattenWorkTypeOptions(nodes) {
    const options = [];
    visitWorkTypes(nodes, '', options, false);
    return options;
}
/** 考评员授权可选树中任一有效节点，并展示父级路径。 */
export function flattenAssessorWorkTypeOptions(nodes) {
    const options = [];
    visitWorkTypes(nodes, '', options, true);
    return options;
}
export function workTypeTitleForId(nodes, id) {
    if (id == null)
        return undefined;
    const match = flattenAssessorWorkTypeOptions(nodes).find((option) => option.value === String(id));
    return match?.title;
}
/** Resolve legacy names by exact title, defaulting duplicate titles to their unique top-level occupation. */
export function uniqueWorkTypeIdForTitle(nodes, title) {
    const normalized = title?.trim();
    if (!normalized)
        return undefined;
    const matches = [];
    const collectMatches = (items) => {
        items.forEach((node) => {
            if (node.status !== 1)
                return;
            if (node.title?.trim() === normalized)
                matches.push(node);
            collectMatches(node.children ?? []);
        });
    };
    collectMatches(nodes);
    if (matches.length === 1)
        return String(matches[0].id);
    const topLevelMatches = matches.filter((node) => node.parentId == null || node.parentId <= 0);
    return topLevelMatches.length === 1 ? String(topLevelMatches[0].id) : undefined;
}
function visitWorkTypes(nodes, parentPath, options, includeParents) {
    nodes.forEach((node) => {
        if (node.status !== 1 || !node.title)
            return;
        const path = parentPath ? `${parentPath} / ${node.title}` : node.title;
        const activeChildren = (node.children ?? []).filter((child) => child.status === 1);
        if (includeParents || activeChildren.length === 0) {
            options.push({ value: String(node.id), label: path, title: node.title });
        }
        visitWorkTypes(activeChildren, path, options, includeParents);
    });
}
