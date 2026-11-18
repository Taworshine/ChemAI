<template>
  <view v-if="meta.id">
    <!-- 教案信息 -->
    <view class="card">
      <view class="row">
        <view v-if="meta.fileExt" class="fmt-badge" :style="{ background: badgeColor }">{{ badgeText }}</view>
        <view style="flex:1;min-width:0">
          <text class="p-title">{{ meta.title }}</text>
          <view class="row" style="gap:14rpx;margin-top:14rpx">
            <view class="status-chip" :class="'p' + (meta.parseStatus || 0)">
              <view class="status-dot" :class="'p' + (meta.parseStatus || 0)"></view>
              {{ ['未解析', '解析中', '已解析', '解析失败'][meta.parseStatus || 0] }}
            </view>
            <text class="muted" v-if="meta.fileExt">{{ meta.fileExt.slice(1).toUpperCase() }} 文件</text>
          </view>
        </view>
      </view>
      <!-- 教案预览图 -->
      <image v-if="docImagePath" :src="docImagePath" class="doc-img" mode="widthFix" />
      <!-- PDF/Word 提示 -->
      <view v-else-if="meta.isPdf || meta.isWord" class="doc-note">
        <uni-icons type="doc" size="36" color="#86909c" />
        <text>{{ meta.isPdf ? 'PDF 已入库' : 'Word 文档已入库' }}，点下方「智能解析」提取知识点、题型与思维路径</text>
      </view>
    </view>

    <!-- 操作区：一行两个 -->
    <view class="ops">
      <view class="btn-outline op" @click="parse" v-if="meta.parseStatus !== 1">
        <uni-icons type="gear" size="34" color="#00b578" />
        <text>智能解析</text>
      </view>
      <view class="btn-outline op" @click="goReview" v-if="meta.parseStatus === 2">
        <uni-icons type="chart" size="34" color="#00b578" />
        <text>教案体检</text>
      </view>
    </view>

    <!-- 关联题目 -->
    <view class="group-title">关联题目（{{ links.wrongs.length + links.exercises.length }}）</view>
    <view class="card" style="padding:0;overflow:hidden">
      <view v-if="!links.wrongs.length && !links.exercises.length" class="link-empty">
        <text class="muted">还没有关联题目</text>
        <text class="muted" style="font-size:22rpx">关联错题和习题，教案更有针对性</text>
      </view>
      <view v-for="l in links.wrongs" :key="'w' + l.linkId" class="cell" @longpress="unlink(l.linkId)">
        <text class="tag wrong">错题</text>
        <view class="cell-body">
          <text class="cell-name link-name">{{ l.item.content }}</text>
          <text class="cell-desc">{{ l.item.conceptName }} · {{ l.item.wrongReason }}</text>
        </view>
      </view>
      <view v-for="l in links.exercises" :key="'e' + l.linkId" class="cell" @longpress="unlink(l.linkId)">
        <text class="tag exercise">习题</text>
        <view class="cell-body">
          <text class="cell-name link-name">{{ l.item.content }}</text>
          <text class="cell-desc">{{ l.item.conceptName }} · {{ l.item.difficulty }}</text>
        </view>
      </view>
      <view class="cell" @click="showAdd = true">
        <uni-icons class="cell-icon" type="plus" size="36" color="#00b578" />
        <view class="cell-body"><text class="cell-name" style="color:#00b578">添加关联</text></view>
      </view>
    </view>

    <!-- 添加关联弹层 -->
    <view v-if="showAdd" class="mask" @click="showAdd = false">
      <view class="sheet" @click.stop>
        <view class="sheet-head">
          <text class="sheet-title">从题库选择</text>
          <uni-icons type="close" size="36" color="#86909c" @click="showAdd = false" />
        </view>
        <view class="sheet-seg">
          <view class="seg-item" :class="{ on: pickTab === 'WRONG' }" @click="pickTab = 'WRONG'">错题</view>
          <view class="seg-item" :class="{ on: pickTab === 'EXERCISE' }" @click="pickTab = 'EXERCISE'">习题</view>
        </view>
        <scroll-view scroll-y class="sheet-list">
          <view v-if="pickTab === 'WRONG'">
            <view v-for="w in wrongPool" :key="w.id" class="sheet-item" @click="link('WRONG', w.id)">
              <text class="sheet-text">{{ w.content }}</text>
              <text class="muted">{{ w.conceptName }}</text>
            </view>
            <view v-if="!wrongPool.length" class="muted" style="text-align:center;padding:40rpx 0">题库还没有错题</view>
          </view>
          <view v-else>
            <view v-for="q in exercisePool" :key="q.id" class="sheet-item" @click="link('EXERCISE', q.id)">
              <text class="sheet-text">{{ q.content }}</text>
              <text class="muted">{{ q.conceptName }}</text>
            </view>
            <view v-if="!exercisePool.length" class="muted" style="text-align:center;padding:40rpx 0">题库还没有习题</view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { request, getBaseUrl } from '@/utils/request.js'

