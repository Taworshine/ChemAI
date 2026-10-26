<template>
  <el-container class="shell">
    <!-- 侧边栏 -->
    <el-aside width="216px" class="side">
      <div class="logo">
        <div class="logo-icon">
          <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
            <path d="M9 3h6M10 3v5.5L4.5 18a2 2 0 001.8 3h11.4a2 2 0 001.8-3L14 8.5V3" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M7 15h10" stroke="#ffd9ad" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="logo-text">
          <strong>思维赋能</strong>
          <span>Mind Empower</span>
        </div>
      </div>

      <el-menu :default-active="$route.path" :default-openeds="openeds" router class="menu">
        <template v-if="role === 'TEACHER'">
          <el-menu-item index="/teacher/dashboard">
            <el-icon><Monitor /></el-icon><span>工作台</span>
          </el-menu-item>
          <el-menu-item index="/teacher/graph">
            <el-icon><Share /></el-icon><span>知识图谱</span>
          </el-menu-item>
          <el-sub-menu index="t-res">
            <template #title><el-icon><FolderOpened /></el-icon><span>资源管理</span></template>
            <el-menu-item index="/teacher/knowledge">
              <el-icon><Collection /></el-icon><span>知识管理</span>
            </el-menu-item>
            <el-menu-item index="/teacher/question">
              <el-icon><Notebook /></el-icon><span>题库管理</span>
            </el-menu-item>
            <el-menu-item index="/teacher/document">
              <el-icon><Document /></el-icon><span>文档管理</span>
            </el-menu-item>
            <el-menu-item index="/teacher/extract">
              <el-icon><UploadFilled /></el-icon><span>知识入库</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="t-research">
            <template #title>
              <el-icon><DataAnalysis /></el-icon><span>教研中心</span>
              <el-badge v-if="todoCount > 0" :value="todoCount" :max="99" class="todo-badge" />
            </template>
            <el-menu-item index="/teacher/confirm">
              <el-icon><Finished /></el-icon><span>确认中心</span>
              <el-badge v-if="pendingTasks > 0" :value="pendingTasks" :max="99" class="item-badge" />
            </el-menu-item>
            <el-menu-item index="/teacher/faq-manage">
              <el-icon><QuestionFilled /></el-icon><span>问题解答</span>
              <el-badge v-if="pendingFaqs > 0" :value="pendingFaqs" :max="99" class="item-badge" />
            </el-menu-item>
            <el-menu-item index="/teacher/research">
              <el-icon><TrendCharts /></el-icon><span>教研看板</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/teacher/analysis">
            <el-icon><DataAnalysis /></el-icon><span>学情分析</span>
          </el-menu-item>
        </template>
        <template v-else>
          <el-menu-item index="/student/dashboard">
            <el-icon><Monitor /></el-icon><span>个人面板</span>
          </el-menu-item>
          <el-sub-menu index="s-study">
            <template #title><el-icon><Reading /></el-icon><span>学习中心</span></template>
            <el-menu-item index="/student/chat">
              <el-icon><ChatDotRound /></el-icon><span>智能追问</span>
            </el-menu-item>
            <el-menu-item index="/student/faq">
              <el-icon><QuestionFilled /></el-icon><span>高频问题</span>
            </el-menu-item>
            <el-menu-item index="/student/graph">
              <el-icon><Share /></el-icon><span>知识图谱</span>
            </el-menu-item>
            <el-menu-item index="/student/practice">
              <el-icon><EditPen /></el-icon><span>在线练习</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon><span>学情报告</span>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="side-user">
        <div class="avatar">{{ (realName || 'U').charAt(0) }}</div>
        <div class="info">
          <strong>{{ realName }}</strong>
          <span>{{ role === 'TEACHER' ? '教师端' : '学生端' }}</span>
        </div>
        <el-tooltip content="退出登录" placement="top">
          <el-icon class="logout" @click="logout"><SwitchButton /></el-icon>
        </el-tooltip>
      </div>
    </el-aside>

    <el-container>
      <el-header class="topbar" height="60px">
        <div class="crumb-wrap">
          <span class="crumb-root">思维赋能</span>
          <el-icon class="crumb-sep" :size="12"><ArrowRight /></el-icon>
          <span class="crumb">{{ $route.meta.title || '' }}</span>
        </div>
        <div class="top-right">
          <span class="faint today">{{ today }}</span>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <span class="uc-avatar">{{ (realName || 'U').charAt(0) }}</span>
              <span class="uc-name">{{ realName }}</span>
              <span class="uc-role">{{ role === 'TEACHER' ? '教师' : '学生' }}</span>
              <el-icon :size="12" class="uc-caret"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home">
                  <el-icon><Monitor /></el-icon>{{ role === 'TEACHER' ? '工作台' : '个人面板' }}
                </el-dropdown-item>
                <el-dropdown-item command="profile" v-if="role !== 'TEACHER'">
                  <el-icon><User /></el-icon>学情报告
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Monitor, Share, Collection, Notebook, FolderOpened, DataAnalysis, Document, Reading,
         ChatDotRound, EditPen, User, SwitchButton, ArrowRight, ArrowDown,
         UploadFilled, Finished, TrendCharts, QuestionFilled } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const role = ref(localStorage.getItem('role'))
const realName = ref(localStorage.getItem('realName') || '')
const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
/** 下拉分组默认展开 */
const openeds = [role.value === 'TEACHER' ? 't-res' : 's-study']

