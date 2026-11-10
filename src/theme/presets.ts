// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later

import {
  DEFAULT_THEME_PRIMARY_COLOR,
  hexToRgba,
  normalizeHexColor,
  shiftColor,
} from '../utils/themeColor';

export type ThemePresetId =
  | 'nxe_2008'
  | 'nxe_arctic'
  | 'nxe_sapphire'
  | 'nxe_classic_green'
  | 'xbox_series'
  | 'blades_360'
  | 'midnight_glass'
  | 'dark'
  | 'light'
  | 'auto';

export interface ThemeGlassTokens {
  id: ThemePresetId;
  name: string;
  eraLabel: string;
  isLight: boolean;
  isDarkCarbonStage?: boolean;
  primaryColor: string;
  accentLime: string;
  accentGlow: string;
  // Upper Lit Atmospheric Glass Backdrop Wall
  stageSkyTop: string;
  stageSkyMid: string;
  stageHorizon: string;
  // Lower Sleek Frosted Glass Reflective Stage Floor
  stageFloor: string;
  stageFloorTop: string;
  stageFloorMid: string;
  stageFloorBottom: string;
  stageFloorHorizonLine: string;
  // Horizon Bloom & Ambient Accents
  horizonGlow: string;
  horizonRim: string;
  // Translucent Frosted Glass Tile Stops
  tileTopHighlight: string;
  tileMidColor: string;
  tileBottomShade: string;
  tileBottomLip: string;
  tileRingWatermark: string;
  // Glass & Surface Tokens
  glassBg: string;
  glassBgElevated: string;
  glassBorder: string;
  glassBorderHighlight: string;
  glassSheen: string;
  textPrimary: string;
  textSecondary: string;
  tileColors: {
    cloud: string;
    achievements: string;
    settings: string;
  };
}

const isBlueFamilyHex = (hexColor: string): boolean => {
  const clean = normalizeHexColor(hexColor).replace('#', '');
  const r = parseInt(clean.slice(0, 2), 16);
  const g = parseInt(clean.slice(2, 4), 16);
  const b = parseInt(clean.slice(4, 6), 16);
  return b > r + 25 && b >= g;
};

