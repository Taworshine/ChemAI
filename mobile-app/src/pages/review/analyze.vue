<template>
  <view>
    <view class="card head-card">
      <view class="row between">
        <text class="title">量化评审</text>
        <view class="head-tag">AI × 知识图谱</view>
      </view>
      <view class="muted sub">教案解析出的知识点与标准图谱自动比对，给出三项质量指标与缺失项改进建议</view>
    </view>

    <!-- 三项指标 -->
    <view v-if="result" class="card score-card">
      <view v-for="s in scores" :key="s.label" class="score-block">
        <view class="score-top">
          <view class="score-ring" :style="{ borderColor: s.color }">
            <text class="score-num" :style="{ color: s.color }">{{ s.data.percent }}<text class="pct">%</text></text>
          </view>
          <view class="score-info">
            <text class="score-label">{{ s.label }}</text>
            <text class="muted">已覆盖 {{ s.data.covered.length }} / {{ s.data.covered.length + s.data.missing.length }} 项</text>
          </view>
        </view>
        <view class="progress-track">
          <view class="progress-fill" :style="{ width: s.data.percent + '%', background: s.color }"></view>
        </view>
      </view>
    </view>

    <!-- 缺失项 -->
    <view v-if="result" class="card">
      <view class="block-head">
        <view class="block-bar warn"></view>
        <text class="block-title">教案未覆盖的图谱要点</text>
      </view>
      <view class="missing-group">
        <text class="group-label">缺失知识点</text>
        <text v-for="m in result.conceptCoverage.missing" :key="m" class="chip chip-red">{{ m }}</text>
        <text v-if="!result.conceptCoverage.missing.length" class="chip chip-green"><uni-icons type="check" size="20" color="#00b578" /> 全部覆盖</text>
      </view>
      <view class="missing-group">
        <text class="group-label">缺失思维路径</text>
        <text v-for="m in result.pathCoverage.missing" :key="m" class="chip chip-red">{{ m }}</text>
        <text v-if="!result.pathCoverage.missing.length" class="chip chip-green"><uni-icons type="check" size="20" color="#00b578" /> 全部覆盖</text>
      </view>
      <view class="missing-group" style="margin-bottom:0">
        <text class="group-label">未预判的常见错误</text>
        <text v-for="m in result.misconceptionCoverage.missing" :key="m" class="chip chip-red">{{ m }}</text>
        <text v-if="!result.misconceptionCoverage.missing.length" class="chip chip-green"><uni-icons type="check" size="20" color="#00b578" /> 全部覆盖</text>
      </view>
    </view>

    <!-- 已覆盖 -->
    <view v-if="result" class="card">
      <view class="block-head">
        <view class="block-bar ok"></view>
        <text class="block-title">教案已覆盖的知识点</text>
      </view>
      <text v-for="c in result.conceptCoverage.covered" :key="c" class="chip chip-green">{{ c }}</text>
      <text v-if="!result.conceptCoverage.covered.length" class="muted">暂无命中</text>
    </view>

    <view class="footer">
      <button class="btn-primary back press" @click="back">返回会议</button>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

export default {
  data() {
    return {
      docId: null,
      result: null
    }
  },
  computed: {
    scores() {
      if (!this.result) return []
      return [
        { label: '知识覆盖率', data: this.result.conceptCoverage, color: '#00b578' },
        { label: '思维路径完整性', data: this.result.pathCoverage, color: '#00b42a' },
        { label: '重难点预判', data: this.result.misconceptionCoverage, color: '#ff7d00' }
      ]
    }
  },
  onLoad(options) {
    this.docId = options.docId
    this.load()
  },
  methods: {
    async load() {
      if (!this.docId) return
      try {
        this.result = await request({ url: '/api/mobile/review/analyze/' + this.docId })
      } catch (e) { this.result = null }
    },
    back() { uni.navigateBack() }
  }
}
</script>

<style scoped>
.head-card .title { font-size: 36rpx; font-weight: 700; }
.head-tag {
  font-size: 22rpx; color: #0f5c46; background: #e2f7ee;
  padding: 8rpx 24rpx; border-radius: 999rpx; font-weight: 600;
}
.sub { margin-top: 14rpx; line-height: 1.6; }

.score-card { padding: 6rpx 32rpx; }
.score-block { padding: 30rpx 0; border-bottom: 1rpx solid #f5f6f8; }
.score-block:last-child { border-bottom: none; }
.score-top { display: flex; align-items: center; }
.score-ring {
  width: 116rpx; height: 116rpx; border-radius: 50%;
  border: 10rpx solid #00b578;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; background: #fff;
}
.score-num { font-size: 34rpx; font-weight: 700; }
.pct { font-size: 20rpx; font-weight: 500; }
.score-info { margin-left: 28rpx; display: flex; flex-direction: column; gap: 10rpx; }
.score-label { font-size: 30rpx; font-weight: 600; }
.progress-track { background: #f0f1f3; border-radius: 999rpx; height: 12rpx; margin-top: 24rpx; overflow: hidden; }
.progress-fill { height: 100%; border-radius: 999rpx; transition: width 0.5s; }

.block-head { display: flex; align-items: center; gap: 14rpx; margin-bottom: 24rpx; }
.block-bar { width: 8rpx; height: 30rpx; border-radius: 999rpx; }
.block-bar.warn { background: #ff7d00; }
.block-bar.ok { background: #00b578; }
.block-title { font-size: 30rpx; font-weight: 700; }
.missing-group { margin-bottom: 28rpx; }
.group-label { display: block; font-size: 24rpx; color: #86909c; margin-bottom: 14rpx; }

.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1rpx solid #f2f3f5;
}
.back { border-radius: 999rpx; }
</style>
