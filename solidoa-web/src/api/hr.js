import request from '@/utils/request'

// HR 模块 API - 统一考勤和财务功能
export const hrApi = {
  // ========== 考勤相关 ==========
  // 打卡
  check: (data) => request.post('/v1/hr/attendance/check', data),
  getRecords: (params) => request.get('/v1/hr/attendance/records', { params }),
  getClockRecords: (userId, startDate, endDate) =>
    request.get('/v1/hr/attendance/clock', { params: { userId, startDate, endDate } }),

  // 汇总
  getSummary: (yearMonth) => request.get('/v1/hr/attendance/summary', { params: { yearMonth } }),

  // 异常
  getExceptions: (params) => request.get('/v1/hr/attendance/exceptions', { params }),

  // 考勤异常
  getMyAnomalies: (month) => request.get('/v1/hr/attendance/anomaly/my', { params: { month } }),
  processAnomaly: (id, form) => request.post(`/v1/hr/attendance/anomaly/${id}/process`, form),
  getAnomalyStatistics: (startDate, endDate) =>
    request.get('/v1/hr/attendance/anomaly/statistics', { params: { startDate, endDate } }),

  // ========== 假期管理 ==========
  getLeaveTypes: () => request.get('/v1/hr/attendance/leave/types'),
  createLeaveType: (data) => request.post('/v1/hr/attendance/leave/types', data),
  updateLeaveType: (id, data) => request.put(`/v1/hr/attendance/leave/types/${id}`, data),
  deleteLeaveType: (id) => request.delete(`/v1/hr/attendance/leave/types/${id}`),
  getMyLeaveBalance: (year) => request.get('/v1/hr/attendance/leave/balance', { params: { year } }),
  initLeaveBalance: (userId, data) => request.post(`/v1/hr/attendance/leave/balance/init`, { userId, ...data }),
  adjustLeaveBalance: (userId, data) => request.post(`/v1/hr/attendance/leave/balance/adjust`, { userId, ...data }),

  // ========== 班次管理 ==========
  getShifts: () => request.get('/v1/hr/attendance/shift'),
  createShift: (data) => request.post('/v1/hr/attendance/shift', data),
  updateShift: (id, data) => request.put(`/v1/hr/attendance/shift/${id}`, data),
  deleteShift: (id) => request.delete(`/v1/hr/attendance/shift/${id}`),

  // ========== 考勤组管理 ==========
  getGroups: () => request.get('/v1/hr/attendance/group'),
  createGroup: (data) => request.post('/v1/hr/attendance/group', data),
  updateGroup: (id, data) => request.put(`/v1/hr/attendance/group/${id}`, data),
  deleteGroup: (id) => request.delete(`/v1/hr/attendance/group/${id}`),

  // ========== 节假日管理 ==========
  getHolidays: (params) => request.get('/v1/hr/attendance/holiday', { params }),
  createHoliday: (data) => request.post('/v1/hr/attendance/holiday', data),
  updateHoliday: (id, data) => request.put(`/v1/hr/attendance/holiday/${id}`, data),
  deleteHoliday: (id) => request.delete(`/v1/hr/attendance/holiday/${id}`),
  importHolidays: (data) => request.post('/v1/hr/attendance/holiday/import', data),

  // ========== 规则配置 ==========
  getRules: () => request.get('/v1/hr/attendance/rule'),
  updateRules: (data) => request.put('/v1/hr/attendance/rule', data),

  // ========== 补卡申请 ==========
  getRepairCardList: (params) => request.get('/v1/hr/attendance/repair', { params }),
  getRepairCardById: (id) => request.get(`/v1/hr/attendance/repair/${id}`),
  createRepairCard: (data) => request.post('/v1/hr/attendance/repair', data),
  approveRepairCard: (id, data) => request.post(`/v1/hr/attendance/repair/${id}/approve`, data),
  cancelRepairCard: (id) => request.put(`/v1/hr/attendance/repair/${id}/cancel`),
  getRepairCardStatistics: (params) => request.get('/v1/hr/attendance/repair/statistics', { params }),

  // ========== 外出申请 ==========
  getGoOutList: (params) => request.get('/v1/hr/attendance/go-out', { params }),
  getGoOutById: (id) => request.get(`/v1/hr/attendance/go-out/${id}`),
  createGoOut: (data) => request.post('/v1/hr/attendance/go-out', data),
  approveGoOut: (id, data) => request.post(`/v1/hr/attendance/go-out/${id}/approve`, data),
  cancelGoOut: (id) => request.put(`/v1/hr/attendance/go-out/${id}/cancel`),

  // ========== 加班申请 ==========
  getOvertimeList: (params) => request.get('/v1/hr/attendance/overtime', { params }),
  getOvertimeById: (id) => request.get(`/v1/hr/attendance/overtime/${id}`),
  createOvertime: (data) => request.post('/v1/hr/attendance/overtime', data),
  approveOvertime: (id, data) => request.post(`/v1/hr/attendance/overtime/${id}/approve`, data),
  cancelOvertime: (id) => request.put(`/v1/hr/attendance/overtime/${id}/cancel`),
  getOvertimeBalance: () => request.get('/v1/hr/attendance/overtime/balance'),

  // ========== 出差申请 ==========
  getBusinessTripList: (params) => request.get('/v1/hr/attendance/business-trip', { params }),
  getBusinessTripById: (id) => request.get(`/v1/hr/attendance/business-trip/${id}`),
  createBusinessTrip: (data) => request.post('/v1/hr/attendance/business-trip', data),
  approveBusinessTrip: (id, data) => request.post(`/v1/hr/attendance/business-trip/${id}/approve`, data),
  cancelBusinessTrip: (id) => request.put(`/v1/hr/attendance/business-trip/${id}/cancel`),
  getApprovedBusinessTrips: () => request.get('/v1/hr/attendance/business-trip/approved'),

  // ========== 钉钉考勤同步 ==========
  syncClockRecords: (userId, startDate, endDate) =>
    request.get('/v1/dingtalk/attendance/sync/clock', { params: { userId, startDate, endDate } }),
  syncBatchClockRecords: (userIds, date) =>
    request.get('/v1/dingtalk/attendance/sync/batch/clock', { params: { userIds: userIds?.join(','), date } }),
  syncOvertimeRecords: (userId, startTime, endTime) =>
    request.get('/v1/dingtalk/attendance/sync/overtime', { params: { userId, startTime, endTime } }),
  getDingTalkStatistics: (userId, month) =>
    request.get('/v1/dingtalk/attendance/statistics', { params: { userId, month } }),

  // ========== 财务相关 ==========
  // 报销
  getExpenseList: (params) => request.get('/v1/hr/finance/expense', { params }),
  getExpenseById: (id) => request.get(`/v1/hr/finance/expense/${id}`),
  createExpense: (data) => request.post('/v1/hr/finance/expense', data),
  approveExpense: (id, data) => request.post(`/v1/hr/finance/expense/${id}/approve`, data),
  cancelExpense: (id) => request.put(`/v1/hr/finance/expense/${id}/cancel`),

  // 预算
  getBudgetList: (params) => request.get('/v1/hr/finance/budget', { params }),
  getStatistics: (params) => request.get('/v1/hr/finance/expense/statistics', { params }),

  // ========== 工资管理 ==========
  getSalaryItems: () => request.get('/v1/hr/finance/salary/items'),
  getSalaryList: (params) => request.get('/v1/hr/finance/salary', { params }),
  getSalaryById: (id) => request.get(`/v1/hr/finance/salary/${id}`),
  createSalary: (data) => request.post('/v1/hr/finance/salary', data),
  batchCreateSalary: (data) => request.post('/v1/hr/finance/salary/batch', data),
  updateSalary: (id, data) => request.put(`/v1/hr/finance/salary/${id}`, data),
  submitSalary: (id) => request.post(`/v1/hr/finance/salary/${id}/submit`),
  cancelSalary: (id) => request.post(`/v1/hr/finance/salary/${id}/cancel`),
  approveSalary: (id, data) => request.post(`/v1/hr/finance/salary/${id}/approve`, data),
  rejectSalary: (id, data) => request.post(`/v1/hr/finance/salary/${id}/reject`, data),
  paySalary: (id) => request.post(`/v1/hr/finance/salary/${id}/pay`),
  batchPaySalary: (data) => request.post('/v1/hr/finance/salary/batch-pay', data),
  getMySalary: (params) => request.get('/v1/hr/finance/salary/my', { params }),
  exportSalary: (params) => request.get('/v1/hr/finance/salary/export', { params, responseType: 'blob' })
}
