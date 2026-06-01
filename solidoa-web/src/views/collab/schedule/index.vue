<template>
  <div class="schedule-container">
    <div class="schedule-wrapper">
      <!-- 日历面板 -->
      <div class="calendar-panel card">
        <div class="panel-header">
          <span class="panel-icon">▣</span>
          <span>日程管理</span>
          <div class="header-actions">
            <button class="btn" @click="handleAdd">
              <span class="btn-text">+ 新增日程</span>
            </button>
          </div>
        </div>

        <div class="calendar-wrapper">
          <el-calendar v-model="currentDate" class="calendar">
            <template #date-cell="{ data }">
              <div class="calendar-cell" :class="{ selected: isSelected(data.day), today: isToday(data.day) }">
                <div class="date-header">
                  <span class="date-num">{{ data.day.split('-')[2] }}</span>
                  <span v-if="isToday(data.day)" class="today-tag">TODAY</span>
                </div>
                <div v-if="getSchedules(data.day).length" class="schedule-list">
                  <div
                    v-for="item in getSchedules(data.day)"
                    :key="item.id"
                    class="schedule-item"
                    :style="{
                      backgroundColor: item.color + '33',
                      borderLeft: '3px solid ' + item.color
                    }"
                    @click.stop="handleView(item)"
                  >
                    <span class="schedule-time">{{ item.startTime }}</span>
                    <span class="schedule-title" v-html="escapeHtml(item.title)"></span>
                  </div>
                </div>
              </div>
            </template>
          </el-calendar>
        </div>
      </div>

      <!-- 日程列表 -->
      <div class="schedule-list-panel card">
        <div class="panel-header">
          <span class="panel-icon">◇</span>
          <span>日程列表</span>
          <span class="schedule-count">{{ schedules.length }} 个日程</span>
        </div>

        <div class="schedule-timeline">
          <div
            v-for="schedule in schedules"
            :key="schedule.id"
            class="schedule-card"
            :style="{
              borderLeftColor: schedule.color
            }"
            @click="handleView(schedule)"
          >
            <div class="schedule-color-bar" :style="{ backgroundColor: schedule.color }"></div>
            <div class="schedule-content">
              <div class="schedule-header">
                <h4 class="schedule-title" v-html="escapeHtml(schedule.title)"></h4>
                <span class="schedule-status" :class="getStatusClass(schedule)">{{ getStatusLabel(schedule) }}</span>
              </div>
              <div class="schedule-meta">
                <span class="meta-item">
                  <span class="meta-icon">◈</span>
                  {{ schedule.date }} {{ schedule.startTime }} - {{ schedule.endTime }}
                </span>
                <span class="meta-item">
                  <span class="meta-icon">◎</span>
                  {{ schedule.location }}
                </span>
              </div>
              <div class="schedule-desc">{{ schedule.content }}</div>
            </div>
            <div class="schedule-actions">
              <button class="action-btn" @click.stop="handleEdit(schedule)">
                <span class="btn-icon">✎</span>
              </button>
              <button class="action-btn danger" @click.stop="handleDelete(schedule)">
                <span class="btn-icon">✕</span>
              </button>
            </div>
          </div>

          <div v-if="schedules.length === 0" class="empty-state">
            <div class="empty-icon">◇</div>
            <div class="empty-text">暂无日程安排</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      class="dialog"
    >
      <el-form ref="formRef" :model="form" label-width="80px" class="form">
        <el-form-item label="日程标题">
          <el-input v-model="form.title" class="input" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="form.startTime" type="datetime" class="input" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="form.endTime" type="datetime" class="input" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" class="input" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.content" type="textarea" :rows="3" class="input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button class="btn secondary" @click="dialogVisible = false">取消</el-button>
          <el-button class="btn" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '@/api'

const currentDate = ref(new Date())
const dialogVisible = ref(false)
const dialogTitle = ref('新增日程')
const formRef = ref()
const editingId = ref(null)
const form = reactive({
  title: '',
  startTime: null,
  endTime: null,
  location: '',
  content: ''
})

const schedules = ref([
  { id: 1, title: '团队周会', startTime: '09:00', endTime: '10:00', location: '会议室A', color: '#60A5FA', date: '2026-05-26', content: '讨论本周工作进度和计划' },
  { id: 2, title: '项目评审', startTime: '14:00', endTime: '16:00', location: '会议室B', color: '#A78BFA', date: '2026-05-27', content: '项目进度汇报和风险评估' },
  { id: 3, title: '技术培训', startTime: '10:00', endTime: '12:00', location: '培训室', color: '#34D399', date: '2026-05-28', content: '新技术分享和实践演练' }
])

const selectedDate = ref('')

