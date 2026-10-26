<template>
  <!-- 个人面板：独立的 Bento 风格，与工作台/报告等标准页明显区分 -->
  <div class="me">
    <!-- 顶部欢迎横幅 -->
    <section class="me-hero">
      <div class="hero-glow g1"></div>
      <div class="hero-glow g2"></div>
      <div class="hero-main">
        <div class="hero-avatar">{{ (realName || '同').charAt(0) }}</div>
        <div class="hero-text">
          <h2>{{ greeting }}，{{ realName }}</h2>
          <p>{{ today }} · {{ motto }}</p>
        </div>
      </div>
      <div class="hero-actions">
        <button class="h-btn primary" @click="$router.push('/student/practice')">开始今日练习</button>
        <button class="h-btn ghost" @click="$router.push('/student/chat')">向 AI 提问</button>
      </div>
      <!-- 横幅内嵌数据带 -->
      <div class="hero-band">
        <div class="hb-item"><strong>{{ stats.practices }}</strong><span>练习次数</span></div>
        <i class="hb-sep"></i>
        <div class="hb-item"><strong>{{ stats.avg }}%</strong><span>平均正确率</span></div>
        <i class="hb-sep"></i>
        <div class="hb-item"><strong>{{ stats.sessions }}</strong><span>AI 追问会话</span></div>
        <i class="hb-sep"></i>
        <div class="hb-item"><strong>{{ weakConcepts.length }}</strong><span>待巩固概念</span></div>
      </div>
    </section>

    <!-- Bento 网格 -->
    <div class="bento">
      <!-- 待巩固概念 -->
      <section class="tile weak span7">
        <header class="t-head">
          <h3>待巩固概念</h3>
          <span class="t-tag warn" v-if="weakConcepts.length">TOP {{ weakConcepts.length }}</span>
          <span class="t-tag" v-else>状态良好</span>
        </header>
        <div v-if="weakConcepts.length" class="weak-list">
          <div v-for="w in weakConcepts" :key="w.name" class="weak-item" @click="$router.push('/student/graph')">
            <span class="w-rank">{{ weakConcepts.indexOf(w) + 1 }}</span>
            <span class="w-name">{{ w.name }}</span>
            <div class="w-bar"><i :style="{ width: Math.round(w.value * 100) + '%', background: barColor(w.value) }"></i></div>
            <span class="w-val" :style="{ color: barColor(w.value) }">{{ Math.round(w.value * 100) }}%</span>
          </div>
        </div>
        <div v-else class="tile-empty">太棒了，暂时没有明显薄弱概念 🎉</div>
        <p class="t-foot">点击概念可跳转到知识图谱查看它的上下游关系</p>
      </section>

      <!-- 学科平均分 -->
      <section class="tile chart span5">
        <header class="t-head"><h3>各学科平均分</h3><span class="t-tag">练习数据</span></header>
        <div ref="barRef" class="bar"></div>
        <div v-if="!bySubject.length" class="tile-empty">还没有练习记录，先来一场练习吧</div>
      </section>

      <!-- 快捷入口 -->
      <section class="tile span5">
        <header class="t-head"><h3>快捷入口</h3></header>
        <div class="apps">
          <div v-for="a in apps" :key="a.to" class="app" :style="{ '--c': a.c }" @click="$router.push(a.to)">
            <span class="app-icon"><el-icon :size="20"><component :is="a.icon" /></el-icon></span>
            <span class="app-name">{{ a.name }}</span>
            <span class="app-desc">{{ a.desc }}</span>
          </div>
        </div>
      </section>

      <!-- 最近动态 -->
      <section class="tile span7">
        <header class="t-head"><h3>最近练习动态</h3></header>
        <div v-if="recent.length" class="feed">
          <div v-for="r in recent" :key="r.id" class="feed-item">
            <span class="f-dot" :style="{ background: r.score * 100 / r.total >= 80 ? '#4a7dff' : '#ff9a4d' }"></span>
            <span class="f-text">完成 <b>{{ r.subject }}</b> 练习，得分 {{ r.score }}/{{ r.total }}</span>
            <span class="f-time">{{ fmtTime(r.createTime) }}</span>
          </div>
        </div>
        <div v-else class="tile-empty">暂无动态，去练习或提问都会产生记录</div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { ChatDotRound, Share, EditPen, User } from '@element-plus/icons-vue'
import api from '../../api'

const realName = ref(localStorage.getItem('realName') || '')
const barRef = ref(null)
const records = ref([])
const bySubject = ref([])
const recent = ref([])
const sessionCount = ref(0)
const mastery = ref({})
let bar = null

const hour = new Date().getHours()
const greeting = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好'
const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })
const motto = '启发思考，而非给出答案'

