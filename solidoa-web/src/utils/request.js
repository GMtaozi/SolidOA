/**
 * Axios 请求封装
 * 统一处理：请求拦截、响应拦截、错误脱敏
 */

import axios from 'axios'
import router from '@/router'

let isRedirecting = false

// 创建 axios 实例
const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 敏感字段列表 - 这些字段不应暴露给前端
const SENSITIVE_PATTERNS = [
  /password\s*[:=]\s*\S+/gi,
  /secret\s*[:=]\s*\S+/gi,
  /Bearer\s+[\w\-\.]+/gi,
  /Basic\s+[\w\+\/\=]+/gi,
  /private.*key\s*[:=]\s*\S+/gi,
  /credential\s*[:=]\s*\S+/gi,
  /stack\s*trace/gi,
  /at\s+[\w\.\$]+\([^)]+\)/gi,
  /at\s+[\w\.]+:\d+/gi
]

// 需要脱敏的响应字段
const SENSITIVE_FIELDS = [
  'password',
  'secret',
  'token',
  'accessToken',
  'refreshToken',
  'privateKey',
  'secretKey',
  'apiKey',
  'credential',
  'sql',
  'stackTrace',
  'exception',
  'rootCause',
  'causedBy',
  'systemPath',
  'filePath',
  'classPath'
]

/**
 * 深度脱敏对象中的敏感字段
 * @param {any} obj - 需要脱敏的对象
 * @param {number} depth - 当前递归深度，防止无限递归
 * @returns {any} 脱敏后的对象
 */
function sanitizeObject(obj, depth = 0, maxDepth = 10) {
  // 防止无限递归，超过深度限制时只做浅层脱敏
  if (obj === null || obj === undefined) {
    return obj
  }

  if (depth > maxDepth) {
    // 超过深度限制时，仍然检查顶层敏感字段
    if (typeof obj === 'object') {
      const sanitized = {}
      for (const [key, value] of Object.entries(obj)) {
        if (SENSITIVE_FIELDS.some(field => key.toLowerCase().includes(field.toLowerCase()))) {
          sanitized[key] = '[已脱敏]'
        } else {
          sanitized[key] = value
        }
      }
      return sanitized
    }
    return obj
  }

  if (Array.isArray(obj)) {
    return obj.map(item => sanitizeObject(item, depth + 1, maxDepth))
  }

  if (typeof obj === 'object') {
    const sanitized = {}
    for (const [key, value] of Object.entries(obj)) {
      // 检查是否是敏感字段
      if (SENSITIVE_FIELDS.some(field => key.toLowerCase().includes(field.toLowerCase()))) {
        if (typeof value === 'string' && value.length > 0) {
          // 密码类字段直接替换为星号
          if (/password|passwd/i.test(key)) {
            sanitized[key] = '******'
          }
          // Token 类字段只保留前几位
          else if (/token/i.test(key) && value.length > 8) {
            sanitized[key] = value.substring(0, 8) + '...'
          }
          // 其他敏感字段
          else {
            sanitized[key] = '[已脱敏]'
          }
        } else {
          sanitized[key] = '[已脱敏]'
        }
      } else if (typeof value === 'object') {
        sanitized[key] = sanitizeObject(value, depth + 1, maxDepth)
      } else {
        sanitized[key] = value
      }
    }
    return sanitized
  }

  return obj
}

/**
 * 脱敏错误消息
 * @param {string} message - 原始错误消息
 * @returns {string} 脱敏后的消息
 */
function sanitizeErrorMessage(message) {
  if (!message || typeof message !== 'string') {
    return '操作失败，请稍后重试'
  }

  let sanitized = message

  // 替换敏感模式
  for (const pattern of SENSITIVE_PATTERNS) {
    sanitized = sanitized.replace(pattern, '[已脱敏]')
  }

  // 移除可能的文件路径
  sanitized = sanitized.replace(/[A-Za-z]:\\[^\s]+/g, '[路径已脱敏]')
  sanitized = sanitized.replace(/\/[\w\/\.-]+\//g, '/[路径已脱敏]/')

  // 移除IP地址
  sanitized = sanitized.replace(/\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/g, '[IP已脱敏]')

  // 移除端口号
  sanitized = sanitized.replace(/:\d{2,5}/g, ':[端口已脱敏]')

  // 移除堆栈跟踪中的文件路径
  sanitized = sanitized.replace(/at\s+[\w\.]+\s*\([^)]*\)/g, '[位置已脱敏]')

  // 移除 SQL 语句中的具体值
  sanitized = sanitized.replace(/VALUES\s*\([^)]+\)/gi, 'VALUES [数据已脱敏]')

  return sanitized.trim()
}

/**
 * 从错误响应中提取安全的消息
 * @param {object} error - 错误对象
 * @returns {object} 脱敏后的响应
 */
