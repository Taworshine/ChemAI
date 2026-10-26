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
        <el-input v-model="keyword" size="small" :placeholder="ontology ? '在 Neo4j 本体中搜索（如：氧化）…' : '搜索知识点…'"
                  clearable style="width:190px" :prefix-icon="Search" />
        <el-button size="small" circle @click="zoom(1.25)"><el-icon><ZoomIn /></el-icon></el-button>
        <el-button size="small" circle @click="zoom(0.8)"><el-icon><ZoomOut /></el-icon></el-button>
        <el-button size="small" circle @click="fit"><el-icon><Aim /></el-icon></el-button>
        <el-button size="small" plain @click="expandAll">展开全部</el-button>
        <el-button size="small" plain @click="collapseAll">收起全部</el-button>
        <el-button size="small" :type="focusOn ? 'primary' : 'default'" plain @click="toggleFocus">
          {{ focusOn ? '聚焦中' : '聚焦模式' }}
        </el-button>
        <el-button size="small" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="body">
      <div class="soft-card canvas-card">
        <div class="legend">
          <template v-if="ontology">
            <span v-for="t in ontoLegend" :key="t.label">
              <i class="dot" :style="{ background: COLORS[t.label] || '#999' }"></i>{{ t.label === 'Section' ? '教材章节' : t.label }}{{ t.count > 1 ? ` ${t.count}` : '' }}
            </span>
          </template>
          <template v-else>
            <span><i class="dot" style="background:#4a7dff"></i>概念</span>
            <span><i class="dot" style="background:#ff9a4d"></i>题型</span>
            <span><i class="dot" style="background:#38b8a0"></i>思维路径</span>
            <span><i class="dot" style="background:#f5679a"></i>常见卡点</span>
          </template>
          <span class="stats faint">{{ statText }}</span>
        </div>
        <div ref="container" class="canvas"></div>
      </div>

      <div class="soft-card detail" v-if="current">
        <div class="detail-head">
          <el-tag size="small" effect="dark" :color="colorOf(current.nodeType)" style="border:none">{{ typeName(current.nodeType) }}</el-tag>
          <el-icon class="close" @click="current = null; provenance = null"><Close /></el-icon>
        </div>
        <h4>{{ current.label }}</h4>
        <div class="kv" v-for="(v, k) in displayProps" :key="k">
          <span class="k">{{ fieldName(k) }}</span>
          <span class="v">{{ v }}</span>
        </div>
        <template v-if="provenance">
          <el-divider style="margin:14px 0" />
          <p class="rel-title">知识点来源</p>
          <div v-if="provenance.sourceTask" class="prov-box">
            <span class="prov-doc">📄 {{ provenance.sourceTask.title }}</span>
            <span class="prov-meta">{{ { A: '教案', B: '讲义', C: '知识点' }[provenance.sourceTask.docType] || '' }} · {{ provenance.sourceTask.teacherName || '教师' }} 提供</span>
          </div>
          <div v-else class="prov-box preset">系统预置知识点</div>
        </template>
        <template v-if="related.length">
          <el-divider style="margin:14px 0" />
          <p class="rel-title">与「{{ current.label }}」相关</p>
          <div class="rel-item" v-for="r in related" :key="r.id" @click="focusNode(r.id)">
            <i class="dot" :style="{ background: colorOf(r.nodeType) }"></i>{{ r.label }}
            <span class="rel-type">{{ r.rel }}</span>
          </div>
        </template>
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
import api from '../../api'
import { COLORS, TYPE_NAMES, REL_NAMES, FIELD_NAMES, nodeLabel,
         toG6Data, filterBySubject, createGraph,
         computeVisible, expandPathTo, FORCE_LAYOUT, ONTO_LEGEND } from '../../utils/graphRender'

const container = ref(null)
const subjects = ref([])
const subject = ref('')
const keyword = ref('')
const current = ref(null)
const provenance = ref(null)   // 节点溯源（来源文档/教师）
const focusOn = ref(false)
const ontology = ref(false)   // true = 化学本体视图（Neo4j 中预置的高中化学本体数据）
let graph = null
let allNodes = []
let allEdges = []
let matched = new Set()       // 搜索命中节点 id，渲染时金色高亮
let expanded = new Set()   // 已展开节点：画布初始只显示核心（概念）节点，点击展开 1 跳邻居，再点收起
const counts = ref({ nodes: 0, total: 0, edges: 0 })

const colorOf = t => COLORS[t] || '#999'
const typeName = t => TYPE_NAMES[t] || t
const fieldName = k => FIELD_NAMES[k] || k

/** 本体模式下图例：按预置顺序统计当前数据里各类型数量（allNodes 非响应式，load 完成后手动刷新） */
const ontoLegend = ref([])

function refreshOntoLegend() {
  const c = {}
  allNodes.forEach(n => { c[n.label] = (c[n.label] || 0) + 1 })
  ontoLegend.value = ONTO_LEGEND.filter(t => c[t]).map(t => ({ label: t, count: c[t] }))
}

/** 切换 项目图谱 / 化学本体视图 */
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
  const res = await api.get('/graph/search', { params: { q: kw, scope: 'student' } })
  allNodes = res.data.nodes
  allEdges = res.data.edges
  matched = new Set(res.data.matched || [])
  expanded = new Set(allNodes.map(n => n.id))
  refreshOntoLegend()
  render()
}

const SKIP = new Set(['x', 'y'])
const displayProps = computed(() => current.value
  ? Object.fromEntries(Object.entries(current.value.props).filter(([k]) => !SKIP.has(k)))
  : {})

