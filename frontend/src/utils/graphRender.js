/**
 * 知识图谱 G6 渲染公共模块（教师端 / 学生端共用）
 */
import G6 from '@antv/g6'

export const TYPE_NAMES = { Concept: '概念', QuestionType: '题型', ThinkingPath: '思维路径', Misconception: '卡点' }
export const REL_NAMES = { HAS_QUESTION_TYPE: '包含题型', REQUIRES_PATH: '思维路径', COMMON_MISCONCEPTION: '常见卡点', PREREQUISITE: '前置知识' }
export const FIELD_NAMES = { id: '编号', name: '名称', type: '类别', subject: '学科', grade: '年级', keywords: '关键词', description: '描述',
  difficulty: '难度', typicalExample: '典型例题', stepOrder: '步骤序号', hint: '启发提示', frequency: '出现频次' }

/**
 * 化学本体（Neo4j 中预置的高中化学本体数据）节点类型 → 说明。
 * 本体节点的 label 直接就是中文子类型（概念/物质/反应类型…），这里只补全图例顺序。
 */
export const ONTO_LEGEND = ['概念', '物质', '反应类型', '化学量', '化学式', '电极', '装置', '实验', '迷思概念', '原理', 'Section']

/** hex 颜色按 amt 微调：正数调亮、负数调暗（用于从主题色自动生成 3D 球体的亮/暗/描边三色） */
export function shade(hex, amt) {
  const n = parseInt(hex.slice(1), 16)
  const f = c => {
    const v = amt > 0 ? c + (255 - c) * amt : c * (1 + amt)
    return Math.round(v).toString(16).padStart(2, '0')
  }
  return `#${f((n >> 16) & 255)}${f((n >> 8) & 255)}${f(n & 255)}`
}
const ballCache = new Map()
/** 从主题色一键生成球体三色（本体类型没有手写 ball 时使用） */
export function ballOf(color) {
  if (!ballCache.has(color)) {
    ballCache.set(color, { light: shade(color, .48), dark: shade(color, -.08), stroke: shade(color, -.32) })
  }
  return ballCache.get(color)
}

/**
 * 双主题：教师端「教研风格」与学生端「学习风格」在配色 / 节点大小 / 边线上完全不同，避免两端照搬。
 * student —— 圆润柔和、节点大、阴影软（学习探索感）
 * teacher —— 冷峻专业、节点小、间距密、紫青配色（编辑工具感）
 */
export const THEMES = {
  student: {
    colors: { Concept: '#4a7dff', QuestionType: '#ff9a4d', ThinkingPath: '#38b8a0', Misconception: '#f5679a',
      // 化学本体类型（Neo4j 导入数据的中文子类型）
      '概念': '#4a7dff', '物质': '#ff9a4d', '反应类型': '#38b8a0', '化学量': '#e8b93c',
      '化学式': '#a06ef5', '电极': '#4db8e8', '装置': '#8496b0', '实验': '#ef7d7d',
      '迷思概念': '#f5679a', '原理': '#6d7ff0', 'Section': '#9aa7bd' },
    balls: {
      Concept:      { light: '#a8c0ff', dark: '#3a66e0', stroke: '#2c4fb0' },
      QuestionType: { light: '#ffd0a3', dark: '#f07f1d', stroke: '#c05f10' },
      ThinkingPath: { light: '#9fe8d6', dark: '#1f9e85', stroke: '#12735f' },
      Misconception:{ light: '#ffb3cd', dark: '#e23a7d', stroke: '#b02260' }
    },
    size: 48, labelFill: '#4a5268', labelSize: 12,
    edge: '#c3cde4', edgeHover: '#4a7dff',
    shadow: 'rgba(50, 70, 130, .35)', shadowStrong: 'rgba(50, 70, 130, .5)',
    linkDistance: 150, nodeSpacing: 46
  },
  teacher: {
    colors: { Concept: '#8b5cf6', QuestionType: '#0ea5e9', ThinkingPath: '#f59e0b', Misconception: '#ef4444',
      // 化学本体类型（教研主题：与种子类型错开的紫青冷色系）
      '概念': '#8b5cf6', '物质': '#0ea5e9', '反应类型': '#f59e0b', '化学量': '#84cc16',
      '化学式': '#d946ef', '电极': '#14b8a6', '装置': '#64748b', '实验': '#ef4444',
      '迷思概念': '#f43f5e', '原理': '#6366f1', 'Section': '#94a3b8' },
    balls: {
      Concept:      { light: '#c4b5fd', dark: '#6d28d9', stroke: '#5b21b6' },
      QuestionType: { light: '#a5e3f5', dark: '#0284c7', stroke: '#0369a1' },
      ThinkingPath: { light: '#fcd9a0', dark: '#d97706', stroke: '#b45309' },
      Misconception:{ light: '#fca5a5', dark: '#dc2626', stroke: '#b91c1c' }
    },
    size: 40, labelFill: '#3f3f56', labelSize: 11.5,
    edge: '#b6bdd6', edgeHover: '#8b5cf6',
    shadow: 'rgba(80, 60, 160, .3)', shadowStrong: 'rgba(80, 60, 160, .45)',
    linkDistance: 130, nodeSpacing: 40
  }
}
/** 兼容旧引用：默认学生主题色 */
export const COLORS = THEMES.student.colors
export function colorsOf(theme) { return THEMES[theme || 'student'].colors }
const DEFAULT_BALL = { light: '#c9d4ea', dark: '#7c8db0', stroke: '#5a6a8c' }

