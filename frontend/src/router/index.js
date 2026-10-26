import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: () => localStorage.getItem('role') === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard',
    children: [
      { path: 'teacher/dashboard', component: () => import('../views/teacher/Dashboard.vue'), meta: { role: 'TEACHER', title: '工作台' } },
      { path: 'teacher/graph', component: () => import('../views/teacher/Graph.vue'), meta: { role: 'TEACHER', title: '知识图谱' } },
      { path: 'teacher/knowledge', component: () => import('../views/teacher/Knowledge.vue'), meta: { role: 'TEACHER', title: '知识管理' } },
      { path: 'teacher/question', component: () => import('../views/teacher/QuestionBank.vue'), meta: { role: 'TEACHER', title: '题库管理' } },
      { path: 'teacher/document', component: () => import('../views/teacher/Document.vue'), meta: { role: 'TEACHER', title: '文档管理' } },
      { path: 'teacher/extract', component: () => import('../views/teacher/ExtractUpload.vue'), meta: { role: 'TEACHER', title: '知识入库' } },
      { path: 'teacher/confirm', component: () => import('../views/teacher/ConfirmCenter.vue'), meta: { role: 'TEACHER', title: '确认中心' } },
      { path: 'teacher/research', component: () => import('../views/teacher/Research.vue'), meta: { role: 'TEACHER', title: '教研看板' } },
      { path: 'teacher/faq-manage', component: () => import('../views/teacher/FaqManage.vue'), meta: { role: 'TEACHER', title: '问题解答' } },
      { path: 'teacher/analysis', component: () => import('../views/teacher/Analysis.vue'), meta: { role: 'TEACHER', title: '学情分析' } },
      { path: 'student/dashboard', component: () => import('../views/student/Dashboard.vue'), meta: { role: 'STUDENT', title: '个人面板' } },
      { path: 'student/chat', component: () => import('../views/student/Chat.vue'), meta: { role: 'STUDENT', title: '智能追问' } },
      { path: 'student/faq', component: () => import('../views/student/Faq.vue'), meta: { role: 'STUDENT', title: '高频问题' } },
      { path: 'student/graph', component: () => import('../views/student/GraphView.vue'), meta: { role: 'STUDENT', title: '知识图谱' } },
      { path: 'student/practice', component: () => import('../views/student/Practice.vue'), meta: { role: 'STUDENT', title: '在线练习' } },
      { path: 'student/profile', component: () => import('../views/student/Profile.vue'), meta: { role: 'STUDENT', title: '学情报告' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  if (to.meta.public) return true
  const token = localStorage.getItem('satoken')
  if (!token) return '/login'
  if (to.meta.role && localStorage.getItem('role') !== to.meta.role) {
    return localStorage.getItem('role') === 'TEACHER' ? '/teacher/dashboard' : '/student/dashboard'
  }
  return true
})

export default router
