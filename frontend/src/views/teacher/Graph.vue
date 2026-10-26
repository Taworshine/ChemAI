<template>
  <div class="graph-page">
    <div class="soft-card toolbar">
      <div class="subjects">
        <div class="chip view-chip" :class="{ on: !ontology }" @click="setView(false)">项目图谱</div>
        <div class="chip view-chip onto" :class="{ on: ontology }" @click="setView(true)">化学本体 · Neo4j</div>
        <template v-if="!ontology">
          <span class="divider"></span>
          <div class="chip" :class="{ on: subject === '' }" @click="setSubject('')">全部学科</div>
          <div v-for="s in subjects" :key="s" class="chip" :class="{ on: subject === s }" @click="setSubject(s)">{{ s }}</div>
        </template>
      </div>
      <div class="tools">
        <el-input v-model="keyword" size="small" :placeholder="ontology ? '在 Neo4j 本体中搜索（如：氧化）…' : '搜索节点名称…'"
                  clearable style="width:190px" :prefix-icon="Search" />
        <el-button v-if="!ontology" size="small" :type="linking ? 'warning' : 'primary'" plain @click="toggleLink">
          {{ linking ? '点击目标节点完成连线' : '+ 创建关系' }}
        </el-button>
        <el-button size="small" :type="focusOn ? 'primary' : 'default'" plain @click="toggleFocus">
          {{ focusOn ? '聚焦中（点空白处还原）' : '聚焦模式' }}
        </el-button>
        <el-button size="small" plain @click="expandAll">展开全部</el-button>
        <el-button size="small" plain @click="collapseAll">收起全部</el-button>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="body">
      <div class="soft-card canvas-card">
        <div class="legend">
          <template v-if="ontology">
            <span v-for="t in ontoLegend" :key="t.label">
              <i class="dot" :style="{ background: THEME_COLORS[t.label] || '#999' }"></i>{{ t.label === 'Section' ? '教材章节' : t.label }}{{ t.count > 1 ? ` ${t.count}` : '' }}
            </span>
          </template>
          <template v-else>
            <span><i class="dot" :style="{ background: THEME_COLORS.Concept }"></i>概念</span>
            <span><i class="dot" :style="{ background: THEME_COLORS.QuestionType }"></i>题型</span>
            <span><i class="dot" :style="{ background: THEME_COLORS.ThinkingPath }"></i>思维路径</span>
            <span><i class="dot" :style="{ background: THEME_COLORS.Misconception }"></i>常见卡点</span>
          </template>
          <span class="stats faint">{{ statText }}</span>
          <span class="zoom-btns">
            <el-button size="small" circle @click="zoom(1.25)"><el-icon><ZoomIn /></el-icon></el-button>
            <el-button size="small" circle @click="zoom(0.8)"><el-icon><ZoomOut /></el-icon></el-button>
            <el-button size="small" circle @click="fit"><el-icon><Aim /></el-icon></el-button>
          </span>
        </div>
        <div ref="container" class="canvas"></div>
      </div>

      <div class="soft-card detail" v-if="current">
        <div class="detail-head">
          <el-tag size="small" effect="dark" :color="colorOf(current.nodeType)" style="border:none">{{ typeName(current.nodeType) }}</el-tag>
          <el-icon class="close" @click="current = null"><Close /></el-icon>
        </div>
        <h4>{{ current.label }}</h4>
        <div class="kv" v-for="(v, k) in displayProps" :key="k">
          <span class="k">{{ fieldName(k) }}</span>
          <span class="v">{{ v }}</span>
        </div>
        <template v-if="provenance">
          <el-divider style="margin:14px 0" />
          <div class="prov-title">节点溯源</div>
          <template v-if="provenance.sourceTask">
            <div class="kv"><span class="k">来源文档</span><span class="v">{{ provenance.sourceTask.title }}</span></div>
            <div class="kv"><span class="k">文档类型</span><span class="v">{{ { A: '教案', B: '讲义', C: '知识点录入' }[provenance.sourceTask.docType] || provenance.sourceTask.docType }}</span></div>
            <div class="kv"><span class="k">入库教师</span><span class="v">{{ provenance.sourceTask.teacherName || '-' }}</span></div>
            <div class="kv"><span class="k">入库时间</span><span class="v">{{ (provenance.sourceTask.createdAt || '').replace('T', ' ').slice(0, 16) }}</span></div>
            <div class="kv"><span class="k">任务状态</span><span class="v">{{ { CONFIRMED: '已入库', PUBLISHED: '已发布', OFFLINE: '已下线', ROLLED_BACK: '已回滚' }[provenance.sourceTask.status] || provenance.sourceTask.status }}</span></div>
          </template>
          <div v-else class="prov-preset">系统预置节点（非教师入库）</div>
          <div class="prov-rels" v-if="provenance.relations && provenance.relations.length">
            <div class="prov-title" style="margin-top:8px">关系（{{ provenance.relations.length }}）</div>
            <div class="prov-rel" v-for="(r, i) in provenance.relations.slice(0, 6)" :key="i">
              <span class="dir">{{ r.direction === 'out' ? '→' : '←' }}</span>
              <span class="rel-type">{{ r.type }}</span>
              <span class="rel-name">{{ r.otherName }}</span>
            </div>
          </div>
        </template>
        <el-divider style="margin:14px 0" />
        <el-form v-if="!ontology" label-position="top" size="small">
          <el-form-item v-for="f in editableFields" :key="f.key" :label="f.label">
            <el-input v-model="editForm[f.key]" type="textarea" :rows="1" autosize />
          </el-form-item>
        </el-form>
        <div v-if="!ontology" class="detail-actions">
          <el-button size="small" type="primary" @click="saveNode">保存修改</el-button>
          <el-popconfirm title="确定删除该节点及其所有关系？" @confirm="removeNode">
            <template #reference><el-button size="small" type="danger" plain>删除节点</el-button></template>
          </el-popconfirm>
        </div>
      </div>
      <div class="soft-card detail empty" v-else>
        <el-empty description="点击节点展开 / 收起上下游" :image-size="90" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { Search, Close, ZoomIn, ZoomOut, Aim } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'
