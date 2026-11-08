// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
import {
  formatConnectionStatus,
  formatFriendlyStreamError,
} from '../src/utils/connectionStatus';

describe('utils/connectionStatus (Xbox Connection HUD & Technical Jargon Masking)', () => {
  it('masks raw SDP and offer negotiation messages into Stage 2 Display & Audio status', () => {
    const offerStatus = formatConnectionStatus(
      'Configuration obtained successfully, initiating offer...',
    );
    expect(offerStatus.step).toBe(2);
    expect(offerStatus.headline).toBe('Configuring Display & Audio');
    expect(offerStatus.subtitle.toLowerCase()).not.toContain('sdp');
    expect(offerStatus.subtitle.toLowerCase()).not.toContain('offer');

    const remoteOfferStatus = formatConnectionStatus(
      'Remote offer retrieved successfully...',
    );
    expect(remoteOfferStatus.step).toBe(2);
    expect(remoteOfferStatus.headline).toBe('Configuring Display & Audio');
  });

  it('masks raw ICE candidate messages into Stage 3 Linking and Stage 4 Launch statuses', () => {
    const sendingIce = formatConnectionStatus('Ready to send ICE...');
    expect(sendingIce.step).toBe(3);
    expect(sendingIce.headline).toBe('Linking to Your Xbox');
    expect(sendingIce.subtitle.toLowerCase()).not.toContain('ice');

    const exchangeIce = formatConnectionStatus('Exchange ICE successfully...');
    expect(exchangeIce.step).toBe(4);
    expect(exchangeIce.headline).toBe('Launching Xbox Stream');
    expect(exchangeIce.subtitle.toLowerCase()).not.toContain('ice');
  });

  it('maps initial connecting and retry messages to Stage 1 and distinguishes Console Wake from Cloud Allocation', () => {
    const connecting = formatConnectionStatus('Connecting... (2/20)');
    expect(connecting.step).toBe(1);
    expect(connecting.headline).toContain('Connecting to Xbox (Retry 2/20)');

    const wakingConsole = formatConnectionStatus('Waking Xbox console...');
    expect(wakingConsole.badge).toBe('XBOX REMOTE PLAY');
    expect(wakingConsole.headline).toBe('Waking Xbox Console');

    const allocatingCloud = formatConnectionStatus('Allocating cloud server...');
    expect(allocatingCloud.badge).toBe('XBOX CLOUD GAMING');
    expect(allocatingCloud.headline).toBe('Preparing Cloud Console');
  });

  it('strips internal function bracket prefixes from stream errors', () => {
    const msg = formatFriendlyStreamError(
      'ice',
      '[sendICECandidates] fail: Network timeout',
    );
    expect(msg).not.toContain('[sendICECandidates]');
    expect(msg).toContain('Could not establish a direct network link');
    expect(msg).toContain('Network timeout');
  });
});
