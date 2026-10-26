<template>
  <div class="confirm-page">
    <!-- 左侧：任务列表 -->
    <div class="soft-card task-list">
      <h4>入库任务</h4>
      <div class="task-scroll">
        <div v-for="t in tasks" :key="t.task.id" class="task-item" :class="{ on: currentId === t.task.id }"
             @click="select(t.task.id)">
          <div class="t-title">{{ t.task.title }}</div>
          <div class="t-meta">
            <span class="tag" :class="statusClass(t.task.status)">{{ statusText(t.task.status) }}</span>
            <span class="faint small">{{ { A: '教案', B: '讲义', C: '知识点' }[t.task.docType] }}</span>
            <el-badge v-if="t.pending > 0" :value="t.pending" type="warning" class="pend" />
          </div>
        </div>
        <el-empty v-if="!tasks.length" description="暂无入库任务" :image-size="80" />
      </div>
    </div>

    <!-- 右侧：确认工作区 -->
    <div class="soft-card workbench" v-if="task">
      <div class="wb-head">
        <div>
          <h3>{{ task.title }}</h3>
          <p class="faint small">AI 只负责提候选，您点"确认"的才会写进图谱——每条都可以改</p>
        </div>
        <div class="wb-actions">
          <el-button v-if="task.status === 'PENDING_CONFIRM'" size="small" plain @click="acceptAll">听 AI 的建议（一键决定）</el-button>
          <el-button v-if="task.status === 'PENDING_CONFIRM'" size="small" type="primary" :disabled="pending > 0"
                     @click="submit">确认写进图谱</el-button>
          <el-button v-if="task.status === 'CONFIRMED'" size="small" type="success" @click="publish">发布给学生</el-button>
          <el-button v-if="task.status === 'PUBLISHED'" size="small" type="warning" plain @click="offline">下线</el-button>
          <el-popconfirm v-if="['CONFIRMED','PUBLISHED','OFFLINE'].includes(task.status)"
                         width="300"
                         title="撤销将删除本次新建的知识点，合并进已有知识点的内容保留。确定撤销？"
                         confirm-button-text="确定撤销" confirm-button-type="danger"
                         cancel-button-text="取消" @confirm="rollback">
            <template #reference>
              <el-button size="small" type="danger" plain>撤销本次入库</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>

      <!-- AI 正在处理中的友好提示 -->
      <div class="ai-working" v-if="WORKING[task.status]">
        <span class="dots"><i></i><i></i><i></i></span>
        {{ WORKING[task.status] }}，完成后这里会出现候选清单，请稍等～
      </div>

      <template v-if="task.status === 'PENDING_CONFIRM' || items.length">
        <!-- 裁决进度 -->
        <div class="progress-row" v-if="task.status === 'PENDING_CONFIRM'">
          <el-progress :percentage="progressPct" :stroke-width="14"
                       :status="pending === 0 ? 'success' : ''" striped striped-flow />
          <span class="progress-text">
            已决定 {{ decided }} / {{ items.length }} 条
            <template v-if="pending === 0">· 全部决定好啦，可以提交 ✓</template>
          </span>
        </div>

        <div class="summary">
          <span class="sum-chip warn">还没决定 {{ pending }}</span>
          <span class="sum-chip new">建新节点 {{ counts.NEW || 0 }}</span>
          <span class="sum-chip merge">合并 / 挂接 {{ (counts.MERGE || 0) + (counts.RELATE || 0) }}</span>
          <span class="sum-chip dim">不要 {{ counts.IGNORE || 0 }}</span>
          <el-button size="small" text type="primary" @click="showManual = true">+ 手动补充知识点</el-button>
        </div>

        <div class="kind-chips">
          <div class="chip" :class="{ on: kindFilter === '' }" @click="kindFilter = ''">全部 {{ items.length }}</div>
          <div class="chip dup" :class="{ on: kindFilter === 'DUP' }" @click="kindFilter = 'DUP'">和库里很像 {{ dupCount }}</div>
          <div class="chip" :class="{ on: kindFilter === 'concept' }" @click="kindFilter = 'concept'">概念</div>
          <div class="chip" :class="{ on: kindFilter === 'qtype' }" @click="kindFilter = 'qtype'">题型</div>
          <div class="chip" :class="{ on: kindFilter === 'path' }" @click="kindFilter = 'path'">思维路径</div>
          <div class="chip" :class="{ on: kindFilter === 'misconception' }" @click="kindFilter = 'misconception'">易错点</div>
        </div>

        <div class="item-scroll">
          <div v-for="it in filtered" :key="it.id" class="item-card" :class="[it.decision.toLowerCase(), { dup: it.matchLevel }]">
            <div class="i-head">
              <el-tag size="small" effect="dark" :color="kindColor(it.kind)" style="border:none">{{ kindName(it.kind) }}</el-tag>
              <span v-if="it.matchLevel" class="match-tag" :class="it.matchLevel">
                {{ it.matchLevel === 'L1' ? '名字一样' : '长得很像' }} {{ pct(it.similarity) }}
              </span>
              <span v-if="lowConfidence(it)" class="low-conf" title="AI 对这条不太有把握，请重点核对">AI 不太确定，请看一眼</span>
              <span class="spacer"></span>
              <span class="dec-label">{{ decisionText(it.decision) }}</span>
            </div>

            <!-- 重复提示条（大白话） -->
            <div v-if="it.matchNodeName" class="dup-bar">
              <el-icon><Warning /></el-icon>
              知识图谱里已经有「<b>{{ it.matchNodeName }}</b>」了——建议合并，别建重复的
            </div>

            <!-- 概念 / 题型 / 易错点：行内编辑 -->
            <template v-if="it.kind !== 'path'">
              <div class="i-grid">
                <div class="g-field name">
                  <label>名称</label>
                  <el-input v-model="pl(it).name" size="small" @change="savePayload(it)" />
                </div>
                <div class="g-field">
                  <label>学科</label>
                  <el-select v-model="pl(it).subject" size="small" @change="savePayload(it)">
                    <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
                  </el-select>
                </div>
                <div class="g-field" v-if="it.kind === 'concept'">
                  <label>年级</label>
                  <el-select v-model="pl(it).grade" size="small" @change="savePayload(it)">
                    <el-option v-for="g in GRADES" :key="g" :label="g" :value="g" />
                  </el-select>
                </div>
                <div class="g-field" v-if="it.kind === 'qtype'">
                  <label>难度</label>
                  <el-select v-model="pl(it).difficulty" size="small" @change="savePayload(it)">
                    <el-option v-for="d in ['基础','中等','较难']" :key="d" :label="d" :value="d" />
                  </el-select>
                </div>
                <div class="g-field wide" v-if="pl(it).description !== undefined">
                  <label>描述</label>
                  <el-input v-model="pl(it).description" size="small" type="textarea" :rows="1" autosize @change="savePayload(it)" />
                </div>
              </div>
              <p v-if="pl(it).quote" class="quote faint">原文依据：{{ pl(it).quote }}</p>
            </template>

            <!-- 思维路径：步骤编辑 -->
            <template v-else>
              <div class="path-head">
                <span>对应题型：</span>
                <el-input v-model="pl(it).question_type" size="small" style="width:200px" @change="savePayload(it)" />
              </div>
              <div v-for="(s, i) in pl(it).steps" :key="i" class="step-row">
                <span class="step-no">{{ i + 1 }}</span>
                <el-input v-model="s.description" size="small" placeholder="这一步做什么" @change="savePayload(it)" />
                <el-input v-model="s.hint" size="small" placeholder="给学生的提示（可选）" @change="savePayload(it)" />
                <el-button size="small" text type="danger" :disabled="pl(it).steps.length <= 2"
                           @click="pl(it).steps.splice(i, 1); savePayload(it)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button size="small" text type="primary" @click="pl(it).steps.push({ order: pl(it).steps.length + 1, description: '', hint: '' })">
                + 加一步
              </el-button>
            </template>

            <!-- 裁决区：疑似重复用三张大选项卡，其余用简洁按钮 -->
            <div class="decide">
              <template v-if="it.kind !== 'path'">
                <div v-if="it.matchNodeId" class="opt-grid">
                  <button class="opt merge" :class="{ on: it.decision === 'MERGE' }" @click="decide(it, 'MERGE')">
                    <span class="opt-ico">⇲</span>
                    <span class="opt-t">合并进去</span>
                    <span class="opt-d">不建新点，内容补充到「{{ shortName(it.matchNodeName) }}」</span>
                  </button>
                  <button class="opt relate" :class="{ on: it.decision === 'RELATE' }" @click="decide(it, 'RELATE')">
                    <span class="opt-ico">⤳</span>
                    <span class="opt-t">挂在它前面</span>
                    <span class="opt-d">先学它、再学「{{ shortName(it.matchNodeName) }}」，连成前置关系</span>
                  </button>
                  <button class="opt new" :class="{ on: it.decision === 'NEW' }" @click="decide(it, 'NEW')">
                    <span class="opt-ico">✚</span>
                    <span class="opt-t">确实是新的</span>
                    <span class="opt-d">和「{{ shortName(it.matchNodeName) }}」不是一回事，新建知识点</span>
                  </button>
                </div>
                <button v-else class="d-btn new" :class="{ on: it.decision === 'NEW' }" @click="decide(it, 'NEW')">✚ 新知识，建节点</button>
              </template>
              <template v-else>
                <button class="d-btn merge" :class="{ on: it.decision === 'NEW' || it.decision === 'MERGE' }" @click="decide(it, 'NEW')">采纳这条思路</button>
              </template>
              <button class="d-btn ignore" :class="{ on: it.decision === 'IGNORE' }" @click="decide(it, 'IGNORE')">✕ 这条不要</button>
            </div>
          </div>
          <el-empty v-if="!filtered.length" description="该分组暂无条目" :image-size="80" />
        </div>
      </template>
    </div>
    <div class="soft-card workbench empty" v-else>
      <el-empty description="从左侧选择一个入库任务" :image-size="120" />
    </div>

    <!-- 手动补漏 -->
    <el-dialog v-model="showManual" title="手动补充知识点" width="460px">
      <el-form label-position="top">
        <el-form-item label="名称"><el-input v-model="manual.name" placeholder="教材通用名" /></el-form-item>
        <el-form-item label="学科">
          <el-select v-model="manual.subject" style="width:100%"><el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" /></el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="manual.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showManual = false">取消</el-button>
        <el-button type="primary" @click="addManual">添加为「新建」</el-button>
      </template>
    </el-dialog>

    <!-- 写入成功：成果一览 + 直达图谱 -->
    <el-dialog v-model="successVisible" width="500px" :show-close="true" class="ok-dialog">
      <div class="ok-hero">🎉</div>
      <h3 class="ok-title">已写进知识图谱</h3>
      <p class="ok-sub">
        新建概念 {{ okStats.newConcepts || 0 }} 个 · 合并 {{ okStats.mergeConcepts || 0 }} 条 ·
        题型 {{ okStats.newQtypes || 0 }} 个 · 思维路径 {{ okStats.paths || 0 }} 条 · 易错点 {{ okStats.misconceptions || 0 }} 个
      </p>
      <div v-if="okStats.createdNames?.length" class="ok-names">
        <span class="ok-label">新建的知识点（点名字直接跳到图谱看它）：</span>
        <span v-for="n in okStats.createdNames.slice(0, 12)" :key="n" class="ok-chip" @click="goGraph(n)">{{ n }}</span>
        <span v-if="okStats.createdNames.length > 12" class="ok-more">等 {{ okStats.createdNames.length }} 个</span>
      </div>
      <template #footer>
        <el-button @click="successVisible = false">留在这里</el-button>
        <el-button type="primary" @click="goGraph(okStats.createdNames?.[0])">去知识图谱看看</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Warning, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const SUBJECTS = ['语文', '数学', '英语', '物理', '化学', '生物', '历史', '地理', '政治']