export function nodeLabel(n) {
  return n.props.name || n.props.description || TYPE_NAMES[n.label]
}

/** 后端数据 → G6 data（3D 球体圆形节点）；badges: Map<节点id, 未展开邻居数>，有值的节点名字后追加 "+N"；theme 决定两端配色 */
export function toG6Data(allNodes, allEdges, badges, theme = 'student') {
  const T = THEMES[theme] || THEMES.student
  const nodes = allNodes.map(n => {
    const ball = T.balls[n.label] || ballOf(T.colors[n.label] || '#8496b0')
    const more = badges && badges.get && badges.get(n.id)
    return {
      id: n.id,
      label: nodeLabel(n) + (more ? ` +${more}` : ''),
      x: n.props.x || 120 + Math.random() * 720,
      y: n.props.y || 80 + Math.random() * 420,
      type: 'circle',
      size: T.size,
      style: {
        fill: `r(0.35,0.3,0.95) 0:${ball.light} 1:${ball.dark}`,
        stroke: ball.stroke,
        lineWidth: 2,
        shadowColor: T.shadow,
        shadowBlur: 12,
        shadowOffsetY: 4
      },
      labelCfg: {
        position: 'bottom',
        offset: 10,
        style: { fontSize: T.labelSize, fill: T.labelFill, fontWeight: 600 }
      },
      // 每个节点自带状态样式：hover / selected 使用同色系深色描边，绝不出现白色描边
      stateStyles: {
        hover: { stroke: ball.stroke, lineWidth: 5, shadowBlur: 22, shadowColor: T.shadowStrong },
        selected: { stroke: ball.stroke, lineWidth: 4, shadowBlur: 20, shadowColor: T.shadow },
        linksource: { stroke: '#ff9a4d', lineWidth: 5 }
      },
      nodeType: n.label,
      props: n.props
    }
  })
  const ids = new Set(nodes.map(n => n.id))
  const edges = allEdges.filter(e => ids.has(e.source) && ids.has(e.target)).map(e => ({
    source: e.source,
    target: e.target,
    id: e.id,
    relType: e.type,
    type: 'quadratic',
    style: {
      stroke: T.edge,
      lineWidth: 1.5,
      endArrow: { path: G6.Arrow.triangle(7, 9, 14), fill: T.edge },
      shadowColor: 'rgba(120,140,190,.15)',
      shadowBlur: 3
    }
  }))
  return { nodes, edges }
}

