import React from 'react';
import {
  Animated,
  Easing,
  Modal,
  NativeModules,
  StyleProp,
  StyleSheet,
  Text,
  TextStyle,
  View,
} from 'react-native';
import Svg, {
  Circle,
  Defs,
  G,
  LinearGradient,
  Path,
  RadialGradient,
  Stop,
} from 'react-native-svg';
import {getSettings} from '../store/settingStore';
import {resolveThemeTokens} from '../theme/presets';
import {formatConnectionStatus} from '../utils/connectionStatus';
import {hexToRgba, shiftColor} from '../utils/themeColor';

type Props = {
  loading: boolean;
  cancelable?: boolean;
  text?: string;
  textStyle?: StyleProp<TextStyle>;
  closeCb?: () => void;
  onChange?: (value: any) => void;
};

const Loading: React.FC<Props> = ({
  loading,
  cancelable = false,
  text = '',
  textStyle,
  closeCb,
}) => {
  const spinAnim = React.useRef(new Animated.Value(0)).current;
  const pulseAnim = React.useRef(new Animated.Value(0.65)).current;

  React.useEffect(() => {
    if (!loading) {
      spinAnim.stopAnimation();
      pulseAnim.stopAnimation();
      return;
    }

    spinAnim.setValue(0);
    const spinLoop = Animated.loop(
      Animated.timing(spinAnim, {
        toValue: 1,
        duration: 2100,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    );

    const pulseLoop = Animated.loop(
      Animated.sequence([
        Animated.timing(pulseAnim, {
          toValue: 1,
          duration: 950,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
        Animated.timing(pulseAnim, {
          toValue: 0.55,
          duration: 950,
          easing: Easing.inOut(Easing.quad),
          useNativeDriver: true,
        }),
      ]),
    );

    spinLoop.start();
    pulseLoop.start();

    return () => {
      spinLoop.stop();
      pulseLoop.stop();
    };
  }, [loading, pulseAnim, spinAnim]);

  if (
    !loading ||
    NativeModules.NativeUiBridge?.isNativeUiEnabled?.() === true
  ) {
    return null;
  }

  const settings = getSettings();
  const tokens = resolveThemeTokens(
    settings.theme,
    settings.theme === 'light' ? 'light' : 'dark',
    settings.theme_primary_color,
  );
  const status = formatConnectionStatus(text);

  const rotateInterpolate = spinAnim.interpolate({
    inputRange: [0, 1],
    outputRange: ['0deg', '360deg'],
  });

  const ringColor = tokens.accentLime;
  const primaryColor = tokens.primaryColor;
  const deepShade = shiftColor(primaryColor, -0.48);

  return (
    <Modal
      transparent={true}
      animationType="fade"
      visible={loading}
      onRequestClose={() => {
        if (cancelable && closeCb) {
          closeCb();
        }
      }}>
      <View style={styles.backdrop}>
        <View
          style={[
            styles.hudCard,
            {
              backgroundColor: tokens.isLight
                ? 'rgba(244, 248, 252, 0.94)'
                : hexToRgba(deepShade, 0.9),
              borderColor: tokens.glassBorder,
              shadowColor: primaryColor,
            },
          ]}>
          {/* Top NXE Bevel Highlight */}
          <View
            pointerEvents="none"
            style={[
              styles.topBevel,
              {backgroundColor: tokens.glassBorderHighlight},
            ]}
          />

          {/* Diagonal Gloss Sheen */}
          <View
            pointerEvents="none"
            style={[styles.glossSheen, {backgroundColor: tokens.glassSheen}]}
          />

          {/* Xbox 360 Ring of Light + 3D Guide Sphere */}
          <View style={styles.orbWrapper}>
            <Animated.View
              style={[
                styles.pulseHalo,
                {
                  backgroundColor: tokens.accentGlow,
                  borderColor: hexToRgba(ringColor, 0.45),
                  opacity: pulseAnim,
                },
              ]}
            />

            {/* Rotating 4-Quadrant Ring of Light */}
            <Animated.View
              style={[
                styles.rotatingRing,
                {transform: [{rotate: rotateInterpolate}]},
              ]}>
              <Svg width={92} height={92} viewBox="0 0 92 92">
                <Defs>
                  <LinearGradient id="rolGrad" x1="0" y1="0" x2="1" y2="1">
                    <Stop offset="0" stopColor="#FFFFFF" stopOpacity="0.95" />
                    <Stop offset="0.45" stopColor={ringColor} stopOpacity="1" />
                    <Stop
                      offset="1"
                      stopColor={primaryColor}
                      stopOpacity="0.35"
                    />
                  </LinearGradient>
                </Defs>
                {/* Track ring */}
                <Circle
                  cx="46"
                  cy="46"
                  r="40"
                  stroke={hexToRgba(ringColor, 0.18)}
                  strokeWidth="4"
                  fill="none"
                />
                {/* 4 Xbox 360 Ring-of-Light Quadrants */}
                <Circle
                  cx="46"
                  cy="46"
                  r="40"
                  stroke="url(#rolGrad)"
                  strokeWidth="4.5"
                  strokeLinecap="round"
                  strokeDasharray="46 17"
                  fill="none"
                />
              </Svg>
            </Animated.View>

            {/* Inner 3D Metallic & Glowing Xbox Sphere */}
            <View style={styles.sphereCore}>
              <Svg width={60} height={60} viewBox="0 0 60 60">
                <Defs>
                  <RadialGradient
                    id="xboxSphereBody"
                    cx="35%"
                    cy="28%"
                    rx="65%"
                    ry="65%">
                    <Stop offset="0" stopColor="#FFFFFF" />
                    <Stop offset="0.35" stopColor="#D6DEE6" />
                    <Stop offset="0.78" stopColor="#84909E" />
                    <Stop offset="1" stopColor="#4A5460" />
                  </RadialGradient>
                  <LinearGradient id="xboxXMark" x1="0" y1="0" x2="1" y2="1">
                    <Stop offset="0" stopColor="#FFFFFF" />
                    <Stop offset="0.35" stopColor={ringColor} />
                    <Stop offset="1" stopColor={primaryColor} />
                  </LinearGradient>
                </Defs>
                <Circle cx="30" cy="30" r="26" fill="url(#xboxSphereBody)" />
                {/* Glowing X Swooshes on the Sphere */}
                <G>
                  <Path
                    d="M 16 15 C 25 22, 35 32, 44 45"
                    stroke="url(#xboxXMark)"
                    strokeWidth="5"
                    strokeLinecap="round"
                    fill="none"
                  />
                  <Path
                    d="M 44 15 C 35 22, 25 32, 16 45"
                    stroke="url(#xboxXMark)"
                    strokeWidth="5"
                    strokeLinecap="round"
                    fill="none"
                  />
                </G>
                {/* Top Specular Glint */}
                <Circle
                  cx="23"
                  cy="18"
                  r="8"
                  fill="rgba(255, 255, 255, 0.35)"
                />
              </Svg>
            </View>
          </View>

          {/* Badge */}
          <View
            style={[
              styles.badgePill,
              {
                backgroundColor: hexToRgba(primaryColor, 0.28),
                borderColor: hexToRgba(ringColor, 0.45),
              },
            ]}>
            <View
              style={[styles.badgeDot, {backgroundColor: ringColor}]}
            />
            <Text style={[styles.badgeText, {color: ringColor}]}>
              {status.badge}
            </Text>
          </View>

          {/* Friendly Headline */}
          <Text style={[styles.headline, {color: tokens.textPrimary}]}>
            {status.headline}
          </Text>

          {/* Masked Friendly Subtitle */}
          <Text
            style={StyleSheet.flatten([
              styles.subtitle,
              {color: tokens.textSecondary},
              textStyle,
            ])}>
            {status.subtitle}
          </Text>

          {/* 4-Quadrant NXE Progress Steps (shown during stream connection) */}
          {status.step > 0 && (
            <View style={styles.stepBarRow}>
              {[1, 2, 3, 4].map(idx => {
                const isActive = idx <= status.step;
                const isCurrent = idx === status.step;
                return (
                  <View
                    key={idx}
                    style={[
                      styles.stepSegment,
                      {
                        backgroundColor: isActive
                          ? ringColor
                          : hexToRgba(tokens.textSecondary, 0.2),
                        borderColor: isCurrent
                          ? '#FFFFFF'
                          : 'transparent',
                        opacity: isActive ? 1 : 0.45,
                      },
                    ]}
                  />
                );
              })}
            </View>
          )}
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(5, 11, 20, 0.68)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  hudCard: {
    width: '100%',
    maxWidth: 370,
    borderRadius: 16,
    borderWidth: 1.5,
    paddingVertical: 26,
    paddingHorizontal: 24,
    alignItems: 'center',
    overflow: 'hidden',
    elevation: 18,
    shadowOffset: {width: 0, height: 12},
    shadowOpacity: 0.55,
    shadowRadius: 24,
  },
  topBevel: {
    position: 'absolute',
    top: 0,
    left: 10,
    right: 10,
    height: 1.5,
    borderRadius: 2,
  },
  glossSheen: {
    position: 'absolute',
    top: -45,
    left: -35,
    width: '75%',
    height: 130,
    borderRadius: 65,
    transform: [{rotate: '-10deg'}],
  },
  orbWrapper: {
    width: 96,
    height: 96,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  pulseHalo: {
    position: 'absolute',
    width: 84,
    height: 84,
    borderRadius: 42,
    borderWidth: 1,
  },
  rotatingRing: {
    position: 'absolute',
    width: 92,
    height: 92,
    alignItems: 'center',
    justifyContent: 'center',
  },
  sphereCore: {
    width: 60,
    height: 60,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgePill: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 999,
    borderWidth: 1,
    marginBottom: 8,
  },
  badgeDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    marginRight: 6,
  },
  badgeText: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 1.2,
  },
  headline: {
    fontSize: 19,
    fontWeight: '800',
    letterSpacing: 0.3,
    textAlign: 'center',
    marginBottom: 6,
  },
  subtitle: {
    fontSize: 13,
    fontWeight: '500',
    textAlign: 'center',
    lineHeight: 19,
  },
  stepBarRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 18,
    width: '82%',
  },
  stepSegment: {
    flex: 1,
    height: 6,
    borderRadius: 3,
    marginHorizontal: 4,
    borderWidth: 0.5,
  },
});

export default Loading;
