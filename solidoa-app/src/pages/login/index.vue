<template>
  <view class="login-container">
    <view class="login-header">
      <image class="logo" src="/static/logo.png" mode="aspectFit" />
      <text class="title">SolidOA</text>
      <text class="subtitle">企业办公自动化系统</text>
    </view>

    <view class="login-form">
      <view class="form-item">
        <text class="label">用户名</text>
        <input
          v-model="form.username"
          type="text"
          placeholder="请输入用户名"
          class="input"
        />
      </view>

      <view class="form-item">
        <text class="label">密码</text>
        <input
          v-model="form.password"
          type="password"
          placeholder="请输入密码"
          class="input"
        />
      </view>

      <button class="login-btn" :loading="loading" @click="handleLogin">登 录</button>

      <view class="login-tip">
        <text>默认账号: admin / admin123</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const handleLogin = async () => {
  if (!form.username) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!form.password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    uni.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => {
      uni.switchTab({ url: '/pages/index/index' })
    }, 1000)
  } catch (e) {
    console.error('登录失败', e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 50rpx;
}

.login-header {
  text-align: center;
  margin-bottom: 80rpx;

  .logo {
    width: 160rpx;
    height: 160rpx;
    border-radius: 40rpx;
    margin-bottom: 30rpx;
  }

  .title {
    display: block;
    font-size: 56rpx;
    font-weight: bold;
    color: #fff;
    margin-bottom: 16rpx;
  }

  .subtitle {
    display: block;
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.8);
  }
}

.login-form {
  width: 100%;
  background: #fff;
  border-radius: 24rpx;
  padding: 50rpx 40rpx;

  .form-item {
    margin-bottom: 40rpx;

    .label {
      display: block;
      font-size: 28rpx;
      color: #303133;
      margin-bottom: 16rpx;
    }

    .input {
      height: 88rpx;
      background: #f5f7fa;
      border-radius: 16rpx;
      padding: 0 30rpx;
      font-size: 28rpx;
    }
  }

  .login-btn {
    width: 100%;
    height: 88rpx;
    background: #409eff;
    color: #fff;
    font-size: 32rpx;
    border-radius: 16rpx;
    margin-top: 20rpx;

    &::after {
      border: none;
    }
  }

  .login-tip {
    text-align: center;
    margin-top: 30rpx;
    font-size: 24rpx;
    color: #909399;
  }
}
</style>