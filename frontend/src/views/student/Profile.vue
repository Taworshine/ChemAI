<template>
  <div class="profile">
    <div class="soft-card">
      <h3 class="section-title">学科能力雷达</h3>
      <div ref="radarRef" class="chart"></div>
    </div>

    <div class="soft-card">
      <div class="card-head">
        <div>
          <h3 class="section-title" style="margin:0">我的足迹</h3>
          <p class="ph-desc" style="margin:6px 0 0">已留下 {{ footprints.length }} 条足迹</p>
        </div>
        <div class="fp-filter">
          <el-checkbox-group v-model="selectedSubjects" class="fp-subjects">
            <el-checkbox-button v-for="s in footprintSubjects" :key="s" :label="s" :value="s" />
          </el-checkbox-group>
        </div>
      </div>
      <div class="footprint-grid">
        <div v-for="f in filteredFootprints" :key="f.id" class="fp-card">
          <div class="fp-head">
            <el-tag size="small" effect="plain" class="fp-subj">{{ f.subject || '其他' }}</el-tag>
            <span class="fp-name">{{ f.footprint }}</span>
            <span class="fp-del" title="删除足迹" @click.stop="deleteFootprint(f)">✕</span>
          </div>
          <div class="fp-comment" @click="editComment(f)" title="点击修改笔记">
            {{ f.comment || '（暂无笔记，点击添加）' }}
          </div>
          <div class="fp-date">{{ fmtDate(f.updateTime) }}</div>
        </div>
      </div>
      <el-empty v-if="!filteredFootprints.length"
                :description="footprints.length ? '没有选中学科的足迹' : '还没有足迹，去智能追问学懂一个知识点吧'"
                :image-size="70" />
      <div class="fp-foot">
        <el-button type="primary" round @click="showAddDialog = true">+ 新增足迹</el-button>
      </div>
    </div>

    <!-- 新增足迹弹窗 -->
    <el-dialog v-model="showAddDialog" title="新增足迹" width="520px" class="fp-add-dialog" align-center append-to-body>
      <el-form :model="addForm" label-width="70px">
        <el-form-item label="学科">
          <el-select v-model="addForm.subject" placeholder="请选择学科" style="width:100%">
            <el-option v-for="s in subjectOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="addForm.footprint" placeholder="请输入知识点名称" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="笔记">
          <el-input v-model="addForm.comment" type="textarea" :rows="4" placeholder="请输入该知识点的笔记" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确认</el-button>
      </template>
    </el-dialog>

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
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const radarRef = ref(null)
const footprints = ref([])
const selectedSubjects = ref([])
const records = ref([])
const practices = ref([])
const detailVisible = ref(false)
const detailRows = ref([])
const showAddDialog = ref(false)
const addForm = ref({ subject: '', footprint: '', comment: '' })
const subjectOptions = ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '政治', '其他']
let chart = null

const misText = t => ({ CONCEPT_UNCLEAR: '概念不清', CALC_ERROR: '计算错误', LOGIC_ERROR: '逻辑错误' }[t] || t)

/** 当前足迹中出现过的学科 */
const footprintSubjects = computed(() => [...new Set(footprints.value.map(f => f.subject || '其他'))])

/**
 * 学科多选筛选（Faceted Search）：OR 逻辑。
 * selectedSubjects 为空 → 空列表；否则展示学科属于已勾选集合的卡片。
 */
const filteredFootprints = computed(() => {
  if (!selectedSubjects.value.length) return []
  return footprints.value.filter(f => selectedSubjects.value.includes(f.subject || '其他'))
})

/** 足迹数据变化时，把新出现的学科默认纳入勾选，保持"全选"体验 */
watch(footprintSubjects, subjects => {
  const merged = new Set([...selectedSubjects.value, ...subjects])
  selectedSubjects.value = [...merged]
})

/** 仅保留日期（YYYY-MM-DD），不显示时刻 */
function fmtDate(t) {
  if (!t) return ''
  return String(t).slice(0, 10)
}

/** 点击笔记修改（弹窗扩大至约1.5倍） */
async function editComment(f) {
  try {
    const { value } = await ElMessageBox.prompt('修改该知识点的笔记', '编辑笔记', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: f.comment || '',
      inputType: 'textarea',
      customClass: 'fp-comment-box'
    })
    await api.put(`/analysis/profile/${f.id}/comment`, { comment: value.trim() })
    f.comment = value.trim()
    ElMessage.success('笔记已更新')
  } catch { /* 用户取消 */ }
}

/** 新增足迹 */
async function confirmAdd() {
  if (!addForm.value.footprint.trim()) {
    ElMessage.warning('请输入知识点名称')
    return
  }
  try {
    const res = await api.post('/analysis/profile', {
      subject: addForm.value.subject,
      footprint: addForm.value.footprint.trim(),
      comment: addForm.value.comment.trim()
    })
    ElMessage.success('足迹已添加')
    showAddDialog.value = false
    addForm.value = { subject: '', footprint: '', comment: '' }
    // 重新拉取足迹列表（含新建记录的 update_time）
    const userId = localStorage.getItem('userId')
    const radar = await api.get(`/analysis/student/${userId}/radar`)
    footprints.value = radar.data.footprints || []
  } catch { /* 接口错误已由拦截器提示 */ }
}