/** 学科过滤：概念/题型按学科属性，路径/卡点跟随其上游题型 */
export function filterBySubject(nodes, edges, subject) {
  if (!subject) return { nodes, edges }
  const keepIds = new Set(nodes.filter(n =>
    (n.label === 'Concept' || n.label === 'QuestionType') && n.props.subject === subject).map(n => n.id))
  edges.forEach(e => { if (keepIds.has(e.source)) keepIds.add(e.target) })
  return {
    nodes: nodes.filter(n => keepIds.has(n.id)),
    edges: edges.filter(e => keepIds.has(e.source) && keepIds.has(e.target))
  }
}

/** 核心节点类型：概念是知识骨架，画布初始只显示它们，点击再展开上下游 */
export const CORE_TYPES = ['Concept']

/**
 * 由展开状态计算可见子图：
 *   可见 = 核心节点 ∪ 每个已展开节点的 1 跳邻居
 * expandedIds: Set<节点id>；返回 { nodes, edges, hiddenCount: Map<可见节点id, 隐藏的邻居数> }
 */
export function computeVisible(allNodes, allEdges, expandedIds) {
  const visible = new Set(allNodes.filter(n => CORE_TYPES.includes(n.label)).map(n => n.id))
  for (const id of expandedIds) {
    allEdges.forEach(e => {
      if (e.source === id) visible.add(e.target)
      if (e.target === id) visible.add(e.source)
    })
  }
  const nodes = allNodes.filter(n => visible.has(n.id))
  const edges = allEdges.filter(e => visible.has(e.source) && visible.has(e.target))
  // 每个可见节点还有多少邻居没显示（用于 "+N" 徽标提示可展开）
  const hiddenCount = new Map()
  nodes.forEach(n => {
    let c = 0
    allEdges.forEach(e => {
      if (e.source === n.id && !visible.has(e.target)) c++
      if (e.target === n.id && !visible.has(e.source)) c++
    })
    if (c > 0) hiddenCount.set(n.id, c)
  })
  return { nodes, edges, hiddenCount }
}

/** 找到把 target 带进可见区的那个端点（用于"详情里点相关节点"时自动展开） */
export function expandPathTo(allEdges, visibleIds, target) {
  const path = []
  for (const e of allEdges) {
    if ((e.source === target && visibleIds.has(e.target)) ||
        (e.target === target && visibleIds.has(e.source))) {
      const other = e.source === target ? e.target : e.source
      if (!path.includes(other)) path.push(other)
    }
  }
  return path
}

export function filterByKeyword(nodes, edges, keyword) {
  if (!keyword) return { nodes, edges }
  const hit = new Set(nodes.filter(n => JSON.stringify(n.props).includes(keyword)).map(n => n.id))
  return {
    nodes: nodes.filter(n => hit.has(n.id)),
    edges: edges.filter(e => hit.has(e.source) && hit.has(e.target))
  }
}

/** 力导向布局配置：节点有碰撞体积（preventOverlap），展开时新节点从中心向外弹开 */
export const FORCE_LAYOUT = {
  type: 'force',
  preventOverlap: true,   // 节点间碰撞排斥，不会叠在一起
  nodeSpacing: 46,
  linkDistance: 150,
  nodeStrength: -90,
  edgeStrength: 0.04,
  animate: true,          // 布局过程有动画，不再"僵硬"
  alphaDecay: 0.028       // 冷却不那么快，运动更持久
}

/**
 * 创建美化后的 G6 实例
 * hooks: { onNodeClick(model,item), onEdgeClick(model), onNodeDragEnd(model) }
 */
