import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue') },
  {
    path: '/',
    component: () => import('./views/Layout.vue'),
    redirect: '/overview',
    children: [
      { path: 'overview', component: () => import('./views/Overview.vue'), meta: { title: '数据总览' } },
      { path: 'users', component: () => import('./views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'meetings', component: () => import('./views/Meetings.vue'), meta: { title: '会议管理' } },
      { path: 'questions', component: () => import('./views/Questions.vue'), meta: { title: '题库管理' } },
      { path: 'wrongs', component: () => import('./views/Wrongs.vue'), meta: { title: '错题本' } },
      { path: 'documents', component: () => import('./views/Documents.vue'), meta: { title: '教学文档' } },
      { path: 'meeting-data', component: () => import('./views/MeetingData.vue'), meta: { title: '会议过程数据' } },
      { path: 'misc', component: () => import('./views/Misc.vue'), meta: { title: '问答与学情' } }
    ]
  }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach(to => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) return '/login'
  if (to.path === '/login' && token) return '/'
})

export default router
