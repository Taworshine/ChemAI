<template>
  <view>
    <!-- 头部：深墨绿渐变 + 三格数据条 -->
    <view class="profile">
      <view class="p-top">
        <view class="avatar big" :style="{ background: subjectColor }">{{ (user.realName || '教').charAt(0) }}</view>
        <view class="p-info">
          <view class="row">
            <text class="p-name">{{ user.realName || '未登录' }}</text>
            <uni-icons type="edit" size="30" color="rgba(255,255,255,0.85)" />
          </view>
          <text class="p-sub">{{ subject ? subject + '教研组' : '未分配学科' }} · {{ user.role === 'TEACHER' ? '教师' : '学生' }}</text>
        </view>
      </view>
      <view class="p-stats">
        <view class="p-stat" @click="go('/pages/meeting/list')">
          <text class="p-num">{{ stats.meetings }}</text>
          <text class="p-label">会议</text>
        </view>
        <view class="p-divider"></view>
        <view class="p-stat">
          <text class="p-num">{{ stats.plans }}</text>
          <text class="p-label">教案</text>
        </view>
        <view class="p-divider"></view>
        <view class="p-stat">
          <text class="p-num">{{ stats.questions }}</text>
          <text class="p-label">录题</text>
        </view>
      </view>
    </view>

    <!-- 我的工作：下拉分组 -->
    <view class="group-title">我的工作</view>
    <view class="card menu-card">
      <view class="cell" @click="toggle('work')">
        <uni-icons class="cell-icon" type="grid" size="40" color="#00b578" />
        <view class="cell-body"><text class="cell-name">工作清单</text></view>
        <uni-icons :type="open.work ? 'down' : 'right'" size="32" color="#c4c9d2" />
      </view>
      <view v-show="open.work" class="menu-drop">
        <view class="cell sub" @click="go('/pages/mine/minutes')">
          <uni-icons class="cell-icon" type="doc" size="38" color="#4a6fa5" />
          <view class="cell-body"><text class="cell-name">会议纪要</text></view>
          <text class="cell-value">{{ stats.minutes }} 篇</text>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
        <view class="cell sub" @click="go('/pages/plan/list')">
          <uni-icons class="cell-icon" type="book" size="38" color="#00b578" />
          <view class="cell-body"><text class="cell-name">我的教案</text></view>
          <text class="cell-value">{{ stats.plans }} 份</text>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
        <view class="cell sub" @click="go('/pages/mine/tasks')">
          <uni-icons class="cell-icon" type="task" size="38" color="#ed7b2f" />
          <view class="cell-body"><text class="cell-name">会后任务</text></view>
          <text class="cell-value">{{ pendingTasks.length }} 项</text>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
      </view>
    </view>

    <!-- 数据中心：并排宫格 2x2 -->
    <view class="group-title">数据中心</view>
    <view class="card grid-card">
      <view class="grid">
        <view class="grid-item" @click="go('/pages/analysis/analysis')">
          <view class="grid-icon" style="background:#e2f7ee"><uni-icons type="chart" size="44" color="#00b578" /></view>
          <text class="grid-name">学情热力图</text>
        </view>
        <view class="grid-item" @click="go('/pages/mine/graph')">
          <view class="grid-icon" style="background:#eef2fa"><uni-icons type="map" size="44" color="#4a6fa5" /></view>
          <text class="grid-name">班级知识图谱</text>
        </view>
        <view class="grid-item" @click="go('/pages/mine/notice')">
          <view class="grid-icon" style="background:#fdf3e7"><uni-icons type="bell" size="44" color="#ed7b2f" /></view>
          <text class="grid-name">消息通知</text>
          <view v-if="unread" class="grid-dot"></view>
        </view>
        <view class="grid-item" @click="go('/pages/mine/favorites')">
          <view class="grid-icon" style="background:#f3effa"><uni-icons type="star" size="44" color="#7b61c9" /></view>
          <text class="grid-name">我的收藏</text>
        </view>
      </view>
    </view>

    <!-- 系统：下拉分组 -->
    <view class="group-title">系统</view>
    <view class="card menu-card">
      <view class="cell" @click="toggle('sys')">
        <uni-icons class="cell-icon" type="gear" size="40" color="#86909c" />
        <view class="cell-body"><text class="cell-name">设置</text></view>
        <uni-icons :type="open.sys ? 'down' : 'right'" size="32" color="#c4c9d2" />
      </view>
      <view v-show="open.sys" class="menu-drop">
        <view class="cell sub" @click="editServer">
          <uni-icons class="cell-icon" type="scan" size="36" color="#4a6fa5" />
          <view class="cell-body"><text class="cell-name">服务器连接</text></view>
          <text class="cell-value" style="color:#00b578">已连接</text>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
        <view class="cell sub" @click="clearCache">
          <uni-icons class="cell-icon" type="brush" size="36" color="#86909c" />
          <view class="cell-body"><text class="cell-name">清除缓存</text></view>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
        <view class="cell sub" @click="about">
          <uni-icons class="cell-icon" type="info" size="36" color="#86909c" />
          <view class="cell-body"><text class="cell-name">关于</text></view>
          <text class="cell-value">v1.1.0</text>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
      </view>
    </view>

    <view class="btn-danger-text logout" @click="logout">退出登录</view>
  </view>
