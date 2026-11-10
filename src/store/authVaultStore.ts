// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
import {NativeModules} from 'react-native';
import {storage} from './mmkv';

interface NativeAuthVaultBridge {
  saveBackupToken?: (key: string, value: string) => void;
  getBackupToken?: (key: string) => string | null;
  clearBackupToken?: (key: string) => void;
}

const getNativeVault = (): NativeAuthVaultBridge | undefined => {
  return (NativeModules as {NativeUiBridge?: NativeAuthVaultBridge})
    ?.NativeUiBridge;
};

const isMeaningfulJsonPayload = (raw: string | undefined | null): boolean => {
  if (!raw) {
    return false;
  }
  const trimmed = raw.trim();
  return trimmed.length > 2 && trimmed !== '{}' && trimmed !== 'null';
};

/**
 * Reads a serialized auth store JSON string from MMKV first, and if missing or empty (`{}`),
 * automatically recovers it from the native Android SharedPreferences vault (`xstreaming_auth_vault`).
 * Also back-fills the native vault if MMKV has a valid payload that isn't mirrored yet.
 */
export const readPersistentAuthJson = (
  key: string,
  fallback = '{}',
): string => {
  let mmkvValue: string | undefined;
  try {
    mmkvValue = storage.getString(key);
  } catch {
    mmkvValue = undefined;
  }

  const nativeVault = getNativeVault();

  if (isMeaningfulJsonPayload(mmkvValue)) {
    try {
      nativeVault?.saveBackupToken?.(key, mmkvValue as string);
    } catch {
      // Ignore native bridge unavailability in pure JS test environments
    }
    return mmkvValue as string;
  }

  try {
    const backupValue = nativeVault?.getBackupToken?.(key);
    if (isMeaningfulJsonPayload(backupValue)) {
      storage.set(key, backupValue as string);
      return backupValue as string;
    }
  } catch {
    // Ignore native bridge unavailability
  }

  return mmkvValue ?? fallback;
};

/**
 * Synchronously persists a serialized auth store JSON string to both MMKV
 * and the native Android SharedPreferences vault (`xstreaming_auth_vault`).
 */
export const writePersistentAuthJson = (key: string, json: string): void => {
  try {
    storage.set(key, json);
  } catch {
    // Ignore storage write failure
  }

  if (isMeaningfulJsonPayload(json)) {
    try {
      getNativeVault()?.saveBackupToken?.(key, json);
    } catch {
      // Ignore native bridge unavailability
    }
  }
};

/**
 * Purges a serialized auth store key from both MMKV and the native Android SharedPreferences vault.
 * Should only be called on explicit user logout or permanent OAuth `invalid_grant` rejection.
 */
export const clearPersistentAuthJson = (key: string): void => {
  try {
    storage.delete(key);
  } catch {
    // Ignore delete failure
  }

  try {
    getNativeVault()?.clearBackupToken?.(key);
  } catch {
    // Ignore native bridge unavailability
  }
};
