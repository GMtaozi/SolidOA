import request from '@/utils/request'

export const api = {
  // 登录
  login: (data) => request.post('/api/v1/auth/login', data),

  // 请假
  getLeaveList: (params) => request.get('/api/v1/workflow/leave', { params }),
  createLeave: (data) => request.post('/api/v1/workflow/leave', data),
  approveLeave: (id, data) => request.post(`/api/v1/workflow/leave/${id}/approve`, data),

  // 考勤
  check: (data) => request.post('/api/v1/attendance/check', data),
  getAttendanceRecords: (params) => request.get('/api/v1/attendance/records', { params }),
  getAttendanceSummary: (params) => request.get('/api/v1/attendance/summary', { params }),

  // 消息
  getMessageList: (params) => request.get('/api/v1/collab/messages', { params }),

  // 审批任务
  getMyTasks: () => request.get('/api/v1/workflow/tasks/my'),
  getProcessedTasks: () => request.get('/api/v1/workflow/tasks/processed')
}