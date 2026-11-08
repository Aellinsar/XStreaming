// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
// Copyright (C) 2026 Aellinsar (https://github.com/Aellinsar/XStreaming)

export const STREAM_CONNECTED = 'connected';
export const STREAM_CLOSED = 'closed';
export const STREAM_FAILED = 'failed';
export const STREAM_DISCONNECTED = 'disconnected';

export type StreamConnectionContext = {
  state: string;
  previousState: string;
  isConnected: boolean;
  isRequestExit: boolean;
  streamType?: string;
};

export type StreamTransitionAction =
  | {type: 'connected'}
  | {type: 'reconnect'}
  | {type: 'exit_stream'}
  | {type: 'alert_closed'}
  | {type: 'alert_reconnected_failed'}
  | {type: 'alert_nat_failed'}
  | {type: 'none'};

/**
 * Determines whether a stream session is eligible to schedule an automatic
 * WebRTC transport reconnect.
 *
 * IMPORTANT: Only 'cloud' sessions may auto-reconnect via `startSession()`,
 * because `XcloudApi.startSession()` on 'cloud' reuses a persisted sessionPath,
 * whereas on 'home' (Console Remote Play) it sends `POST /v5/sessions/home/play`,
 * which acts as a Wake-on-LAN / Instant-On wake command and would force a
 * powered-off console back on.
 */
export function canScheduleStreamReconnect(params: {
  streamType?: string;
  isConnected: boolean;
  isRequestExit: boolean;
}): boolean {
  return (
    params.streamType === 'cloud' &&
    !params.isRequestExit &&
    params.isConnected
  );
}

/**
 * Determines whether waking/resuming the app (`AppState === 'active'`) should
 * immediately trigger a WebRTC transport reconnect (for 'cloud' sessions).
 */
export function shouldReconnectOnAppActive(params: {
  appState: string;
  streamType?: string;
  isConnected: boolean;
  isRequestExit: boolean;
  currentConnectState: string;
}): boolean {
  if (params.appState !== 'active') {
    return false;
  }
  if (!canScheduleStreamReconnect(params)) {
    return false;
  }
  return (
    params.currentConnectState === STREAM_DISCONNECTED ||
    params.currentConnectState === STREAM_FAILED ||
    params.currentConnectState === STREAM_CLOSED
  );
}

/**
 * Determines whether waking/resuming the app (`AppState === 'active'`) on a
 * Console Remote Play ('home') session that dropped while dimmed/backgrounded
 * should cleanly exit back to the Home launch screen instead of hanging on a
 * black surface or waking a powered-off console.
 */
export function shouldExitStreamOnAppActive(params: {
  appState: string;
  streamType?: string;
  isConnected: boolean;
  isRequestExit: boolean;
  currentConnectState: string;
}): boolean {
  if (params.appState !== 'active') {
    return false;
  }
  if (
    params.streamType === 'cloud' ||
    params.isRequestExit ||
    !params.isConnected
  ) {
    return false;
  }
  return (
    params.currentConnectState === STREAM_DISCONNECTED ||
    params.currentConnectState === STREAM_FAILED ||
    params.currentConnectState === STREAM_CLOSED
  );
}

/**
 * Pure state-transition resolver for WebRTC `setConnectedHandler` in `NativeStream.tsx`.
 * Guarantees that:
 * 1. Console Remote Play ('home') sessions exit cleanly back to the Home launch
 *    screen when the console turns off, stops streaming, or drops connection
 *    (never hanging on a black/frozen screen and never auto-waking the console).
 * 2. Cloud sessions auto-reconnect up to the retry limit and show an alert if
 *    retries are exhausted.
 */
export function resolveStreamConnectionTransition(
  ctx: StreamConnectionContext,
  tryScheduleReconnect: () => boolean,
): StreamTransitionAction {
  const {state, previousState, isConnected, isRequestExit, streamType} = ctx;
  const isHomeStream = streamType !== 'cloud';

  if (state === STREAM_CONNECTED) {
    return {type: 'connected'};
  }

  if (state === STREAM_DISCONNECTED) {
    if (tryScheduleReconnect()) {
      return {type: 'reconnect'};
    }
    return {type: 'none'};
  }

  if (state === STREAM_CLOSED) {
    if (isRequestExit) {
      return {type: 'none'};
    }
    if (isConnected && tryScheduleReconnect()) {
      return {type: 'reconnect'};
    }
    if (isHomeStream && isConnected) {
      return {type: 'exit_stream'};
    }
    if (previousState !== STREAM_CONNECTED) {
      return {type: 'none'};
    }
    return {type: 'alert_closed'};
  }

  if (state === STREAM_FAILED) {
    if (isRequestExit) {
      return {type: 'none'};
    }
    if (isConnected) {
      if (tryScheduleReconnect()) {
        return {type: 'reconnect'};
      }
      if (isHomeStream) {
        return {type: 'exit_stream'};
      }
      return {type: 'alert_reconnected_failed'};
    }
    return {type: 'alert_nat_failed'};
  }

  return {type: 'none'};
}
