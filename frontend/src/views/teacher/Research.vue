<template>
  <div class="research-page">
    <div class="soft-card head-card">
      <div>
        <h3>教研看板</h3>
        <p class="faint">图谱资产 · 入库进度 · 卡点热点 · 知识缺口，一站式教研视角</p>
      </div>
      <el-button type="primary" plain @click="$router.push('/teacher/extract')">去上传入库</el-button>
    </div>

    <!-- 指标卡 -->
    <div class="metric-row">
      <div class="soft-card metric" v-for="m in metrics" :key="m.label">
        <p class="m-label">{{ m.label }}</p>
        <p class="m-value" :style="{ color: m.color }">{{ m.value }}</p>
        <p class="m-sub faint">{{ m.sub }}</p>
      </div>
    </div>

    <div class="grid-2">
      <!-- 学科覆盖 -->
      <div class="soft-card panel">
        <h4>学科覆盖</h4>
        <div v-for="(c, name) in data.bySubject || {}" :key="name" class="bar-row">
          <span class="bar-name">{{ name }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: barWidth(c) + '%' }"></div>
          </div>
          <span class="bar-val">{{ c }}</span>
        </div>
        <el-empty v-if="!data.bySubject" description="加载中…" :image-size="60" />
      </div>

      <!-- 卡点热点 -->
      <div class="soft-card panel">
        <h4>学生卡点热点 Top5</h4>
        <div v-for="(m, i) in data.misconceptionTop || []" :key="m.type" class="mis-row">
          <span class="rank" :class="'r' + (i + 1)">{{ i + 1 }}</span>
          <span class="mis-type">{{ misName(m.type) }}</span>
          <span class="mis-count">{{ m.count }} 次</span>
        </div>
        <el-empty v-if="data.misconceptionTop && !data.misconceptionTop.length" description="暂无卡点记录" :image-size="60" />
      </div>

      <!-- 入库任务状态 -->
      <div class="soft-card panel">
        <h4>入库任务</h4>
        <div class="task-chips">
          <span v-for="t in data.taskStatus || []" :key="t.status" class="t-chip" :class="t.status">
            {{ statusText(t.status) }} <b>{{ t.c }}</b>
          </span>
        </div>
        <div class="gap-actions">
          <p class="faint small">待裁决条目 {{ data.pendingItems ?? 0 }} · 待解答高频问题 {{ data.faqPending ?? 0 }}</p>
          <el-button size="small" text type="primary" @click="$router.push('/teacher/confirm')">去确认中心</el-button>
        </div>
      </div>

      <!-- 缺口清单 -->
      <div class="soft-card panel">
        <h4>知识缺口清单 <span class="faint small">学生常问但图谱没有</span></h4>
        <div v-for="g in gaps" :key="g.id" class="gap-row">
          <div class="gap-q">
            <p class="q-text">{{ g.question }}</p>
            <p class="faint small">{{ g.askCount }} 人问过 · {{ g.answered ? '已解答' : '未解答' }}</p>
          </div>
          <el-button size="small" type="primary" plain
                     @click="$router.push({ path: '/teacher/extract', query: { prefill: g.question } })">
            补知识点
          </el-button>
        </div>
        <el-empty v-if="!gaps.length" description="暂无缺口，图谱覆盖良好" :image-size="60" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../../api'

const data = ref({})
const gaps = ref([])

const metrics = computed(() => {
  const byLabel = data.value.byLabel || {}
  const total = Object.values(byLabel).reduce((a, b) => a + b, 0)
  const curation = data.value.byCuration || {}
  const draft = (curation['DRAFT'] || 0)
  return [
    { label: '图谱知识点', value: total, sub: `思维路径 ${byLabel.ThinkingPath || 0} 条`, color: '#4a7dff' },
    { label: '近 30 天新增', value: data.value.recent30 ?? 0, sub: '来自教师入库', color: '#38b8a0' },
    { label: '待发布草稿', value: draft, sub: '确认后未发布', color: '#e8a23c' },
    { label: '高频问题待解答', value: data.value.faqPending ?? 0, sub: `追问会话 ${data.value.sessionCount ?? 0} 个`, color: '#f5679a' }
  ]
})

