<template>
  <div class="container page">
    <div style="max-width: 760px; margin: 0 auto">
      <div class="section-head">
        <div>
          <h1 class="section-title" style="font-size: 25px">新建仓库</h1>
          <p class="section-desc">仓库会立即初始化，并给出可用的克隆地址。</p>
        </div>
        <router-link to="/dashboard" class="btn btn-sm">
          <Icon name="arrowLeft" :size="14" />返回
        </router-link>
      </div>

      <form class="form-card" @submit.prevent="submit">
        <div v-if="error" class="form-error">{{ error }}</div>

        <div class="field">
          <label for="repoName">仓库名称</label>
          <input id="repoName" v-model.trim="form.name" type="text" placeholder="例如 kunfu-demo" />
          <span class="hint">将以 @{{ auth.user?.username }}/{{ form.name || '仓库名' }} 的形式展示。</span>
        </div>

        <div class="field">
          <div class="label-row">
            <label for="repoDesc">简介</label>
            <span class="hint">{{ form.description.length }} / 500</span>
          </div>
          <textarea
            id="repoDesc"
            v-model="form.description"
            maxlength="500"
            style="min-height: 92px"
            placeholder="用一两句话说明这个项目做什么（可选）"
          ></textarea>
        </div>

        <div class="field">
          <label>可见性</label>
          <div class="choice-grid">
            <div class="choice" :class="{ active: form.visibility === 'PUBLIC' }" @click="form.visibility = 'PUBLIC'">
              <div class="choice-title"><Icon name="globe" :size="15" />公开</div>
              <div class="choice-desc">任何人都能浏览与克隆，适合展示与分享。</div>
            </div>
            <div class="choice" :class="{ active: form.visibility === 'PRIVATE' }" @click="form.visibility = 'PRIVATE'">
              <div class="choice-title"><Icon name="lock" :size="15" />私有</div>
              <div class="choice-desc">只有你自己能查看和推送。</div>
            </div>
          </div>
        </div>

        <div class="field">
          <label class="checkbox-row" for="readme">
            <input id="readme" v-model="form.initialReadme" type="checkbox" />
            <span>初始化 README.md 与第一次提交，让仓库可以直接克隆</span>
          </label>
        </div>

        <div class="hero-actions" style="margin-top: 6px">
          <button class="btn btn-primary" type="submit" :disabled="submitting">
            <Icon name="plus" :size="15" />{{ submitting ? '正在创建…' : '创建仓库' }}
          </button>
          <router-link to="/dashboard" class="btn">取消</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import { auth } from '../stores/auth'
import Icon from '../components/Icon.vue'

const router = useRouter()
const form = reactive({
  name: '',
  description: '',
  visibility: 'PUBLIC',
  initialReadme: true
})
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (!form.name) {
    error.value = '请填写仓库名称'
    return
  }
  submitting.value = true
  try {
    const { data } = await api.post('/repos', {
      name: form.name,
      description: form.description,
      visibility: form.visibility,
      initialReadme: form.initialReadme
    })
    router.push(`/${data.repo.owner}/${data.repo.name}`)
  } catch (ex) {
    error.value = apiMessage(ex, '创建仓库失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>
