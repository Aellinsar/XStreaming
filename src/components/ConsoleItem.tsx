import React from 'react';
import {
  StyleSheet,
  View,
  Image,
  TouchableOpacity,
  useWindowDimensions,
} from 'react-native';
import {Text, Menu, IconButton, Icon, useTheme} from 'react-native-paper';
import {useTranslation} from 'react-i18next';
import Svg, {
  Circle,
  Defs,
  LinearGradient,
  Path,
  RadialGradient,
  Rect,
  Stop,
  SvgXml,
} from 'react-native-svg';
import icons from '../common/svg';
import {getSettings} from '../store/settingStore';
import {resolveThemeTokens} from '../theme/presets';
import {hexToRgba} from '../utils/themeColor';

const ConsoleItem = (props: any) => {
  const {t} = useTranslation();
  const theme = useTheme();
  const settings = getSettings();
  const tokens = resolveThemeTokens(
    theme.dark ? settings.theme : 'light',
    theme.dark ? 'dark' : 'light',
    settings.theme_primary_color,
  );

  const [menuVisible, setMenuVisible] = React.useState(false);
  const openMenu = () => setMenuVisible(true);
  const closeMenu = () => setMenuVisible(false);

  const consoleItem = props.consoleItem;
  const {width} = useWindowDimensions();
  const isWide = width > 640;
  const isSingleHero = !!props.isHero;
  const isFocused = !!props.isFocused;
  const uid = String(consoleItem?.serverId || 'default').replace(
    /[^a-zA-Z0-9_-]/g,
    '',
  );

  const consoleLabel =
    consoleItem.consoleType === 'XboxSeriesX'
      ? 'Xbox Series X'
      : consoleItem.consoleType === 'XboxSeriesS'
      ? 'Xbox Series S'
      : consoleItem.consoleType || 'Xbox Console';

  const isOnline = consoleItem.powerState === 'On';
  const isStandby = consoleItem.powerState === 'ConnectedStandby';
  const statusColor = isOnline
    ? '#4ADE80'
    : isStandby
    ? '#FBBF24'
    : '#FB7185';
  const statusBgColor = isOnline
    ? 'rgba(16, 185, 129, 0.22)'
    : isStandby
    ? 'rgba(245, 158, 11, 0.22)'
    : 'rgba(244, 63, 94, 0.20)';
  const statusBorderColor = isOnline
    ? 'rgba(74, 222, 128, 0.68)'
    : isStandby
    ? 'rgba(251, 191, 36, 0.68)'
    : 'rgba(251, 113, 133, 0.58)';
  const statusText = isOnline
    ? t('Powered on').toUpperCase()
    : isStandby
    ? t('Standby').toUpperCase()
    : String(consoleItem.powerState || '').toUpperCase();

  const isPowerOnStream = settings.power_on && isStandby;
  const handlePrimaryAction = isPowerOnStream
    ? props.onPoweronStream
    : props.onPress;
  const primaryButtonLabel = isPowerOnStream
    ? t('Power on and start stream')
    : t('Start stream');

  const tileTop = tokens.tileTopHighlight;
  const tileMid = tokens.tileMidColor;
  const tileBottom = tokens.tileBottomShade;

  const renderConsoleArtwork = () => {
    const type = consoleItem.consoleType;
    if (type === 'XboxSeriesX') {
      return (
        <Image
          source={require('../assets/console/series-x.png')}
          style={[
            styles.consoleImage,
            isWide && styles.consoleImageWide,
            isSingleHero && isWide && styles.consoleImageHero,
          ]}
          resizeMode="contain"
        />
      );
    } else if (type === 'XboxSeriesS') {
      return (
        <Image
          source={require('../assets/console/series-s.png')}
          style={[
            styles.consoleImage,
            isWide && styles.consoleImageWide,
            isSingleHero && isWide && styles.consoleImageHero,
          ]}
          resizeMode="contain"
        />
      );
    } else {
      return <SvgXml xml={icons.ConsoleIcon} width={88} height={88} />;
    }
  };

  return (
    <View style={styles.tileWrapper}>
      <TouchableOpacity
        activeOpacity={0.86}
        onPress={handlePrimaryAction}
        style={[
          styles.glassTile,
          {
            backgroundColor: tokens.isLight
              ? 'rgba(255, 255, 255, 0.42)'
              : 'rgba(8, 18, 48, 0.56)',
            borderColor: isFocused
              ? tokens.accentLime
              : tokens.glassBorder,
          },
          isFocused && [
            styles.glassTileFocused,
            {
              borderColor: tokens.accentLime,
              shadowColor: tokens.accentLime,
            },
          ],
        ]}>
        {/* Sleek Translucent Royal Glass Layers & Cyan Specular Sheen (Zero concentric rings) */}
        <Svg
          pointerEvents="none"
          width="100%"
          height="100%"
          viewBox="0 0 400 220"
          preserveAspectRatio="none"
          style={StyleSheet.absoluteFillObject}>
          <Defs>
            {/* Deep Royal Sapphire Translucent Glass Body */}
            <LinearGradient
              id={`glassConsoleBody_${uid}`}
              x1="0"
              y1="0"
              x2="0.85"
              y2="1">
              <Stop
                offset="0"
                stopColor={tileTop}
                stopOpacity={tokens.isDarkCarbonStage ? '0.52' : '0.62'}
              />
              <Stop
                offset="0.52"
                stopColor={tileMid}
                stopOpacity={tokens.isDarkCarbonStage ? '0.42' : '0.52'}
              />
              <Stop
                offset="1"
                stopColor={tileBottom}
                stopOpacity={tokens.isDarkCarbonStage ? '0.58' : '0.68'}
              />
            </LinearGradient>

            {/* Top-Left Electric Cyan Frost Bloom */}
            <RadialGradient
              id={`glassConsoleFrost_${uid}`}
              cx="22%"
              cy="28%"
              rx="58%"
              ry="60%">
              <Stop
                offset="0"
                stopColor={tokens.horizonRim}
                stopOpacity="0.28"
              />
              <Stop
                offset="0.55"
                stopColor={tokens.primaryColor}
                stopOpacity="0.12"
              />
              <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
            </RadialGradient>

            {/* Bottom-Right Subtle Emerald Action Glow */}
            <RadialGradient
              id={`glassConsoleEmerald_${uid}`}
              cx="82%"
              cy="84%"
              rx="48%"
              ry="48%">
              <Stop offset="0" stopColor="#22C55E" stopOpacity="0.18" />
              <Stop offset="0.6" stopColor="#10B981" stopOpacity="0.06" />
              <Stop offset="1" stopColor="#10B981" stopOpacity="0" />
            </RadialGradient>

            {/* Sleek Diagonal Glass Pane Reflection */}
            <LinearGradient
              id={`glassConsoleSheen_${uid}`}
              x1="0"
              y1="0"
              x2="0.45"
              y2="1">
              <Stop
                offset="0"
                stopColor={tokens.horizonRim}
                stopOpacity="0.22"
              />
              <Stop offset="0.45" stopColor="#FFFFFF" stopOpacity="0.07" />
              <Stop offset="1" stopColor="#FFFFFF" stopOpacity="0" />
            </LinearGradient>
          </Defs>

          <Rect
            x="0"
            y="0"
            width="400"
            height="220"
            fill={`url(#glassConsoleBody_${uid})`}
          />
          <Rect
            x="0"
            y="0"
            width="400"
            height="220"
            fill={`url(#glassConsoleFrost_${uid})`}
          />
          <Rect
            x="0"
            y="0"
            width="400"
            height="220"
            fill={`url(#glassConsoleEmerald_${uid})`}
          />
          {/* Diagonal Specular Glass Reflection Pane */}
          <Path
            d="M 0 0 L 400 0 L 400 84 Q 210 120 0 148 Z"
            fill={`url(#glassConsoleSheen_${uid})`}
          />
        </Svg>

        {/* Razor-Thin Electric Cyan Top Specular Glass Edge */}
        <View
          pointerEvents="none"
          style={[
            styles.topSpecularEdge,
            {backgroundColor: hexToRgba(tokens.accentLime, 0.9)},
          ]}
        />

        {/* Tile Content */}
        <View
          style={[
            styles.tileInner,
            isSingleHero && isWide && styles.tileInnerHero,
          ]}>
          {/* Top Bar: Semantic Status Pill + Hardware Tag + Power Menu */}
          <View style={styles.topRow}>
            <View style={styles.statusCluster}>
              <View
                style={[
                  styles.statusBadge,
                  {
                    backgroundColor: statusBgColor,
                    borderColor: statusBorderColor,
                  },
                ]}>
                <View
                  style={[
                    styles.statusDot,
                    {
                      backgroundColor: statusColor,
                      shadowColor: statusColor,
                    },
                  ]}
                />
                <Text style={[styles.statusText, {color: '#FFFFFF'}]}>
                  {statusText}
                </Text>
              </View>

              <View
                style={[
                  styles.consoleGenerationPill,
                  {
                    borderColor: hexToRgba(tokens.accentLime, 0.38),
                    backgroundColor: 'rgba(6, 15, 40, 0.48)',
                  },
                ]}>
                <Icon
                  source="microsoft-xbox"
                  size={12}
                  color={tokens.accentLime}
                />
                <Text
                  style={[
                    styles.consoleGenerationText,
                    {color: tokens.accentLime},
                  ]}>
                  {consoleLabel.toUpperCase()}
                </Text>
              </View>
            </View>

            <Menu
              visible={menuVisible}
              onDismiss={closeMenu}
              anchor={
                <IconButton
                  icon="dots-horizontal"
                  size={18}
                  iconColor={tokens.accentLime}
                  style={[
                    styles.menuBtn,
                    {borderColor: hexToRgba(tokens.accentLime, 0.45)},
                  ]}
                  onPress={openMenu}
                />
              }>
              <Menu.Item
                leadingIcon="power"
                onPress={() => {
                  closeMenu();
                  props.onPoweron && props.onPoweron();
                }}
                title={t('Powered on')}
              />
              <Menu.Item
                leadingIcon="power-sleep"
                onPress={() => {
                  closeMenu();
                  props.onPoweroff && props.onPoweroff();
                }}
                title={t('Powered off')}
              />
            </Menu>
          </View>

          {/* Main Showcase Row: Illuminated Console Pedestal + Console Details & Vibrant CTA */}
          <View
            style={[
              styles.bodyRow,
              isSingleHero && styles.bodyRowHorizontal,
            ]}>
            {/* Sleek Frosted Glass Halo Pedestal + Console Render */}
            <View
              style={[
                styles.medallionArea,
                isSingleHero && styles.medallionAreaHorizontal,
                isSingleHero && isWide && styles.medallionAreaHeroWide,
              ]}>
              <Svg
                pointerEvents="none"
                width={isSingleHero && isWide ? 142 : 114}
                height={isSingleHero && isWide ? 142 : 114}
                viewBox="0 0 140 140"
                style={styles.medallionSvg}>
                <Defs>
                  <RadialGradient
                    id={`glassHaloGlow_${uid}`}
                    cx="45%"
                    cy="38%"
                    rx="58%"
                    ry="58%">
                    <Stop
                      offset="0"
                      stopColor="#38BDF8"
                      stopOpacity="0.42"
                    />
                    <Stop
                      offset="0.52"
                      stopColor={tokens.primaryColor}
                      stopOpacity="0.28"
                    />
                    <Stop
                      offset="1"
                      stopColor={tokens.tileBottomShade}
                      stopOpacity="0.16"
                    />
                  </RadialGradient>
                </Defs>
                <Circle
                  cx="70"
                  cy="70"
                  r="58"
                  fill={`url(#glassHaloGlow_${uid})`}
                  stroke={tokens.accentLime}
                  strokeWidth="1.5"
                  strokeOpacity="0.78"
                />
              </Svg>
              {renderConsoleArtwork()}
            </View>

            {/* Info & High-Contrast Emerald/Cyan Play Action */}
            <View
              style={[
                styles.infoArea,
                isSingleHero && styles.infoAreaHorizontal,
              ]}>
              <Text
                variant="labelSmall"
                style={[styles.remotePlayEyebrow, {color: tokens.textSecondary}]}>
                REMOTE PLAY CONSOLE
              </Text>
              <Text
                variant={isSingleHero && isWide ? 'headlineSmall' : 'titleLarge'}
                numberOfLines={1}
                style={styles.consoleTitle}>
                {consoleItem.deviceName}
              </Text>

              <View style={styles.actionRow}>
                <View
                  style={[
                    styles.glassPlayPill,
                    isFocused && [
                      styles.glassPlayPillFocused,
                      {borderColor: '#FFFFFF'},
                    ],
                  ]}>
                  <View style={styles.xboxABadge}>
                    <Text style={styles.xboxABadgeText}>A</Text>
                  </View>
                  <Text
                    numberOfLines={1}
                    style={styles.glassPlayPillLabel}>
                    {primaryButtonLabel}
                  </Text>
                  <Icon
                    source="chevron-right"
                    size={18}
                    color="#FFFFFF"
                  />
                </View>
              </View>
            </View>
          </View>
        </View>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  tileWrapper: {
    width: '100%',
  },
  glassTile: {
    borderRadius: 16,
    borderWidth: 1.2,
    overflow: 'hidden',
    elevation: 8,
    shadowColor: '#020617',
    shadowOffset: {
      width: 0,
      height: 8,
    },
    shadowOpacity: 0.36,
    shadowRadius: 16,
  },
  glassTileFocused: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
    transform: [{scale: 1.015}],
    elevation: 14,
    shadowOpacity: 0.65,
    shadowRadius: 20,
    zIndex: 99,
  },
  topSpecularEdge: {
    position: 'absolute',
    top: 0,
    left: 10,
    right: 10,
    height: 1.5,
    backgroundColor: 'rgba(255, 255, 255, 0.85)',
    zIndex: 3,
  },
  tileInner: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 14,
  },
  tileInnerHero: {
    paddingHorizontal: 22,
    paddingVertical: 16,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  statusCluster: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
    gap: 8,
    flex: 1,
    marginRight: 8,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
    elevation: 3,
  },
  statusText: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  consoleGenerationPill: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    paddingHorizontal: 9,
    paddingVertical: 3.5,
    borderRadius: 999,
    gap: 5,
  },
  consoleGenerationText: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.7,
  },
  menuBtn: {
    margin: 0,
    width: 34,
    height: 34,
    borderRadius: 10,
    backgroundColor: 'rgba(6, 15, 40, 0.56)',
    borderColor: 'rgba(0, 229, 255, 0.45)',
    borderWidth: 1,
  },
  bodyRow: {
    flexDirection: 'column',
    alignItems: 'center',
  },
  bodyRowHorizontal: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  medallionArea: {
    width: '100%',
    height: 118,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  medallionAreaHorizontal: {
    width: 116,
    height: 116,
  },
  medallionAreaHeroWide: {
    width: 146,
    height: 146,
  },
  medallionSvg: {
    position: 'absolute',
  },
  consoleImage: {
    width: 96,
    height: 96,
  },
  consoleImageWide: {
    width: 112,
    height: 112,
  },
  consoleImageHero: {
    width: 126,
    height: 126,
  },
  infoArea: {
    width: '100%',
    marginTop: 8,
    alignItems: 'flex-start',
  },
  infoAreaHorizontal: {
    flex: 1,
    marginTop: 0,
    paddingLeft: 14,
    justifyContent: 'center',
  },
  remotePlayEyebrow: {
    fontWeight: '700',
    letterSpacing: 1.0,
    fontSize: 10,
    opacity: 0.82,
  },
  consoleTitle: {
    color: '#FFFFFF',
    fontWeight: '800',
    letterSpacing: 0.2,
    fontSize: 22,
    textShadowColor: 'rgba(0, 0, 0, 0.35)',
    textShadowOffset: {width: 0, height: 1},
    textShadowRadius: 3,
    marginTop: 2,
    marginBottom: 12,
  },
  actionRow: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
  },
  glassPlayPill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#16A34A',
    borderColor: '#4ADE80',
    borderWidth: 1.2,
    borderRadius: 12,
    paddingVertical: 9,
    paddingHorizontal: 14,
    shadowColor: '#22C55E',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 5,
  },
  glassPlayPillFocused: {
    backgroundColor: '#22C55E',
    borderColor: '#FFFFFF',
    borderWidth: 2,
  },
  xboxABadge: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: 'rgba(5, 46, 22, 0.68)',
    borderWidth: 1,
    borderColor: '#BBF7D0',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
  },
  xboxABadgeText: {
    color: '#FFFFFF',
    fontWeight: '900',
    fontSize: 11,
  },
  glassPlayPillLabel: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 13.5,
    letterSpacing: 0.3,
    marginRight: 4,
    flexShrink: 1,
  },
});

export default React.memo(ConsoleItem, (prev, next) => {
  if (prev.isFocused !== next.isFocused) return false;
  if (prev.isHero !== next.isHero) return false;
  if (prev.themeKey !== next.themeKey) return false;
  if (prev.consoleItem?.serverId !== next.consoleItem?.serverId) return false;
  if (prev.consoleItem?.powerState !== next.consoleItem?.powerState) return false;
  if (prev.consoleItem?.deviceName !== next.consoleItem?.deviceName) return false;
  if (prev.consoleItem?.consoleType !== next.consoleItem?.consoleType) return false;
  return true;
});
