<template>
  <div>
    <div class="bar">
      <el-input v-model="keyword" placeholder="按题干 / 知识点搜索" clearable style="width: 260px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openEdit(null)">录入错题</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="subject" label="学科" width="80" />
      <el-table-column prop="conceptName" label="知识点" width="140" show-overflow-tooltip />
      <el-table-column prop="wrongReason" label="错因" width="110" />
      <el-table-column prop="content" label="题干" min-width="280" show-overflow-tooltip />
      <el-table-column prop="wrongCount" label="错的人数" width="90" />
      <el-table-column prop="createBy" label="录入人ID" width="90" />
      <el-table-column prop="createTime" label="录入时间" width="160" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除？" @confirm="del(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlg" :title="form.id ? '编辑错题' : '录入错题'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="学科"><el-input v-model="form.subject" placeholder="如：化学" /></el-form-item>
        <el-form-item label="知识点"><el-input v-model="form.conceptName" placeholder="对齐 Neo4j 图谱概念名" /></el-form-item>
        <el-form-item label="错因标签">
          <el-select v-model="form.wrongReason" allow-create filterable default-first-option>
            <el-option v-for="r in REASONS" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="错的人数"><el-input-number v-model="form.wrongCount" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const REASONS = ['条件混淆', '概念不清', '计算错误', '审题偏差', '表达不规范']
const rows = ref([])
const keyword = ref('')
const dlg = ref(false)
const form = reactive({})

async function load() {
  const res = await http.get('/dbadmin/wrong-questions', { params: { keyword: keyword.value || undefined } })
  rows.value = res.data
}

function openEdit(row) {
  Object.keys(form).forEach(k => delete form[k])
  if (row) Object.assign(form, { ...row })
  else Object.assign(form, { wrongCount: 1 })
  dlg.value = true
}

async function save() {
  if (form.id) await http.put(`/dbadmin/wrong-questions/${form.id}`, form)
  else await http.post('/dbadmin/wrong-questions', form)
  ElMessage.success('已保存')
  dlg.value = false
  load()
}

async function del(row) {
  await http.delete(`/dbadmin/wrong-questions/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
