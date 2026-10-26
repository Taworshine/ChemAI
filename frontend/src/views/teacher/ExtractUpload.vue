<template>
  <div class="extract-page">
    <div class="hero soft-card">
      <div>
        <h3>知识入库</h3>
        <p class="faint">上传教案 / 讲义 / 知识点，大模型自动抽取候选 → 教师确认后写入知识图谱</p>
      </div>
      <el-button type="primary" plain @click="$router.push('/teacher/confirm')">
        前往确认中心 <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>

    <div class="soft-card upload-card">
      <el-tabs v-model="docType" class="type-tabs">
        <el-tab-pane label="教案" name="A" />
        <el-tab-pane label="讲义" name="B" />
        <el-tab-pane label="纯知识点" name="C" />
      </el-tabs>

      <div class="form-grid">
        <div class="field">
          <label>标题</label>
          <el-input v-model="title" size="large" :placeholder="titlePlaceholder" />
        </div>
        <div class="field hint-field">
          <label>命名规范</label>
          <p class="faint spec">建议：学科_年级_课题_作者_日期，例：化学_高一_氧化还原反应_王老师_20261026</p>
        </div>
      </div>

      <!-- A/B：文件上传 -->
      <div v-if="docType !== 'C'" class="drop-zone" :class="{ over: dragging }"
           @dragover.prevent="dragging = true" @dragleave="dragging = false" @drop.prevent="onDrop">
        <input ref="fileInput" type="file" accept=".docx,.pdf,.md,.txt" hidden @change="onPick" />
        <template v-if="!file">
          <el-icon class="up-icon"><UploadFilled /></el-icon>
          <p>点击或拖拽文件到此处</p>
          <p class="faint small">支持 .docx / .pdf / .md / .txt，≤ 20MB</p>
        </template>
        <template v-else>
          <el-icon class="file-icon"><Document /></el-icon>
          <p class="file-name">{{ file.name }}</p>
          <el-button size="small" text type="danger" @click.stop="file = null">移除</el-button>
        </template>
      </div>

      <!-- C：文本粘贴 -->
      <div v-else class="text-zone">
        <div class="text-head">
          <p class="faint small">逐条输入知识点，每行一条，格式：名称｜学科｜年级｜描述（后三项可省略）</p>
          <el-button size="small" text type="primary" @click="downloadTemplate">下载 Excel 模板</el-button>
        </div>
        <el-input v-model="text" type="textarea" :rows="8"
                  placeholder="氧化还原反应｜化学｜高一｜有化合价升降的化学反应&#10;离子反应｜化学｜高一｜在水溶液中发生的离子参与的反应" />
        <div class="text-or faint">— 或者上传填写好的模板文件 —</div>
        <div class="drop-zone slim" @click="fileInput.click()">
          <input ref="fileInput" type="file" accept=".txt,.md,.csv" hidden @change="onPick" />
          <el-icon class="up-icon small"><UploadFilled /></el-icon>
          <p class="small">{{ file ? file.name : '选择模板 / 文本文件' }}</p>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" size="large" :loading="submitting" :disabled="!canSubmit" @click="submit">
          {{ submitting ? '上传中…' : '开始抽取' }}
        </el-button>
        <span v-if="submitting" class="faint small">大模型抽取约需 10~40 秒，完成后请到「确认中心」裁决</span>
      </div>
    </div>

    <!-- 最近任务 -->
    <div class="soft-card tasks-card">
      <div class="card-head">
        <h4>最近上传</h4>
        <el-button size="small" text @click="loadTasks"><el-icon><Refresh /></el-icon></el-button>
      </div>
      <el-table :data="tasks" size="large" empty-text="还没有上传记录">
        <el-table-column prop="task.title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.task.docType === 'C' ? 'warning' : 'info'">
              {{ { A: '教案', B: '讲义', C: '知识点' }[row.task.docType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <span class="status" :class="statusClass(row.task.status)">{{ statusText(row.task.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="待裁决" width="80">
          <template #default="{ row }">
            <el-badge v-if="row.pending > 0" :value="row.pending" type="warning" />
            <span v-else class="faint">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="task.createdAt" label="时间" width="170" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.task.status === 'PENDING_CONFIRM'" size="small" type="primary" plain
                       @click="$router.push('/teacher/confirm?taskId=' + row.task.id)">去确认</el-button>
            <el-button v-else-if="row.task.status === 'CONFIRMED'" size="small" type="success" plain
                       @click="publish(row.task.id)">发布</el-button>
            <el-button v-else size="small" text @click="$router.push('/teacher/confirm?taskId=' + row.task.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowRight, UploadFilled, Document, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const docType = ref('A')
const title = ref('')
const text = ref('')
const file = ref(null)
const fileInput = ref(null)
const dragging = ref(false)
const submitting = ref(false)
const tasks = ref([])

const titlePlaceholder = computed(() => ({
  A: '例：高一化学《氧化还原反应》教案',
  B: '例：高一化学氧化还原反应讲义',
  C: '例：高一化学离子反应知识点批量录入'
}[docType.value]))

const canSubmit = computed(() =>
  docType.value === 'C' ? !!(text.value.trim() || file.value) : !!file.value)

function onPick(e) {
  const f = e.target.files[0]
  if (f) file.value = f
  e.target.value = ''
}
function onDrop(e) {
  dragging.value = false
  const f = e.dataTransfer.files[0]
  if (f) file.value = f
}

async function submit() {
  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('docType', docType.value)
    if (title.value.trim()) fd.append('title', title.value.trim())
    if (text.value.trim()) fd.append('text', text.value.trim())
    if (file.value) fd.append('file', file.value)
    await api.post('/extract/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('已提交，抽取完成后请到确认中心裁决')
    title.value = ''
    text.value = ''
    file.value = null
    loadTasks()
  } catch { /* 拦截器已提示 */ } finally {
    submitting.value = false
  }
}

async function publish(id) {
  await api.post(`/extract/task/${id}/publish`)
  ElMessage.success('已发布，学生端现在可以看到这些内容了')
  loadTasks()
}

async function loadTasks() {
  const res = await api.get('/extract/tasks')
  tasks.value = res.data.slice(0, 8)
}

const STATUS = {
  PARSING: '解析中', EXTRACTING: '抽取中', MATCHING: '匹配中',
  PENDING_CONFIRM: '待确认', CONFIRMED: '已入库', PUBLISHED: '已发布',
  OFFLINE: '已下线', FAILED: '失败', CANCELLED: '已取消'
}
const statusText = s => STATUS[s] || s
const statusClass = s => ({
  PENDING_CONFIRM: 'warn', CONFIRMED: 'ok', PUBLISHED: 'ok', FAILED: 'bad'
}[s] || 'dim')

function downloadTemplate() {
  const csv = '知识点名称,学科,年级,描述,关键词,前置概念\n氧化还原反应,化学,高一,有化合价升降的化学反应,化合价;电子转移,化合价\n'
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '知识点导入模板.csv'
  a.click()
  URL.revokeObjectURL(a.href)
}

onMounted(() => {
  loadTasks()
  // 从教研看板缺口清单跳入：预填知识点文本
  const prefill = new URLSearchParams(location.search).get('prefill')
  if (prefill) {
    docType.value = 'C'
    text.value = prefill + '｜｜｜学生高频提问但图谱缺少的知识点，请补充定义与讲解'
  }
})
</script>

<style scoped>
.extract-page { display: flex; flex-direction: column; gap: 16px; }
.hero {
  display: flex; justify-content: space-between; align-items: center;
  padding: 20px 24px;
  background: linear-gradient(120deg, #f4f0ff 0%, #fdfcff 55%, #eefaf7 100%);
}
.hero h3 { margin: 0 0 6px; font-size: 19px; color: var(--ink); }
.hero p { margin: 0; font-size: 13px; }
.upload-card { padding: 20px 24px; }
.type-tabs { margin-bottom: 8px; }
.type-tabs :deep(.el-tabs__item) { font-size: 15px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.field label { display: block; font-size: 13px; color: var(--ink-2); margin-bottom: 6px; font-weight: 600; }
.field .spec { font-size: 12px; margin: 4px 0 0; line-height: 1.6; }
@media (max-width: 900px) { .form-grid { grid-template-columns: 1fr; } }

.drop-zone {
  border: 2px dashed #c9d4ec; border-radius: 14px; padding: 34px 20px; text-align: center;
  cursor: pointer; transition: all .2s; background: #fafcff;
}
.drop-zone:hover, .drop-zone.over { border-color: var(--primary); background: #f2f7ff; }
.drop-zone.slim { padding: 14px; }
.up-icon { font-size: 38px; color: var(--primary); opacity: .7; }
.up-icon.small { font-size: 20px; }
.file-icon { font-size: 34px; color: var(--primary); }
.drop-zone p { margin: 8px 0 0; color: var(--ink-2); font-size: 14px; }
.drop-zone .file-name { font-weight: 600; color: var(--ink); }

.text-zone .text-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.text-zone .text-head p { margin: 0; }
.text-or { text-align: center; font-size: 12px; margin: 14px 0; }
.actions { display: flex; align-items: center; gap: 14px; margin-top: 18px; }

.tasks-card { padding: 16px 20px; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.card-head h4 { margin: 0; color: var(--ink); }
.status { font-size: 12.5px; font-weight: 600; }
.status.warn { color: #d48806; } .status.ok { color: #2f9e6e; }
.status.bad { color: #e05260; } .status.dim { color: var(--ink-3); }
.small { font-size: 12px; }
</style>
