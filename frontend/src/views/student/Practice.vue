<template>
  <div class="practice">
    <!-- 选学科 -->
    <div v-if="!started" class="soft-card pick">
      <h3 class="section-title">选择学科开始练习</h3>
      <p class="faint" style="margin:0 0 20px;font-size:13px">从题库随机抽题，提交后自动判分并归档到学情报告</p>
      <div class="subj-grid">
        <div v-for="s in subjects" :key="s.name" class="subj-card" @click="start(s.name)">
          <div class="subj-icon" :style="{ background: s.color }">{{ s.name.charAt(0) }}</div>
          <span>{{ s.name }}</span>
        </div>
      </div>
    </div>

    <!-- 答题 -->
    <div v-else-if="!result" class="soft-card quiz">
      <div class="quiz-head">
        <el-button size="small" text @click="reset">← 返回</el-button>
        <span class="muted">{{ currentSubject }} · 共 {{ questions.length }} 题</span>
      </div>
      <div class="q-list">
        <div v-for="(q, i) in questions" :key="q.id" class="q-item">
          <div class="q-title">
            <el-tag size="small" effect="plain" style="margin-right:8px">{{ q.difficulty }}</el-tag>
            <span>{{ i + 1 }}. {{ q.content }}</span>
          </div>
          <el-input v-model="answers[q.id]" type="textarea" :rows="2" placeholder="写下你的答案…" />
        </div>
      </div>
      <div class="submit-bar">
        <el-progress :percentage="donePercent" :stroke-width="8" style="flex:1" />
        <el-button type="primary" size="large" :disabled="!allAnswered" :loading="submitting" @click="submit">
          提交判分
        </el-button>
      </div>
    </div>

    <!-- 结果 -->
    <div v-else class="soft-card result">
      <div class="score-hero">
        <div class="ring">
          <svg viewBox="0 0 120 120" width="150" height="150">
            <circle cx="60" cy="60" r="52" fill="none" stroke="#eef1f8" stroke-width="10"/>
            <circle cx="60" cy="60" r="52" fill="none" stroke="url(#g)" stroke-width="10" stroke-linecap="round"
                    :stroke-dasharray="`${pct * 3.27} 327`" transform="rotate(-90 60 60)"/>
            <defs><linearGradient id="g"><stop offset="0%" stop-color="#6a94ff"/><stop offset="100%" stop-color="#38b8a0"/></linearGradient></defs>
          </svg>
          <div class="num">{{ result.score }}<small>/{{ result.total }}</small></div>
        </div>
        <div>
          <h3>{{ pct >= 80 ? '非常棒！' : pct >= 60 ? '不错，继续加油！' : '别灰心，再看看解析～' }}</h3>
          <p class="muted">正确率 {{ pct }}%，本次练习已记录到学情报告</p>
          <el-button type="primary" plain @click="reset">再练一次</el-button>
        </div>
      </div>
      <el-divider />
      <div v-for="d in result.details" :key="d.questionId" class="review" :class="{ wrong: !d.correct }">
        <div class="r-title">
          <el-icon v-if="d.correct" color="#38b8a0"><CircleCheckFilled /></el-icon>
          <el-icon v-else color="#f5679a"><CircleCloseFilled /></el-icon>
          <span>{{ d.content }}</span>
        </div>
        <div class="r-row"><span class="k">你的答案</span><span :class="{ bad: !d.correct }">{{ d.userAnswer || '（未作答）' }}</span></div>
        <div class="r-row" v-if="!d.correct"><span class="k">参考答案</span><span class="good">{{ d.rightAnswer }}</span></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const SUBJECT_COLORS = {
  '语文': 'linear-gradient(135deg,#c39bff,#9a6fff)', '数学': 'linear-gradient(135deg,#6a94ff,#4a7dff)',
  '英语': 'linear-gradient(135deg,#ff9fb8,#f5679a)', '物理': 'linear-gradient(135deg,#ffb27a,#ff8a3d)',
  '化学': 'linear-gradient(135deg,#6fd6c0,#38b8a0)', '生物': 'linear-gradient(135deg,#8ee08a,#4fbf6a)',
  '政治': 'linear-gradient(135deg,#ff9a9a,#e86060)', '历史': 'linear-gradient(135deg,#ffd27a,#e8a83d)',
  '地理': 'linear-gradient(135deg,#7ac8ff,#3d9de8)'
}
const subjects = Object.keys(SUBJECT_COLORS).map(name => ({ name, color: SUBJECT_COLORS[name] }))

const started = ref(false)
const currentSubject = ref('')
const questions = ref([])
const answers = ref({})
const submitting = ref(false)
const result = ref(null)

const donePercent = computed(() => questions.value.length
  ? Math.round(Object.keys(answers.value).filter(k => answers.value[k]?.trim()).length / questions.value.length * 100) : 0)
const allAnswered = computed(() => questions.value.every(q => answers.value[q.id]?.trim()))
const pct = computed(() => result.value ? Math.round(result.value.score * 100 / result.value.total) : 0)

async function start(subject) {
  currentSubject.value = subject
  const res = await api.get('/practice/questions', { params: { subject, count: 5 } })
  if (!res.data.length) { ElMessage.info(`${subject}题库暂时没有题目，请先通知老师录入`); return }
  questions.value = res.data
  answers.value = {}
  result.value = null
  started.value = true
}

async function submit() {
  submitting.value = true
  try {
    const payload = {
      subject: currentSubject.value,
      answers: Object.keys(answers.value).map(qid => ({ questionId: qid, userAnswer: answers.value[qid] }))
    }
    const res = await api.post('/practice/submit', payload)
    result.value = res.data
  } finally {
    submitting.value = false
  }
}

function reset() {
  started.value = false
  result.value = null
}
</script>

<style scoped>
.pick { max-width: 860px; margin: 30px auto; }
.subj-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 14px; }
.subj-card {
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  padding: 22px 10px; border-radius: 14px; background: #f6f8fd;
  cursor: pointer; transition: all .22s; color: var(--ink);
}
.subj-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-hover); background: #fff; }
.subj-icon {
  width: 48px; height: 48px; border-radius: 14px; color: #fff; font-size: 20px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}

.quiz { max-width: 860px; margin: 0 auto; }
.quiz-head { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.q-list { display: flex; flex-direction: column; gap: 18px; }
.q-item { background: #f8fafd; border-radius: 12px; padding: 16px 18px; }
.q-title { margin-bottom: 10px; font-size: 14.5px; line-height: 1.7; }
.submit-bar { display: flex; align-items: center; gap: 16px; margin-top: 20px; }

.result { max-width: 860px; margin: 0 auto; }
.score-hero { display: flex; align-items: center; gap: 30px; }
.ring { position: relative; }
.ring .num { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 30px; font-weight: 700; color: var(--ink); }
.ring .num small { font-size: 15px; color: var(--ink-3); }
.score-hero h3 { margin: 0 0 6px; }
.review { background: #f6fbf8; border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
.review.wrong { background: #fdf3f5; }
.r-title { display: flex; align-items: center; gap: 8px; font-weight: 600; margin-bottom: 6px; }
.r-row { display: flex; gap: 12px; font-size: 13px; margin: 3px 0; }
.r-row .k { width: 64px; color: var(--ink-3); flex-shrink: 0; }
.r-row .good { color: #38b8a0; }
.r-row .bad { color: #f5679a; }
</style>
