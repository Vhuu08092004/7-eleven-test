import { useRef, useCallback } from 'react'

export function useDebounce(callback, delay = 500) {
  const timeoutRef = useRef(null)
  const callbackRef = useRef(callback)

  // Keep the latest callback available
  callbackRef.current = callback

  return useCallback((...args) => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }
    timeoutRef.current = setTimeout(() => {
      callbackRef.current(...args)
    }, delay)
  }, [delay])
}
