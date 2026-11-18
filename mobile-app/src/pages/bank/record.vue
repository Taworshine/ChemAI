<template>
  <view>
    <!-- 第一步：拍照 -->
    <view v-if="step === 1" class="step">
      <view class="shoot-area" @click="takePhoto">
        <image v-if="imagePath" :src="imagePath" class="shoot-img" mode="aspectFit" />
        <view v-else class="shoot-hint">
          <uni-icons type="camera" size="80" color="#c4c9d2" />
          <text class="muted">点击拍摄或从相册选择错题</text>
        </view>
      </view>
      <view class="btn-primary submit" :class="{ disabled: !imagePath }" @click="toStep2">下一步：AI 分析</view>
    </view>

    <!-- 第二步：AI 分析结果确认 -->
    <view v-else class="step">
      <view class="card">
        <view class="row" style="gap:12rpx;margin-bottom:10rpx">
          <image :src="imagePath" class="thumb" mode="aspectFill" />
          <text class="step-title">识别结果确认</text>
        </view>
        <text class="muted" style="font-size:22rpx;display:block;margin-bottom:20rpx">AI 已分析，请核对并修改：</text>

        <text class="field-label">题干</text>
        <textarea class="input ta" v-model="form.content" placeholder="题干内容" placeholder-class="ph" />

        <text class="field-label">考查知识点</text>
        <input class="input" v-model="form.conceptName" placeholder="如：卤代烃的消去反应" placeholder-class="ph" />

        <text class="field-label">错因</text>
        <view class="reason-group">
          <view v-for="r in reasons" :key="r" class="reason-chip" :class="{ on: form.wrongReason === r }" @click="form.wrongReason = r">{{ r }}</view>
        </view>

        <text class="field-label">错误人数</text>
        <view class="row count-row">
          <view class="count-btn" @click="form.wrongCount > 1 && form.wrongCount--">−</view>
          <text class="count-num">{{ form.wrongCount }}</text>
          <view class="count-btn" @click="form.wrongCount++">＋</view>
        </view>
      </view>
      <view class="btn-primary submit" @click="submit">确认入库</view>
    </view>
  </view>
</template>

<script>
import { request, getBaseUrl } from '@/utils/request.js'

const REASONS = ['条件混淆', '概念不清', '计算错误', '审题偏差', '表达不规范']

export default {
  data() {
    return {
      step: 1,
      imagePath: '',
      serverPath: '',
      form: { content: '', conceptName: '', wrongReason: '', wrongCount: 1, subject: '化学' },
      reasons: REASONS
    }
  },
  onLoad() {
    const info = uni.getStorageSync('userInfo') || {}
    if (info.subject) this.form.subject = info.subject
  },
  methods: {
    takePhoto() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          this.imagePath = res.tempFilePaths[0]
          uni.showLoading({ title: '上传中' })
          try {
            const up = await uni.uploadFile({
              url: getBaseUrl() + '/api/document/upload',
              filePath: this.imagePath,
              name: 'file',
              header: { satoken: uni.getStorageSync('satoken') },
              formData: { docType: 'COURSEWARE' }
            })
            const doc = JSON.parse(up.data).data
            this.serverPath = doc.filePath || ''
            uni.hideLoading()
          } catch (e) { uni.hideLoading() }
        }
      })
    },
    toStep2() {
      if (!this.imagePath) return
      // MVP：AI 识别在服务端解析链路完善后接入，这里先由老师填写/核对
      this.step = 2
    },
    async submit() {
      if (!this.form.content.trim()) {
        uni.showToast({ title: '请填写题干', icon: 'none' })
        return
      }
      try {
        await request({
          url: '/api/mobile/wrong/add',
          method: 'POST',
          data: {
            subject: this.form.subject,
            conceptName: this.form.conceptName.trim(),
            wrongReason: this.form.wrongReason,
            content: this.form.content.trim(),
            imagePath: this.serverPath,
            wrongCount: this.form.wrongCount
          }
        })
        uni.showToast({ title: '已入库', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 400)
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style scoped>
.step { padding: 30rpx 0; }
.shoot-area {
  margin: 0 32rpx; height: 560rpx;
  border: 3rpx dashed #d8dce3; border-radius: 24rpx;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden; background: #fff;
}
.shoot-hint { display: flex; flex-direction: column; align-items: center; gap: 20rpx; }
.shoot-img { width: 100%; height: 100%; }
.submit { margin: 40rpx 32rpx; }
.submit.disabled { background: #b8e6d4; }

.step-title { font-size: 32rpx; font-weight: 600; }
.thumb { width: 96rpx; height: 96rpx; border-radius: 12rpx; }
.field-label { display: block; font-size: 26rpx; color: #86909c; margin: 24rpx 0 14rpx; }
.ta { min-height: 140rpx; width: 100%; box-sizing: border-box; }

.reason-group { display: flex; flex-wrap: wrap; gap: 16rpx; }
.reason-chip {
  padding: 12rpx 32rpx; border-radius: 999rpx; background: #f2f3f5;
  font-size: 26rpx; color: #4e5969; border: 2rpx solid transparent;
}
.reason-chip.on { background: #e2f7ee; color: #00b578; border-color: #00b578; font-weight: 600; }

.count-row { justify-content: flex-start; gap: 30rpx; }
.count-btn {
  width: 64rpx; height: 64rpx; border-radius: 50%;
  background: #f2f3f5; color: #4e5969; font-size: 36rpx;
  display: flex; align-items: center; justify-content: center;
}
.count-num { font-size: 40rpx; font-weight: 600; min-width: 60rpx; text-align: center; }
</style>
