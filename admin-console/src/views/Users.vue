<template>
  <div>
    <div class="bar">
      <el-input v-model="keyword" placeholder="按用户名 / 姓名搜索" clearable style="width: 260px" @keyup.enter="load" />
      <el-select v-model="roleFilter" placeholder="角色" clearable style="width: 140px">
        <el-option label="教师" value="TEACHER" />
        <el-option label="学生" value="STUDENT" />
        <el-option label="管理员" value="ADMIN" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openEdit(null)">新增用户</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'TEACHER' ? 'primary' : 'success'">
            {{ { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }[row.role] || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="className" label="班级" width="110" />
      <el-table-column prop="subject" label="学科" width="90" />
      <el-table-column prop="createTime" label="创建时间" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该用户？" @confirm="del(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="440px">
      <el-form label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item :label="form.id ? '重置密码' : '密码'">
          <el-input v-model="form.password" type="password" show-password :placeholder="form.id ? '留空则不修改' : '至少 6 位'" />
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role">
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
            <el-option v-if="form.id" label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="学科"><el-input v-model="form.subject" placeholder="教师填，如：化学" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const rows = ref([])
const keyword = ref('')
const roleFilter = ref('')
const dlg = ref(false)
const saving = ref(false)
const form = reactive({})

async function load() {
  const res = await http.get('/dbadmin/users', { params: { keyword: keyword.value || undefined, role: roleFilter.value || undefined } })
  rows.value = res.data
}

function openEdit(row) {
  Object.keys(form).forEach(k => delete form[k])
  if (row) Object.assign(form, { ...row, password: '' })
  else Object.assign(form, { role: 'STUDENT' })
  dlg.value = true
}

async function save() {
  saving.value = true
  try {
    if (form.id) await http.put(`/dbadmin/users/${form.id}`, form)
    else await http.post('/dbadmin/users', form)
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function del(row) {
  await http.delete(`/dbadmin/users/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