import { colorsOf, TYPE_NAMES, FIELD_NAMES, toG6Data, filterBySubject, createGraph, computeVisible, FORCE_LAYOUT, ONTO_LEGEND } from '../../utils/graphRender'

/** 教师端图谱 = 教研风格主题（紫青配色，见 graphRender.js THEMES.teacher） */
const THEME = 'teacher'
const THEME_COLORS = colorsOf(THEME)

const container = ref(null)
const subjects = ref([])
const subject = ref('')
const keyword = ref('')
const linking = ref(false)
const current = ref(null)
const provenance = ref(null)   // 节点溯源：来源任务/教师/关系摘要
const editForm = ref({})
const editableFields = ref([])
const focusOn = ref(false)
const ontology = ref(false)   // true = 化学本体视图（Neo4j 中预置的高中化学本体数据）
let graph = null
let allNodes = []
let allEdges = []
let matched = new Set()       // 搜索命中节点 id，渲染时金色高亮
let linkSource = null
let expanded = new Set()   // 已展开节点：初始只显示核心（概念）节点，点击展开 1 跳邻居，再点收起
const counts = ref({ nodes: 0, total: 0, edges: 0 })

const colorOf = t => THEME_COLORS[t] || '#999'
const typeName = t => TYPE_NAMES[t] || t
const fieldName = k => FIELD_NAMES[k] || k

/** 本体模式下图例：按预置顺序统计当前数据里各类型数量（allNodes 非响应式，load 完成后手动刷新） */
const ontoLegend = ref([])

function refreshOntoLegend() {
  const c = {}
  allNodes.forEach(n => { c[n.label] = (c[n.label] || 0) + 1 })
  ontoLegend.value = ONTO_LEGEND.filter(t => c[t]).map(t => ({ label: t, count: c[t] }))
}

const SKIP = new Set(['x', 'y'])
const displayProps = computed(() => current.value
  ? Object.fromEntries(Object.entries(current.value.props).filter(([k]) => !SKIP.has(k)))
  : {})

const statText = computed(() => counts.value.total
  ? `可见 ${counts.value.nodes} / 共 ${counts.value.total} 个节点 · ${counts.value.edges} 条关系`
  : '加载中…')

const EDITABLE = {
  Concept: ['name', 'subject', 'grade', 'keywords', 'description'],
  QuestionType: ['name', 'subject', 'difficulty', 'typicalExample'],
  ThinkingPath: ['description', 'stepOrder', 'hint'],
  Misconception: ['description', 'frequency']
}

function setSubject(s) { subject.value = s; load() }

/** 切换 项目图谱 / 化学本体视图：本体数据直接来自 Neo4j 预置的高中化学本体 */
function setView(o) {
  if (ontology.value === o) return
  ontology.value = o
  keyword.value = ''
  matched = new Set()
  current.value = null
  load()
}

