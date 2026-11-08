export const DEFAULT_THEME_PRIMARY_COLOR = '#1D4ED8';

export interface ThemeColorOption {
  value: string;
  label: string;
}

export const THEME_PRIMARY_COLOR_OPTIONS: ThemeColorOption[] = [
  // Curated Deep Royal Blues with Cyan Accents (Default & Primary Spectrum)
  {value: '#1D4ED8', label: 'Royal Sapphire & Cyan (Default) (#1D4ED8)'},
  {value: '#1E3A8A', label: 'Deep Royal Midnight & Cyan (#1E3A8A)'},
  {value: '#2563EB', label: 'Imperial Cobalt & Cyan (#2563EB)'},
  {value: '#1E40AF', label: 'Regal Indigo Sapphire (#1E40AF)'},
  {value: '#0284C7', label: 'Abyssal Cerulean & Cyan (#0284C7)'},
  {value: '#3B82F6', label: 'Electric Royal Azure (#3B82F6)'},
  {value: '#4F46E5', label: 'Royal Ultramarine & Cyan (#4F46E5)'},
  {value: '#0EA5E9', label: 'Aero Ice Blue (#0EA5E9)'},
  {value: '#06B6D4', label: 'Crystal Cyan Glass (#06B6D4)'},
  // Classic Xbox Greens
  {value: '#107C10', label: 'Xbox Series Emerald (#107C10)'},
  {value: '#6CC644', label: 'Classic Xbox Green (#6CC644)'},
  {value: '#84CC16', label: 'Neon Velocity Lime (#84CC16)'},
  // Additional Sleek Glass Accents
  {value: '#14B8A6', label: 'Aquamarine Glass (#14B8A6)'},
  {value: '#7C3AED', label: 'Violet Prism Glass (#7C3AED)'},
  {value: '#EC4899', label: 'Rose Quartz Glass (#EC4899)'},
  {value: '#F43F5E', label: 'Crimson Ruby Glass (#F43F5E)'},
  {value: '#F59E0B', label: 'Solar Amber Glass (#F59E0B)'},
  {value: '#64748B', label: 'Smoked Titanium Glass (#64748B)'},
];

export const THEME_PRIMARY_PRESET_COLORS = THEME_PRIMARY_COLOR_OPTIONS.map(
  option => option.value,
);

type Rgb = {
  r: number;
  g: number;
  b: number;
};

const clamp = (value: number, min = 0, max = 255) =>
  Math.max(min, Math.min(max, value));

const rgbStringToRgb = (color: string): Rgb | null => {
  const matched = color
    .trim()
    .match(/^rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})/i);
  if (!matched) {
    return null;
  }
  return {
    r: clamp(parseInt(matched[1], 10)),
    g: clamp(parseInt(matched[2], 10)),
    b: clamp(parseInt(matched[3], 10)),
  };
};

const hexToRgb = (hexColor: string): Rgb => {
  const normalized = normalizeHexColor(hexColor);
  const clean = normalized.replace('#', '');
  const r = parseInt(clean.slice(0, 2), 16);
  const g = parseInt(clean.slice(2, 4), 16);
  const b = parseInt(clean.slice(4, 6), 16);
  return {r, g, b};
};

const rgbToHex = ({r, g, b}: Rgb): string =>
  `#${clamp(r).toString(16).padStart(2, '0')}${clamp(g)
    .toString(16)
    .padStart(2, '0')}${clamp(b).toString(16).padStart(2, '0')}`.toUpperCase();

const toRgb = (color: string): Rgb => {
  const rgb = rgbStringToRgb(color);
  if (rgb) {
    return rgb;
  }
  return hexToRgb(color);
};

export const hexToRgba = (hexColor: string, alpha: number): string => {
  const {r, g, b} = hexToRgb(hexColor);
  const clampedAlpha = Math.max(0, Math.min(1, alpha));
  return `rgba(${r}, ${g}, ${b}, ${clampedAlpha})`;
};

const mixColor = (sourceColor: string, targetColor: string, ratio: number) => {
  const from = toRgb(sourceColor);
  const to = toRgb(targetColor);
  const p = Math.max(0, Math.min(1, ratio));
  return rgbToHex({
    r: Math.round(from.r + (to.r - from.r) * p),
    g: Math.round(from.g + (to.g - from.g) * p),
    b: Math.round(from.b + (to.b - from.b) * p),
  });
};

export const normalizeHexColor = (
  color: string | undefined,
  fallback = DEFAULT_THEME_PRIMARY_COLOR,
) => {
  if (!color || typeof color !== 'string') {
    return fallback;
  }
  const input = color.trim().replace('#', '');
  if (/^[0-9a-fA-F]{3}$/.test(input)) {
    const expanded = input
      .split('')
      .map(ch => ch + ch)
      .join('');
    return `#${expanded.toUpperCase()}`;
  }
  if (/^[0-9a-fA-F]{6}$/.test(input)) {
    return `#${input.toUpperCase()}`;
  }
  return fallback;
};

