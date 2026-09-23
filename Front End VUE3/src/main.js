import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { pinia } from './stores'
import { setupApiAccessHandling } from './services/api'
import { useAuthStore } from './stores/authStore'

const app = createApp(App)

app.use(pinia)
app.use(router)

setupApiAccessHandling(router, () => useAuthStore(pinia))

app.mount('#app')
