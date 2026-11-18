<template>
  <view>
    <view v-if="!list.length && !loading" class="empty-state">
      <view class="empty-icon"><uni-icons type="doc" size="72" color="#c4c9d2" /></view>
      <text style="color:#4e5969">还没有会议纪要</text>
      <text class="muted">主持人结束会议时自动生成</text>
    </view>
    <view v-for="s in list" :key="s.id" class="card" @click="open(s)">
      <view class="row between">
        <text class="m-title">会议纪要 #{{ s.meetingId }}</text>
        <text class="muted" style="font-size:22rpx">{{ fmt(s.createTime) }}</text>
      </view>
      <view class="row" style="gap:12rpx;margin-top:14rpx">
        <text class="chip chip-green" v-if="s.isConfirmed">已确认</text>
        <text class="chip chip-gray" v-else>待确认</text>
        <text class="chip chip-blue" v-if="s.autoGenerate">自动生成</text>
      </view>
    </view>

    <!-- 纪要详情弹层 -->
    <view v-if="detail" class="mask" @click="detail = null">
      <scroll-view scroll-y class="sheet" @click.stop>
        <view class="sheet-head">
          <text class="sheet-title">会议纪要</text>
          <uni-icons type="close" size="36" color="#86909c" @click="detail = null" />
        </view>
        <view class="sheet-body">
          <template v-for="sec in sections" :key="sec.key">
            <text class="sec-title">{{ sec.name }}</text>
            <text class="sec-text">{{ detail.content[sec.key] || '（无）' }}</text>
          </template>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import { request } from '@/utils/request.js'

export default {
  data() {
    return {
      loading: true,
      list: [],
      detail: null,
      sections: [
        { key: 'background', name: '会议背景' },
        { key: 'discussion', name: '讨论要点' },
        { key: 'resolution', name: '决议结论' },
        { key: 'tasks', name: '会后任务' }
      ]
    }
  },
  onShow() { this.load() },
  methods: {
    async load() {
      try { this.list = await request({ url: '/api/mobile/summary/list' }) } catch (e) { this.list = [] }
      this.loading = false
    },
    open(s) {
      try {
        this.detail = { ...s, content: JSON.parse(s.content || '{}') }
      } catch (e) {
        this.detail = { ...s, content: { background: s.content } }
      }
    },
    fmt(t) { return t ? String(t).replace('T', ' ').substring(0, 10) : '' }
  }
}
</script>

<style scoped>
.m-title { font-size: 30rpx; font-weight: 600; }
.mask { position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 99; display: flex; align-items: flex-end; }
.sheet { background: #fff; width: 100%; border-radius: 28rpx 28rpx 0 0; max-height: 75vh; }
.sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 32rpx 36rpx; }
.sheet-title { font-size: 34rpx; font-weight: 600; }
.sheet-body { padding: 0 36rpx 50rpx; }
.sec-title { display: block; font-size: 30rpx; font-weight: 600; margin: 30rpx 0 12rpx; color: #00b578; }
.sec-text { display: block; font-size: 28rpx; color: #4e5969; line-height: 1.7; white-space: pre-wrap; }
</style>
