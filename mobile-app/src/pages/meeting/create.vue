<template>
  <view>
    <view class="card form-card">
      <view class="field">
        <text class="field-label">会议主题</text>
        <input class="input" v-model="title" placeholder="例如：高三化学教研组：氧化还原教案评审" placeholder-class="ph" />
      </view>

      <view class="field">
        <text class="field-label">学科</text>
        <view class="chip-group">
          <view v-for="s in subjects" :key="s" class="pick-chip press" :class="{ active: subject === s }" @click="subject = s">
            {{ s }}
          </view>
        </view>
      </view>

      <view class="field">
        <text class="field-label">议程</text>
        <view class="agenda-list">
          <view v-for="a in agendas" :key="a.key" class="agenda-item press" :class="{ active: agenda === a.key }" @click="agenda = a.key">
            <view class="agenda-icon" :class="{ active: agenda === a.key }">
              <uni-icons :type="a.icon" size="40" :color="agenda === a.key ? '#00b578' : '#86909c'" />
            </view>
            <view class="agenda-text">
              <text class="agenda-name" :class="{ active: agenda === a.key }">{{ a.name }}</text>
              <text class="agenda-desc">{{ a.desc }}</text>
            </view>
            <view class="agenda-check" v-if="agenda === a.key"><uni-icons type="check" size="24" color="#fff" /></view>
            <view class="agenda-ring" v-else></view>
          </view>
        </view>
      </view>

      <view class="field" style="margin-bottom:0">
        <text class="field-label">邀请参会教师（{{ selected.length }} 人）</text>
        <view v-if="teachers.length" class="chip-group">
          <view v-for="t in teachers" :key="t.userId" class="pick-chip person press" :class="{ active: selected.includes(t.userId) }" @click="toggleTeacher(t.userId)">
            <uni-icons v-if="selected.includes(t.userId)" type="check" size="22" color="#fff" />
            <text style="margin-left:6rpx">{{ t.realName }}</text>
          </view>
        </view>
        <text v-else class="muted">暂无其他教师</text>
      </view>
    </view>

    <view class="footer">
      <button class="btn-primary submit press" :disabled="!title.trim()" @click="create">创建会议</button>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

export default {
  data() {
    return {
      title: '',
      subject: '化学',
      agenda: 'LESSON_REVIEW',
      subjects: ['化学', '数学', '物理', '生物', '语文', '英语', '历史', '地理', '政治'],
      agendas: [
        { key: 'LESSON_REVIEW', name: '教案评审', icon: 'doc', desc: '上传教案 · 圈画批注 · 量化评分' },
        { key: 'COLLECTIVE_PREP', name: '集体备课', icon: 'edit', desc: '共同研讨教学设计' },
        { key: 'PROPOSITION', name: '命题研讨', icon: 'list', desc: '查重 · 组卷 · 审题投票' },
        { key: 'PAPER_ANALYSIS', name: '试卷分析', icon: 'chart', desc: '考试数据与学情复盘' }
      ],
      teachers: [],
      selected: []
    }
  },
  onLoad() {
    const info = uni.getStorageSync('userInfo') || {}
    if (info.subject) this.subject = info.subject
    this.loadTeachers()
  },
  methods: {
    async loadTeachers() {
      try {
        this.teachers = await request({ url: '/api/mobile/meeting/teachers' })
      } catch (e) { this.teachers = [] }
    },
    toggleTeacher(id) {
      const i = this.selected.indexOf(id)
      i >= 0 ? this.selected.splice(i, 1) : this.selected.push(id)
    },
    async create() {
      if (!this.title.trim()) {
        uni.showToast({ title: '请填写会议主题', icon: 'none' })
        return
      }
      try {
        const data = await request({
          url: '/api/mobile/meeting/create',
          method: 'POST',
          data: { title: this.title, subject: this.subject, agenda: this.agenda, memberIds: this.selected }
        })
        uni.showModal({
          title: '会议已创建',
          content: '会议号：' + data.meeting.meetingNo + '\n已生成二维码，成员可扫码或输码加入',
          showCancel: false,
          success: () => uni.redirectTo({ url: '/pages/meeting/room?id=' + data.meeting.id })
        })
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style scoped>
.form-card { margin-top: 24rpx; }
.chip-group { display: flex; flex-wrap: wrap; gap: 18rpx; }
.pick-chip {
  padding: 14rpx 36rpx; border-radius: 999rpx; background: #f2f3f5;
  font-size: 26rpx; color: #4e5969; display: inline-flex; align-items: center;
  transition: all 0.15s;
}
.pick-chip.active { background: #e2f7ee; color: #00b578; font-weight: 600; box-shadow: inset 0 0 0 2rpx #00b578; }
.pick-chip.person.active { background: #00b578; color: #fff; box-shadow: none; }

.agenda-list { display: flex; flex-direction: column; gap: 20rpx; }
.agenda-item {
  display: flex; align-items: center; background: #f7f8fa;
  border-radius: 20rpx; padding: 26rpx 28rpx;
  border: 2rpx solid transparent;
  transition: all 0.15s;
}
.agenda-item.active { background: #e2f7ee; border-color: #00b578; }
.agenda-icon {
  width: 84rpx; height: 84rpx; border-radius: 22rpx; flex-shrink: 0;
  background: #eceef1; margin-right: 24rpx;
  display: flex; align-items: center; justify-content: center;
  transition: background 0.15s;
}
.agenda-icon.active { background: #ffffff; }
.agenda-text { flex: 1; display: flex; flex-direction: column; }
.agenda-name { font-size: 30rpx; font-weight: 600; color: #191919; }
.agenda-name.active { color: #00b578; }
.agenda-desc { font-size: 22rpx; color: #86909c; margin-top: 6rpx; }
.agenda-check {
  width: 44rpx; height: 44rpx; border-radius: 50%;
  background: #00b578;
  display: flex; align-items: center; justify-content: center;
}
.agenda-ring {
  width: 40rpx; height: 40rpx; border-radius: 50%;
  border: 2rpx solid #d5d9e0;
}

/* 底部固定提交 */
.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1rpx solid #f2f3f5;
}
.submit { border-radius: 999rpx; }
.submit[disabled] { background: #c9cdd4; color: #fff; }
</style>
