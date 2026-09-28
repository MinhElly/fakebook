import { fetchEventSource } from "@microsoft/fetch-event-source";

import { resolveApiUrl } from "@/config/runtime-config";
import keycloak from "@/services/keycloak";

interface RealtimeEventBase {
  eventId: string;
  occurredAt: string;
}

export interface PostReactionChangedEvent extends RealtimeEventBase {
  eventType: "POST_REACTION_CHANGED";
  postId: string;
}

export interface FriendRequestCreatedEvent extends RealtimeEventBase {
  eventType: "FRIEND_REQUEST_CREATED";
  requestId: string;
  actorUserId: string;
  recipientId: string;
}

export type RealtimeEvent = PostReactionChangedEvent | FriendRequestCreatedEvent;

class RetryableRealtimeError extends Error {}
class RetryableAuthenticationError extends RetryableRealtimeError {}
class FatalRealtimeError extends Error {}

const MAX_RETRY_DELAY_MS = 30_000;

function getRetryDelay(attempt: number): number {
  const baseDelay = Math.min(1_000 * 2 ** attempt, MAX_RETRY_DELAY_MS);
  const jitter = baseDelay * 0.2 * (Math.random() * 2 - 1);
  return Math.max(1_000, Math.round(baseDelay + jitter));
}

export async function connectRealtime(
  signal: AbortSignal,
  onEvent: (event: RealtimeEvent) => void,
  onConnected?: () => void,
): Promise<void> {
  let forceRefreshToken = false;
  let unauthorizedRetries = 0;
  let retryAttempt = 0;

  const authenticatedFetch: typeof fetch = async (input, init = {}) => {
    try {
      await keycloak.updateToken(forceRefreshToken ? -1 : 30);
    } catch (cause) {
      keycloak.clearToken();
      throw new FatalRealtimeError(
        cause instanceof Error ? cause.message : "Unable to refresh access token",
      );
    }

    forceRefreshToken = false;
    if (!keycloak.token) {
      keycloak.clearToken();
      throw new FatalRealtimeError("Missing access token");
    }

    const headers = new Headers(init.headers);
    headers.set("Authorization", `Bearer ${keycloak.token}`);
    headers.set("Accept", "text/event-stream");

    return fetch(input, { ...init, headers });
  };

  await fetchEventSource(resolveApiUrl("/api/realtime/events"), {
    signal,
    fetch: authenticatedFetch,
    headers: {
      Accept: "text/event-stream",
    },
    openWhenHidden: false,
    async onopen(response) {
      const contentType = response.headers.get("content-type");
      if (response.ok && contentType?.includes("text/event-stream")) {
        retryAttempt = 0;
        unauthorizedRetries = 0;
        onConnected?.();
        return;
      }

      if (response.status === 401 && unauthorizedRetries === 0) {
        unauthorizedRetries += 1;
        forceRefreshToken = true;
        throw new RetryableAuthenticationError("Realtime access token expired");
      }

      if (response.status === 401) {
        keycloak.clearToken();
        throw new FatalRealtimeError("Realtime authentication failed after token refresh");
      }

      if (response.status === 403) {
        throw new FatalRealtimeError("Realtime access forbidden");
      }

      if (response.status === 429 || response.status >= 500) {
        throw new RetryableRealtimeError(`Realtime server error: ${response.status}`);
      }

      throw new FatalRealtimeError(`Realtime connection failed: ${response.status}`);
    },
    onmessage(message) {
      if (!message.event || !message.data || message.event === "message") {
        return;
      }

      try {
        onEvent(JSON.parse(message.data) as RealtimeEvent);
      } catch (error) {
        console.error("[Realtime] Invalid event payload", error);
      }
    },
    onerror(error) {
      if (signal.aborted || error instanceof FatalRealtimeError) {
        throw error;
      }

      if (error instanceof RetryableAuthenticationError) {
        return 0;
      }

      const delay = getRetryDelay(retryAttempt);
      retryAttempt += 1;
      console.warn(`[Realtime] Connection lost; retrying in ${delay}ms`, error);
      return delay;
    },
    onclose() {
      if (!signal.aborted) {
        throw new RetryableRealtimeError("Realtime connection closed unexpectedly");
      }
    },
  });
}
