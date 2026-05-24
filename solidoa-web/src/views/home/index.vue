<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #409eff">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">12</div>
            <div class="stat-label">待审批</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #67c23a">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">5</div>
            <div class="stat-label">未读消息</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #e6a23c">
            <el-icon><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">28</div>
            <div class="stat-label">本月考勤</div>
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f56c6c">
            <el-icon><Money /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">3</div>
            <div class="stat-label">报销待审</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="router.push('/workflow/leave')">
              <el-icon><Plus /></el-icon>
              请假申请
            </el-button>
            <el-button type="success" @click="router.push('/attendance/check')">
              <el-icon><Timer /></el-icon>
              考勤打卡
            </el-button>
            <el-button type="warning" @click="router.push('/finance/expense')">
              <el-icon><Money /></el-icon>
              报销申请
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <span>最新消息</span>
          </template>
          <el-empty description="暂无消息" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped lang="scss">
.home-container {
  .stat-card {
    display: flex;
    align-items: center;
    padding: 20px;
    background: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);

    .stat-icon {
      width: 60px;
      height: 60px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #fff;
      margin-right: 16px;
    }

    .stat-info {
      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .quick-actions {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}
</style>