const GRADES = ['高一', '高二', '高三']
// AI 正在处理各阶段的"人话"
const WORKING = {
  PARSING: 'AI 正在读这份文档',
  EXTRACTING: 'AI 正在提取知识点',
  MATCHING: 'AI 正在和已有图谱比对',
}

const tasks = ref([])
const currentId = ref(null)
const task = ref(null)
const items = ref([])
const kindFilter = ref('')
const showManual = ref(false)
const manual = ref({ name: '', subject: '化学', description: '' })
const successVisible = ref(false)
const okStats = ref({})
const payloadCache = new Map()
const router = useRouter()

const pending = computed(() => items.value.filter(i => i.decision === 'PENDING').length)
const decided = computed(() => items.value.length - pending.value)
const progressPct = computed(() => items.value.length ? Math.round(decided.value / items.value.length * 100) : 0)
const counts = computed(() => {
  const c = {}
  items.value.forEach(i => { c[i.decision] = (c[i.decision] || 0) + 1 })
  return c
})
const dupCount = computed(() => items.value.filter(i => i.matchLevel).length)
const filtered = computed(() => {
  let list = items.value
  if (kindFilter.value === 'DUP') list = list.filter(i => i.matchLevel)
  else if (kindFilter.value) list = list.filter(i => i.kind === kindFilter.value)
  // 疑似重复排前面，低置信度其次
  return [...list].sort((a, b) => (b.matchLevel ? 1 : 0) - (a.matchLevel ? 1 : 0))
})

