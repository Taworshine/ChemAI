<template>
  <view class="page">
    <!-- 1. 文件选择 -->
    <view class="group-title">选择文件</view>
    <view class="card pad">
      <!-- 未选择：两个入口 -->
      <view v-if="!file" class="pick-row">
        <view class="pick-tile" @click="pickDoc">
          <view class="pick-ico doc-ico"><text>文</text></view>
          <text class="pick-name">上传文档</text>
          <text class="pick-sub">PDF / Word</text>
        </view>
        <view class="pick-tile" @click="pickImage('camera')">
          <view class="pick-ico cam-ico"><uni-icons type="camera" size="44" color="#00B578" /></view>
          <text class="pick-name">拍照上传</text>
          <text class="pick-sub">试卷 / 板书</text>
        </view>
        <view class="pick-tile" @click="pickImage('album')">
          <view class="pick-ico alb-ico"><uni-icons type="image" size="44" color="#4E8BD8" /></view>
          <text class="pick-name">相册选择</text>
          <text class="pick-sub">JPG / PNG</text>
        </view>
      </view>

      <!-- 已选择：文件卡片 -->
      <view v-else class="file-card">
        <view class="file-badge" :style="{ background: badgeColor }">{{ badgeText }}</view>
        <view class="file-info">
          <text class="file-name">{{ file.name }}</text>
          <text class="file-meta">{{ file.sizeText }} · 来自{{ file.sourceText }}</text>
        </view>
        <text class="file-repick" @click="repick">重新选择</text>
      </view>
      <text v-if="!file" class="fmt-hint">支持 PDF、Word（.doc/.docx）、图片，单个文件不超过 50MB</text>
    </view>

    <!-- 2. 教案信息 -->
    <view class="group-title">教案信息</view>
    <view class="card pad">
      <view class="field">
        <text class="field-label">标题</text>
        <input class="field-input" v-model="title" placeholder="给这份教案起个名字" maxlength="60" />
      </view>
      <view class="field">
        <text class="field-label">类型</text>
        <view class="type-row">
          <view v-for="t in DOC_TYPES" :key="t.value" class="type-chip"
                :class="{ on: docType === t.value }" @click="docType = t.value">{{ t.label }}</view>
        </view>
      </view>
      <view class="field">
        <text class="field-label">所属学科</text>
        <view class="subj-chip" :style="{ background: subjectColor }">{{ subject || '未分配' }}</view>
      </view>
    </view>

    <!-- 3. 上传进度 -->
    <view v-if="uploading" class="card pad prog-card">
      <view class="row between">
        <text class="prog-title">正在上传…</text>
        <text class="prog-pct">{{ progress }}%</text>
      </view>
      <view class="prog-track"><view class="prog-fill" :style="{ width: progress + '%' }"></view></view>
      <text class="prog-cancel" @click="cancel">取消上传</text>
    </view>

    <!-- 底部上传按钮 -->
    <view class="footer">
      <button class="btn-primary upload-btn" :disabled="!canSubmit" :loading="uploading" @click="submit">
        {{ uploading ? '上传中' : '上传教案' }}
      </button>
    </view>
  </view>
</template>

<script>
import { getBaseUrl } from '@/utils/request.js'

const DOC_TYPES = [
  { label: '课件', value: 'COURSEWARE' },
  { label: '教案', value: 'LESSON_PLAN' },
  { label: '试卷', value: 'EXAM' },
  { label: '错题本', value: 'ERROR_BOOK' }
]
const SUBJECT_COLORS = {
  '化学': '#00B578', '数学': '#4A6FA5', '物理': '#7B61C9', '生物': '#3AA8AE',
  '语文': '#D97706', '英语': '#4E8BD8', '历史': '#B4534E', '地理': '#2E9E97', '政治': '#8C5E3C'
}
const MAX_SIZE = 50 * 1024 * 1024

