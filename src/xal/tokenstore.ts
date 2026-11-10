import {
  readPersistentAuthJson,
  writePersistentAuthJson,
  clearPersistentAuthJson,
} from '../store/authVaultStore';
import SisuToken, {SisuTokenData} from '../tokens/sisutoken';
import UserToken, {UserTokenData} from '../tokens/usertoken';
import XstsToken, {XstsTokenData} from '../tokens/xststoken';

const STORE_KEY = 'user.tokenstore';

export type AuthenticationMethod = 'xal' | 'msal' | 'none';

export type TokenStoreData = {
  userToken?: UserTokenData;
  sisuToken?: SisuTokenData;
  xstsToken?: XstsTokenData;
  gssvToken?: XstsTokenData;
  tokenUpdateTime?: number;
};

export default class TokenStore {
  private _userToken?: UserToken;
  private _sisuToken?: SisuToken;
  private _xstsToken?: XstsToken;
  private _gssvToken?: XstsToken;
  private _tokenUpdateTime = 0;

  load(): boolean {
    let tokens = '{}';
    try {
      tokens = readPersistentAuthJson(STORE_KEY, '{}');
    } catch {
      tokens = '{}';
    }
    this.loadJson(tokens);

    return true;
  }

  loadJson(json: string): boolean {
    let jsonData: TokenStoreData = {};
    try {
      jsonData = JSON.parse(json) as TokenStoreData;
    } catch {
      jsonData = {};
    }

    this._userToken = jsonData.userToken
      ? new UserToken(jsonData.userToken)
      : undefined;

    this._sisuToken = jsonData.sisuToken
      ? new SisuToken(jsonData.sisuToken)
      : undefined;

    this._xstsToken = jsonData.xstsToken
      ? new XstsToken(jsonData.xstsToken)
      : undefined;

    this._gssvToken = jsonData.gssvToken
      ? new XstsToken(jsonData.gssvToken)
      : undefined;

    this._tokenUpdateTime = jsonData.tokenUpdateTime ?? 0;

    return true;
  }

  exportData(): TokenStoreData {
    return {
      userToken: this._userToken?.data,
      sisuToken: this._sisuToken?.data,
      xstsToken: this._xstsToken?.data,
      gssvToken: this._gssvToken?.data,
      tokenUpdateTime: this._tokenUpdateTime,
    };
  }

  saveData(data: TokenStoreData): void {
    const normalizedData: TokenStoreData = {
      userToken: data?.userToken,
      sisuToken: data?.sisuToken,
      xstsToken: data?.xstsToken,
      gssvToken: data?.gssvToken,
      tokenUpdateTime: data?.tokenUpdateTime ?? 0,
    };

    const serialized = JSON.stringify(normalizedData);
    writePersistentAuthJson(STORE_KEY, serialized);
    this._userToken = undefined;
    this._sisuToken = undefined;
    this._xstsToken = undefined;
    this._gssvToken = undefined;
    this._tokenUpdateTime = 0;
    this.loadJson(serialized);
  }

  setUserToken(userToken: UserToken): void {
    const expireDate = new Date();
    const expiresIn = userToken.data.expires_in ?? 0;
    if (expiresIn > 0) {
      expireDate.setSeconds(expireDate.getSeconds() + expiresIn);
    }

    this._userToken = new UserToken({
      ...userToken.data,
      expires_on:
        expiresIn > 0
          ? expireDate.toISOString()
          : userToken.data.expires_on || expireDate.toISOString(),
    });
  }

  getUserToken(): UserToken | undefined {
    return this._userToken;
  }

  setSisuToken(sisuToken: SisuToken): void {
    this._sisuToken = new SisuToken(sisuToken.data);
  }

  getSisuToken(): SisuToken | undefined {
    return this._sisuToken;
  }

  setXstsToken(xstsToken: XstsToken | undefined): void {
    this._xstsToken = xstsToken ? new XstsToken(xstsToken.data) : undefined;
  }

  getXstsToken(): XstsToken | undefined {
    return this._xstsToken;
  }

  setGssvToken(gssvToken: XstsToken | undefined): void {
    this._gssvToken = gssvToken ? new XstsToken(gssvToken.data) : undefined;
  }

  getGssvToken(): XstsToken | undefined {
    return this._gssvToken;
  }

  getTokenUpdateTime(): number {
    return this._tokenUpdateTime;
  }

  save(): void {
    const data = JSON.stringify({
      userToken: this._userToken?.data,
      sisuToken: this._sisuToken?.data,
      xstsToken: this._xstsToken?.data,
      gssvToken: this._gssvToken?.data,
      tokenUpdateTime: Date.now(),
    });

    writePersistentAuthJson(STORE_KEY, data);
  }

  clear(): void {
    clearPersistentAuthJson(STORE_KEY);

    this._userToken = undefined;
    this._sisuToken = undefined;
    this._xstsToken = undefined;
    this._gssvToken = undefined;
  }

  clearTokenUpdateTime(): void {
    const data = JSON.stringify({
      userToken: this._userToken?.data,
      sisuToken: this._sisuToken?.data,
      xstsToken: this._xstsToken?.data,
      gssvToken: this._gssvToken?.data,
      tokenUpdateTime: 0,
    });

    writePersistentAuthJson(STORE_KEY, data);
    this._tokenUpdateTime = 0;
  }

  hasValidAuthTokens(): boolean {
    if (!this._userToken || !this._userToken.isValid()) {
      return false;
    }

    if (!this._sisuToken || !this._sisuToken.isValid()) {
      return false;
    }

    return true;
  }

  getAuthenticationMethod(): AuthenticationMethod {
    if (this._sisuToken) {
      return 'xal';
    } else if (this._userToken) {
      return 'msal';
    }
    return 'none';
  }
}
