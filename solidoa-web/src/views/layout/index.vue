<template>
  <div class="layout-container">
    <!-- Skip Link -->
    <a href="#main-content" class="skip-link">跳转到主内容</a>

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: !isMobileMenuOpen }">
      <div class="logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
          </svg>
        </div>
        <span class="logo-text">SolidOA</span>
      </div>

      <nav class="sidebar-nav">
        <!-- 遍历菜单分组 -->
        <div v-for="group in filteredMenuGroups" :key="group.title" class="menu-group">
          <!-- 分组标题（可点击展开/收起） -->
          <div
            class="group-header"
            @click="toggleGroup(group.title)"
          >
            <span class="group-title">{{ group.title }}</span>
            <span class="group-arrow" :class="{ expanded: expandedGroups.includes(group.title) }">
              <el-icon><ArrowRight /></el-icon>
            </span>
          </div>

          <!-- 分组菜单项（可折叠） -->
          <div class="group-items" :class="{ collapsed: !expandedGroups.includes(group.title) }">
            <button
              v-for="item in group.items"
              :key="item.path"
              class="nav-item"
              :class="{ active: currentPath === item.path }"
              @click="navigateTo(item.path)"
            >
              <span class="nav-icon"><el-icon><component :is="item.iconComponent" /></el-icon></span>
              <span class="nav-text">{{ item.name }}</span>
              <div class="nav-indicator"></div>
            </button>
          </div>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="status-indicator">
          <span class="status-dot"></span>
          <span>系统正常</span>
        </div>
      </div>
    </aside>

    <!-- 移动端遮罩 -->
    <div class="sidebar-overlay" v-if="isMobileMenuOpen" @click="isMobileMenuOpen = false" aria-hidden="true"></div>

    <!-- 主内容区 -->
    <main class="main-wrapper">
      <header class="header">
        <div class="header-left">
          <button class="mobile-menu-btn" @click="isMobileMenuOpen = !isMobileMenuOpen">
            <el-icon><Expand /></el-icon>
          </button>
          <div class="page-title">
            <span class="title-text">{{ pageTitle }}</span>
            <span class="title-line"></span>
          </div>
        </div>
        <div class="header-right">
          <span class="header-time">{{ currentTime }}</span>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-dropdown">
              <span class="user-avatar">{{ userInitials }}</span>
              <div class="user-info">
                <span class="username">{{ username }}</span>
                <span class="user-role">{{ userRole }}</span>
              </div>
              <span class="dropdown-arrow"><el-icon><ArrowDown /></el-icon></span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">
                  <el-icon><Lock /></el-icon>
                  修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="main-content" id="main-content">
        <router-view />
      </div>
    </main>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="450px" class="cyber-dialog">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px" class="cyber-form">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" class="cyber-input" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码（6-20位）" class="cyber-input" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChangePassword" :loading="passwordLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTime } from '@/composables/useTime'