const maxSubject = computed(() => Math.max(1, ...Object.values(data.value.bySubject || { _: 1 })))
const barWidth = c => Math.max(6, Math.round(c / maxSubject.value * 100))

const MIS = {
  CONCEPT_UNCLEAR: '概念不清', LOGIC_ERROR: '逻辑错误', CALC_ERROR: '计算错误',
  READ_MISS: '审题遗漏', GIVE_UP: '放弃求助'
}
const misName = t => MIS[t] || t

const STATUS = {
  PENDING_CONFIRM: '待确认', CONFIRMED: '已入库', PUBLISHED: '已发布',
  OFFLINE: '已下线', FAILED: '失败', EXTRACTING: '抽取中', MATCHING: '匹配中', PARSING: '解析中'
}
const statusText = s => STATUS[s] || s

onMounted(async () => {
  const [d, g] = await Promise.all([api.get('/research/dashboard'), api.get('/research/gap-list')])
  data.value = d.data
  gaps.value = g.data.slice(0, 6)
})
</script>

<style scoped>
.research-page { display: flex; flex-direction: column; gap: 16px; }
.head-card {
  display: flex; justify-content: space-between; align-items: center; padding: 20px 24px;
  background: linear-gradient(120deg, #f4f0ff 0%, #fdfcff 55%, #eefaf7 100%);
}
.head-card h3 { margin: 0 0 6px; font-size: 19px; color: var(--ink); }
.head-card p { margin: 0; font-size: 13px; }

.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
@media (max-width: 1000px) { .metric-row { grid-template-columns: repeat(2, 1fr); } }
.metric { padding: 18px 20px; }
.m-label { margin: 0; font-size: 12.5px; color: var(--ink-3); }
.m-value { margin: 8px 0 4px; font-size: 30px; font-weight: 800; }
.m-sub { margin: 0; font-size: 12px; }

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
@media (max-width: 1000px) { .grid-2 { grid-template-columns: 1fr; } }
.panel { padding: 18px 20px; }
.panel h4 { margin: 0 0 14px; color: var(--ink); font-size: 15px; }

.bar-row { display: grid; grid-template-columns: 52px 1fr 40px; gap: 10px; align-items: center; margin-bottom: 10px; }
.bar-name { font-size: 13px; color: var(--ink-2); }
.bar-track { height: 10px; border-radius: 999px; background: #eef2fa; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 999px; background: linear-gradient(90deg, #6a94ff, #38b8a0); }
.bar-val { font-size: 12.5px; font-weight: 700; color: var(--ink-2); text-align: right; }

.mis-row { display: flex; align-items: center; gap: 12px; padding: 9px 4px; border-bottom: 1px dashed var(--line); }
.mis-row:last-child { border-bottom: none; }
.rank { width: 22px; height: 22px; border-radius: 7px; background: #eef2fa; color: var(--ink-3); font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center; }
.rank.r1 { background: #ffe9d6; color: #d46b08; }
.rank.r2 { background: #fff3d6; color: #b8860b; }
.rank.r3 { background: #e9f8f3; color: #1f9e85; }
.mis-type { font-size: 13.5px; color: var(--ink); font-weight: 600; }
.mis-count { margin-left: auto; font-size: 12.5px; color: var(--ink-3); }

.task-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.t-chip { padding: 5px 13px; border-radius: 999px; font-size: 12.5px; background: #f2f5fc; color: var(--ink-2); }
.t-chip b { margin-left: 3px; }
.t-chip.PENDING_CONFIRM { background: #fff7e8; color: #d48806; }
.t-chip.PUBLISHED, .t-chip.CONFIRMED { background: #e9f8f3; color: #1f9e85; }
.t-chip.FAILED { background: #ffe4e8; color: #d4385c; }
.gap-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; }
.gap-actions p { margin: 0; }

.gap-row { display: flex; align-items: center; gap: 10px; padding: 10px 4px; border-bottom: 1px dashed var(--line); }
.gap-row:last-child { border-bottom: none; }
.gap-q { flex: 1; min-width: 0; }
.q-text { margin: 0 0 3px; font-size: 13.5px; color: var(--ink); font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.gap-q p { margin: 0; }
.small { font-size: 12px; }
</style>
