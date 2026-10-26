<template>
  <div class="profile">
    <div class="soft-card">
      <h3 class="section-title">五维能力雷达</h3>
      <div ref="radarRef" class="chart"></div>
    </div>

    <div class="soft-card">
      <div class="card-head">
        <div>
          <h3 class="section-title" style="margin:0">概念掌握度</h3>
          <p class="ph-desc" style="margin:6px 0 0">覆盖全部 9 个学科，按学科筛选查看</p>
        </div>
        <el-select v-model="masterySubject" size="small" placeholder="全部学科" style="width:130px">
          <el-option label="全部学科" value="" />
          <el-option v-for="s in masterySubjects" :key="s" :label="s" :value="s" />
        </el-select>
      </div>
      <div class="concepts">
        <div v-for="c in filteredMastery" :key="c.name" class="concept">
          <el-tag size="small" effect="plain" class="c-subj">{{ c.subject }}</el-tag>
          <span class="name">{{ c.name }}</span>
          <el-progress :percentage="Math.round(c.value * 100)" :stroke-width="12" :show-text="false"
                       :color="c.value >= 0.8 ? '#4a7dff' : c.value >= 0.6 ? '#ff9a4d' : '#f5679a'" />
          <span class="val" :style="{ color: c.value >= 0.8 ? '#4a7dff' : c.value >= 0.6 ? '#ff9a4d' : '#f5679a' }">
            {{ Math.round(c.value * 100) }}%
          </span>
        </div>
      </div>
      <el-empty v-if="!filteredMastery.length" description="该学科暂无掌握度数据" :image-size="70" />
    </div>

    <div class="soft-card">
      <h3 class="section-title">练习历史</h3>
      <el-table :data="practices" stripe size="small">
        <el-table-column prop="subject" label="学科" width="90">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.subject }}</el-tag></template>
        </el-table-column>
        <el-table-column label="得分" width="110">
          <template #default="{ row }">
            <span :style="{ color: row.score * 100 / row.total >= 80 ? '#38b8a0' : '#ff9a4d', fontWeight: 600 }">
              {{ row.score }} / {{ row.total }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="答题明细">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!practices.length" description="还没有练习记录，去在线练习试试" :image-size="70" />
    </div>

    <div class="soft-card">
      <h3 class="section-title">错题记录（对话中检测到的卡点）</h3>
      <el-table :data="records" stripe size="small">
        <el-table-column prop="sessionId" label="会话" width="76" />
        <el-table-column prop="content" label="AI 追问" min-width="260" show-overflow-tooltip />
        <el-table-column label="卡点" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="warning" effect="light">{{ misText(row.misconceptionType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="165" />
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="答题明细" width="620px">
      <div v-for="(d, i) in detailRows" :key="i" class="d-row" :class="{ wrong: !d.correct }">
        <div class="r-title">
          <el-icon v-if="d.correct" color="#38b8a0"><CircleCheckFilled /></el-icon>
          <el-icon v-else color="#f5679a"><CircleCloseFilled /></el-icon>
          <span>{{ d.content }}</span>
        </div>
        <div class="r-line"><span class="k">我的答案</span>{{ d.userAnswer || '（未作答）' }}</div>
        <div class="r-line" v-if="!d.correct"><span class="k">参考答案</span>{{ d.rightAnswer }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import api from '../../api'

const radarRef = ref(null)
const mastery = ref({})
const conceptSubject = ref({})   // 概念名 → 学科（来自 Neo4j 图谱）
const masterySubject = ref('')
const records = ref([])
const practices = ref([])
const detailVisible = ref(false)
const detailRows = ref([])
let chart = null

const misText = t => ({ CONCEPT_UNCLEAR: '概念不清', CALC_ERROR: '计算错误', LOGIC_ERROR: '逻辑错误' }[t] || t)

/** 概念名模糊匹配学科：先精确，再前缀（如"氧化还原"匹配"氧化还原反应"），最后返回空串 */
function subjectOf(name) {
  if (conceptSubject.value[name]) return conceptSubject.value[name]
  const hit = Object.keys(conceptSubject.value).find(k => k.startsWith(name) || name.startsWith(k))
  return hit ? conceptSubject.value[hit] : '其他'
}

/** 带学科信息的掌握度列表 */
const masteryList = computed(() => Object.entries(mastery.value)
  .map(([name, value]) => ({ name, value: Number(value), subject: subjectOf(name) })))
const masterySubjects = computed(() => [...new Set(masteryList.value.map(c => c.subject))])
const filteredMastery = computed(() => masterySubject.value
  ? masteryList.value.filter(c => c.subject === masterySubject.value)
  : masteryList.value)

function viewDetail(row) {
  try {
    detailRows.value = JSON.parse(row.detail || '[]')
    detailVisible.value = true
  } catch { /* 忽略 */ }
}

onMounted(async () => {
  const userId = localStorage.getItem('userId')
  chart = echarts.init(radarRef.value)
  // 拉取图谱概念，建立 概念名 → 学科 映射，用于掌握度按学科分组
  try {
    const g = await api.get('/graph/full')
    const map = {}
    g.data.nodes.filter(n => n.label === 'Concept').forEach(n => { map[n.props.name] = n.props.subject })
    conceptSubject.value = map
  } catch { /* 图谱不可用时全部归入"其他" */ }
  const res = await api.get(`/analysis/student/${userId}/radar`)
  chart.setOption({
    radar: { indicator: res.data.dimensions.map(d => ({ name: d, max: 100 })), radius: '66%',
             axisName: { color: '#6b7386' }, splitArea: { areaStyle: { color: ['#fbfcff', '#f2f5fd'] } } },
    series: [{ type: 'radar',
      data: [{ value: res.data.values, name: '五维能力',
               areaStyle: { color: 'rgba(74,125,255,.28)' }, itemStyle: { color: '#4a7dff' } }] }]
  })
  try { mastery.value = JSON.parse(res.data.conceptMastery || '{}') } catch { mastery.value = {} }

  const [sess, prac] = await Promise.all([api.get('/agent/sessions'), api.get('/practice/records')])
  const all = []
  for (const s of sess.data) {
    const hist = await api.get(`/agent/history/${s.id}`)
    hist.data.filter(m => m.role === 'AI' && m.misconceptionType)
      .forEach(m => all.push({ ...m, sessionId: s.id }))
  }
  records.value = all.reverse()
  practices.value = prac.data
  window.addEventListener('resize', () => chart.resize())
})

onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped>
.profile { display: flex; flex-direction: column; gap: 18px; }
.chart { height: 340px; }
.concepts { display: flex; flex-direction: column; gap: 13px; }
.concept { display: flex; align-items: center; gap: 12px; }
.concept .c-subj { flex-shrink: 0; width: 52px; text-align: center; }
.concept .name { width: 110px; font-size: 14px; color: var(--ink-2); flex-shrink: 0; }
.concept :deep(.el-progress) { flex: 1; }
.concept .val { width: 48px; text-align: right; font-weight: 600; font-size: 13px; }
.d-row { background: #f6fbf8; border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
.d-row.wrong { background: #fdf3f5; }
.r-title { display: flex; align-items: center; gap: 8px; font-weight: 600; margin-bottom: 6px; }
.r-line { font-size: 13px; color: var(--ink-2); margin: 3px 0; }
.r-line .k { display: inline-block; width: 70px; color: var(--ink-3); }
</style>
