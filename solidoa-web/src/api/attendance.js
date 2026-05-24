import api from './index'

export const attendanceApi = {
  // 打卡
  check: (data) => api.post('/v1/attendance/check', data),
  getRecords: (params) => api.get('/v1/attendance/records', { params }),

  // 汇总
  getSummary: (yearMonth) => api.get('/v1/attendance/summary', { params: { yearMonth } }),

  // 异常
  getExceptions: (params) => api.get('/v1/attendance/exceptions', { params })
}