<template>
  <view>
    <!-- 问候区 -->
    <view class="greet">
      <text class="greet-hi">{{ greeting }}，{{ (user.realName || '老师').replace(/老师$/, '') }}老师</text>
      <text class="greet-sub">{{ todaySummary }}</text>
    </view>

    <!-- 双主按钮 -->
    <view class="actions">
      <view class="btn-primary act-main" @click="goCreate">
        <uni-icons type="plus" size="40" color="#fff" />
        <text>发起会议</text>
      </view>
      <view class="btn-outline act-main" @click="goJoin">
        <uni-icons type="scan" size="40" color="#00b578" />
        <text>扫码加入</text>
      </view>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading">
      <view class="skeleton"><view class="skeleton-line" style="width:60%"></view><view class="skeleton-line" style="width:40%"></view></view>
      <view class="skeleton"><view class="skeleton-line" style="width:50%"></view><view class="skeleton-line" style="width:70%"></view></view>
    </view>

    <template v-else>
      <!-- 进行中 -->
      <view v-if="live.length" class="group-title">进行中</view>
      <view v-for="item in live" :key="item.meeting.id" class="card meet-card" @click="enterRoom(item.meeting)">
        <view class="row between">
          <view class="row meet-head">
            <view class="subject-tag" :style="{ background: colorOf(item.meeting.subject) }">{{ (item.meeting.subject || '?').charAt(0) }}</view>
            <text class="meet-title">{{ item.meeting.title }}</text>
          </view>
          <view class="live-dot-wrap"><view class="live-dot"></view></view>
        </view>
        <view class="meet-meta">
          <uni-icons type="clock" size="26" color="#86909c" />
          <text>{{ fmtTime(item.meeting.startTime) }} · {{ hostName(item) }} 主持</text>
        </view>
        <view class="meet-foot" v-if="item.latestChat">
          <uni-icons type="chat" size="26" color="#00b578" />
          <text class="foot-text">讨论 {{ item.latestChat }} 条新消息</text>
          <text class="foot-go">进入 ›</text>
        </view>
        <view class="meet-foot" v-else>
          <text class="foot-text muted">{{ item.members.length }} 人参会 · № {{ item.meeting.meetingNo }}</text>
          <text class="foot-go">进入 ›</text>
        </view>
      </view>

      <!-- 待开始 -->
      <view v-if="upcoming.length" class="group-title">待开始</view>
      <view v-for="item in upcoming" :key="item.meeting.id" class="card meet-card" @click="enterRoom(item.meeting)">
        <view class="row between">
          <view class="row meet-head">
            <view class="subject-tag" :style="{ background: colorOf(item.meeting.subject) }">{{ (item.meeting.subject || '?').charAt(0) }}</view>
            <text class="meet-title">{{ item.meeting.title }}</text>
          </view>
          <view class="status-dot wait"></view>
        </view>
        <view class="meet-meta">
          <uni-icons type="clock" size="26" color="#86909c" />
          <text>{{ fmtTime(item.meeting.startTime) }} · {{ hostName(item) }} 主持</text>
        </view>
      </view>

      <!-- 空态 -->
      <view v-if="!live.length && !upcoming.length && !finished.length" class="empty-state">
        <view class="empty-icon"><uni-icons type="chat" size="72" color="#c4c9d2" /></view>
        <text style="color:#4e5969">今天还没有教研会</text>
        <text class="muted">点上方「发起会议」约一场吧</text>
      </view>

      <!-- 已完成：折叠 -->
      <view v-if="finished.length" class="cell fold-cell" @click="showFinished = !showFinished">
        <view class="cell-body"><text class="cell-name" style="color:#86909c">已完成（{{ finished.length }}）</text></view>
        <uni-icons :type="showFinished ? 'down' : 'right'" size="32" color="#c4c9d2" />
      </view>
      <template v-if="showFinished">
        <view v-for="item in finished" :key="item.meeting.id" class="card meet-card done-card" @click="enterRoom(item.meeting)">
          <view class="row meet-head">
            <view class="subject-tag" :style="{ background: colorOf(item.meeting.subject) }">{{ (item.meeting.subject || '?').charAt(0) }}</view>
            <text class="meet-title done">{{ item.meeting.title }}</text>
            <text class="muted" style="margin-left:auto">已结束</text>
          </view>
        </view>
      </template>
    </template>

    <!-- 会后任务入口 -->
    <view class="card task-entry" v-if="pendingTasks.length" @click="goTasks">
      <view class="row">
        <uni-icons type="task" size="40" color="#ed7b2f" />
        <text class="task-name">会后任务</text>
        <text class="task-badge">{{ pendingTasks.length }}</text>
      </view>
      <view class="row between" style="margin-top:16rpx">
        <text class="muted" style="font-size:26rpx">{{ pendingTasks[0].task.title }}</text>
        <uni-icons type="right" size="30" color="#c4c9d2" />
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

const SUBJECT_COLORS = {
  '化学': '#00B578', '数学': '#4A6FA5', '物理': '#7B61C9', '生物': '#3AAE8C',
  '语文': '#D97706', '英语': '#4E8BD8', '历史': '#B4534E', '地理': '#2E9E97', '政治': '#8C5E3C'
}

