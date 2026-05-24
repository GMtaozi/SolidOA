<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span>SolidOA</span>
      </div>

      <el-menu
        :default-active="route.path"
        router
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/home">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/system/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>

        <el-menu-item index="/workflow/leave">
          <el-icon><Document /></el-icon>
          <span>请假申请</span>
        </el-menu-item>

        <el-menu-item index="/collab/messages">
          <el-icon><ChatDotRound /></el-icon>
          <span>消息中心</span>
        </el-menu-item>

        <el-menu-item index="/finance/expense">
          <el-icon><Money /></el-icon>
          <span>报销管理</span>
        </el-menu-item>

        <el-menu-item index="/attendance/check">
          <el-icon><Timer /></el-icon>
          <span>考勤打卡</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="page-title">{{ route.meta.title }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">{{ userStore.userInfo?.realName || '用户' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    font-weight: bold;
    color: #fff;
    background-color: #263445;
  }

  .sidebar-menu {
    border-right: none;
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);

  .page-title {
    font-size: 16px;
    font-weight: 500;
    color: #303133;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;

    .username {
      color: #606266;
    }
  }
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>