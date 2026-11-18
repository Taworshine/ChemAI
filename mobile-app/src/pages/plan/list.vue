<template>
  <view>
    <!-- 顶部：标题 + 上传 -->
    <view class="head">
      <text class="head-title">我的教案</text>
      <text class="muted">{{ list.length }} 份</text>
      <view class="head-add" @click="showSheet = true">
        <uni-icons type="plus" size="36" color="#fff" />
      </view>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading">
      <view class="skeleton"><view class="skeleton-line" style="width:55%"></view><view class="skeleton-line" style="width:35%"></view></view>
      <view class="skeleton"><view class="skeleton-line" style="width:45%"></view><view class="skeleton-line" style="width:60%"></view></view>
    </view>

    <!-- 空态 -->
    <view v-else-if="!list.length" class="empty-state">
      <view class="empty-icon"><uni-icons type="book" size="72" color="#c4c9d2" /></view>
      <text style="color:#4e5969">还没有教案</text>
      <text class="muted">点右上角 ＋ 上传 PDF / Word / 照片</text>
      <view class="btn-outline empty-btn" @click="showSheet = true">立即上传</view>
    </view>

    <!-- 教案卡流 -->
    <view v-else>
      <view v-for="doc in list" :key="doc.id" class="card plan-card" @click="openDetail(doc)">
        <view class="row">
          <view class="fmt-badge" :style="{ background: badgeColor(doc.fileExt) }">{{ badgeText(doc.fileExt) }}</view>
          <view class="plan-head">
            <text class="plan-title">{{ doc.title }}</text>
            <view class="row" style="gap:12rpx;margin-top:10rpx">
              <text class="type-chip">{{ typeName(doc.docType) }}</text>
              <text class="subj-chip" :style="{ color: colorOf(doc.subject), background: colorOf(doc.subject) + '18' }">{{ doc.subject || '未标学科' }}</text>
            </view>
          </view>
        </view>
        <view class="plan-foot">
          <view class="row" style="gap:14rpx">
            <view class="status-chip" :class="'p' + (doc.parseStatus || 0)">
              <view class="status-dot" :class="'p' + (doc.parseStatus || 0)"></view>
              {{ ['未解析', '解析中', '已解析', '解析失败'][doc.parseStatus || 0] }}
            </view>
            <text class="muted">{{ fmtDate(doc.createTime) }}</text>
          </view>
          <uni-icons type="right" size="30" color="#c4c9d2" />
        </view>
      </view>
    </view>

    <!-- 上传入口弹层 -->
    <view v-if="showSheet" class="mask" @click="showSheet = false">
      <view class="sheet" @click.stop>
        <view class="sheet-title">上传教案</view>
        <view class="sheet-opt" @click="goUpload('doc')">
          <view class="sheet-ico" style="background:#fdeceb"><text style="color:#fa5151;font-weight:700">文</text></view>
          <view class="sheet-opt-body">
            <text class="sheet-opt-name">上传文档</text>
            <text class="sheet-opt-sub">PDF、Word（.doc / .docx）</text>
          </view>
          <uni-icons type="right" size="32" color="#c4c9d2" />
        </view>
        <view class="sheet-opt" @click="goUpload('camera')">
          <view class="sheet-ico" style="background:#e2f7ee"><uni-icons type="camera" size="40" color="#00B578" /></view>
          <view class="sheet-opt-body">
            <text class="sheet-opt-name">拍照上传</text>
            <text class="sheet-opt-sub">试卷、板书、纸质教案</text>
          </view>
          <uni-icons type="right" size="32" color="#c4c9d2" />
        </view>
        <view class="sheet-opt" @click="goUpload('album')">
          <view class="sheet-ico" style="background:#e8f1fc"><uni-icons type="image" size="40" color="#4E8BD8" /></view>
          <view class="sheet-opt-body">
            <text class="sheet-opt-name">从相册选择</text>
            <text class="sheet-opt-sub">JPG / PNG 图片</text>
          </view>
          <uni-icons type="right" size="32" color="#c4c9d2" />
        </view>
        <view class="sheet-cancel" @click="showSheet = false">取消</view>
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
const TYPE_NAMES = { COURSEWARE: '课件', LESSON_PLAN: '教案', EXAM: '试卷', ERROR_BOOK: '错题本' }

