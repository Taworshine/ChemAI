<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="高频问题" name="faqs">
        <div class="bar">
          <el-input v-model="faqKw" placeholder="按问题搜索" clearable style="width: 240px" @keyup.enter="loadFaqs" />
          <el-button type="primary" @click="loadFaqs">查询</el-button>
        </div>
        <el-table :data="faqs" border stripe>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="questionText" label="问题" min-width="240" show-overflow-tooltip />
          <el-table-column prop="conceptName" label="知识点" width="130" show-overflow-tooltip />
          <el-table-column prop="askCount" label="被问次数" width="90" />
          <el-table-column label="发布状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.publishStatus ? 'success' : 'info'">{{ row.publishStatus ? '已发布' : '未发布' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="editFaq(row)">{{ row.publishStatus ? '下线' : '发布' }}</el-button>
              <el-popconfirm title="确定删除？" @confirm="delFaq(row)">
                <template #reference><el-button size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="AI 对话会话" name="sessions">
        <data-table :loader="loadSessions" :deleter="delSession" :columns="sessionCols" />
      </el-tab-pane>

      <el-tab-pane label="练习记录" name="practices">
        <data-table :loader="loadPractices" :deleter="delPractice" :columns="practiceCols" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { h, ref } from 'vue'
import { ElMessage, ElTag } from 'element-plus'
import http from '../api'
import DataTable from './DataTable.vue'

const tab = ref('faqs')
const faqs = ref([])
const faqKw = ref('')

async function loadFaqs() {
  const res = await http.get('/dbadmin/faqs', { params: { keyword: faqKw.value || undefined } })
  faqs.value = res.data
}
async function editFaq(row) {
  await http.put(`/dbadmin/faqs/${row.id}`, { publishStatus: row.publishStatus ? 0 : 1 })
  ElMessage.success('已更新')
  loadFaqs()
}
async function delFaq(row) {
  await http.delete(`/dbadmin/faqs/${row.id}`)
  ElMessage.success('已删除')
  loadFaqs()
}

const sessionCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'studentId', label: '学生ID', width: 80 },
  { prop: 'questionTypeId', label: '题型ID', width: 80 },
  { label: '状态', width: 90, render: r => h(ElTag, { type: ['success', 'primary', 'info'][r.status] || 'info' }, () => ['进行中', '已解决', '关闭'][r.status] ?? r.status) },
  { prop: 'summary', label: '总结', minWidth: 300, overflow: true }
]
async function loadSessions() { return (await http.get('/dbadmin/dialog-sessions')).data }
async function delSession(row) { await http.delete(`/dbadmin/dialog-sessions/${row.id}`); ElMessage.success('已删除') }

const practiceCols = [
  { prop: 'id', label: 'ID', width: 60 },
  { prop: 'studentId', label: '学生ID', width: 80 },
  { prop: 'subject', label: '学科', width: 80 },
  { prop: 'score', label: '答对', width: 70 },
  { prop: 'total', label: '总题数', width: 80 },
  { prop: 'createTime', label: '时间', width: 160 }
]
async function loadPractices() { return (await http.get('/dbadmin/practice-records')).data }
async function delPractice(row) { await http.delete(`/dbadmin/practice-records/${row.id}`); ElMessage.success('已删除') }

loadFaqs()
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
