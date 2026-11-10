import React from 'react';
import {
  StyleSheet,
  View,
  Alert,
  Platform,
  ScrollView,
  Dimensions,
  useWindowDimensions,
  StatusBar,
  NativeModules,
  ToastAndroid,
  Linking,
  BackHandler,
  TouchableOpacity,
  DeviceEventEmitter,
} from 'react-native';
import {useSafeAreaInsets} from 'react-native-safe-area-context';
import {
  Button,
  Text,
  Portal,
  Modal,
  Card,
  IconButton,
  Icon,
  useTheme,
} from 'react-native-paper';
import {useIsFocused} from '@react-navigation/native';
import RNRestart from 'react-native-restart';
import ConsoleItem from '../components/ConsoleItem';
import HomeItem from '../components/HomeItem';
import {
  resolveThemeTokens,
  THEME_PRESET_LIST,
  THEME_PRESETS,
} from '../theme/presets';
import {
  THEME_PRIMARY_COLOR_OPTIONS,
  hexToRgba,
} from '../utils/themeColor';
import GamepadFooterHints, {
  GamepadHintItem,
} from '../components/GamepadFooterHints';
import {enableSystemRotationSuggestions} from '../utils/orientation';
import {
  getSettings,
  saveSettings,
  SETTINGS_CHANGED_EVENT,
  Settings,
} from '../store/settingStore';

import MsalAuthentication from '../MsalAuthentication';
import WebApi from '../web';
import {
  useGamepadNavigation,
  useGamepadActiveState,
} from '../utils/useGamepadNavigation';

import {useSelector, useDispatch} from 'react-redux';
import SplashScreen from 'react-native-splash-screen';
import {useTranslation} from 'react-i18next';
import NetInfo from '@react-native-community/netinfo';
import {debugFactory} from '../utils/debug';
import XcloudApi from '../xCloud';
import {
  saveConsolesData,
  getConsolesData,
  isConsolesDataValid,
} from '../store/consolesStore';
import {getXcloudData} from '../store/xcloudStore';
import {clearStreamToken} from '../store/streamTokenStore';
import {clearWebToken} from '../store/webTokenStore';
import {storage} from '../store/mmkv';
import {
  extractGamertagFromClaimsOrProfile,
  extractGamerpicFromProfile,
  resolveXboxGamertag,
  resolveXboxGamerpic,
} from '../utils/xcloud';
import MsalAuth from '../components/MsalAuth';
import SessionReportModal from '../components/SessionReportModal';
import Spinner from '../components/Spinner';

const log = debugFactory('HomeScreen');

const {UsbRumbleManager, FullScreenManager, NativeUiBridge, GamepadManager} =
  NativeModules;

const HARMOBY_URL =
  'https://appgallery.huawei.com/app/detail?id=com.lijiahao.xstreamingoh';

