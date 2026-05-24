import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', icon: 'House' }
      },
      {
        path: 'system/users',
        name: 'SystemUsers',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'workflow/leave',
        name: 'WorkflowLeave',
        component: () => import('@/views/workflow/leave/index.vue'),
        meta: { title: '请假申请', icon: 'Document' }
      },
      {
        path: 'collab/messages',
        name: 'CollabMessages',
        component: () => import('@/views/collab/message/index.vue'),
        meta: { title: '消息中心', icon: 'ChatDotRound' }
      },
      {
        path: 'finance/expense',
        name: 'FinanceExpense',
        component: () => import('@/views/finance/expense/index.vue'),
        meta: { title: '报销管理', icon: 'Money' }
      },
      {
        path: 'attendance/check',
        name: 'AttendanceCheck',
        component: () => import('@/views/attendance/check/index.vue'),
        meta: { title: '考勤打卡', icon: 'Timer' }
      },
      {
        path: 'system/depts',
        name: 'SystemDepts',
        component: () => import('@/views/system/dept/index.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'system/roles',
        name: 'SystemRoles',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色权限', icon: 'Key' }
      },
      {
        path: 'collab/contacts',
        name: 'CollabContacts',
        component: () => import('@/views/collab/contact/index.vue'),
        meta: { title: '通讯录', icon: 'Phone' }
      },
      {
        path: 'collab/schedules',
        name: 'CollabSchedules',
        component: () => import('@/views/collab/schedule/index.vue'),
        meta: { title: '日程管理', icon: 'Calendar' }
      },
      {
        path: 'finance/budget',
        name: 'FinanceBudget',
        component: () => import('@/views/finance/budget/index.vue'),
        meta: { title: '预算管理', icon: 'Wallet' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.path !== '/login' && !userStore.isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router