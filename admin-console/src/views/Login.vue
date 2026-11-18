<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="logo">DB</div>
      <h2>思维赋能 · 后台管理系统</h2>
      <p class="sub">数据库管理后台 · 仅管理员可登录</p>
      <el-form @submit.prevent="doLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="管理员账号" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="doLogin">登 录</el-button>
      </el-form>
      <p class="tip">演示账号：admin / 123456</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function doLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  loading.value = true
  try {
    const res = await http.post('/auth/login', form)
    if (res.data.role !== 'ADMIN') {
      ElMessage.error('该账号不是管理员，无法登录后台管理系统')
      return
    }
    localStorage.setItem('admin_token', res.data.token)
    localStorage.setItem('admin_name', res.data.realName || res.data.role)
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b5aed 100%);
}
.login-box {
  width: 380px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  text-align: center;
}
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: #2b5aed;
  color: #fff;
  font-weight: 700;
  font-size: 22px;
  line-height: 56px;
}
h2 { margin: 0 0 4px; font-size: 20px; }
.sub { color: #86909c; font-size: 13px; margin: 0 0 24px; }
.btn { width: 100%; }
.tip { color: #a9aeb8; font-size: 12px; margin-top: 16px; }
</style>
