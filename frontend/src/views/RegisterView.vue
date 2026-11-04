<template>
  <div class="container page">
    <div class="auth-wrap">
      <div class="auth-aside">
        <span class="eyebrow">新的开始</span>
        <h1>给自己开一方<br />代码的自留地</h1>
        <p>注册账号后即可创建仓库、接收推送、发起合并请求。所有内容只保存在这台机器上。</p>
        <ul class="auth-points">
          <li><span class="tick"><Icon name="check" :size="12" /></span>公开或私有仓库随手切换</li>
          <li><span class="tick"><Icon name="check" :size="12" /></span>浏览器里读代码、看提交</li>
          <li><span class="tick"><Icon name="check" :size="12" /></span>议题讨论与合并流程齐全</li>
        </ul>
      </div>

      <div class="auth-card">
        <h2>创建账号</h2>
        <p class="auth-sub">只需几步，就能开始推送代码</p>
        <form @submit.prevent="submit">
          <div v-if="error" class="form-error">{{ error }}</div>
          <div class="field">
            <label for="username">用户名</label>
            <input id="username" v-model.trim="form.username" type="text" autocomplete="username" placeholder="例如 akun" />
            <span class="hint">3-39 位字母、数字、下划线或短横线，将用于仓库地址。</span>
          </div>
          <div class="field">
            <label for="email">邮箱</label>
            <input id="email" v-model.trim="form.email" type="email" autocomplete="email" placeholder="you@example.com" />
          </div>
          <div class="field">
            <label for="displayName">昵称</label>
            <input id="displayName" v-model.trim="form.displayName" type="text" autocomplete="nickname" placeholder="展示在提交与讨论里的名字（可选）" />
          </div>
          <div class="field">
            <label for="password">密码</label>
            <input id="password" v-model="form.password" type="password" autocomplete="new-password" placeholder="至少 6 位" />
          </div>
          <button class="btn btn-primary btn-block btn-lg" type="submit" :disabled="submitting">
            {{ submitting ? '创建中…' : '注册并进入' }}
          </button>
        </form>
        <p class="auth-switch">
          已经有账号了？<router-link to="/login">直接登录</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../stores/auth'
import { apiMessage } from '../api/client'
import Icon from '../components/Icon.vue'

const router = useRouter()
const form = reactive({ username: '', email: '', displayName: '', password: '' })
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (!form.username || !form.email || !form.password) {
    error.value = '请把用户名、邮箱和密码填写完整'
    return
  }
  submitting.value = true
  try {
    await register({
      username: form.username,
      email: form.email,
      displayName: form.displayName || form.username,
      password: form.password
    })
    router.push('/dashboard')
  } catch (ex) {
    error.value = apiMessage(ex, '注册失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>
