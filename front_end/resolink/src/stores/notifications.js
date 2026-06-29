import { defineStore } from 'pinia'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    notices: [],
    notificationCount: 0
  }),
  actions: {
    addNotice(notice) {
      this.notices.unshift(notice)
      this.notificationCount++
    },
    clearNotifications() {
      this.notices = []
      this.notificationCount = 0
    },
    markAsRead() {
      this.notificationCount = 0
    },
    deleteNotice(id) {
      const index = this.notices.findIndex(notice => notice.id === id)
      if (index !== -1) {
        this.notices.splice(index, 1)
        if (this.notificationCount > 0) {
          this.notificationCount--
        }
      }
    },
    deleteReadNotices() {
      this.notices = []
      this.notificationCount = 0
    }
  }
})