export default {
  data() {
    return {
      DOC_TYPES,
      file: null,
      title: '',
      docType: 'LESSON_PLAN',
      subject: '',
      uploading: false,
      progress: 0,
      task: null
    }
  },
  computed: {
    canSubmit() { return !!this.file && !!this.title.trim() && !this.uploading },
    badgeText() {
      if (!this.file) return ''
      const e = this.file.ext
      if (e === '.pdf') return 'PDF'
      if (e === '.doc' || e === '.docx') return 'W'
      return '图'
    },
    badgeColor() {
      if (!this.file) return '#ccc'
      const e = this.file.ext
      if (e === '.pdf') return '#FA5151'
      if (e === '.doc' || e === '.docx') return '#4E8BD8'
      return '#00B578'
    },
    subjectColor() { return SUBJECT_COLORS[this.subject] || '#86909C' }
  },
  onLoad(options) {
    const info = uni.getStorageSync('userInfo') || {}
    this.subject = info.subject || ''
    if (options.mode === 'doc') this.pickDoc()
    else if (options.mode === 'camera') this.pickImage('camera')
    else if (options.mode === 'album') this.pickImage('album')
  },
  methods: {
    fmtSize(bytes) {
      if (!bytes && bytes !== 0) return ''
      if (bytes < 1024) return bytes + ' B'
      if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(0) + ' KB'
      return (bytes / 1024 / 1024).toFixed(1) + ' MB'
    },
    extOf(name) {
      const i = name.lastIndexOf('.')
      return i >= 0 ? name.slice(i).toLowerCase() : ''
    },
    baseName(name) {
      const i = name.lastIndexOf('.')
      return i >= 0 ? name.slice(0, i) : name
    },
    acceptFile(name, size, sourceText) {
      const ext = this.extOf(name)
      const ok = ['.pdf', '.doc', '.docx', '.png', '.jpg', '.jpeg'].includes(ext)
      if (!ok) {
        uni.showToast({ title: '仅支持 PDF / Word / 图片', icon: 'none' })
        return
      }
      if (size > MAX_SIZE) {
        uni.showToast({ title: '文件超过 50MB', icon: 'none' })
        return
      }
      this.file = { path: this._tmpPath, name, size, ext, sourceText }
      if (!this.title) this.title = this.baseName(name)
    },
    pickDoc() {
      uni.chooseFile({
        count: 1,
        extension: ['.pdf', '.doc', '.docx'],
        success: (res) => {
          const f = res.tempFiles[0]
          this._tmpPath = f.path
          this.acceptFile(f.name, f.size, '文档')
        },
        fail: () => {}
      })
    },
    pickImage(sourceType) {
      uni.chooseImage({
        count: 1,
        sourceType: [sourceType],
        success: (res) => {
          const path = res.tempFilePaths[0]
          const name = (res.tempFiles && res.tempFiles[0] && res.tempFiles[0].name) || path.slice(path.lastIndexOf('/') + 1)
          this._tmpPath = path
          this.acceptFile(name, res.tempFiles && res.tempFiles[0] ? res.tempFiles[0].size : 0, sourceType === 'camera' ? '相机' : '相册')
        },
        fail: () => {}
      })
    },
    repick() {
      uni.showActionSheet({
        itemList: ['上传文档（PDF/Word）', '拍照', '从相册选择'],
        success: (res) => {
          if (res.tapIndex === 0) this.pickDoc()
          else if (res.tapIndex === 1) this.pickImage('camera')
          else this.pickImage('album')
        }
      })
    },
    submit() {
      if (!this.canSubmit) return
      this.uploading = true
      this.progress = 0
      const task = uni.uploadFile({
        url: getBaseUrl() + '/api/document/upload',
        filePath: this.file.path,
        name: 'file',
        header: { satoken: uni.getStorageSync('satoken') },
        formData: { docType: this.docType, title: this.title.trim() },
        success: (up) => {
          this.uploading = false
          let body = {}
          try { body = JSON.parse(up.data) } catch (e) { /* 非 JSON */ }
          if (up.statusCode >= 200 && up.statusCode < 300 && body.code === 200) {
            uni.showToast({ title: '上传成功', icon: 'success' })
            setTimeout(() => {
              uni.redirectTo({ url: '/pages/plan/detail?id=' + body.data.id })
            }, 600)
          } else {
            uni.showToast({ title: body.msg || '上传失败', icon: 'none' })
          }
        },
        fail: () => {
          this.uploading = false
          uni.showToast({ title: '网络异常，上传失败', icon: 'none' })
        }
      })
      task.onProgressUpdate((r) => { this.progress = r.progress })
      this.task = task
    },
    cancel() {
      if (this.task) this.task.abort()
      this.uploading = false
      this.progress = 0
    }
  }
}
</script>