/** 搜索：防抖后走 Neo4j 服务端检索（/graph/search），返回命中节点 + 一跳邻居并金色高亮 */
let searchTimer = null
watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(runSearch, 300)
})

async function runSearch() {
  if (!graphInited) return
  const kw = keyword.value.trim()
  if (!kw) { matched = new Set(); load(); return }
  if (ontology.value) {
    // 本体模式：搜索项目种子 + 本体的全部 Neo4j 节点
    const res = await api.get('/graph/search', { params: { q: kw } })
    allNodes = res.data.nodes
    allEdges = res.data.edges
    matched = new Set(res.data.matched || [])
    const ids = new Set(allNodes.map(n => n.id))
    expanded = new Set([...expanded].filter(id => ids.has(id)))
    refreshOntoLegend()
    render()
  } else {
    // 项目模式：搜索全库，命中后自动切本体视图也能看到本体实体
    const res = await api.get('/graph/search', { params: { q: kw } })
    allNodes = res.data.nodes
    allEdges = res.data.edges
    matched = new Set(res.data.matched || [])
    expanded = new Set(allNodes.map(n => n.id))
    refreshOntoLegend()
    render()
  }
}

function toggleFocus() {
  focusOn.value = !focusOn.value
  if (graph) graph.setFocusMode(focusOn.value)
}

async function load() {
  if (ontology.value) {
    // 化学本体：直接读 Neo4j 里的 Entity + Section + 本体关系
    const res = await api.get('/graph/ontology')
    allNodes = res.data.nodes
    allEdges = res.data.edges
    expanded = new Set(allNodes.map(n => n.id))   // 本体一次性全量展示（113 节点规模适中）
  } else {
    const res = await api.get('/graph/full')
    const { nodes, edges } = filterBySubject(res.data.nodes, res.data.edges, subject.value)
    allNodes = nodes
    allEdges = edges
  }
  // 过滤后已不存在的节点从展开集合中清掉，避免残留失效 id
  const ids = new Set(allNodes.map(n => n.id))
  expanded = new Set([...expanded].filter(id => ids.has(id)))
  refreshOntoLegend()
  render()
}

/** 当前应显示的子图：搜索时全量显示；平时只显示核心节点 + 已展开节点的 1 跳邻居 */
function visibleData() {
  if (keyword.value) return { nodes: allNodes, edges: allEdges, hiddenCount: new Map() }
  return computeVisible(allNodes, allEdges, expanded)
}

/** 点击节点：展开 / 收起它的邻居，新节点以被点节点为中心向外弹开 */
function toggleExpand(model) {
  if (keyword.value) return
  if (expanded.has(model.id)) expanded.delete(model.id)
  else expanded.add(model.id)
  render([model.x, model.y])
  const item = graph.findById(model.id)
  if (item) {
    graph.setItemState(item, 'selected', true)
    graph.focusItem(item, true, { easing: 'easeCubic', duration: 300 })
  }
}

function expandAll() { expanded = new Set(allNodes.map(n => n.id)); render() }
function collapseAll() { expanded.clear(); render(); setTimeout(() => graph && graph.fitView(40), 500) }

let graphInited = false
function render(center) {
  if (!graph) return
  const { nodes, edges, hiddenCount } = visibleData()
  counts.value = { nodes: nodes.length, total: allNodes.length, edges: edges.length }
  const data = toG6Data(nodes, edges, hiddenCount, THEME)
  if (!graphInited) {
    graph.data(data)
    graph.render()
    graph.fitView(30)
    graphInited = true
    return
  }
  // 以刚点击的节点为布局中心：新展开的节点从它四周发散
  if (center) graph.updateLayout({ ...FORCE_LAYOUT, center })
  graph.changeData(data)
  // 搜索命中的节点金色高亮
  if (matched.size) {
    matched.forEach(id => {
      const item = graph.findById(id)
      if (item) graph.setItemState(item, 'matched', true)
    })
  }
}

function openDetail(model) {
  current.value = model
  provenance.value = null
  // 溯源：该节点来自哪份教案/讲义/知识点录入（种子/本体节点无 sourceTask，显示系统预置）
  api.get('/graph/node/provenance', { params: { elementId: model.id } })
    .then(res => { if (current.value && current.value.id === model.id) provenance.value = res.data })
    .catch(() => {})
  const fields = EDITABLE[model.nodeType] || ['name']
  editForm.value = {}
  fields.forEach(f => { editForm.value[f] = model.props[f] ?? '' })
  editableFields.value = fields.map(f => ({ key: f, label: FIELD_NAMES[f] || f }))
}

