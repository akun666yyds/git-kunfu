<template>
  <div ref="root" class="markdown" v-html="html"></div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/common'

const props = defineProps({
  content: { type: String, default: '' }
})

marked.setOptions({ gfm: true, breaks: true })

const html = computed(() => {
  try {
    const raw = marked.parse(props.content || '')
    return DOMPurify.sanitize(raw, { ADD_ATTR: ['target', 'rel'] })
  } catch {
    return ''
  }
})

const root = ref(null)

async function decorate() {
  await nextTick()
  if (!root.value) return
  root.value.querySelectorAll('pre code').forEach((block) => {
    try {
      hljs.highlightElement(block)
    } catch {
      // 忽略无法识别语言的高亮
    }
  })
  root.value.querySelectorAll('a').forEach((link) => {
    const href = link.getAttribute('href') || ''
    if (href.startsWith('http://') || href.startsWith('https://')) {
      link.setAttribute('target', '_blank')
      link.setAttribute('rel', 'noopener noreferrer')
    }
  })
}

watch(() => props.content, decorate)
onMounted(decorate)
</script>
