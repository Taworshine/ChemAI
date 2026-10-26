"""文档解析：PDF / Word / 纯文本 → 文本"""
import os


def parse_file(file_path: str) -> str:
    """按扩展名解析本地文件，返回提取的纯文本。文件不存在时返回空串。"""
    if not file_path or not os.path.exists(file_path):
        return ""
    ext = os.path.splitext(file_path)[1].lower()
    try:
        if ext == ".pdf":
            import pdfplumber
            parts = []
            with pdfplumber.open(file_path) as pdf:
                for page in pdf.pages:
                    parts.append(page.extract_text() or "")
            return "\n".join(parts)
        if ext in (".docx", ".doc"):
            import docx
            d = docx.Document(file_path)
            return "\n".join(p.text for p in d.paragraphs)
        # txt / md / 其他纯文本
        with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
            return f.read()
    except Exception as e:
        raise RuntimeError(f"文档解析失败：{e}")
