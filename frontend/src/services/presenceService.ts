import api from "@/services/apis";
import keycloak from "@/services/keycloak";
import { getRuntimeConfig } from "@/config/runtime-config";
import type { PresenceStatus } from "@/components/ui/PresenceBadge";

export interface Presence { status: PresenceStatus; lastSeenAt: string | null }
export const UNKNOWN_PRESENCE: Presence = { status: "unknown", lastSeenAt: null };
interface Ticket { ticket: string; authExpiresAt: string }

export function presenceUrl() {
  const url = new URL(`${getRuntimeConfig().apiBaseUrl}/api/presence/ws`, window.location.origin);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  return url.toString();
}

/** One instance per authenticated tab. Native browser PONGs are handled by the browser. */
export class PresenceConnection {
  private active = false;
  private socket?: WebSocket;
  private targets = new Set<string>();
  private ready = false;
  private retry = 0;
  private generation = 0;
  private epoch = "";
  private sequence = 0;
  private timer?: ReturnType<typeof setTimeout>;
  private renewal?: ReturnType<typeof setTimeout>;
  private watchdog?: ReturnType<typeof setTimeout>;
  private renewing = false;

  constructor(
    private readonly onEntries: (entries: Record<string, Presence>, reset: boolean) => void,
    private readonly onConnection: (connected: boolean) => void,
    private readonly getTicket: () => Promise<Ticket> = async () => {
      await keycloak.updateToken(45);
      return (await api.post<Ticket>("/api/presence/tickets", undefined, { timeout: 5000 })).data;
    },
    private readonly createSocket: () => WebSocket = () => new WebSocket(presenceUrl()),
  ) {}

  start() { if (!this.active) { this.active = true; void this.connect(); } }

  stop() {
    this.active = false; this.generation++;
    clearTimeout(this.timer); clearTimeout(this.renewal); clearTimeout(this.watchdog);
    this.socket?.close(1000, "Session ended");
    this.socket = undefined; this.ready = false;
    this.onEntries({}, true); this.onConnection(false);
  }

  setTargets(ids: string[]) {
    const next = new Set(ids.slice(0, 100));
    const removed = [...this.targets].filter(id => !next.has(id));
    const added = [...next].filter(id => !this.targets.has(id));
    this.targets = next;
    if (removed.length) this.onEntries(Object.fromEntries(removed.map(id => [id, UNKNOWN_PRESENCE])), false);
    if (this.ready) {
      if (removed.length) this.send({ type: "UNSUBSCRIBE", userIds: removed });
      if (added.length) this.send({ type: "SUBSCRIBE", userIds: added });
    }
  }

  private send(value: object) {
    if (this.socket?.readyState === WebSocket.OPEN) this.socket.send(JSON.stringify(value));
  }

  private async connect() {
    const generation = ++this.generation;
    try {
      const ticket = await this.getTicket();
      if (!this.active || generation !== this.generation) return;
      const socket = this.createSocket(); this.socket = socket;
      this.armWatchdog(10000);
      this.epoch = ""; this.sequence = 0;
      const current = () => this.active && generation === this.generation && socket === this.socket;
      socket.onopen = () => { if (current()) this.send({ type: "AUTH", ticket: ticket.ticket }); };
      socket.onmessage = event => {
        if (!current()) return;
        this.armWatchdog(75000);
        try { this.receive(JSON.parse(event.data)); }
        catch { socket.close(1002, "Invalid presence frame"); }
      };
      socket.onerror = () => { if (current()) socket.close(); };
      socket.onclose = () => { if (current()) this.disconnected(); };
    } catch {
      if (this.active && generation === this.generation) this.disconnected();
    }
  }

  private receive(message: any) {
    if (message.type === "READY") {
      this.ready = true; this.retry = 0; this.epoch = message.epoch; this.sequence = 0;
      this.onConnection(true);
      this.send({ type: "SUBSCRIBE", userIds: [...this.targets] });
      this.scheduleRenewal(message.authExpiresAt);
    } else if (message.type === "REAUTH_REQUIRED") {
      void this.reauthenticate();
    } else if (message.type === "REAUTHENTICATED") {
      this.scheduleRenewal(message.authExpiresAt);
    } else if (message.type === "PRESENCE_SNAPSHOT" || message.type === "PRESENCE_CHANGED") {
      if (!this.ready || message.epoch !== this.epoch || message.sequence <= this.sequence) return;
      this.sequence = message.sequence;
      const entries: Record<string, Presence> = {};
      for (const row of message.entries || []) {
        if (this.targets.has(row.userId) && (row.status === "online" || row.status === "offline")) {
          entries[row.userId] = { status: row.status, lastSeenAt: row.lastSeenAt || null };
        }
      }
      for (const id of message.removed || []) if (this.targets.has(id)) entries[id] = UNKNOWN_PRESENCE;
      this.onEntries(entries, false);
    } else if (message.type === "PRESENCE_REMOVED") {
      this.onEntries(Object.fromEntries((message.userIds || []).map((id: string) => [id, UNKNOWN_PRESENCE])), false);
    } else if (message.type === "ERROR") {
      this.onEntries({}, true);
    }
  }

  private scheduleRenewal(expiresAt: string) {
    clearTimeout(this.renewal);
    this.renewal = setTimeout(() => void this.reauthenticate(), Math.max(1000, Date.parse(expiresAt) - Date.now() - 45000));
  }

  private async reauthenticate() {
    if (!this.ready || this.renewing) return;
    const generation = this.generation;
    this.renewing = true;
    try {
      const ticket = await this.getTicket();
      if (this.active && generation === this.generation && this.ready) this.send({ type: "REAUTH", ticket: ticket.ticket });
    } catch {
      if (generation === this.generation) this.socket?.close(1000, "Authentication refresh failed");
    } finally { this.renewing = false; }
  }

  private disconnected() {
    this.ready = false; clearTimeout(this.renewal); clearTimeout(this.watchdog);
    this.onEntries({}, true); this.onConnection(false);
    if (!this.active) return;
    clearTimeout(this.timer);
    const delay = Math.min(30000, 1000 * 2 ** Math.min(this.retry++, 5)) + Math.random() * 500;
    this.timer = setTimeout(() => void this.connect(), delay);
  }

  private armWatchdog(timeout: number) {
    clearTimeout(this.watchdog);
    this.watchdog = setTimeout(() => {
      // A half-open connection may never fire onclose. Discard stale state and
      // start a new generation without waiting for the browser's TCP timeout.
      const stale = this.socket;
      this.generation++; this.socket = undefined;
      stale?.close(1000, "Presence timeout");
      if (this.active) this.disconnected();
    }, timeout);
  }
}
