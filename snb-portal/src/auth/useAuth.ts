import { useSyncExternalStore } from 'react'
import { getUserSnapshot, subscribeAuth, type AuthUser } from './tokens'

export function useAuthUser(): AuthUser | null {
  return useSyncExternalStore(subscribeAuth, getUserSnapshot)
}
