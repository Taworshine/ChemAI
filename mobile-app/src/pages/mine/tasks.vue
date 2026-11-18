<template>
  <view>
    <view v-if="!tasks.length && !loading" class="empty-state">
      <view class="empty-icon"><uni-icons type="task" size="72" color="#c4c9d2" /></view>
      <text style="color:#4e5969">暂无会后任务</text>
      <text class="muted">会议中分配的任务会出现在这里</text>
    </view>
    <view class="card" style="padding:8rpx 32rpx">
      <view v-for="t in tasks" :key="t.task.id" class="task-item" :class="{ done: t.task.status === 2 }">
        <view class="task-check" :class="{ checked: t.task.status === 2 }" @click="t.task.status !== 2 && complete(t.task.id)">
          <uni-icons v-if="t.task.status === 2" type="check" size="26" color="#fff" />
        </view>
        <view class="task-body">
          <text class="task-title">{{ t.task.title }}</text>
          <text class="muted task-meta">会议 #{{ t.task.meetingId }} · 截止 {{ fmt(t.task.deadline) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

export default {
  data() { return { loading: true, tasks: [] } },
  onShow() { this.load() },
  methods: {
    async load() {
      try { this.tasks = await request({ url: '/api/mobile/task/list' }) } catch (e) { this.tasks = [] }
      this.loading = false
    },
    complete(id) {
      uni.showModal({
        title: '确认完成？',
        success: async (res) => {
          if (res.confirm) {
            await request({ url: '/api/mobile/task/' + id + '/complete', method: 'POST' })
            uni.showToast({ title: '已完成', icon: 'success' })
            this.load()
          }
        }
      })
    },
    fmt(t) { return t ? String(t).replace('T', ' ').substring(0, 16) : '未设置' }
  }
}
</script>

<style scoped>
.task-item { display: flex; align-items: flex-start; padding: 28rpx 0; border-bottom: 1rpx solid #f5f6f8; }
.task-item:last-child { border-bottom: none; }
.task-item.done { opacity: 0.5; }
.task-item.done .task-title { text-decoration: line-through; }
.task-check {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  border: 4rpx solid #c8c9cc; margin-right: 24rpx; margin-top: 2rpx;
  display: flex; align-items: center; justify-content: center;
  box-sizing: border-box; flex-shrink: 0;
}
.task-check.checked { background: #00b578; border-color: #00b578; }
.task-body { flex: 1; }
.task-title { font-size: 30rpx; display: block; line-height: 1.5; }
.task-meta { margin-top: 8rpx; display: block; }
</style>
