import { defineStore } from 'pinia'
import { authApi, getApiErrorMessage } from '../services/api.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    initialized: false,
    loading: false
  }),

  getters: {
    isAuthenticated: state => Boolean(state.user),
    isAdmin: state => state.user?.role === 'admin',
    isMembre: state => state.user?.role === 'membre'
  },

  actions: {
    async restoreSession(force = false) {
      if (this.loading) return
      if (this.initialized && !force) return

      this.loading = true
      try {
        this.user = await authApi.me()
      } catch (error) {
        if (error?.response?.status === 401) {
          this.user = null
        }
        if (error?.response?.status && ![401, 403].includes(error.response.status)) {
          throw error
        }
      } finally {
        this.initialized = true
        this.loading = false
      }
    },

    async login(credentials) {
      this.loading = true
      try {
        this.user = await authApi.login(credentials)
        this.initialized = true
        return this.user
      } catch (error) {
        this.user = null
        this.initialized = true
        throw new Error(getApiErrorMessage(error))
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await authApi.logout()
      } finally {
        this.clear()
      }
    },

    setUser(user) {
      this.user = user
      this.initialized = true
    },

    clear() {
      this.user = null
      this.initialized = true
      this.loading = false
    }
  }
})
