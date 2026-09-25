import { fetchEventSource } from '@microsoft/fetch-event-source';

import { resolveApiUrl } from '@/config/runtime-config';
import keycloak from '@/services/keycloak';

export interface RealtimeEvent {
  eventId: string;
  eventType: 'POST_REACTION_CHANGED';
  postId: string;
  occurredAt: string;
}

export async function connectRealtime(
  signal: AbortSignal,
  onEvent: (event: RealtimeEvent) => void,
): Promise<void> {
  await keycloak.updateToken(30);
  if (!keycloak.token) {
    throw new Error('Missing access token');
  }

  await fetchEventSource(resolveApiUrl('/api/realtime/events'), {
    signal,
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
      Accept: 'text/event-stream',
    },
    openWhenHidden: false,
    onmessage(message) {
      if (message.event !== 'post-reaction-changed' || !message.data) {
        return;
      }
      onEvent(JSON.parse(message.data) as RealtimeEvent);
    },
    async onopen(response) {
      if (response.ok && response.headers.get('content-type')?.includes('text/event-stream')) {
        return;
      }
      if (response.status === 401 || response.status === 403) {
        throw new Error('Realtime authentication failed');
      }
      throw new Error(`Realtime connection failed: ${response.status}`);
    },
    onerror(error) {
      throw error;
    },
  });
}
