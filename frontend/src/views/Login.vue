<template>
  <div class="login">
    <!-- 左侧品牌区 -->
    <div class="brand-pane">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
      <div class="grid-mask"></div>

      <div class="brand-inner">
        <div class="logo-badge">
          <svg width="34" height="34" viewBox="0 0 24 24" fill="none">
            <path d="M9 3h6M10 3v5.5L4.5 18a2 2 0 001.8 3h11.4a2 2 0 001.8-3L14 8.5V3" stroke="#fff" stroke-width="1.8" stroke-linecap="round"/>
            <path d="M7 15h10" stroke="#ffd9ad" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <h1>思维赋能</h1>
        <p class="slogan">启发思考，而非给出答案</p>

        <div class="feature-list">
          <div class="feature" v-for="f in features" :key="f.t">
            <div class="f-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" v-html="f.icon"></svg>
            </div>
            <div>
              <p class="f-t">{{ f.t }}</p>
              <p class="f-d">{{ f.d }}</p>
            </div>
          </div>
        </div>

        <svg class="art" viewBox="0 0 420 200" fill="none">
          <ellipse cx="210" cy="188" rx="150" ry="10" fill="rgba(255,255,255,.10)"/>
          <circle class="n n1" cx="120" cy="120" r="8" fill="#8fb0ff"/>
          <circle class="n n2" cx="200" cy="70" r="10" fill="#ffb27a"/>
          <circle class="n n3" cx="290" cy="110" r="7" fill="#9fe8d6"/>
          <circle class="n n4" cx="245" cy="150" r="5" fill="#ffb3cd"/>
          <path class="l l1" d="M128 116 L192 74" stroke="rgba(255,255,255,.55)" stroke-width="1.5"/>
          <path class="l l2" d="M208 74 L284 106" stroke="rgba(255,255,255,.55)" stroke-width="1.5"/>
          <path class="l l3" d="M212 80 L243 145" stroke="rgba(255,255,255,.4)" stroke-width="1.5"/>
          <path class="l l4" d="M284 115 L249 148" stroke="rgba(255,255,255,.4)" stroke-width="1.5"/>
          <path class="l l5" d="M127 127 L240 153" stroke="rgba(255,255,255,.3)" stroke-width="1.5"/>
        </svg>
      </div>
    </div>

    <!-- 右侧登录卡 -->
    <div class="form-pane">
      <div class="glass rise">
        <div class="g-head">
          <h2>欢迎回来</h2>
          <p class="sub">请使用学校账号登录系统</p>
        </div>
        <el-form size="large" @submit.prevent>
          <el-form-item>
            <el-input v-model="username" placeholder="用户名 / 学号" :prefix-icon="User" class="fancy-input" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="password" type="password" placeholder="密码" show-password
                      :prefix-icon="Lock" class="fancy-input" @keyup.enter="doLogin" />
          </el-form-item>
          <el-button type="primary" class="btn" size="large" :loading="loading" @click="doLogin">
            登 录
          </el-button>
        </el-form>

        <div class="accounts">
          <el-divider content-position="center"><span class="faint" style="font-size:12px">演示账号 · 点击快速填入</span></el-divider>
          <div class="acc-grid">
            <div class="acc-card" @click="fill('teacher', '123456')">
              <span class="acc-avatar teacher">师</span>
              <div><p class="acc-name">教师端</p><p class="acc-p">teacher / 123456</p></div>
            </div>
            <div class="acc-card" @click="fill('2023001', '123456')">
              <span class="acc-avatar student">李</span>
              <div><p class="acc-name">学生端</p><p class="acc-p">2023001 / 123456</p></div>
            </div>
          </div>
          <p class="acc-tip">学生学号 2023001 ~ 2023010，密码均为 123456</p>
        </div>
      </div>
      <p class="foot">思维赋能 Mind Empower · 高中全学科苏格拉底式智能教学辅助系统</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