const apps = [
  { to: '/student/chat', name: '智能追问', desc: 'AI 循循善诱', icon: ChatDotRound, c: '#4a7dff' },
  { to: '/student/graph', name: '知识图谱', desc: '探索全学科', icon: Share, c: '#38b8a0' },
  { to: '/student/practice', name: '在线练习', desc: '刷题巩固', icon: EditPen, c: '#ff9a4d' },
  { to: '/student/profile', name: '学情报告', desc: '五维能力', icon: User, c: '#8b5cf6' }
]

const stats = computed(() => {
  const total = records.value.reduce((s, r) => s + Number(r.total || 0), 0)
  const score = records.value.reduce((s, r) => s + Number(r.score || 0), 0)
  return {
    practices: records.value.length,
    avg: total ? Math.round(score * 100 / total) : 0,
    sessions: sessionCount.value
  }
})

const weakConcepts = computed(() => Object.entries(mastery.value)
  .map(([name, value]) => ({ name, value: Number(value) }))
  .filter(c => c.value < 0.6)
  .sort((a, b) => a.value - b.value)
  .slice(0, 5))

const barColor = v => v >= 0.5 ? '#ff9a4d' : '#f5679a'

function fmtTime(t) {
  if (!t) return ''
  const d = new Date(String(t).replace(' ', 'T'))
  return isNaN(d) ? String(t).slice(5, 16) : `${d.getMonth() + 1}月${d.getDate()}日`
}

function renderBar() {
  if (!bySubject.value.length || !barRef.value) return
  bar = echarts.init(barRef.value)
  bar.setOption({
    grid: { left: 34, right: 10, top: 20, bottom: 26 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: bySubject.value.map(s => s.subject),
             axisLine: { lineStyle: { color: '#d8dfee' } }, axisLabel: { color: '#6b7386', fontSize: 11 } },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#eef1f8' } }, axisLabel: { color: '#9aa1b5' } },
    series: [{
      type: 'bar', data: bySubject.value.map(s => s.avg), barWidth: 22,
      itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: '#6a94ff' }, { offset: 1, color: '#c9d8ff' }]) },
      label: { show: true, position: 'top', color: '#6b7386', fontSize: 11 }
    }]
  })
}

onMounted(async () => {
  const userId = localStorage.getItem('userId')
  const [prac, sess, radar] = await Promise.all([
    api.get('/practice/records'),
    api.get('/agent/sessions'),
    api.get(`/analysis/student/${userId}/radar`)
  ])
  records.value = prac.data
  bySubject.value = Object.entries(
    prac.data.reduce((acc, r) => {
      (acc[r.subject] = acc[r.subject] || { s: 0, t: 0 })
      acc[r.subject].s += Number(r.score); acc[r.subject].t += Number(r.total)
      return acc
    }, {})
  ).map(([subject, v]) => ({ subject, avg: v.t ? Math.round(v.s * 100 / v.t) : 0 }))
  recent.value = prac.data.slice(0, 6)
  sessionCount.value = sess.data.length
  try { mastery.value = JSON.parse(radar.data.conceptMastery || '{}') } catch { mastery.value = {} }
  renderBar()
  window.addEventListener('resize', () => bar && bar.resize())
})

onBeforeUnmount(() => bar && bar.dispose())
</script>

<style scoped>
/* ============ 个人面板：独立 Bento 风格 ============ */
.me { display: flex; flex-direction: column; gap: 18px; }

