<template>
  <div class="admin-users">
    <div class="page-head">
      <div>
        <h2>教师学科分配</h2>
        <p class="faint">为每位教师指定所属学科，教师登录后仅能看到和录入本学科的教案、题库与错题。</p>
      </div>
      <el-button :icon="Refresh" circle @click="load" title="刷新" />
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="teachers" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="130" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="className" label="任教班级" min-width="120">
          <template #default="{ row }">{{ row.className || '—' }}</template>
        </el-table-column>
        <el-table-column label="当前学科" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.subject" type="success" effect="light">{{ row.subject }}</el-tag>
            <el-tag v-else type="info" effect="plain">未分配</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分配学科" min-width="220">
          <template #default="{ row }">
            <div class="assign-row">
              <el-select v-model="drafts[row.id]" placeholder="选择学科" clearable style="width: 150px">
                <el-option v-for="s in subjects" :key="s" :label="s" :value="s" />
              </el-select>
              <el-button type="primary" plain size="small"
                         :disabled="drafts[row.id] === (row.subject || '')"
                         @click="save(row)">保存</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const teachers = ref([])
const subjects = ref([])
const drafts = ref({})
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const [tRes, sRes] = await Promise.all([
      api.get('/admin/teachers'),
      api.get('/admin/subjects')
    ])
    teachers.value = tRes.data || []
    subjects.value = sRes.data || []
    const d = {}
    for (const t of teachers.value) d[t.id] = t.subject || ''
    drafts.value = d
  } finally {
    loading.value = false
  }
}

async function save(row) {
  try {
    await api.put(`/admin/teachers/${row.id}/subject`, { subject: drafts.value[row.id] || '' })
    ElMessage.success(`已将 ${row.realName} 的学科设为「${drafts.value[row.id] || '未分配'}」`)
    load()
  } catch { /* 拦截器已提示 */ }
}

onMounted(load)
</script>

<style scoped>
.admin-users { padding: 4px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-head h2 { margin: 0 0 6px; font-size: 20px; }
.faint { color: #86909c; font-size: 13px; margin: 0; }
.table-card { border-radius: 10px; }
.assign-row { display: flex; gap: 8px; align-items: center; }
</style>
