<template>
  <view class="page">
    <!-- 品牌区：深绿渐变 -->
    <view class="hero">
      <view class="hero-bg"></view>
      <view class="logo-mark">
        <text class="logo-text">研</text>
      </view>
      <view class="hero-title">思维赋能 · 教研会议</view>
      <view class="hero-sub">高中教研组移动协作平台</view>
    </view>

    <!-- 登录表单：浮动卡片 -->
    <view class="form-wrap">
      <view class="form-card">
        <view class="field">
          <text class="field-label">用户名 / 工号</text>
          <input class="input" v-model="username" placeholder="请输入用户名" placeholder-class="ph" />
        </view>
        <view class="field">
          <text class="field-label">密码</text>
          <input class="input" v-model="password" type="password" placeholder="请输入密码" placeholder-class="ph" @confirm="login" />
        </view>
        <view class="btn-primary login-btn press" @click="login">登 录</view>
        <view class="to-register" @click="showRegister = true">没有账号？<text class="link">立即注册</text></view>
        <view class="demo-tip">演示账号：teacher / 123456</view>
      </view>

      <!-- 注册弹层：学生 / 教师自助注册 -->
      <view v-if="showRegister" class="modal-mask" @click="showRegister = false">
        <view class="modal" @click.stop>
          <view class="modal-title">注册账号</view>
          <view class="field">
            <text class="field-label">用户名 / 工号</text>
            <input class="input" v-model="reg.username" placeholder="3~50 个字符，注册后不可改" placeholder-class="ph" />
          </view>
          <view class="field">
            <text class="field-label">姓名</text>
            <input class="input" v-model="reg.realName" placeholder="真实姓名" placeholder-class="ph" />
          </view>
          <view class="field">
            <text class="field-label">身份</text>
            <view class="role-row">
              <view class="role-item" :class="{ active: reg.role === 'STUDENT' }" @click="reg.role = 'STUDENT'">学生</view>
              <view class="role-item" :class="{ active: reg.role === 'TEACHER' }" @click="reg.role = 'TEACHER'">教师</view>
            </view>
          </view>
          <view v-if="reg.role === 'STUDENT'" class="field">
            <text class="field-label">班级</text>
            <input class="input" v-model="reg.className" placeholder="如：高三(2)班" placeholder-class="ph" />
          </view>
          <view v-if="reg.role === 'TEACHER'" class="field">
            <text class="field-label">任教学科（可空）</text>
            <input class="input" v-model="reg.subject" placeholder="如：化学，也可注册后由管理员分配" placeholder-class="ph" />
          </view>
          <view class="field">
            <text class="field-label">密码</text>
            <input class="input" v-model="reg.password" type="password" placeholder="至少 6 位" placeholder-class="ph" />
          </view>
          <view class="field">
            <text class="field-label">确认密码</text>
            <input class="input" v-model="reg.confirm" type="password" placeholder="再输一遍" placeholder-class="ph" />
          </view>
          <view class="btn-primary reg-btn press" :class="{ disabled: reging }" @click="register">{{ reging ? '注册中…' : '注 册'}}</view>
          <view class="btn-outline cancel-btn press" @click="showRegister = false">取 消</view>
        </view>
      </view>

      <!-- 服务器配置（折叠） -->
      <view class="server-card" @click="showServerEdit = !showServerEdit">
        <view class="row between">
          <view class="row">
            <uni-icons type="gear" size="34" color="#86909c" />
            <text class="server-label">服务器地址</text>
          </view>
          <view class="row">
            <text class="server-url">{{ serverBase }}</text>
            <uni-icons type="down" size="26" color="#c4c9d2" :class="{ rotated: showServerEdit }" />
          </view>
        </view>
        <view v-if="showServerEdit" class="server-edit" @click.stop>
          <input class="input" v-model="serverBaseInput" placeholder="http://192.168.x.x:8080" placeholder-class="ph" />
          <view class="btn-outline save-btn press" @click.stop="saveServer">保存地址</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { request, setBaseUrl } from '@/utils/request.js'

