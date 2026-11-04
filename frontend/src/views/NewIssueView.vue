<template>
  <div style="max-width: 860px; margin: 0 auto">
    <div class="section-head">
      <div>
        <h1 class="section-title" style="font-size: 22px">新建议题</h1>
        <p class="section-desc">在 {{ repo.owner }}/{{ repo.name }} 里记录一个问题或想法。</p>
      </div>
      <router-link class="btn btn-sm" :to="`/${repo.owner}/${repo.name}/issues`">
        <Icon name="arrowLeft" :size="14" />返回议题
      </router-link>
    </div>

    <form class="form-card" @submit.prevent="submit">
      <div v-if="error" class="form-error">{{ error }}</div>
      <div class="field">
        <label for="title">标题</label>
        <input id="title" v-model.trim="form.title" type="text" maxlength="255" placeholder="用一句话说清楚要讨论的事" />
      </div>
      <div class="field">
        <div class="label-row">
          <label for="body">描述</label>
          <span class="hint">支持 Markdown 语法</span>
        </div>
        <textarea
          id="body"
          v-model="form.body"
          maxlength="20000"
          style="min-height: 220px"
          placeholder="补充背景、复现步骤或期望的结果…"
        ></textarea>
      </div>
      <div class="hero-actions" style="margin-top: 4px">
        <button class="btn btn-primary" type="submit" :disabled="submitting">
          <Icon name="issue" :size="15" />{{ submitting ? '提交中…' : '创建议题' }}
        </button>
        <router-link class="btn" :to="`/${repo.owner}/${repo.name}/issues`">取消</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, apiMessage } from '../api/client'
import Icon from '../components/Icon.vue'

const props = defineProps({
  repo: { type: Object, required: true },
  refreshRepo: { type: Function, default: null }
})

const router = useRouter()
const form = reactive({ title: '', body: '' })
const error = ref('')
const submitting = ref(false)

async function submit() {
  error.value = ''
  if (!form.title) {
    error.value = '请填写议题标题'
    return
  }
  submitting.value = true
  try {
    const { data } = await api.post(`/repos/${props.repo.owner}/${props.repo.name}/issues`, {
      title: form.title,
      body: form.body
    })
    if (props.refreshRepo) props.refreshRepo()
    router.push(`/${props.repo.owner}/${props.repo.name}/issues/${data.issue.number}`)
  } catch (ex) {
    error.value = apiMessage(ex, '议题创建失败，请稍后再试')
  } finally {
    submitting.value = false
  }
}
</script>