/** 解析条目 payload（editedPayload 优先），用缓存避免编辑丢失 */
function pl(it) {
  if (!payloadCache.has(it.id)) {
    const raw = it.editedPayload || it.payload
    try { payloadCache.set(it.id, JSON.parse(raw)) }
    catch { payloadCache.set(it.id, {}) }
  }
  return payloadCache.get(it.id)
}

const pct = v => v == null ? '' : ` ${Math.round(v * 100)}%`
const shortName = n => (n && n.length > 12) ? n.slice(0, 12) + '…' : n
const lowConfidence = it => { const c = pl(it).confidence; return c != null && c < 0.6 }
const kindName = k => ({ concept: '概念', qtype: '题型', path: '思维路径', misconception: '易错点' }[k] || k)
const kindColor = k => ({ concept: '#4a7dff', qtype: '#ff9a4d', path: '#38b8a0', misconception: '#f5679a' }[k] || '#999')
const decisionText = d => ({ PENDING: '还没决定', NEW: '建新节点', MERGE: '合并', RELATE: '挂接', IGNORE: '已忽略' }[d] || d)
const statusText = s => ({
  PARSING: '读取中', EXTRACTING: '提取中', MATCHING: '比对中', PENDING_CONFIRM: '等您确认',
  CONFIRMED: '已入库', PUBLISHED: '已发布', OFFLINE: '已下线', ROLLED_BACK: '已撤销', FAILED: '失败', CANCELLED: '已取消'
}[s] || s)
const statusClass = s => ({
  PENDING_CONFIRM: 'warn', CONFIRMED: 'ok', PUBLISHED: 'ok', ROLLED_BACK: 'bad', FAILED: 'bad'
}[s] || 'dim')