function toggleLink() {
  linking.value = !linking.value
  linkSource = null
  if (linking.value) ElMessage.info('关系模式：请先点击起点节点，再点击终点节点')
}

const REL_TYPE_NAMES = { HAS_QUESTION_TYPE: '包含题型', REQUIRES_PATH: '思维路径', COMMON_MISCONCEPTION: '常见卡点', PREREQUISITE: '前置知识' }

async function chooseRelType(source, target) {
  try {
    const { value } = await ElMessageBox.prompt(
      '输入关系类型编号：1 包含题型 / 2 需要思维路径 / 3 常见卡点 / 4 前置知识',
      '选择关系类型', { inputValue: '4', confirmButtonText: '创建' })
    const map = { '1': 'HAS_QUESTION_TYPE', '2': 'REQUIRES_PATH', '3': 'COMMON_MISCONCEPTION', '4': 'PREREQUISITE' }
    const type = map[value]
    if (!type) { ElMessage.warning('无效编号'); return }
    await api.post('/graph/edge', { source, target, type })
    ElMessage.success(`已创建关系：${REL_TYPE_NAMES[type]}`)
    load()
  } catch { /* 取消 */ }
}

function zoom(ratio) { graph && graph.zoom(ratio) }
function fit() { graph && graph.fitView(40) }

onMounted(async () => {
  const subj = await api.get('/graph/subjects')
  subjects.value = subj.data
  await load()
  graph = createGraph(container.value, {
    theme: THEME,
    onNodeClick: async (model) => {
      if (linking.value) {
        if (!linkSource) {
          linkSource = model.id
          const item = graph.findById(model.id)
          item && graph.setItemState(item, 'linksource', true)
          ElMessage.info(`已选起点「${model.label}」，请点击终点节点`)
        } else {
          graph.getNodes().forEach(n => graph.setItemState(n, 'linksource', false))
          const s = linkSource
          linkSource = null
          linking.value = false
          await chooseRelType(s, model.id)
        }
        return
      }
      openDetail(model)
      toggleExpand(model)
    },
    onEdgeClick: async (model) => {
      if (linking.value || ontology.value) return
      try {
        await ElMessageBox.confirm('要删除这条关系吗？', '删除关系', { type: 'warning' })
        await api.delete(`/graph/edge/${model.id}`)
        ElMessage.success('关系已删除')
        load()
      } catch { /* 取消 */ }
    },
    onNodeDragEnd: (model) => {
      // 本体数据是 Neo4j 预置资产，拖拽不回踩坐标；项目节点才保存位置
      if (ontology.value) return
      api.put(`/graph/node/${model.id}`, { x: Math.round(model.x), y: Math.round(model.y) })
    }
  })
  render()
  applyFocus()
  window.addEventListener('resize', resize)
})

/** 外部跳转直达：?focus=节点名（确认中心"写入成功"弹窗跳过来）——展开、居中、打开详情 */
async function applyFocus() {
  const name = new URLSearchParams(location.search).get('focus')
  if (!name || !graph) return
  let node = allNodes.find(n => n.label === name || n.props?.name === name)
  if (!node && subject.value) {
    // 节点不在当前学科筛选内：清空筛选重新加载再找
    subject.value = ''
    await load()
    node = allNodes.find(n => n.label === name || n.props?.name === name)
  }
  if (!node) { ElMessage.info(`图谱里没找到「${name}」，它可能还没发布或在其他视图`); return }
  expanded.add(node.id)
  render()
  setTimeout(() => {
    const item = graph.findById(node.id)
    if (item) graph.focusItem(item, true, { easing: 'easeCubic', duration: 500 })
    openDetail(node)
  }, 800)
}

function resize() {
  if (graph && container.value) graph.changeSize(container.value.clientWidth, container.value.clientHeight)
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  graph && graph.destroy()
})

async function saveNode() {
  const props = { ...editForm.value }
  if (props.stepOrder !== undefined) props.stepOrder = Number(props.stepOrder)
  if (props.frequency !== undefined) props.frequency = Number(props.frequency)
  await api.put(`/graph/node/${current.value.id}`, props)
  ElMessage.success('已保存到 Neo4j')
  load()
}

