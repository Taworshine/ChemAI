<template>
  <div class="faq-page">
    <div class="soft-card faq-hero">
      <div>
        <h3>高频问题</h3>
        <p class="faint">同学们问得最多的问题都在这里，带官方解答的先看解答，仍有疑问一键追问</p>
      </div>
      <el-input v-model="keyword" size="large" placeholder="搜索问题…" clearable class="search"
                :prefix-icon="Search" @input="debounceLoad" />
    </div>

    <div class="faq-list">
      <div v-for="(f, i) in list" :key="f.id" class="soft-card faq-item" :class="{ hot: i < 3 && f.askCount >= 2 }">
        <div class="f-rank" :class="'r' + (i + 1)" v-if="i < 3">TOP{{ i + 1 }}</div>
        <div class="f-main">
          <div class="f-head">
            <p class="q">{{ f.questionText }}</p>
            <div class="f-meta">
              <el-tag size="small" effect="plain" type="info" v-if="f.conceptName">{{ f.conceptName }}</el-tag>
              <span class="ask">{{ f.askCount }} 人问过</span>
            </div>
          </div>

          <div v-if="f.answered" class="answer">
            <div class="a-badge">教师解答</div>
            <p class="a-text">{{ f.answer }}</p>
            <div class="a-actions">
              <button class="fb" :class="{ on: f.myFeedback === 'useful' }" @click="feedback(f, 'useful')">
                有用 {{ f.useful || '' }}
              </button>
              <button class="fb" :class="{ on: f.myFeedback === 'useless' }" @click="feedback(f, 'useless')">
                无用 {{ f.useless || '' }}
              </button>
            </div>
          </div>
          <div v-else class="pending-ans faint">
            <span class="dot"></span>教师解答中，你也可以追问加速解答
          </div>

          <div class="f-foot">
            <el-button size="small" text type="primary" @click="askToo(f)">
              <el-icon><ChatDotRound /></el-icon> 我也有这个疑问
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!list.length" description="暂无高频问题，去智能追问提出第一个问题吧" :image-size="100" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const router = useRouter()
const list = ref([])
const keyword = ref('')
let timer = null

async function load() {
  const res = await api.get('/agent/faq/list', { params: { keyword: keyword.value || undefined } })
  list.value = res.data
}
function debounceLoad() {
  clearTimeout(timer)
  timer = setTimeout(load, 300)
}

async function feedback(f, type) {
  await api.post(`/agent/faq/${f.id}/feedback`, { feedback: type })
  f.myFeedback = f.myFeedback === type ? null : type
  // 本地即时反馈数量
  if (f.myFeedback === type) {
    if (type === 'useful') f.useful = (f.useful || 0) + 1
    else f.useless = (f.useless || 0) + 1
  }
  ElMessage.success('已反馈，谢谢！')
}

function askToo(f) {
  router.push({ path: '/student/chat', query: { question: f.questionText } })
}

onMounted(load)
</script>

<style scoped>
.faq-page { display: flex; flex-direction: column; gap: 16px; }
.faq-hero {
  display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: 20px 24px; flex-wrap: wrap;
  background: linear-gradient(120deg, #eef4ff 0%, #fdfcff 55%, #eafaf4 100%);
}
.faq-hero h3 { margin: 0 0 6px; font-size: 19px; color: var(--ink); }
.faq-hero p { margin: 0; font-size: 13px; }
.search { max-width: 320px; }

.faq-list { display: flex; flex-direction: column; gap: 12px; }
.faq-item { display: flex; gap: 14px; padding: 16px 20px; position: relative; overflow: hidden; }
.faq-item.hot::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 4px;
  background: linear-gradient(180deg, #ff9a4d, #f5679a);
}
.f-rank { flex-shrink: 0; align-self: flex-start; font-size: 11px; font-weight: 800; padding: 3px 8px; border-radius: 6px; }
.f-rank.r1 { background: #ffe9d6; color: #d46b08; }
.f-rank.r2 { background: #fff3d6; color: #b8860b; }
.f-rank.r3 { background: #e9f8f3; color: #1f9e85; }
.f-main { flex: 1; min-width: 0; }
.f-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.q { margin: 0; font-size: 14.5px; font-weight: 600; color: var(--ink); line-height: 1.6; }
.f-meta { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.ask { font-size: 12px; color: var(--ink-3); }

.answer { margin-top: 10px; background: #f4f9f6; border: 1px solid #d9efe6; border-radius: 10px; padding: 12px 14px; }
.a-badge { display: inline-block; font-size: 11px; font-weight: 700; color: #1f9e85; background: #d9efe6; padding: 2px 10px; border-radius: 999px; margin-bottom: 8px; }
.a-text { margin: 0; font-size: 13.5px; color: var(--ink-2); line-height: 1.8; white-space: pre-wrap; }
.a-actions { display: flex; gap: 8px; margin-top: 10px; }
.fb { padding: 4px 14px; border-radius: 999px; border: 1px solid var(--line); background: #fff; font-size: 12px; color: var(--ink-3); cursor: pointer; transition: all .18s; }
.fb:hover { border-color: #8fd9c2; color: #1f9e85; }
.fb.on { background: #38b8a0; border-color: #38b8a0; color: #fff; }

.pending-ans { display: flex; align-items: center; gap: 6px; margin-top: 10px; font-size: 12.5px; }
.pending-ans .dot { width: 7px; height: 7px; border-radius: 50%; background: #e8a23c; animation: pulse 1.6s infinite; }
@keyframes pulse { 0%,100% { opacity: 1 } 50% { opacity: .3 } }

.f-foot { margin-top: 6px; }
</style>