async function loadTasks(keepCurrent = true) {
  const res = await api.get('/extract/tasks')
  tasks.value = res.data
  if (!keepCurrent || !currentId.value) {
    const first = res.data.find(t => t.task.status === 'PENDING_CONFIRM') || res.data[0]
    if (first) select(first.task.id)
  }
}

async function select(id) {
  currentId.value = id
  payloadCache.clear()
  kindFilter.value = ''
  const [t, its] = await Promise.all([
    api.get(`/extract/task/${id}`),
    api.get(`/extract/task/${id}/items`)
  ])
  task.value = t.data
  items.value = its.data
}

async function decide(it, decision) {
  it.decision = decision
  await api.put(`/extract/item/${it.id}`, { decision })
}

async function savePayload(it) {
  const payload = JSON.stringify(pl(it))
  it.editedPayload = payload
  await api.put(`/extract/item/${it.id}`, { payload })
}

async function acceptAll() {
  await api.post(`/extract/task/${currentId.value}/accept-all`)
  ElMessage.success('已按 AI 建议一键决定（一样的合并、其余新建），还可以逐条改')
  select(currentId.value)
}

async function submit() {
  try {
    await ElMessageBox.confirm(
      `确认写进知识图谱吗？\n建新节点 ${counts.value.NEW || 0} 个 · 合并/挂接 ${(counts.value.MERGE || 0) + (counts.value.RELATE || 0)} 条 · 丢弃 ${counts.value.IGNORE || 0} 条`,
      '确认入库', { type: 'warning', confirmButtonText: '写进图谱' })
  } catch { return }
  const res = await api.post(`/extract/task/${currentId.value}/submit`)
  okStats.value = res.data || {}
  successVisible.value = true
  await select(currentId.value)
  loadTasks()
}

