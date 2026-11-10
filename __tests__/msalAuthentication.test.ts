// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
const mockStorage = new Map<string, string>();
const mockNativeVault = new Map<string, string>();

jest.mock('react-native', () => ({
  NativeModules: {
    NativeUiBridge: {
      saveBackupToken: (key: string, val: string) => {
        mockNativeVault.set(key, val);
      },
      getBackupToken: (key: string) => mockNativeVault.get(key) || '',
      clearBackupToken: (key: string) => {
        mockNativeVault.delete(key);
      },
    },
  },
}));

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

import axios from 'axios';
import MsalAuthentication from '../src/MsalAuthentication';
import {TokenRefreshError} from '../src/xal/msal';
import XstsToken from '../src/tokens/xststoken';
import {
  getStreamToken,
  saveStreamToken,
} from '../src/store/streamTokenStore';
import {getWebToken, saveWebToken} from '../src/store/webTokenStore';
import StreamingToken from '../src/tokens/streamingtoken';

describe('MsalAuthentication', () => {
  beforeEach(() => {
    mockStorage.clear();
    mockNativeVault.clear();
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

    jest
      .spyOn(auth._msal, 'doPollForDeviceCodeAuth')
      .mockImplementation(async () => {
        throw new Error('Device code polling cancelled');
      });

    const pollPromise = auth.doPollForDeviceCodeAuth('device-code-123');
    auth.cancelDeviceCodeAuth();
    await pollPromise;

    expect(onFailed).not.toHaveBeenCalled();
  });

  it('preserves user.tokenstore refresh_token when getTokens() encounters a transient network error', async () => {
    mockStorage.set(
      'user.tokenstore',
      JSON.stringify({
        userToken: {
          access_token: 'valid-access-token',
          refresh_token: 'long-lived-refresh-token',
          scope: 'XboxLive.signin openid profile offline_access',
          expires_on: new Date(Date.now() + 3600 * 1000).toISOString(),
        },
      }),
    );

    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);
    auth._tokenStore.load();

    jest
      .spyOn(auth, 'getStreamingToken')
      .mockRejectedValue(new Error('Network Error: timeout of 15000ms exceeded'));

    await auth.getTokens();

    expect(onFailed).toHaveBeenCalledWith(
      expect.stringContaining('Network Error'),
      false,
    );
    expect(auth._tokenStore.getUserToken()?.data.refresh_token).toBe(
      'long-lived-refresh-token',
    );
    expect(mockStorage.get('user.tokenstore')).toContain(
      'long-lived-refresh-token',
    );
  });

  it('clears user.tokenstore only when OAuth refresh_token is permanently rejected (TokenRefreshError)', async () => {
    mockStorage.set(
      'user.tokenstore',
      JSON.stringify({
        userToken: {
          access_token: 'expired-access-token',
          refresh_token: 'revoked-refresh-token',
          scope: 'XboxLive.signin',
          expires_on: new Date(Date.now() - 3600 * 1000).toISOString(),
        },
      }),
    );

    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);
    auth._tokenStore.load();

    jest
      .spyOn(auth, 'getStreamingToken')
      .mockRejectedValue(
        new TokenRefreshError('Failed to refresh tokens', new Error('400')),
      );

    await auth.getTokens();

    expect(onFailed).toHaveBeenCalledWith(
      expect.stringContaining('Failed to refresh tokens'),
      true,
    );
    expect(auth._tokenStore.getUserToken()).toBeUndefined();
    expect(mockStorage.get('user.tokenstore')).toBeUndefined();
    expect(mockNativeVault.get('user.tokenstore')).toBeUndefined();
  });

  it('recovers user.tokenstore from Native SharedPreferences vault when MMKV is empty (Upgrade B)', () => {
    const backupPayload = JSON.stringify({
      userToken: {
        access_token: 'vault-access-token',
        refresh_token: 'vault-90d-refresh-token',
        scope: 'xboxlive.signin openid profile offline_access',
        client_id: '000000004c20a908',
        expires_on: new Date(Date.now() + 3600 * 1000).toISOString(),
      },
    });
    mockNativeVault.set('user.tokenstore', backupPayload);

    const auth = new MsalAuthentication(jest.fn(), jest.fn());
    const loaded = auth._tokenStore.load();

    expect(loaded).toBe(true);
    expect(auth._tokenStore.getUserToken()?.data.refresh_token).toBe(
      'vault-90d-refresh-token',
    );
    expect(mockStorage.get('user.tokenstore')).toBe(backupPayload);
  });

  it('persists and reuses valid xstsToken and gssvToken from TokenStore without network round-trips (Upgrade C)', async () => {
    const futureExpiry = new Date(Date.now() + 7200 * 1000).toISOString();
    mockStorage.set(
      'user.tokenstore',
      JSON.stringify({
        userToken: {
          access_token: 'valid-access-token',
          refresh_token: 'valid-refresh-token',
          scope: 'xboxlive.signin openid profile offline_access',
          expires_on: futureExpiry,
        },
        xstsToken: {
          IssueInstant: new Date().toISOString(),
          NotAfter: futureExpiry,
          Token: 'cached-xsts-web-token',
          DisplayClaims: {xui: [{uhs: '112233'}]},
        },
        gssvToken: {
          IssueInstant: new Date().toISOString(),
          NotAfter: futureExpiry,
          Token: 'cached-gssv-token',
          DisplayClaims: {xui: [{uhs: '112233'}]},
        },
      }),
    );

    const auth = new MsalAuthentication(jest.fn(), jest.fn());
    auth._tokenStore.load();

    const spyXsts = jest.spyOn(auth._msal, 'doXstsAuthentication');
    const spyXstsAuthz = jest
      .spyOn(auth._msal, 'doXstsAuthorization')
      .mockResolvedValue(
        new XstsToken({
          IssueInstant: new Date().toISOString(),
          NotAfter: futureExpiry,
          Token: 'authorized-web-token',
          DisplayClaims: {xui: [{uhs: '112233'}]},
        }),
      );

    const webToken = await auth._msal.getWebToken();
    const gssvToken = await auth._msal.getGssvToken();

    expect(spyXsts).not.toHaveBeenCalled();
    expect(spyXstsAuthz).toHaveBeenCalledTimes(1);
    expect(spyXstsAuthz).toHaveBeenCalledWith(
      'cached-xsts-web-token',
      'http://xboxlive.com',
    );
    expect(webToken.data.Token).toBe('authorized-web-token');
    expect(gssvToken?.data.Token).toBe('cached-gssv-token');
  });

  it('generates PKCE S256 authorize URL and exchanges redirect code with client_id 000000004c20a908 (Upgrade A)', async () => {
    const onCompleted = jest.fn();
    const onFailed = jest.fn();
    const auth = new MsalAuthentication(onCompleted, onFailed);

    const authorizeUrl = auth.getPkceAuthorizeUrl();
    expect(authorizeUrl).toContain(
      'https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?',
    );
    expect(authorizeUrl).toContain('client_id=000000004c20a908');
    expect(authorizeUrl).toContain(
      'redirect_uri=ms-xal-000000004c20a908%3A%2F%2Fauth',
    );
    expect(authorizeUrl).toContain('code_challenge_method=S256');

    const stateMatch = authorizeUrl.match(/[?&]state=([^&]+)/);
    expect(stateMatch).not.toBeNull();
    const state = decodeURIComponent(stateMatch![1]);

    const spyUrlSearchParamsGet = jest
      .spyOn(URLSearchParams.prototype, 'get')
      .mockImplementation(() => {
        throw new Error('URLSearchParams.get is not implemented');
      });

    const spyAxiosPost = jest.spyOn(axios, 'post').mockResolvedValue({
      data: {
        token_type: 'Bearer',
        scope: 'xboxlive.signin openid profile',
        expires_in: 3600,
        access_token: 'pkce-access-token',
        refresh_token: 'pkce-refresh-token',
      },
    } as any);
    jest.spyOn(auth, 'getTokens').mockResolvedValue(undefined);

    await auth.handlePkceRedirectUrl(
      `ms-xal-000000004c20a908://auth?code=auth-code-xyz&state=${encodeURIComponent(state)}`,
    );

    expect(onFailed).not.toHaveBeenCalled();
    expect(spyAxiosPost).toHaveBeenCalledTimes(1);
    expect(auth._tokenStore.getUserToken()?.data.access_token).toBe(
      'pkce-access-token',
    );
    expect(auth._tokenStore.getUserToken()?.data.refresh_token).toBe(
      'pkce-refresh-token',
    );
    expect(auth._tokenStore.getUserToken()?.data.client_id).toBe(
      '000000004c20a908',
    );
    expect(mockNativeVault.get('user.tokenstore')).toContain(
      'pkce-refresh-token',
    );
    expect(auth.getTokens).toHaveBeenCalledTimes(1);
    spyAxiosPost.mockRestore();
    spyUrlSearchParamsGet.mockRestore();
  });

  it('preserves modern #12345 gamertag suffix when resolving Xbox Live display claims', () => {
    const {
      extractGamertagFromClaimsOrProfile,
      resolveXboxGamertag,
    } = require('../src/utils/xcloud');

    expect(
      extractGamertagFromClaimsOrProfile({
        umg: 'NobleSix#31245',
        gtg: 'NobleSix31245',
      }),
    ).toBe('NobleSix#31245');

    expect(
      extractGamertagFromClaimsOrProfile({
        modernGamertag: 'MasterChief',
        modernGamertagSuffix: '117',
      }),
    ).toBe('MasterChief#117');

    expect(
      extractGamertagFromClaimsOrProfile({
        modernGamertag: 'ClassicTag',
        modernGamertagSuffix: '',
      }),
    ).toBe('ClassicTag');

    expect(
      resolveXboxGamertag(
        '',
        null,
        {
          data: {
            DisplayClaims: {
              xui: [{uhs: '123', mgt: 'Cortana', mgs: '0452', gtg: 'Cortana0452'}],
            },
          },
        },
        null,
        '',
      ),
    ).toBe('Cortana#0452');
  });

  it('extracts and normalizes Xbox Live gamerpic URLs to HTTPS across V1 and V2 profile payloads', () => {
    const {
      extractGamerpicFromProfile,
      resolveXboxGamerpic,
    } = require('../src/utils/xcloud');

    expect(
      extractGamerpicFromProfile({
        displayPicRaw:
          'http://images-eds.xboxlive.com/image?url=abc123&format=png',
      }),
    ).toBe('https://images-eds-ssl.xboxlive.com/image?url=abc123&format=png');

    expect(
      extractGamerpicFromProfile({
        GameDisplayPicRaw:
          'https://images-eds-ssl.xboxlive.com/image?url=xyz789&format=png',
      }),
    ).toBe('https://images-eds-ssl.xboxlive.com/image?url=xyz789&format=png');

    mockStorage.set(
      'user.gamerpic',
      'https://images-eds-ssl.xboxlive.com/image?url=cachedPic',
    );
    expect(resolveXboxGamerpic('', null)).toBe(
      'https://images-eds-ssl.xboxlive.com/image?url=cachedPic',
    );
  });
});