import { useUserStore } from '@/stores/user'
import { systemApi } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'
import { HomeFilled, DataLine, DocumentChecked, Money, TrendCharts, Calendar, Check, Memo, User, OfficeBuilding, Setting, ArrowDown, ArrowRight, Message, Avatar, Expand, Stamp, Clock, Connection, Lock, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const { currentTime } = useTime()
const userStore = useUserStore()

const isMobileMenuOpen = ref(false)

// 修改密码相关
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在6-20位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 从userStore获取用户信息
const username = computed(() => userStore.userInfo?.username || userStore.userInfo?.nickname || '未登录')
const userRole = computed(() => {
  const roles = userStore.roles || userStore.userInfo?.roles || []
  return roles.length > 0 ? roles[0].name || roles[0] : '普通用户'
})

// 默认展开的分组
const expandedGroups = ref(['工作台', 'OA审批'])

// 菜单分组配置
const menuGroups = [
  {
    title: '工作台',
    items: [
      { path: '/home', name: '首页', iconComponent: HomeFilled },
      { path: '/dashboard', name: '仪表盘', iconComponent: DataLine },
    ]
  },
  {
    title: 'OA审批',
    items: [
      { path: '/approval', name: '审批管理', iconComponent: DocumentChecked },
      { path: '/expense', name: '报销管理', iconComponent: Money },
      { path: '/leave', name: '请假申请', iconComponent: Calendar },
      { path: '/salary', name: '工资发放审批', iconComponent: Money },
      { path: '/overtime', name: '加班申请', iconComponent: Clock },
      { path: '/business-trip', name: '出差申请', iconComponent: Connection },
      { path: '/go-out', name: '外出申请', iconComponent: Connection },
      { path: '/repair-card', name: '补卡申请', iconComponent: Calendar },
      { path: '/purchase', name: '采购申请', iconComponent: TrendCharts },
      { path: '/stamp', name: '用印申请', iconComponent: Stamp },
      { path: '/cc', name: '抄送管理', iconComponent: Message },
      { path: '/approval-record', name: '审批记录', iconComponent: DocumentChecked },
    ]
  },
  {
    title: '考勤管理',
    items: [
      { path: '/check', name: '考勤打卡', iconComponent: Check },
      { path: '/leave-balance', name: '假期余额', iconComponent: Calendar },
    ]
  },
  {
    title: '协作办公',
    items: [
      { path: '/contact', name: '通讯录', iconComponent: Avatar },
      { path: '/message', name: '消息中心', iconComponent: Message },
      { path: '/schedule', name: '日程管理', iconComponent: Memo },
    ]
  },
  {
    title: '个人',
    items: [
      { path: '/salary-slip', name: '工资条', iconComponent: Money },
    ]
  },
  {
    title: '系统管理',
    adminOnly: true,
    items: [
      { path: '/user', name: '用户管理', iconComponent: User },
      { path: '/dept', name: '部门管理', iconComponent: OfficeBuilding },
      { path: '/role', name: '角色权限', iconComponent: Setting },
      { path: '/shifts', name: '班次管理', iconComponent: Clock },
      { path: '/attendance-groups', name: '考勤组', iconComponent: Check },
      { path: '/holidays', name: '节假日', iconComponent: Calendar },
      { path: '/attendance-rules', name: '考勤规则', iconComponent: Setting },
      { path: '/leave-types', name: '假期类型', iconComponent: Calendar },
    ]
  }
]

// 切换分组展开/收起
const toggleGroup = (title) => {
  const index = expandedGroups.value.indexOf(title)
  if (index > -1) {
    expandedGroups.value.splice(index, 1)
  } else {
    expandedGroups.value.push(title)
  }
}

const currentPath = computed(() => route.path)

// 当前用户角色判断
// 【安全说明】此变量仅用于控制前端菜单的显示/隐藏
// 【重要】真正的权限控制必须在后端API层实现
// 后端必须：1) 校验用户角色 2) 过滤无权限数据 3) 验证操作权限
// 前端权限控制可被DevTools绕过，无法保证系统安全
const isAdmin = computed(() => {
  const roles = userStore.roles || userStore.userInfo?.roles || []
  return roles.some(r => {
    const roleName = typeof r === 'string' ? r : r.name
    return roleName === 'admin' || roleName === 'SYSTEM_ADMIN' || roleName === '管理员' || roleName === '系统管理员'
  })
})

// 过滤菜单：移除管理员专属菜单
// 【安全说明】此过滤逻辑仅用于改善用户体验，不具备安全防护能力
// 【重要】后端API必须基于用户角色返回对应的菜单数据，实现服务端权限控制
const filteredMenuGroups = computed(() => {
  return menuGroups.filter(group => {
    // 如果不是管理员且该分组标记为adminOnly，则隐藏
    if (group.adminOnly && !isAdmin.value) {
      return false
    }
    return true
  })
})

// 扁平化所有菜单项用于查找标题
const flatMenuItems = computed(() => {
  return filteredMenuGroups.value.flatMap(g => g.items)
})

const pageTitle = computed(() => {
  const item = flatMenuItems.value.find(m => m.path === currentPath.value)
  return item?.name || 'SolidOA'
})

const userInitials = computed(() => {
  return username.value.charAt(0).toUpperCase()
})

const navigateTo = (path) => {
  router.push(path)
  isMobileMenuOpen.value = false
}

// 处理用户下拉菜单命令
const handleUserCommand = (command) => {
  if (command === 'changePassword') {
    // 重置表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordDialogVisible.value = true
  } else if (command === 'logout') {
    handleLogout()
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', { type: 'warning' })
    await systemApi.logout()
  } catch {
    // 用户取消或API调用失败，继续清除本地状态
  } finally {
    // 清除本地存储
    localStorage.removeItem('token')
    localStorage.removeItem('access_token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('__token_encoded')
    // 清除用户状态
    userStore.$reset()
    // 跳转到登录页
    router.push('/login')
  }
}

// 提交修改密码
const submitChangePassword = async () => {
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordLoading.value = true
  try {
    await systemApi.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    // 清除登录状态，跳转到登录页
    localStorage.removeItem('token')
    localStorage.removeItem('access_token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('__token_encoded')
    userStore.$reset()
    router.push('/login')
  } catch (error) {
    console.error('修改密码失败', error)
    ElMessage.error(error.message || '修改密码失败，请稍后重试')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped lang="scss">
// 变量 - Soft & Comfortable 主题
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;
$text-muted: #B0B0B0;

// 阴影
$shadow-base: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

// 圆角
$radius-lg: 16px;
$radius-md: 12px;
$radius-sm: 8px;

.layout-container {
  display: flex;
  height: 100vh;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  position: relative;
  overflow: hidden;
}

.skip-link {
  position: absolute;
  left: -9999px;
  z-index: 999;
  padding: 1rem;
  background: $primary;
  color: white;
  text-decoration: none;
  &:focus {
    left: 0;
  }
}

// 侧边栏
.sidebar {
  width: 240px;
  height: 100vh;
  background: $bg-card;
  border-right: 1px solid $border-color;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 100;
  transition: transform 0.3s ease;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1.25rem 1.25rem 1rem;
  border-bottom: 1px solid $border-color;
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: $primary;
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 600;
  color: $text-primary;
}

// 侧边栏导航
.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0;
}

// 菜单分组
.menu-group {
  margin-bottom: 0.25rem;
}

// 分组标题头（可点击）
.group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.625rem 1rem;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;

  &:hover {
    background: rgba($primary, 0.04);
  }
}

.group-title {
  font-size: 0.6875rem;
  font-weight: 600;
  color: $text-muted;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.group-arrow {
  color: $text-muted;
  font-size: 0.75rem;
  transition: transform 0.2s ease;

  &.expanded {
    transform: rotate(90deg);
  }
}

// 分组菜单项容器
.group-items {
  overflow: hidden;
  max-height: 500px;
  transition: max-height 0.3s ease, opacity 0.2s ease;

  &.collapsed {
    max-height: 0;
    opacity: 0;
  }
}

// 导航项
.nav-item {
  width: calc(100% - 1rem);
  margin: 0.125rem 0.5rem;
  padding: 0.5rem 0.875rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  text-align: left;
  color: $text-secondary;
  font-size: 0.875rem;
  position: relative;

  &:hover {
    background: rgba($primary, 0.06);
    color: $text-primary;
  }

  &.active {
    background: rgba($primary, 0.1);
    color: $primary;
    font-weight: 500;

    .nav-indicator {
      display: block;
    }
  }
}

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
}

.nav-text {
  flex: 1;
}

.nav-indicator {
  display: none;
  width: 6px;
  height: 6px;
  background: $primary;
  border-radius: 50%;
}

// 页脚
.sidebar-footer {
  padding: 1rem 1.25rem;
  border-top: 1px solid $border-color;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: $text-secondary;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: $success;
  border-radius: 50%;
}

// 遮罩层
.sidebar-overlay {
  display: none;
}

// 主内容区
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// 头部
.header {
  height: 64px;
  padding: 0 1.5rem;
  background: $bg-card;
  border-bottom: 1px solid $border-color;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.mobile-menu-btn {
  display: none;
  padding: 0.5rem;
  background: transparent;
  border: none;
  border-radius: $radius-sm;
  cursor: pointer;
  color: $text-secondary;
  font-size: 1.25rem;

  &:hover {
    background: $bg-primary;
  }
}

.page-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.title-text {
  font-size: 1rem;
  font-weight: 500;
  color: $text-primary;
}

.title-line {
  width: 4px;
  height: 16px;
  background: $primary;
  border-radius: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.header-time {
  font-size: 0.875rem;
  color: $text-secondary;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.375rem 0.75rem 0.375rem 0.375rem;
  background: $bg-primary;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  outline: none;

  &:hover {
    background: darken($bg-primary, 2%);
  }
}

.user-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, $primary, lighten($primary, 15%));
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 600;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 0.875rem;
  font-weight: 500;
  color: $text-primary;
}

.user-role {
  font-size: 0.6875rem;
  color: $text-secondary;
}

.dropdown-arrow {
  color: $text-secondary;
  font-size: 0.75rem;
}

// 主内容
.main-content {
  flex: 1;
  overflow-y: auto;
  background: $bg-primary;
}

// 响应式
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    transform: translateX(-100%);

    &:not(.collapsed) {
      transform: translateX(0);
    }
  }

  .sidebar-overlay {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.3);
    z-index: 99;
  }

  .mobile-menu-btn {
    display: flex;
  }

  .user-info {
    display: none;
  }

  .header-time {
    display: none;
  }
}

// 修改密码弹窗样式
.cyber-dialog {
  :deep(.el-dialog) {
    background: $bg-card;
    border-radius: $radius-lg;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);

    .el-dialog__header {
      border-bottom: 1px solid $border-color;
      padding: 20px 24px;
      margin: 0;

      .el-dialog__title {
        color: $text-primary;
        font-weight: 600;
      }
    }

    .el-dialog__body {
      padding: 30px 24px;
    }

    .el-dialog__footer {
      border-top: 1px solid $border-color;
      padding: 16px 24px;
    }
  }

  .cyber-form {
    :deep(.el-form-item__label) {
      color: $text-secondary;
    }
  }

  .cyber-input {
    :deep(.el-input__wrapper) {
      background: $bg-card;
      border: 1px solid $border-color;
      border-radius: $radius-md;
      box-shadow: none;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      padding: 8px 16px;

      &:hover {
        border-color: $primary;
      }

      &.is-focus {
        border-color: $primary;
        box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.1);
      }
    }

    :deep(.el-input__inner) {
      color: $text-primary;

      &::placeholder {
        color: $text-secondary;
      }
    }
  }
}
</style>