async function removeNode() {
  await api.delete(`/graph/node/${current.value.id}`)
  expanded.delete(current.value.id)
  ElMessage.success('节点已删除')
  current.value = null
  load()
}
</script>

<style scoped>
.graph-page { display: flex; flex-direction: column; gap: 16px; height: 100%; }
.toolbar { display: flex; justify-content: space-between; align-items: center; padding: 14px 20px; flex-wrap: wrap; gap: 10px; }
.subjects { display: flex; gap: 8px; flex-wrap: wrap; }
.chip {
  padding: 6px 14px; border-radius: 999px; background: #f2f5fc; color: var(--ink-2);
  font-size: 13px; cursor: pointer; transition: all .2s; user-select: none;
}
.chip:hover { background: #e6edfb; }
.chip.on { background: linear-gradient(90deg, #a78bfa, #8b5cf6); color: #fff; box-shadow: 0 4px 12px rgba(139,92,246,.32); }
/* 视图切换 chip：未选中时描边样式，与学科筛选 chip 区分层级 */
.chip.view-chip { border: 1.5px solid transparent; font-weight: 600; }
.chip.view-chip:not(.on) { border-color: #c9b8f5; color: #7c5cd6; background: #faf8ff; }
.chip.view-chip.onto.on { background: linear-gradient(90deg, #14b8a6, #0d9488); box-shadow: 0 4px 12px rgba(13,148,136,.32); }
.subjects .divider { width: 1px; height: 20px; background: var(--line); margin: 0 4px; align-self: center; }
.tools { display: flex; gap: 8px; }

.body { display: flex; gap: 16px; flex: 1; min-height: 0; }
/* 窄屏（如侧边预览面板）：详情栏移到画布下方，保证图谱始终有充足宽度 */
@media (max-width: 1100px) {
  .body { flex-direction: column; overflow-y: auto; }
  .canvas-card { flex: 1; min-height: 420px; }
  .detail { width: 100%; flex-shrink: 0; }
}
.canvas-card { flex: 1; display: flex; flex-direction: column; padding: 14px 16px; min-width: 0; }
.legend { display: flex; gap: 16px; align-items: center; color: var(--ink-2); font-size: 13px; padding: 2px 4px 10px; flex-wrap: wrap; row-gap: 6px; }
.dot { display: inline-block; width: 9px; height: 9px; border-radius: 50%; margin-right: 5px; }
.stats { margin-left: auto; font-size: 12px; }
.zoom-btns { display: flex; gap: 6px; }
/* 教师端画布：纯白 + 细网格，工具感；与学生端渐变画布区分 */
.canvas {
  flex: 1; border-radius: 12px; border: 1px solid var(--line); position: relative; overflow: hidden;
  background-color: #fdfdff;
  background-image: radial-gradient(circle, #e3e8f4 1px, transparent 1px);
  background-size: 22px 22px;
}
/* G6 小地图：教师端固定在画布右上角（学生端在右下角） */
.canvas :deep(.g6-minimap) {
  position: absolute; right: 12px; top: 12px; border-radius: 10px;
  border: 1px solid var(--line); box-shadow: 0 6px 18px rgba(60, 80, 150, .14);
  background: #fff; overflow: hidden;
}
/* 悬停提示框不拦截鼠标——之前它会挡住画布导致"点过之后滑不动" */
.canvas :deep(.g6-tooltip) { pointer-events: none !important; }

.detail { width: 300px; overflow-y: auto; }
.detail.empty { display: flex; align-items: center; justify-content: center; }
.detail-head { display: flex; justify-content: space-between; align-items: center; }
.detail-head .close { cursor: pointer; color: var(--ink-3); }
.detail-head .close:hover { color: var(--ink); }
.detail h4 { margin: 12px 0; color: var(--ink); }
.kv { display: flex; gap: 10px; font-size: 13px; margin: 5px 0; }
.kv .k { width: 62px; color: var(--ink-3); flex-shrink: 0; }
.kv .v { color: var(--ink-2); word-break: break-all; }
.detail-actions { display: flex; gap: 8px; margin-top: 4px; }
.prov-title { font-size: 13px; font-weight: 600; color: var(--ink); margin-bottom: 6px }
.prov-preset { font-size: 12px; color: var(--ink-3); background: #f4f6fb; border-radius: 8px; padding: 8px 10px }
.prov-rel { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--ink-2); padding: 3px 0 }
.prov-rel .dir { color: #4a7dff; font-weight: 600 }
.prov-rel .rel-type { color: var(--ink-3) }
</style>
