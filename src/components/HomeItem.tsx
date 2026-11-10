import React from 'react';
import {
  StyleSheet,
  View,
  Platform,
  TouchableOpacity,
  useWindowDimensions,
} from 'react-native';
import {Text, Icon, useTheme} from 'react-native-paper';
import Svg, {
  Defs,
  LinearGradient,
  Path,
  RadialGradient,
  Rect,
  Stop,
} from 'react-native-svg';
import {getSettings} from '../store/settingStore';
import {resolveThemeTokens} from '../theme/presets';
import {hexToRgba, normalizeHexColor} from '../utils/themeColor';

type Props = {
  title: string;
  subtitle?: string;
  icon: string;
  color?: string;
  onPress: () => void;
  isFocused?: boolean;
};

const HomeItem: React.FC<Props> = ({
  title,
  subtitle,
  icon,
  color,
  onPress,
  isFocused = false,
}) => {
  const theme = useTheme();
  const {width} = useWindowDimensions();
  const isWide = width > 640;
  const settings = getSettings();
  const tokens = resolveThemeTokens(
    theme.dark ? settings.theme : 'light',
    theme.dark ? 'dark' : 'light',
    settings.theme_primary_color,
  );

  const accentColor = normalizeHexColor(
    color || tokens.accentLime,
    tokens.accentLime,
  );
  const uid = String(icon || title || 'tile').replace(/[^a-zA-Z0-9]/g, '');

  const tileTop = tokens.tileTopHighlight;
  const tileMid = tokens.tileMidColor;
  const tileBottom = tokens.tileBottomShade;

  const handlePress = () => {
    onPress && onPress();
  };

  return (
    <View style={styles.wrapper}>
      <TouchableOpacity
        onPress={handlePress}
        activeOpacity={0.82}
        accessible={true}
        accessibilityRole="button"
        accessibilityLabel={title}
        style={[
          styles.glassCard,
          !isWide && styles.glassCardCompactRow,
          {
            backgroundColor: tokens.isLight
              ? 'rgba(255, 255, 255, 0.42)'
              : 'rgba(8, 18, 48, 0.56)',
            borderColor: isFocused
              ? accentColor
              : hexToRgba(accentColor, 0.48),
          },
          isFocused && [
            styles.glassCardFocused,
            {
              borderColor: accentColor,
              shadowColor: accentColor,
            },
          ],
        ]}>
        {/* Sleek Translucent Royal Glass Layers & Distinct Multi-Accent Bloom */}
        <Svg
          pointerEvents="none"
          width="100%"
          height="100%"
          viewBox="0 0 260 110"
          preserveAspectRatio="none"
          style={StyleSheet.absoluteFillObject}>
          <Defs>
            <LinearGradient
              id={`glassSubBody_${uid}`}
              x1="0"
              y1="0"
              x2="0.85"
              y2="1">
              <Stop
                offset="0"
                stopColor={tileTop}
                stopOpacity={tokens.isDarkCarbonStage ? '0.46' : '0.56'}
              />
              <Stop
                offset="0.52"
                stopColor={tileMid}
                stopOpacity={tokens.isDarkCarbonStage ? '0.38' : '0.48'}
              />
              <Stop
                offset="1"
                stopColor={tileBottom}
                stopOpacity={tokens.isDarkCarbonStage ? '0.56' : '0.66'}
              />
            </LinearGradient>
            <RadialGradient
              id={`glassSubFrost_${uid}`}
              cx="18%"
              cy="35%"
              rx="55%"
              ry="68%">
              <Stop offset="0" stopColor={accentColor} stopOpacity="0.26" />
              <Stop
                offset="0.58"
                stopColor={tokens.primaryColor}
                stopOpacity="0.10"
              />
              <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
            </RadialGradient>
            <LinearGradient
              id={`glassSubSheen_${uid}`}
              x1="0"
              y1="0"
              x2="0.45"
              y2="1">
              <Stop offset="0" stopColor={accentColor} stopOpacity="0.18" />
              <Stop offset="0.5" stopColor="#FFFFFF" stopOpacity="0.05" />
              <Stop offset="1" stopColor="#FFFFFF" stopOpacity="0" />
            </LinearGradient>
          </Defs>
          <Rect
            x="0"
            y="0"
            width="260"
            height="110"
            fill={`url(#glassSubBody_${uid})`}
          />
          <Rect
            x="0"
            y="0"
            width="260"
            height="110"
            fill={`url(#glassSubFrost_${uid})`}
          />
          {/* Diagonal Specular Glass Reflection Pane */}
          <Path
            d="M 0 0 L 260 0 L 260 44 Q 140 64 0 82 Z"
            fill={`url(#glassSubSheen_${uid})`}
          />
        </Svg>

        {/* Razor-Thin Multi-Accent Top Specular Glass Edge */}
        <View
          pointerEvents="none"
          style={[
            styles.topSpecularEdge,
            {backgroundColor: hexToRgba(accentColor, 0.88)},
          ]}
        />

        {isWide ? (
          <View style={styles.cardContentWide}>
            <View style={styles.iconRowWide}>
              <View
                style={[
                  styles.iconBadge,
                  {
                    backgroundColor: hexToRgba(accentColor, 0.2),
                    borderColor: hexToRgba(accentColor, 0.72),
                  },
                ]}>
                <Icon source={icon} color={accentColor} size={25} />
              </View>
              <View
                style={[
                  styles.chevronBadge,
                  {
                    backgroundColor: hexToRgba(accentColor, 0.16),
                    borderColor: hexToRgba(accentColor, 0.42),
                  },
                ]}>
                <Icon source="chevron-right" color={accentColor} size={18} />
              </View>
            </View>

            <View style={styles.textColumnWide}>
              <Text
                variant="titleMedium"
                numberOfLines={1}
                style={styles.title}>
                {title}
              </Text>
              {subtitle ? (
                <Text
                  variant="labelSmall"
                  numberOfLines={1}
                  style={[styles.subtitle, {color: tokens.textSecondary}]}>
                  {subtitle}
                </Text>
              ) : null}
            </View>
          </View>
        ) : (
          <View style={styles.cardContentRow}>
            <View
              style={[
                styles.iconBadge,
                {
                  backgroundColor: hexToRgba(accentColor, 0.2),
                  borderColor: hexToRgba(accentColor, 0.72),
                },
              ]}>
              <Icon source={icon} color={accentColor} size={24} />
            </View>

            <View style={styles.textColumnRow}>
              <Text
                variant="titleMedium"
                numberOfLines={1}
                style={styles.title}>
                {title}
              </Text>
              {subtitle ? (
                <Text
                  variant="labelSmall"
                  numberOfLines={1}
                  style={[styles.subtitle, {color: tokens.textSecondary}]}>
                  {subtitle}
                </Text>
              ) : null}
            </View>

            <View
              style={[
                styles.chevronBadge,
                {
                  backgroundColor: hexToRgba(accentColor, 0.16),
                  borderColor: hexToRgba(accentColor, 0.42),
                },
              ]}>
              <Icon source="chevron-right" color={accentColor} size={18} />
            </View>
          </View>
        )}
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    width: '100%',
  },
  glassCard: {
    minHeight: 106,
    borderRadius: 15,
    borderWidth: 1.2,
    overflow: 'hidden',
    elevation: 6,
    shadowColor: '#020617',
    shadowOffset: {
      width: 0,
      height: 6,
    },
    shadowOpacity: 0.28,
    shadowRadius: 14,
  },
  glassCardCompactRow: {
    minHeight: 74,
  },
  glassCardFocused: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
    transform: [{scale: Platform.isTV ? 1.03 : 1.015}],
    elevation: 10,
    shadowOpacity: 0.65,
    shadowRadius: 18,
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
  cardContentWide: {
    minHeight: 106,
    flexDirection: 'column',
    justifyContent: 'space-between',
    paddingVertical: 12,
    paddingHorizontal: 14,
  },
  cardContentRow: {
    minHeight: 74,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 12,
    paddingHorizontal: 14,
  },
  iconRowWide: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  iconBadge: {
    width: 44,
    height: 44,
    borderRadius: 13,
    backgroundColor: 'rgba(6, 15, 40, 0.62)',
    borderWidth: 1.2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chevronBadge: {
    width: 30,
    height: 30,
    borderRadius: 15,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  textColumnWide: {
    marginTop: 10,
  },
  textColumnRow: {
    flex: 1,
    marginHorizontal: 13,
    justifyContent: 'center',
  },
  title: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 16,
    letterSpacing: 0.2,
    textShadowColor: 'rgba(0, 0, 0, 0.32)',
    textShadowOffset: {width: 0, height: 1},
    textShadowRadius: 3,
  },
  subtitle: {
    color: 'rgba(224, 242, 254, 0.88)',
    marginTop: 2,
    fontSize: 12,
    letterSpacing: 0.2,
    fontWeight: '600',
  },
});

export default React.memo(HomeItem);