export default {
  data() {
    return {
      username: '',
      password: '',
      showRegister: false,
      reging: false,
      reg: { username: '', realName: '', role: 'STUDENT', className: '', subject: '', password: '', confirm: '' },
      showServerEdit: false,
      // #ifdef H5
      serverBase: '本地调试代理',
      // #endif
      // #ifndef H5
      serverBase: uni.getStorageSync('serverBase') || 'http://localhost:8080',
      // #endif
      serverBaseInput: ''
    }
  },
  onLoad() {
    if (uni.getStorageSync('satoken')) {
      uni.switchTab({ url: '/pages/meeting/list' })
    }
  },
  methods: {
    saveServer() {
      if (this.serverBaseInput) {
        setBaseUrl(this.serverBaseInput.replace(/\/$/, ''))
        this.serverBase = this.serverBaseInput
        this.showServerEdit = false
        uni.showToast({ title: '已保存', icon: 'success' })
      }
    },
    async login() {
      if (!this.username || !this.password) {
        uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
        return
      }
      try {
        const data = await request({
          url: '/api/auth/login',
          method: 'POST',
          data: { username: this.username, password: this.password }
        })
        uni.setStorageSync('satoken', data.token)
        uni.setStorageSync('userInfo', data)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.switchTab({ url: '/pages/meeting/list' }), 300)
      } catch (e) { /* 错误已在封装中提示 */ }
    },
    async register() {
      const r = this.reg
      if (!r.username || r.username.trim().length < 3) return uni.showToast({ title: '用户名至少 3 个字符', icon: 'none' })
      if (!r.password || r.password.length < 6) return uni.showToast({ title: '密码至少 6 位', icon: 'none' })
      if (r.password !== r.confirm) return uni.showToast({ title: '两次密码不一致', icon: 'none' })
      this.reging = true
      try {
        await request({
          url: '/api/auth/register',
          method: 'POST',
          data: {
            username: r.username.trim(),
            password: r.password,
            realName: r.realName,
            role: r.role,
            className: r.className,
            subject: r.subject
          }
        })
        uni.showToast({ title: '注册成功，已自动登录', icon: 'success' })
        // 注册成功直接用新账号登录
        this.username = r.username.trim()
        this.password = r.password
        this.showRegister = false
        setTimeout(() => this.login(), 600)
      } catch (e) { /* 错误已在封装中提示 */ } finally {
        this.reging = false
      }
    }
  }
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f6f7f9; }

/* 品牌区 */
.hero {
  position: relative;
  padding: 130rpx 0 110rpx;
  text-align: center;
  overflow: hidden;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(160deg, #0f5c46 0%, #0b7a58 55%, #00a86e 100%);
  border-radius: 0 0 48rpx 48rpx;
}
.hero > view:not(.hero-bg) { position: relative; z-index: 1; }
.logo-mark {
  width: 124rpx; height: 124rpx; border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.16);
  border: 2rpx solid rgba(255, 255, 255, 0.35);
  margin: 0 auto 30rpx;
  display: flex; align-items: center; justify-content: center;
  backdrop-filter: blur(8rpx);
}
.logo-text { color: #fff; font-size: 58rpx; font-weight: 700; }
.hero-title { font-size: 40rpx; font-weight: 700; color: #ffffff; letter-spacing: 2rpx; }
.hero-sub { font-size: 24rpx; color: rgba(255, 255, 255, 0.75); margin-top: 14rpx; letter-spacing: 5rpx; }

/* 浮动表单 */
.form-wrap { padding: 0 40rpx; margin-top: -70rpx; position: relative; }
.form-card {
  background: #ffffff;
  border-radius: 28rpx;
  padding: 44rpx 36rpx 36rpx;
  border: 1rpx solid #f2f3f5;
}
.login-btn { margin-top: 44rpx; }
.demo-tip { text-align: center; font-size: 24rpx; color: #b8bec8; margin-top: 28rpx; }

/* 注册入口与弹层 */
.to-register { text-align: center; font-size: 26rpx; color: #86909c; margin-top: 28rpx; }
.to-register .link { color: #00a86e; font-weight: 600; }
.modal-mask {
  position: fixed; inset: 0; background: rgba(0, 0, 0, 0.45);
  display: flex; align-items: center; justify-content: center; z-index: 99;
}
.modal {
  width: 620rpx; max-height: 82vh; overflow-y: auto;
  background: #ffffff; border-radius: 28rpx; padding: 40rpx 36rpx;
}
.modal-title { font-size: 34rpx; font-weight: 700; text-align: center; margin-bottom: 30rpx; color: #1d2129; }
.role-row { display: flex; gap: 24rpx; }
.role-item {
  flex: 1; text-align: center; padding: 18rpx 0; border-radius: 16rpx;
  background: #f2f3f5; color: #4e5969; font-size: 28rpx;
}
.role-item.active { background: rgba(0, 168, 110, 0.12); color: #00a86e; font-weight: 600; }
.reg-btn { margin-top: 36rpx; }
.reg-btn.disabled { opacity: 0.6; }
.cancel-btn { margin-top: 20rpx; font-size: 28rpx; padding: 20rpx 0; text-align: center; }

/* 服务器配置 */
.server-card {
  margin-top: 28rpx;
  background: #ffffff;
  border-radius: 24rpx;
  border: 1rpx solid #f2f3f5;
  padding: 30rpx 32rpx;
}
.server-label { font-size: 26rpx; color: #4e5969; margin-left: 14rpx; }
.server-url { color: #00a86e; font-size: 24rpx; max-width: 360rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-right: 8rpx; }
.rotated { transform: rotate(180deg); transition: transform 0.2s; }
.server-edit { margin-top: 24rpx; }
.save-btn { margin-top: 20rpx; font-size: 26rpx; padding: 16rpx 0; }
</style>
