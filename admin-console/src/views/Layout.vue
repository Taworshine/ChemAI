<template>
  <el-container class="shell">
    <el-aside width="220px">
      <div class="brand">
        <span class="dot">DB</span>
        <span>思维赋能后台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#a9aeb8" active-text-color="#ffffff">
        <el-menu-item index="/overview">📊 数据总览</el-menu-item>
        <el-menu-item index="/users">👤 用户管理</el-menu-item>
        <el-menu-item index="/meetings">🗓️ 会议管理</el-menu-item>
        <el-menu-item index="/questions">📝 题库管理</el-menu-item>
        <el-menu-item index="/wrongs">❌ 错题本</el-menu-item>
        <el-menu-item index="/documents">📄 教学文档</el-menu-item>
        <el-menu-item index="/meeting-data">💬 会议过程数据</el-menu-item>
        <el-menu-item index="/misc">📈 问答与学情</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="head">
        <span class="page-title">{{ $route.meta.title }}</span>
        <div class="who">
          <span>{{ name }}</span>
          <el-button size="small" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const name = ref(localStorage.getItem('admin_name') || '管理员')

function logout() {
  localStorage.removeItem('admin_token')
  localStorage.removeItem('admin_name')
  router.push('/login')
}
</script>

<style scoped>
.shell { min-height: 100vh; }
.el-aside { background: #1f2d3d; }
.brand {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #fff;
  font-weight: 600;
}
.dot {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #2b5aed;
  text-align: center;
  line-height: 32px;
  font-size: 13px;
}
.el-menu { border-right: none; }
.head {
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title { font-weight: 600; font-size: 16px; }
.who { display: flex; align-items: center; gap: 12px; color: #4e5969; }
</style>
