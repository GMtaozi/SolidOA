/**
 * API 模块导出
 * 统一导出所有 API 模块
 */

import request from '@/utils/request'
import { workflowApi } from './workflow'
import { hrApi } from './hr'
import { systemApi } from './system'
import * as fileApi from './file'

// 导出 request 实例供直接使用
export { request }

// 导出所有 API 模块
export { workflowApi, hrApi, systemApi, fileApi }

// 导出所有类型
export * from './types'

// 默认导出 request
export default request
