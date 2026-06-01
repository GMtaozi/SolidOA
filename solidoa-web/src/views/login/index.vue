<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="grid-pattern"></div>
    </div>

    <div class="login-wrapper">
      <!-- 左侧品牌区 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="brand-card">
            <div class="brand-logo-wrap">
              <img src="/logo.png" alt="SolidOA" class="brand-logo" />
            </div>
            <div class="brand-text">
              <h1 class="brand-title">SolidOA</h1>
              <p class="brand-desc">企业办公自动化系统</p>
            </div>
          </div>

          <div class="brand-features">
            <div class="feature-item">
              <span class="feature-icon">⚡</span>
              <span>高效审批</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🔒</span>
              <span>安全可靠</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📊</span>
              <span>数据洞察</span>
            </div>
          </div>

          <div class="brand-footer">
            <p>专注企业办公自动化</p>
            <p>让工作更高效</p>
          </div>
        </div>
      </div>

      <!-- 右侧登录区 -->
      <div class="login-section">
        <div class="login-box">
          <div class="login-header">
            <div class="login-icon">
              <span>👋</span>
            </div>
            <h2>欢迎回来</h2>
            <p>请登录您的账号</p>
          </div>

          <el-form :model="form" class="login-form" @submit.prevent="handleLogin">
            <div class="form-item" style="animation-delay: 0.1s">
              <label>用户名</label>
              <div class="input-wrapper">
                <span class="input-icon">👤</span>
                <el-input
                  v-model="form.username"
                  placeholder="请输入用户名"
                  class="cyber-input"
                  clearable
                />
              </div>
            </div>

            <div class="form-item" style="animation-delay: 0.2s">
              <label>密码</label>
              <div class="input-wrapper">
                <span class="input-icon">🔒</span>
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  class="cyber-input"
                  show-password
                />
              </div>
            </div>

            
            <div class="form-item" style="animation-delay: 0.3s">
              <button type="submit" class="cyber-btn" :class="{ loading: loading }">
                <span class="btn-text">{{ loading ? '登录中...' : '登 录' }}</span>
                <span class="btn-shine"></span>
              </button>
            </div>
          </el-form>

          <div class="login-footer">
            <p>SolidOA © {{ new Date().getFullYear() }} 企业内部系统</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  password: ''
})

const remember = ref(false)
const loading = ref(false)

// 防暴力破解：记录连续失败次数
const loginAttempts = ref(0)
const MAX_ATTEMPTS = 5 // 最多尝试次数
const lockoutTime = ref(0) // 锁定剩余秒数

const handleLogin = async () => {
  // 防暴力破解检查
  if (lockoutTime.value > 0) {
    ElMessage.warning(`登录已被锁定，请 ${lockoutTime.value} 秒后重试`)
    return
  }

  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  // 超过尝试次数时提示
  if (loginAttempts.value >= MAX_ATTEMPTS) {
    lockoutTime.value = 60
    ElMessage.error('登录失败次数过多，请 60 秒后重试')
    const timer = setInterval(() => {
      lockoutTime.value--
      if (lockoutTime.value <= 0) {
        clearInterval(timer)
        loginAttempts.value = 0
      }
    }, 1000)
    return
  }

  loading.value = true
  try {
    await userStore.login(form.value.username, form.value.password)
    ElMessage.success('登录成功')
    loginAttempts.value = 0 // 重置失败计数
    router.push('/home')
  } catch (error) {
    loginAttempts.value++
    if (loginAttempts.value >= 3) {
      ElMessage.warning(`登录失败，请注意：还剩 ${MAX_ATTEMPTS - loginAttempts.value} 次尝试机会`)
    } else {
      ElMessage.error(error.message || '登录失败，请检查网络连接')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
// 变量 - 柔和冷色系
$bg-primary: #f0f4f8;
$bg-warm: #eef4f7;
$bg-card: #ffffff;
$primary: #3B82F6;
$primary-light: rgba(59, 130, 246, 0.1);
$primary-medium: rgba(59, 130, 246, 0.18);
$secondary: #10B981;
$accent: #F59E0B;
$text-primary: #1E3A5F;
$text-secondary: #64748B;
$text-light: #FFFFFF;
$text-muted: #94A3B8;
$border-color: #dce5f0;

// 阴影
$shadow-base: 0 10px 25px -5px rgba(59, 130, 246, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
$shadow-hover: 0 20px 35px -12px rgba(59, 130, 246, 0.15);
$shadow-primary: 0 4px 20px rgba(59, 130, 246, 0.3);

// 圆角
$radius-lg: 24px;
$radius-md: 16px;
$radius-sm: 12px;
$radius-full: 50px;

.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, $bg-primary 0%, $bg-warm 100%);
  position: relative;
  overflow: hidden;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
}

// 背景装饰
.bg-decoration {
  position: fixed;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(59, 130, 246, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(59, 130, 246, 0.04) 1px, transparent 1px);
  background-size: 60px 60px;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  25% { transform: translate(10px, -20px) rotate(2deg); }
  50% { transform: translate(-5px, -40px) rotate(-1deg); }
  75% { transform: translate(-15px, -20px) rotate(1deg); }
}

// 布局
.login-wrapper {
  display: flex;
  min-height: 100vh;
  position: relative;
  z-index: 1;
}

// 左侧品牌区 - 浅色舒适背景
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef4f7 100%);
  position: relative;
}

