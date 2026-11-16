import MarkdownIt from 'markdown-it'
import katex from '@traptitech/markdown-it-katex'
import sub from 'markdown-it-sub'
import sup from 'markdown-it-sup'
import 'katex/dist/katex.min.css'

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: true
})

// 数学公式：$...$ 行内，$$...$$ 块级
md.use(katex, { throwOnError: false, errorColor: '#cc0000' })

// 化学角标：H~2~O → H₂O，x^2^ → x²
md.use(sub)
md.use(sup)

export function renderMarkdown(text) {
  if (!text) return ''
  return md.render(String(text))
}