/** 教研中心待办徽标：待确认入库任务 + 待解答高频问题（教师端每 60s 轮询） */
const pendingTasks = ref(0)
const pendingFaqs = ref(0)
const todoCount = computed(() => pendingTasks.value + pendingFaqs.value)
let todoTimer = null

async function loadTodos() {
  if (role.value !== 'TEACHER') return
  try {
    const [tasks, faq] = await Promise.all([
      api.get('/extract/tasks'),
      api.get('/agent/faq/manage')
    ])
    pendingTasks.value = (tasks.data || []).filter(t => t.task.status === 'PENDING_CONFIRM').length
    pendingFaqs.value = faq.data?.pending || 0
  } catch { /* 未登录或网络异常时静默 */ }
}

function onCommand(cmd) {
  if (cmd === 'logout') logout()
  else if (cmd === 'home') router.push(role.value === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard')
  else if (cmd === 'profile') router.push('/student/profile')
}

onMounted(async () => {
  try {
    const res = await api.get('/auth/info')
    realName.value = res.data.realName
    localStorage.setItem('realName', res.data.realName)
  } catch { /* 401 由拦截器处理 */ }
  loadTodos()
  todoTimer = setInterval(loadTodos, 60000)
})

onBeforeUnmount(() => clearInterval(todoTimer))

async function logout() {
  try { await api.post('/auth/logout') } catch { /* 忽略 */ }
  localStorage.clear()
  router.push('/login')
}
</script>

<style scoped>
.shell { height: 100vh; }
.side {
  background: #fff;
  display: flex; flex-direction: column;
  border-right: 1px solid var(--line);
}
.logo { display: flex; align-items: center; gap: 10px; padding: 20px 18px 16px; }
.logo-icon {
  width: 40px; height: 40px; border-radius: 12px;
  background: linear-gradient(135deg, #6a94ff, #3b6ce0);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 12px rgba(74, 125, 255, .35);
}
.logo-text { display: flex; flex-direction: column; line-height: 1.25; }
.logo-text strong { font-size: 16px; color: var(--ink); letter-spacing: 1px; }
.logo-text span { font-size: 11px; color: var(--ink-3); }

.menu { flex: 1; padding: 6px 10px; }
.menu :deep(.el-menu-item) {
  border-radius: 10px; margin: 3px 0; color: var(--ink-2); height: 46px;
}
.menu :deep(.el-menu-item:hover) { background: #f2f5fd; color: var(--primary); }
.menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, rgba(74,125,255,.12), rgba(74,125,255,.04));
  color: var(--primary); font-weight: 600;
}
/* 下拉分组：标题与一级菜单同高，子项缩进 */
.menu :deep(.el-sub-menu__title) {
  border-radius: 10px; margin: 3px 0; color: var(--ink-2); height: 46px;
}
.menu :deep(.el-sub-menu__title:hover) { background: #f2f5fd; color: var(--primary); }
.menu :deep(.el-sub-menu .el-menu-item) { min-width: 0; padding-left: 44px !important; }
.menu :deep(.el-sub-menu__title) { position: relative; }
/* 待办徽标：分组标题右上角 + 子项文字后 */
.todo-badge { margin-left: auto; }
.todo-badge :deep(.el-badge__content) { position: static; transform: none; }
.item-badge { margin-left: auto; }
.item-badge :deep(.el-badge__content) { position: static; transform: none; }

.side-user {
  margin: 10px 14px 16px; padding: 12px; border-radius: 12px;
  background: #f6f8fd; display: flex; align-items: center; gap: 10px;
}
.side-user .avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #ffb27a, #ff8a3d);
  color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 600;
}
.side-user .info { flex: 1; display: flex; flex-direction: column; line-height: 1.3; }
.side-user .info strong { font-size: 14px; color: var(--ink); }
.side-user .info span { font-size: 12px; color: var(--ink-3); }
.logout { cursor: pointer; color: var(--ink-3); }
.logout:hover { color: #f56c6c; }

.topbar {
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255,255,255,.75); backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line); padding: 0 26px;
}
.crumb-wrap { display: flex; align-items: center; gap: 8px; min-width: 0; white-space: nowrap; }
.crumb-root { font-size: 13px; color: var(--ink-3); }
.crumb-sep { color: var(--ink-3); }
.crumb { font-size: 15px; font-weight: 600; color: var(--ink); letter-spacing: .5px; }
.top-right { display: flex; align-items: center; gap: 16px; }
.today { font-size: 13px; }
.user-chip {
  display: flex; align-items: center; gap: 8px; padding: 5px 12px 5px 6px;
  background: #f6f8fd; border-radius: 999px; border: 1px solid var(--line);
  cursor: pointer; transition: border-color .2s, box-shadow .2s; outline: none;
}
.user-chip:hover { border-color: rgba(74,125,255,.4); box-shadow: 0 2px 10px rgba(74,125,255,.12); }
.uc-caret { color: var(--ink-3); }
.uc-avatar {
  width: 28px; height: 28px; border-radius: 50%; color: #fff; font-size: 13px; font-weight: 600;
  background: linear-gradient(135deg, #6a94ff, #4a7dff);
  display: flex; align-items: center; justify-content: center;
}
.uc-name { font-size: 13px; font-weight: 600; color: var(--ink); }
.uc-role { font-size: 11px; color: var(--ink-3); }
@media (max-width: 720px) { .today { display: none; } .uc-name { display: none; } }
.content { padding: 22px 26px; overflow-y: auto; }
</style>
