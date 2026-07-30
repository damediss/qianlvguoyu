import { getCompetitionPlayerToken, redirectToCompetitionLogin } from '@/utils/competition-auth'
import { useCompetitionPlayerStore } from '@/store/competitionPlayer'

/**
 * Guard for the standalone player application only. It never reads the
 * training user store or redirects to a training route.
 */
export function guardCompetitionPlayerRoute() {
  const store = useCompetitionPlayerStore()
  const hasPlayerIdentity = Boolean(store.playerId || store.playerInfo?.playerId || store.playerInfo?.id)
  const allowed = Boolean(getCompetitionPlayerToken()) && hasPlayerIdentity

  if (!allowed) {
    store.clearSession()
    redirectToCompetitionLogin()
  }
  return allowed
}
