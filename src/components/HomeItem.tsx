import React from 'react';
import {StyleSheet, View, Platform, TouchableOpacity} from 'react-native';
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
import {hexToRgba, normalizeHexColor, shiftColor} from '../utils/themeColor';

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
  const settings = getSettings();
  const tokens = resolveThemeTokens(
    theme.dark ? settings.theme : 'light',
    theme.dark ? 'dark' : 'light',
    settings.theme_primary_color,
  );

  const seed = normalizeHexColor(
    color || tokens.primaryColor,
    tokens.primaryColor,
  );
  const tileTop = shiftColor(seed, 0.12);
  const tileMid = shiftColor(seed, -0.14);
  const tileBottom = shiftColor(seed, -0.46);

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
          {
            backgroundColor: tokens.isLight
              ? 'rgba(255, 255, 255, 0.34)'
              : 'rgba(8, 18, 48, 0.44)',
            borderColor: isFocused
              ? tokens.accentLime
              : tokens.glassBorder,
          },
          isFocused && [
            styles.glassCardFocused,
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
          viewBox="0 0 260 130"
          preserveAspectRatio="none"
          style={StyleSheet.absoluteFillObject}>
          <Defs>
            <LinearGradient id="glassSubTileBody" x1="0" y1="0" x2="0.85" y2="1">
              <Stop
                offset="0"
                stopColor={tileTop}
                stopOpacity={tokens.isDarkCarbonStage ? '0.46' : '0.56'}
              />
              <Stop
                offset="0.52"
                stopColor={tileMid}
                stopOpacity={tokens.isDarkCarbonStage ? '0.36' : '0.46'}
              />
              <Stop
                offset="1"
                stopColor={tileBottom}
                stopOpacity={tokens.isDarkCarbonStage ? '0.52' : '0.62'}
              />
            </LinearGradient>
            <RadialGradient
              id="glassSubTileFrost"
              cx="24%"
              cy="16%"
              rx="60%"
              ry="54%">
              <Stop
                offset="0"
                stopColor={tokens.horizonRim}
                stopOpacity="0.24"
              />
              <Stop
                offset="0.6"
                stopColor={tokens.primaryColor}
                stopOpacity="0.08"
              />
              <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
            </RadialGradient>
            <LinearGradient id="glassSubTileSheen" x1="0" y1="0" x2="0.45" y2="1">
              <Stop
                offset="0"
                stopColor={tokens.horizonRim}
                stopOpacity="0.18"
              />
              <Stop offset="0.5" stopColor="#FFFFFF" stopOpacity="0.05" />
              <Stop offset="1" stopColor="#FFFFFF" stopOpacity="0" />
            </LinearGradient>
          </Defs>
          <Rect
            x="0"
            y="0"
            width="260"
            height="130"
            fill="url(#glassSubTileBody)"
          />
          <Rect
            x="0"
            y="0"
            width="260"
            height="130"
            fill="url(#glassSubTileFrost)"
          />
          {/* Diagonal Specular Glass Reflection Pane */}
          <Path
            d="M 0 0 L 260 0 L 260 48 Q 140 70 0 88 Z"
            fill="url(#glassSubTileSheen)"
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

        <View style={styles.cardContent}>
          {/* Top-Left Frosted Glass Icon Medallion with Cyan Ring */}
          <View style={styles.iconRow}>
            <View
              style={[
                styles.iconBadge,
                {borderColor: hexToRgba(tokens.accentLime, 0.68)},
              ]}>
              <Icon source={icon} color="#FFFFFF" size={28} />
            </View>
            <Icon
              source="chevron-right"
              color={tokens.accentLime}
              size={20}
            />
          </View>

          {/* Bottom-Left Channel Label */}
          <View style={styles.textColumn}>
            <Text variant="titleMedium" numberOfLines={1} style={styles.title}>
              {title}
            </Text>
            {subtitle ? (
              <Text
                variant="labelSmall"
                numberOfLines={1}
                style={styles.subtitle}>
                {subtitle}
              </Text>
            ) : null}
          </View>
        </View>
      </TouchableOpacity>

      {/* Subtle Frosted Glass Floor Reflection Under Tile */}
      <View pointerEvents="none" style={styles.floorReflectionWrap}>
        <Svg
          width="100%"
          height="12"
          viewBox="0 0 260 12"
          preserveAspectRatio="none">
          <Defs>
            <LinearGradient id="glassSubReflect" x1="0" y1="0" x2="0" y2="1">
              <Stop
                offset="0"
                stopColor={hexToRgba(tileTop, 0.30)}
                stopOpacity="0.26"
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
            width="248"
            height="12"
            rx="6"
            fill="url(#glassSubReflect)"
          />
        </Svg>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  wrapper: {
    width: '100%',
  },
  glassCard: {
    minHeight: 112,
    borderRadius: 14,
    borderWidth: 1,
    overflow: 'hidden',
    elevation: 5,
    shadowColor: '#020617',
    shadowOffset: {
      width: 0,
      height: 6,
    },
    shadowOpacity: 0.24,
    shadowRadius: 14,
  },
  glassCardFocused: {
    borderWidth: 2,
    borderColor: '#FFFFFF',
    transform: [{scale: Platform.isTV ? 1.04 : 1.02}],
    elevation: 10,
    shadowOpacity: 0.65,
    shadowRadius: 18,
    zIndex: 99,
  },
  topSpecularEdge: {
    position: 'absolute',
    top: 0,
    left: 6,
    right: 6,
    height: 1.2,
    backgroundColor: 'rgba(255, 255, 255, 0.80)',
    zIndex: 3,
  },
  cardContent: {
    minHeight: 112,
    flexDirection: 'column',
    justifyContent: 'space-between',
    paddingVertical: 12,
    paddingHorizontal: 14,
  },
  iconRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  iconBadge: {
    width: 46,
    height: 46,
    borderRadius: 23,
    backgroundColor: 'rgba(6, 15, 40, 0.56)',
    borderWidth: 1,
    borderColor: 'rgba(0, 229, 255, 0.68)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  textColumn: {
    marginTop: 10,
  },
  title: {
    color: '#FFFFFF',
    fontWeight: '800',
    letterSpacing: 0.2,
    textShadowColor: 'rgba(0, 0, 0, 0.28)',
    textShadowOffset: {width: 0, height: 1},
    textShadowRadius: 3,
  },
  subtitle: {
    color: 'rgba(255, 255, 255, 0.88)',
    marginTop: 1,
    letterSpacing: 0.2,
    fontWeight: '600',
  },
  floorReflectionWrap: {
    width: '100%',
    height: 12,
    marginTop: 2,
    opacity: 0.85,
  },
});

export default React.memo(HomeItem);