export default {
  data() { return { loading: true, list: [], showSheet: false } },
  onShow() { this.load() },
  onPullDownRefresh() { this.load().finally(() => uni.stopPullDownRefresh()) },
  methods: {
    async load() {
      this.loading = true
      try { this.list = await request({ url: '/api/mobile/document/library' }) } catch (e) { this.list = [] }
      this.loading = false
    },
    colorOf(s) { return SUBJECT_COLORS[s] || '#86909C' },
    typeName(t) { return TYPE_NAMES[t] || '资料' },
    badgeText(ext) {
      if (ext === '.pdf') return 'PDF'
      if (ext === '.doc' || ext === '.docx') return 'W'
      if (ext) return '图'
      return '文'
    },
    badgeColor(ext) {
      if (ext === '.pdf') return '#FA5151'
      if (ext === '.doc' || ext === '.docx') return '#4E8BD8'
      if (ext) return '#00B578'
      return '#86909C'
    },
    fmtDate(t) {
      if (!t) return ''
      const d = new Date(String(t).replace(' ', 'T'))
      return `${d.getMonth() + 1}月${d.getDate()}日上传`
    },
    openDetail(doc) { uni.navigateTo({ url: '/pages/plan/detail?id=' + doc.id }) },
    goUpload(mode) {
      this.showSheet = false
      uni.navigateTo({ url: '/pages/plan/upload?mode=' + mode })
    }
  }
}
</script>

<style scoped>
.head {
  display: flex; align-items: baseline; gap: 16rpx;
  padding: 32rpx 40rpx 10rpx;
}
.head-title { font-size: 40rpx; font-weight: 600; color: #191919; flex: 1; }
.head-add {
  width: 72rpx; height: 72rpx; border-radius: 50%;
  background: #00b578; display: flex; align-items: center; justify-content: center;
  align-self: center;
}
.head-add:active { background: #009b68; }
.empty-btn { margin-top: 30rpx; padding: 16rpx 60rpx; border-radius: 999rpx; font-size: 28rpx; }

.plan-card { padding: 30rpx; }
.plan-head { flex: 1; min-width: 0; }
.fmt-badge {
  width: 76rpx; height: 76rpx; border-radius: 18rpx; flex-shrink: 0;
  color: #fff; font-size: 26rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  margin-right: 22rpx;
}
.plan-title { font-size: 32rpx; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.type-chip { font-size: 22rpx; color: #4e5969; background: #f2f3f5; padding: 4rpx 16rpx; border-radius: 8rpx; }
.subj-chip { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 8rpx; font-weight: 500; }
.plan-foot {
  margin-top: 22rpx; padding-top: 20rpx; border-top: 1rpx solid #f5f6f8;
  display: flex; align-items: center; justify-content: space-between;
}
.status-chip {
  display: inline-flex; align-items: center; gap: 10rpx;
  font-size: 24rpx; padding: 6rpx 18rpx; border-radius: 8rpx; background: #f2f3f5; color: #86909c;
}
.status-chip.p2 { background: #e2f7ee; color: #00b578; }
.status-chip.p1 { background: #fdf3e7; color: #ed7b2f; }
.status-chip.p3 { background: #fdeceb; color: #fa5151; }
.status-dot { width: 10rpx; height: 10rpx; border-radius: 50%; background: currentColor; }

/* 底部弹层 */
.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 99;
  display: flex; align-items: flex-end;
}
.sheet {
  background: #fff; width: 100%; border-radius: 28rpx 28rpx 0 0;
  padding: 36rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
}
.sheet-title { font-size: 30rpx; font-weight: 600; color: #191919; text-align: center; margin-bottom: 16rpx; }
.sheet-opt {
  display: flex; align-items: center; gap: 24rpx;
  padding: 28rpx 8rpx; border-bottom: 1rpx solid #f5f6f8;
}
.sheet-ico {
  width: 84rpx; height: 84rpx; border-radius: 22rpx;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.sheet-opt-body { flex: 1; display: flex; flex-direction: column; gap: 6rpx; }
.sheet-opt-name { font-size: 30rpx; font-weight: 600; color: #191919; }
.sheet-opt-sub { font-size: 24rpx; color: #86909c; }
.sheet-cancel {
  margin-top: 20rpx; text-align: center; padding: 24rpx 0;
  font-size: 30rpx; color: #4e5969; background: #f7f8fa; border-radius: 16rpx;
}
</style>
