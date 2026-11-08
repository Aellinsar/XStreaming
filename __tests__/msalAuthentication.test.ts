// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
const mockStorage = new Map<string, string>();

jest.mock('../src/store/mmkv', () => ({
  storage: {
    getString: (key: string) => mockStorage.get(key),
    set: (key: string, val: string) => mockStorage.set(key, val),
    delete: (key: string) => mockStorage.delete(key),
  },
}));

jest.mock('../src/store/settingStore', () => ({
  getSettings: () => ({
    force_region_ip: '',
    signaling_home_name: '',
    signaling_cloud_name: '',
    debug: false,
  }),
}));

import MsalAuthentication from '../src/MsalAuthentication';
import {
  getStreamToken,
  saveStreamToken,
} from '../src/store/streamTokenStore';
import {getWebToken, saveWebToken} from '../src/store/webTokenStore';
import StreamingToken from '../src/tokens/streamingtoken';

describe('MsalAuthentication', () => {
  beforeEach(() => {
    mockStorage.clear();
  });

  it('purges deprecated XAL token scope during checkAuthentication()', async () => {
    mockStorage.set(
      'user.tokenstore',
      JSON.stringify({
        userToken: {
          access_token: 'legacy-xal-token',
          scope: 'service::user.auth.xboxlive.com::MBI_SSL',
          expires_on: new Date(Date.now() + 3600 * 1000).toISOString(),
        },
      }),
    );

    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);

    const isAuth = await auth.checkAuthentication();
    expect(isAuth).toBe(false);
    expect(auth._tokenStore.getUserToken()).toBeUndefined();
    expect(onCompleted).not.toHaveBeenCalled();
  });

  it('reuses valid cached streamToken and webToken in getTokens() without network calls', async () => {
    const validStreamTokens = {
      xHomeToken: new StreamingToken({
        gsToken: 'cached-home-gstoken',
        durationInSeconds: 3600,
        offeringSettings: {regions: []},
      }),
      xCloudToken: null,
    };
    const validWebToken = {
      data: {
        Token: 'cached-web-token',
        NotAfter: new Date(Date.now() + 3600 * 1000).toISOString(),
        DisplayClaims: {xui: [{uhs: '12345'}]},
      },
    };
    saveStreamToken(validStreamTokens);
    saveWebToken(validWebToken);

    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);
    const spyStreaming = jest.spyOn(auth, 'getStreamingToken');

    await auth.getTokens();

    expect(spyStreaming).not.toHaveBeenCalled();
    expect(onCompleted).toHaveBeenCalledTimes(1);
    expect(onCompleted.mock.calls[0][0].xHomeToken.data.gsToken).toBe(
      'cached-home-gstoken',
    );
    expect(onCompleted.mock.calls[0][1]).toEqual(validWebToken);
  });

  it('persists newly fetched streamingTokens and webToken to MMKV in getTokens()', async () => {
    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);

    const freshHomeToken = new StreamingToken({
      gsToken: 'fresh-home-gstoken',
      durationInSeconds: 3600,
      offeringSettings: {regions: []},
    });
    const freshWebToken = {
      data: {
        Token: 'fresh-web-token',
        NotAfter: new Date(Date.now() + 3600 * 1000).toISOString(),
        DisplayClaims: {xui: [{uhs: '99999'}]},
      },
    };

    jest.spyOn(auth, 'getStreamingToken').mockResolvedValue({
      xHomeToken: freshHomeToken,
      xCloudToken: undefined as any,
    });
    jest
      .spyOn(auth._msal, 'getWebToken')
      .mockResolvedValue(freshWebToken as any);

    await auth.getTokens();

    expect(onCompleted).toHaveBeenCalledTimes(1);
    expect(getStreamToken()?.xHomeToken?.data?.gsToken).toBe(
      'fresh-home-gstoken',
    );
    expect(getWebToken()?.data?.Token).toBe('fresh-web-token');
  });

  it('cancels device code polling cleanly without invoking authenticationFailed', async () => {
    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);

    const pollPromise = auth.doPollForDeviceCodeAuth('device-code-123');
    auth.cancelDeviceCodeAuth();
    await pollPromise;

    expect(onFailed).not.toHaveBeenCalled();
  });
});
