export function isAnsweredLocal(type, answer) {
    if (type === 'fill') {
        return Boolean(answer.blanks?.some((item) => item && item.trim()));
    }
    return (answer.selectedOptionIds || []).length > 0;
}
