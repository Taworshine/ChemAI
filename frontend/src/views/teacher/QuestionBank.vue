<template>
  <div class="soft-card">
    <div class="page-header" style="margin-bottom:18px">
      <div>
        <h2 class="ph-title">题库管理</h2>
        <p class="ph-desc">录入各学科练习题，关联知识图谱概念后自动进入学生「在线练习」抽题池</p>
      </div>
      <div class="ph-extra">
        <el-select v-model="subject" size="small" clearable placeholder="全部学科" style="width:120px" @change="load">
          <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
        </el-select>
        <el-input v-model="keyword" size="small" placeholder="搜索题目 / 知识点…" clearable style="width:210px"
                  :prefix-icon="Search" @input="onKeyword" @clear="load" />
        <el-button size="small" type="primary" @click="openCreate">+ 录入题目</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="subject" label="学科" width="76">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.subject }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="70">
        <template #default="{ row }">
          <el-tag size="small" :type="{ '易': 'success', '中': 'warning', '难': 'danger' }[row.difficulty]" effect="light">
            {{ row.difficulty }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="conceptName" label="知识点" width="120" show-overflow-tooltip />
      <el-table-column prop="questionType" label="题型" width="130" show-overflow-tooltip />
      <el-table-column prop="content" label="题目" min-width="220" show-overflow-tooltip />
      <el-table-column prop="answer" label="参考答案" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button size="small" link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除该题？" @confirm="remove(row)">
            <template #reference><el-button size="small" link type="danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dlgVisible" :title="editing ? '编辑题目' : '录入题目'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="学科">
          <el-select v-model="form.subject" style="width:100%">
            <el-option v-for="s in SUBJECTS" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-radio-group v-model="form.difficulty">
            <el-radio-button value="易">易</el-radio-button>
            <el-radio-button value="中">中</el-radio-button>
            <el-radio-button value="难">难</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="form.conceptName" placeholder="关联知识图谱中的概念名" />
        </el-form-item>
        <el-form-item label="题型">
          <el-input v-model="form.questionType" placeholder="如：氧化还原方程式配平" />
        </el-form-item>
        <el-form-item label="题目">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="参考答案">
          <el-input v-model="form.answer" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlgVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../../api'

const SUBJECTS = ['语文', '数学', '英语', '物理', '化学', '生物', '政治', '历史', '地理']
const rows = ref([])
const loading = ref(false)
const subject = ref('')
const keyword = ref('')
const dlgVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    const res = await api.get('/question/list', {
      params: { subject: subject.value || undefined, keyword: keyword.value || undefined }
    })
    rows.value = res.data
  } finally {
    loading.value = false
  }
}

/** 关键词输入防抖 350ms，边输边搜 */
let kwTimer = null
function onKeyword() { clearTimeout(kwTimer); kwTimer = setTimeout(load, 350) }

function openCreate() {
  editing.value = null
  form.value = { subject: subject.value || '化学', difficulty: '中' }
  dlgVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = { ...row }
  dlgVisible.value = true
}

async function save() {
  saving.value = true
  try {
    if (editing.value) await api.put(`/question/${editing.value.id}`, form.value)
    else await api.post('/question', form.value)
    ElMessage.success('已保存')
    dlgVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  await api.delete(`/question/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

