<template>
  <div class="analysis">
    <div class="soft-card">
      <div class="card-head">
        <div>
          <h3 class="section-title" style="margin:0">班级概念掌握度热力图</h3>
          <p class="ph-desc" style="margin:6px 0 0">颜色越蓝表示掌握越牢，越橙表示越薄弱，点击色块查看详情</p>
        </div>
      </div>
      <div ref="heatRef" class="chart heat"></div>
    </div>
    <div class="soft-card">
      <div class="card-head">
        <div>
          <h3 class="section-title" style="margin:0">个体五维能力雷达</h3>
          <p class="ph-desc" style="margin:6px 0 0">从概念理解、逻辑推理、计算准确、知识迁移、表达规范五个维度评估</p>
        </div>
      </div>
      <div class="radar-row">
        <div ref="radarRef" class="chart radar"></div>
        <div class="student-list">
          <p class="label">选择学生</p>
          <div class="stu-scroll">
            <div v-for="s in students" :key="s.studentId" class="student-item"
                 :class="{ active: s.studentId === currentId }" @click="select(s.studentId)">
              <span class="s-av">{{ (s.realName || '学').charAt(0) }}</span>
              <span class="s-name">{{ s.realName }}</span>
              <span class="no">{{ s.username }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'
import api from '../../api'

const heatRef = ref(null)
const radarRef = ref(null)
const students = ref([])
const currentId = ref(null)
let heatChart = null
let radarChart = null

async function loadHeat() {
  const res = await api.get('/analysis/class/heatmap')
  const { students: names, concepts, data } = res.data
  heatChart.setOption({
    tooltip: { formatter: p => `${names[p.value[1]]}<br/>${concepts[p.value[0]]}：${(p.value[2] * 100).toFixed(0)}%` },
    grid: { height: '62%', top: '10%' },
    xAxis: { type: 'category', data: concepts, splitArea: { show: true } },
    yAxis: { type: 'category', data: names, splitArea: { show: true } },
    visualMap: {
      min: 0, max: 1, calculable: true, orient: 'horizontal', left: 'center', bottom: '2%',
      inRange: { color: ['#ffd9ad', '#ff9a4d', '#4a7dff'] }, text: ['掌握', '薄弱'],
      textStyle: { color: '#6b7386' }
    },
    series: [{
      type: 'heatmap', data,
      label: { show: true, formatter: p => (p.value[2] * 100).toFixed(0) },
      itemStyle: { borderColor: '#fff', borderWidth: 2 }
    }]
  })
}

async function select(id) {
  currentId.value = id
  const res = await api.get(`/analysis/student/${id}/radar`)
  radarChart.setOption({
    title: { text: res.data.realName, left: 'center', textStyle: { fontSize: 15, color: '#2c3347' } },
    radar: {
      indicator: res.data.dimensions.map(d => ({ name: d, max: 100 })),
      radius: '62%',
      axisName: { color: '#6b7386' },
      splitArea: { areaStyle: { color: ['#fbfcff', '#f2f5fd'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: res.data.values,
        name: '五维能力',
        areaStyle: { color: 'rgba(74,125,255,.22)' },
        itemStyle: { color: '#4a7dff' },
        lineStyle: { color: '#4a7dff' }
      }]
    }]
  })
}

onMounted(async () => {
  heatChart = echarts.init(heatRef.value)
  radarChart = echarts.init(radarRef.value)
  const res = await api.get('/analysis/students')
  students.value = res.data
  await loadHeat()
  if (students.value.length) select(students.value[0].studentId)
  window.addEventListener('resize', () => { heatChart.resize(); radarChart.resize() })
})

onBeforeUnmount(() => { heatChart?.dispose(); radarChart?.dispose() })
</script>

<style scoped>
.analysis { display: flex; flex-direction: column; gap: 20px; }
.chart.heat { height: 340px; }
.radar-row { display: flex; gap: 24px; }
.chart.radar { height: 380px; flex: 1; }
.student-list { width: 230px; display: flex; flex-direction: column; }
.label { color: var(--ink-3); font-size: 13px; margin: 0 0 10px; }
.stu-scroll { overflow-y: auto; max-height: 380px; display: flex; flex-direction: column; gap: 6px; padding-right: 4px; }
.student-item {
  padding: 8px 12px; border-radius: 10px; cursor: pointer;
  color: var(--ink-2); font-size: 14px; display: flex; align-items: center; gap: 8px;
  transition: all .18s; border: 1px solid transparent;
}
.student-item:hover { background: #f4f7fd; }
.student-item.active {
  background: rgba(74,125,255,.1); border-color: rgba(74,125,255,.35);
  color: var(--primary); font-weight: 600;
}
.s-av {
  width: 26px; height: 26px; border-radius: 50%; color: #fff; font-size: 12px; font-weight: 600;
  background: linear-gradient(135deg, #6a94ff, #4a7dff);
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.student-item.active .s-av { background: linear-gradient(135deg, #ffb27a, #ff8a3d); }
.s-name { flex: 1; }
.student-item .no { color: var(--ink-3); font-size: 12px; }
</style>