/**
 * Shifts a hex color toward luminous ice-white (`amount >= 0`) or deep jewel
 * sapphire-midnight (`#08142C` for `amount < 0`) rather than dead black (`#000000`),
 * preserving chromatic clarity for sleek frosted glass surfaces.
 */
export const shiftColor = (hexColor: string, amount: number) => {
  const normalized = normalizeHexColor(hexColor);
  if (amount >= 0) {
    return mixColor(normalized, '#F8FAFC', amount);
  }
  return mixColor(normalized, '#08142C', -amount);
};

export const getContrastTextColor = (
  hexColor: string,
  light = '#FFFFFF',
  dark = '#0F172A',
) => {
  const {r, g, b} = hexToRgb(normalizeHexColor(hexColor));
  const yiq = (r * 299 + g * 587 + b * 114) / 1000;
  return yiq >= 150 ? dark : light;
};

export const applyPrimaryColorToPaperTheme = (
  baseTheme: any,
  mode: 'light' | 'dark',
  primaryColor: string,
) => {
  const seed = normalizeHexColor(primaryColor);
  const surface =
    baseTheme.colors?.surface || (mode === 'light' ? '#F8FAFC' : '#0B1528');
  const onSurface =
    baseTheme.colors?.onSurface || (mode === 'light' ? '#0F172A' : '#F1F5F9');

  if (mode === 'light') {
    const lightPrimary = seed;
    const primaryContainer = shiftColor(seed, 0.68);
    const secondary = shiftColor(seed, -0.18);
    const secondaryContainer = mixColor(surface, seed, 0.2);
    const tertiary = shiftColor(seed, -0.32);
    const tertiaryContainer = mixColor(surface, seed, 0.26);
    const surfaceVariant = mixColor(surface, seed, 0.1);
    const outline = mixColor(onSurface, seed, 0.35);

    return {
      ...baseTheme,
      colors: {
        ...baseTheme.colors,
        primary: lightPrimary,
        onPrimary: getContrastTextColor(lightPrimary),
        primaryContainer,
        onPrimaryContainer: getContrastTextColor(primaryContainer),
        secondary,
        onSecondary: getContrastTextColor(secondary),
        secondaryContainer,
        onSecondaryContainer: getContrastTextColor(secondaryContainer),
        tertiary,
        onTertiary: getContrastTextColor(tertiary),
        tertiaryContainer,
        onTertiaryContainer: getContrastTextColor(tertiaryContainer),
        surfaceVariant,
        outline,
        inversePrimary: shiftColor(seed, 0.28),
        elevation: {
          ...baseTheme.colors?.elevation,
          level0: 'transparent',
          level1: mixColor(surface, seed, 0.05),
          level2: mixColor(surface, seed, 0.08),
          level3: mixColor(surface, seed, 0.11),
          level4: mixColor(surface, seed, 0.13),
          level5: mixColor(surface, seed, 0.16),
        },
      },
    };
  }

  const darkPrimary = shiftColor(seed, 0.28);
  const primaryContainer = shiftColor(seed, -0.42);
  const secondary = shiftColor(seed, 0.38);
  const secondaryContainer = mixColor(surface, seed, 0.24);
  const tertiary = shiftColor(seed, 0.52);
  const tertiaryContainer = mixColor(surface, seed, 0.3);
  const surfaceVariant = mixColor(surface, seed, 0.18);
  const outline = mixColor(onSurface, seed, 0.44);

  return {
    ...baseTheme,
    colors: {
      ...baseTheme.colors,
      primary: darkPrimary,
      onPrimary: getContrastTextColor(darkPrimary),
      primaryContainer,
      onPrimaryContainer: getContrastTextColor(primaryContainer),
      secondary,
      onSecondary: getContrastTextColor(secondary),
      secondaryContainer,
      onSecondaryContainer: getContrastTextColor(secondaryContainer),
      tertiary,
      onTertiary: getContrastTextColor(tertiary),
      tertiaryContainer,
      onTertiaryContainer: getContrastTextColor(tertiaryContainer),
      surfaceVariant,
      outline,
      inversePrimary: seed,
      elevation: {
        ...baseTheme.colors?.elevation,
        level0: 'transparent',
        level1: mixColor(surface, seed, 0.1),
        level2: mixColor(surface, seed, 0.14),
        level3: mixColor(surface, seed, 0.18),
        level4: mixColor(surface, seed, 0.22),
        level5: mixColor(surface, seed, 0.26),
      },
    },
  };
};

export const colorizeMacroIconXml = (xml: string, primaryColor: string) => {
  const normalizedPrimary = normalizeHexColor(primaryColor);
  const innerPrimary = shiftColor(normalizedPrimary, -0.25);
  return xml
    .replace(/__PRIMARY__/g, normalizedPrimary)
    .replace(/__PRIMARY_DARK__/g, innerPrimary);
};
