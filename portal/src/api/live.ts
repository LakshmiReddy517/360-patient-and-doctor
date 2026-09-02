import { useEffect, useRef } from 'react';
import { API_BASE, TOKEN_KEY } from './client';

export type LiveEventName = 'hello' | 'event' | 'location' | 'notification' | 'request';

/**
 * Subscribes to the backend's real-time SSE stream and invokes `onEvent` for each event.
 * EventSource can't send an Authorization header, so the JWT is passed as a query param.
 */
export function useLive(onEvent: (name: LiveEventName, data: any) => void) {
  const handler = useRef(onEvent);
  handler.current = onEvent;

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return;
    const url = `${API_BASE}/live/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);

    const names: LiveEventName[] = ['hello', 'event', 'location', 'notification', 'request'];
    const listeners = names.map((name) => {
      const fn = (e: MessageEvent) => {
        let data: any = e.data;
        try { data = JSON.parse(e.data); } catch { /* keep raw */ }
        handler.current(name, data);
      };
      es.addEventListener(name, fn as EventListener);
      return { name, fn };
    });

    return () => {
      listeners.forEach((l) => es.removeEventListener(l.name, l.fn as EventListener));
      es.close();
    };
  }, []);
}