<style scoped>
.page { padding-bottom: 180rpx; }
.pad { padding: 30rpx; }

/* 选择入口 */
.pick-row { display: flex; gap: 20rpx; }
.pick-tile {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 8rpx;
  padding: 36rpx 0 30rpx; border-radius: 16rpx; background: #f7f8fa;
  border: 1rpx solid #eef0f3;
}
.pick-tile:active { background: #eef7f2; }
.pick-ico {
  width: 84rpx; height: 84rpx; border-radius: 22rpx;
  display: flex; align-items: center; justify-content: center; margin-bottom: 6rpx;
}
.doc-ico { background: #fdeceb; }
.doc-ico text { color: #fa5151; font-size: 36rpx; font-weight: 700; }
.cam-ico { background: #e2f7ee; }
.alb-ico { background: #e8f1fc; }
.pick-name { font-size: 28rpx; color: #191919; font-weight: 600; }
.pick-sub { font-size: 22rpx; color: #86909c; }
.fmt-hint { display: block; margin-top: 22rpx; font-size: 22rpx; color: #a9aeb8; }

/* 已选文件卡片 */
.file-card {
  display: flex; align-items: center; gap: 22rpx;
  padding: 26rpx; border-radius: 16rpx; background: #f7f8fa;
}
.file-badge {
  width: 80rpx; height: 80rpx; border-radius: 18rpx; flex-shrink: 0;
  color: #fff; font-size: 28rpx; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.file-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 6rpx; }
.file-name { font-size: 30rpx; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-meta { font-size: 22rpx; color: #86909c; }
.file-repick { font-size: 26rpx; color: #00b578; flex-shrink: 0; padding: 8rpx 0 8rpx 16rpx; }

/* 信息表单 */
.field { padding: 22rpx 0; border-bottom: 1rpx solid #f5f6f8; }
.field:last-child { border-bottom: none; }
.field-label { display: block; font-size: 24rpx; color: #86909c; margin-bottom: 16rpx; }
.field-input {
  font-size: 32rpx; color: #191919; background: #f7f8fa;
  border-radius: 14rpx; padding: 20rpx 24rpx;
}
.type-row { display: flex; gap: 18rpx; flex-wrap: wrap; }
.type-chip {
  padding: 14rpx 44rpx; border-radius: 999rpx; background: #f2f3f5;
  font-size: 28rpx; color: #4e5969; border: 1rpx solid transparent;
}
.type-chip.on { background: #e2f7ee; color: #00b578; font-weight: 600; border-color: #00b578; }
.subj-chip {
  display: inline-block; color: #fff; font-size: 26rpx; font-weight: 600;
  padding: 10rpx 34rpx; border-radius: 999rpx;
}

/* 进度 */
.prog-card { margin-top: 24rpx; }
.prog-title { font-size: 28rpx; color: #191919; font-weight: 600; }
.prog-pct { font-size: 28rpx; color: #00b578; font-weight: 600; }
.prog-track { height: 12rpx; border-radius: 999rpx; background: #f0f1f3; margin-top: 20rpx; overflow: hidden; }
.prog-fill { height: 100%; border-radius: 999rpx; background: #00b578; transition: width 0.2s; }
.prog-cancel { display: block; text-align: center; margin-top: 22rpx; font-size: 26rpx; color: #86909c; }

/* 底部按钮 */
.footer {
  position: fixed; left: 0; right: 0; bottom: 0; padding: 24rpx 32rpx calc(24rpx + env(safe-area-inset-bottom));
  background: #fff; border-top: 1rpx solid #f0f1f3;
}
.upload-btn { border-radius: 999rpx; }
.upload-btn[disabled] { background: #c9cdd4; color: #fff; }
</style>
