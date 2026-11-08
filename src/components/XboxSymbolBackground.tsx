import React from 'react';
import {
  DeviceEventEmitter,
  StyleSheet,
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

const BACKGROUND_OVERDRAW = 96;

const XboxSymbolBackground = ({isLight, primaryColor, themeName}: Props) => {
  const {width, height: windowHeight} = useWindowDimensions();
  const height = windowHeight + BACKGROUND_OVERDRAW;

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

  // Flat, Sleek Glass Studio Horizon (Zero Blade-era curved arcs or concentric rings)
  const horizonY = Math.round(windowHeight * 0.58);

  return (
    <Svg
      pointerEvents="none"
      width={width}
      height={height}
      viewBox={`0 0 ${width} ${height}`}
      style={styles.background}>
      <Defs>
        {/* 1. Upper Atmospheric Glass Studio Wall */}
        <LinearGradient id="glassWallGrad" x1="0.1" y1="0" x2="0.2" y2="1">
          <Stop offset="0" stopColor={tokens.stageSkyTop} />
          <Stop offset="0.42" stopColor={tokens.stageSkyMid} />
          <Stop offset="0.65" stopColor={tokens.stageHorizon} />
          <Stop offset="1" stopColor={tokens.stageFloorBottom} />
        </LinearGradient>

        {/* 2. Left-Horizon Royal Sapphire & Electric Cyan Studio Bloom */}
        <RadialGradient
          id="glassLeftBloom"
          cx="22%"
          cy="54%"
          rx="62%"
          ry="46%">
          <Stop
            offset="0"
            stopColor={tokens.isLight ? '#FFFFFF' : tokens.accentLime}
            stopOpacity={
              tokens.isLight ? '0.62' : tokens.isDarkCarbonStage ? '0.18' : '0.24'
            }
          />
          <Stop
            offset="0.40"
            stopColor={tokens.horizonGlow}
            stopOpacity={tokens.isDarkCarbonStage ? '0.24' : '0.38'}
          />
          <Stop
            offset="0.78"
            stopColor={tokens.primaryColor}
            stopOpacity={tokens.isDarkCarbonStage ? '0.12' : '0.18'}
          />
          <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
        </RadialGradient>

        {/* 3. Upper-Right Ambient Cyan & Royal Glow */}
        <RadialGradient
          id="glassRightBloom"
          cx="84%"
          cy="28%"
          rx="48%"
          ry="40%">
          <Stop
            offset="0"
            stopColor={tokens.horizonRim}
            stopOpacity={tokens.isDarkCarbonStage ? '0.16' : '0.26'}
          />
          <Stop
            offset="0.55"
            stopColor={tokens.primaryColor}
            stopOpacity={tokens.isDarkCarbonStage ? '0.10' : '0.18'}
          />
          <Stop offset="1" stopColor={tokens.primaryColor} stopOpacity="0" />
        </RadialGradient>

        {/* 4. Subtle Diagonal Cyan Glass Light Refraction Beam */}
        <LinearGradient id="glassLightBeam" x1="0" y1="0" x2="1" y2="1">
          <Stop
            offset="0"
            stopColor={tokens.isLight ? '#FFFFFF' : tokens.horizonRim}
            stopOpacity="0.10"
          />
          <Stop
            offset="0.45"
            stopColor={tokens.isLight ? '#FFFFFF' : tokens.primaryColor}
            stopOpacity="0.04"
          />
          <Stop offset="1" stopColor="#000000" stopOpacity="0" />
        </LinearGradient>

        {/* 5. Deep Royal Frosted Glass Reflective Floor */}
        <LinearGradient id="glassFloorGrad" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor={tokens.stageFloorMid} stopOpacity="0.95" />
          <Stop offset="0.28" stopColor={tokens.stageFloorTop} stopOpacity="0.98" />
          <Stop offset="1" stopColor={tokens.stageFloorBottom} stopOpacity="1" />
        </LinearGradient>

        {/* 6. Frosted Floor Cyan & Royal Specular Reflection Wash */}
        <RadialGradient
          id="glassFloorSpecular"
          cx="38%"
          cy="12%"
          rx="60%"
          ry="52%">
          <Stop
            offset="0"
            stopColor={tokens.isLight ? '#FFFFFF' : tokens.accentLime}
            stopOpacity={
              tokens.isLight ? '0.36' : tokens.isDarkCarbonStage ? '0.12' : '0.18'
            }
          />
          <Stop
            offset="0.52"
            stopColor={tokens.primaryColor}
            stopOpacity={tokens.isDarkCarbonStage ? '0.08' : '0.16'}
          />
          <Stop offset="1" stopColor="#000000" stopOpacity="0" />
        </RadialGradient>

        {/* 7. Electric Cyan Horizon Laser Seam */}
        <LinearGradient id="glassHorizonSeam" x1="0" y1="0" x2="1" y2="0">
          <Stop offset="0" stopColor={tokens.accentLime} stopOpacity="0.18" />
          <Stop offset="0.28" stopColor={tokens.accentLime} stopOpacity="0.82" />
          <Stop offset="0.68" stopColor={tokens.horizonRim} stopOpacity="0.58" />
          <Stop offset="1" stopColor={tokens.accentLime} stopOpacity="0.16" />
        </LinearGradient>
      </Defs>

      {/* Upper Lit Atmospheric Glass Backdrop */}
      <Rect x="0" y="0" width={width} height={height} fill="url(#glassWallGrad)" />

      {/* Left-Horizon Ice/Glass Studio Bloom */}
      <Rect
        x="0"
        y="0"
        width={width}
        height={windowHeight * 0.72}
        fill="url(#glassLeftBloom)"
      />

      {/* Right-Horizon Ambient Crystalline Glow */}
      <Rect
        x="0"
        y="0"
        width={width}
        height={windowHeight * 0.72}
        fill="url(#glassRightBloom)"
      />

      {/* Subtle Diagonal Glass Light Refraction */}
      <Path
        d={`M 0 0 L ${width * 0.62} 0 L ${width * 0.28} ${horizonY} L 0 ${horizonY} Z`}
        fill="url(#glassLightBeam)"
      />

      {/* Lower Sleek Frosted Glass Reflective Floor (Flat Horizon) */}
      <Rect
        x="0"
        y={horizonY}
        width={width}
        height={height - horizonY}
        fill="url(#glassFloorGrad)"
      />

      {/* Specular Reflection Wash on the Frosted Floor */}
      <Rect
        x="0"
        y={horizonY}
        width={width}
        height={height - horizonY}
        fill="url(#glassFloorSpecular)"
      />

      {/* Razor-Thin Flat Glass Horizon Line */}
      <Rect
        x="0"
        y={horizonY - 0.5}
        width={width}
        height={1.2}
        fill="url(#glassHorizonSeam)"
      />
    </Svg>
  );
};

const styles = StyleSheet.create({
  background: {
    ...StyleSheet.absoluteFillObject,
    bottom: -BACKGROUND_OVERDRAW,
  },
});

export default React.memo(XboxSymbolBackground);
