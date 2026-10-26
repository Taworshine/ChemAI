<template>
  <div class="soft-card">
    <div class="page-header" style="margin-bottom:18px">
      <div>
        <h2 class="ph-title">文档管理</h2>
        <p class="ph-desc">
          查看已上传的教学文档与解析结果；新文档请前往
          <router-link to="/teacher/extract" class="link">知识入库</router-link>
          上传（经大模型抽取 + 教师确认后写入图谱）
        </p>
      </div>
      <el-input v-model="keyword" placeholder="搜索文档标题" clearable style="width:220px" :prefix-icon="Search" />
    </div>

    <el-table :data="filteredDocs" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" effect="light" :color="TYPE_COLORS[row.docType] || '#8a97b8'" style="color:#fff;border:none">
            {{ TYPE_NAMES[row.docType] || row.docType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="解析状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.parseStatus)" effect="light">{{ statusText(row.parseStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="上传时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewResult(row)" :disabled="!row.parseResult">查看结果</el-button>
          <el-popconfirm title="确定删除该文档？磁盘文件将一并清理" @confirm="remove(row)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty><el-empty description="还没有上传过文档" :image-size="80" /></template>
    </el-table>

    <el-dialog v-model="resultVisible" title="AI 抽取结果" width="640px">
      <pre class="result">{{ prettyResult }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const docs = ref([])
const loading = ref(false)
const keyword = ref('')
const resultVisible = ref(false)
const currentResult = ref('')

// 历史数据可能混有旧版分类（课件/试卷/错题本），新版入库走 教案/讲义/知识点
const TYPE_NAMES = {
  COURSEWARE: '课件', EXAM: '试卷', ERROR_BOOK: '错题本',
  A: '教案', B: '讲义', C: '知识点',
}
const TYPE_COLORS = {
  COURSEWARE: '#4a7dff', EXAM: '#ff9a4d', ERROR_BOOK: '#f5679a',
  A: '#4a7dff', B: '#22b899', C: '#b16df0',
}

const filteredDocs = computed(() =>
  docs.value.filter(d => !keyword.value || (d.title || '').includes(keyword.value)))

const prettyResult = computed(() => {
  try { return JSON.stringify(JSON.parse(currentResult.value), null, 2) } catch { return currentResult.value }
})

function statusType(s) { return [ 'info', 'warning', 'success', 'danger' ][s] ?? 'info' }
function statusText(s) { return [ '待解析', '解析中', '已完成', '失败' ][s] ?? '未知' }

async function load() {
  loading.value = true
  try {
    const res = await api.get('/document/list')
    docs.value = res.data
  } finally {
    loading.value = false
  }
}

function viewResult(row) {
  currentResult.value = row.parseResult
  resultVisible.value = true
}

async function remove(row) {
  try {
    await api.delete(`/document/${row.id}`)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.link { color: var(--primary); font-weight: 600; text-decoration: none; }
.link:hover { text-decoration: underline; }
.result {
  background: #f7f9fd; border: 1px solid var(--line); padding: 14px 16px;
  border-radius: 10px; max-height: 420px; overflow: auto; font-size: 12.5px; line-height: 1.6;
}
</style>
