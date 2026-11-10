import Msal from './xal/msal';
import TokenStore from './xal/tokenstore';
import {debugFactory} from './utils/debug';
import StreamingToken from './tokens/streamingtoken';
import {
  getStreamToken,
  saveStreamToken,
  clearStreamToken,
  isStreamTokenValid,
} from './store/streamTokenStore';
import {
  getWebToken,
  saveWebToken,
  clearWebToken,
  isWebTokenValid,
} from './store/webTokenStore';

const log = debugFactory('MsalAuthentication.ts');

export default class MsalAuthentication {
  _msal: Msal;
  _tokenStore: TokenStore;
  _isAuthenticating: boolean = false;
  _authenticationCompleted: any;
  _authenticationFailed: any;
  _authenticationProgress?: (stage: string) => void;

  constructor(
    authenticationCompleted: any,
    authenticationFailed: any,
    authenticationProgress?: (stage: string) => void,
  ) {
    this._tokenStore = new TokenStore();
    this._authenticationProgress = authenticationProgress;
    this._msal = new Msal(
      this._tokenStore,
      stage => this._authenticationProgress?.(stage),
    );
    this._authenticationCompleted = authenticationCompleted;
    this._authenticationFailed = authenticationFailed;
  }

  async checkAuthentication() {
    this._tokenStore.load();
    log.info('[checkAuthentication()] Starting token check...');

    const existingToken = this._tokenStore.getUserToken();
    if (!existingToken) {
      log.info('[checkAuthentication()] No tokens are present.');
      return false;
    }

    const scopeStr = String(existingToken.data.scope || '');
    if (
      this._tokenStore.getAuthenticationMethod() === 'xal' ||
      !/xboxlive\.signin/i.test(scopeStr)
    ) {
      log.info(
        '[checkAuthentication()] Deprecating old XAL token scope. Starting auth flow to get new tokens.',
      );
      clearStreamToken();
      clearWebToken();
      this._tokenStore.clear();
      return false;
    }

    if (existingToken.isValid()) {
      log.info('[checkAuthentication()] MSAL user token is valid.');
      this.startSilentFlow();
      return true;
    }

    log.info(
      '[checkAuthentication()] MSAL user token is expired, refreshing in silent flow.',
    );
    this.startSilentFlow();
    return true;
  }

  startSilentFlow() {
    if (this._isAuthenticating) {
      log.info('[startSilentFlow()] Authentication is already in progress.');
      return;
    }

    log.info('[startSilentFlow()] Starting silent flow...');
    this._isAuthenticating = true;
    void this.getTokens();
  }

  async getTokens() {
    try {
      const streamToken = getStreamToken();
      const cachedWebToken = getWebToken();
      const xHomeToken = streamToken?.xHomeToken;
      const xCloudToken = streamToken?.xCloudToken;

      if (
        xHomeToken &&
        isStreamTokenValid(xHomeToken) &&
        isWebTokenValid(cachedWebToken)
      ) {
        this._authenticationProgress?.('Using saved login credentials...');
        await this._authenticationCompleted(
          {
            xHomeToken: new StreamingToken(
              xHomeToken.data,
              xHomeToken.offering,
              xHomeToken._objectCreateTime,
            ),
            xCloudToken: xCloudToken
              ? new StreamingToken(
                  xCloudToken.data,
                  xCloudToken.offering,
                  xCloudToken._objectCreateTime,
                )
              : xCloudToken,
          },
          cachedWebToken,
        );
        return;
      }

      this._authenticationProgress?.('Getting streaming credentials...');
      const streamingTokens = await this.getStreamingToken();
      log.info('[getTokens()] Retrieved streaming tokens:' + streamingTokens);

      this._authenticationProgress?.('Getting web credentials...');
      const webToken = await this._msal.getWebToken();
      log.info('[getTokens()] Web token received:' + webToken);
      saveStreamToken(streamingTokens);
      saveWebToken(webToken);
      await this._authenticationCompleted(streamingTokens, webToken);
    } catch (error: any) {
      log.info('[getTokens()] Authentication token request failed:', error);
      const isPermanentAuthRejection =
        error?.name === 'TokenRefreshError' ||
        error?.message === 'sisuTokenIsMissing' ||
        String(error?.message || '').includes('No refresh token found');

      if (isPermanentAuthRejection) {
        clearStreamToken();
        clearWebToken();
        this._tokenStore.clear();
      }
      this._authenticationFailed(
        '[MSAL getTokens()] Login failed, please try again:' +
          (error?.message || String(error)),
        isPermanentAuthRejection,
      );
    } finally {
      this._isAuthenticating = false;
    }
  }

  async getStreamingToken() {
    const userToken = this._tokenStore.getUserToken();
    if (!userToken) {
      throw new Error('sisuTokenIsMissing');
    }
    log.info('[getStreamingToken()] Found local token:' + userToken);

    const streamingTokens = await this._msal.getStreamingTokens();

    log.info(
      '[getStreamingToken()] Retrieved streaming tokens:' + streamingTokens,
    );
    return {
      xHomeToken: streamingTokens.xHomeToken,
      xCloudToken: streamingTokens.xCloudToken,
    };
  }

  /**
   * Generates a 1-tap OAuth 2.0 PKCE authorize URL for Chrome Custom Tabs / System Browser.
   */
  getPkceAuthorizeUrl(): string {
    return this._msal.createPkceAuthUrl();
  }

  /**
   * Completes 1-tap OAuth 2.0 PKCE sign-in when `ms-xal-000000004c20a908://auth?code=...` returns.
   */
  async handlePkceRedirectUrl(redirectUrl: string): Promise<boolean> {
    if (!redirectUrl || !redirectUrl.startsWith(Msal.PKCE_REDIRECT_URI)) {
      return false;
    }
    try {
      this._authenticationProgress?.('Completing login...');
      await this._msal.exchangePkceRedirectUrl(redirectUrl);
      clearStreamToken();
      clearWebToken();
      await this.getTokens();
      return true;
    } catch (error: any) {
      log.info('[handlePkceRedirectUrl()] PKCE auth error:', error);
      this._authenticationFailed(
        '[PKCE Sign-In] ' + (error?.message || String(error)),
        false,
      );
      return false;
    }
  }

  getMsalDeviceCode() {
    log.info('[getMsalDeviceCode()] Starting get device code');
    this._authenticationProgress?.('Getting authorization code...');
    return this._msal.doDeviceCodeAuth();
  }

  cancelDeviceCodeAuth() {
    this._msal.cancelDeviceCodeAuth();
  }

  doPollForDeviceCodeAuth(deviceCode: any) {
    this._authenticationProgress?.('Waiting for authorization...');
    return this._msal
      .doPollForDeviceCodeAuth(deviceCode)
      .then((token: any) => {
        log.info(
          '[doPollForDeviceCodeAuth()] Devicecode authentication successful:' +
            token,
        );

        this._authenticationProgress?.('Completing login...');
        clearStreamToken();
        clearWebToken();
        this.getTokens();
      })
      .catch((error: any) => {
        if (error?.message === 'Device code polling cancelled') {
          log.info('[doPollForDeviceCodeAuth()] Polling cancelled by user.');
          return;
        }
        log.info(
          '[doPollForDeviceCodeAuth()] Error during devicecode polling auth:' +
            error.message,
        );
        this._authenticationFailed(
          '[MSAL doPollForDeviceCodeAuth()] Failed to retrieve device code token:' +
            error.message,
          false,
        );
      });
  }
}