const statText = computed(() => counts.value.total
  ? `可见 ${counts.value.nodes} / 共 ${counts.value.total} 个知识点 · ${counts.value.edges} 条关系`
  : '加载中…')

const related = computed(() => {
  if (!current.value) return []
  const id = current.value.id
  const seen = new Set([id])
  const items = []
  allEdges.forEach(e => {
    let other = null, rel = ''
    if (e.source === id) { other = e.target; rel = e.type }
    else if (e.target === id) { other = e.source; rel = 'in' }
    if (other && !seen.has(other)) {
      seen.add(other)
      const node = allNodes.find(n => n.id === other)
      if (node) items.push({
        id: other,
        label: nodeLabel(node),
        nodeType: node.label,
        rel: rel === 'in' ? '属于' : (REL_NAMES[rel] || rel)
      })
    }
  })
  return items
})

function focusNode(id) {
  ensureVisible(id)
  const item = graph.findById(id)
  if (!item) return
  graph.getNodes().forEach(n => graph.setItemState(n, 'selected', false))
  graph.setItemState(item, 'selected', true)
  current.value = item.getModel()
  fetchProvenance(item.getModel())
  graph.focusItem(item, true, { easing: 'easeCubic', duration: 400 })
}

/** 溯源：点击节点时查询它来自哪份教案/讲义/知识点 */
function fetchProvenance(model) {
  provenance.value = null
  api.get('/graph/node/provenance', { params: { elementId: model.id } })
    .then(res => { if (current.value && current.value.id === model.id) provenance.value = res.data })
    .catch(() => {})
}

/** 目标节点若在收起区，自动展开它相邻的可见节点，把它带进画布 */
function ensureVisible(id) {
  if (keyword.value) return
  const vis = visibleData()
  if (vis.nodes.some(n => n.id === id)) return
  const visibleIds = new Set(vis.nodes.map(n => n.id))
  expandPathTo(allEdges, visibleIds, id).forEach(p => expanded.add(p))
  render()
}

/** 当前应显示的子图：搜索时全量显示；平时只显示核心节点 + 已展开节点的 1 跳邻居 */
function visibleData() {
  if (keyword.value) return { nodes: allNodes, edges: allEdges, hiddenCount: new Map() }
  return computeVisible(allNodes, allEdges, expanded)
}

/** 点击节点：展开 / 收起它的邻居 */
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

function zoom(ratio) { graph && graph.zoom(ratio) }
function fit() { graph && graph.fitView(40) }
function setSubject(s) { subject.value = s; load() }

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
    expanded = new Set(allNodes.map(n => n.id))   // 本体一次性全量展示
  } else {
    // 学生端只看已发布内容：入库节点经教师发布后才可见
    const res = await api.get('/graph/full', { params: { scope: 'student' } })
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

let graphInited = false
function render(center) {
  if (!graph) return
  const { nodes, edges, hiddenCount } = visibleData()
  counts.value = { nodes: nodes.length, total: allNodes.length, edges: edges.length }
  const data = toG6Data(nodes, edges, hiddenCount)
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

onMounted(async () => {
  const subj = await api.get('/graph/subjects')
  subjects.value = subj.data
  await load()
  graph = createGraph(container.value, {
    theme: 'student',   // 学习风格主题：圆润大节点、柔和蓝橙配色（教师端为教研紫青主题）
    onNodeClick: (model) => { current.value = model; fetchProvenance(model); toggleExpand(model) }
  })
  render()
  window.addEventListener('resize', resize)
})

function resize() {
  if (graph && container.value) graph.changeSize(container.value.clientWidth, container.value.clientHeight)
}

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  graph && graph.destroy()
})
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
.chip.on { background: linear-gradient(90deg, #6a94ff, #4a7dff); color: #fff; box-shadow: 0 4px 12px rgba(74,125,255,.3); }
/* 视图切换 chip：未选中时描边样式，与学科筛选 chip 区分层级 */
.chip.view-chip { border: 1.5px solid transparent; font-weight: 600; }
.chip.view-chip:not(.on) { border-color: #a9c2ff; color: #3d6de0; background: #f7faff; }
.chip.view-chip.onto.on { background: linear-gradient(90deg, #2bb3a3, #38b8a0); box-shadow: 0 4px 12px rgba(56,184,160,.32); }
.subjects .divider { width: 1px; height: 20px; background: var(--line); margin: 0 4px; align-self: center; }
.tools { display: flex; gap: 6px; align-items: center; }
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
.canvas { flex: 1; border-radius: 12px; background: linear-gradient(180deg, #fbfcff, #f4f7fd); border: 1px solid var(--line); position: relative; overflow: hidden; }
/* G6 小地图：固定在画布右下角 */
.canvas :deep(.g6-minimap) {
  position: absolute; right: 12px; bottom: 12px; border-radius: 10px;
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
.rel-title { font-size: 13px; color: var(--ink-3); margin: 0 0 8px; }
.rel-item {
  display: flex; align-items: center; gap: 6px; font-size: 13.5px; color: var(--ink-2);
  padding: 7px 10px; border-radius: 8px; cursor: pointer; transition: background .18s;
}
.rel-item:hover { background: #f4f7fd; color: var(--primary); }
.rel-item .rel-type { margin-left: auto; font-size: 11px; color: var(--ink-3); }
.prov-box {
  display: flex; flex-direction: column; gap: 3px;
  background: #f4f7fd; border-radius: 10px; padding: 10px 12px;
}
.prov-box .prov-doc { font-size: 13px; font-weight: 600; color: var(--ink) }
.prov-box .prov-meta { font-size: 12px; color: var(--ink-3) }
.prov-box.preset { font-size: 12px; color: var(--ink-3) }
</style>
