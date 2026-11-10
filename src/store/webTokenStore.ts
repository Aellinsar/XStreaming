import {
  readPersistentAuthJson,
  writePersistentAuthJson,
  clearPersistentAuthJson,
} from './authVaultStore';
import {debugFactory} from '../utils/debug';
const log = debugFactory('webTokenStore');

const STORE_KEY = 'user.webToken';

export const saveWebToken = (token: any) => {
  log.info('saveStreamToken:', token);
  writePersistentAuthJson(STORE_KEY, JSON.stringify(token));
};

export const getWebToken = (): any => {
  const token = readPersistentAuthJson(STORE_KEY, '');
  if (!token) {
    return null;
  }
  try {
    const _token = JSON.parse(token) as any;
    return _token;
  } catch {
    return null;
  }
};

export const clearWebToken = () => {
  clearPersistentAuthJson(STORE_KEY);
};

const calculateSecondsLeft = (date: any) => {
  const expiresOn = date;
  const currentDate = new Date();
  return Math.floor((expiresOn.getTime() - currentDate.getTime()) / 1000);
};

export const isWebTokenValid = (token: any) => {
  if (!token || !token.data || !token.data.NotAfter) {
    return false;
  }
  if (calculateSecondsLeft(new Date(token.data.NotAfter)) <= 0) {
    return false;
  }

  return true;
};
