import React from 'react';
import {
  DeviceEventEmitter,
  Dimensions,
  StyleSheet,
  View,
  useWindowDimensions,
} from 'react-native';
import Svg, {
  Defs,
  LinearGradient,
  Path,
  RadialGradient,
  Rect,
  Stop,
} from 'react-native-svg';
import {resolveThemeTokens} from '../theme/presets';
import {
  getSettings,
  SETTINGS_CHANGED_EVENT,
  Settings,
} from '../store/settingStore';

type Props = {
  isLight: boolean;
  primaryColor?: string;
  themeName?: string;
};

const BACKGROUND_OVERDRAW = 160;

const XboxSymbolBackground = ({isLight, primaryColor, themeName}: Props) => {
  const {width: windowWidth, height: windowHeight} = useWindowDimensions();
  const screenDims = Dimensions.get('screen');
  const width = Math.max(windowWidth, screenDims.width || 0, 1);
  const height =
    Math.max(windowHeight, screenDims.height || 0, 1) + BACKGROUND_OVERDRAW;

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

  const activeThemeName = themeName || liveSettings.theme;
  const activePrimaryColor = liveSettings.theme_primary_color || primaryColor;

  const tokens = resolveThemeTokens(
    isLight ? 'light' : activeThemeName,
    isLight ? 'light' : 'dark',
    activePrimaryColor,
  );

  return (
    <View pointerEvents="none" style={styles.background}>
      <Svg
        pointerEvents="none"
        width="100%"
        height="100%"
        viewBox={`0 0 ${width} ${height}`}
        preserveAspectRatio="none">
        <Defs>
          {/* 1. Continuous Full-Screen Midnight & Royal Sapphire Studio Backdrop */}
          <LinearGradient id="glassWallGrad" x1="0.15" y1="0" x2="0.85" y2="1">
            <Stop offset="0" stopColor={tokens.stageSkyTop} />
            <Stop offset="0.32" stopColor={tokens.stageSkyMid} />
            <Stop offset="0.68" stopColor={tokens.stageHorizon} />
            <Stop offset="1" stopColor={tokens.stageFloorBottom} />
          </LinearGradient>

          {/* 2. Upper-Left Electric Cyan & Royal Ambient Studio Glow */}
          <RadialGradient
            id="glassLeftBloom"
            cx="18%"
            cy="24%"
            rx="68%"
            ry="44%">
            <Stop
              offset="0"
              stopColor={tokens.isLight ? '#FFFFFF' : tokens.accentLime}
              stopOpacity={
                tokens.isLight
                  ? '0.52'
                  : tokens.isDarkCarbonStage
                  ? '0.16'
                  : '0.22'
              }
            />
            <Stop
              offset="0.45"
              stopColor={tokens.horizonGlow}
              stopOpacity={tokens.isDarkCarbonStage ? '0.22' : '0.34'}
            />
            <Stop
              offset="0.82"
              stopColor={tokens.primaryColor}
              stopOpacity={tokens.isDarkCarbonStage ? '0.08' : '0.14'}
            />
            <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
          </RadialGradient>

          {/* 3. Center-Right Royal Sapphire & Cyan Depth Bloom */}
          <RadialGradient
            id="glassRightBloom"
            cx="86%"
            cy="52%"
            rx="62%"
            ry="46%">
            <Stop
              offset="0"
              stopColor={tokens.horizonRim}
              stopOpacity={tokens.isDarkCarbonStage ? '0.14' : '0.22'}
            />
            <Stop
              offset="0.55"
              stopColor={tokens.primaryColor}
              stopOpacity={tokens.isDarkCarbonStage ? '0.12' : '0.22'}
            />
            <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
          </RadialGradient>

          {/* 4. Subtle Diagonal Glass Light Refraction Beam */}
          <LinearGradient id="glassLightBeam" x1="0" y1="0" x2="1" y2="1">
            <Stop
              offset="0"
              stopColor={tokens.isLight ? '#FFFFFF' : tokens.horizonRim}
              stopOpacity="0.09"
            />
            <Stop
              offset="0.45"
              stopColor={tokens.isLight ? '#FFFFFF' : tokens.primaryColor}
              stopOpacity="0.03"
            />
            <Stop offset="1" stopColor="#000000" stopOpacity="0" />
          </LinearGradient>

          {/* 5. Lower Ambient Floor Specular Wash (Smoothly blended, zero hard seam) */}
          <RadialGradient
            id="glassFloorSpecular"
            cx="45%"
            cy="88%"
            rx="72%"
            ry="36%">
            <Stop
              offset="0"
              stopColor={tokens.isLight ? '#FFFFFF' : tokens.primaryColor}
              stopOpacity={
                tokens.isLight
                  ? '0.32'
                  : tokens.isDarkCarbonStage
                  ? '0.16'
                  : '0.24'
              }
            />
            <Stop
              offset="0.58"
              stopColor={tokens.stageFloorMid}
              stopOpacity={tokens.isDarkCarbonStage ? '0.10' : '0.18'}
            />
            <Stop offset="1" stopColor="#000000" stopOpacity="0" />
          </RadialGradient>
        </Defs>

        {/* Continuous Atmospheric Studio Backdrop */}
        <Rect
          x="0"
          y="0"
          width={width}
          height={height}
          fill="url(#glassWallGrad)"
        />

        {/* Upper-Left Ambient Cyan/Royal Bloom */}
        <Rect
          x="0"
          y="0"
          width={width}
          height={height}
          fill="url(#glassLeftBloom)"
        />

        {/* Center-Right Ambient Crystalline Glow */}
        <Rect
          x="0"
          y="0"
          width={width}
          height={height}
          fill="url(#glassRightBloom)"
        />

        {/* Subtle Diagonal Glass Light Refraction */}
        <Path
          d={`M 0 0 L ${width * 0.65} 0 L ${width * 0.25} ${height * 0.68} L 0 ${height * 0.68} Z`}
          fill="url(#glassLightBeam)"
        />

        {/* Smooth Lower Floor Specular Wash */}
        <Rect
          x="0"
          y="0"
          width={width}
          height={height}
          fill="url(#glassFloorSpecular)"
        />
      </Svg>
    </View>
  );
};

const styles = StyleSheet.create({
  background: {
    ...StyleSheet.absoluteFillObject,
    bottom: -BACKGROUND_OVERDRAW,
  },
});

export default React.memo(XboxSymbolBackground);
