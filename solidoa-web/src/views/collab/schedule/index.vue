<template>
  <div class="schedule-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>日程管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增日程
          </el-button>
        </div>
      </template>

      <el-calendar v-model="currentDate">
        <template #date-cell="{ data }">
          <div class="calendar-cell">
            <div class="date-num">{{ data.day.split('-')[2] }}</div>
            <div v-if="getSchedules(data.day).length" class="schedule-list">
              <div
                v-for="item in getSchedules(data.day)"
                :key="item.id"
                class="schedule-item"
                :style="{ backgroundColor: item.color }"
                @click="handleView(item)"
              >
                {{ item.title }}
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="日程标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.content" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

const currentDate = ref(new Date())
const dialogVisible = ref(false)
const dialogTitle = ref('新增日程')
const formRef = ref()
const form = reactive({
  title: '',
  startTime: '',
  endTime: '',
  location: '',
  content: ''
})

const schedules = ref([
  { id: 1, title: '团队周会', startTime: '09:00', endTime: '10:00', location: '会议室A', color: '#409eff', date: '2026-05-26' },
  { id: 2, title: '项目评审', startTime: '14:00', endTime: '16:00', location: '会议室B', color: '#67c23a', date: '2026-05-27' }
])

const getSchedules = (date) => {
  return schedules.value.filter(s => s.date === date)
}

const handleAdd = () => {
  dialogTitle.value = '新增日程'
  Object.assign(form, { title: '', startTime: '', endTime: '', location: '', content: '' })
  dialogVisible.value = true
}

const handleView = (item) => {
  ElMessage.info('查看日程: ' + item.title)
}

const handleSubmit = () => {
  ElMessage.success('保存成功')
  dialogVisible.value = false
}
</script>

<style scoped lang="scss">
.schedule-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .calendar-cell {
    min-height: 60px;

    .date-num {
      font-weight: bold;
      margin-bottom: 4px;
    }

    .schedule-list {
      .schedule-item {
        font-size: 12px;
        color: #fff;
        padding: 2px 4px;
        border-radius: 2px;
        margin-bottom: 2px;
        cursor: pointer;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }
}
</style>