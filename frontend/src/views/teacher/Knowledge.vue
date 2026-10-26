<template>
  <div class="soft-card">
    <div class="page-header" style="margin-bottom:18px">
      <div>
        <h2 class="ph-title">知识管理</h2>
        <p class="ph-desc">维护图谱中的概念、题型、思维路径与常见卡点，保存后实时写入 Neo4j</p>
      </div>
      <div class="ph-extra">
        <el-select v-model="subject" size="small" clearable placeholder="全部学科" style="width:120px" @change="onFilter">
          <el-option v-for="s in subjects" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="keyword" size="small" placeholder="搜索名称 / 描述…" clearable style="width:200px"
                  :prefix-icon="Search" @input="onKeyword" @clear="onFilter" />
        <el-button size="small" type="primary" @click="openCreate">+ 新建</el-button>
      </div>
    </div>

    <el-tabs v-model="label" @tab-change="onFilter">
      <el-tab-pane label="概念" name="Concept" />
      <el-tab-pane label="题型" name="QuestionType" />
      <el-tab-pane label="思维路径" name="ThinkingPath" />
      <el-tab-pane label="常见卡点" name="Misconception" />
    </el-tabs>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="类型" width="100">
        <template #default>
          <el-tag size="small" effect="light" :color="TYPE_COLORS[label]" style="color:#fff;border:none">
            {{ TYPE_NAMES[label] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-for="col in columns" :key="col.key" :prop="'props.' + col.key" :label="col.label"
                       :min-width="col.width" show-overflow-tooltip>
        <template #default="{ row }">{{ row.props[col.key] }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该节点？" @confirm="remove(row)">
            <template #reference><el-button size="small" link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top:14px;justify-content:flex-end" background
                   layout="total, prev, pager, next" :total="total"
                   :page-size="size" :current-page="page" @current-change="p => { page = p; load() }" />

    <!-- 新建 / 编辑对话框 -->
    <el-dialog v-model="dlgVisible" :title="(editing ? '编辑' : '新建') + TYPE_NAMES[label]" width="480px">
      <el-form label-width="90px">
        <el-form-item v-for="f in formFields" :key="f.key" :label="f.label">
          <el-input-number v-if="f.type === 'number'" v-model="form[f.key]" style="width:100%" />
          <el-select v-else-if="f.key === 'subject'" v-model="form[f.key]" style="width:100%">
            <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
          </el-select>
          <el-select v-else-if="f.key === 'difficulty'" v-model="form[f.key]" style="width:100%">
            <el-option label="易" value="易" /><el-option label="中" value="中" /><el-option label="难" value="难" />
          </el-select>
          <el-input v-else v-model="form[f.key]" type="textarea" :rows="1" autosize />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存到 Neo4j</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const TYPE_NAMES = { Concept: '概念', QuestionType: '题型', ThinkingPath: '思维路径', Misconception: '卡点' }
const TYPE_COLORS = { Concept: '#4a7dff', QuestionType: '#ff9a4d', ThinkingPath: '#38b8a0', Misconception: '#f5679a' }
const SUBJECTS = ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理']

const label = ref('Concept')
const subject = ref('')
const keyword = ref('')
const subjects = ref([])
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = 10
const loading = ref(false)

const FORMS = {
  Concept: [{ key: 'name', label: '名称' }, { key: 'subject', label: '学科' }, { key: 'grade', label: '年级' },
            { key: 'keywords', label: '关键词' }, { key: 'description', label: '描述' }],
  QuestionType: [{ key: 'name', label: '名称' }, { key: 'subject', label: '学科' }, { key: 'difficulty', label: '难度' },
                 { key: 'typicalExample', label: '典型例题' }],
  ThinkingPath: [{ key: 'description', label: '步骤描述' }, { key: 'stepOrder', label: '步骤序号', type: 'number' },
                 { key: 'hint', label: '启发提示' }],
  Misconception: [{ key: 'description', label: '卡点描述' }, { key: 'frequency', label: '出现频次', type: 'number' }]
}
const formFields = ref([])
const form = ref({})
const dlgVisible = ref(false)
const editing = ref(null)
const saving = ref(false)

const columns = ref([])

function refreshColumns() {
  columns.value = FORMS[label.value]
}

async function load() {
  loading.value = true
  try {
    const res = await api.get('/graph/nodes', {
      params: { label: label.value, subject: subject.value || undefined, keyword: keyword.value || undefined,
                page: page.value, size }
    })
    rows.value = res.data.list
    total.value = res.data.total
    refreshColumns()
  } finally {
    loading.value = false
  }
}

/** 筛选变化时回到第一页 */
function onFilter() { page.value = 1; load() }
/** 关键词输入防抖 350ms，边输边搜，不用按回车 */
let kwTimer = null
function onKeyword() { clearTimeout(kwTimer); kwTimer = setTimeout(onFilter, 350) }

function openCreate() {
  editing.value = null
  form.value = {}
  formFields.value = FORMS[label.value]
  dlgVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = { ...row.props }
  formFields.value = FORMS[label.value]
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await api.put(`/graph/node/${editing.value.id}`, form.value)
    } else {
      await api.post('/graph/node', { label: label.value, props: form.value })
    }
    ElMessage.success('已保存到 Neo4j')
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await api.delete(`/graph/node/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  const res = await api.get('/graph/subjects')
  subjects.value = res.data
  load()
})
</script>

