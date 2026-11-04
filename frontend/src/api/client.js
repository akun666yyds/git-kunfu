import axios from 'axios'

const TOKEN_KEY = 'gitkunfu_token'

export const api = axios.create({
  baseURL: '/api',
  timeout: 20000
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function apiMessage(error, fallback = '网络不太顺畅，请稍后再试') {
  return error?.response?.data?.message || fallback
}

export { TOKEN_KEY }