const features = [
  { t: '全学科知识图谱', d: '语数英物化生政史地 · Neo4j 实时图谱',
    icon: '<circle cx="5" cy="6" r="2.4" fill="#8fb0ff"/><circle cx="19" cy="5" r="2.4" fill="#ffb27a"/><circle cx="12" cy="18" r="2.4" fill="#9fe8d6"/><path d="M7 7l10-1M6 8l5 8M18 7l-5 9" stroke="#fff" stroke-width="1.3"/>' },
  { t: '苏格拉底式追问', d: '不直接给答案 · AI 循循善诱启发思路',
    icon: '<path d="M4 5h16v10H9l-5 4z" stroke="#fff" stroke-width="1.5" fill="none" stroke-linejoin="round"/><path d="M8 9h8M8 12h5" stroke="#ffd9ad" stroke-width="1.5" stroke-linecap="round"/>' },
  { t: '班级学情分析', d: '概念掌握热力图 · 个体五维思维雷达',
    icon: '<path d="M4 19V10M10 19V5M16 19v-7M22 19H2" stroke="#fff" stroke-width="1.5" stroke-linecap="round" fill="none"/>' }
]

function fill(u, p) {
  username.value = u
  password.value = p
  ElMessage.info(`已填入 ${u}，点击登录进入`)
}

