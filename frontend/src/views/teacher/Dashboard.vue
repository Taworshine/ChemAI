<template>
  <div class="dash">
    <!-- 统计卡片 -->
    <div class="stats">
      <div class="soft-card hoverable stat">
        <div class="icon stu"><el-icon :size="22"><User /></el-icon></div>
        <div><strong>{{ stats.students }}</strong><span>在校学生</span></div>
      </div>
      <div class="soft-card hoverable stat">
        <div class="icon graph"><el-icon :size="22"><Share /></el-icon></div>
        <div><strong>{{ stats.graphNodes }}</strong><span>图谱知识点</span></div>
      </div>
      <div class="soft-card hoverable stat">
        <div class="icon doc"><el-icon :size="22"><FolderOpened /></el-icon></div>
        <div><strong>{{ stats.documents }}</strong><span>教学文档</span></div>
      </div>
      <div class="soft-card hoverable stat">
        <div class="icon quiz"><el-icon :size="22"><EditPen /></el-icon></div>
        <div><strong>{{ stats.questions }}</strong><span>题库题目</span></div>
      </div>
      <div class="soft-card hoverable stat">
        <div class="icon practice"><el-icon :size="22"><DataLine /></el-icon></div>
        <div><strong>{{ stats.practices }}</strong><span>练习次数</span></div>
      </div>
    </div>

    <div class="grid">
      <!-- 学科均分柱状图 -->
      <div class="soft-card">
        <h3 class="section-title">各学科练习平均分</h3>
        <div ref="barRef" class="bar"></div>
        <el-empty v-if="!subjectAvg.length" description="暂无练习数据" :image-size="80" />
      </div>
      <!-- 快捷入口 -->
      <div class="soft-card">
        <h3 class="section-title">快捷入口</h3>
        <div class="quick">
          <div class="q-item" @click="$router.push('/teacher/graph')">
            <el-icon :size="20"><Share /></el-icon><span>编辑知识图谱</span>
          </div>
          <div class="q-item" @click="$router.push('/teacher/knowledge')">
            <el-icon :size="20"><Collection /></el-icon><span>维护知识点</span>
          </div>
          <div class="q-item" @click="$router.push('/teacher/question')">
            <el-icon :size="20"><Notebook /></el-icon><span>录入题库</span>
          </div>
          <div class="q-item" @click="$router.push('/teacher/document')">
            <el-icon :size="20"><UploadFilled /></el-icon><span>上传教案</span>
          </div>
        </div>
        <h3 class="section-title" style="margin-top:22px">最近练习动态</h3>
        <el-timeline style="padding-left:4px">
          <el-timeline-item v-for="r in recent" :key="r.id" :timestamp="r.createTime" placement="top"
                            :color="r.score * 100 / r.total >= 80 ? '#4a7dff' : '#ff9a4d'">
            学生 #{{ r.studentId }} 完成 {{ r.subject }}练习，得分 {{ r.score }}/{{ r.total }}
          </el-timeline-item>
          <el-empty v-if="!recent.length" description="暂无记录" :image-size="60" />
        </el-timeline>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import { User, Share, FolderOpened, EditPen, DataLine, Collection, Notebook, UploadFilled } from '@element-plus/icons-vue'
import api from '../../api'

const stats = ref({ students: 0, graphNodes: 0, documents: 0, questions: 0, practices: 0 })
const subjectAvg = ref([])
const recent = ref([])
const barRef = ref(null)
let bar = null

onMounted(async () => {
  const [students, graph, docs, questions, practice] = await Promise.all([
    api.get('/analysis/students'),
    api.get('/graph/full'),
    api.get('/document/list'),
    api.get('/question/list'),
    api.get('/practice/stats')
  ])
  stats.value = {
    students: students.data.length,
    graphNodes: graph.data.nodes.length,
    documents: docs.data.length,
    questions: questions.data.length,
    practices: practice.data.recent.length
  }
  subjectAvg.value = practice.data.bySubject
  recent.value = practice.data.recent

  if (subjectAvg.value.length) {
    bar = echarts.init(barRef.value)
    bar.setOption({
      grid: { left: 40, right: 16, top: 26, bottom: 28 },
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: subjectAvg.value.map(s => s.subject), axisLine: { lineStyle: { color: '#d8dfee' } } },
      yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#eef1f8' } } },
      series: [{
        type: 'bar', data: subjectAvg.value.map(s => s.avg), barWidth: 30,
        itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#6a94ff' }, { offset: 1, color: '#a9c3ff' }]) },
        label: { show: true, position: 'top', color: '#6b7386', fontSize: 12 }
      }]
    })
  }
  window.addEventListener('resize', () => bar && bar.resize())
})

onBeforeUnmount(() => bar && bar.dispose())
</script>

<style scoped>
.dash { display: flex; flex-direction: column; gap: 20px; }
.stats { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; }
.stat { display: flex; align-items: center; gap: 14px; }
.stat .icon {
  width: 48px; height: 48px; border-radius: 14px; color: #fff;
  display: flex; align-items: center; justify-content: center;
}
.icon.stu { background: linear-gradient(135deg, #6a94ff, #4a7dff); }
.icon.graph { background: linear-gradient(135deg, #ffb27a, #ff8a3d); }
.icon.doc { background: linear-gradient(135deg, #6fd6c0, #38b8a0); }
.icon.quiz { background: linear-gradient(135deg, #c39bff, #9a6fff); }
.icon.practice { background: linear-gradient(135deg, #ff9fb8, #f5679a); }
.stat strong { font-size: 24px; color: var(--ink); display: block; }
.stat span { font-size: 12px; color: var(--ink-3); }

.grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 20px; align-items: start; }
.bar { height: 300px; }
.quick { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.q-item {
  display: flex; align-items: center; gap: 10px; padding: 14px 16px;
  border-radius: 12px; background: #f6f8fd; color: var(--ink-2); cursor: pointer;
  font-size: 14px; transition: all .2s;
}
.q-item:hover { background: rgba(74,125,255,.1); color: var(--primary); transform: translateY(-2px); }
</style>
