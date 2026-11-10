import React from 'react';
import {
  StyleSheet,
  View,
  ToastAndroid,
  Platform,
  Linking,
  TouchableOpacity,
} from 'react-native';
import {
  Text,
  Button,
  ActivityIndicator,
  Surface,
  useTheme,
} from 'react-native-paper';
import {useTranslation} from 'react-i18next';
import QRCode from 'react-native-qrcode-svg';
import RNRestart from 'react-native-restart';
import Clipboard from '@react-native-clipboard/clipboard';
import {
  useGamepadNavigation,
  useGamepadActiveState,
} from '../utils/useGamepadNavigation';

type Props = {
  data: any;
  onCancel?: () => void;
};

const formatSeconds = (seconds: number) => {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};

const MsalAuth: React.FC<Props> = ({data, onCancel}) => {
  const {t} = useTranslation();
  const theme = useTheme();
  const [countdown, setCountdown] = React.useState(data.expires_in);
  const [isGamepadActive] = useGamepadActiveState();
  const [focusedBtn, setFocusedBtn] = React.useState<
    'open' | 'copy' | 'cancel'
  >('open');

  const copyAndOpenBrowser = React.useCallback(() => {
    if (data?.user_code) {
      Clipboard.setString(data.user_code);
      ToastAndroid.show(t('Copied'), ToastAndroid.SHORT);
    }
    const targetUrl =
      data?.verification_uri_complete ||
      data?.verification_uri ||
      'https://login.live.com/oauth20_remoteconnect.srf';
    Linking.openURL(targetUrl);
  }, [
    data?.user_code,
    data?.verification_uri_complete,
    data?.verification_uri,
    t,
  ]);

  const copyCodeOnly = React.useCallback(() => {
    if (data?.user_code) {
      Clipboard.setString(data.user_code);
      ToastAndroid.show(t('Copied'), ToastAndroid.SHORT);
    }
  }, [data?.user_code, t]);

  useGamepadNavigation({
    priority: 10,
    onUp: () => {
      setFocusedBtn(curr => {
        if (curr === 'cancel') return 'copy';
        if (curr === 'copy') return 'open';
        return 'open';
      });
    },
    onDown: () => {
      setFocusedBtn(curr => {
        if (curr === 'open') return 'copy';
        if (curr === 'copy') return 'cancel';
        return 'cancel';
      });
    },
    onSelect: () => {
      if (focusedBtn === 'open') {
        if (countdown > 0) {
          copyAndOpenBrowser();
        } else {
          RNRestart.restart();
        }
      } else if (focusedBtn === 'copy') {
        copyCodeOnly();
      } else {
        onCancel?.();
      }
    },
    onBack: () => {
      onCancel?.();
    },
  });

  React.useEffect(() => {
    if (data?.user_code) {
      Clipboard.setString(data.user_code);
      ToastAndroid.show(t('Copied'), ToastAndroid.SHORT);
    }
  }, [data?.user_code, t]);

  React.useEffect(() => {
    if (countdown <= 0) {
      return;
    }

    const timer = setInterval(() => {
      setCountdown(prev => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(timer);
  }, [countdown]);

  return (
    <View style={styles.container}>
      <Surface style={styles.card} elevation={2}>
        <Text variant="titleLarge" style={styles.title}>
          Microsoft / Xbox Sign-In
        </Text>
        <Text variant="bodyMedium" style={styles.subtitle}>
          Code copied to clipboard. Open the link below and paste your code -
          this screen will continue automatically as soon as you finish signing
          in.
        </Text>

        {countdown > 0 && (
          <TouchableOpacity
            activeOpacity={0.8}
            onPress={copyCodeOnly}
            style={[
              styles.codeBox,
              {borderColor: theme.colors.primary},
              (isGamepadActive || Platform.isTV) &&
                focusedBtn === 'copy' &&
                styles.tvButtonFocused,
            ]}>
            <Text
              variant="headlineMedium"
              style={[styles.codeText, {color: theme.colors.primary}]}>
              {data.user_code}
            </Text>
            <Text variant="labelSmall" style={styles.copyHint}>
              Tap to copy • Expires in {formatSeconds(countdown)}
            </Text>
          </TouchableOpacity>
        )}

        {countdown > 0 ? (
          <View style={styles.actionWrap}>
            <Button
              style={[
                styles.primaryButton,
                (isGamepadActive || Platform.isTV) &&
                  focusedBtn === 'open' &&
                  styles.tvButtonFocused,
              ]}
              mode="contained"
              buttonColor={theme.colors.primary}
              textColor="#FFFFFF"
              onPress={copyAndOpenBrowser}>
              Copy Code & Open {data.verification_uri}
            </Button>

            <View style={styles.waitingRow}>
              <ActivityIndicator size="small" color={theme.colors.primary} />
              <Text variant="labelMedium" style={styles.waitingText}>
                Waiting for authorization in browser...
              </Text>
            </View>
          </View>
        ) : (
          <View style={styles.actionWrap}>
            <Text variant="labelSmall" style={styles.red}>
              {t('MsalAuthTimeout')}
            </Text>
            <Button
              style={[
                styles.mt10,
                styles.primaryButton,
                (isGamepadActive || Platform.isTV) &&
                  focusedBtn === 'open' &&
                  styles.tvButtonFocused,
              ]}
              mode="contained"
              onPress={() => RNRestart.restart()}>
              &nbsp;{t('Refresh')}&nbsp;
            </Button>
          </View>
        )}

        <View style={styles.qrSection}>
          <View style={styles.qrWrapper}>
            <QRCode
              value={data.verification_uri_complete || data.verification_uri}
              size={120}
              color="#000000"
            />
          </View>
        </View>

        {onCancel && (
          <Button
            style={[
              styles.mt10,
              styles.actionButton,
              (isGamepadActive || Platform.isTV) &&
                focusedBtn === 'cancel' &&
                styles.tvButtonFocused,
            ]}
            mode={
              (isGamepadActive || Platform.isTV) && focusedBtn === 'cancel'
                ? 'contained'
                : 'text'
            }
            buttonColor={
              (isGamepadActive || Platform.isTV) && focusedBtn === 'cancel'
                ? theme.colors.primary
                : undefined
            }
            textColor={
              (isGamepadActive || Platform.isTV) && focusedBtn === 'cancel'
                ? '#FFFFFF'
                : undefined
            }
            onPress={onCancel}>
            &nbsp;{t('Cancel')}&nbsp;
          </Button>
        )}
      </Surface>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 16,
  },
  card: {
    maxWidth: 460,
    width: '100%',
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
  },
  title: {
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 8,
  },
  subtitle: {
    textAlign: 'center',
    opacity: 0.8,
    marginBottom: 16,
    lineHeight: 20,
  },
  codeBox: {
    borderWidth: 2,
    borderRadius: 12,
    paddingVertical: 12,
    paddingHorizontal: 28,
    alignItems: 'center',
    marginBottom: 16,
    backgroundColor: 'rgba(255,255,255,0.04)',
  },
  codeText: {
    fontWeight: '800',
    letterSpacing: 4,
  },
  copyHint: {
    opacity: 0.7,
    marginTop: 4,
  },
  center: {
    textAlign: 'center',
  },
  red: {
    color: 'red',
  },
  mt10: {
    marginTop: 10,
  },
  mb10: {
    marginBottom: 10,
  },
  actionWrap: {
    alignItems: 'center',
    width: '100%',
  },
  primaryButton: {
    minWidth: 260,
    borderRadius: 10,
    paddingVertical: 4,
  },
  actionButton: {
    minWidth: 180,
    borderRadius: 8,
  },
  waitingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 14,
    gap: 10,
  },
  waitingText: {
    opacity: 0.8,
    marginLeft: 8,
  },
  qrSection: {
    marginTop: 16,
    alignItems: 'center',
  },
  qrWrapper: {
    padding: 10,
    backgroundColor: '#FFFFFF',
    borderRadius: 10,
  },
  tvButtonFocused: {
    borderColor: '#FFFFFF',
    borderWidth: 2.5,
    transform: [{scale: 1.05}],
    elevation: 8,
  },
});

export default MsalAuth;
