<template>
  <el-card>
    <template #header>
      <span>考勤打卡</span>
    </template>

    <div class="check-container">
      <div class="check-card">
        <div class="time">{{ currentTime }}</div>
        <div class="date">{{ currentDate }}</div>
        <el-button type="primary" size="large" @click="handleCheck">
          {{ checkType === 'SIGN_IN' ? '签到' : '签退' }}
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { attendanceApi } from '@/api/attendance'
import { ElMessage } from 'element-plus'

const currentTime = ref('')
const currentDate = ref('')
const checkType = ref('SIGN_IN')

let timer = null

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { weekday: 'long', month: 'long', day: 'numeric' })
}

const handleCheck = async () => {
  try {
    await attendanceApi.check({
      checkType: checkType.value,
      location: '办公室'
    })
    ElMessage.success(checkType.value === 'SIGN_IN' ? '签到成功' : '签退成功')
    checkType.value = checkType.value === 'SIGN_IN' ? 'SIGN_OUT' : 'SIGN_IN'
  } catch (error) {
    console.error('打卡失败', error)
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.check-container {
  display: flex;
  justify-content: center;

  .check-card {
    text-align: center;
    padding: 40px;

    .time {
      font-size: 48px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 8px;
    }

    .date {
      font-size: 16px;
      color: #909399;
      margin-bottom: 30px;
    }
  }
}
</style>