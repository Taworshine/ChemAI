<template>
  <view>
    <!-- 顶栏：分段切换 + 录错题 -->
    <view class="head">
      <view class="seg">
        <view class="seg-item" :class="{ on: tab === 'wrong' }" @click="switchTab('wrong')">错题</view>
        <view class="seg-item" :class="{ on: tab === 'exercise' }" @click="switchTab('exercise')">习题</view>
        <view class="seg-item" :class="{ on: tab === 'study' }" @click="switchTab('study')">学情</view>
      </view>
      <view class="head-add" @click="goRecord">
        <uni-icons type="camera" size="36" color="#fff" />
      </view>
    </view>

    <!-- ========== 错题 ========== -->
    <scroll-view v-show="tab === 'wrong'" scroll-y>
      <view v-if="loading" class="skeleton"><view class="skeleton-line" style="width:70%"></view><view class="skeleton-line" style="width:40%"></view></view>
      <view v-else-if="!wrongs.length" class="empty-state">
        <view class="empty-icon"><uni-icons type="doc" size="72" color="#c4c9d2" /></view>
        <text style="color:#4e5969">还没有错题</text>
        <text class="muted">点右上角相机，拍一道错题试试</text>
      </view>
      <view v-else>
        <view v-for="w in wrongs" :key="w.id" class="card q-card" @longpress="removeWrong(w)">
          <view class="row between">
            <view class="row" style="gap:12rpx;flex:1;min-width:0">
              <text class="q-type wrong">错题</text>
              <text class="q-concept">{{ w.conceptName || '未标注' }}</text>
            </view>
            <text class="q-count">{{ w.wrongCount }} 人错</text>
          </view>
          <text class="q-content">{{ w.content }}</text>
          <view class="row" style="gap:12rpx">
            <text v-if="w.wrongReason" class="q-reason">错因：{{ w.wrongReason }}</text>
            <text class="muted" style="font-size:22rpx">{{ w.subject }}</text>
          </view>
        </view>
      </view>
    </scroll-view>

    <!-- ========== 习题 ========== -->
    <scroll-view v-show="tab === 'exercise'" scroll-y>
      <view v-if="!exercises.length" class="empty-state">
        <view class="empty-icon"><uni-icons type="list" size="72" color="#c4c9d2" /></view>
        <text style="color:#4e5969">还没有习题</text>
        <text class="muted">习题由命题研讨和录题流程积累</text>
      </view>
      <view v-else>
        <view v-for="q in exercises" :key="q.id" class="card q-card">
          <view class="row between">
            <view class="row" style="gap:12rpx;flex:1;min-width:0">
              <text class="q-type exercise">习题</text>
              <text class="q-concept">{{ q.conceptName || '未标注' }}</text>
            </view>
            <text class="q-count">{{ '★'.repeat({ easy: 1, medium: 2, hard: 3 }[q.difficulty] || 1) }}</text>
          </view>
          <text class="q-content">{{ q.content }}</text>
        </view>
      </view>
    </scroll-view>

    <!-- ========== 学情 ========== -->
    <scroll-view v-show="tab === 'study'" scroll-y>
      <view class="card study-card" @click="goHeatmap">
        <view class="row between">
          <text class="study-title">班级掌握度热力图</text>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
        <text class="muted">行 = 学生，列 = 知识点，越绿掌握越好</text>
      </view>

      <view class="card">
        <view class="row between" style="margin-bottom:20rpx">
          <text class="study-title">本班薄弱点</text>
          <text class="muted" style="font-size:22rpx">来自 {{ wrongs.length }} 道错题</text>
        </view>
        <view v-if="weakness.length" class="weak-list">
          <view v-for="(p, i) in weakness" :key="p.concept" class="weak-item">
            <text class="weak-rank" :class="'r' + i">{{ i + 1 }}</text>
            <text class="weak-name">{{ p.concept }}</text>
            <view class="weak-bar"><view class="weak-fill" :style="{ width: (p.count / weakness[0].count * 100) + '%' }"></view></view>
            <text class="weak-count">{{ p.count }}</text>
          </view>
        </view>
        <view v-else class="muted" style="text-align:center;padding:30rpx 0">积累错题后自动生成薄弱点</view>
      </view>
    </scroll-view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

