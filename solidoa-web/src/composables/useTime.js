import { ref, computed, onUnmounted } from 'vue'

const fullDatetime = ref('')
const dateWithWeekday = ref('')

let intervalId = null
let refCount = 0

const startTimer = () => {
  if (intervalId) return

  const update = () => {
    const now = new Date()
    fullDatetime.value = now.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    })
    dateWithWeekday.value = now.toLocaleDateString('zh-CN', {
      weekday: 'long',
      month: 'long',
      day: 'numeric'
    })
  }

  update()
  intervalId = setInterval(update, 1000)
}

const stopTimer = () => {
  if (intervalId) {
    clearInterval(intervalId)
    intervalId = null
  }
}

export function formatDate(dateStr, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  if (isNaN(date.getTime())) return '-'

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

export function useTime() {
  refCount++
  if (!intervalId) {
    startTimer()
  }

  onUnmounted(() => {
    refCount--
    if (refCount === 0) {
      stopTimer()
    }
  })

  const currentTime = computed(() => formatDate(fullDatetime.value, 'HH:mm'))

  const currentSeconds = computed(() => formatDate(fullDatetime.value, 'ss'))

  return {
    fullDatetime,
    dateWithWeekday,
    currentTime,
    currentSeconds
  }
}