const buildGlassTokensFromPrimary = (
  id: ThemePresetId,
  name: string,
  eraLabel: string,
  primaryHex: string,
  options?: {
    isLight?: boolean;
    isDarkCarbonStage?: boolean;
    accentHex?: string;
    floorTop?: string;
    floorMid?: string;
    floorBottom?: string;
  },
): ThemeGlassTokens => {
  const seed = normalizeHexColor(primaryHex, DEFAULT_THEME_PRIMARY_COLOR);
  const isLight = !!options?.isLight;
  const isDarkCarbonStage = !!options?.isDarkCarbonStage;
  const isBlueTheme = isBlueFamilyHex(seed);

  // Electric Cyan accent for Deep Royal Blue themes; bright luminous tint for non-blue themes
  const accentHighlight = options?.accentHex
    ? normalizeHexColor(options.accentHex)
    : isBlueTheme
    ? '#00E5FF'
    : shiftColor(seed, 0.45);
  const cyanRim = isBlueTheme ? '#22D3EE' : shiftColor(seed, 0.58);

  // Rich Deep Royal Glass Tile Stops (No washed-out baby-blue pastel)
  const tileTopHighlight = isBlueTheme
    ? shiftColor(seed, 0.14)
    : shiftColor(seed, 0.32);
  const tileMidColor = isBlueTheme
    ? shiftColor(seed, -0.14)
    : shiftColor(seed, 0.06);
  const tileBottomShade = isBlueTheme
    ? shiftColor(seed, -0.46)
    : shiftColor(seed, -0.32);
  const tileBottomLip = shiftColor(seed, -0.56);

  // Deep Royal Sapphire & Midnight Indigo Atmospheric Backdrop Wall
  // Kept dark & rich so translucent Royal Sapphire cards pop with crisp contrast
  const stageSkyTop = isLight
    ? shiftColor(seed, 0.86)
    : isDarkCarbonStage
    ? '#030712'
    : shiftColor(seed, -0.82);
  const stageSkyMid = isLight
    ? shiftColor(seed, 0.72)
    : isDarkCarbonStage
    ? '#071026'
    : shiftColor(seed, -0.62);
  const stageHorizon = isLight
    ? shiftColor(seed, 0.56)
    : isDarkCarbonStage
    ? '#0B193B'
    : shiftColor(seed, -0.42);

  // Deep Royal Smoked-Glass Reflective Stage Floor
  const stageFloorTop =
    options?.floorTop ||
    (isLight
      ? '#CBD5E1'
      : isDarkCarbonStage
      ? '#071128'
      : shiftColor(seed, -0.58));
  const stageFloorMid =
    options?.floorMid ||
    (isLight
      ? '#E2E8F0'
      : isDarkCarbonStage
      ? '#0A1938'
      : shiftColor(seed, -0.48));
  const stageFloorBottom =
    options?.floorBottom ||
    (isLight
      ? '#94A3B8'
      : isDarkCarbonStage
      ? '#030712'
      : shiftColor(seed, -0.86));

  return {
    id,
    name,
    eraLabel,
    isLight,
    isDarkCarbonStage,
    primaryColor: seed,
    accentLime: accentHighlight,
    accentGlow: hexToRgba(accentHighlight, 0.48),
    stageSkyTop,
    stageSkyMid,
    stageHorizon,
    stageFloor: stageFloorBottom,
    stageFloorTop,
    stageFloorMid,
    stageFloorBottom,
    stageFloorHorizonLine: isLight ? '#FFFFFF' : accentHighlight,
    horizonGlow: seed,
    horizonRim: cyanRim,
    tileTopHighlight,
    tileMidColor,
    tileBottomShade,
    tileBottomLip,
    tileRingWatermark: hexToRgba(accentHighlight, 0.14),
    glassBg: isLight
      ? 'rgba(255, 255, 255, 0.78)'
      : hexToRgba(shiftColor(seed, -0.48), 0.72),
    glassBgElevated: isLight
      ? 'rgba(255, 255, 255, 0.90)'
      : hexToRgba(shiftColor(seed, -0.36), 0.86),
    glassBorder: hexToRgba(accentHighlight, 0.56),
    glassBorderHighlight: accentHighlight,
    glassSheen: isLight
      ? 'rgba(255, 255, 255, 0.52)'
      : hexToRgba(cyanRim, 0.18),
    textPrimary: isLight ? '#0F172A' : '#FFFFFF',
    textSecondary: isLight
      ? 'rgba(15, 23, 42, 0.76)'
      : 'rgba(224, 242, 254, 0.90)',
    tileColors: {
      cloud: isLight ? '#0284C7' : '#00E5FF',
      achievements: isLight ? '#D97706' : '#F59E0B',
      settings: isLight ? '#7C3AED' : '#A78BFA',
    },
  };
};

export const THEME_PRESETS: Record<string, ThemeGlassTokens> = {
  nxe_2008: buildGlassTokensFromPrimary(
    'nxe_2008',
    'Royal Sapphire & Cyan Glass (Default)',
    'ROYAL GLASS • ELECTRIC CYAN',
    '#1D4ED8',
    {
      accentHex: '#00E5FF',
    },
  ),
  nxe_sapphire: buildGlassTokensFromPrimary(
    'nxe_sapphire',
    'Deep Royal Midnight & Cyan',
    'MIDNIGHT ROYAL • NEON CYAN',
    '#1E3A8A',
    {
      accentHex: '#00E5FF',
    },
  ),
  nxe_arctic: buildGlassTokensFromPrimary(
    'nxe_arctic',
    'Imperial Cobalt & Ice Cyan',
    'IMPERIAL COBALT • CYAN GLOW',
    '#2563EB',
    {
      accentHex: '#22D3EE',
    },
  ),
  nxe_classic_green: buildGlassTokensFromPrimary(
    'nxe_classic_green',
    'Xbox Emerald & Lime Glass',
    'XBOX GLASS • EMERALD LIME',
    '#107C10',
    {
      accentHex: '#84CC16',
    },
  ),
  xbox_series: buildGlassTokensFromPrimary(
    'xbox_series',
    'Abyssal Royal & Cyan Carbon',
    'SERIES X|S • ABYSSAL CYAN',
    '#1D4ED8',
    {
      isDarkCarbonStage: true,
      accentHex: '#00E5FF',
    },
  ),
  light: buildGlassTokensFromPrimary(
    'light',
    'Daylight Royal & Cyan Crystal',
    'ROYAL GLASS • DAYLIGHT CYAN',
    '#1D4ED8',
    {
      isLight: true,
      accentHex: '#0284C7',
    },
  ),
};