export default {
  data() {
    return {
      docId: null,
      meta: {},
      docImagePath: '',
      links: { wrongs: [], exercises: [] },
      showAdd: false,
      pickTab: 'WRONG',
      wrongPool: [],
      exercisePool: []
    }
  },
  onLoad(options) { this.docId = options.id },
  onShow() { this.load() },
  methods: {
    async load() {
      try {
        this.meta = await request({ url: '/api/mobile/document/' + this.docId + '/meta' })
        if (this.meta.isImage) {
          const res = await uni.downloadFile({
            url: getBaseUrl() + '/api/mobile/document/' + this.docId + '/file',
            header: { satoken: uni.getStorageSync('satoken') }
          })
          this.docImagePath = res.tempFilePath
        }
        this.links = await request({ url: '/api/mobile/plan-link/list?planId=' + this.docId })
      } catch (e) { /* 已提示 */ }
    },
    async parse() {
      uni.showLoading({ title: '智能解析中' })
      try {
        await request({ url: '/api/document/parse/' + this.docId, method: 'POST' })
        uni.hideLoading()
        uni.showToast({ title: '解析完成', icon: 'success' })
        this.load()
      } catch (e) { uni.hideLoading() }
    },
    goReview() { uni.navigateTo({ url: '/pages/review/analyze?docId=' + this.docId }) },
    async link(type, targetId) {
      try {
        await request({
          url: '/api/mobile/plan-link/add', method: 'POST',
          data: { planId: Number(this.docId), targetType: type, targetId }
        })
        this.showAdd = false
        uni.showToast({ title: '已关联', icon: 'success' })
        this.load()
      } catch (e) { /* 已提示 */ }
    },
    unlink(linkId) {
      uni.showModal({
        title: '取消关联？',
        success: async (res) => {
          if (res.confirm) {
            await request({ url: '/api/mobile/plan-link/' + linkId, method: 'DELETE' })
            this.load()
          }
        }
      })
    }
  },
  computed: {
    badgeText() {
      const e = this.meta.fileExt
      if (e === '.pdf') return 'PDF'
      if (e === '.doc' || e === '.docx') return 'W'
      return '图'
    },
    badgeColor() {
      const e = this.meta.fileExt
      if (e === '.pdf') return '#FA5151'
      if (e === '.doc' || e === '.docx') return '#4E8BD8'
      return '#00B578'
    }
  },
  watch: {
    showAdd(open) {
      if (open && !this.wrongPool.length && !this.exercisePool.length) {
        request({ url: '/api/mobile/wrong/list' }).then(l => (this.wrongPool = l)).catch(() => {})
        request({ url: '/api/mobile/exercise/list' }).then(l => (this.exercisePool = l)).catch(() => {})
      }
    }
  }
}
</script>

<style scoped>
.p-title { font-size: 36rpx; font-weight: 600; }
.fmt-badge {
  width: 72rpx; height: 72rpx; border-radius: 18rpx; flex-shrink: 0;
  color: #fff; font-size: 26rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  margin-right: 22rpx;
}
.doc-note {
  margin-top: 24rpx; padding: 24rpx; border-radius: 14rpx; background: #f7f8fa;
  display: flex; align-items: center; gap: 16rpx;
  font-size: 24rpx; color: #86909c; line-height: 1.5;
}
.status-chip {
  display: inline-flex; align-items: center; gap: 10rpx;
  font-size: 24rpx; padding: 6rpx 18rpx; border-radius: 8rpx; background: #f2f3f5; color: #86909c;
}
.status-chip.p2 { background: #e2f7ee; color: #00b578; }
.status-chip.p1 { background: #fdf3e7; color: #ed7b2f; }
.status-chip.p3 { background: #fdeceb; color: #fa5151; }
.status-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: currentColor; }
.doc-img { width: 100%; border-radius: 12rpx; margin-top: 24rpx; display: block; }

.ops { display: flex; gap: 24rpx; padding: 4rpx 24rpx; }
.op { flex: 1; display: flex; align-items: center; justify-content: center; gap: 10rpx; }

.link-empty { display: flex; flex-direction: column; align-items: center; gap: 8rpx; padding: 50rpx 0; }
.link-name { max-width: 480rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tag {
  font-size: 20rpx; padding: 4rpx 12rpx; border-radius: 6rpx;
  margin-right: 18rpx; flex-shrink: 0;
}
.tag.wrong { background: #fdeceb; color: #fa5151; }
.tag.exercise { background: #e2f7ee; color: #00b578; }

.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 99;
  display: flex; align-items: flex-end;
}
.sheet {
  background: #fff; width: 100%; border-radius: 28rpx 28rpx 0 0;
  max-height: 70vh; display: flex; flex-direction: column;
}
.sheet-head { display: flex; align-items: center; justify-content: space-between; padding: 32rpx 36rpx 10rpx; }
.sheet-title { font-size: 32rpx; font-weight: 600; }
.sheet-seg { display: flex; gap: 16rpx; padding: 16rpx 36rpx; }
.seg-item {
  padding: 12rpx 40rpx; border-radius: 999rpx; background: #f2f3f5;
  font-size: 26rpx; color: #4e5969;
}
.seg-item.on { background: #e2f7ee; color: #00b578; font-weight: 600; }
.sheet-list { padding: 0 36rpx 40rpx; box-sizing: border-box; }
.sheet-item {
  padding: 24rpx 0; border-bottom: 1rpx solid #f5f6f8;
  display: flex; flex-direction: column; gap: 6rpx;
}
.sheet-text { font-size: 28rpx; color: #191919; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
