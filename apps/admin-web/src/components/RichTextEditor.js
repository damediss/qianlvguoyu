import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { BoldOutlined, ItalicOutlined, LinkOutlined, OrderedListOutlined, RedoOutlined, StrikethroughOutlined, UnderlineOutlined, UndoOutlined, UnorderedListOutlined, } from '@ant-design/icons';
import { Button, Divider, Input, Space, Tooltip } from 'antd';
import { useEffect, useState } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Link from '@tiptap/extension-link';
import sanitizeHtml from 'sanitize-html';
import './RichTextEditor.css';
export function sanitizeRichTextHtml(value) {
    if (!value)
        return '';
    return sanitizeHtml(value, {
        allowedTags: [
            'p', 'br', 'strong', 'em', 'u', 's', 'blockquote', 'ul', 'ol', 'li',
            'h1', 'h2', 'h3', 'a', 'span', 'div',
        ],
        allowedAttributes: { a: ['href', 'title', 'target', 'rel'], '*': ['class'] },
        allowedSchemes: ['http', 'https', 'mailto'],
        allowProtocolRelative: false,
    });
}
export function RichTextEditor({ value, onChange, placeholder = '请输入内容', minHeight = 180, disabled = false, }) {
    const [linkOpen, setLinkOpen] = useState(false);
    const [linkUrl, setLinkUrl] = useState('');
    const editor = useEditor({
        extensions: [
            StarterKit.configure({ heading: { levels: [1, 2, 3] } }),
            Underline,
            Link.configure({ openOnClick: false, autolink: true, protocols: ['http', 'https', 'mailto'] }),
        ],
        content: value || '<p></p>',
        editable: !disabled,
        editorProps: {
            attributes: { class: 'rich-text-editor__content', 'data-placeholder': placeholder },
        },
        onUpdate: ({ editor: current }) => onChange?.(current.getHTML()),
    });
    useEffect(() => {
        if (!editor)
            return;
        editor.setEditable(!disabled);
    }, [disabled, editor]);
    useEffect(() => {
        if (!editor || value === undefined)
            return;
        const next = value || '<p></p>';
        if (next !== editor.getHTML())
            editor.commands.setContent(next, false);
    }, [editor, value]);
    if (!editor)
        return null;
    const toggleLink = () => {
        if (editor.isActive('link')) {
            editor.chain().focus().unsetLink().run();
            setLinkOpen(false);
            return;
        }
        setLinkUrl(editor.getAttributes('link').href || '');
        setLinkOpen(true);
    };
    const applyLink = () => {
        if (linkUrl.trim())
            editor.chain().focus().setLink({ href: linkUrl.trim() }).run();
        else
            editor.chain().focus().unsetLink().run();
        setLinkOpen(false);
    };
    return (_jsxs("div", { className: "rich-text-editor", style: { minHeight }, children: [!disabled && (_jsxs("div", { className: "rich-text-editor__toolbar", children: [_jsxs(Space, { size: 0, wrap: true, children: [_jsx(Tooltip, { title: "\u7C97\u4F53", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(BoldOutlined, {}), onClick: () => editor.chain().focus().toggleBold().run() }) }), _jsx(Tooltip, { title: "\u659C\u4F53", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(ItalicOutlined, {}), onClick: () => editor.chain().focus().toggleItalic().run() }) }), _jsx(Tooltip, { title: "\u4E0B\u5212\u7EBF", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(UnderlineOutlined, {}), onClick: () => editor.chain().focus().toggleUnderline().run() }) }), _jsx(Tooltip, { title: "\u5220\u9664\u7EBF", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(StrikethroughOutlined, {}), onClick: () => editor.chain().focus().toggleStrike().run() }) }), _jsx(Divider, { type: "vertical" }), _jsx(Tooltip, { title: "\u9879\u76EE\u7B26\u53F7", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(UnorderedListOutlined, {}), onClick: () => editor.chain().focus().toggleBulletList().run() }) }), _jsx(Tooltip, { title: "\u7F16\u53F7\u5217\u8868", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(OrderedListOutlined, {}), onClick: () => editor.chain().focus().toggleOrderedList().run() }) }), _jsx(Button, { type: "text", size: "small", onClick: () => editor.chain().focus().toggleHeading({ level: 2 }).run(), children: "\u6807\u9898" }), _jsx(Tooltip, { title: "\u94FE\u63A5", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(LinkOutlined, {}), onClick: toggleLink }) }), _jsx(Divider, { type: "vertical" }), _jsx(Tooltip, { title: "\u64A4\u9500", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(UndoOutlined, {}), disabled: !editor.can().undo(), onClick: () => editor.chain().focus().undo().run() }) }), _jsx(Tooltip, { title: "\u91CD\u505A", children: _jsx(Button, { type: "text", size: "small", icon: _jsx(RedoOutlined, {}), disabled: !editor.can().redo(), onClick: () => editor.chain().focus().redo().run() }) })] }), linkOpen && (_jsxs(Space.Compact, { className: "rich-text-editor__link-input", children: [_jsx(Input, { size: "small", value: linkUrl, placeholder: "https://", onChange: (event) => setLinkUrl(event.target.value), onPressEnter: applyLink }), _jsx(Button, { size: "small", type: "primary", onClick: applyLink, children: "\u786E\u5B9A" })] }))] })), _jsx(EditorContent, { editor: editor })] }));
}
export function RichTextPreview({ value, className }) {
    return _jsx("div", { className: className || 'rich-text-preview', dangerouslySetInnerHTML: { __html: sanitizeRichTextHtml(value) } });
}
