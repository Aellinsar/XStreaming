import {NativeModules} from 'react-native';
import axios from 'axios';
import UserToken from '../tokens/usertoken';
import MsalToken from '../tokens/msaltoken';
import XstsToken from '../tokens/xststoken';
import StreamingToken from '../tokens/streamingtoken';
import {getSettings} from '../store/settingStore';
import {
  readPersistentAuthJson,
  writePersistentAuthJson,
  clearPersistentAuthJson,
} from '../store/authVaultStore';
import TokenStore from './tokenstore';

const AUTH_REQUEST_TIMEOUT = 15000;
const PKCE_PENDING_KEY = 'user.pkce_pending';

/**
 * Encodes key/value pairs as `application/x-www-form-urlencoded` without relying on
 * React Native's incomplete `URLSearchParams` polyfill.
 */
export function encodeFormParams(params: Record<string, string>): string {
  return Object.keys(params)
    .map(
      key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key] ?? '')}`,
    )
    .join('&');
}

/**
 * Parses a URL query string into a key/value map without calling `URLSearchParams.get()`
 * (which throws `URLSearchParams.get is not implemented` in React Native 0.72 Hermes).
 */
export function parseQueryParams(queryString: string): Record<string, string> {
  const result: Record<string, string> = {};
  const cleaned = queryString.replace(/^[?#]/, '').trim();
  if (!cleaned) {
    return result;
  }
  for (const part of cleaned.split('&')) {
    if (!part) {
      continue;
    }
    const eqIdx = part.indexOf('=');
    const rawKey = eqIdx >= 0 ? part.slice(0, eqIdx) : part;
    const rawVal = eqIdx >= 0 ? part.slice(eqIdx + 1) : '';
    try {
      const key = decodeURIComponent(rawKey.replace(/\+/g, ' '));
      const val = decodeURIComponent(rawVal.replace(/\+/g, ' '));
      result[key] = val;
    } catch {
      result[rawKey] = rawVal;
    }
  }
  return result;
}

export class TokenRefreshError extends Error {
  public error: unknown;

  constructor(message: string, error: unknown) {
    super(message);
    this.name = 'TokenRefreshError';
    this.error = error;
  }
}

export interface DeviceCodeResponse {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  interval: number;
  message: string;
  verification_uri_complete?: string;
}

interface PkceChallenge {
  value: string;
  method: string;
  verifier: string;
}

interface PendingPkceSession {
  verifier: string;
  state: string;
  createdAt: number;
}

export default class Msal {
  public static readonly DEVICE_CODE_CLIENT_ID =
    '1f907974-e22b-4810-a9de-d9647380c97e';
  public static readonly PKCE_CLIENT_ID = '000000004c20a908';
  public static readonly PKCE_REDIRECT_URI = 'ms-xal-000000004c20a908://auth';
  public static readonly OAUTH_SCOPE =
    'xboxlive.signin openid profile offline_access';

  private _tokenStore: TokenStore;
  private _progress?: (stage: string) => void;
  private readonly _clientId = Msal.DEVICE_CODE_CLIENT_ID;
  private _xstsToken?: XstsToken;
  private _gssvToken?: XstsToken;
  private _activeDeviceCode?: string;
  private _pkceVerifier?: string;
  private _pkceState?: string;

  constructor(tokenStore: TokenStore, progress?: (stage: string) => void) {
    this._tokenStore = tokenStore;
    this._progress = progress;
  }

  cancelDeviceCodeAuth(): void {
    this._activeDeviceCode = undefined;
  }

  getForceIp(): string {
    const _settings = getSettings();
    return (_settings?.force_region_ip || '').trim();
  }

  /**
   * Generates a 1-tap OAuth 2.0 Authorization Code + PKCE (S256) URL for Chrome Custom Tabs /
   * System Browser sign-in (`ms-xal-000000004c20a908://auth`).
   */
  createPkceAuthUrl(): string {
    const xalManager = (NativeModules as any)?.XalManager;
    const challengeObj: PkceChallenge = xalManager?.getCodeChallange?.() || {
      value: 'E9Melhoa2OwvFrGMTJguSR7e7IF75c185h1V12L-s7E',
      method: 'S256',
      verifier: 'dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk',
    };
    const state: string =
      xalManager?.getRandomState?.() || `pkce_${Date.now()}`;

    this._pkceVerifier = challengeObj.verifier;
    this._pkceState = state;

    const pendingSession: PendingPkceSession = {
      verifier: challengeObj.verifier,
      state,
      createdAt: Date.now(),
    };
    writePersistentAuthJson(PKCE_PENDING_KEY, JSON.stringify(pendingSession));

    const query = encodeFormParams({
      client_id: Msal.PKCE_CLIENT_ID,
      response_type: 'code',
      redirect_uri: Msal.PKCE_REDIRECT_URI,
      scope: Msal.OAUTH_SCOPE,
      code_challenge: challengeObj.value,
      code_challenge_method: challengeObj.method || 'S256',
      state,
      prompt: 'select_account',
    });

    return `https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?${query}`;
  }

  /**
   * Exchanges the `ms-xal-000000004c20a908://auth?code=...&state=...` redirect URL
   * for a 90-day OAuth 2.0 UserToken (`access_token` + `refresh_token`).
   */
  async exchangePkceRedirectUrl(redirectUrl: string): Promise<UserToken> {
    if (!redirectUrl || !redirectUrl.startsWith(Msal.PKCE_REDIRECT_URI)) {
      throw new Error('Invalid OAuth PKCE redirect URI');
    }

    const queryIndex = redirectUrl.indexOf('?');
    if (queryIndex < 0) {
      throw new Error('Missing query parameters on OAuth PKCE redirect URI');
    }

    const searchParams = parseQueryParams(redirectUrl.slice(queryIndex + 1));
    const oauthError = searchParams.error;
    if (oauthError) {
      const errorDesc = searchParams.error_description || oauthError;
      throw new Error(`OAuth sign-in cancelled or failed: ${errorDesc}`);
    }

    const code = searchParams.code;
    const returnedState = searchParams.state;
    if (!code) {
      throw new Error('Authorization code missing from redirect URI');
    }

    let verifier = this._pkceVerifier;
    let expectedState = this._pkceState;

    if (!verifier || !expectedState) {
      try {
        const persisted = JSON.parse(
          readPersistentAuthJson(PKCE_PENDING_KEY, '{}'),
        ) as Partial<PendingPkceSession>;
        verifier = verifier || persisted.verifier;
        expectedState = expectedState || persisted.state;
      } catch {
        // Ignore parse failure
      }
    }

    if (!verifier) {
      throw new Error('PKCE code_verifier missing for authorization exchange');
    }
    if (expectedState && returnedState && expectedState !== returnedState) {
      throw new Error('PKCE OAuth state mismatch');
    }

    this._progress?.('Completing login...');

    const payloadBody = encodeFormParams({
      client_id: Msal.PKCE_CLIENT_ID,
      grant_type: 'authorization_code',
      code,
      redirect_uri: Msal.PKCE_REDIRECT_URI,
      code_verifier: verifier,
      scope: Msal.OAUTH_SCOPE,
    });

    const forceIp = this.getForceIp();
    const headers: Record<string, string> = {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Cache-Control': 'no-store, must-revalidate, no-cache',
    };
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    const response = await axios.post(
      'https://login.microsoftonline.com/consumers/oauth2/v2.0/token',
      payloadBody,
      {
        headers,
        timeout: AUTH_REQUEST_TIMEOUT,
      },
    );

    const userToken = new UserToken({
      ...response.data,
      client_id: Msal.PKCE_CLIENT_ID,
    });

    clearPersistentAuthJson(PKCE_PENDING_KEY);
    this._pkceVerifier = undefined;
    this._pkceState = undefined;
    this._xstsToken = undefined;
    this._gssvToken = undefined;

    this._tokenStore.clear();
    this._tokenStore.setUserToken(userToken);
    this._tokenStore.save();

    return userToken;
  }

  /**
   * Creates a new device code authentication request (for Android TV / secondary device sign-in).
   */
  doDeviceCodeAuth(): Promise<DeviceCodeResponse> {
    this._progress?.('Getting authorization code...');
    const data = new URLSearchParams();
    data.append('client_id', this._clientId);
    data.append('scope', Msal.OAUTH_SCOPE);

    const forceIp = this.getForceIp();
    const headers: Record<string, string> = {
      'Content-Type': 'application/x-www-form-urlencoded',
    };
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    return axios
      .post(
        'https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode',
        data.toString(),
        {
          headers,
          timeout: AUTH_REQUEST_TIMEOUT,
        },
      )
      .then(response => {
        const body = response.data as DeviceCodeResponse;
        if (!body.verification_uri_complete && body.verification_uri && body.user_code) {
          const sep = body.verification_uri.includes('?') ? '&' : '?';
          body.verification_uri_complete = `${body.verification_uri}${sep}otc=${encodeURIComponent(
            body.user_code,
          )}`;
        }
        return body;
      })
      .catch((error: any) => {
        throw new Error(`Error during device code auth: ${error.message}`);
      });
  }

  /**
   * Keeps polling for authentication changes. The promise will be fulfilled once the user has authenticated.
   */
  async doPollForDeviceCodeAuth(
    deviceCode: string,
    timeout = 900 * 1000,
    startTime = Date.now(),
    isInitial = true,
  ): Promise<Record<string, any>> {
    if (isInitial) {
      this._activeDeviceCode = deviceCode;
    } else if (this._activeDeviceCode !== deviceCode) {
      throw new Error('Device code polling cancelled');
    }
    this._progress?.('Waiting for authorization...');
    const data = new URLSearchParams();
    data.append('grant_type', 'urn:ietf:params:oauth:grant-type:device_code');
    data.append('client_id', this._clientId);
    data.append('device_code', deviceCode);

    const forceIp = this.getForceIp();
    const headers: Record<string, string> = {
      'Content-Type': 'application/x-www-form-urlencoded',
    };
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    try {
      const response = await axios.post(
        'https://login.microsoftonline.com/consumers/oauth2/v2.0/token',
        data.toString(),
        {
          headers,
          timeout: AUTH_REQUEST_TIMEOUT,
        },
      );

      if (this._activeDeviceCode !== deviceCode) {
        throw new Error('Device code polling cancelled');
      }

      const body = response.data;
      const userToken = new UserToken({
        ...body,
        client_id: this._clientId,
      });

      this._xstsToken = undefined;
      this._gssvToken = undefined;
      this._tokenStore.clear();
      this._tokenStore.setUserToken(userToken);
      this._tokenStore.save();
      this._activeDeviceCode = undefined;

      return body;
    } catch (error: any) {
      if (this._activeDeviceCode !== deviceCode) {
        throw new Error('Device code polling cancelled');
      }
      if (Date.now() - startTime >= timeout) {
        throw new Error('doPollForDeviceCodeAuth timeout');
      }

      await new Promise<void>(resolve => setTimeout(resolve, 1000));
      return this.doPollForDeviceCodeAuth(
        deviceCode,
        timeout,
        startTime,
        false,
      );
    }
  }

  async getMsalToken(): Promise<MsalToken> {
    await this.getOrRefreshUserToken();

    const storedUserToken = this._tokenStore.getUserToken();
    const refreshToken = storedUserToken?.data.refresh_token;
    const activeClientId = storedUserToken?.data.client_id || this._clientId;

    if (!refreshToken) {
      throw new Error('No refresh token found. Please authenticate first.');
    }

    const data = new URLSearchParams();
    data.append('client_id', activeClientId);
    data.append(
      'scope',
      'service::http://Passport.NET/purpose::PURPOSE_XBOX_CLOUD_CONSOLE_TRANSFER_TOKEN',
    );
    data.append('grant_type', 'refresh_token');
    data.append('refresh_token', refreshToken);

    const forceIp = this.getForceIp();
    const headers: Record<string, string> = {
      'Content-Type': 'application/x-www-form-urlencoded',
    };
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    const response = await axios.post(
      'https://login.live.com/oauth20_token.srf',
      data.toString(),
      {
        headers,
        timeout: AUTH_REQUEST_TIMEOUT,
      },
    );

    const body = response.data;

    return new MsalToken({
      lpt: body.access_token,
      refresh_token: body.refresh_token,
      user_id: body.user_id,
    });
  }

  async doXstsAuthorization(
    userToken: string,
    relyingParty: string,
  ): Promise<XstsToken> {
    const payload = {
      Properties: {
        SandboxId: 'RETAIL',
        UserTokens: [userToken],
      },
      RelyingParty: relyingParty,
      TokenType: 'JWT',
    };

    const url = 'https://xsts.auth.xboxlive.com/xsts/authorize';

    const headers: Record<string, string> = {
      'x-xbl-contract-version': '1',
      'Cache-Control': 'no-cache',
      'Content-Type': 'application/json',
      Origin: 'https://www.xbox.com',
      Referer: 'https://www.xbox.com/',
      Accept: '*/*',
      'ms-cv': '0',
      'User-Agent':
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36',
    };

    const forceIp = this.getForceIp();
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    const response = await axios.post(url, payload, {
      headers,
      timeout: AUTH_REQUEST_TIMEOUT,
    });

    return new XstsToken(response.data);
  }

  async refreshUserToken(): Promise<UserToken> {
    try {
      const storedUserToken = this._tokenStore.getUserToken();
      const refreshToken = storedUserToken?.data.refresh_token;
      const activeClientId = storedUserToken?.data.client_id || this._clientId;

      if (!refreshToken) {
        throw new Error('No refresh token found. Please authenticate first.');
      }

      const payload: Record<string, string> = {
        client_id: activeClientId,
        grant_type: 'refresh_token',
        refresh_token: refreshToken,
        scope: Msal.OAUTH_SCOPE,
      };

      const url =
        'https://login.microsoftonline.com/consumers/oauth2/v2.0/token';
      const headers: Record<string, string> = {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Cache-Control': 'no-store, must-revalidate, no-cache',
      };

      const forceIp = this.getForceIp();
      if (forceIp.length > 0) {
        headers['X-Forwarded-For'] = forceIp;
      }
      const body = new URLSearchParams(payload).toString();

      const response = await axios.post(url, body, {
        headers,
        timeout: AUTH_REQUEST_TIMEOUT,
      });

      const userToken = new UserToken({
        ...response.data,
        refresh_token: response.data?.refresh_token || refreshToken,
        client_id: activeClientId,
      });

      this._tokenStore.setUserToken(userToken);
      this._tokenStore.save();

      return userToken;
    } catch (error: any) {
      const oauthError = error?.response?.data?.error;
      if (
        error?.response?.status === 400 &&
        (oauthError === 'invalid_grant' ||
          oauthError === 'interaction_required' ||
          oauthError === 'unauthorized_client')
      ) {
        throw new TokenRefreshError('Failed to refresh tokens', error);
      }
      throw error;
    }
  }

  async getOrRefreshUserToken(): Promise<string> {
    const userToken = this._tokenStore.getUserToken();

    if (userToken === undefined || userToken.getSecondsValid() <= 60) {
      return (await this.refreshUserToken()).data.access_token ?? '';
    }

    return userToken.data.access_token ?? '';
  }

  async doXstsAuthentication(): Promise<XstsToken> {
    const userToken = await this.getOrRefreshUserToken();
    if (!userToken) {
      throw new Error('No user token found. Please authenticate first.');
    }

    const payload = {
      Properties: {
        AuthMethod: 'RPS',
        RpsTicket: 'd=' + userToken,
        SiteName: 'user.auth.xboxlive.com',
      },
      RelyingParty: 'http://auth.xboxlive.com',
      TokenType: 'JWT',
    };

    const url = 'https://user.auth.xboxlive.com/user/authenticate';
    const headers: Record<string, string> = {
      'x-xbl-contract-version': '1',
      'Cache-Control': 'no-cache',
      'Content-Type': 'application/json',
      Origin: 'https://www.xbox.com',
      Referer: 'https://www.xbox.com/',
    };

    const forceIp = this.getForceIp();
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    const response = await axios.post(url, payload, {
      headers,
      timeout: AUTH_REQUEST_TIMEOUT,
    });

    this._xstsToken = new XstsToken(response.data);
    this._tokenStore.setXstsToken(this._xstsToken);
    this._tokenStore.save();
    return this._xstsToken;
  }

  private getOrHydrateXstsToken(): XstsToken | undefined {
    if (!this._xstsToken) {
      this._xstsToken = this._tokenStore.getXstsToken();
    }
    return this._xstsToken;
  }

  private getOrHydrateGssvToken(): XstsToken | undefined {
    if (!this._gssvToken) {
      this._gssvToken = this._tokenStore.getGssvToken();
    }
    return this._gssvToken;
  }

  async getWebToken(): Promise<XstsToken> {
    let xstsToken = this.getOrHydrateXstsToken();
    if (xstsToken === undefined || xstsToken.getSecondsValid() <= 60) {
      xstsToken = await this.doXstsAuthentication();
    }

    const userToken = xstsToken?.data.Token;

    if (userToken === undefined) {
      throw new Error('No user token found. Please authenticate first.');
    }

    return await this.doXstsAuthorization(userToken, 'http://xboxlive.com');
  }

  async getGssvToken(): Promise<XstsToken> {
    let gssvToken = this.getOrHydrateGssvToken();
    if (gssvToken !== undefined && gssvToken.getSecondsValid() > 60) {
      return gssvToken;
    }

    let xstsToken = this.getOrHydrateXstsToken();
    if (xstsToken === undefined || xstsToken.getSecondsValid() <= 60) {
      xstsToken = await this.doXstsAuthentication();
    }

    const userToken = xstsToken?.data.Token;

    if (userToken === undefined) {
      throw new Error('No user token found. Please authenticate first.');
    }

    gssvToken = await this.doXstsAuthorization(
      userToken,
      'http://gssv.xboxlive.com/',
    );
    this._gssvToken = gssvToken;
    this._tokenStore.setGssvToken(gssvToken);
    this._tokenStore.save();

    return gssvToken;
  }

  async getStreamingTokens(): Promise<{
    xHomeToken: StreamingToken;
    xCloudToken?: StreamingToken;
  }> {
    const gssvToken = await this.getGssvToken();

    if (gssvToken === undefined) {
      throw new Error('No gssv token found. Please authenticate first.');
    }

    const _xhomeToken = await this.getStreamToken(
      gssvToken.data.Token as string,
      'xhome',
    );

    let _xcloudToken;
    try {
      _xcloudToken = await this.getStreamToken(
        gssvToken.data.Token as string,
        'xgpuweb',
      );
    } catch {
      try {
        _xcloudToken = await this.getStreamToken(
          gssvToken.data.Token as string,
          'xgpuwebf2p',
        );
      } catch (err) {
        console.log(
          'Failed to get xcloud streaming token, probably a non-supported country. Error:',
          err,
        );
      }
    }

    return {xHomeToken: _xhomeToken, xCloudToken: _xcloudToken};
  }

  async getStreamToken(
    userToken: string,
    offering: string,
  ): Promise<StreamingToken> {
    const payload = {
      token: userToken,
      offeringId: offering,
    };

    const url = `https://${offering}.gssv-play-prod.xboxlive.com/v2/login/user`;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      'Cache-Control': 'no-store, must-revalidate, no-cache',
      'x-gssv-client': 'XboxComBrowser',
    };

    const forceIp = this.getForceIp();
    if (forceIp.length > 0) {
      headers['X-Forwarded-For'] = forceIp;
    }

    const response = await axios.post(url, payload, {
      headers,
      timeout: AUTH_REQUEST_TIMEOUT,
    });

    return new StreamingToken(response.data, offering);
  }
}
