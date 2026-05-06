import axios from 'axios'

function normalizeBaseUrl(url) {
  return url.endsWith('/') ? url.slice(0, -1) : url
}

function resolveApiBaseUrl() {
  const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()
  if (configuredBaseUrl) {
    return normalizeBaseUrl(configuredBaseUrl)
  }

  if (typeof window === 'undefined') {
    return ''
  }

  const { protocol, hostname, port } = window.location
  if (protocol === 'file:') {
    return 'http://127.0.0.1:8080'
  }

  if ((hostname === 'localhost' || hostname === '127.0.0.1') && port && port !== '8080') {
    return `${protocol}//${hostname}:8080`
  }

  return ''
}

export function extractApiErrorMessage(error, fallbackMessage) {
  if (error?.response?.data?.message) {
    return error.response.data.message
  }

  if (error?.code === 'ECONNABORTED') {
    return '请求超时，请稍后重试。'
  }

  if (!error?.response) {
    return '无法连接后端服务。请先启动 http://127.0.0.1:8080 的后端，再刷新页面。'
  }

  return fallbackMessage
}

axios.defaults.baseURL = resolveApiBaseUrl()
axios.defaults.timeout = 10000
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'

export default axios
