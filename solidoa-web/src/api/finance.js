import api from './index'

export const financeApi = {
  // 报销
  getExpenseList: (params) => api.get('/v1/finance/expense', { params }),
  getExpenseById: (id) => api.get(`/v1/finance/expense/${id}`),
  createExpense: (data) => api.post('/v1/finance/expense', data),

  // 预算
  getBudgetList: (params) => api.get('/v1/finance/budget', { params }),

  // 统计
  getStatistics: (params) => api.get('/v1/finance/expense/statistics', { params })
}