/** 跳到图谱并定位到某个新建节点（Neo4j 协同：确认 → 立刻可视化） */
function goGraph(name) {
  successVisible.value = false
  if (name) router.push({ path: '/teacher/graph', query: { focus: name } })
  else router.push('/teacher/graph')
}

async function publish() {
  await api.post(`/extract/task/${currentId.value}/publish`)
  ElMessage.success('已发布，学生端可见')
  select(currentId.value); loadTasks()
}

async function offline() {
  try { await ElMessageBox.confirm('下线后学生端立即看不到这些知识点，确定？', '下线', { type: 'warning' }) } catch { return }
  await api.post(`/extract/task/${currentId.value}/offline`)
  ElMessage.success('已下线')
  select(currentId.value); loadTasks()
}

async function rollback() {
  const res = await api.post(`/extract/task/${currentId.value}/rollback`)
  ElMessage.success(`已撤销：删除本次新建节点 ${res.data.deleted}/${res.data.recorded} 个，合并的内容保留`)
  select(currentId.value); loadTasks()
}

async function addManual() {
  if (!manual.value.name.trim()) { ElMessage.warning('请填写名称'); return }
  await api.post(`/extract/task/${currentId.value}/manual-item`, {
    kind: 'concept',
    payload: JSON.stringify({ ...manual.value, grade: '高一', keywords: [], confidence: 1, quote: '教师手动补充' })
  })
  ElMessage.success('已添加')
  showManual.value = false
  manual.value = { name: '', subject: '化学', description: '' }
  select(currentId.value)
}

onMounted(async () => {
  await loadTasks(false)
  const q = Number(new URLSearchParams(location.search).get('taskId') || useRoute().query.taskId)
  if (q) select(q)
})
</script>