function HomeScreen({navigation, route}) {
  const {t} = useTranslation();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const [loading, setLoading] = React.useState(false);
  const [loadingText, setLoadingText] = React.useState('');
  const [consoles, setConsoles] = React.useState<any[]>(() => {
    const cacheData = getConsolesData();
    if (
      cacheData &&
      isConsolesDataValid(cacheData) &&
      Array.isArray(cacheData.consoles)
    ) {
      return cacheData.consoles;
    }
    return [];
  });
  const [isConnected, setIsConnected] = React.useState(true);
  const [currentConsoleId, setCurrentConsoleId] = React.useState('');
  const [showUsbWarnModal, setShowUsbWarnShowModal] = React.useState(false);
  const [showHarmonyModal, setShowHarmonyModal] = React.useState(false);
  const [numColumns, setNumColumns] = React.useState(2);
  const [showMsalLogin, setShowMsalLogin] = React.useState(false);
  const [showMsal, setShowMsal] = React.useState(false);
  const [msalBtnLoading, setMsalBtnLoading] = React.useState(false);
  const [msalData, setMsalData] = React.useState(null);
  const [showThemeModal, setShowThemeModal] = React.useState(false);
  const [liveSettings, setLiveSettings] = React.useState<Settings>(() =>
    getSettings(),
  );

  React.useEffect(() => {
    const sub = DeviceEventEmitter.addListener(
      SETTINGS_CHANGED_EVENT,
      (next: Settings) => {
        if (next) {
          setLiveSettings({...next});
        }
      },
    );
    return () => sub.remove();
  }, []);

  // Session report modal state
  const [sessionReport, setSessionReport] = React.useState<any>(null);

  React.useEffect(() => {
    if (route?.params?.sessionReport) {
      const currentSettings = getSettings();
      const isShowReport =
        String(currentSettings.show_session_report) === 'true';
      if (isShowReport) {
        setSessionReport(route.params.sessionReport);
      }
    }
  }, [route?.params?.sessionReport]);

  const handleDismissReport = React.useCallback(() => {
    setSessionReport(null);
    navigation.setParams({sessionReport: undefined});
  }, [navigation]);

  const handleDoneReport = React.useCallback(
    (dontShowAgain: boolean) => {
      if (dontShowAgain) {
        const currentSettings = getSettings();
        saveSettings({...currentSettings, show_session_report: false});
      }
      setSessionReport(null);
      navigation.setParams({sessionReport: undefined});
    },
    [navigation],
  );

  const authentication = useSelector((state: any) => state.authentication);
  const _authentication = React.useRef(authentication);

  const isLogined = useSelector((state: any) => state.isLogined);
  const _isLogined = React.useRef(isLogined);

  const streamingTokens = useSelector((state: any) => state.streamingTokens);
  const webToken = useSelector((state: any) => state.webToken);
  const profile = useSelector((state: any) => state.profile);
  const [fetchedGamertag, setFetchedGamertag] = React.useState('');
  const [fetchedGamerpic, setFetchedGamerpic] = React.useState(
    () => storage.getString('user.gamerpic') || '',
  );
  const webTokenRef = React.useRef(null);

  const _xHomeApiRef = React.useRef<any>(null);

  const dispatch = useDispatch();

  const isFocused = useIsFocused();
  const _isFocused = React.useRef(isFocused);
  React.useEffect(() => {
    _isFocused.current = isFocused;
    if (isFocused) {
      enableSystemRotationSuggestions();
      NativeUiBridge?.setRouteScreen?.('home');
    }
  }, [isFocused]);

  const {width, height} = useWindowDimensions();
  const emptyConsoleCardStyle = React.useMemo(
    () => [styles.emptyConsoleCard, theme.dark && styles.emptyConsoleCardDark],
    [theme.dark],
  );

  // 1. One-time mount initialization (listeners, non-blocking checks)
  React.useEffect(() => {
    log.info('Page loaded.');
    SplashScreen.hide();

    const updateLayout = () => {
      const {width: w, height: h} = Dimensions.get('window');
      setNumColumns(w > h ? 4 : 2);
    };

    updateLayout();
    const subscription = Dimensions.addEventListener('change', updateLayout);

    const unsubscribeNet = NetInfo.addEventListener((state: any) => {
      setIsConnected(state.isConnected);
    });

    // Defer heavy synchronous native calls so they don't block the initial render frame
    const timer = setTimeout(() => {
      try {
        const _settings = getSettings();
        const webviewVersion = FullScreenManager?.getWebViewVersion?.();
        const deviceInfos = FullScreenManager?.getDeviceInfos?.();
        if (webviewVersion && deviceInfos) {
          const verArr = webviewVersion.split('.');
          const mainVer = parseInt(verArr[0], 10);
          if (deviceInfos.androidVer < 12 && mainVer < 91) {
            _settings.render_engine = 'native';
            saveSettings(_settings);
          }
        }

        if (
          deviceInfos &&
          deviceInfos.factor &&
          deviceInfos.factor.indexOf('HUAWEI') > -1 &&
          _settings.locale === 'zh' &&
          _settings.show_harmony_modal
        ) {
          setShowHarmonyModal(true);
        }
      } catch (err) {
        log.warn('Device info check error:', err);
      }
    }, 500);

    return () => {
      subscription?.remove();
      unsubscribeNet();
      clearTimeout(timer);
    };
  }, []);

  // 2. Authentication & Consoles Synchronization
  React.useEffect(() => {
    if (!isConnected) {
      Alert.alert(
        t('Warning'),
        t('Currently no network connection, please connect and try again'),
        [
          {
            text: t('Confirm'),
            style: 'default',
            onPress: () => {},
          },
        ],
      );
      return;
    }

    const _settings = getSettings();

    const authenticationProgress = (stage: string) => {
      setLoadingText(t(stage));
      if (stage === 'Completing login...') {
        setLoading(true);
        setShowMsalLogin(false);
        setShowMsal(false);
      }
    };

    // Auth completed callback
    const authenticationCompleted = async (
      _streamingTokens: any,
      _webToken: any,
    ) => {
      log.info('Authentication completed');
      webTokenRef.current = _webToken;
      dispatch({
        type: 'SET_STREAMING_TOKEN',
        payload: _streamingTokens,
      });
      dispatch({
        type: 'SET_WEB_TOKEN',
        payload: _webToken,
      });
      dispatch({
        type: 'SET_LOGIN',
        payload: true,
      });
      _isLogined.current = true;
      setShowMsalLogin(false);
      setShowMsal(false);

      const webApi = new WebApi(_webToken);
      webApi
        .getUserProfileV2()
        .then((person: any) => {
          const uniqueTag = extractGamertagFromClaimsOrProfile(person);
          if (uniqueTag) {
            setFetchedGamertag(uniqueTag);
            storage.set('user.unique_gamertag', uniqueTag);
          }
          const picUrl = extractGamerpicFromProfile(person);
          if (picUrl) {
            setFetchedGamerpic(picUrl);
            storage.set('user.gamerpic', picUrl);
          } else {
            webApi
              .getUserProfile()
              .then((info: any) => {
                const v1Pic = extractGamerpicFromProfile(info);
                if (v1Pic) {
                  setFetchedGamerpic(v1Pic);
                  storage.set('user.gamerpic', v1Pic);
                }
              })
              .catch(() => {});
          }
        })
        .catch(() => {
          webApi
            .getUserProfile()
            .then((info: any) => {
              const fallbackTag = extractGamertagFromClaimsOrProfile(info);
              if (fallbackTag) {
                setFetchedGamertag(fallbackTag);
                storage.set('user.unique_gamertag', fallbackTag);
              }
              const fallbackPic = extractGamerpicFromProfile(info);
              if (fallbackPic) {
                setFetchedGamerpic(fallbackPic);
                storage.set('user.gamerpic', fallbackPic);
              }
            })
            .catch(() => {});
        });

      try {
        authenticationProgress('Fetching consoles...');
        const _xHomeApi = new XcloudApi(
          _streamingTokens.xHomeToken.getDefaultRegion().baseUri,
          _streamingTokens.xHomeToken.data.gsToken,
          'home',
          () => {},
        );
        _xHomeApiRef.current = _xHomeApi;

        let _consoles: any = await _xHomeApi.getConsoles();

        if (!_consoles || !_consoles.length) {
          _consoles = await webApi.getConsoles();
        }

        if (_consoles && _consoles.length > 0) {
          setConsoles(_consoles);
          saveConsolesData({
            consoles: _consoles,
          });
        }
      } catch (e: any) {
        log.error('Fetch consoles error:', e);
      } finally {
        setLoading(false);
      }
    };

    // Auth failed callback
    const authenticationFailed = (msg: any, isPermanentRejection?: boolean) => {
      setLoading(false);
      Alert.alert(t('Error'), t('AuthFailDesc') + msg, [
        {
          text: t('Confirm'),
          style: 'default',
          onPress: () => {
            if (isPermanentRejection || consoles.length === 0) {
              setShowMsalLogin(true);
            }
            setShowMsal(false);
          },
        },
      ]);
    };

    if (!_authentication.current) {
      log.info('Authentication initial (MSAL OAuth Device Code).');
      _authentication.current = new MsalAuthentication(
        authenticationCompleted,
        authenticationFailed,
        authenticationProgress,
      );
      _authentication.current._tokenStore.load();
      dispatch({
        type: 'SET_AUTHENTICATION',
        payload: _authentication.current,
      });
    } else {
      // Keep callbacks bound to the active HomeScreen render/instance
      // so switching to Chrome and returning never invokes a stale closure.
      _authentication.current._authenticationCompleted =
        authenticationCompleted;
      _authentication.current._authenticationFailed = authenticationFailed;
      _authentication.current._authenticationProgress = authenticationProgress;
    }

    if (route.params?.needRefresh && webTokenRef.current) {
      if (_xHomeApiRef.current) {
        const webApi = new WebApi(webTokenRef.current);
        _xHomeApiRef.current.getConsoles().then((_consoles: any) => {
          if (!_consoles.length) {
            webApi.getConsoles().then((_consolesV1: any) => {
              if (_consolesV1.length) {
                setConsoles(_consolesV1);
                saveConsolesData({consoles: _consolesV1});
              }
            });
          } else {
            setConsoles(_consoles);
            saveConsolesData({consoles: _consoles});
          }
        });
      }
    } else if (!_isLogined.current && _isFocused.current) {
      // If we already have cached consoles displayed, refresh silently without freezing UI
      const hasCachedConsoles = consoles.length > 0;
      if (!hasCachedConsoles) {
        setLoading(true);
        setLoadingText(t('Checking login status...'));
      }

      _authentication.current
        .checkAuthentication()
        .then((isAuth: boolean) => {
          if (!isAuth) {
            setLoading(false);
            setShowMsalLogin(true);
            setShowMsal(false);
          }
        })
        .catch((e: any) => {
          authenticationFailed(
            '[checkAuthentication()] Login check failed:' +
              (e?.message || String(e)),
          );
        });
    }
  }, [route.params?.needRefresh, isConnected, isFocused]);

  const ensureAuthReadyForStream = async () => {
    if (_isLogined.current && (webTokenRef.current || webToken)) {
      return true;
    }
    if (_authentication.current?.checkAuthentication) {
      try {
        return await _authentication.current.checkAuthentication();
      } catch (_e) {
        return false;
      }
    }
    return false;
  };

  const handlePoweronAndStream = async sessionId => {
    setLoading(true);
    setLoadingText(t('Loading...'));
    await ensureAuthReadyForStream();
    const activeWebToken = webTokenRef.current || webToken;
    const webApi = new WebApi(activeWebToken);
    try {
      const powerOnRes = await webApi.powerOn(sessionId);
      log.info('powerOn:', powerOnRes);
      setLoading(false);
      handleStartStream(sessionId);
    } catch (e) {
      setLoading(false);
      handleStartStream(sessionId);
    }
  };

  const handlePower = async (sessionId, off = false) => {
    setLoading(true);
    setLoadingText(t('Loading...'));
    const webApi = new WebApi(webToken);
    try {
      if (off) {
        const powerOnRes = await webApi.powerOff(sessionId);
        log.info('powerOff:', powerOnRes);
        ToastAndroid.show(t('PoweredOffSentText'), ToastAndroid.SHORT);
      } else {
        const powerOnRes = await webApi.powerOn(sessionId);
        log.info('powerOn:', powerOnRes);
        ToastAndroid.show(t('PoweredOnSentText'), ToastAndroid.SHORT);
      }

      let _consoles = await _xHomeApiRef.current.getConsoles();

      if (!_consoles.length) {
        _consoles = await webApi.getConsoles();
      }

      if (_consoles.length > 0) {
        setConsoles(_consoles);
      }

      setLoading(false);
    } catch (e) {
      setLoading(false);
    }
  };

  const handleRefreshConsoles = async () => {
    setLoading(true);
    setLoadingText(t('Loading...'));

    try {
      const webApi = new WebApi(webToken);
      let _consoles: any = [];

      if (_xHomeApiRef.current) {
        _consoles = await _xHomeApiRef.current.getConsoles();
      }

      if (!_consoles.length) {
        _consoles = await webApi.getConsoles();
      }

      if (_consoles.length > 0) {
        setConsoles(_consoles);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleStartStream = async sessionId => {
    await ensureAuthReadyForStream();
    const settings = getSettings();
    const hasValidUsbDevice = await UsbRumbleManager.getHasValidUsbDevice();
    const isUsbMode = settings.bind_usb_device && hasValidUsbDevice;
    const isNativeUi = NativeUiBridge?.isNativeUiEnabled?.() === true;

    setCurrentConsoleId(sessionId);
    if (isUsbMode && !Platform.isTV && !isNativeUi) {
      setShowUsbWarnShowModal(true);
    } else {
      handleNavigateStream(sessionId);
    }
  };

  const handleNavigateStream = async sessionId => {
    const settings = getSettings();
    const hasValidUsbDevice = await UsbRumbleManager.getHasValidUsbDevice();
    const usbController = await UsbRumbleManager.getUsbController();
    const isUsbMode = settings.bind_usb_device && hasValidUsbDevice;
    const isNativeUi = NativeUiBridge?.isNativeUiEnabled?.() === true;

    const webviewVersion = FullScreenManager.getWebViewVersion();
    const deviceInfos = FullScreenManager.getDeviceInfos();

    let isLagecy = false;
    if (webviewVersion) {
      const verArr = webviewVersion.split('.');
      const mainVer = verArr[0];

      // webview version is below 91
      if (deviceInfos.androidVer < 12 && mainVer < 91) {
        isLagecy = true;
      }
    }

    let routeName = 'Stream';
    if (isNativeUi || settings.render_engine === 'native') {
      routeName = settings.native_portrait_mode
        ? 'NativePortraitStream'
        : 'NativeStream';
    } else if (settings.render_engine === 'nano') {
      routeName = 'NanoStream';
    }

    // Lagecy user force to native stream
    if (isLagecy && routeName === 'Stream') {
      routeName = settings.native_portrait_mode
        ? 'NativePortraitStream'
        : 'NativeStream';
    }

    navigation.navigate({
      name: routeName,
      params: {
        sessionId,
        settings,
        isUsbMode,
        usbController,
      },
    });
  };

  // Focus navigation state for gamepad / TV remote
  const [isGamepadActive, setIsGamepadActive] = useGamepadActiveState();
  const [focusedSection, setFocusedSection] = React.useState<
    'consoles' | 'refresh' | 'more'
  >('more');
  const [focusedIndex, setFocusedIndex] = React.useState<number>(0);

  // Sync focusedSection when consoles change
  React.useEffect(() => {
    if (consoles.length > 0 && focusedSection === 'refresh') {
      setFocusedSection('consoles');
      setFocusedIndex(0);
    }
  }, [consoles.length, focusedSection]);

  useGamepadNavigation({
    enabled:
      isFocused &&
      !loading &&
      !showMsalLogin &&
      !showMsal &&
      !showUsbWarnModal &&
      !showHarmonyModal &&
      !sessionReport,
    onRight: () => {
      if (focusedSection === 'consoles') {
        if (focusedIndex < consoles.length - 1) {
          setFocusedIndex(prev => prev + 1);
        }
      } else if (focusedSection === 'more') {
        if (focusedIndex < 2) {
          setFocusedIndex(prev => prev + 1);
        }
      }
    },
    onLeft: () => {
      if (focusedIndex > 0) {
        setFocusedIndex(prev => prev - 1);
      }
    },
    onDown: () => {
      if (focusedSection === 'consoles') {
        if (focusedIndex + numColumns < consoles.length) {
          setFocusedIndex(prev => prev + numColumns);
        } else {
          setFocusedSection('more');
          setFocusedIndex(0);
        }
      } else if (focusedSection === 'refresh') {
        setFocusedSection('more');
        setFocusedIndex(0);
      }
    },
    onUp: () => {
      if (focusedSection === 'more') {
        if (consoles.length > 0) {
          setFocusedSection('consoles');
          setFocusedIndex(0);
        } else {
          setFocusedSection('refresh');
          setFocusedIndex(0);
        }
      } else if (focusedSection === 'consoles') {
        if (focusedIndex >= numColumns) {
          setFocusedIndex(prev => prev - numColumns);
        }
      }
    },
    onSelect: () => {
      if (focusedSection === 'consoles') {
        const item: any = consoles[focusedIndex];
        if (item) {
          handleStartStream(item.serverId);
        }
      } else if (focusedSection === 'refresh') {
        handleRefreshConsoles();
      } else if (focusedSection === 'more') {
        if (focusedIndex === 0) {
          navigation.navigate('Cloud');
        } else if (focusedIndex === 1) {
          navigation.navigate('Achivements');
        } else if (focusedIndex === 2) {
          navigation.navigate('Settings');
        }
      }
    },
    onActionX: () => {
      if (focusedSection === 'consoles') {
        const item: any = consoles[focusedIndex];
        if (item) {
          handlePoweronAndStream(item.serverId);
        }
      } else if (focusedSection === 'more' && focusedIndex === 0) {
        navigation.navigate('Cloud');
      }
    },
    onBack: () => {
      BackHandler.exitApp();
    },
  });

  // Focus navigation state for login buttons (when not logged in)
  const [focusedLoginBtn, setFocusedLoginBtn] = React.useState<
    'browser' | 'device_code' | 'settings'
  >(Platform.isTV ? 'device_code' : 'browser');
  const [focusedHarmonyBtn, setFocusedHarmonyBtn] = React.useState<
    'dismiss' | 'install'
  >('install');

  useGamepadNavigation({
    enabled:
      isFocused &&
      !loading &&
      showMsalLogin &&
      !showMsal &&
      !showUsbWarnModal &&
      !showHarmonyModal &&
      !sessionReport,
    priority: 5,
    onUp: () => {
      setFocusedLoginBtn(prev => {
        if (prev === 'settings') {
          return 'device_code';
        }
        return Platform.isTV ? 'device_code' : 'browser';
      });
    },
    onDown: () => {
      setFocusedLoginBtn(prev => {
        if (prev === 'browser') {
          return 'device_code';
        }
        return 'settings';
      });
    },
    onSelect: () => {
      if (focusedLoginBtn === 'browser') {
        handlePkceBrowserLogin();
      } else if (focusedLoginBtn === 'device_code') {
        handleMsalLogin();
      } else {
        navigation.navigate('Settings');
      }
    },
    onBack: () => {
      BackHandler.exitApp();
    },
  });

  useGamepadNavigation({
    enabled: isFocused && showUsbWarnModal,
    priority: 10,
    onSelect: () => {
      setShowUsbWarnShowModal(false);
      handleNavigateStream(currentConsoleId);
    },
    onBack: () => {
      setShowUsbWarnShowModal(false);
    },
  });

  useGamepadNavigation({
    enabled: isFocused && showHarmonyModal,
    priority: 10,
    onLeft: () => setFocusedHarmonyBtn('dismiss'),
    onRight: () => setFocusedHarmonyBtn('install'),
    onUp: () => setFocusedHarmonyBtn('dismiss'),
    onDown: () => setFocusedHarmonyBtn('install'),
    onSelect: () => {
      if (focusedHarmonyBtn === 'install') {
        Linking.openURL(HARMOBY_URL);
        setShowHarmonyModal(false);
      } else {
        let _settings = getSettings();
        _settings.show_harmony_modal = false;
        saveSettings(_settings);
        setShowHarmonyModal(false);
      }
    },
    onBack: () => {
      setShowHarmonyModal(false);
    },
  });

  const gamepadHints: GamepadHintItem[] = React.useMemo(() => {
    return [
      {button: 'A', label: t('Select')},
      {button: 'B', label: t('Exit')},
    ];
  }, [t]);

  // Warn: xboxOne controller must press Nexus button first to active button
  const renderUsbWarningModal = () => {
    if (!showUsbWarnModal) {
      return null;
    }
    return (
      <Portal>
        <Modal
          visible={showUsbWarnModal}
          onDismiss={() => {
            setShowUsbWarnShowModal(false);
          }}
          contentContainerStyle={{marginLeft: '4%', marginRight: '4%'}}>
          <Card>
            <Card.Content>
              <Text>
                TIPS1:{' '}
                {t(
                  'It has been detected that you are using the wired connection mode with the Overwrite Android driver. If the USB connection is disconnected during the game, please exit the game and reconnect the controller; otherwise, the controller buttons will become unresponsive',
                )}
              </Text>
              <Text>
                TIPS2:{' '}
                {t(
                  'If you are using an Xbox One/S/X controller and encounter unresponsive buttons when entering the game, please press the home button on the controller first',
                )}
              </Text>

              <Button
                mode="contained"
                style={styles.actionButtonFocused}
                onPress={() => {
                  setShowUsbWarnShowModal(false);
                  handleNavigateStream(currentConsoleId);
                }}>
                {t('Confirm')}
              </Button>
            </Card.Content>
          </Card>
        </Modal>
      </Portal>
    );
  };

  const renderHarmonyModal = () => {
    if (!showHarmonyModal) {
      return null;
    }
    return (
      <Portal>
        <Modal
          visible={true}
          onDismiss={() => {
            setShowHarmonyModal(false);
          }}
          contentContainerStyle={{marginLeft: '4%', marginRight: '4%'}}>
          <Card>
            <Card.Content>
              <Text>
                XStreaming鸿蒙版已正式发布App Gallery，如您的设备系统为HarmonyOS
                5以上，您可以安装原生版本以获得更好的串流体验(点击立即下载或应用商店搜索"XStreaming"进行安装)。
              </Text>

              <Button
                mode={focusedHarmonyBtn === 'dismiss' ? 'contained' : 'text'}
                style={
                  focusedHarmonyBtn === 'dismiss'
                    ? styles.actionButtonFocused
                    : undefined
                }
                onPress={() => {
                  let _settings = getSettings();
                  _settings.show_harmony_modal = false;
                  saveSettings(_settings);
                  setShowHarmonyModal(false);
                }}>
                不再提示
              </Button>
              <Button
                mode={
                  focusedHarmonyBtn === 'install' ? 'contained' : 'elevated'
                }
                style={
                  focusedHarmonyBtn === 'install'
                    ? styles.actionButtonFocused
                    : undefined
                }
                onPress={() => {
                  Linking.openURL(HARMOBY_URL);
                  setShowHarmonyModal(false);
                }}>
                去安装
              </Button>
            </Card.Content>
          </Card>
        </Modal>
      </Portal>
    );
  };

  const _lastHandledOAuthRedirectRef = React.useRef<string>('');

  const handleIncomingOAuthUrl = React.useCallback(
    (incomingUrl?: string | null) => {
      if (
        !incomingUrl ||
        !incomingUrl.startsWith('ms-xal-000000004c20a908://auth')
      ) {
        return;
      }
      if (_lastHandledOAuthRedirectRef.current === incomingUrl) {
        return;
      }
      _lastHandledOAuthRedirectRef.current = incomingUrl;
      log.info('Received PKCE OAuth redirect URL');
      setShowMsalLogin(false);
      setShowMsal(false);
      setLoading(true);
      setLoadingText(t('Completing login...'));
      _authentication.current?.handlePkceRedirectUrl?.(incomingUrl);
    },
    [t],
  );

  React.useEffect(() => {
    Linking.getInitialURL()
      .then(initialUrl => {
        if (initialUrl) {
          handleIncomingOAuthUrl(initialUrl);
        }
      })
      .catch(() => {});

    const linkingSub = Linking.addEventListener('url', event => {
      handleIncomingOAuthUrl(event?.url);
    });

    const nativeSub = DeviceEventEmitter.addListener(
      'onOAuthRedirectUrl',
      (event: {url?: string}) => {
        handleIncomingOAuthUrl(event?.url);
      },
    );

    return () => {
      linkingSub.remove();
      nativeSub.remove();
    };
  }, [handleIncomingOAuthUrl]);

  const handlePkceBrowserLogin = () => {
    try {
      const authUrl = _authentication.current?.getPkceAuthorizeUrl?.();
      if (authUrl) {
        Linking.openURL(authUrl);
      }
    } catch (e: any) {
      log.error('PKCE browser login error:', e);
      Alert.alert(
        t('Error'),
        'Browser login error: ' + (e?.message || String(e)),
        [{text: t('Confirm'), style: 'default'}],
      );
    }
  };

  const handleMsalLogin = () => {
    setMsalBtnLoading(true);
    _authentication.current
      .getMsalDeviceCode()
      .then(data => {
        log.info('MSAL device code response:', data);
        _authentication.current.doPollForDeviceCodeAuth(data.device_code);
        setMsalData(data);
        setShowMsalLogin(false);
        setShowMsal(true);
        setMsalBtnLoading(false);
      })
      .catch(e => {
        log.error('MSAL device code error:', e);
        setMsalBtnLoading(false);
        Alert.alert(
          t('Error'),
          'MSAL device code error: ' + (e?.message || String(e)),
          [
            {
              text: t('Confirm'),
              style: 'default',
            },
          ],
        );
      });
  };

  const handleSelectThemePreset = (presetId: string) => {
    const current = getSettings();
    const presetTokens = THEME_PRESETS[presetId];
    const nextSettings: Settings = {
      ...current,
      theme: presetId,
      theme_primary_color:
        presetTokens?.primaryColor || current.theme_primary_color,
    };
    saveSettings(nextSettings);
    setLiveSettings(nextSettings);
  };

  const handleSelectPrimaryColor = (colorHex: string) => {
    const current = getSettings();
    const nextSettings: Settings = {
      ...current,
      theme_primary_color: colorHex,
    };
    saveSettings(nextSettings);
    setLiveSettings(nextSettings);
  };

  // Synchronize auth, consoles, settings, cloud catalog, achievements, and session report state with Native Android UI (Xbox-14 & Xbox-17)
  React.useEffect(() => {
    if (!NativeUiBridge) {
      return;
    }
    try {
      NativeUiBridge.syncSettings?.(liveSettings as any);
    } catch (_e) {}
  }, [liveSettings]);

  const syncNativeCloudCatalog = React.useCallback(() => {
    if (!NativeUiBridge?.syncCloudTitles) {
      return;
    }
    try {
      const cached = getXcloudData();
      const rawTitles = Array.isArray(cached?.titles)
        ? cached.titles
        : Array.isArray(cached?.recentTitles)
          ? cached.recentTitles
          : [];
      const mapped = rawTitles.slice(0, 48).map((item: any) => ({
        titleId: String(item.titleId || item.id || ''),
        name: String(
          item.ProductTitle || item.title || item.name || item.titleId || '',
        ),
        category: item.isTouchEnabled
          ? 'Touch + Controller'
          : 'Xbox Cloud Gaming',
        subtitle: '60 FPS • AMD FSR 1.0 Ready',
      }));
      NativeUiBridge.syncCloudTitles(mapped, false);
    } catch (_e) {}
  }, []);

  const syncNativeAchievements = React.useCallback(() => {
    if (!NativeUiBridge?.syncAchievements) {
      return;
    }
    const token = webTokenRef.current || webToken;
    if (!token) {
      return;
    }
    try {
      NativeUiBridge.syncAchievements([], true);
      const webApi = new WebApi(token);
      webApi
        .getHistoryAchivements()
        .then((data: any) => {
          const list = Array.isArray(data) ? data : [];
          const mapped = list.slice(0, 40).map((item: any, idx: number) => ({
            titleId: String(item.titleId || idx),
            name: String(item.name || 'Xbox Title'),
            lastUnlock: String(item.lastUnlock || '').slice(0, 10),
            currentGamerscore: Number(item.currentGamerscore || 0),
            maxGamerscore: Number(item.maxGamerscore || 1000),
          }));
          NativeUiBridge.syncAchievements(mapped, false);
        })
        .catch(() => {
          NativeUiBridge.syncAchievements([], false);
        });
    } catch (_e) {}
  }, [webToken]);

  React.useEffect(() => {
    if (isLogined || consoles.length > 0) {
      syncNativeCloudCatalog();
      syncNativeAchievements();
    }
  }, [isLogined, consoles.length, syncNativeCloudCatalog, syncNativeAchievements]);

  React.useEffect(() => {
    if (!NativeUiBridge) {
      return;
    }
    try {
      NativeUiBridge.syncConsoles(Array.isArray(consoles) ? consoles : []);
    } catch (_e) {}
  }, [consoles]);

  const resolvedNativeGamertag = React.useMemo(() => {
    return resolveXboxGamertag(
      fetchedGamertag,
      profile,
      webTokenRef.current || webToken,
      streamingTokens,
      '',
    );
  }, [fetchedGamertag, profile, webToken, streamingTokens]);

  const resolvedNativeGamerpic = React.useMemo(() => {
    return resolveXboxGamerpic(fetchedGamerpic, profile);
  }, [fetchedGamerpic, profile]);

  React.useEffect(() => {
    const activeToken = webTokenRef.current || webToken;
    if (!activeToken || fetchedGamerpic) {
      return;
    }
    const webApi = new WebApi(activeToken);
    webApi
      .getUserProfileV2()
      .then((person: any) => {
        const uniqueTag = extractGamertagFromClaimsOrProfile(person);
        if (uniqueTag) {
          setFetchedGamertag(uniqueTag);
          storage.set('user.unique_gamertag', uniqueTag);
        }
        const picUrl = extractGamerpicFromProfile(person);
        if (picUrl) {
          setFetchedGamerpic(picUrl);
          storage.set('user.gamerpic', picUrl);
        }
      })
      .catch(() => {
        webApi
          .getUserProfile()
          .then((info: any) => {
            const fallbackPic = extractGamerpicFromProfile(info);
            if (fallbackPic) {
              setFetchedGamerpic(fallbackPic);
              storage.set('user.gamerpic', fallbackPic);
            }
          })
          .catch(() => {});
      });
  }, [webToken, fetchedGamerpic]);

  React.useEffect(() => {
    if (!NativeUiBridge) {
      return;
    }
    try {
      const msalObj: any = msalData;
      if (showMsal && msalObj?.user_code) {
        NativeUiBridge.syncAuthState(
          'device_code_pending',
          msalObj.message || '',
          msalObj.user_code || '',
          msalObj.verification_uri_complete ||
            msalObj.verification_uri ||
            'https://microsoft.com/link',
          resolvedNativeGamertag,
          resolvedNativeGamerpic,
        );
      } else if (isLogined || consoles.length > 0) {
        NativeUiBridge.syncAuthState(
          'authenticated',
          'Signed in to Xbox Network',
          '',
          '',
          resolvedNativeGamertag,
          resolvedNativeGamerpic,
        );
      } else if (showMsalLogin && !loading) {
        NativeUiBridge.syncAuthState(
          'unauthenticated',
          'Sign in to stream from your Xbox or Cloud',
          '',
          '',
          '',
          '',
        );
      } else {
        NativeUiBridge.syncAuthState(
          'loading',
          loadingText || 'Checking Xbox Live tokens...',
          '',
          '',
          resolvedNativeGamertag,
          resolvedNativeGamerpic,
        );
      }
    } catch (_e) {}
  }, [
    showMsal,
    msalData,
    isLogined,
    consoles.length,
    showMsalLogin,
    loading,
    loadingText,
    resolvedNativeGamertag,
    resolvedNativeGamerpic,
  ]);

  React.useEffect(() => {
    if (!NativeUiBridge || !sessionReport) {
      return;
    }
    try {
      NativeUiBridge.syncSessionReport({
        targetName: sessionReport.serverName || 'Xbox Console',
        streamType: sessionReport.streamType || 'home',
        durationText: sessionReport.durationText || '0m 00s',
        avgFps: Number(sessionReport.avgFps || 60),
        avgBitrateMbps: Number(sessionReport.avgBitrateMbps || 0),
        avgRttMs: Number(sessionReport.avgRttMs || 0),
        avgDecodeMs: Number(sessionReport.avgDecodeMs || 0),
        packetLossPct: Number(sessionReport.packetLossPct || 0),
        exitReason: sessionReport.exitReason || 'Clean Session Exit',
      });
    } catch (_e) {}
  }, [sessionReport]);

  React.useEffect(() => {
    if (!NativeUiBridge) {
      return;
    }
    const sub = DeviceEventEmitter.addListener(
      'onNativeUiCommand',
      (event: {command?: string; payload?: string}) => {
        if (!event?.command) {
          return;
        }
        let parsed: any = {};
        try {
          parsed = event.payload ? JSON.parse(event.payload) : {};
        } catch (_e) {}

        switch (event.command) {
          case 'startConsoleStream': {
            const serverId = parsed.serverId || consoles[0]?.serverId;
            if (serverId) {
              if (parsed.powerOnFirst) {
                handlePoweronAndStream(serverId);
              } else {
                handleStartStream(serverId);
              }
            }
            break;
          }
          case 'refreshConsoles':
            handleRefreshConsoles();
            break;
          case 'startMsalLogin':
            handleMsalLogin();
            break;
          case 'startWebLogin':
            handlePkceBrowserLogin();
            break;
          case 'cancelMsalLogin':
            _authentication.current?.cancelDeviceCodeAuth?.();
            setShowMsal(false);
            setShowMsalLogin(true);
            break;
          case 'openCloudCatalog':
            GamepadManager?.setCurrentScreen?.('cloud_catalog');
            navigation.navigate('Cloud');
            break;
          case 'refreshCloudTitles':
            syncNativeCloudCatalog();
            break;
          case 'startCloudStream': {
            if (parsed.titleId) {
              const settings = getSettings();
              const isNativeUi = NativeUiBridge?.isNativeUiEnabled?.() === true;
              let routeName = 'Stream';
              if (isNativeUi || settings.render_engine === 'native') {
                routeName = settings.native_portrait_mode
                  ? 'NativePortraitStream'
                  : 'NativeStream';
              } else if (settings.render_engine === 'nano') {
                routeName = 'NanoStream';
              }
              navigation.navigate({
                name: routeName,
                params: {
                  sessionId: parsed.titleId,
                  streamType: 'cloud',
                  settings,
                },
              });
            }
            break;
          }
          case 'openAchievements':
            GamepadManager?.setCurrentScreen?.('achievements_view');
            navigation.navigate('Achivements');
            break;
          case 'refreshAchievements':
            syncNativeAchievements();
            break;
          case 'signOut':
            _authentication.current?._tokenStore?.clear?.();
            clearStreamToken();
            clearWebToken();
            storage.delete('user.unique_gamertag');
            storage.delete('user.gamertag');
            storage.delete('user.gamerpic');
            setFetchedGamertag('');
            setFetchedGamerpic('');
            _isLogined.current = false;
            dispatch({type: 'SET_LOGIN', payload: false});
            setConsoles([]);
            setShowMsal(false);
            setShowMsalLogin(true);
            break;
          case 'navigateHome':
            GamepadManager?.setCurrentScreen?.('home');
            navigation.navigate('Home');
            break;
          case 'updateTheme': {
            const current = getSettings();
            const nextSettings: Settings = {
              ...current,
              theme: parsed.theme_preset || current.theme,
              theme_primary_color:
                parsed.primary_color || current.theme_primary_color,
            };
            saveSettings(nextSettings);
            setLiveSettings(nextSettings);
            break;
          }
          case 'updateSetting': {
            if (parsed.key) {
              const current = getSettings();
              const nextSettings: any = {
                ...current,
                [parsed.key]: parsed.value,
              };
              saveSettings(nextSettings);
              setLiveSettings(nextSettings);
            }
            break;
          }
        }
      },
    );
    return () => sub.remove();
  }, [consoles, navigation]);

  const renderThemeStudioModal = () => {
    if (!showThemeModal) {
      return null;
    }
    const tokens = resolveThemeTokens(
      liveSettings.theme === 'light' ? 'light' : liveSettings.theme,
      liveSettings.theme === 'light' ? 'light' : 'dark',
      liveSettings.theme_primary_color,
    );

    return (
      <Portal>
        <Modal
          visible={showThemeModal}
          onDismiss={() => setShowThemeModal(false)}
          contentContainerStyle={styles.themeModalContainer}>
          <View
            style={[
              styles.themeModalCard,
              {
                backgroundColor: tokens.isLight
                  ? 'rgba(245, 249, 253, 0.96)'
                  : 'rgba(14, 24, 38, 0.95)',
                borderColor: tokens.glassBorder,
              },
            ]}>
            <View
              pointerEvents="none"
              style={[
                styles.specularTopBar,
                {backgroundColor: tokens.glassBorderHighlight},
              ]}
            />
            <View style={styles.themeModalHeader}>
              <View>
                <Text
                  style={[styles.themeModalBadge, {color: tokens.accentLime}]}>
                  2008 NXE STUDIO • LIVE PREVIEW
                </Text>
                <Text
                  variant="titleLarge"
                  style={[styles.themeModalTitle, {color: tokens.textPrimary}]}>
                  Xbox Theme & Main Color
                </Text>
              </View>
              <IconButton
                icon="close"
                size={20}
                iconColor={tokens.textPrimary}
                onPress={() => setShowThemeModal(false)}
              />
            </View>

            <ScrollView
              style={styles.themeModalScroll}
              showsVerticalScrollIndicator={false}>
              <Text
                style={[styles.themeSectionLabel, {color: tokens.textSecondary}]}>
                1. SELECT MAIN COLOR (BLUES & XBOX ACCENTS)
              </Text>
              <View style={styles.swatchGrid}>
                {THEME_PRIMARY_COLOR_OPTIONS.map(opt => {
                  const isSelected =
                    (
                      liveSettings.theme_primary_color || tokens.primaryColor
                    ).toUpperCase() === opt.value.toUpperCase();
                  const shortName = opt.label.replace(/\s*\(#.*\)$/, '');
                  return (
                    <TouchableOpacity
                      key={opt.value}
                      activeOpacity={0.8}
                      onPress={() => handleSelectPrimaryColor(opt.value)}
                      style={[
                        styles.swatchPill,
                        {
                          borderColor: isSelected
                            ? '#FFFFFF'
                            : 'rgba(255,255,255,0.2)',
                          backgroundColor: isSelected
                            ? hexToRgba(opt.value, 0.35)
                            : 'rgba(0,0,0,0.22)',
                        },
                      ]}>
                      <View
                        style={[
                          styles.swatchDot,
                          {backgroundColor: opt.value},
                        ]}
                      />
                      <Text
                        numberOfLines={1}
                        style={[
                          styles.swatchText,
                          {
                            color: tokens.textPrimary,
                            fontWeight: isSelected ? '800' : '600',
                          },
                        ]}>
                        {shortName}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>

              <Text
                style={[
                  styles.themeSectionLabel,
                  {color: tokens.textSecondary, marginTop: 14},
                ]}>
                2. SELECT PRESET THEME STAGE
              </Text>
              <View style={styles.presetGrid}>
                {THEME_PRESET_LIST.map(preset => {
                  const isSelected = liveSettings.theme === preset.id;
                  return (
                    <TouchableOpacity
                      key={preset.id}
                      activeOpacity={0.8}
                      onPress={() => handleSelectThemePreset(preset.id)}
                      style={[
                        styles.presetCard,
                        {
                          borderColor: isSelected
                            ? tokens.accentLime
                            : 'rgba(255,255,255,0.18)',
                          backgroundColor: isSelected
                            ? hexToRgba(preset.defaultPrimaryColor, 0.32)
                            : 'rgba(0,0,0,0.22)',
                        },
                      ]}>
                      <View
                        style={[
                          styles.presetColorOrb,
                          {backgroundColor: preset.defaultPrimaryColor},
                        ]}
                      />
                      <View style={styles.presetTextCol}>
                        <Text
                          numberOfLines={1}
                          style={[
                            styles.presetTitle,
                            {color: tokens.textPrimary},
                          ]}>
                          {preset.name}
                        </Text>
                        <Text
                          numberOfLines={1}
                          style={[
                            styles.presetEra,
                            {color: tokens.textSecondary},
                          ]}>
                          {preset.eraLabel}
                        </Text>
                      </View>
                      {isSelected && (
                        <Icon
                          source="check-circle"
                          size={18}
                          color={tokens.accentLime}
                        />
                      )}
                    </TouchableOpacity>
                  );
                })}
              </View>
            </ScrollView>
          </View>
        </Modal>
      </Portal>
    );
  };

  const renderMsalLogin = () => {
    const tokens = resolveThemeTokens(
      liveSettings.theme === 'light' ? 'light' : liveSettings.theme,
      liveSettings.theme === 'light' ? 'light' : 'dark',
      liveSettings.theme_primary_color,
    );
    const isBrowserFocused =
      (isGamepadActive || Platform.isTV) && focusedLoginBtn === 'browser';
    const isDeviceCodeFocused =
      (isGamepadActive || Platform.isTV) && focusedLoginBtn === 'device_code';
    const isSettingsFocused =
      (isGamepadActive || Platform.isTV) && focusedLoginBtn === 'settings';

    return (
      <View
        style={[
          styles.loginCard,
          {
            backgroundColor: tokens.glassBgElevated,
            borderColor: tokens.glassBorder,
          },
        ]}>
        <View
          pointerEvents="none"
          style={[
            styles.specularTopBar,
            {backgroundColor: tokens.glassBorderHighlight},
          ]}
        />
        <View
          style={[
            styles.heroLogoOrb,
            {
              borderColor: tokens.accentLime,
              backgroundColor: hexToRgba(tokens.primaryColor, 0.28),
            },
          ]}>
          <Icon
            source="microsoft-xbox"
            color={tokens.accentLime}
            size={36}
          />
        </View>
        <Text
          variant="headlineSmall"
          style={[styles.loginTitle, {color: tokens.textPrimary}]}>
          XStreaming
        </Text>
        <Text style={[styles.title, {color: tokens.textSecondary}]}>
          {t('NoLogin')}
        </Text>
        {!Platform.isTV && (
          <Button
            style={[
              styles.loginButton,
              isBrowserFocused && styles.actionButtonFocused,
            ]}
            mode="contained"
            buttonColor={
              isBrowserFocused ? tokens.accentLime : tokens.primaryColor
            }
            textColor={isBrowserFocused ? '#071204' : '#FFFFFF'}
            onPress={handlePkceBrowserLogin}>
            &nbsp;1-Tap Browser Sign-In&nbsp;
          </Button>
        )}

        <Button
          style={[
            styles.loginButton,
            !Platform.isTV && styles.mt10,
            isDeviceCodeFocused && styles.actionButtonFocused,
          ]}
          mode={
            Platform.isTV || isDeviceCodeFocused ? 'contained' : 'outlined'
          }
          buttonColor={
            isDeviceCodeFocused
              ? tokens.accentLime
              : Platform.isTV
                ? tokens.primaryColor
                : undefined
          }
          textColor={
            isDeviceCodeFocused
              ? '#071204'
              : Platform.isTV
                ? '#FFFFFF'
                : tokens.textPrimary
          }
          loading={msalBtnLoading}
          onPress={handleMsalLogin}>
          &nbsp;{Platform.isTV ? t('Login') : 'Sign In with Device Code'}&nbsp;
        </Button>

        <Button
          style={[
            styles.loginButton,
            styles.mt10,
            isSettingsFocused && styles.actionButtonFocused,
          ]}
          mode={isSettingsFocused ? 'contained' : 'text'}
          buttonColor={isSettingsFocused ? tokens.primaryColor : undefined}
          textColor={isSettingsFocused ? '#FFFFFF' : tokens.textSecondary}
          onPress={() => navigation.navigate('Settings')}>
          &nbsp;{t('Settings')}&nbsp;
        </Button>
      </View>
    );
  };

  const renderContent = () => {
    if (loading && consoles.length === 0 && !showMsalLogin && !showMsal) {
      return null;
    }
    if (showMsalLogin) {
      return <View style={styles.centerContainer}>{renderMsalLogin()}</View>;
    } else if (showMsal) {
      return (
        <View style={styles.centerContainer}>
          <MsalAuth
            data={msalData}
            onCancel={() => {
              _authentication.current?.cancelDeviceCodeAuth?.();
              setShowMsal(false);
              setShowMsalLogin(true);
            }}
          />
        </View>
      );
    } else {
      const tokens = resolveThemeTokens(
        liveSettings.theme === 'light' ? 'light' : liveSettings.theme,
        liveSettings.theme === 'light' ? 'light' : 'dark',
        liveSettings.theme_primary_color,
      );
      const isWide = width > 640;
      const themeKey = `${liveSettings.theme}:${liveSettings.theme_primary_color}`;
      const topSafeInset = Math.max(insets.top, StatusBar.currentHeight || 28);
      const bottomSafeInset = Math.max(insets.bottom, 20);

      return (
        <View
          style={[styles.container, {paddingTop: topSafeInset + 8}]}
          onTouchStart={() => {
            if (!Platform.isTV) setIsGamepadActive(false);
          }}>
          <ScrollView
            style={styles.scrollView}
            showsVerticalScrollIndicator={false}
            contentContainerStyle={[
              styles.scrollContent,
              {
                paddingBottom:
                  bottomSafeInset +
                  (isGamepadActive || Platform.isTV ? 64 : 28),
              },
            ]}>
            {/* Sleek Top Console Dashboard Header Bar (Zero overlap or clipping) */}
            <View style={styles.dashboardTopRow}>
              <View style={styles.brandTitleColumn}>
                <View style={styles.brandEyebrowRow}>
                  <View
                    style={[
                      styles.brandLivePip,
                      {
                        backgroundColor: tokens.accentLime,
                        shadowColor: tokens.accentLime,
                      },
                    ]}
                  />
                  <Text
                    numberOfLines={1}
                    style={[
                      styles.brandEyebrowText,
                      {color: tokens.accentLime},
                    ]}>
                    XSTREAMING
                  </Text>
                </View>
                <Text
                  numberOfLines={1}
                  style={[
                    styles.dashboardTitle,
                    {color: tokens.textPrimary},
                  ]}>
                  My Xbox
                </Text>
              </View>

              {/* Right Quick Actions: Theme Studio Pill + Refresh + Settings */}
              <View style={styles.headerActionsCluster}>
                <TouchableOpacity
                  activeOpacity={0.78}
                  onPress={() => setShowThemeModal(true)}
                  style={[
                    styles.themeQuickPill,
                    {
                      backgroundColor: tokens.isLight
                        ? 'rgba(255, 255, 255, 0.56)'
                        : 'rgba(6, 15, 40, 0.62)',
                      borderColor: tokens.glassBorder,
                    },
                  ]}>
                  <View
                    style={[
                      styles.themeColorDot,
                      {
                        backgroundColor: tokens.primaryColor,
                        borderColor: tokens.accentLime,
                      },
                    ]}
                  />
                  <Text
                    numberOfLines={1}
                    style={[
                      styles.themeQuickPillText,
                      {color: tokens.textPrimary},
                    ]}>
                    {isWide ? 'Theme Studio' : 'Theme'}
                  </Text>
                  <View
                    style={[
                      styles.themeIconBadge,
                      {backgroundColor: hexToRgba(tokens.accentLime, 0.2)},
                    ]}>
                    <Icon
                      source="palette-outline"
                      size={13}
                      color={tokens.accentLime}
                    />
                  </View>
                </TouchableOpacity>

                <TouchableOpacity
                  activeOpacity={0.8}
                  onPress={handleRefreshConsoles}
                  style={[
                    styles.headerIconTile,
                    {
                      backgroundColor: tokens.isLight
                        ? 'rgba(255, 255, 255, 0.56)'
                        : 'rgba(6, 15, 40, 0.62)',
                      borderColor: tokens.glassBorder,
                    },
                  ]}>
                  <Icon
                    source="refresh"
                    size={19}
                    color={tokens.textPrimary}
                  />
                </TouchableOpacity>

                <TouchableOpacity
                  activeOpacity={0.8}
                  onPress={() => navigation.navigate('Settings')}
                  style={[
                    styles.headerIconTile,
                    {
                      backgroundColor: tokens.isLight
                        ? 'rgba(255, 255, 255, 0.56)'
                        : 'rgba(6, 15, 40, 0.62)',
                      borderColor: tokens.glassBorder,
                    },
                  ]}>
                  <Icon
                    source="cog-outline"
                    size={19}
                    color={tokens.textPrimary}
                  />
                </TouchableOpacity>
              </View>
            </View>

            {/* Primary Console Carousel / Stage */}
            {consoles.length > 0 ? (
              <View style={styles.consoleList}>
                <View style={styles.consoleGrid}>
                  {consoles.map((item: any, index: number) => {
                    const isItemFocused =
                      (isGamepadActive || Platform.isTV) &&
                      focusedSection === 'consoles' &&
                      focusedIndex === index;
                    const isSingleConsole = consoles.length === 1;
                    const isTwoConsoles = consoles.length === 2;
                    return (
                      <View
                        key={item.serverId || index}
                        style={[
                          styles.consoleItem,
                          isSingleConsole
                            ? styles.listItemSingle
                            : isTwoConsoles && isWide
                            ? styles.listItemV
                            : numColumns === 4
                            ? styles.listItemH
                            : isWide
                            ? styles.listItemV
                            : styles.listItemSingle,
                          isItemFocused && {zIndex: 99, overflow: 'visible'},
                        ]}>
                        <ConsoleItem
                          themeKey={themeKey}
                          isFocused={isItemFocused}
                          isHero={isSingleConsole}
                          consoleItem={item}
                          onPress={() => handleStartStream(item.serverId)}
                          onPoweronStream={() =>
                            handlePoweronAndStream(item.serverId)
                          }
                          onPoweron={() => handlePower(item.serverId)}
                          onPoweroff={() => handlePower(item.serverId, true)}
                        />
                      </View>
                    );
                  })}
                </View>
              </View>
            ) : (
              <View style={styles.noConsoles}>
                <View
                  style={[
                    emptyConsoleCardStyle,
                    {
                      backgroundColor: tokens.glassBg,
                      borderColor: tokens.glassBorder,
                    },
                  ]}>
                  <Text
                    style={[
                      styles.emptyConsoleDesc,
                      {color: tokens.textSecondary},
                    ]}>
                    {t('NoConsoles')}
                  </Text>
                  <View style={styles.emptyConsoleActions}>
                    <Button
                      mode={
                        (isGamepadActive || Platform.isTV) &&
                        focusedSection === 'refresh'
                          ? 'elevated'
                          : 'contained-tonal'
                      }
                      style={[
                        styles.emptyActionBtn,
                        (isGamepadActive || Platform.isTV) &&
                          focusedSection === 'refresh' &&
                          styles.actionButtonFocused,
                      ]}
                      onPress={handleRefreshConsoles}>
                      {t('Refresh')}
                    </Button>
                  </View>
                </View>
              </View>
            )}

            {/* Section Header for Quick Access Channels */}
            <View style={styles.sectionDividerRow}>
              <Text
                style={[
                  styles.sectionDividerLabel,
                  {color: tokens.textSecondary},
                ]}>
                CHANNELS & QUICK ACCESS
              </Text>
              <View
                style={[
                  styles.sectionDividerLine,
                  {backgroundColor: hexToRgba(tokens.accentLime, 0.24)},
                ]}
              />
            </View>

            {/* Multi-Accent Channel Tiles (Cloud Gaming, Achievements, Settings) */}
            <View style={styles.moreItems}>
              <View
                style={[
                  styles.moreItem,
                  {width: isWide ? '31.5%' : '100%'},
                  (isGamepadActive || Platform.isTV) &&
                    focusedSection === 'more' &&
                    focusedIndex === 0 && {zIndex: 99, overflow: 'visible'},
                ]}>
                <HomeItem
                  isFocused={
                    (isGamepadActive || Platform.isTV) &&
                    focusedSection === 'more' &&
                    focusedIndex === 0
                  }
                  title={t('Xcloud')}
                  subtitle="Xbox Cloud Gaming"
                  icon={'google-controller'}
                  color={tokens.tileColors.cloud}
                  onPress={() => navigation.navigate('Cloud')}
                />
              </View>

              <View
                style={[
                  styles.moreItem,
                  {width: isWide ? '31.5%' : '100%'},
                  (isGamepadActive || Platform.isTV) &&
                    focusedSection === 'more' &&
                    focusedIndex === 1 && {zIndex: 99, overflow: 'visible'},
                ]}>
                <HomeItem
                  isFocused={
                    (isGamepadActive || Platform.isTV) &&
                    focusedSection === 'more' &&
                    focusedIndex === 1
                  }
                  title={t('Achivements')}
                  subtitle="Gamerscore & Trophies"
                  icon={'trophy-outline'}
                  color={tokens.tileColors.achievements}
                  onPress={() => navigation.navigate('Achivements')}
                />
              </View>

              <View
                style={[
                  styles.moreItem,
                  {width: isWide ? '31.5%' : '100%'},
                  (isGamepadActive || Platform.isTV) &&
                    focusedSection === 'more' &&
                    focusedIndex === 2 && {zIndex: 99, overflow: 'visible'},
                ]}>
                <HomeItem
                  isFocused={
                    (isGamepadActive || Platform.isTV) &&
                    focusedSection === 'more' &&
                    focusedIndex === 2
                  }
                  title={t('Settings')}
                  subtitle="Display, Audio & Themes"
                  icon={'cog-outline'}
                  color={tokens.tileColors.settings}
                  onPress={() => navigation.navigate('Settings')}
                />
              </View>
            </View>
          </ScrollView>
        </View>
      );
    }
  };

  if (NativeUiBridge?.isNativeUiEnabled?.() === true) {
    return <View style={styles.root} pointerEvents="none" />;
  }

  return (
    <View
      style={styles.root}
      onTouchStart={() => {
        if (!Platform.isTV) setIsGamepadActive(false);
      }}>
      <Spinner loading={loading} text={loadingText} />

      {showUsbWarnModal && renderUsbWarningModal()}

      {showHarmonyModal && renderHarmonyModal()}

      {showThemeModal && renderThemeStudioModal()}

      {renderContent()}

      {sessionReport && (
        <SessionReportModal
          visible={true}
          report={sessionReport}
          onDismiss={handleDismissReport}
          onDone={handleDoneReport}
        />
      )}

      <GamepadFooterHints
        visible={isGamepadActive || Platform.isTV}
        hints={gamepadHints}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  container: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  scrollView: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: 14,
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  specularTopBar: {
    position: 'absolute',
    top: 0,
    left: 16,
    right: 16,
    height: 1.5,
    borderRadius: 2,
  },
  heroLogoOrb: {
    width: 68,
    height: 68,
    borderRadius: 34,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 12,
  },
  loginTitle: {
    fontWeight: '800',
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  title: {
    fontSize: 15,
    marginBottom: 20,
    textAlign: 'center',
  },
  loginCard: {
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    maxWidth: 400,
    padding: 28,
    borderRadius: 16,
    borderWidth: 1.5,
    overflow: 'hidden',
    elevation: 10,
  },
  loginButton: {
    minWidth: 220,
    borderRadius: 10,
  },
  // Sleek Top Console Dashboard Header Bar
  dashboardTopRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 6,
    paddingTop: 2,
    paddingBottom: 12,
  },
  brandTitleColumn: {
    flex: 1,
    marginRight: 10,
    justifyContent: 'center',
  },
  brandEyebrowRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 2,
  },
  brandLivePip: {
    width: 7,
    height: 7,
    borderRadius: 3.5,
    marginRight: 6,
    elevation: 3,
    shadowColor: '#00E5FF',
    shadowOpacity: 0.85,
    shadowRadius: 4,
  },
  brandEyebrowText: {
    fontSize: 10.5,
    fontWeight: '800',
    letterSpacing: 1.4,
  },
  dashboardTitle: {
    fontSize: 25,
    fontWeight: '800',
    letterSpacing: 0.2,
    textShadowColor: 'rgba(0, 0, 0, 0.35)',
    textShadowOffset: {width: 0, height: 1},
    textShadowRadius: 4,
  },
  headerActionsCluster: {
    flexDirection: 'row',
    alignItems: 'center',
    flexShrink: 0,
    gap: 8,
  },
  themeQuickPill: {
    height: 38,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1.2,
    borderRadius: 12,
    paddingLeft: 10,
    paddingRight: 7,
    elevation: 4,
  },
  themeColorDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    borderWidth: 1.2,
    borderColor: '#FFFFFF',
    marginRight: 6,
  },
  themeQuickPillText: {
    fontSize: 12.5,
    fontWeight: '800',
    marginRight: 6,
  },
  themeIconBadge: {
    width: 22,
    height: 22,
    borderRadius: 7,
    alignItems: 'center',
    justifyContent: 'center',
  },
  headerIconTile: {
    width: 38,
    height: 38,
    borderRadius: 12,
    borderWidth: 1.2,
    alignItems: 'center',
    justifyContent: 'center',
    elevation: 4,
  },
  sectionDividerRow: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 8,
    marginTop: 10,
    marginBottom: 8,
  },
  sectionDividerLabel: {
    fontSize: 10.5,
    fontWeight: '800',
    letterSpacing: 1.2,
    marginRight: 10,
    opacity: 0.85,
  },
  sectionDividerLine: {
    flex: 1,
    height: 1,
    borderRadius: 1,
  },
  // Live Theme & Main Color Studio Modal
  themeModalContainer: {
    marginHorizontal: 18,
    alignItems: 'center',
    justifyContent: 'center',
  },
  themeModalCard: {
    width: '100%',
    maxWidth: 620,
    maxHeight: 520,
    borderRadius: 14,
    borderWidth: 1.5,
    paddingHorizontal: 20,
    paddingVertical: 16,
    overflow: 'hidden',
    elevation: 18,
  },
  themeModalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  themeModalBadge: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1.2,
  },
  themeModalTitle: {
    fontWeight: '800',
    letterSpacing: 0.3,
  },
  themeModalScroll: {
    maxHeight: 410,
  },
  themeSectionLabel: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1.0,
    marginBottom: 8,
  },
  swatchGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  swatchPill: {
    width: '48.5%',
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 8,
    borderWidth: 1.5,
    marginBottom: 8,
  },
  swatchDot: {
    width: 18,
    height: 18,
    borderRadius: 9,
    borderWidth: 1.5,
    borderColor: '#FFFFFF',
    marginRight: 8,
  },
  swatchText: {
    fontSize: 12,
    flex: 1,
  },
  presetGrid: {
    flexDirection: 'column',
    paddingBottom: 12,
  },
  presetCard: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 10,
    borderWidth: 1.5,
    marginBottom: 8,
  },
  presetColorOrb: {
    width: 24,
    height: 24,
    borderRadius: 6,
    borderWidth: 1.5,
    borderColor: '#FFFFFF',
    marginRight: 12,
  },
  presetTextCol: {
    flex: 1,
  },
  presetTitle: {
    fontSize: 14,
    fontWeight: '800',
  },
  presetEra: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginTop: 1,
  },
  spinnerTextStyle: {
    color: '#0072CE',
    textAlign: 'center',
  },
  menuWrap: {
    width: 250,
  },
  noConsoles: {
    paddingLeft: 8,
    paddingRight: 8,
    paddingBottom: 16,
  },
  emptyConsoleCard: {
    borderRadius: 12,
    padding: 20,
    backgroundColor: 'rgba(255, 255, 255, 0.68)',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.56)',
    overflow: 'hidden',
    elevation: 4,
  },
  emptyConsoleCardDark: {
    backgroundColor: 'rgba(18, 28, 44, 0.84)',
    borderColor: 'rgba(255, 255, 255, 0.28)',
  },
  emptyConsoleDesc: {
    opacity: 0.92,
    lineHeight: 20,
    marginBottom: 14,
  },
  emptyConsoleActions: {
    flexDirection: 'row',
    justifyContent: 'flex-start',
  },
  emptyActionBtn: {
    marginRight: 10,
  },
  actionButtonFocused: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
    elevation: 8,
    shadowColor: '#FFFFFF',
    shadowOpacity: 0.6,
  },
  consoleList: {
    paddingHorizontal: 2,
    paddingBottom: 6,
  },
  consoleGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  listContainer: {},
  consoleItem: {
    padding: 6,
  },
  listItemSingle: {
    width: '100%',
    maxWidth: 760,
    justifyContent: 'center',
  },
  listItemH: {
    width: '25%',
    justifyContent: 'center',
  },
  listItemV: {
    width: '50%',
    justifyContent: 'center',
  },
  moreItems: {
    paddingHorizontal: 6,
    paddingTop: 2,
    paddingBottom: 12,
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  moreItem: {
    width: '31.5%',
    marginBottom: 10,
  },
  mt10: {
    marginTop: 10,
  },
});

export default HomeScreen;