/** 删除足迹 */
async function deleteFootprint(f) {
  try {
    await ElMessageBox.confirm('删除后，该足迹将不可恢复', '删除足迹', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
      customClass: 'chat-msgbox chat-delete-box'
    })
    await api.delete(`/analysis/profile/${f.id}`)
    ElMessage.success('足迹已删除')
    footprints.value = footprints.value.filter(x => x.id !== f.id)
  } catch { /* 用户取消 */ }
}

function viewDetail(row) {
  try {
    detailRows.value = JSON.parse(row.detail || '[]')
    detailVisible.value = true
  } catch { /* 忽略 */ }
}

onMounted(async () => {
  const userId = localStorage.getItem('userId')
  const res = await api.get(`/analysis/student/${userId}/radar`)
  // 先加载足迹数据（不依赖图表，避免图表渲染异常时卡片不显示）
  footprints.value = res.data.footprints || []

  // 雷达图渲染（独立 try/catch，失败不影响其余数据加载）
  try {
    chart = echarts.init(radarRef.value)
    if (res.data.dimensions && res.data.dimensions.length) {
      chart.setOption({
        radar: { indicator: res.data.dimensions.map(d => ({ name: d, max: 100 })), radius: '66%',
                 axisName: { color: '#6b7386' }, splitArea: { areaStyle: { color: ['#fbfcff', '#f2f5fd'] } } },
        series: [{ type: 'radar',
          data: [{ value: res.data.values, name: '五维能力',
                   areaStyle: { color: 'rgba(74,125,255,.28)' }, itemStyle: { color: '#4a7dff' } }] }]
      })
    }
  } catch (e) { console.warn('雷达图渲染失败', e) }

  const [sess, prac] = await Promise.all([api.get('/agent/sessions'), api.get('/practice/records')])
  const all = []
  for (const s of sess.data) {
    const hist = await api.get(`/agent/history/${s.id}`)
    hist.data.filter(m => m.role === 'AI' && m.misconceptionType)
      .forEach(m => all.push({ ...m, sessionId: s.id }))
  }
  records.value = all.reverse()
  practices.value = prac.data
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => chart?.dispose())
</script>

<style scoped>
.profile { display: flex; flex-direction: column; gap: 18px; }
.chart { height: 340px; }

/* 我的足迹：卡片网格，每行 3 个 */
.footprint-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.fp-filter { display: flex; justify-content: flex-end; flex-wrap: wrap; gap: 6px; }
.fp-subjects :deep(.el-checkbox-button__inner) { border-radius: 999px; border-left-width: 1px; margin: 0 2px; }
.fp-subjects :deep(.el-checkbox-button:first-child .el-checkbox-button__inner) { border-left-width: 1px; border-radius: 999px; }
.fp-card {
  background: #fff;
  border: 1px solid #e8eefc;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(74,125,255,.06);
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 110px;
}
.fp-head { display: flex; align-items: center; gap: 8px; }
.fp-subj { flex-shrink: 0; font-weight: 600; }
.fp-name { font-weight: 600; font-size: 14.5px; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.fp-del {
  flex-shrink: 0;
  width: 20px; height: 20px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%;
  font-size: 12px; line-height: 1;
  color: var(--ink-3);
  cursor: pointer;
  transition: all .15s;
}
.fp-del:hover { background: #fef0f0; color: #f56c6c; }
.fp-comment {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.6;
  flex: 1;
  cursor: pointer;
  word-break: break-word;
  border-radius: 6px;
  padding: 2px 4px;
  transition: background .15s;
}
.fp-comment:hover { background: #f4f7fd; }
.fp-date { font-size: 11.5px; color: var(--ink-3); text-align: right; }
.fp-foot { display: flex; justify-content: flex-end; margin-top: 14px; }

.d-row { background: #f6fbf8; border-radius: 10px; padding: 12px 16px; margin-bottom: 10px; }
.d-row.wrong { background: #fdf3f5; }
.r-title { display: flex; align-items: center; gap: 8px; font-weight: 600; margin-bottom: 6px; }
.r-line { font-size: 13px; color: var(--ink-2); margin: 3px 0; }
.r-line .k { display: inline-block; width: 70px; color: var(--ink-3); }
</style>

<!-- ElMessageBox 渲染于 body，需用非 scoped 样式 -->
<style>
/* 编辑笔记弹窗：扩大至默认 420px 的约 1.5 倍 */
.fp-comment-box { width: 630px !important; max-width: 90vw; border-radius: 16px !important; }
.fp-comment-box .el-message-box__title { font-weight: 600; }
.fp-comment-box .el-message-box__input textarea { min-height: 140px !important; border-radius: 10px !important; }
.fp-comment-box .el-message-box__btns .el-button { border-radius: 999px !important; min-width: 84px; }
/* 删除足迹弹窗：复用胶囊+红色风格 */
.chat-delete-box .el-message-box__btns .el-button--primary {
  background: #f56c6c !important;
  border-color: #f56c6c !important;
}
.chat-delete-box .el-message-box__btns .el-button--primary:hover {
  background: #f78989 !important;
  border-color: #f78989 !important;
}
.chat-delete-box { border-radius: 16px !important; }
.chat-delete-box .el-message-box__btns .el-button { border-radius: 999px !important; min-width: 84px; }
</style>