export default {
  data() {
    return {
      tab: 'wrong',
      loading: true,
      wrongs: [],
      exercises: [],
      weakness: []
    }
  },
  onShow() { this.loadAll() },
  onPullDownRefresh() { this.loadAll().finally(() => uni.stopPullDownRefresh()) },
  methods: {
    async loadAll() {
      this.loading = true
      try { this.wrongs = await request({ url: '/api/mobile/wrong/list' }) } catch (e) { this.wrongs = [] }
      try { this.exercises = await request({ url: '/api/mobile/exercise/list' }) } catch (e) { this.exercises = [] }
      try {
        const w = await request({ url: '/api/mobile/wrong/weakness' })
        this.weakness = w.points || []
      } catch (e) { this.weakness = [] }
      this.loading = false
    },
    switchTab(t) { this.tab = t },
    goRecord() { uni.navigateTo({ url: '/pages/bank/record' }) },
    goHeatmap() { uni.navigateTo({ url: '/pages/analysis/analysis' }) },
    removeWrong(w) {
      uni.showModal({
        title: '删除这道错题？',
        success: async (res) => {
          if (res.confirm) {
            await request({ url: '/api/mobile/wrong/' + w.id, method: 'DELETE' })
            this.loadAll()
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 20rpx; padding: 28rpx 32rpx 10rpx; }
.seg {
  flex: 1; display: flex; background: #eceef1; border-radius: 14rpx; padding: 6rpx;
}
.seg-item {
  flex: 1; text-align: center; padding: 14rpx 0;
  font-size: 26rpx; color: #4e5969; border-radius: 10rpx;
}
.seg-item.on { background: #fff; color: #00b578; font-weight: 600; }
.head-add {
  width: 72rpx; height: 72rpx; border-radius: 50%;
  background: #00b578; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.head-add:active { background: #009b68; }

.q-card { padding: 28rpx 30rpx; }
.q-type { font-size: 20rpx; padding: 4rpx 14rpx; border-radius: 6rpx; flex-shrink: 0; }
.q-type.wrong { background: #fdeceb; color: #fa5151; }
.q-type.exercise { background: #e2f7ee; color: #00b578; }
.q-concept { font-size: 24rpx; color: #4a6fa5; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.q-count { font-size: 24rpx; color: #ed7b2f; flex-shrink: 0; }
.q-content {
  display: block; font-size: 28rpx; color: #191919; line-height: 1.6;
  margin: 18rpx 0 14rpx;
  display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden;
}
.q-reason { font-size: 22rpx; color: #ed7b2f; background: #fdf3e7; padding: 4rpx 14rpx; border-radius: 6rpx; }

.study-card { padding: 30rpx; }
.study-title { font-size: 30rpx; font-weight: 600; }

.weak-list { display: flex; flex-direction: column; gap: 24rpx; }
.weak-item { display: flex; align-items: center; gap: 18rpx; }
.weak-rank {
  width: 40rpx; height: 40rpx; border-radius: 10rpx;
  display: flex; align-items: center; justify-content: center;
  font-size: 24rpx; background: #f2f3f5; color: #86909c; flex-shrink: 0;
}
.weak-rank.r0 { background: #fdeceb; color: #fa5151; }
.weak-rank.r1 { background: #fdf3e7; color: #ed7b2f; }
.weak-rank.r2 { background: #fff8e0; color: #d4a017; }
.weak-name { width: 240rpx; font-size: 27rpx; flex-shrink: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.weak-bar { flex: 1; height: 16rpx; background: #f2f3f5; border-radius: 8rpx; overflow: hidden; }
.weak-fill { height: 16rpx; background: #fa5151; border-radius: 8rpx; transition: width 0.4s; }
.weak-count { width: 60rpx; text-align: right; font-size: 26rpx; color: #4e5969; flex-shrink: 0; }
</style>
