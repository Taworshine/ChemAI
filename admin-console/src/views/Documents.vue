<template>
  <div>
    <div class="bar">
      <el-input v-model="keyword" placeholder="按标题搜索" clearable style="width: 260px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="docType" label="类型" width="110" />
      <el-table-column prop="subject" label="学科" width="80" />
      <el-table-column label="解析状态" width="100">
        <template #default="{ row }">
          <el-tag :type="['info', 'primary', 'success', 'danger'][row.parseStatus]">
            {{ ['待解析', '解析中', '成功', '失败'][row.parseStatus] ?? row.parseStatus }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uploadUserId" label="上传人ID" width="90" />
      <el-table-column prop="createTime" label="上传时间" width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-popconfirm title="确定删除该文档记录？" @confirm="del(row)">
            <template #reference><el-button size="small" type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api'

const rows = ref([])
const keyword = ref('')

async function load() {
  const res = await http.get('/dbadmin/documents', { params: { keyword: keyword.value || undefined } })
  rows.value = res.data
}

async function del(row) {
  await http.delete(`/dbadmin/documents/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
