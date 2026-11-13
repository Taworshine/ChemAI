<template>
  <div class="chat">
    <!-- 会话列表 -->
    <div class="soft-card side">
      <div class="side-head">
        <span>历史会话</span>
        <el-button size="small" type="primary" round @click="newSession">+ 新会话</el-button>
      </div>
      <div class="sessions">
        <div v-for="s in sessions" :key="s.id" class="session" :class="{ on: s.id === currentSessionId }"
             @click="openSession(s.id)">
          <div class="s-title">{{ s.summary || '自由提问' }}</div>
          <div class="s-sum">{{ fmtTime(s.createTime) }}</div>
          <div class="s-actions" @click.stop>
            <span class="s-btn" title="重命名" @click="renameSession(s)">✎</span>
            <span class="s-btn del" title="删除" @click="deleteSession(s)">🗑</span>
          </div>
        </div>
        <el-empty v-if="!sessions.length" description="点击右上角开始新对话" :image-size="70" />
      </div>
    </div>

    <!-- 聊天区 -->
    <div class="soft-card main">
      <div class="banner">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M9 3h6M10 3v5.5L4.5 18a2 2 0 001.8 3h11.4a2 2 0 001.8-3L14 8.5V3" stroke="#4a7dff" stroke-width="1.8" stroke-linecap="round"/>
        </svg>
        AI 思维引导者 · 我会用追问启发你自己找到答案
      </div>
      <div class="msgs" ref="msgBox">
        <div v-if="!messages.length && !streaming" class="starter">
          <p class="st-title">从哪里开始都可以，比如：</p>
          <div class="st-chips">
            <span v-for="q in starters" :key="q" class="st-chip" @click="input = q">{{ q }}</span>
          </div>
        </div>
        <template v-for="m in messages" :key="m.id">
          <div class="row me" v-if="m.role === 'USER'">
            <div class="bubble">{{ m.content }}</div>
            <div class="av me">我</div>
          </div>
          <div class="row ai" v-else>
            <div class="av ai-av">AI</div>
            <div class="bubble">
              {{ m.content }}
              <div class="meta" v-if="m.misconceptionType">
                <el-tag size="small" type="warning" effect="light">卡点：{{ misText(m.misconceptionType) }}</el-tag>
              </div>
            </div>
          </div>
        </template>
        <div class="row ai" v-if="streaming || thinking">
          <div class="av ai-av">AI</div>
          <div class="bubble"><span v-if="thinking" class="thinking">正在思考 ···</span>{{ streaming }}<span class="cursor">▍</span></div>
        </div>
      </div>
      <div class="input-bar">
        <el-input v-model="input" type="textarea" :rows="2" resize="none"
                  placeholder="输入你的问题，例如：Cu + HNO3 怎么配平？我不会直接给答案哦～"
                  @keyup.enter.exact="send" />
        <el-button type="primary" size="large" round :loading="sending" :disabled="!!streaming" @click="send">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const sessions = ref([])
const messages = ref([])
const currentSessionId = ref(null)
const input = ref('')
const sending = ref(false)
const streaming = ref('')
const thinking = ref(false)
const msgBox = ref(null)

const misText = t => ({ CONCEPT_UNCLEAR: '概念不清', CALC_ERROR: '计算错误', LOGIC_ERROR: '逻辑错误' }[t] || t)

const starters = [
  '化学平衡移动怎么判断？',
  '函数单调性我总搞混，能举个例子吗？',
  '牛顿第二定律里的合力怎么找？'
]

function fmtTime(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  if (isNaN(d)) return String(t).slice(0, 10)
  const today = new Date()
  const sameDay = d.toDateString() === today.toDateString()
  const hm = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
  return sameDay ? hm : `${d.getMonth() + 1}月${d.getDate()}日 ${hm}`
}

async function loadSessions() {
  const res = await api.get('/agent/sessions')
  sessions.value = res.data
}

