<template>
  <view class="page">
    <view class="card code-card">
      <view class="code-title">输入 6 位会议号</view>
      <view class="code-area" @click="focusCode">
        <input ref="codeInput" class="code-input-real" v-model="meetingNo" maxlength="6" type="number" confirm-type="go" @confirm="join" />
        <view class="code-dots">
          <view v-for="i in 6" :key="i" class="dot" :class="{ filled: (meetingNo || '').length >= i, next: (meetingNo || '').length === i - 1 }">
            <text v-if="(meetingNo || '')[i - 1]" class="dot-num">{{ meetingNo[i - 1] }}</text>
          </view>
        </view>
      </view>
      <view class="muted code-tip">或扫描主持人展示的二维码加入</view>
    </view>

    <view class="btn-primary join-btn press" @click="join">加入会议</view>

    <view class="card hint-card">
      <view class="hint-row">
        <view class="hint-ico"><uni-icons type="info" size="32" color="#00b578" /></view>
        <text class="hint-text">会议由主持人在 APP 内创建，会议号同时编码在二维码里</text>
      </view>
      <view class="hint-row">
        <view class="hint-ico"><uni-icons type="close" size="32" color="#fa5151" /></view>
        <text class="hint-text">会议被锁定时无法加入，请联系主持人</text>
      </view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

export default {
  data() {
    return { meetingNo: '' }
  },
  onLoad(options) {
    if (options.no) this.meetingNo = options.no
  },
  methods: {
    focusCode() {
      this.$refs.codeInput.focus()
    },
    async join() {
      if (!/^\d{6}$/.test(this.meetingNo)) {
        uni.showToast({ title: '请输入 6 位数字会议号', icon: 'none' })
        return
      }
      try {
        const data = await request({
          url: '/api/mobile/meeting/join',
          method: 'POST',
          data: { meetingNo: this.meetingNo }
        })
        uni.redirectTo({ url: '/pages/meeting/room?id=' + data.meeting.id })
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style scoped>
.page { padding-top: 30rpx; }
.code-card { text-align: center; padding: 56rpx 36rpx 48rpx; }
.code-title { font-size: 28rpx; font-weight: 600; color: #4e5969; }
.code-input-real { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.01; z-index: 2; }
.code-dots { display: flex; justify-content: center; gap: 18rpx; margin: 40rpx 0 30rpx; position: relative; }
.dot {
  width: 88rpx; height: 108rpx; border-radius: 20rpx; background: #f4f5f7;
  border: 2rpx solid transparent; border-bottom: 6rpx solid #e3e5e9;
  transition: all 0.15s;
  display: flex; align-items: center; justify-content: center;
}
.dot.filled { background: #e2f7ee; border-color: #b4e9cb; border-bottom-color: #00b578; }
.dot-num { font-size: 52rpx; font-weight: 700; color: #00b578; font-family: 'Courier New', monospace; }
.code-area { position: relative; }
.code-tip { margin-top: 6rpx; }
.join-btn { margin: 36rpx 32rpx; }
.hint-card { padding: 30rpx 32rpx; background: #f7f8fa; box-shadow: none; border-style: dashed; }
.hint-row { display: flex; align-items: flex-start; padding: 10rpx 0; gap: 16rpx; }
.hint-ico { margin-top: 2rpx; flex-shrink: 0; }
.hint-text { flex: 1; font-size: 24rpx; color: #4e5969; line-height: 1.6; }
</style>