/* 欢迎横幅：深色渐变 + 漂浮光斑，与其他页面的白底卡片完全不同 */
.me-hero {
  position: relative; overflow: hidden; border-radius: 22px;
  background: linear-gradient(120deg, #2b3a8f 0%, #3f5bd0 55%, #5a7bf5 100%);
  color: #fff; padding: 30px 34px 0;
  box-shadow: 0 18px 44px rgba(59, 82, 200, .32);
}
.hero-glow { position: absolute; border-radius: 50%; filter: blur(70px); opacity: .5; pointer-events: none; }
.hero-glow.g1 { width: 300px; height: 300px; background: #8fb0ff; top: -140px; right: 8%; }
.hero-glow.g2 { width: 220px; height: 220px; background: #ff9a4d; bottom: -130px; left: 26%; opacity: .3; }

.hero-main { display: flex; align-items: center; gap: 18px; position: relative; }
.hero-avatar {
  width: 62px; height: 62px; border-radius: 20px; font-size: 26px; font-weight: 700;
  background: rgba(255,255,255,.18); border: 1px solid rgba(255,255,255,.35);
  backdrop-filter: blur(6px);
  display: flex; align-items: center; justify-content: center;
}
.hero-text h2 { margin: 0; font-size: 23px; letter-spacing: .5px; }
.hero-text p { margin: 5px 0 0; font-size: 13px; opacity: .78; }

.hero-actions { position: absolute; right: 34px; top: 38px; display: flex; gap: 10px; }
.h-btn {
  border: none; cursor: pointer; font-size: 13.5px; padding: 10px 20px; border-radius: 999px;
  transition: all .2s; font-family: inherit;
}
.h-btn.primary { background: #fff; color: #3f5bd0; font-weight: 600; box-shadow: 0 6px 16px rgba(0,0,0,.18); }
.h-btn.primary:hover { transform: translateY(-2px); }
.h-btn.ghost { background: rgba(255,255,255,.14); color: #fff; border: 1px solid rgba(255,255,255,.35); }
.h-btn.ghost:hover { background: rgba(255,255,255,.24); }

.hero-band {
  position: relative; display: flex; align-items: center; gap: 26px;
  margin: 26px -34px 0; padding: 16px 34px;
  background: rgba(10, 18, 60, .28); backdrop-filter: blur(6px);
  border-top: 1px solid rgba(255,255,255,.14);
}
.hb-item { display: flex; flex-direction: column; }
.hb-item strong { font-size: 21px; }
.hb-item span { font-size: 11.5px; opacity: .72; margin-top: 2px; }
.hb-sep { width: 1px; height: 26px; background: rgba(255,255,255,.18); }

/* Bento 网格 */
.bento { display: grid; grid-template-columns: repeat(12, 1fr); gap: 16px; }
.tile {
  background: #fff; border-radius: 20px; padding: 20px 22px;
  border: 1px solid var(--line);
  box-shadow: 0 2px 12px rgba(76, 96, 160, 0.06);
  transition: box-shadow .25s, transform .25s;
}
.tile:hover { box-shadow: 0 10px 30px rgba(76, 96, 160, .12); transform: translateY(-2px); }
.span7 { grid-column: span 7; }
.span5 { grid-column: span 5; }

.t-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.t-head h3 { margin: 0; font-size: 15.5px; color: var(--ink); letter-spacing: .5px; }
.t-tag {
  font-size: 11px; color: var(--ink-3); background: #f2f5fc; padding: 3px 10px; border-radius: 999px;
}
.t-tag.warn { color: #e8843d; background: rgba(255,154,77,.12); }
.t-foot { font-size: 11.5px; color: var(--ink-3); margin: 12px 0 0; }
.tile-empty { color: var(--ink-3); font-size: 13px; padding: 18px 0; text-align: center; }

/* 待巩固概念排行 */
.weak-list { display: flex; flex-direction: column; gap: 12px; }
.weak-item { display: flex; align-items: center; gap: 12px; cursor: pointer; padding: 4px 2px; border-radius: 8px; }
.weak-item:hover { background: #f8fafe; }
.w-rank {
  width: 22px; height: 22px; border-radius: 7px; font-size: 12px; font-weight: 700; color: #fff;
  background: linear-gradient(135deg, #ffb27a, #ff8a3d);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.w-name { width: 92px; font-size: 13.5px; color: var(--ink); flex-shrink: 0;
          overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.w-bar { flex: 1; height: 8px; border-radius: 4px; background: #eef1f8; overflow: hidden; }
.w-bar i { display: block; height: 100%; border-radius: 4px; transition: width .6s ease; }
.w-val { width: 42px; text-align: right; font-size: 12.5px; font-weight: 700; }

.bar { height: 240px; }

/* 快捷入口 */
.apps { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.app {
  --c: #4a7dff;
  display: flex; flex-direction: column; align-items: flex-start; gap: 4px;
  padding: 14px; border-radius: 14px; cursor: pointer;
  background: #f8fafe; transition: all .2s; border: 1px solid transparent;
}
.app:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(76,96,160,.14); border-color: var(--c); }
.app-icon {
  width: 38px; height: 38px; border-radius: 12px; color: #fff;
  background: var(--c); display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 10px color-mix(in srgb, var(--c) 40%, transparent);
  margin-bottom: 4px;
}
.app-name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.app-desc { font-size: 11.5px; color: var(--ink-3); }

/* 动态 Feed */
.feed { display: flex; flex-direction: column; }
.feed-item { display: flex; align-items: center; gap: 10px; padding: 10px 2px; border-bottom: 1px dashed var(--line); }
.feed-item:last-child { border-bottom: none; }
.f-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.f-text { flex: 1; font-size: 13px; color: var(--ink-2); }
.f-text b { color: var(--ink); }
.f-time { font-size: 12px; color: var(--ink-3); }

@media (max-width: 1100px) {
  .span7, .span5 { grid-column: span 12; }
  .hero-actions { position: static; margin-top: 16px; }
  .me-hero { padding-bottom: 20px; }
  .hero-band { margin: 22px -34px -20px; }
}
</style>
