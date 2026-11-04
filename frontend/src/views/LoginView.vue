<template>
  <div class="container page">
    <div class="auth-wrap">
      <div class="auth-aside">
        <span class="eyebrow">欢迎回来</span>
        <h1>继续打磨<br />属于你的代码</h1>
        <p>登录后可以管理仓库、处理议题与合并请求，并用同一组账号在本地推送代码。</p>
        <ul class="auth-points">
          <li><span class="tick"><Icon name="check" :size="12" /></span>完整 Git 协议，命令行无缝接入</li>
          <li><span class="tick"><Icon name="check" :size="12" /></span>议题与合并请求留痕清晰</li>
          <li><span class="tick"><Icon name="check" :size="12" /></span>数据全部保存在本机</li>
        </ul>
      </div>

      <div class="auth-card">
        <h2>登录功夫吉特</h2>
        <p class="auth-sub">使用用户名或邮箱登录</p>
        <form @submit.prevent="submit">
          <div v-if="error" class="form-error">{{ error }}</div>
          <div class="field">
            <label for="login">用户名或邮箱</label>
            <input id="login" v-model.trim="form.login" type="text" autocomplete="username" placeholder="akun 或 you@example.com" />
          </div>
          <div class="field">
            <div class="label-row">
              <label for="password">密码</label>
            </div>
            <input id="password" v-model="form.password" type="password" autocomplete="current-password" placeholder="请输入密码" />
          </div>
          <button class="btn btn-primary btn-block btn-lg" type="submit" :disabled="submitting">
            {{ submitting ? '登录中…' : '登录' }}
          </button>
        </form>
        <p class="auth-switch">
          还没有账号？<router-link to="/register">立即注册</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../stores/auth'
import { apiMessage } from '../api/client'
import Icon from '../components/Icon.vue'

const route = useRoute()
const router = useRouter()
const form = reactive({ login: '', password: '' })
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (!form.login || !form.password) {
    error.value = '请填写用户名和密码'
    return
  }
  submitting.value = true
  try {
    await login(form.login, form.password)
    router.push(route.query.redirect || '/dashboard')
  } catch (ex) {
    error.value = apiMessage(ex, '登录失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>
