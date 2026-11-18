<template>
  <el-table :data="rows" border stripe v-loading="loading">
    <el-table-column v-for="c in columns" :key="c.prop || c.label" v-bind="c" :show-overflow-tooltip="!!c.overflow">
      <template v-if="c.render" #default="{ row }">
        <component :is="c.render(row)" />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="100" fixed="right">
      <template #default="{ row }">
        <el-popconfirm title="确定删除该记录？" @confirm="onDelete(row)">
          <template #reference><el-button size="small" type="danger">删除</el-button></template>
        </el-popconfirm>
      </template>
    </el-table-column>
    <template #empty><el-empty description="暂无数据" /></template>
  </el-table>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  loader: { type: Function, required: true },
  deleter: { type: Function, required: true },
  columns: { type: Array, required: true }
})

const rows = ref([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    rows.value = await props.loader()
  } finally {
    loading.value = false
  }
}

async function onDelete(row) {
  try {
    await props.deleter(row)
    load()
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(load)
defineExpose({ load })
</script>