<style scoped>
.confirm-page { display: flex; gap: 16px; height: 100%; min-height: 0; }
.task-list { width: 250px; flex-shrink: 0; padding: 16px 14px; display: flex; flex-direction: column; }
.task-list h4 { margin: 2px 6px 12px; color: var(--ink); }
.task-scroll { overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 8px; }
.task-item { padding: 10px 12px; border-radius: 10px; cursor: pointer; border: 1.5px solid transparent; transition: all .18s; }
.task-item:hover { background: #f4f7fd; }
.task-item.on { background: #f0f5ff; border-color: #b9cdfa; }
.t-title { font-size: 13.5px; font-weight: 600; color: var(--ink); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.t-meta { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.tag { font-size: 11.5px; font-weight: 600; }
.tag.warn { color: #d48806; } .tag.ok { color: #2f9e6e; } .tag.bad { color: #e05260; } .tag.dim { color: var(--ink-3); }
.pend { margin-left: auto; }

.workbench { flex: 1; min-width: 0; padding: 18px 22px; display: flex; flex-direction: column; }
.workbench.empty { align-items: center; justify-content: center; }
.wb-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; }
.wb-head h3 { margin: 0 0 4px; color: var(--ink); }
.wb-head p { margin: 0; }
.wb-actions { display: flex; gap: 8px; flex-shrink: 0; }

/* AI 处理中的跳点提示 */
.ai-working { display: flex; align-items: center; gap: 10px; margin-top: 14px; padding: 14px 16px; border-radius: 12px; background: linear-gradient(90deg, #f0f5ff, #eafaf5); color: var(--ink-2); font-size: 13.5px; }
.dots { display: inline-flex; gap: 5px; }
.dots i { width: 8px; height: 8px; border-radius: 50%; background: #4a7dff; animation: bounce 1.2s infinite; }
.dots i:nth-child(2) { animation-delay: .18s; background: #38b8a0; }
.dots i:nth-child(3) { animation-delay: .36s; background: #a06ef5; }
@keyframes bounce { 0%, 60%, 100% { transform: translateY(0); opacity: .5; } 30% { transform: translateY(-6px); opacity: 1; } }

/* 裁决进度 */
.progress-row { display: flex; align-items: center; gap: 14px; margin-top: 14px; }
.progress-row :deep(.el-progress) { flex: 1; }
.progress-text { font-size: 12.5px; color: var(--ink-2); white-space: nowrap; }

.summary { display: flex; align-items: center; gap: 10px; margin: 14px 0 10px; flex-wrap: wrap; }
.sum-chip { padding: 4px 12px; border-radius: 999px; font-size: 12.5px; font-weight: 600; }
.sum-chip.warn { background: #fff7e8; color: #d48806; }
.sum-chip.new { background: #eaf1ff; color: #3d6de0; }
.sum-chip.merge { background: #e9f8f3; color: #1f9e85; }
.sum-chip.dim { background: #f2f4f8; color: var(--ink-3); }

.kind-chips { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px; }
.chip { padding: 5px 14px; border-radius: 999px; background: #f2f5fc; color: var(--ink-2); font-size: 12.5px; cursor: pointer; transition: all .18s; user-select: none; }
.chip:hover { background: #e6edfb; }
.chip.on { background: linear-gradient(90deg, #6a94ff, #4a7dff); color: #fff; box-shadow: 0 3px 10px rgba(74,125,255,.28); }
.chip.dup.on { background: linear-gradient(90deg, #f5a623, #e8890c); box-shadow: 0 3px 10px rgba(232,137,12,.28); }

.item-scroll { overflow-y: auto; flex: 1; display: flex; flex-direction: column; gap: 12px; padding-right: 4px; }
.item-card { border: 1.5px solid var(--line); border-radius: 14px; padding: 14px 16px; transition: all .18s; background: #fff; }
.item-card:hover { box-shadow: 0 6px 18px rgba(70, 95, 160, .1); }
.item-card.dup { border-color: #f3c98a; background: #fffdf8; }
.item-card.merge { border-color: #8fd9c2; }
.item-card.ignore { opacity: .55; }
.i-head { display: flex; align-items: center; gap: 10px; }
.i-head .spacer { flex: 1; }
.match-tag { font-size: 11.5px; font-weight: 700; padding: 2px 10px; border-radius: 999px; }
.match-tag.L1 { background: #ffe9d6; color: #d46b08; }
.match-tag.L2 { background: #fff3d6; color: #b8860b; }
.low-conf { font-size: 11.5px; font-weight: 700; padding: 2px 10px; border-radius: 999px; background: #ffe4e8; color: #d4385c; }
.dec-label { font-size: 12.5px; font-weight: 700; color: var(--ink-3); }
.item-card.new .dec-label { color: #3d6de0; }
.item-card.merge .dec-label { color: #1f9e85; }
.item-card.ignore .dec-label { color: var(--ink-3); }

.dup-bar { display: flex; align-items: center; gap: 6px; margin-top: 10px; padding: 8px 12px; border-radius: 8px; background: #fff5e3; color: #8a5a00; font-size: 12.5px; }

.i-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 10px; margin-top: 12px; }
.i-grid .wide { grid-column: 1 / -1; }
.g-field label { display: block; font-size: 11.5px; color: var(--ink-3); margin-bottom: 3px; }
.quote { margin: 8px 0 0; font-size: 12px; border-left: 3px solid var(--line); padding-left: 8px; }

.path-head { display: flex; align-items: center; gap: 8px; margin: 12px 0 10px; font-size: 13px; color: var(--ink-2); }
.step-row { display: grid; grid-template-columns: 28px 1fr 1fr 32px; gap: 8px; align-items: center; margin-bottom: 8px; }
.step-no { width: 22px; height: 22px; border-radius: 50%; background: #eaf1ff; color: #3d6de0; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; }

.decide { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; align-items: stretch; }
.d-btn { padding: 6px 16px; border-radius: 999px; border: 1.5px solid var(--line); background: #fff; font-size: 12.5px; cursor: pointer; color: var(--ink-2); transition: all .18s; }
.d-btn:hover { border-color: #b9cdfa; color: #3d6de0; }
.d-btn.new.on { background: #4a7dff; border-color: #4a7dff; color: #fff; }
.d-btn.merge.on { background: #38b8a0; border-color: #38b8a0; color: #fff; }
.d-btn.ignore.on { background: #9aa7bd; border-color: #9aa7bd; color: #fff; }

/* 疑似重复：三张大选项卡 */
.opt-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; flex: 1; }
.opt { display: flex; flex-direction: column; align-items: flex-start; gap: 3px; padding: 10px 12px; border-radius: 12px; border: 1.5px solid var(--line); background: #fff; cursor: pointer; text-align: left; transition: all .18s; }
.opt:hover { transform: translateY(-2px); box-shadow: 0 6px 14px rgba(70,95,160,.12); }
.opt-ico { font-size: 17px; line-height: 1; }
.opt-t { font-size: 13px; font-weight: 700; color: var(--ink); }
.opt-d { font-size: 11.5px; color: var(--ink-3); line-height: 1.45; }
.opt.merge.on { border-color: #38b8a0; background: #effaf7; }
.opt.merge.on .opt-t { color: #178f76; }
.opt.relate.on { border-color: #a06ef5; background: #f7f2ff; }
.opt.relate.on .opt-t { color: #8a4de8; }
.opt.new.on { border-color: #4a7dff; background: #f0f5ff; }
.opt.new.on .opt-t { color: #3d6de0; }

/* 成功弹窗 */
.ok-hero { font-size: 44px; text-align: center; margin-bottom: 6px; }
.ok-title { margin: 0 0 8px; text-align: center; color: var(--ink); }
.ok-sub { margin: 0 0 14px; text-align: center; color: var(--ink-2); font-size: 13px; line-height: 1.7; }
.ok-names { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.ok-label { width: 100%; text-align: center; font-size: 12px; color: var(--ink-3); margin-bottom: 2px; }
.ok-chip { padding: 4px 12px; border-radius: 999px; background: #eaf1ff; color: #3d6de0; font-size: 12.5px; font-weight: 600; cursor: pointer; transition: all .18s; }
.ok-chip:hover { background: #4a7dff; color: #fff; }
.ok-more { font-size: 12px; color: var(--ink-3); align-self: center; }

.small { font-size: 12px; }
@media (max-width: 1000px) {
  .confirm-page { flex-direction: column; overflow-y: auto; }
  .task-list { width: 100%; max-height: 220px; }
  .workbench { min-height: 60vh; }
  .opt-grid { grid-template-columns: 1fr; }
}
</style>
