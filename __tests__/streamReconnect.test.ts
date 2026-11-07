// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
// Copyright (C) 2026 Aellinsar (https://github.com/Aellinsar/XStreaming)

import {describe, expect, it, jest} from '@jest/globals';
import {
  canScheduleStreamReconnect,
  resolveStreamConnectionTransition,
  shouldExitStreamOnAppActive,
  shouldReconnectOnAppActive,
  STREAM_CLOSED,
  STREAM_CONNECTED,
  STREAM_DISCONNECTED,
  STREAM_FAILED,
} from '../src/utils/streamReconnect';

describe('streamReconnect state machine', () => {
  it('only allows automatic startSession reconnect for cloud sessions, never home consoles (to prevent waking powered-off Xbox)', () => {
    expect(
      canScheduleStreamReconnect({
        streamType: 'cloud',
        isConnected: true,
        isRequestExit: false,
      }),
    ).toBe(true);
    expect(
      canScheduleStreamReconnect({
        streamType: 'home',
        isConnected: true,
        isRequestExit: false,
      }),
    ).toBe(false);
    expect(
      canScheduleStreamReconnect({
        streamType: undefined,
        isConnected: true,
        isRequestExit: false,
      }),
    ).toBe(false);
    expect(
      canScheduleStreamReconnect({
        streamType: 'cloud',
        isConnected: false,
        isRequestExit: false,
      }),
    ).toBe(false);
    expect(
      canScheduleStreamReconnect({
        streamType: 'cloud',
        isConnected: true,
        isRequestExit: true,
      }),
    ).toBe(false);
  });

  it('schedules reconnect on mid-stream FAILED for cloud when scheduleReconnect succeeds', () => {
    const scheduleReconnect = jest.fn(() => true);
    const action = resolveStreamConnectionTransition(
      {
        state: STREAM_FAILED,
        previousState: STREAM_DISCONNECTED,
        isConnected: true,
        isRequestExit: false,
        streamType: 'cloud',
      },
      scheduleReconnect,
    );

    expect(scheduleReconnect).toHaveBeenCalledTimes(1);
    expect(action).toEqual({type: 'reconnect'});
  });

  it('exits stream cleanly back to Home launch screen when xHome Console Remote Play closes or fails mid-stream', () => {
    const scheduleReconnect = jest.fn(() => false);
    expect(
      resolveStreamConnectionTransition(
        {
          state: STREAM_CLOSED,
          previousState: STREAM_CONNECTED,
          isConnected: true,
          isRequestExit: false,
          streamType: 'home',
        },
        scheduleReconnect,
      ),
    ).toEqual({type: 'exit_stream'});

    expect(
      resolveStreamConnectionTransition(
        {
          state: STREAM_FAILED,
          previousState: STREAM_DISCONNECTED,
          isConnected: true,
          isRequestExit: false,
          streamType: 'home',
        },
        scheduleReconnect,
      ),
    ).toEqual({type: 'exit_stream'});
  });

  it('falls back to alert_reconnected_failed for cloud when mid-stream FAILED cannot reconnect', () => {
    const scheduleReconnect = jest.fn(() => false);
    const action = resolveStreamConnectionTransition(
      {
        state: STREAM_FAILED,
        previousState: STREAM_DISCONNECTED,
        isConnected: true,
        isRequestExit: false,
        streamType: 'cloud',
      },
      scheduleReconnect,
    );

    expect(scheduleReconnect).toHaveBeenCalledTimes(1);
    expect(action).toEqual({type: 'alert_reconnected_failed'});
  });

  it('shows alert_nat_failed when FAILED occurs before initial connection', () => {
    const scheduleReconnect = jest.fn(() => true);
    const action = resolveStreamConnectionTransition(
      {
        state: STREAM_FAILED,
        previousState: '',
        isConnected: false,
        isRequestExit: false,
        streamType: 'home',
      },
      scheduleReconnect,
    );

    expect(scheduleReconnect).not.toHaveBeenCalled();
    expect(action).toEqual({type: 'alert_nat_failed'});
  });

  it('handles CLOSED state on cloud by reconnecting or alerting only when not exiting', () => {
    expect(
      resolveStreamConnectionTransition(
        {
          state: STREAM_CLOSED,
          previousState: STREAM_CONNECTED,
          isConnected: true,
          isRequestExit: true,
          streamType: 'cloud',
        },
        () => true,
      ),
    ).toEqual({type: 'none'});

    expect(
      resolveStreamConnectionTransition(
        {
          state: STREAM_CLOSED,
          previousState: STREAM_CONNECTED,
          isConnected: true,
          isRequestExit: false,
          streamType: 'cloud',
        },
        () => false,
      ),
    ).toEqual({type: 'alert_closed'});
  });

  it('triggers reconnect on AppState active resume for cloud, and clean exit for home when transport dropped while dimmed/backgrounded', () => {
    expect(
      shouldReconnectOnAppActive({
        appState: 'active',
        streamType: 'cloud',
        isConnected: true,
        isRequestExit: false,
        currentConnectState: STREAM_FAILED,
      }),
    ).toBe(true);

    expect(
      shouldReconnectOnAppActive({
        appState: 'active',
        streamType: 'home',
        isConnected: true,
        isRequestExit: false,
        currentConnectState: STREAM_FAILED,
      }),
    ).toBe(false);

    expect(
      shouldExitStreamOnAppActive({
        appState: 'active',
        streamType: 'home',
        isConnected: true,
        isRequestExit: false,
        currentConnectState: STREAM_FAILED,
      }),
    ).toBe(true);

    expect(
      shouldExitStreamOnAppActive({
        appState: 'active',
        streamType: 'home',
        isConnected: true,
        isRequestExit: false,
        currentConnectState: STREAM_CONNECTED,
      }),
    ).toBe(false);
  });
});