export default {
  data() {
    return {
      loading: true,
      list: [],
      tasks: [],
      showFinished: false,
      user: {}
    }
  },
  computed: {
    greeting() {
      const h = new Date().getHours()
      return h < 6 ? '夜深了' : h < 12 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
    },
    live() { return this.list.filter(i => i.meeting.status === 1) },
    upcoming() { return this.list.filter(i => i.meeting.status === 0) },
    finished() { return this.list.filter(i => i.meeting.status >= 2) },
    pendingTasks() { return this.tasks.filter(t => t.task.status !== 2) },
    todaySummary() {
      const n = this.live.length + this.upcoming.length
      if (this.loading) return '正在加载…'
      return n ? `有 ${n} 场教研会等着你` : '今天没有安排教研会'
    }
  },
  onShow() {
    this.user = uni.getStorageSync('userInfo') || {}
    this.load()
    this.loadTasks()
  },
  onPullDownRefresh() {
    Promise.all([this.load(), this.loadTasks()]).finally(() => uni.stopPullDownRefresh())
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const [host, member] = await Promise.all([
          request({ url: '/api/mobile/meeting/list?tab=host' }),
          request({ url: '/api/mobile/meeting/list?tab=member' })
        ])
        const seen = new Set()
        this.list = [...host, ...member].filter(i => !seen.has(i.meeting.id) && seen.add(i.meeting.id))
      } catch (e) { this.list = [] }
      this.loading = false
    },
    async loadTasks() {
      try { this.tasks = await request({ url: '/api/mobile/task/list' }) } catch (e) { this.tasks = [] }
    },
    colorOf(s) { return SUBJECT_COLORS[s] || '#00B578' },
    hostName(item) {
      const host = item.members.find(m => m.role === 'HOST')
      return host ? host.realName : ''
    },
    fmtTime(t) {
      if (!t) return '时间待定'
      const d = new Date(t.replace(' ', 'T'))
      const today = new Date()
      const sameDay = d.toDateString() === today.toDateString()
      const hm = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
      if (sameDay) return `今天 ${hm}`
      const tomorrow = new Date(today.getTime() + 86400000)
      if (d.toDateString() === tomorrow.toDateString()) return `明天 ${hm}`
      return `${d.getMonth() + 1}月${d.getDate()}日 ${hm}`
    },
    goCreate() { uni.navigateTo({ url: '/pages/meeting/create' }) },
    goJoin() { uni.navigateTo({ url: '/pages/meeting/join' }) },
    goTasks() { uni.navigateTo({ url: '/pages/mine/tasks' }) },
    enterRoom(meeting) { uni.navigateTo({ url: '/pages/meeting/room?id=' + meeting.id }) }
  }
}
</script>

<style scoped>
.greet { padding: 32rpx 40rpx 8rpx; }
.greet-hi { font-size: 40rpx; font-weight: 600; color: #191919; display: block; }
.greet-sub { font-size: 26rpx; color: #86909c; margin-top: 10rpx; display: block; }

.actions { display: flex; gap: 24rpx; padding: 26rpx 32rpx 8rpx; }
.act-main { flex: 1; display: flex; align-items: center; justify-content: center; gap: 12rpx; }

.meet-card { padding: 30rpx; }
.meet-head { flex: 1; min-width: 0; }
.subject-tag {
  width: 56rpx; height: 56rpx; border-radius: 16rpx;
  color: #fff; font-size: 28rpx; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  margin-right: 20rpx; flex-shrink: 0;
}
.meet-title { font-size: 32rpx; font-weight: 600; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meet-title.done { color: #86909c; font-weight: 500; }
.meet-meta { display: flex; align-items: center; gap: 10rpx; margin-top: 18rpx; font-size: 26rpx; color: #4e5969; }
.meet-foot {
  margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid #f5f6f8;
  display: flex; align-items: center; gap: 10rpx;
}
.foot-text { font-size: 26rpx; color: #00b578; flex: 1; }
.foot-go { font-size: 26rpx; color: #c4c9d2; }
.live-dot-wrap { flex-shrink: 0; padding: 10rpx; }
.live-dot { width: 16rpx; height: 16rpx; border-radius: 50%; background: #00b578; animation: breathe 1.6s ease-in-out infinite; }
.status-dot.wait { width: 14rpx; height: 14rpx; border-radius: 50%; background: #c4c9d2; flex-shrink: 0; }
@keyframes breathe { 0%,100% { opacity: 1; } 50% { opacity: 0.25; } }

.done-card { padding: 24rpx 30rpx; margin: 12rpx 24rpx; }
.fold-cell { padding: 0 32rpx; }

.task-entry { margin-top: 28rpx; }
.task-name { font-size: 30rpx; font-weight: 600; margin-left: 16rpx; flex: 1; }
.task-badge {
  background: #fa5151; color: #fff; font-size: 22rpx;
  min-width: 36rpx; height: 36rpx; border-radius: 18rpx; padding: 0 10rpx;
  display: flex; align-items: center; justify-content: center;
}
</style>
