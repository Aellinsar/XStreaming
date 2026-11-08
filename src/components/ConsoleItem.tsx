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
  const isHero = !!props.isHero && isWide;
  const isFocused = !!props.isFocused;

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
    ? tokens.accentLime
    : '#FCA5A5';
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
            isHero && styles.consoleImageHero,
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
            isHero && styles.consoleImageHero,
          ]}
          resizeMode="contain"
        />
      );
    } else {
      return <SvgXml xml={icons.ConsoleIcon} width={96} height={96} />;
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
              ? 'rgba(255, 255, 255, 0.34)'
              : 'rgba(8, 18, 48, 0.44)',
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
          viewBox="0 0 400 240"
          preserveAspectRatio="none"
          style={StyleSheet.absoluteFillObject}>
          <Defs>
            {/* Deep Royal Sapphire Translucent Glass Body */}
            <LinearGradient id="glassConsoleBody" x1="0" y1="0" x2="0.85" y2="1">
              <Stop
                offset="0"
                stopColor={tileTop}
                stopOpacity={tokens.isDarkCarbonStage ? '0.48' : '0.58'}
              />
              <Stop
                offset="0.52"
                stopColor={tileMid}
                stopOpacity={tokens.isDarkCarbonStage ? '0.38' : '0.48'}
              />
              <Stop
                offset="1"
                stopColor={tileBottom}
                stopOpacity={tokens.isDarkCarbonStage ? '0.54' : '0.64'}
              />
            </LinearGradient>

            {/* Top-Left Electric Cyan Frost Bloom */}
            <RadialGradient
              id="glassConsoleFrost"
              cx="24%"
              cy="14%"
              rx="58%"
              ry="52%">
              <Stop
                offset="0"
                stopColor={tokens.horizonRim}
                stopOpacity="0.24"
              />
              <Stop
                offset="0.55"
                stopColor={tokens.primaryColor}
                stopOpacity="0.10"
              />
              <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
            </RadialGradient>

            {/* Sleek Diagonal Glass Pane Reflection */}
            <LinearGradient id="glassConsoleSheen" x1="0" y1="0" x2="0.45" y2="1">
              <Stop
                offset="0"
                stopColor={tokens.horizonRim}
                stopOpacity="0.20"
              />
              <Stop offset="0.45" stopColor="#FFFFFF" stopOpacity="0.06" />
              <Stop offset="1" stopColor="#FFFFFF" stopOpacity="0" />
            </LinearGradient>
          </Defs>

          <Rect
            x="0"
            y="0"
            width="400"
            height="240"
            fill="url(#glassConsoleBody)"
          />
          <Rect
            x="0"
            y="0"
            width="400"
            height="240"
            fill="url(#glassConsoleFrost)"
          />
          {/* Diagonal Specular Glass Reflection Pane */}
          <Path
            d="M 0 0 L 400 0 L 400 92 Q 210 128 0 156 Z"
            fill="url(#glassConsoleSheen)"
          />
        </Svg>

        {/* Razor-Thin Electric Cyan Top Specular Glass Edge */}
        <View
          pointerEvents="none"
          style={[
            styles.topSpecularEdge,
            {backgroundColor: hexToRgba(tokens.accentLime, 0.85)},
          ]}
        />

        {/* Tile Content */}
        <View style={[styles.tileInner, isHero && styles.tileInnerHero]}>
          {/* Top Bar: Frosted Status Pill + Power Menu */}
          <View style={styles.topRow}>
            <View
              style={[
                styles.statusBadge,
                {borderColor: hexToRgba(tokens.accentLime, 0.52)},
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

            <Menu
              visible={menuVisible}
              onDismiss={closeMenu}
              anchor={
                <IconButton
                  icon="dots-horizontal"
                  size={20}
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

          {/* Center / Hero Visual + Details */}
          <View style={[styles.bodyRow, isHero && styles.bodyRowHero]}>
            {/* Sleek Frosted Glass Halo Pedestal + Console Render (No pie-wedge disc) */}
            <View
              style={[
                styles.medallionArea,
                isHero && styles.medallionAreaHero,
              ]}>
              <Svg
                pointerEvents="none"
                width={isHero ? 152 : 132}
                height={isHero ? 152 : 132}
                viewBox="0 0 140 140"
                style={styles.medallionSvg}>
                <Defs>
                  <RadialGradient
                    id="glassHaloGlow"
                    cx="40%"
                    cy="34%"
                    rx="62%"
                    ry="62%">
                    <Stop
                      offset="0"
                      stopColor={tokens.accentLime}
                      stopOpacity="0.26"
                    />
                    <Stop
                      offset="0.58"
                      stopColor={tokens.primaryColor}
                      stopOpacity="0.22"
                    />
                    <Stop
                      offset="1"
                      stopColor={tokens.tileBottomShade}
                      stopOpacity="0.10"
                    />
                  </RadialGradient>
                </Defs>
                <Circle
                  cx="70"
                  cy="70"
                  r="56"
                  fill="url(#glassHaloGlow)"
                  stroke={tokens.accentLime}
                  strokeWidth="1.3"
                  strokeOpacity="0.68"
                />
              </Svg>
              {renderConsoleArtwork()}
            </View>

            {/* Info & Frosted Glass Play Action */}
            <View style={[styles.infoArea, isHero && styles.infoAreaHero]}>
              <Text
                variant="labelMedium"
                style={[styles.consoleTypeTag, {color: tokens.accentLime}]}>
                {consoleLabel.toUpperCase()}
              </Text>
              <Text
                variant={isHero ? 'headlineSmall' : 'titleLarge'}
                numberOfLines={1}
                style={styles.consoleTitle}>
                {consoleItem.deviceName}
              </Text>

              <View style={styles.actionRow}>
                <View
                  style={[
                    styles.glassPlayPill,
                    {borderColor: hexToRgba(tokens.accentLime, 0.68)},
                    isFocused && [
                      styles.glassPlayPillFocused,
                      {borderColor: tokens.accentLime},
                    ],
                  ]}>
                  <View style={styles.xboxABadge}>
                    <Text style={styles.xboxABadgeText}>A</Text>
                  </View>
                  <Text style={styles.glassPlayPillLabel}>
                    {primaryButtonLabel}
                  </Text>
                  <Icon
                    source="chevron-right"
                    size={18}
                    color={tokens.accentLime}
                  />
                </View>
              </View>
            </View>
          </View>
        </View>
      </TouchableOpacity>

      {/* Subtle Frosted Glass Floor Reflection Under Tile */}
      <View pointerEvents="none" style={styles.floorReflectionWrap}>
        <Svg
          width="100%"
          height="16"
          viewBox="0 0 400 16"
          preserveAspectRatio="none">
          <Defs>
            <LinearGradient id="glassTileReflect" x1="0" y1="0" x2="0" y2="1">
              <Stop
                offset="0"
                stopColor={hexToRgba(tileTop, 0.34)}
                stopOpacity="0.28"
              />
              <Stop
                offset="1"
                stopColor={hexToRgba(tileTop, 0)}
                stopOpacity="0"
              />
            </LinearGradient>
          </Defs>
          <Rect
            x="6"
            y="0"
            width="388"
            height="16"
            rx="8"
            fill="url(#glassTileReflect)"
          />
        </Svg>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  tileWrapper: {
    width: '100%',
  },
  glassTile: {
    borderRadius: 14,
    borderWidth: 1,
    overflow: 'hidden',
    elevation: 6,
    shadowColor: '#020617',
    shadowOffset: {
      width: 0,
      height: 8,
    },
    shadowOpacity: 0.28,
    shadowRadius: 16,
  },
  glassTileFocused: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
    transform: [{scale: 1.02}],
    elevation: 12,
    shadowOpacity: 0.65,
    shadowRadius: 20,
    zIndex: 99,
  },
  topSpecularEdge: {
    position: 'absolute',
    top: 0,
    left: 6,
    right: 6,
    height: 1.2,
    backgroundColor: 'rgba(255, 255, 255, 0.82)',
    zIndex: 3,
  },
  tileInner: {
    paddingHorizontal: 18,
    paddingTop: 12,
    paddingBottom: 16,
  },
  tileInnerHero: {
    paddingHorizontal: 24,
    paddingVertical: 16,
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(6, 15, 40, 0.55)',
    borderColor: 'rgba(0, 229, 255, 0.52)',
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 999,
  },
  statusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginRight: 6,
  },
  statusText: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
  menuBtn: {
    margin: 0,
    backgroundColor: 'rgba(6, 15, 40, 0.52)',
    borderColor: 'rgba(0, 229, 255, 0.45)',
    borderWidth: 1,
  },
  bodyRow: {
    flexDirection: 'column',
    alignItems: 'center',
  },
  bodyRowHero: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  medallionArea: {
    width: '100%',
    height: 138,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  medallionAreaHero: {
    width: '40%',
    height: 156,
  },
  medallionSvg: {
    position: 'absolute',
  },
  consoleImage: {
    width: 118,
    height: 118,
  },
  consoleImageWide: {
    width: 128,
    height: 128,
  },
  consoleImageHero: {
    width: 142,
    height: 142,
  },
  infoArea: {
    width: '100%',
    marginTop: 6,
    alignItems: 'flex-start',
  },
  infoAreaHero: {
    width: '58%',
    marginTop: 0,
    paddingLeft: 14,
  },
  consoleTypeTag: {
    color: 'rgba(255, 255, 255, 0.88)',
    fontWeight: '800',
    letterSpacing: 1.1,
    fontSize: 11,
  },
  consoleTitle: {
    color: '#FFFFFF',
    fontWeight: '800',
    letterSpacing: 0.2,
    textShadowColor: 'rgba(0, 0, 0, 0.28)',
    textShadowOffset: {width: 0, height: 1},
    textShadowRadius: 3,
    marginTop: 1,
    marginBottom: 10,
  },
  actionRow: {
    width: '100%',
    flexDirection: 'row',
    alignItems: 'center',
  },
  glassPlayPill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(6, 15, 40, 0.58)',
    borderColor: 'rgba(0, 229, 255, 0.68)',
    borderWidth: 1,
    borderRadius: 10,
    paddingVertical: 8,
    paddingHorizontal: 13,
  },
  glassPlayPillFocused: {
    backgroundColor: 'rgba(255, 255, 255, 0.30)',
    borderColor: '#FFFFFF',
    borderWidth: 1.5,
  },
  xboxABadge: {
    width: 20,
    height: 20,
    borderRadius: 10,
    backgroundColor: '#22C55E',
    borderWidth: 1,
    borderColor: '#DCFCE7',
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
    fontSize: 13,
    letterSpacing: 0.3,
    marginRight: 4,
  },
  floorReflectionWrap: {
    width: '100%',
    height: 16,
    marginTop: 2,
    opacity: 0.85,
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
