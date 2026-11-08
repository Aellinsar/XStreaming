// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
jest.mock('react-native-webrtc', () => ({
  RTCPeerConnection: jest.fn(),
  RTCIceCandidate: jest.fn(),
  RTCSessionDescription: jest.fn(),
  MediaStream: jest.fn(),
}));

jest.mock('react-native', () => ({
  NativeModules: {},
}));

jest.mock('../src/store/settingStore', () => ({
  getSettings: () => ({
    debug: false,
    ipv6: false,
    prefer_ipv6: false,
    use_inner_turn_server: false,
    custom_turn_server: false,
  }),
}));

import webRTCClient from '../src/webrtc/index';

describe('webRTCClient._setBitrate (RFC 4566 SDP bandwidth injection)', () => {
  const sampleSdp = [
    'v=0',
    'o=- 12345 2 IN IP4 127.0.0.1',
    's=-',
    't=0 0',
    'm=video 9 UDP/TLS/RTP/SAVPF 96',
    'c=IN IP4 0.0.0.0',
    'a=rtpmap:96 H264/90000',
    'm=audio 9 UDP/TLS/RTP/SAVPF 111',
    'c=IN IP4 0.0.0.0',
    'a=rtpmap:111 opus/48000/2',
  ].join('\r\n');

  it('inserts b=AS:<kbps> after c= line and before a= lines for video and audio', () => {
    const afterVideo = webRTCClient.prototype._setBitrate(
      sampleSdp,
      'video',
      20000,
    );
    const afterBoth = webRTCClient.prototype._setBitrate(
      afterVideo,
      'audio',
      256,
    );

    const lines = afterBoth.split('\r\n');
    const videoIdx = lines.indexOf('m=video 9 UDP/TLS/RTP/SAVPF 96');
    expect(lines[videoIdx + 1]).toBe('c=IN IP4 0.0.0.0');
    expect(lines[videoIdx + 2]).toBe('b=AS:20000');
    expect(lines[videoIdx + 3]).toBe('a=rtpmap:96 H264/90000');

    const audioIdx = lines.indexOf('m=audio 9 UDP/TLS/RTP/SAVPF 111');
    expect(lines[audioIdx + 1]).toBe('c=IN IP4 0.0.0.0');
    expect(lines[audioIdx + 2]).toBe('b=AS:256');
    expect(lines[audioIdx + 3]).toBe('a=rtpmap:111 opus/48000/2');
  });

  it('replaces an existing b=AS:<kbps> line without duplicating it', () => {
    const first = webRTCClient.prototype._setBitrate(sampleSdp, 'video', 15000);
    const updated = webRTCClient.prototype._setBitrate(first, 'video', 30000);

    const bLines = updated
      .split('\r\n')
      .filter((line: string) => line.startsWith('b=AS:'));
    expect(bLines).toEqual(['b=AS:30000']);
  });
});