async function newSession() {
  const res = await api.post('/agent/session', {})
  currentSessionId.value = res.data.id
  messages.value = []
  loadSessions()
}

async function openSession(id) {
  currentSessionId.value = id
  const res = await api.get(`/agent/history/${id}`)
  messages.value = res.data
  scrollBottom()
}

async function renameSession(s) {
  try {
    const { value } = await ElMessageBox.prompt('', '重命名对话', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: s.summary || '',
      inputValidator: v => (v && v.trim()) ? true : '名称不能为空',
      customClass: 'chat-msgbox chat-prompt-box'
    })
    await api.put(`/agent/session/${s.id}/summary`, { summary: value.trim() })
    ElMessage.success('已重命名')
    loadSessions()
  } catch (e) {
    // 用户取消不提示
  }
}

async function deleteSession(s) {
  try {
    await ElMessageBox.confirm(
      '删除后，该对话将不可恢复',
      '删除对话',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'chat-msgbox chat-delete-box'
      }
    )
    await api.delete(`/agent/session/${s.id}`)
    ElMessage.success('已删除')
    // 若删除的是当前会话，清空消息区并打开第一个会话或新建
    if (s.id === currentSessionId.value) {
      currentSessionId.value = null
      messages.value = []
      await loadSessions()
      if (sessions.value.length) openSession(sessions.value[0].id)
    } else {
      loadSessions()
    }
  } catch (e) {
    // 用户取消不提示
  }
}

async function send() {
  if (!input.value.trim()) return
  if (!currentSessionId.value) await newSession()
  const text = input.value
  input.value = ''
  // 乐观渲染：立即把用户提问插入消息列表，等待 AI 回复时用户能看到自己的话
  messages.value.push({ id: Date.now(), role: 'USER', content: text })
  scrollBottom()
  sending.value = true
  streaming.value = ''
  thinking.value = true
  try {
    const resp = await fetch('/api/agent/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', satoken: localStorage.getItem('satoken') },
      body: JSON.stringify({ sessionId: currentSessionId.value, message: text })
    })
    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })
      const events = buffer.split('\n\n')
      buffer = events.pop()
      for (const ev of events) {
        const lines = ev.split('\n')
        const nameLine = lines.find(l => l.startsWith('event:'))
        const dataLine = lines.find(l => l.startsWith('data:'))
        if (!dataLine) continue
        const name = nameLine ? nameLine.slice(6).trim() : 'message'
        const data = dataLine.slice(5).trim()
        if (name === 'message') { thinking.value = false; streaming.value += data; scrollBottom() }
        else if (name === 'error') { thinking.value = false; ElMessage.error(data) }
      }
    }
    await openSession(currentSessionId.value)
    loadSessions()
  } finally {
    sending.value = false
    streaming.value = ''
    thinking.value = false
  }
}

function scrollBottom() {
  nextTick(() => { msgBox.value.scrollTop = msgBox.value.scrollHeight })
}

onMounted(async () => {
  await loadSessions()
  if (sessions.value.length) openSession(sessions.value[0].id)
  else newSession()
  // 从高频问题跳入：自动带上问题
  const q = new URLSearchParams(location.search).get('question')
  if (q) input.value = q
})
</script>

