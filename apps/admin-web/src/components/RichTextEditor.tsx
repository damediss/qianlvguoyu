import {
  BoldOutlined,
  ItalicOutlined,
  LinkOutlined,
  OrderedListOutlined,
  RedoOutlined,
  StrikethroughOutlined,
  UnderlineOutlined,
  UndoOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import { Button, Divider, Input, Space, Tooltip } from 'antd';
import { useEffect, useState } from 'react';
import { EditorContent, useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import Underline from '@tiptap/extension-underline';
import Link from '@tiptap/extension-link';
import sanitizeHtml from 'sanitize-html';
import './RichTextEditor.css';

export function sanitizeRichTextHtml(value?: string | null) {
  if (!value) return '';
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

interface RichTextEditorProps {
  value?: string | null;
  onChange?: (value: string) => void;
  placeholder?: string;
  minHeight?: number;
  disabled?: boolean;
}

export function RichTextEditor({
  value,
  onChange,
  placeholder = '请输入内容',
  minHeight = 180,
  disabled = false,
}: RichTextEditorProps) {
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
    if (!editor) return;
    editor.setEditable(!disabled);
  }, [disabled, editor]);

  useEffect(() => {
    if (!editor || value === undefined) return;
    const next = value || '<p></p>';
    if (next !== editor.getHTML()) editor.commands.setContent(next, false);
  }, [editor, value]);

  if (!editor) return null;

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
    if (linkUrl.trim()) editor.chain().focus().setLink({ href: linkUrl.trim() }).run();
    else editor.chain().focus().unsetLink().run();
    setLinkOpen(false);
  };

  return (
    <div className="rich-text-editor" style={{ minHeight }}>
      {!disabled && (
        <div className="rich-text-editor__toolbar">
          <Space size={0} wrap>
            <Tooltip title="粗体"><Button type="text" size="small" icon={<BoldOutlined />} onClick={() => editor.chain().focus().toggleBold().run()} /></Tooltip>
            <Tooltip title="斜体"><Button type="text" size="small" icon={<ItalicOutlined />} onClick={() => editor.chain().focus().toggleItalic().run()} /></Tooltip>
            <Tooltip title="下划线"><Button type="text" size="small" icon={<UnderlineOutlined />} onClick={() => editor.chain().focus().toggleUnderline().run()} /></Tooltip>
            <Tooltip title="删除线"><Button type="text" size="small" icon={<StrikethroughOutlined />} onClick={() => editor.chain().focus().toggleStrike().run()} /></Tooltip>
            <Divider type="vertical" />
            <Tooltip title="项目符号"><Button type="text" size="small" icon={<UnorderedListOutlined />} onClick={() => editor.chain().focus().toggleBulletList().run()} /></Tooltip>
            <Tooltip title="编号列表"><Button type="text" size="small" icon={<OrderedListOutlined />} onClick={() => editor.chain().focus().toggleOrderedList().run()} /></Tooltip>
            <Button type="text" size="small" onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}>标题</Button>
            <Tooltip title="链接"><Button type="text" size="small" icon={<LinkOutlined />} onClick={toggleLink} /></Tooltip>
            <Divider type="vertical" />
            <Tooltip title="撤销"><Button type="text" size="small" icon={<UndoOutlined />} disabled={!editor.can().undo()} onClick={() => editor.chain().focus().undo().run()} /></Tooltip>
            <Tooltip title="重做"><Button type="text" size="small" icon={<RedoOutlined />} disabled={!editor.can().redo()} onClick={() => editor.chain().focus().redo().run()} /></Tooltip>
          </Space>
          {linkOpen && (
            <Space.Compact className="rich-text-editor__link-input">
              <Input size="small" value={linkUrl} placeholder="https://" onChange={(event) => setLinkUrl(event.target.value)} onPressEnter={applyLink} />
              <Button size="small" type="primary" onClick={applyLink}>确定</Button>
            </Space.Compact>
          )}
        </div>
      )}
      <EditorContent editor={editor} />
    </div>
  );
}

export function RichTextPreview({ value, className }: { value?: string | null; className?: string }) {
  return <div className={className || 'rich-text-preview'} dangerouslySetInnerHTML={{ __html: sanitizeRichTextHtml(value) }} />;
}
