import { defineStore } from 'pinia'
import {
  clearCompetitionPlayerSession,
  getCompetitionPlayerToken,
  getStoredCompetitionPlayerInfo,
  getCompetitionPlayerEntry,
  getCompetitionPlayerTenantId,
  saveCompetitionPlayerSession,
  saveCompetitionPlayerEntry,
} from '@/utils/competition-auth'

export const useCompetitionPlayerStore = defineStore('competitionPlayer', {
  state: () => ({
    playerToken: getCompetitionPlayerToken(),
    playerInfo: getStoredCompetitionPlayerInfo(),
    playerId: getStoredCompetitionPlayerInfo()?.playerId || getStoredCompetitionPlayerInfo()?.id || null,
    tenantId: getCompetitionPlayerTenantId(),
    entryCode: getCompetitionPlayerEntry(),
    events: [],
    currentEvent: null,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.playerToken && state.playerId),
  },
  actions: {
    setSession({ playerToken, playerInfo, tenantId }) {
      saveCompetitionPlayerSession(playerToken, playerInfo, tenantId)
      this.playerToken = playerToken
      this.playerInfo = playerInfo || null
      this.playerId = playerInfo?.playerId || playerInfo?.id || null
      this.tenantId = tenantId || this.tenantId
    },
    setEntry({ entryCode, tenantId, eventId, eventName, eventStatus }) {
      saveCompetitionPlayerEntry(entryCode)
      this.entryCode = entryCode
      this.tenantId = tenantId || this.tenantId
      this.currentEvent = eventId ? { eventId, eventName, eventStatus } : this.currentEvent
    },
    setProfile(profile) {
      this.playerInfo = profile || null
      this.playerId = profile?.playerId || null
      this.events = profile?.events || []
      if (!this.currentEvent && this.events.length) this.currentEvent = this.events[0]
    },
    setCurrentEvent(event) {
      this.currentEvent = event || null
    },
    clearSession() {
      clearCompetitionPlayerSession()
      this.playerToken = ''
      this.playerInfo = null
      this.playerId = null
      this.currentEvent = null
      this.events = []
    },
  },
})