</template>

<script>
import { request, setBaseUrl } from '@/utils/request.js'

const SUBJECT_COLORS = {
  '化学': '#00B578', '数学': '#4A6FA5', '物理': '#7B61C9', '生物': '#3AAE8C',
  '语文': '#D97706', '英语': '#4E8BD8', '历史': '#B4534E', '地理': '#2E9E97', '政治': '#8C5E3C'
}

export default {
  data() {
    return {
      user: {},
      subject: '',
      tasks: [],
      unread: 0,
      stats: { meetings: 0, plans: 0, questions: 0, minutes: 0 },
      open: { work: true, sys: false }
    }
  },
  computed: {
    subjectColor() { return SUBJECT_COLORS[this.subject] || '#00B578' },
    pendingTasks() { return this.tasks.filter(t => t.task.status !== 2) }
  },
  onShow() {
    const info = uni.getStorageSync('userInfo') || {}
    this.user = info
    this.subject = info.subject || ''
    this.loadAll()
  },
  methods: {
    toggle(key) { this.open[key] = !this.open[key] },
    go(url) { uni.navigateTo({ url }) },
    async loadAll() {
      try { this.tasks = await request({ url: '/api/mobile/task/list' }) } catch (e) { this.tasks = [] }
      try {
        const s = await request({ url: '/api/mobile/user/stats' })
        this.stats = { meetings: 0, plans: 0, questions: 0, minutes: 0, ...s }
      } catch (e) { /* 接口未上线时保持 0 */ }
    },
    editServer() {
      // #ifndef H5
      uni.showModal({
        title: '服务器连接',
        editable: true,
        content: uni.getStorageSync('serverBase') || 'http://192.168.5.4:8080',
        placeholderText: 'http://192.168.x.x:8080',
        success: (res) => {
          if (res.confirm && res.content) {
            setBaseUrl(res.content.replace(/\/$/, ''))
            uni.showToast({ title: '已保存，重新登录生效', icon: 'none' })
          }
        }
      })
      // #endif
      // #ifdef H5
      uni.showToast({ title: 'H5 调试模式由 vite 代理，无需修改', icon: 'none' })
      // #endif
    },
    clearCache() {
      uni.showModal({
        title: '清除缓存？',
        content: '将清除本地登录信息，需重新登录',
        success: (res) => {
          if (res.confirm) {
            uni.clearStorageSync()
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    },
    about() {
      uni.showModal({ title: '关于', content: '思维赋能 · 教研会议 v1.1.0\n高中教研组移动协作平台', showCancel: false })
    },
    logout() {
      uni.showModal({
        title: '退出登录？',
        success: (res) => {
          if (res.confirm) {
            uni.removeStorageSync('satoken')
            uni.removeStorageSync('userInfo')
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    }
  }
}
</script>

<style scoped>
/* 头部 */
.profile {
  margin: 20rpx 24rpx 0;
  border-radius: 24rpx;
  background: linear-gradient(150deg, #0f5c46, #12805f 70%, #149a6f);
  padding: 44rpx 36rpx 0;
}
.p-top { display: flex; align-items: center; gap: 26rpx; }
.avatar.big { width: 116rpx; height: 116rpx; font-size: 48rpx; border: 4rpx solid rgba(255,255,255,0.5); }
.p-info .p-name { color: #fff; font-size: 40rpx; font-weight: 600; margin-right: 14rpx; }
.p-info .p-sub { color: rgba(255,255,255,0.75); font-size: 26rpx; margin-top: 10rpx; display: block; }
.p-stats {
  margin-top: 38rpx;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 20rpx 20rpx 0 0;
  display: flex;
  padding: 30rpx 0;
}
.p-stat { flex: 1; display: flex; flex-direction: column; align-items: center; }
.p-num { color: #fff; font-size: 52rpx; font-weight: 500; line-height: 1.1; }
.p-label { color: rgba(255,255,255,0.7); font-size: 24rpx; margin-top: 8rpx; }
.p-divider { width: 1rpx; background: rgba(255,255,255,0.2); margin: 10rpx 0; }

/* 菜单卡片 */
.menu-card { padding: 0; overflow: hidden; }
.menu-drop { border-top: 1rpx solid #f5f6f8; }
.cell.sub { min-height: 88rpx; padding-left: 56rpx; }

/* 并排宫格 */
.grid-card { padding: 36rpx 10rpx; }
.grid { display: flex; flex-wrap: wrap; }
.grid-item {
  width: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24rpx 0;
  position: relative;
}
.grid-icon {
  width: 96rpx; height: 96rpx; border-radius: 28rpx;
  display: flex; align-items: center; justify-content: center;
}
.grid-name { font-size: 26rpx; color: #4e5969; margin-top: 16rpx; }
.grid-dot {
  position: absolute; top: 22rpx; right: 30%;
  width: 16rpx; height: 16rpx; border-radius: 50%;
  background: #fa5151;
}

.logout { margin-top: 30rpx; }
</style>
