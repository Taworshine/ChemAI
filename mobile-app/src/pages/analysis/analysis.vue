<template>
  <view>
    <!-- 头部说明卡 -->
    <view class="card head-card">
      <view class="row between">
        <view style="flex:1">
          <text class="title">班级掌握度热力图</text>
          <text class="muted class-name">高三(2)班 · {{ students.length }} 名学生</text>
        </view>
        <view class="heat-icon"><uni-icons type="map" size="56" color="#00B578" /></view>
      </view>
      <view class="muted sub">行 = 学生，列 = 知识点，颜色越绿掌握越好；左右滑动查看全部学科</view>
    </view>

    <!-- 热力图 -->
    <scroll-view scroll-x class="heatmap-scroll">
      <view class="heatmap" :style="{ width: heatmapWidth + 'px' }">
        <view class="h-row head">
          <view class="h-cell name-cell head-cell">学生</view>
          <view v-for="c in concepts" :key="c" class="h-cell concept-cell">
            <text class="v-text">{{ c }}</text>
          </view>
        </view>
        <view v-for="(s, i) in students" :key="s" class="h-row">
          <view class="h-cell name-cell" :class="{ stripe: i % 2 }">{{ s }}</view>
          <view v-for="(c, j) in concepts" :key="c" class="h-cell data-cell" :style="{ background: cellColor(value(i, j)) }">
            <text class="v-num">{{ Math.round(value(i, j) * 100) }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- 图例 -->
    <view class="card legend-card">
      <view class="row between">
        <text class="muted">掌握度图例</text>
        <text class="muted">0 – 100</text>
      </view>
      <view class="legend-bar"></view>
      <view class="row legend-labels">
        <text class="muted">弱</text>
        <text class="muted">中</text>
        <text class="muted">强</text>
      </view>
    </view>

    <view v-if="loaded && students.length === 0" class="empty-state">
      <view class="empty-icon"><uni-icons type="chart" size="72" color="#c4c9d2" /></view>
      <text class="empty-text">暂无学情数据</text>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

const CELL_W = 52
const NAME_W = 110

export default {
  data() {
    return {
      students: [],
      concepts: [],
      data: [],
      loaded: false
    }
  },
  computed: {
    heatmapWidth() {
      return NAME_W + this.concepts.length * CELL_W
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    async load() {
      try {
        const res = await request({ url: '/api/analysis/class/heatmap' })
        this.students = res.students
        this.concepts = res.concepts
        this.data = res.data
        this.loaded = true
      } catch (e) { /* 已提示 */ }
    },
    value(i, j) {
      const hit = this.data.find(d => d[1] === i && d[0] === j)
      return hit ? hit[2] : 0
    },
    cellColor(v) {
      if (v >= 0.85) return '#52c41a'
      if (v >= 0.7) return '#a0d911'
      if (v >= 0.55) return '#faad14'
      if (v >= 0.4) return '#fa8c16'
      return '#ff4d4f'
    }
  }
}
</script>

<style scoped>
.head-card .title { font-size: 34rpx; font-weight: 700; display: block; }
.class-name { margin-top: 10rpx; display: block; }
.heat-icon {
  width: 96rpx; height: 96rpx; border-radius: 26rpx;
  background: #e2f7ee; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.sub { margin-top: 16rpx; line-height: 1.6; }
.heatmap-scroll { margin: 0 24rpx; background: #fff; border-radius: 24rpx; border: 1rpx solid #f2f3f5; }
.heatmap { padding: 16rpx; }
.h-row { display: flex; }
.h-cell { box-sizing: border-box; }
.head-cell { background: #f4f5f7; font-weight: 600; color: #4e5969; }
.name-cell { width: 110px; font-size: 22rpx; display: flex; align-items: center; padding: 0 12rpx; flex-shrink: 0; height: 60rpx; color: #4e5969; }
.name-cell.stripe { background: #fafbfc; }
.concept-cell { width: 52px; height: 92rpx; display: flex; align-items: flex-end; justify-content: center; padding-bottom: 10rpx; }
.v-text { font-size: 20rpx; color: #86909c; writing-mode: vertical-lr; letter-spacing: 2rpx; }
.data-cell { width: 52px; height: 60rpx; display: flex; align-items: center; justify-content: center; border: 1rpx solid rgba(255,255,255,0.4); }
.v-num { font-size: 20rpx; color: rgba(255,255,255,0.95); font-weight: 600; }
.legend-card { display: flex; flex-direction: column; gap: 14rpx; }
.legend-bar {
  height: 24rpx; border-radius: 999rpx;
  background: linear-gradient(90deg, #ff4d4f, #fa8c16, #faad14, #a0d911, #52c41a);
}
.legend-labels { justify-content: space-between; }
</style>
