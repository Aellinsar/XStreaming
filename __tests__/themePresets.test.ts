// SPDX-License-Identifier: MPL-2.0 OR AGPL-3.0-or-later
import {THEME_PRESETS, resolveThemeTokens} from '../src/theme/presets';
import {
  DEFAULT_THEME_PRIMARY_COLOR,
  THEME_PRIMARY_COLOR_OPTIONS,
} from '../src/utils/themeColor';

describe('theme/presets (Deep Royal Blue & Electric Cyan Glass System)', () => {
  it('defaults to Royal Sapphire & Cyan Glass (#1D4ED8 with #00E5FF cyan accents)', () => {
    expect(DEFAULT_THEME_PRIMARY_COLOR).toBe('#1D4ED8');
    expect(THEME_PRIMARY_COLOR_OPTIONS[0].value).toBe('#1D4ED8');
    expect(resolveThemeTokens(undefined, 'dark').id).toBe('nxe_2008');
    expect(resolveThemeTokens('dark', 'dark').id).toBe('nxe_2008');
    expect(resolveThemeTokens('nxe_2008', 'dark').isLight).toBe(false);
    expect(resolveThemeTokens('nxe_2008', 'dark').primaryColor).toBe('#1D4ED8');
    expect(resolveThemeTokens('nxe_2008', 'dark').accentLime).toBe('#00E5FF');
  });

  it('resolves all named sleek glass theme presets accurately', () => {
    expect(resolveThemeTokens('nxe_arctic', 'dark').id).toBe('nxe_arctic');
    expect(resolveThemeTokens('nxe_sapphire', 'dark').id).toBe('nxe_sapphire');
    expect(resolveThemeTokens('nxe_classic_green', 'dark').id).toBe(
      'nxe_classic_green',
    );
    expect(resolveThemeTokens('nxe_classic_green', 'dark').primaryColor).toBe(
      '#107C10',
    );
    expect(resolveThemeTokens('xbox_series', 'dark').id).toBe('xbox_series');
    expect(resolveThemeTokens('light', 'dark').isLight).toBe(true);
  });

  it('dynamically blends any custom selectable main color into the active glass preset', () => {
    const customTokens = resolveThemeTokens('nxe_2008', 'dark', '#2563EB');
    expect(customTokens.id).toBe('nxe_2008');
    expect(customTokens.primaryColor).toBe('#2563EB');
    expect(customTokens.tileMidColor).not.toBe(
      THEME_PRESETS.nxe_2008.tileMidColor,
    );
  });

  it('follows system colorScheme when theme is set to auto', () => {
    expect(resolveThemeTokens('auto', 'light').id).toBe('light');
    expect(resolveThemeTokens('auto', 'dark').id).toBe('nxe_2008');
  });

  it('provides valid translucent glass tile, sky wall, and frosted floor tokens for every preset', () => {
    for (const preset of Object.values(THEME_PRESETS)) {
      expect(preset.primaryColor).toMatch(/^#[0-9A-F]{6}$/i);
      expect(preset.stageSkyTop).toMatch(/^#[0-9A-F]{6}$/i);
      expect(preset.stageFloorTop).toMatch(/^#[0-9A-F]{6}$/i);
      expect(preset.tileTopHighlight).toMatch(/^#[0-9A-F]{6}$/i);
      expect(preset.tileBottomShade).toMatch(/^#[0-9A-F]{6}$/i);
      expect(preset.glassBg).toContain('rgba(');
      expect(preset.glassBorder).toContain('rgba(');
      expect(preset.tileColors.cloud).not.toBe(preset.tileColors.achievements);
      expect(preset.tileColors.achievements).not.toBe(
        preset.tileColors.settings,
      );
      expect(preset.stageHorizon).not.toBe(preset.primaryColor);
    }
  });
});
