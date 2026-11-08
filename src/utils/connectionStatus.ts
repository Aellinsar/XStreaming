// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later

export interface FormattedConnectionStatus {
  badge: string;
  headline: string;
  subtitle: string;
  /** 1..4 for multi-step stream connection progress, or 0 when indeterminate */
  step: number;
  totalSteps: number;
}

/**
 * Masks low-level WebRTC / signaling technical strings (SDP, ICE, offer, candidate,
 * configuration, etc.) and maps connection states to Xbox-authentic, user-friendly
 * HUD copy and a 4-quadrant progress step.
 */
export const formatConnectionStatus = (
  rawText?: string | null,
): FormattedConnectionStatus => {
  const input = (rawText || '').trim();
  const lower = input.toLowerCase();

  if (!input) {
    return {
      badge: 'XBOX NETWORK',
      headline: 'Please Wait',
      subtitle: 'Syncing with Xbox Live...',
      step: 0,
      totalSteps: 4,
    };
  }

  // Disconnecting / stopping stream
  if (
    lower.includes('disconnect') ||
    lower.includes('closing') ||
    lower.includes('断开')
  ) {
    return {
      badge: 'XBOX REMOTE PLAY',
      headline: 'Ending Session',
      subtitle: 'Saving session & disconnecting from Xbox...',
      step: 4,
      totalSteps: 4,
    };
  }

  // Queue / Cloud allocation vs. Console wake/provisioning
  if (lower.includes('queue') || lower.includes('排队')) {
    return {
      badge: 'XBOX CLOUD GAMING',
      headline: 'Waiting in Queue',
      subtitle: input,
      step: 1,
      totalSteps: 4,
    };
  }

  if (
    lower.includes('waking xbox') ||
    lower.includes('waking console') ||
    lower.includes('powering on')
  ) {
    return {
      badge: 'XBOX REMOTE PLAY',
      headline: 'Waking Xbox Console',
      subtitle: 'Powering on console & initializing remote play...',
      step: 1,
      totalSteps: 4,
    };
  }

  if (
    lower.includes('allocating') ||
    lower.includes('provisioning') ||
    lower.includes('分配')
  ) {
    return {
      badge: 'XBOX CLOUD GAMING',
      headline: 'Preparing Cloud Console',
      subtitle: 'Reserving an Xbox Series X blade for your session...',
      step: 1,
      totalSteps: 4,
    };
  }

  // Stage 4: ICE exchange completed / Finalizing stream
  if (
    lower.includes('exchange ice') ||
    lower.includes('ice successfully') ||
    lower.includes('ice交换成功') ||
    lower.includes('connected')
  ) {
    return {
      badge: 'XBOX REMOTE PLAY',
      headline: 'Launching Xbox Stream',
      subtitle: 'Direct link verified • Starting video & controller feed...',
      step: 4,
      totalSteps: 4,
    };
  }

  // Stage 3: Sending ICE / NAT traversal / Candidate negotiation
  if (
    lower.includes('ice') ||
    lower.includes('candidate') ||
    lower.includes('teredo') ||
    lower.includes('nat')
  ) {
    return {
      badge: 'XBOX REMOTE PLAY',
      headline: 'Linking to Your Xbox',
      subtitle: 'Optimizing low-latency network route...',
      step: 3,
      totalSteps: 4,
    };
  }

  // Stage 2: SDP Offer / Answer / Media configuration
  if (
    lower.includes('offer') ||
    lower.includes('sdp') ||
    lower.includes('configuration obtained') ||
    lower.includes('remote offer') ||
    lower.includes('media') ||
    lower.includes('协商')
  ) {
    return {
      badge: 'XBOX REMOTE PLAY',
      headline: 'Configuring Display & Audio',
      subtitle: 'Syncing 1080p HQ video & stereo audio channels...',
      step: 2,
      totalSteps: 4,
    };
  }

  // Stage 1: Ready to connect / Starting session / Waking console
  if (
    lower.includes('ready to connect') ||
    lower.includes('connecting') ||
    lower.includes('连接')
  ) {
    const retryMatch = input.match(/\((\d+\/\d+)\)/);
    const retrySuffix = retryMatch ? ` (Retry ${retryMatch[1]})` : '';
    return {
      badge: 'XBOX REMOTE PLAY',
      headline: `Connecting to Xbox${retrySuffix}`,
      subtitle: 'Waking console & verifying Xbox Live session...',
      step: 1,
      totalSteps: 4,
    };
  }

  // Auth & Console discovery on Home Screen
  if (
    lower.includes('checking login') ||
    lower.includes('completing login') ||
    lower.includes('authenticating') ||
    lower.includes('登录')
  ) {
    return {
      badge: 'XBOX LIVE AUTH',
      headline: 'Signing In to Xbox Live',
      subtitle: 'Verifying your Xbox profile & security tokens...',
      step: 0,
      totalSteps: 4,
    };
  }

  if (lower.includes('fetching consoles') || lower.includes('console')) {
    return {
      badge: 'MY XBOX',
      headline: 'Finding Your Consoles',
      subtitle: 'Scanning your Xbox Live account for registered consoles...',
      step: 0,
      totalSteps: 4,
    };
  }

  if (lower.includes('loading') || lower.includes('加载')) {
    return {
      badge: 'MY XBOX',
      headline: 'Syncing Xbox Live',
      subtitle: 'Communicating with your Xbox console...',
      step: 0,
      totalSteps: 4,
    };
  }

  return {
    badge: 'XBOX NETWORK',
    headline: 'Working on It',
    subtitle: input,
    step: 0,
    totalSteps: 4,
  };
};

/**
 * Formats stream connection errors cleanly without internal function brackets
 * such as `[sendICECandidates] fail:` or `[sendSDPOffer] fail:`.
 */
export const formatFriendlyStreamError = (
  stage: 'session' | 'sdp' | 'ice',
  rawError: unknown,
): string => {
  const raw =
    typeof rawError === 'string'
      ? rawError
      : (rawError as any)?.message || String(rawError || 'Unknown error');

  const cleaned = raw
    .replace(/^\[[^\]]+\]\s*(?:fail|failed|error)\s*:\s*/i, '')
    .trim();

  if (stage === 'ice') {
    return `Could not establish a direct network link to your Xbox console. Make sure your console and device are online.\n\nDetails: ${cleaned}`;
  }
  if (stage === 'sdp') {
    return `Could not negotiate video and audio streaming with your Xbox console.\n\nDetails: ${cleaned}`;
  }
  return cleaned;
};