.brand-content {
  max-width: 480px;
  width: 100%;
  animation: fadeInUp 0.8s ease-out;
  display: flex;
  flex-direction: column;
  gap: 40px;
}

// Logo卡片
.brand-card {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 36px 40px;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-base;
  animation: fadeInUp 0.6s ease-out backwards;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, $primary, $secondary, $accent);
    background-size: 200% 100%;
    animation: shimmer 3s linear infinite;
  }
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.brand-logo-wrap {
  width: 96px;
  height: 96px;
  flex-shrink: 0;
  padding: 10px;
  background: linear-gradient(135deg, $primary-light, rgba(59, 130, 246, 0.05));
  border-radius: $radius-md;
  border: 2px solid rgba(59, 130, 246, 0.15);
}

.brand-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.brand-text {
  text-align: left;
}

.brand-title {
  font-size: 42px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
  letter-spacing: -1px;
}

.brand-desc {
  font-size: 16px;
  color: $text-secondary;
}

// 功能特点
.brand-features {
  display: flex;
  gap: 20px;
  animation: fadeInUp 0.6s ease-out 0.15s backwards;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  background: $bg-card;
  border-radius: $radius-sm;
  font-size: 15px;
  color: $text-primary;
  box-shadow: $shadow-base;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-3px);
    box-shadow: $shadow-hover;
  }
}

.feature-icon {
  font-size: 20px;
}

.brand-footer {
  animation: fadeInUp 0.6s ease-out 0.3s backwards;

  p {
    font-size: 15px;
    color: $text-secondary;
    line-height: 1.8;

    &:first-child {
      font-weight: 500;
      color: $text-primary;
    }
  }
}

// 右侧登录区
.login-section {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px;
  background: $bg-card;
  position: relative;
  box-shadow: -10px 0 40px rgba(0, 0, 0, 0.05);
}

.login-box {
  width: 100%;
  max-width: 380px;
  animation: fadeInUp 0.8s ease-out 0.2s backwards;
}

.login-header {
  margin-bottom: 40px;
  text-align: center;
}

.login-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, $primary, #2563EB);
  border-radius: $radius-lg;
  font-size: 36px;
  box-shadow: $shadow-primary;
  animation: float 3s ease-in-out infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.login-header h2 {
  font-size: 28px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8px;
}

.login-header p {
  font-size: 14px;
  color: $text-secondary;
}

.login-form {
  .form-item {
    margin-bottom: 20px;

    label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
      margin-bottom: 8px;
    }
  }
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  background: $bg-card;
  border: 2px solid #E2E8F0;
  border-radius: $radius-md;
  height: 54px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:focus-within {
    border-color: $primary;
    box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);

    .input-icon {
      color: $primary;
      transform: scale(1.1);
    }
  }
}

.input-icon {
  position: absolute;
  left: 16px;
  font-size: 20px;
  z-index: 1;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

:deep(.cyber-input) {
  background: transparent;
  border: none;
  box-shadow: none;

  .el-input__wrapper {
    background: transparent;
    box-shadow: none;
    padding: 16px 16px 16px 50px;
    height: 54px;
    border-radius: $radius-md;
  }

  .el-input__inner {
    color: $text-primary;
    font-size: 15px;

    &::placeholder {
      color: $text-secondary;
    }
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: $text-secondary;
  cursor: pointer;

  input {
    width: 18px;
    height: 18px;
    accent-color: $primary;
  }
}

.forgot-link {
  font-size: 14px;
  color: $primary;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    opacity: 0.8;
    text-decoration: underline;
  }
}

.cyber-btn {
  width: 100%;
  padding: 16px 32px;
  background: linear-gradient(135deg, $primary, #2563EB);
  border: none;
  border-radius: $radius-full;
  color: #ffffff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  box-shadow: $shadow-primary;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 30px rgba(59, 130, 246, 0.5);
  }

  &:active {
    transform: scale(0.98);
  }

  &.loading {
    pointer-events: none;
    opacity: 0.8;
  }
}

.btn-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(255, 255, 255, 0.35),
    transparent
  );
  animation: btnShine 3s ease-in-out infinite;
}

@keyframes btnShine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}

.login-footer {
  margin-top: 40px;
  text-align: center;

  p {
    font-size: 13px;
    color: $text-muted;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 响应式
@media (max-width: 1024px) {
  .login-wrapper {
    flex-direction: column;
  }

  .brand-section {
    padding: 60px 24px 40px;
    background: linear-gradient(135deg, #f8fbff 0%, #e8eef4 100%);
  }

  .brand-card {
    padding: 28px 32px;
  }

  .brand-logo-wrap {
    width: 80px;
    height: 80px;
  }

  .brand-title {
    font-size: 34px;
  }

  .brand-features {
    gap: 12px;
  }

  .feature-item {
    padding: 12px 18px;
    font-size: 14px;
  }

  .brand-footer {
    display: none;
  }

  .login-section {
    width: 100%;
    padding: 40px 24px 60px;
    background: $bg-primary;
  }
}

@media (max-width: 480px) {
  .login-box {
    padding: 0 12px;
  }

  .login-header h2 {
    font-size: 24px;
  }

  .brand-features {
    flex-wrap: wrap;
  }
}
</style>