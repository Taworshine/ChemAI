<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="(v, k) in data" :key="k" :span="6" style="margin-bottom: 16px">
        <el-card shadow="hover" class="stat">
          <div class="num">{{ v }}</div>
          <div class="label">{{ k }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-alert type="info" :closable="false" show-icon
      title="以上数字直接来自 MySQL 数据库 mind_empower 的实时行数统计，点击左侧菜单可逐表管理数据。" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../api'

const data = ref({})

onMounted(async () => {
  const res = await http.get('/dbadmin/overview')
  data.value = res.data
})
</script>

<style scoped>
.stat { text-align: center; }
.num { font-size: 28px; font-weight: 700; color: #2b5aed; }
.label { color: #4e5969; font-size: 13px; margin-top: 6px; }
</style>
