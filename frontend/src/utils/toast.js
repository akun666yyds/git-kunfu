import { reactive } from 'vue'

export const toasts = reactive([])
let counter = 0

export function notify(message, type = 'info', duration = 2600) {
  const id = ++counter
  toasts.push({ id, message, type })
  window.setTimeout(() => {
    const index = toasts.findIndex((item) => item.id === id)
    if (index >= 0) toasts.splice(index, 1)
  }, duration)
}
