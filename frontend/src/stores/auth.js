import { reactive } from 'vue'
import { api, TOKEN_KEY } from '../api/client'

const USER_KEY = 'gitkunfu_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const auth = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readUser(),
  loading: false
})

export function isLoggedIn() {
  return Boolean(auth.token && auth.user)
}

export function setAuth(token, user) {
  auth.token = token || ''
  auth.user = user || null
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function updateUser(user) {
  auth.user = user
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function logout() {
  setAuth('', null)
}

export async function login(loginName, password) {
  auth.loading = true
  try {
    const { data } = await api.post('/auth/login', { login: loginName, password })
    setAuth(data.token, data.user)
    return data.user
  } finally {
    auth.loading = false
  }
}

export async function register(payload) {
  auth.loading = true
  try {
    const { data } = await api.post('/auth/register', payload)
    setAuth(data.token, data.user)
    return data.user
  } finally {
    auth.loading = false
  }
}

export async function loadMe() {
  if (!auth.token) return null
  const { data } = await api.get('/auth/me')
  updateUser(data.user)
  return data.user
}
