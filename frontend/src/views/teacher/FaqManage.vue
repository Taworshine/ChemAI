<template>
  <div class="fm-page">
    <div class="soft-card fm-hero">
      <div>
        <h3>高频问题解答工作台</h3>
        <p class="faint">学生追问自动聚合为高频问题，发布标准解答后学生会优先看到官方解答；相似问题可一键智能归并</p>
      </div>
      <div class="hero-actions">
        <el-button :loading="clustering" type="primary" plain @click="cluster">
          <el-icon><MagicStick /></el-icon>&nbsp;智能归并相似问题
        </el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="stat-row">
      <div class="soft-card stat">
        <span class="s-num warn">{{ pending }}</span>
        <span class="s-label">待解答</span>
      </div>
      <div class="soft-card stat">
        <span class="s-num ok">{{ published }}</span>
        <span class="s-label">已发布解答</span>
      </div>
      <div class="soft-card stat">
        <span class="s-num">{{ items.length }}</span>
        <span class="s-label">聚合问题总数</span>
      </div>
    </div>

    <div class="soft-card">
      <el-table :data="items" v-loading="loading" stripe>
        <el-table-column label="问题" min-width="280">
          <template #default="{ row }">
            <p class="q-text">{{ row.questionText }}</p>
          </template>
        </el-table-column>
        <el-table-column label="热度" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.askCount >= 3 ? 'danger' : (row.askCount >= 2 ? 'warning' : 'info')" effect="light">
              {{ row.askCount }} 次
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学生反馈" width="120" align="center">
          <template #default="{ row }">
            <span class="fb-stat up" v-if="row.useful">👍 {{ row.useful }}</span>
            <span class="fb-stat down" v-if="row.useless">👎 {{ row.useless }}</span>
            <span class="faint" v-if="!row.useful && !row.useless">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.publishStatus === 1 ? 'success' : 'info'" effect="plain">
              {{ row.publishStatus === 1 ? '已发布' : '待解答' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="解答" min-width="260">
          <template #default="{ row }">
            <p class="a-preview" v-if="row.answer">{{ row.answer }}</p>
            <span class="faint" v-else>尚未解答</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="openAnswer(row)">
              {{ row.publishStatus === 1 ? '编辑解答' : '写解答' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAnswer" :title="current ? '解答：' + current.questionText : ''" width="560px">
      <el-input v-model="answerText" type="textarea" :rows="6" placeholder="给出清晰、规范的标准解答，学生提问命中后会直接看到这段内容" />
      <template #footer>
        <el-button @click="showAnswer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAnswer">发布解答</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const items = ref([])
const loading = ref(false)
const clustering = ref(false)
const saving = ref(false)
const showAnswer = ref(false)
const current = ref(null)
const answerText = ref('')

const pending = computed(() => items.value.filter(i => i.publishStatus !== 1).length)
const published = computed(() => items.value.filter(i => i.publishStatus === 1).length)

async function load() {
  loading.value = true
  try {
    const res = await api.get('/agent/faq/manage')
    items.value = res.data.items
  } finally {
    loading.value = false
  }
}

async function cluster() {
  try {
    await ElMessageBox.confirm(
      '将调用大模型把语义相同的问题归并为一组（次数累加、反馈迁移，不可撤销）。确定执行？',
      '智能归并', { type: 'warning', confirmButtonText: '开始归并' })
  } catch { return }
  clustering.value = true
  try {
    const res = await api.post('/agent/faq/cluster')
    ElMessage.success(`归并完成：合并了 ${res.data.merged} 条重复问题`)
    await load()
  } catch (e) {
    ElMessage.warning(e?.response?.data?.msg || '归并未执行')
  } finally {
    clustering.value = false
  }
}

function openAnswer(row) {
  current.value = row
  answerText.value = row.answer || ''
  showAnswer.value = true
}

async function saveAnswer() {
  if (!answerText.value.trim()) { ElMessage.warning('解答内容不能为空'); return }
  saving.value = true
  try {
    await api.post('/agent/faq/answer', { id: current.value.id, answer: answerText.value.trim() })
    ElMessage.success('解答已发布，学生端立即可见')
    showAnswer.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.fm-page { display: flex; flex-direction: column; gap: 16px; }
.fm-hero {
  display: flex; justify-content: space-between; align-items: center; gap: 20px; padding: 20px 24px; flex-wrap: wrap;
  background: linear-gradient(120deg, #eef4ff 0%, #fdfcff 55%, #eafaf4 100%);
}
.fm-hero h3 { margin: 0 0 6px; font-size: 19px; color: var(--ink); }
.fm-hero p { margin: 0; font-size: 13px; }
.hero-actions { display: flex; gap: 10px; }

.stat-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.stat { padding: 16px 20px; display: flex; flex-direction: column; gap: 4px; }
.s-num { font-size: 26px; font-weight: 800; color: var(--ink); }
.s-num.warn { color: #e6a23c; }
.s-num.ok { color: #1f9e85; }
.s-label { font-size: 12.5px; color: var(--ink-3); }

.q-text { margin: 0; font-size: 13.5px; font-weight: 600; color: var(--ink); line-height: 1.6; }
.a-preview {
  margin: 0; font-size: 12.5px; color: var(--ink-3); line-height: 1.6;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.fb-stat { font-size: 12px; margin-right: 8px; }
.fb-stat.up { color: #1f9e85; }
.fb-stat.down { color: #c0c4cc; }
</style>
