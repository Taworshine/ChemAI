<template>
  <div>
    <div class="bar">
      <el-input v-model="keyword" placeholder="按会议主题 / 会议号搜索" clearable style="width: 260px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="meetingNo" label="会议号" width="90" />
      <el-table-column prop="title" label="会议主题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="subject" label="学科" width="80" />
      <el-table-column prop="agenda" label="议程" width="130" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['info', 'success', 'warning', 'danger'][row.status] || 'info'">
            {{ ['待开始', '进行中', '已结束', '已取消'][row.status] ?? row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="锁定" width="70">
        <template #default="{ row }">{{ row.locked ? '🔒' : '—' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openMembers(row)">成员</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="删除会议将同时删除其成员/批注/聊天/投票/纪要/任务，确定？" @confirm="del(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" title="编辑会议" width="440px">
      <el-form label-width="80px">
        <el-form-item label="主题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="学科"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="议程">
          <el-select v-model="form.agenda">
            <el-option label="教案评审" value="LESSON_REVIEW" />
            <el-option label="集体备课" value="COLLECTIVE_PREP" />
            <el-option label="命题研讨" value="PROPOSITION" />
            <el-option label="试卷分析" value="PAPER_ANALYSIS" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option v-for="(t, i) in ['待开始', '进行中', '已结束', '已取消']" :key="i" :label="t" :value="i" />
          </el-select>
        </el-form-item>
        <el-form-item label="锁定">
          <el-switch v-model="form.locked" :active-value="1" :inactive-value="0" active-text="禁止加入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="memberDlg" :title="`会议成员（会议ID ${memberMeetingId}）`" size="420px">
      <el-table :data="members" size="small">
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column label="在线" width="70">
          <template #default="{ row }">{{ row.online ? '🟢' : '⚪' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-popconfirm title="移出会议？" @confirm="delMember(row)">
              <template #reference><el-button size="small" type="danger">移出</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const rows = ref([])
const keyword = ref('')
const dlg = ref(false)
const form = reactive({})
const memberDlg = ref(false)
const memberMeetingId = ref(null)
const members = ref([])

async function load() {
  const res = await http.get('/dbadmin/meetings', { params: { keyword: keyword.value || undefined } })
  rows.value = res.data
}

function openEdit(row) {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row)
  dlg.value = true
}

async function save() {
  await http.put(`/dbadmin/meetings/${form.id}`, form)
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

async function del(row) {
  await http.delete(`/dbadmin/meetings/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function openMembers(row) {
  memberMeetingId.value = row.id
  const res = await http.get(`/dbadmin/meetings/${row.id}/members`)
  members.value = res.data
  memberDlg.value = true
}

async function delMember(row) {
  await http.delete(`/dbadmin/members/${row.id}`)
  ElMessage.success('已移出')
  openMembers({ id: memberMeetingId.value })
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
