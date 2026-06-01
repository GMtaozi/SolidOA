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
      { path: 'home', name: 'Home', component: () => import('@/views/home/index.vue'), meta: { title: '首页' } },
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '仪表盘' } },
      { path: 'approval', name: 'Approval', component: () => import('@/views/approval/index.vue'), meta: { title: '审批管理' } },
      { path: 'expense', name: 'Expense', component: () => import('@/views/finance/expense/index.vue'), meta: { title: '报销管理' } },
      // { path: 'budget', name: 'Budget', component: () => import('@/views/finance/budget/index.vue'), meta: { title: '预算管理' } },
      { path: 'leave', name: 'Leave', component: () => import('@/views/workflow/leave/index.vue'), meta: { title: '请假申请' } },
      { path: 'salary', name: 'Salary', component: () => import('@/views/workflow/salary/index.vue'), meta: { title: '工资发放审批' } },
      { path: 'overtime', name: 'Overtime', component: () => import('@/views/workflow/overtime/index.vue'), meta: { title: '加班申请' } },
      { path: 'business-trip', name: 'BusinessTrip', component: () => import('@/views/workflow/business-trip/index.vue'), meta: { title: '出差申请' } },
      { path: 'go-out', name: 'GoOut', component: () => import('@/views/workflow/go-out/index.vue'), meta: { title: '外出申请' } },
      { path: 'repair-card', name: 'RepairCard', component: () => import('@/views/attendance/repair-card/index.vue'), meta: { title: '补卡申请' } },
      { path: 'stamp', name: 'Stamp', component: () => import('@/views/workflow/stamp/index.vue'), meta: { title: '用印申请' } },
      { path: 'purchase', name: 'Purchase', component: () => import('@/views/workflow/purchase/index.vue'), meta: { title: '采购申请' } },
      { path: 'cc', name: 'Cc', component: () => import('@/views/workflow/cc/index.vue'), meta: { title: '抄送管理' } },
      { path: 'check', name: 'Check', component: () => import('@/views/attendance/check/index.vue'), meta: { title: '考勤打卡' } },
      { path: 'leave-balance', name: 'LeaveBalance', component: () => import('@/views/attendance/leave-balance/index.vue'), meta: { title: '假期余额' } },
      { path: 'contact', name: 'Contact', component: () => import('@/views/collab/contact/index.vue'), meta: { title: '通讯录' } },
      { path: 'message', name: 'Message', component: () => import('@/views/collab/message/index.vue'), meta: { title: '消息中心' } },
      { path: 'schedule', name: 'Schedule', component: () => import('@/views/collab/schedule/index.vue'), meta: { title: '日程管理' } },
      { path: 'user', name: 'User', component: () => import('@/views/system/user/index.vue'), meta: { title: '用户管理' } },
      { path: 'dept', name: 'Dept', component: () => import('@/views/system/dept/index.vue'), meta: { title: '部门管理' } },
      { path: 'role', name: 'Role', component: () => import('@/views/system/role/index.vue'), meta: { title: '角色管理' } },
      { path: 'leave-types', name: 'LeaveTypes', component: () => import('@/views/system/attendance/leave-types/index.vue'), meta: { title: '假期类型', roles: ['SYSTEM_ADMIN'] } },
      { path: 'shifts', name: 'Shifts', component: () => import('@/views/system/attendance/shifts/index.vue'), meta: { title: '班次管理', roles: ['SYSTEM_ADMIN'] } },
      { path: 'attendance-groups', name: 'AttendanceGroups', component: () => import('@/views/system/attendance/groups/index.vue'), meta: { title: '考勤组', roles: ['SYSTEM_ADMIN'] } },
      { path: 'holidays', name: 'Holidays', component: () => import('@/views/system/attendance/holidays/index.vue'), meta: { title: '节假日', roles: ['SYSTEM_ADMIN'] } },
      { path: 'attendance-rules', name: 'AttendanceRules', component: () => import('@/views/system/attendance/rules/index.vue'), meta: { title: '考勤规则', roles: ['SYSTEM_ADMIN'] } },
      { path: 'approval-record', name: 'ApprovalRecord', component: () => import('@/views/workflow/approval-record/index.vue'), meta: { title: '审批记录' } },
      { path: 'salary-slip', name: 'SalarySlip', component: () => import('@/views/personal/salary-slip/index.vue'), meta: { title: '工资条' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 用户信息加载队列，确保并发导航时用户信息加载完成后再继续
let userInfoLoading = false
const pendingNavigations = []

const processPendingNavigations = () => {
  while (pendingNavigations.length > 0) {
    const { next, resolve } = pendingNavigations.shift()
    resolve(next())
  }
}

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()

  if (to.path !== '/login' && !userStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 页面刷新后 roles 可能为空，需要先异步获取用户信息
  if (userStore.isLoggedIn && !userStore.roles?.length) {
    // 如果正在加载，将当前导航加入队列
    if (userInfoLoading) {
      return new Promise((resolve) => {
        pendingNavigations.push({ next: () => next(), resolve })
      })
    }

    userInfoLoading = true
    try {
      await userStore.getUserInfo()
    } catch (e) {
      userInfoLoading = false
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
    userInfoLoading = false
  }

  if (to.meta.roles && to.meta.roles.length > 0) {
    const userRoles = userStore.roles || []
    const hasPermission = to.meta.roles.some(role => userRoles.includes(role))
    if (!hasPermission) {
      next('/')
      return
    }
  }

  next()
})

export default router