export function createGraph(container, hooks = {}) {
  const theme = hooks.theme || 'student'
  const T = THEMES[theme] || THEMES.student
  const graph = new G6.Graph({
    container,
    width: container.clientWidth,
    height: container.clientHeight,
    layout: { ...FORCE_LAYOUT, linkDistance: T.linkDistance, nodeSpacing: T.nodeSpacing },
    modes: {
      default: [
        // 画布拖拽平移（按住空白处拖动）+ 滚轮缩放，均开启，保证"可滑动"
        { type: 'drag-canvas', enableOptimize: true },
        { type: 'zoom-canvas', sensitivity: 1.6, enableOptimize: true },
        'drag-node',
        { type: 'tooltip',
          shouldBegin: e => ['node', 'edge'].includes(e.item.getType()),
          formatText: model => {
            if (model.relType) return `关系：${REL_NAMES[model.relType] || model.relType}`
            const p = model.props || {}
            const typeText = TYPE_NAMES[model.nodeType] || model.nodeType || ''
            const extra = p.subject ? ` · ${p.subject}` : (p.type ? ` · ${p.type}` : '')
            const desc = p.description ? `\n${p.description}` : ''
            return `${typeText}：${model.label}${extra}${desc}`
          } }
      ]
    },
    plugins: [
      // 右下角小地图：大图滑动时随时定位全局
      new G6.Minimap({ size: [150, 92], type: 'keyShape', className: 'g6-minimap' })
    ],
    defaultNode: {
      style: { cursor: 'pointer' }
    },
    defaultEdge: {
      style: { cursor: 'pointer' }
    },
    nodeStateStyles: {
      hover: { lineWidth: 5, shadowBlur: 22 },
      selected: { lineWidth: 4, stroke: T.colors.Concept, shadowBlur: 20 },
      linksource: { stroke: '#ff9a4d', lineWidth: 5 },
      matched: { stroke: '#ffb020', lineWidth: 5, shadowBlur: 26, shadowColor: 'rgba(255,176,32,.55)' },
      faded: { opacity: 0.15 }
    },
    edgeStateStyles: {
      hover: { stroke: T.edgeHover, lineWidth: 2.5 },
      faded: { opacity: 0.08 }
    }
  })

  // ---- 聚焦模式（默认关闭）：开启后点击节点，无关节点淡出为浅灰低透明，相关节点保持彩色 ----
  graph.focusMode = false
  const clearFade = () => {
    graph.getNodes().forEach(n => graph.setItemState(n, 'faded', false))
    graph.getEdges().forEach(e => graph.setItemState(e, 'faded', false))
  }
  graph.setFocusMode = on => {
    graph.focusMode = on
    if (!on) clearFade()
  }
  graph.on('node:click', e => {
    if (!graph.focusMode) return
    const keep = new Set([e.item.getID()])
    e.item.getNeighbors().forEach(n => keep.add(n.getID()))
    const keepEdges = new Set(e.item.getEdges().map(edge => edge.getID()))
    graph.getNodes().forEach(n => graph.setItemState(n, 'faded', !keep.has(n.getID())))
    graph.getEdges().forEach(edge => graph.setItemState(edge, 'faded', !keepEdges.has(edge.getID())))
  })
  graph.on('canvas:click', () => { if (graph.focusMode) clearFade() })

  if (hooks.onNodeClick) graph.on('node:click', e => hooks.onNodeClick(e.item.getModel(), e.item))
  if (hooks.onEdgeClick) graph.on('edge:click', e => hooks.onEdgeClick(e.item.getModel()))
  if (hooks.onNodeDragEnd) graph.on('node:dragend', e => hooks.onNodeDragEnd(e.item.getModel()))

  // 悬停交互：仅当前节点轻微上浮放大 + 阴影加深，相连的边高亮，其余节点完全不变
  graph.on('node:mouseenter', e => {
    const item = e.item
    const model = item.getModel()
    graph.updateItem(item, {
      size: model.size + 6,
      style: { shadowBlur: 26, shadowOffsetY: 7, shadowColor: T.shadowStrong }
    })
    item.getEdges().forEach(edge => graph.setItemState(edge, 'hover', true))
  })
  graph.on('node:mouseleave', e => {
    const item = e.item
    const model = item.getModel()
    graph.updateItem(item, {
      size: Math.max(T.size, model.size - 6),
      style: { shadowBlur: 12, shadowOffsetY: 4, shadowColor: T.shadow }
    })
    item.getEdges().forEach(edge => graph.setItemState(edge, 'hover', false))
  })
  // 调试句柄：自动化测试/排查时可从 DOM 拿到 G6 实例（如 container.__graph.emit('node:click', …)）
  container.__graph = graph
  return graph
}