async function doLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', { username: username.value, password: password.value })
    localStorage.setItem('satoken', res.data.token)
    localStorage.setItem('role', res.data.role)
    localStorage.setItem('realName', res.data.realName)
    localStorage.setItem('userId', res.data.userId)
    ElMessage.success('登录成功，欢迎回来')
    router.push(res.data.role === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login { display: flex; min-height: 100vh; overflow: hidden; }

/* ---------- 左侧品牌区：深空渐变 + 漂浮光斑 + 网格 ---------- */
.brand-pane {
  flex: 1.25;
  background: linear-gradient(160deg, #1b2a6b 0%, #2c46a8 42%, #4a7dff 78%, #6f9aff 100%);
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
}
.orb { position: absolute; border-radius: 50%; filter: blur(60px); opacity: .55; animation: drift 14s ease-in-out infinite alternate; }
.orb-1 { width: 380px; height: 380px; background: #7fa8ff; top: -120px; right: -80px; }
.orb-2 { width: 300px; height: 300px; background: #ff9a4d; bottom: -100px; left: -60px; opacity: .28; animation-delay: -5s; }
.orb-3 { width: 220px; height: 220px; background: #38b8a0; top: 40%; left: 30%; opacity: .22; animation-delay: -9s; }
@keyframes drift {
  from { transform: translate(0, 0) scale(1); }
  to { transform: translate(40px, 30px) scale(1.12); }
}
.grid-mask {
  position: absolute; inset: 0;
  background-image: linear-gradient(rgba(255,255,255,.05) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(255,255,255,.05) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse at 50% 40%, #000 30%, transparent 75%);
}
.brand-inner { text-align: center; color: #fff; padding: 48px; position: relative; z-index: 1; max-width: 560px; }
.logo-badge {
  width: 66px; height: 66px; border-radius: 20px; margin: 0 auto 22px;
  background: rgba(255,255,255,.16); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 10px 30px rgba(10, 25, 80, .35), inset 0 1px 0 rgba(255,255,255,.35);
  animation: bob 5s ease-in-out infinite;
}
@keyframes bob { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-8px); } }
.brand-inner h1 { font-size: 40px; letter-spacing: 10px; margin: 0 0 12px; text-shadow: 0 4px 20px rgba(0,0,0,.25); }
.slogan { font-size: 17px; opacity: .92; margin: 0 0 36px; letter-spacing: 4px; }

.feature-list { display: flex; flex-direction: column; gap: 14px; margin: 0 auto 30px; max-width: 380px; }
.feature {
  display: flex; align-items: center; gap: 14px; text-align: left;
  background: rgba(255,255,255,.10); border: 1px solid rgba(255,255,255,.16);
  border-radius: 14px; padding: 12px 18px; backdrop-filter: blur(8px);
  transition: transform .25s, background .25s;
}
.feature:hover { transform: translateX(6px); background: rgba(255,255,255,.16); }
.f-icon {
  width: 38px; height: 38px; border-radius: 11px; flex-shrink: 0;
  background: rgba(255,255,255,.14); display: flex; align-items: center; justify-content: center;
}
.f-t { margin: 0; font-size: 14.5px; font-weight: 600; }
.f-d { margin: 2px 0 0; font-size: 12px; opacity: .72; }

.art { width: 380px; max-width: 85%; }
.art .n { animation: bob 6s ease-in-out infinite; }
.art .n2 { animation-delay: -1.5s; } .art .n3 { animation-delay: -3s; } .art .n4 { animation-delay: -4.5s; }
.art .l { stroke-dasharray: 6 5; animation: flow 3s linear infinite; }
@keyframes flow { to { stroke-dashoffset: -22; } }

/* ---------- 右侧表单区 ---------- */
.form-pane {
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 32px 20px; position: relative;
  background: radial-gradient(1200px 600px at 80% -10%, #eef3ff 0%, #f7f9fe 55%, #f2f5fc 100%);
}
.glass {
  width: 400px; max-width: 100%; background: rgba(255,255,255,.86); backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,.95); border-radius: 24px;
  box-shadow: 0 24px 70px rgba(60, 80, 150, .18), 0 2px 8px rgba(60, 80, 150, .06);
  padding: 42px 38px 30px;
}
.rise { animation: rise .6s cubic-bezier(.2,.8,.3,1) both; }
@keyframes rise { from { opacity: 0; transform: translateY(22px); } to { opacity: 1; transform: none; } }
.g-head h2 { margin: 0 0 6px; font-size: 26px; color: var(--ink); letter-spacing: 1px; }
.sub { color: var(--ink-3); font-size: 13px; margin: 0 0 28px; }

:deep(.fancy-input .el-input__wrapper) {
  border-radius: 12px; padding: 4px 14px;
  box-shadow: 0 0 0 1px var(--line) inset; transition: box-shadow .2s;
}
:deep(.fancy-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px #4a7dff inset, 0 4px 14px rgba(74,125,255,.12);
}
.btn {
  width: 100%; font-size: 16px; letter-spacing: 10px; border-radius: 12px; height: 46px; margin-top: 4px;
  background: linear-gradient(90deg, #5f8dff, #4a7dff);
  border: none; box-shadow: 0 8px 20px rgba(74, 125, 255, .38);
  transition: transform .15s, box-shadow .2s;
}
.btn:hover { background: linear-gradient(90deg, #6f9aff, #5a86ff); transform: translateY(-1px); box-shadow: 0 10px 26px rgba(74,125,255,.45); }
.btn:active { transform: translateY(0); }

.accounts { margin-top: 8px; }
.acc-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.acc-card {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border: 1px solid var(--line); border-radius: 12px; cursor: pointer;
  background: #fbfcff; transition: all .2s; user-select: none;
}
.acc-card:hover { border-color: #4a7dff; box-shadow: 0 4px 14px rgba(74,125,255,.14); transform: translateY(-1px); }
.acc-avatar {
  width: 34px; height: 34px; border-radius: 10px; color: #fff; font-size: 14px; font-weight: 600;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.acc-avatar.teacher { background: linear-gradient(135deg, #6a94ff, #4a7dff); }
.acc-avatar.student { background: linear-gradient(135deg, #ffb27a, #ff9a4d); }
.acc-name { margin: 0; font-size: 13px; font-weight: 600; color: var(--ink); }
.acc-p { margin: 1px 0 0; font-size: 11.5px; color: var(--ink-3); }
.acc-tip { text-align: center; font-size: 11.5px; color: var(--ink-3); margin: 12px 0 0; }
.foot { position: absolute; bottom: 16px; font-size: 12px; color: var(--ink-3); opacity: .8; }

/* 窄屏：上下布局 */
@media (max-width: 900px) {
  .login { flex-direction: column; }
  .brand-pane { padding: 40px 0 10px; }
  .brand-inner { padding: 24px; }
  .brand-inner h1 { font-size: 30px; }
  .art { display: none; }
  .feature-list { margin-bottom: 10px; }
  .form-pane { padding-top: 24px; }
}
</style>