function extractSafeError(error) {
  const response = error.response || {}
  const status = response.status
  const statusText = response.statusText || ''

  // 根据 HTTP 状态码返回通用消息
  const statusMessages = {
    400: '请求参数错误',
    401: '登录已过期，请重新登录',
    403: '没有权限执行此操作',
    404: '请求的资源不存在',
    405: '请求方法不允许',
    408: '请求超时，请稍后重试',
    409: '请求冲突，请刷新后重试',
    422: '数据验证失败',
    429: '请求过于频繁，请稍后重试',
    500: '服务器内部错误',
    502: '网关错误',
    503: '服务暂时不可用',
    504: '网关超时'
  }

  // 优先使用后端返回的业务错误消息
  let data = response.data

  // 如果后端返回了敏感数据，进行脱敏
  if (data && typeof data === 'object') {
    data = sanitizeObject(data)

    // 如果后端返回了自定义消息且不包含敏感信息，使用它
    if (data.message && !SENSITIVE_FIELDS.some(f => data.message.toLowerCase().includes(f))) {
      return {
        success: false,
        message: data.message,
        code: data.code || status,
        data: data.data || null
      }
    }
  }

  // 使用状态码对应的通用消息
  return {
    success: false,
    message: statusMessages[status] || sanitizeErrorMessage(data?.message || statusText || error.message),
    code: data?.code || status,
    data: data?.data || null
  }
}

// 读取并解码 token（兼容旧格式）
const getToken = () => {
  let token = localStorage.getItem('token') || localStorage.getItem('access_token') || ''
  if (token) {
    try {
      // 尝试解码（如果存储的是编码后的 token）
      const decoded = decodeURIComponent(atob(token))
      // 如果解码后看起来像 JWT，则使用解码后的值
      if (decoded.includes('.') && decoded.split('.').length === 3) {
        token = decoded
      }
    } catch {
      // 解码失败，可能是旧格式明文 token，直接使用
    }
  }
  return token
}

// 解析 JWT 获取用户信息
const parseJwt = (token) => {
  try {
    if (!token || typeof token !== 'string') return null
    const parts = token.split('.')
    if (parts.length !== 3) return null
    if (!parts[1]) return null
    // JWT payload 使用 base64url 编码，需转换为标准 base64
    let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    while (base64.length % 4) base64 += '='
    const decoded = atob(base64)
    if (!decoded) return null
    return JSON.parse(decoded)
  } catch (e) {
    console.warn('[Request] JWT parse error:', e.message)
    return null
  }
}

// 获取用户信息头
const getUserHeaders = () => {
  // 解密 token
  const SECRET_KEY = 'SolidOA_Token_2024'
  const xorDecrypt = (encoded) => {
    try {
      const decoded = atob(encoded)
      const result = []
      for (let i = 0; i < decoded.length; i++) {
        result.push(String.fromCharCode(decoded.charCodeAt(i) ^ SECRET_KEY.charCodeAt(i % SECRET_KEY.length)))
      }
      return result.join('')
    } catch {
      return ''
    }
  }

  const encodedToken = localStorage.getItem('token') || ''
  const token = xorDecrypt(encodedToken)
  const payload = parseJwt(token)

  if (!payload) {
    console.warn('[Request] Failed to parse JWT token, encoded:', encodedToken ? 'exists' : 'empty')
    console.warn('[Request] Decoded token:', token ? token.substring(0, 50) + '...' : 'empty')
    return {}
  }

  console.log('[Request] JWT payload:', JSON.stringify(payload))
  console.log('[Request] X-User-Id:', payload.userId || payload.sub)

  return {
    'X-User-Id': String(payload.userId || payload.sub || ''),
    'X-User-Name': String(payload.username || ''),
    'X-User-DeptId': String(payload.deptId || ''),
    'X-User-Roles': Array.isArray(payload.roles) ? payload.roles.join(',') : String(payload.roles || ''),
    'X-Request-Source': 'EXTERNAL'
  }
}

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 添加用户信息头（用于直接访问 System 服务）
    const userHeaders = getUserHeaders()
    Object.entries(userHeaders).forEach(([key, value]) => {
      if (value) {
        config.headers[key] = value
      }
    })

    // 添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      }
    }

    return config
  },
  (error) => {
    // 请求错误脱敏
    return Promise.reject(new Error('网络请求失败，请检查网络连接'))
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // 统一处理业务错误码
    if (res.code !== undefined && res.code !== 200) {
      const error = new Error(res.message || '请求失败')
      error.response = { data: res }
      return Promise.reject(error)
    }

    return response
  },
  (error) => {
    // 401 自动清除 token 并跳转登录
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('access_token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('__token_encoded')
      if (!isRedirecting) {
        isRedirecting = true
        // 使用锁机制确保只在首次401时跳转
        router.push('/login')
        setTimeout(() => { isRedirecting = false }, 3000)
      }
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }

    // 错误脱敏处理
    const safeError = extractSafeError(error)
    const sanitizedError = new Error(safeError.message)
    sanitizedError.code = safeError.code
    sanitizedError.data = safeError.data
    sanitizedError.response = error.response

    // 在开发环境打印原始错误（不包含敏感信息的情况下）
    if (import.meta.env.DEV) {
      console.debug('[API Error]', {
        url: error.config?.url,
        method: error.config?.method,
        status: error.response?.status,
        safeMessage: safeError.message
      })
    }

    return Promise.reject(sanitizedError)
  }
)

export default request
export { extractSafeError, sanitizeErrorMessage, sanitizeObject }