// 格式化日期时间
const formatDateTime = (date) => {
  if (!date) return ''
  if (typeof date === 'string') return date
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

// 加载日程列表
const fetchSchedules = async () => {
  try {
    const res = await systemApi.getScheduleAll()
    if (res.data) {
      schedules.value = res.data
    } else {
      schedules.value = res || []
    }
  } catch (error) {
    console.error('获取日程列表失败:', error)
  }
}

const getSchedules = (date) => {
  return schedules.value.filter(s => s.date === date)
}

const isSelected = (date) => {
  return selectedDate.value === date
}

const isToday = (date) => {
  const today = new Date()
  const [year, month, day] = date.split('-')
  return (
    today.getFullYear() === parseInt(year) &&
    today.getMonth() + 1 === parseInt(month) &&
    today.getDate() === parseInt(day)
  )
}

const getStatusClass = (schedule) => {
  const today = new Date()
  const scheduleDate = new Date(schedule.date)
  if (scheduleDate < today) return 'status-passed'
  if (scheduleDate.toDateString() === today.toDateString()) return 'status-today'
  return 'status-upcoming'
}

const getStatusLabel = (schedule) => {
  const status = getStatusClass(schedule)
  const map = {
    'status-passed': '已结束',
    'status-today': '进行中',
    'status-upcoming': '即将开始'
  }
  return map[status]
}

const handleAdd = () => {
  dialogTitle.value = '新增日程'
  editingId.value = null
  Object.assign(form, { title: '', startTime: null, endTime: null, location: '', content: '' })
  dialogVisible.value = true
}

const handleView = (item) => {
  ElMessage.info('查看日程: ' + item.title)
}

const handleEdit = (item) => {
  dialogTitle.value = '编辑日程'
  editingId.value = item.id
  Object.assign(form, {
    title: item.title,
    startTime: item.startTime,
    endTime: item.endTime,
    location: item.location,
    content: item.content
  })
  dialogVisible.value = true
}

const handleDelete = (item) => {
  schedules.value = schedules.value.filter(s => s.id !== item.id)
  ElMessage.success('删除成功')
}

// HTML转义防止XSS攻击
const escapeHtml = (str) => {
  if (!str) return ''
  const div = document.createElement('div')
  div.textContent = str
  return div.innerHTML
}

const handleSubmit = async () => {
  try {
    const data = {
      title: form.title,
      startTime: formatDateTime(form.startTime),
      endTime: formatDateTime(form.endTime),
      location: form.location,
      content: form.content
    }

    if (editingId.value) {
      await systemApi.updateSchedule(editingId.value, data)
      ElMessage.success('更新成功')
    } else {
      await systemApi.createSchedule(data)
      ElMessage.success('保存成功')
    }

    dialogVisible.value = false
    await fetchSchedules()
  } catch (error) {
    console.error('保存日程失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  }
}
</script>

<style scoped lang="scss">
// 柔和舒适风格配色变量
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.schedule-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;

  .schedule-wrapper {
    display: grid;
    grid-template-columns: 1fr 400px;
    gap: 20px;
  }

  .card {
    background: $bg-card;
    border-radius: 16px;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
    transition: box-shadow 0.3s ease;
    overflow: hidden;

    &:hover {
      box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
    }
  }

  .panel-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px 20px;
    border-bottom: 1px solid $border-color;
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;

    .panel-icon {
      color: $primary;
      font-size: 18px;
    }

    .schedule-count {
      margin-left: auto;
      font-size: 12px;
      color: $text-secondary;
      font-weight: normal;
    }

    .header-actions {
      margin-left: auto;
    }
  }

  .btn {
    padding: 8px 16px;
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: 40px;
    color: $text-primary;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

    &:hover {
      border-color: $primary;
      color: $primary;
      box-shadow: 0 4px 12px rgba(96, 165, 250, 0.15);
    }

    &.secondary {
      border-color: $border-color;
      color: $text-secondary;

      &:hover {
        border-color: $primary;
        color: $primary;
      }
    }
  }

  // 日历面板
  .calendar-panel {
    .calendar-wrapper {
      padding: 16px;
    }

    :deep(.calendar) {
      .el-calendar__header {
        padding: 12px 16px;
        border-bottom: 1px solid $border-color;

        .el-calendar__title {
          color: $text-primary;
          font-size: 16px;
          font-weight: 600;
        }

        .el-calendar__body {
          padding: 0;

          .el-calendar-table {
            td {
              border: none;

              &.is-selected {
                background: rgba(96, 165, 250, 0.1);
              }

              &.is-today {
                .el-calendar-day {
                  .date-num {
                    color: $primary;
                    font-weight: bold;
                  }
                }
              }
            }
          }
        }
      }
    }

    .calendar-cell {
      min-height: 80px;
      padding: 8px;
      background: $bg-primary;
      border-radius: 12px;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      cursor: pointer;

      &:hover {
        background: #fcfaf7;
      }

      &.selected {
        background: rgba(96, 165, 250, 0.1);
      }

      .date-header {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-bottom: 6px;

        .date-num {
          font-size: 14px;
          font-weight: 600;
          color: $text-primary;
        }

        .today-tag {
          font-size: 8px;
          padding: 1px 4px;
          background: $primary;
          color: #ffffff;
          border-radius: 8px;
          font-weight: bold;
        }
      }

      .schedule-list {
        .schedule-item {
          display: flex;
          flex-direction: column;
          padding: 4px 6px;
          margin-bottom: 4px;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

          &:hover {
            background: rgba(96, 165, 250, 0.1);
          }

          .schedule-time {
            font-size: 10px;
            color: $text-secondary;
          }

          .schedule-title {
            font-size: 11px;
            color: $text-primary;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
          }
        }
      }
    }
  }

  // 日程列表面板
  .schedule-list-panel {
    max-height: calc(100vh - 40px);
    overflow: hidden;
    display: flex;
    flex-direction: column;

    .schedule-timeline {
      flex: 1;
      overflow-y: auto;
      padding: 16px;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: $bg-primary;
        border-radius: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background: $border-color;
        border-radius: 3px;
      }
    }

    .schedule-card {
      position: relative;
      display: flex;
      gap: 12px;
      padding: 16px;
      margin-bottom: 12px;
      background: $bg-primary;
      border-radius: 16px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

      &:hover {
        background: #fcfaf7;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);

        .schedule-actions {
          opacity: 1;
        }
      }

      .schedule-color-bar {
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 3px;
        border-radius: 16px 0 0 16px;
      }

      .schedule-content {
        flex: 1;
        min-width: 0;

        .schedule-header {
          display: flex;
          justify-content: space-between;
          align-items: flex-start;
          margin-bottom: 8px;

          .schedule-title {
            font-size: 15px;
            font-weight: 600;
            color: $text-primary;
            margin: 0;
          }

          .schedule-status {
            font-size: 10px;
            padding: 2px 8px;
            border-radius: 12px;
            background: rgba(96, 165, 250, 0.1);
            color: $primary;

            &.status-passed {
              background: rgba(156, 163, 175, 0.1);
              color: $text-secondary;
            }

            &.status-today {
              background: rgba(52, 211, 153, 0.1);
              color: $success;
            }

            &.status-upcoming {
              background: rgba(96, 165, 250, 0.1);
              color: $primary;
            }
          }
        }

        .schedule-meta {
          display: flex;
          flex-wrap: wrap;
          gap: 12px;
          margin-bottom: 8px;

          .meta-item {
            display: flex;
            align-items: center;
            gap: 4px;
            font-size: 12px;
            color: $text-secondary;

            .meta-icon {
              color: $primary;
              font-size: 10px;
            }
          }
        }

        .schedule-desc {
          font-size: 12px;
          color: $text-secondary;
          line-height: 1.5;
        }
      }

      .schedule-actions {
        display: flex;
        flex-direction: column;
        gap: 8px;
        opacity: 0;
        transition: opacity 0.3s;

        .action-btn {
          width: 28px;
          height: 28px;
          display: flex;
          align-items: center;
          justify-content: center;
          background: $bg-card;
          border: 1px solid $border-color;
          border-radius: 50%;
          color: $text-secondary;
          cursor: pointer;
          transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

          .btn-icon {
            font-size: 12px;
          }

          &:hover {
            border-color: $primary;
            color: $primary;
            box-shadow: 0 2px 8px rgba(96, 165, 250, 0.15);
          }

          &.danger:hover {
            border-color: #FBBF24;
            color: #FBBF24;
            box-shadow: 0 2px 8px rgba(251, 191, 36, 0.15);
          }
        }
      }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      color: $text-secondary;

      .empty-icon {
        font-size: 48px;
        color: $border-color;
        margin-bottom: 16px;
      }

      .empty-text {
        font-size: 14px;
      }
    }
  }

  // 对话框样式
  :deep(.dialog) {
    --el-dialog-bg-color: #{$bg-card};
    --el-dialog-border-radius: 16px;

    .el-dialog {
      border-radius: 16px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);
    }

    .el-dialog__header {
      padding: 16px 20px;
      border-bottom: 1px solid $border-color;

      .el-dialog__title {
        color: $text-primary;
        font-weight: 600;
      }
    }

    .el-dialog__body {
      padding: 20px;
    }

    .form {
      :deep(.input) {
        .el-input__wrapper {
          background: $bg-card;
          border: 1px solid $border-color;
          border-radius: 12px;
          box-shadow: none;

          &:hover, &:focus {
            border-color: $primary;
            box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.1);
          }
        }

        .el-input__inner {
          color: $text-primary;
        }
      }
    }

    .dialog-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }
}
</style>