// Backwards-compatible aliases for earlier preset IDs
THEME_PRESETS.blades_360 = THEME_PRESETS.nxe_classic_green;
THEME_PRESETS.midnight_glass = THEME_PRESETS.nxe_sapphire;
THEME_PRESETS.dark = THEME_PRESETS.nxe_2008;

export const THEME_PRESET_LIST: Array<{
  id: ThemePresetId;
  name: string;
  eraLabel: string;
  defaultPrimaryColor: string;
}> = [
  {
    id: 'nxe_2008',
    name: THEME_PRESETS.nxe_2008.name,
    eraLabel: THEME_PRESETS.nxe_2008.eraLabel,
    defaultPrimaryColor: THEME_PRESETS.nxe_2008.primaryColor,
  },
  {
    id: 'nxe_arctic',
    name: THEME_PRESETS.nxe_arctic.name,
    eraLabel: THEME_PRESETS.nxe_arctic.eraLabel,
    defaultPrimaryColor: THEME_PRESETS.nxe_arctic.primaryColor,
  },
  {
    id: 'nxe_sapphire',
    name: THEME_PRESETS.nxe_sapphire.name,
    eraLabel: THEME_PRESETS.nxe_sapphire.eraLabel,
    defaultPrimaryColor: THEME_PRESETS.nxe_sapphire.primaryColor,
  },
  {
    id: 'xbox_series',
    name: THEME_PRESETS.xbox_series.name,
    eraLabel: THEME_PRESETS.xbox_series.eraLabel,
    defaultPrimaryColor: THEME_PRESETS.xbox_series.primaryColor,
  },
  {
    id: 'nxe_classic_green',
    name: THEME_PRESETS.nxe_classic_green.name,
    eraLabel: THEME_PRESETS.nxe_classic_green.eraLabel,
    defaultPrimaryColor: THEME_PRESETS.nxe_classic_green.primaryColor,
  },
  {
    id: 'light',
    name: THEME_PRESETS.light.name,
    eraLabel: THEME_PRESETS.light.eraLabel,
    defaultPrimaryColor: THEME_PRESETS.light.primaryColor,
  },
];

export const resolveThemeTokens = (
  themeSetting?: string,
  systemColorScheme?: 'light' | 'dark' | null,
  customPrimaryColor?: string,
): ThemeGlassTokens => {
  let basePreset = THEME_PRESETS.nxe_2008;

  if (themeSetting === 'light') {
    basePreset = THEME_PRESETS.light;
  } else if (themeSetting === 'auto') {
    basePreset =
      systemColorScheme === 'light'
        ? THEME_PRESETS.light
        : THEME_PRESETS.nxe_2008;
  } else if (themeSetting && THEME_PRESETS[themeSetting]) {
    basePreset = THEME_PRESETS[themeSetting];
  }

  if (customPrimaryColor && typeof customPrimaryColor === 'string') {
    const normalizedCustom = normalizeHexColor(
      customPrimaryColor,
      basePreset.primaryColor,
    );
    if (normalizedCustom !== basePreset.primaryColor) {
      return buildGlassTokensFromPrimary(
        basePreset.id,
        basePreset.name,
        basePreset.eraLabel,
        normalizedCustom,
        {
          isLight: basePreset.isLight,
          isDarkCarbonStage: basePreset.isDarkCarbonStage,
          floorTop: basePreset.stageFloorTop,
          floorMid: basePreset.stageFloorMid,
          floorBottom: basePreset.stageFloorBottom,
        },
      );
    }
  }

  return basePreset;
};