<style scoped>
.chat { display: flex; gap: 16px; height: calc(100vh - 104px); }
.side { width: 270px; display: flex; flex-direction: column; padding: 14px; }
.side-head { display: flex; justify-content: space-between; align-items: center; font-weight: 600; padding: 4px 6px 12px; }
.sessions { flex: 1; overflow-y: auto; }
.session { padding: 10px 12px; border-radius: 10px; cursor: pointer; margin-bottom: 4px; transition: background .18s; position: relative; }
.session:hover { background: #f4f7fd; }
.session.on { background: rgba(74,125,255,.1); }
.s-title { font-size: 13.5px; font-weight: 600; color: var(--ink); padding-right: 42px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.s-sum { font-size: 12px; color: var(--ink-3); margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.s-actions { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); display: none; gap: 4px; }
.session:hover .s-actions { display: flex; }
.s-btn { width: 22px; height: 22px; display: flex; align-items: center; justify-content: center; border-radius: 5px; font-size: 13px; color: var(--ink-3); background: rgba(255,255,255,.8); cursor: pointer; transition: all .15s; }
.s-btn:hover { background: var(--primary); color: #fff; }
.s-btn.del:hover { background: #f56c6c; }

.main { flex: 1; display: flex; flex-direction: column; padding: 0; overflow: hidden; }
.banner {
  display: flex; align-items: center; gap: 8px; padding: 13px 20px;
  font-size: 13px; color: var(--ink-2); background: rgba(74,125,255,.06);
  border-bottom: 1px solid var(--line);
}
.msgs { flex: 1; overflow-y: auto; padding: 22px; display: flex; flex-direction: column; gap: 16px; }
.starter { margin: auto; text-align: center; }
.st-title { color: var(--ink-2); font-size: 14px; margin: 0 0 14px; }
.st-chips { display: flex; flex-direction: column; gap: 10px; align-items: center; }
.st-chip {
  padding: 9px 20px; border-radius: 999px; background: #f2f5fc; color: var(--ink-2);
  font-size: 13px; cursor: pointer; transition: all .2s; border: 1px solid transparent;
}
.st-chip:hover { background: rgba(74,125,255,.1); color: var(--primary); border-color: rgba(74,125,255,.3); }
.row { display: flex; gap: 10px; align-items: flex-start; }
.row.me { justify-content: flex-end; }
.av {
  width: 34px; height: 34px; border-radius: 50%; color: #fff; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 12.5px; font-weight: 600;
}
.av.me { background: linear-gradient(135deg, #ffb27a, #ff8a3d); }
.ai-av { background: linear-gradient(135deg, #6a94ff, #4a7dff); box-shadow: 0 3px 8px rgba(74,125,255,.35); }
.bubble {
  max-width: 64%; padding: 11px 15px; border-radius: 14px; font-size: 14px; line-height: 1.75;
  background: #f5f7fc; color: var(--ink); white-space: pre-wrap;
}
.row.me .bubble {
  background: linear-gradient(135deg, #6a94ff, #4a7dff); color: #fff;
  border-bottom-right-radius: 4px;
}
.row.ai .bubble { border-bottom-left-radius: 4px; }
.meta { margin-top: 6px; }
.cursor { color: var(--primary); animation: blink 1s infinite; }
.thinking { color: var(--ink-3); font-size: 12px; margin-right: 6px; }
@keyframes blink { 50% { opacity: 0; } }
.input-bar { display: flex; gap: 10px; padding: 14px 18px; border-top: 1px solid var(--line); background: #fbfcfe; }
</style>

<!-- ElMessageBox 渲染于 body，需用非 scoped 样式 -->
<style>
/* 统一弹窗胶囊风格 */
.chat-msgbox { border-radius: 16px !important; }
.chat-msgbox .el-message-box__title { font-weight: 600; }
/* 按钮统一胶囊形状 */
.chat-msgbox .el-message-box__btns .el-button {
  border-radius: 999px !important;
  min-width: 84px;
}
/* 重命名弹窗：去掉空消息占位，输入框胶囊化 */
.chat-prompt-box .el-message-box__message { display: none; }
.chat-prompt-box .el-message-box__input .el-input__wrapper {
  border-radius: 999px !important;
  padding: 4px 16px;
}
/* 删除弹窗：确认按钮改为醒目红色 */
.chat-delete-box .el-message-box__btns .el-button--primary {
  background: #f56c6c !important;
  border-color: #f56c6c !important;
}
.chat-delete-box .el-message-box__btns .el-button--primary:hover {
  background: #f78989 !important;
  border-color: #f78989 !important;
}
</style>
