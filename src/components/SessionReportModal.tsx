import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  Image,
  Pressable,
  useWindowDimensions,
  ScrollView,
  Platform,
} from 'react-native';
import {Portal, Modal, Icon, useTheme} from 'react-native-paper';
import {useTranslation} from 'react-i18next';
import Svg, {Path, Circle, Text as SvgText} from 'react-native-svg';
import {SessionReportData} from '../utils/sessionStatsTracker';
import {
  useGamepadNavigation,
  useGamepadActiveState,
} from '../utils/useGamepadNavigation';

export interface SessionReportModalProps {
  visible: boolean;
  report: SessionReportData | null;
  onDismiss: () => void;
  onDone: (dontShowAgain: boolean) => void;
}

const polarToCartesian = (
  cx: number,
  cy: number,
  r: number,
  angleDeg: number,
) => {
  const rad = (angleDeg * Math.PI) / 180;
  return {
    x: cx + r * Math.cos(rad),
    y: cy + r * Math.sin(rad),
  };
};

const describeArc = (
  cx: number,
  cy: number,
  r: number,
  startAngle: number,
  endAngle: number,
) => {
  const start = polarToCartesian(cx, cy, r, startAngle);
  const end = polarToCartesian(cx, cy, r, endAngle);
  const arcSweep = endAngle - startAngle <= 180 ? '0' : '1';
  return `M ${start.x} ${start.y} A ${r} ${r} 0 ${arcSweep} 1 ${end.x} ${end.y}`;
};

const SessionReportModal: React.FC<SessionReportModalProps> = ({
  visible,
  report,
  onDismiss,
  onDone,
}) => {
  const {t} = useTranslation();
  const theme = useTheme();
  const isDark = theme.dark;
  const {width: screenWidth, height: screenHeight} = useWindowDimensions();
  const [dontShowAgain, setDontShowAgain] = React.useState(false);
  const [isGamepadActive] = useGamepadActiveState();
  const [focusedBtn, setFocusedBtn] = React.useState<'done' | 'checkbox'>('done');

  React.useEffect(() => {
    if (visible) {
      setDontShowAgain(false);
      setFocusedBtn('done');
    }
  }, [visible]);

  useGamepadNavigation({
    enabled: visible && !!report,
    priority: 30,
    onUp: () => {
      setFocusedBtn('checkbox');
    },
    onDown: () => {
      setFocusedBtn('done');
    },
    onLeft: () => {
      setFocusedBtn('checkbox');
    },
    onRight: () => {
      setFocusedBtn('done');
    },
    onSelect: () => {
      if (focusedBtn === 'checkbox') {
        setDontShowAgain(prev => !prev);
      } else {
        onDone(dontShowAgain);
      }
    },
    onBack: () => {
      onDismiss();
    },
  });

  const colors = React.useMemo(() => {
    return {
      backdropScrim: isDark
        ? 'rgba(2, 6, 18, 0.84)'
        : 'rgba(15, 23, 42, 0.72)',
      cardBg: isDark ? '#0B142C' : '#FFFFFF',
      cardBorder: isDark
        ? 'rgba(0, 229, 255, 0.52)'
        : 'rgba(2, 132, 199, 0.38)',
      topSpecular: isDark ? '#00E5FF' : '#0284C7',
      headerDivider: isDark
        ? 'rgba(56, 189, 248, 0.22)'
        : 'rgba(15, 23, 42, 0.10)',
      cardShadow: '#000000',
      headerEyebrow: isDark ? '#00E5FF' : '#0284C7',
      headerText: isDark ? '#FFFFFF' : '#0F172A',
      closeBtnBg: isDark
        ? 'rgba(15, 30, 68, 0.85)'
        : 'rgba(241, 245, 249, 0.95)',
      closeBtnBorder: isDark
        ? 'rgba(0, 229, 255, 0.42)'
        : 'rgba(148, 163, 184, 0.45)',
      subCardBg: isDark ? 'rgba(14, 27, 60, 0.88)' : '#F1F5F9',
      subCardBorder: isDark
        ? 'rgba(56, 189, 248, 0.24)'
        : 'rgba(15, 23, 42, 0.09)',
      cardHeaderTitle: isDark ? '#38BDF8' : '#0369A1',
      labelColor: isDark ? '#94A3B8' : '#64748B',
      valueColor: isDark ? '#FFFFFF' : '#0F172A',
      subInfoColor: isDark ? '#7DD3FC' : '#64748B',
      iconColor: isDark ? '#38BDF8' : '#0284C7',
      checkboxBorder: isDark ? '#38BDF8' : '#64748B',
      checkboxLabel: isDark ? '#CBD5E1' : '#334155',
      gaugeInnerBg: isDark ? '#091229' : '#FFFFFF',
      gaugeInnerStroke: isDark
        ? 'rgba(0, 229, 255, 0.28)'
        : 'rgba(0, 0, 0, 0.10)',
      scoreTextColor: isDark ? '#FFFFFF' : '#0F172A',
      netInfoLabel: isDark ? '#94A3B8' : '#64748B',
      netInfoValue: isDark ? '#FFFFFF' : '#0F172A',
      doneButtonBg: isDark ? '#16A34A' : theme.colors.primary || '#107C10',
      doneButtonBorder: isDark ? '#4ADE80' : '#22C55E',
      doneButtonText: '#FFFFFF',
      doneButtonFocusedBorder: isDark ? '#FFFFFF' : '#0F172A',
      hintTextColor: isDark ? '#94A3B8' : '#64748B',
      hintKeyBg: isDark ? 'rgba(255, 255, 255, 0.18)' : 'rgba(0, 0, 0, 0.09)',
      hintKeyText: isDark ? '#FFFFFF' : '#0F172A',
      speedArcTrack: isDark
        ? 'rgba(56, 189, 248, 0.24)'
        : 'rgba(0, 0, 0, 0.15)',
      speedArcActive: isDark ? '#00E5FF' : '#0284C7',
      fpsBadgeBorder: isDark
        ? 'rgba(56, 189, 248, 0.4)'
        : 'rgba(0, 0, 0, 0.20)',
      fpsBadgeText: isDark ? '#38BDF8' : '#0369A1',
      heroOverlay: isDark
        ? 'rgba(9, 18, 42, 0.78)'
        : 'rgba(15, 23, 42, 0.60)',
    };
  }, [isDark, theme.colors.primary]);

  if (!report) {
    return null;
  }

  const aspectRatio = screenWidth / Math.max(1, screenHeight);
  const isWideScreen =
    Platform.isTV ||
    aspectRatio >= 1.45 ||
    (screenWidth > screenHeight && screenWidth >= 640);

  const modalWidth = isWideScreen
    ? Math.min(860, screenWidth - 44)
    : Math.min(358, screenWidth - 36);

  const handleDonePress = () => {
    onDone(dontShowAgain);
  };

  const getLatencyColor = (rtt: number) => {
    if (rtt <= 40) return '#2ecc71';
    if (rtt <= 75) return '#f1c40f';
    return '#ff4757';
  };

  const getPacketLossColor = (loss: number) => {
    if (loss <= 0.5) return '#2ecc71';
    if (loss <= 2) return '#f1c40f';
    return '#ff4757';
  };

  const latencyColor = getLatencyColor(report.latencyAvg);
  const packetLossColor = getPacketLossColor(report.packetLossAvg);

  let jitterStatusText = 'MINIMAL';
  let jitterStatusColor = '#2ecc71';
  if (report.jitterAvg > 15) {
    jitterStatusText = 'HIGH';
    jitterStatusColor = '#ff4757';
  } else if (report.jitterAvg > 6) {
    jitterStatusText = 'MODERATE';
    jitterStatusColor = '#f1c40f';
  }

  const gaugeCx = 34;
  const gaugeCy = 34;
  const gaugeRadius = 24;
  const gaugeTotalAngle = 270;
  const gaugeStartAngle = 135;
  const scoreClamped = Math.max(0, Math.min(100, report.score));
  const progressSweep = Math.max(2, (scoreClamped / 100) * gaugeTotalAngle);
  const gaugeEndAngle = gaugeStartAngle + progressSweep;

  const trackPath = describeArc(
    gaugeCx,
    gaugeCy,
    gaugeRadius,
    gaugeStartAngle,
    gaugeStartAngle + gaugeTotalAngle,
  );
  const progressPath = describeArc(
    gaugeCx,
    gaugeCy,
    gaugeRadius,
    gaugeStartAngle,
    gaugeEndAngle,
  );

  const cleanVideoProfile = (report.videoProfile || '')
    .replace(/\/VIDEO\//i, '/ ')
    .replace(/-4D/i, '')
    .replace(/H\.264/i, 'H264');

  // Render Network Performance Metrics rows
  const renderNetworkPerformanceMetrics = (isCompact: boolean) => (
    <>
      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text
            style={[styles.metricLabelText, {color: colors.labelColor}]}
            numberOfLines={1}>
            {t('Data Received (Total):')}
          </Text>
          <Icon source="download" size={13} color={colors.iconColor} />
        </View>
        <Text style={[styles.metricPrimaryValue, {color: colors.valueColor}]}>
          {report.totalDownloadFormatted}
        </Text>
      </View>

      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text
            style={[styles.metricLabelText, {color: colors.labelColor}]}
            numberOfLines={1}>
            {t('Data Sent (Total):')}
          </Text>
          <Icon source="upload" size={13} color={colors.iconColor} />
        </View>
        <Text style={[styles.metricPrimaryValue, {color: colors.valueColor}]}>
          {report.totalUploadFormatted}
        </Text>
      </View>

      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text style={[styles.metricLabelText, {color: colors.labelColor}]}>
            {t('Average Latency:')}
          </Text>
          <Icon source="antenna" size={13} color={colors.iconColor} />
        </View>
        <View style={styles.metricSplitRow}>
          <View>
            <Text style={[styles.metricPrimaryValue, {color: latencyColor}]}>
              {`${report.latencyAvg} ms`}
            </Text>
            <Text
              style={[styles.metricSubInfoText, {color: colors.subInfoColor}]}>
              {`Peak ${report.latencyPeak} ms`}
            </Text>
          </View>
          <Svg width={32} height={14} viewBox="0 0 36 16">
            <Path
              d="M 1 12 Q 10 1, 18 10 T 35 6"
              stroke={latencyColor}
              strokeWidth={2}
              strokeLinecap="round"
              fill="none"
            />
          </Svg>
        </View>
      </View>

      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text style={[styles.metricLabelText, {color: colors.labelColor}]}>
            {t('Average Stream Speed:')}
          </Text>
          <Icon source="speedometer" size={13} color={colors.iconColor} />
        </View>
        <View style={styles.metricSplitRow}>
          <View>
            <Text
              style={[styles.metricPrimaryValue, {color: colors.valueColor}]}>
              {`${report.bitrateAvg} Mbps`}
            </Text>
            <Text
              style={[styles.metricSubInfoText, {color: colors.subInfoColor}]}>
              {`Peak ${report.bitratePeak} Mbps`}
            </Text>
          </View>
          <Svg width={24} height={16} viewBox="0 0 28 18">
            <Path
              d="M 3 15 A 11 11 0 1 1 25 15"
              stroke={colors.speedArcTrack}
              strokeWidth={2}
              strokeLinecap="round"
              fill="none"
            />
            <Path
              d="M 3 15 A 11 11 0 0 1 18 4"
              stroke={colors.speedArcActive}
              strokeWidth={2}
              strokeLinecap="round"
              fill="none"
            />
          </Svg>
        </View>
      </View>
    </>
  );

  // Render Session Stability Metrics rows
  const renderSessionStabilityMetrics = (isCompact: boolean) => (
    <>
      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text style={[styles.metricLabelText, {color: colors.labelColor}]}>
            {t('Packet Loss:')}
          </Text>
          <Icon source="wifi-off" size={13} color={colors.iconColor} />
        </View>
        <View style={styles.metricSplitRow}>
          <Text style={[styles.metricPrimaryValue, {color: packetLossColor}]}>
            {`${report.packetLossAvg.toFixed(2)}%`}
          </Text>
          <View style={styles.metricBadgeRow}>
            <Text
              style={[
                styles.metricBadgeText,
                {color: packetLossColor},
              ]}>
              {t(report.packetLossStatus.toUpperCase())}
            </Text>
            <Icon
              source="arrow-down-drop-circle"
              size={13}
              color={packetLossColor}
            />
          </View>
        </View>
      </View>

      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text style={[styles.metricLabelText, {color: colors.labelColor}]}>
            {t('Jitter (Variation):')}
          </Text>
          <Icon source="tilde" size={13} color={colors.iconColor} />
        </View>
        <View style={styles.metricSplitRow}>
          <View>
            <Text
              style={[styles.metricPrimaryValue, {color: colors.valueColor}]}>
              {`${report.jitterAvg} ms`}
            </Text>
            <Text style={[styles.metricBadgeText, {color: jitterStatusColor}]}>
              {t(jitterStatusText)}
            </Text>
          </View>
          <Svg width={32} height={12} viewBox="0 0 36 14">
            <Path
              d="M 1 7 H 9 L 13 2 L 17 12 L 21 4 L 25 9 L 28 7 H 35"
              stroke={jitterStatusColor}
              strokeWidth={1.8}
              strokeLinecap="round"
              strokeLinejoin="round"
              fill="none"
            />
          </Svg>
        </View>
      </View>

      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text
            style={[styles.metricLabelText, {color: colors.labelColor}]}
            numberOfLines={1}>
            {t('Average Frame Rate:')}
          </Text>
          <View
            style={[
              styles.miniFpsBadge,
              {borderColor: colors.fpsBadgeBorder},
            ]}>
            <Text
              style={[styles.miniFpsBadgeText, {color: colors.fpsBadgeText}]}>
              FPS
            </Text>
          </View>
        </View>
        <Text style={[styles.metricPrimaryValue, {color: colors.valueColor}]}>
          {`${report.fpsAvg} FPS`}
        </Text>
      </View>

      <View
        style={[
          styles.metricRowContainer,
          isCompact && styles.metricRowContainerCompact,
        ]}>
        <View style={styles.metricLabelRow}>
          <Text style={[styles.metricLabelText, {color: colors.labelColor}]}>
            {t('Video Decode Time:')}
          </Text>
          <Icon source="filmstrip" size={13} color={colors.iconColor} />
        </View>
        <Text style={[styles.metricPrimaryValue, {color: colors.valueColor}]}>
          {`${report.decodeAvg} ms`}
        </Text>
        <Text style={[styles.metricSubInfoText, {color: colors.subInfoColor}]}>
          {t('Per video frame')}
        </Text>
      </View>
    </>
  );

  return (
    <Portal>
      <Modal
        visible={visible}
        onDismiss={onDismiss}
        style={styles.fullScreenModal}
        contentContainerStyle={[
          styles.modalOverlay,
          {backgroundColor: colors.backdropScrim},
        ]}>
        <Pressable
          style={StyleSheet.absoluteFillObject}
          onPress={onDismiss}
        />
        <View
          style={[
            styles.cardContainer,
            {
              width: modalWidth,
              maxHeight: screenHeight * 0.86,
              backgroundColor: colors.cardBg,
              borderColor: colors.cardBorder,
              shadowColor: colors.cardShadow,
            },
          ]}>
          {/* Razor-Thin Electric Cyan Top Specular Modal Rim */}
          <View
            pointerEvents="none"
            style={[
              styles.modalTopSpecular,
              {backgroundColor: colors.topSpecular},
            ]}
          />
          {isWideScreen ? (
            /* ========================================================================= */
            /* 16:9 / ANDROID TV / WIDESCREEN DASHBOARD LAYOUT                          */
            /* ========================================================================= */
            <ScrollView
              showsVerticalScrollIndicator={false}
              bounces={false}
              contentContainerStyle={styles.wideScrollContent}>
              {/* Header Bar */}
              <View
                style={[
                  styles.wideHeaderRow,
                  {borderBottomColor: colors.headerDivider},
                ]}>
                <View style={styles.modalHeaderLeft}>
                  <View
                    style={[
                      styles.modalHeaderBadgeIcon,
                      {
                        backgroundColor: colors.closeBtnBg,
                        borderColor: colors.closeBtnBorder,
                      },
                    ]}>
                    <Icon
                      source="speedometer"
                      size={18}
                      color={colors.headerEyebrow}
                    />
                  </View>
                  <View>
                    <Text
                      style={[
                        styles.modalEyebrowText,
                        {color: colors.headerEyebrow},
                      ]}>
                      STREAM TELEMETRY
                    </Text>
                    <Text
                      style={[
                        styles.headerTitleWide,
                        {color: colors.headerText},
                      ]}>
                      {t('Session report')}
                    </Text>
                  </View>
                </View>

                <View style={styles.wideHeaderRight}>
                  <View style={styles.qualityRatingRowWide}>
                    <Text
                      style={[
                        styles.qualityLabelPrefix,
                        {color: colors.labelColor},
                      ]}>
                      {t('SESSION QUALITY:')}{' '}
                    </Text>
                    <Text
                      style={[
                        styles.qualityLabelRating,
                        {color: report.qualityColor},
                      ]}>
                      {t(report.qualityText).toUpperCase()}
                    </Text>
                  </View>

                  <View
                    style={[
                      styles.scorePillBadge,
                      {
                        borderColor: report.qualityColor + '70',
                        backgroundColor: report.qualityColor + '18',
                      },
                    ]}>
                    <Text
                      style={[
                        styles.scorePillText,
                        {color: report.qualityColor},
                      ]}>
                      {`${report.score} / 100`}
                    </Text>
                  </View>

                  <Pressable
                    onPress={onDismiss}
                    hitSlop={8}
                    style={[
                      styles.modalCloseButton,
                      {
                        backgroundColor: colors.closeBtnBg,
                        borderColor: colors.closeBtnBorder,
                      },
                    ]}>
                    <Icon source="close" size={16} color={colors.headerText} />
                  </Pressable>
                </View>
              </View>

              {/* 3-Column Dashboard Body */}
              <View style={styles.wideColumnsRow}>
                {/* Column 1: Hero Poster & Network Information */}
                <View style={styles.wideColLeft}>
                  <View
                    style={[
                      styles.wideHeroCard,
                      {borderColor: colors.subCardBorder},
                    ]}>
                    {!!report.gamePoster ? (
                      <Image
                        source={{uri: report.gamePoster}}
                        style={StyleSheet.absoluteFillObject}
                        resizeMode="cover"
                      />
                    ) : (
                      <View style={styles.heroPosterFallback} />
                    )}
                    <View
                      style={[
                        styles.heroOverlay,
                        {backgroundColor: colors.heroOverlay},
                      ]}
                    />

                    <View style={styles.wideHeroContent}>
                      <Text
                        style={styles.heroGameTitleWide}
                        numberOfLines={2}
                        adjustsFontSizeToFit={true}
                        minimumFontScale={0.8}>
                        {report.gameTitle.toUpperCase()}
                      </Text>

                      <View style={styles.gaugeWrapperCompact}>
                        <Svg width={54} height={54} viewBox="0 0 68 68">
                          <Path
                            d={trackPath}
                            stroke={`${report.qualityColor}30`}
                            strokeWidth={5}
                            strokeLinecap="round"
                            fill="none"
                          />
                          <Path
                            d={progressPath}
                            stroke={report.qualityColor}
                            strokeWidth={5}
                            strokeLinecap="round"
                            fill="none"
                          />
                          <Circle
                            cx={gaugeCx}
                            cy={gaugeCy}
                            r={17}
                            fill={colors.gaugeInnerBg}
                            stroke={colors.gaugeInnerStroke}
                            strokeWidth={1}
                          />
                          <SvgText
                            x={gaugeCx}
                            y={gaugeCy + 6}
                            fill={colors.scoreTextColor}
                            fontSize="16"
                            fontWeight="900"
                            textAnchor="middle">
                            {report.score}
                          </SvgText>
                        </Svg>
                      </View>
                    </View>
                  </View>

                  {/* Network Information Card */}
                  <View
                    style={[
                      styles.wideNetInfoCard,
                      {
                        backgroundColor: colors.subCardBg,
                        borderColor: colors.subCardBorder,
                      },
                    ]}>
                    <Text
                      style={[
                        styles.cardHeaderTitle,
                        {color: colors.cardHeaderTitle, marginBottom: 4},
                      ]}>
                      {t('Network Information')}
                    </Text>

                    <View style={styles.netInfoLine}>
                      <Text
                        style={[
                          styles.netInfoLabel,
                          {color: colors.netInfoLabel},
                        ]}>
                        {`${t('Network Type:')} `}
                      </Text>
                      <Text
                        style={[
                          styles.netInfoValue,
                          {color: colors.netInfoValue},
                        ]}
                        numberOfLines={1}>
                        {report.networkType || 'Wi-Fi (5 GHz)'}
                      </Text>
                    </View>

                    <View style={styles.netInfoLine}>
                      <Text
                        style={[
                          styles.netInfoLabel,
                          {color: colors.netInfoLabel},
                        ]}>
                        {`${t('VIDEO PROFILE:')} `}
                      </Text>
                      <Text
                        style={[
                          styles.netInfoValue,
                          {color: colors.netInfoValue},
                        ]}
                        numberOfLines={1}>
                        {cleanVideoProfile}
                      </Text>
                    </View>
                  </View>
                </View>

                {/* Column 2: Network Performance */}
                <View
                  style={[
                    styles.wideMetricCard,
                    {
                      backgroundColor: colors.subCardBg,
                      borderColor: colors.subCardBorder,
                    },
                  ]}>
                  <Text
                    style={[
                      styles.cardHeaderTitle,
                      {color: colors.cardHeaderTitle},
                    ]}
                    numberOfLines={1}>
                    {t('NETWORK PERFORMANCE')}
                  </Text>
                  {renderNetworkPerformanceMetrics(true)}
                </View>

                {/* Column 3: Session Stability */}
                <View
                  style={[
                    styles.wideMetricCard,
                    {
                      backgroundColor: colors.subCardBg,
                      borderColor: colors.subCardBorder,
                    },
                  ]}>
                  <Text
                    style={[
                      styles.cardHeaderTitle,
                      {color: colors.cardHeaderTitle},
                    ]}
                    numberOfLines={1}>
                    {t('SESSION STABILITY')}
                  </Text>
                  {renderSessionStabilityMetrics(true)}
                </View>
              </View>

              {/* Horizontal Footer Bar */}
              <View style={styles.wideFooterRow}>
                <Pressable
                  onPress={() => setDontShowAgain(!dontShowAgain)}
                  style={({pressed}) => [
                    styles.checkboxWrapper,
                    focusedBtn === 'checkbox' &&
                      (isGamepadActive || Platform.isTV) &&
                      styles.checkboxWrapperFocused,
                    pressed && {opacity: 0.7},
                  ]}>
                  <View
                    style={[
                      styles.checkboxBox,
                      {borderColor: colors.checkboxBorder},
                      dontShowAgain && styles.checkboxBoxChecked,
                    ]}>
                    {dontShowAgain && (
                      <Icon source="check" size={14} color="#FFFFFF" />
                    )}
                  </View>
                  <Text
                    style={[
                      styles.checkboxLabel,
                      {color: colors.checkboxLabel},
                    ]}>
                    {t("Don't show this again")}
                  </Text>
                </Pressable>

                <View style={styles.wideFooterRight}>
                  {(isGamepadActive || Platform.isTV) && (
                    <View style={styles.modalGamepadHintsWide}>
                      <Text
                        style={[
                          styles.modalGamepadHintText,
                          {color: colors.hintTextColor},
                        ]}>
                        <Text
                          style={[
                            styles.hintKeyBadge,
                            {
                              backgroundColor: colors.hintKeyBg,
                              color: colors.hintKeyText,
                            },
                          ]}>
                          A
                        </Text>{' '}
                        {focusedBtn === 'checkbox' ? t('Toggle') : t('Done')}
                        {'   '}
                        <Text
                          style={[
                            styles.hintKeyBadge,
                            {
                              backgroundColor: colors.hintKeyBg,
                              color: colors.hintKeyText,
                            },
                          ]}>
                          B
                        </Text>{' '}
                        {t('Close')}
                      </Text>
                    </View>
                  )}

                  <Pressable
                    onPress={handleDonePress}
                    style={({pressed}) => [
                      styles.donePillButtonWide,
                      {backgroundColor: colors.doneButtonBg},
                      focusedBtn === 'done' &&
                        (isGamepadActive || Platform.isTV) && [
                          styles.donePillButtonFocused,
                          {borderColor: colors.doneButtonFocusedBorder},
                        ],
                      pressed && styles.donePillButtonPressed,
                    ]}>
                    <Text
                      style={[
                        styles.donePillButtonText,
                        {color: colors.doneButtonText},
                      ]}>
                      {t('Done')}
                    </Text>
                  </Pressable>
                </View>
              </View>
            </ScrollView>
          ) : (
            /* ========================================================================= */
            /* PORTRAIT MOBILE MODAL DIALOG LAYOUT                                       */
            /* ========================================================================= */
            <ScrollView
              showsVerticalScrollIndicator={false}
              contentContainerStyle={styles.scrollContent}>
              {/* Elevated Modal Header Bar with Telemetry Badge & Close Button */}
              <View
                style={[
                  styles.portraitModalHeader,
                  {borderBottomColor: colors.headerDivider},
                ]}>
                <View style={styles.modalHeaderLeft}>
                  <View
                    style={[
                      styles.modalHeaderBadgeIcon,
                      {
                        backgroundColor: colors.closeBtnBg,
                        borderColor: colors.closeBtnBorder,
                      },
                    ]}>
                    <Icon
                      source="speedometer"
                      size={18}
                      color={colors.headerEyebrow}
                    />
                  </View>
                  <View>
                    <Text
                      style={[
                        styles.modalEyebrowText,
                        {color: colors.headerEyebrow},
                      ]}>
                      STREAM TELEMETRY
                    </Text>
                    <Text
                      style={[styles.headerTitle, {color: colors.headerText}]}>
                      {t('Session report')}
                    </Text>
                  </View>
                </View>

                <Pressable
                  onPress={onDismiss}
                  hitSlop={8}
                  style={[
                    styles.modalCloseButton,
                    {
                      backgroundColor: colors.closeBtnBg,
                      borderColor: colors.closeBtnBorder,
                    },
                  ]}>
                  <Icon source="close" size={17} color={colors.headerText} />
                </Pressable>
              </View>

              {/* Compact Horizontal Hero Quality Banner */}
              <View
                style={[
                  styles.heroCard,
                  {borderColor: colors.subCardBorder},
                ]}>
                {!!report.gamePoster ? (
                  <Image
                    source={{uri: report.gamePoster}}
                    style={StyleSheet.absoluteFillObject}
                    resizeMode="cover"
                  />
                ) : (
                  <View style={styles.heroPosterFallback} />
                )}

                <View
                  style={[
                    styles.heroOverlay,
                    {backgroundColor: colors.heroOverlay},
                  ]}
                />

                <View style={styles.heroContent}>
                  <View style={styles.heroTitleContainer}>
                    <Text
                      style={styles.heroGameTitle}
                      numberOfLines={2}
                      adjustsFontSizeToFit={true}
                      minimumFontScale={0.75}>
                      {report.gameTitle.toUpperCase()}
                    </Text>
                    <View style={styles.qualityRatingRow}>
                      <Text
                        style={[
                          styles.qualityLabelPrefix,
                          {color: colors.labelColor},
                        ]}>
                        {t('SESSION QUALITY:')}{' '}
                      </Text>
                      <Text
                        style={[
                          styles.qualityLabelRating,
                          {color: report.qualityColor},
                        ]}>
                        {t(report.qualityText).toUpperCase()}
                      </Text>
                    </View>
                  </View>

                  <View style={styles.gaugeWrapper}>
                    <Svg width={60} height={60} viewBox="0 0 68 68">
                      <Path
                        d={trackPath}
                        stroke={`${report.qualityColor}35`}
                        strokeWidth={5}
                        strokeLinecap="round"
                        fill="none"
                      />
                      <Path
                        d={progressPath}
                        stroke={report.qualityColor}
                        strokeWidth={5}
                        strokeLinecap="round"
                        fill="none"
                      />
                      <Circle
                        cx={gaugeCx}
                        cy={gaugeCy}
                        r={17}
                        fill={colors.gaugeInnerBg}
                        stroke={colors.gaugeInnerStroke}
                        strokeWidth={1.2}
                      />
                      <SvgText
                        x={gaugeCx}
                        y={gaugeCy + 6}
                        fill={colors.scoreTextColor}
                        fontSize="16"
                        fontWeight="900"
                        textAnchor="middle">
                        {report.score}
                      </SvgText>
                    </Svg>
                  </View>
                </View>
              </View>

              <View style={styles.metricsTwoColumn}>
                <View
                  style={[
                    styles.metricCard,
                    {
                      backgroundColor: colors.subCardBg,
                      borderColor: colors.subCardBorder,
                    },
                  ]}>
                  <Text
                    style={[
                      styles.cardHeaderTitle,
                      {color: colors.cardHeaderTitle},
                    ]}
                    numberOfLines={1}
                    adjustsFontSizeToFit={true}>
                    {t('NETWORK PERFORMANCE')}
                  </Text>
                  {renderNetworkPerformanceMetrics(true)}
                </View>

                <View
                  style={[
                    styles.metricCard,
                    {
                      backgroundColor: colors.subCardBg,
                      borderColor: colors.subCardBorder,
                    },
                  ]}>
                  <Text
                    style={[
                      styles.cardHeaderTitle,
                      {color: colors.cardHeaderTitle},
                    ]}
                    numberOfLines={1}
                    adjustsFontSizeToFit={true}>
                    {t('SESSION STABILITY')}
                  </Text>
                  {renderSessionStabilityMetrics(true)}
                </View>
              </View>

              <View
                style={[
                  styles.netInfoCard,
                  {
                    backgroundColor: colors.subCardBg,
                    borderColor: colors.subCardBorder,
                  },
                ]}>
                <Text
                  style={[
                    styles.netInfoCardTitle,
                    {color: colors.cardHeaderTitle},
                  ]}>
                  {t('Network Information')}
                </Text>

                <View style={styles.netInfoLine}>
                  <Text
                    style={[
                      styles.netInfoLabel,
                      {color: colors.netInfoLabel},
                    ]}>
                    {`${t('Network Type:')} `}
                  </Text>
                  <Text
                    style={[
                      styles.netInfoValue,
                      {color: colors.netInfoValue},
                    ]}>
                    {report.networkType || 'Wi-Fi (5 GHz)'}
                  </Text>
                </View>

                <View style={styles.netInfoLine}>
                  <Text
                    style={[
                      styles.netInfoLabel,
                      {color: colors.netInfoLabel},
                    ]}>
                    {`${t('VIDEO PROFILE:')} `}
                  </Text>
                  <Text
                    style={[
                      styles.netInfoValue,
                      {color: colors.netInfoValue},
                    ]}>
                    {cleanVideoProfile}
                  </Text>
                </View>
              </View>

              <View style={styles.footerContainer}>
                <Pressable
                  onPress={() => setDontShowAgain(!dontShowAgain)}
                  style={({pressed}) => [
                    styles.checkboxWrapper,
                    focusedBtn === 'checkbox' &&
                      (isGamepadActive || Platform.isTV) &&
                      styles.checkboxWrapperFocused,
                    pressed && {opacity: 0.7},
                  ]}>
                  <View
                    style={[
                      styles.checkboxBox,
                      {borderColor: colors.checkboxBorder},
                      dontShowAgain && styles.checkboxBoxChecked,
                    ]}>
                    {dontShowAgain && (
                      <Icon source="check" size={14} color="#FFFFFF" />
                    )}
                  </View>
                  <Text
                    style={[
                      styles.checkboxLabel,
                      {color: colors.checkboxLabel},
                    ]}>
                    {t("Don't show this again")}
                  </Text>
                </Pressable>

                <Pressable
                  onPress={handleDonePress}
                  style={({pressed}) => [
                    styles.donePillButton,
                    {
                      backgroundColor: colors.doneButtonBg,
                      borderColor: colors.doneButtonBorder,
                    },
                    focusedBtn === 'done' &&
                      (isGamepadActive || Platform.isTV) && [
                        styles.donePillButtonFocused,
                        {borderColor: colors.doneButtonFocusedBorder},
                      ],
                    pressed && styles.donePillButtonPressed,
                  ]}>
                  <Text
                    style={[
                      styles.donePillButtonText,
                      {color: colors.doneButtonText},
                    ]}>
                    {t('Done')}
                  </Text>
                </Pressable>

                {(isGamepadActive || Platform.isTV) && (
                  <View style={styles.modalGamepadHints}>
                    <Text
                      style={[
                        styles.modalGamepadHintText,
                        {color: colors.hintTextColor},
                      ]}>
                      <Text
                        style={[
                          styles.hintKeyBadge,
                          {
                            backgroundColor: colors.hintKeyBg,
                            color: colors.hintKeyText,
                          },
                        ]}>
                        A
                      </Text>{' '}
                      {focusedBtn === 'checkbox' ? t('Toggle') : t('Done')}{'   '}
                      <Text
                        style={[
                          styles.hintKeyBadge,
                          {
                            backgroundColor: colors.hintKeyBg,
                            color: colors.hintKeyText,
                          },
                        ]}>
                        B
                      </Text>{' '}
                      {t('Close')}
                    </Text>
                  </View>
                )}
              </View>
            </ScrollView>
          )}
        </View>
      </Modal>
    </Portal>
  );
};

const styles = StyleSheet.create({
  fullScreenModal: {
    margin: 0,
    padding: 0,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalOverlay: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 18,
    paddingVertical: 24,
  },
  cardContainer: {
    backgroundColor: '#0B142C',
    borderRadius: 22,
    borderWidth: 1.5,
    borderColor: 'rgba(0, 229, 255, 0.52)',
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: {width: 0, height: 16},
    shadowOpacity: 0.78,
    shadowRadius: 28,
    elevation: 24,
  },
  modalTopSpecular: {
    position: 'absolute',
    top: 0,
    left: 18,
    right: 18,
    height: 2,
    borderRadius: 2,
    zIndex: 10,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 14,
    paddingBottom: 16,
  },
  portraitModalHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingBottom: 10,
    marginBottom: 10,
    borderBottomWidth: 1,
  },
  modalHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    flex: 1,
  },
  modalHeaderBadgeIcon: {
    width: 34,
    height: 34,
    borderRadius: 10,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  modalEyebrowText: {
    fontSize: 9.5,
    fontWeight: '800',
    letterSpacing: 1.1,
    marginBottom: 1,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#FFFFFF',
    letterSpacing: 0.2,
  },
  modalCloseButton: {
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  heroCard: {
    position: 'relative',
    height: 82,
    borderRadius: 14,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(56, 189, 248, 0.24)',
    marginBottom: 8,
    backgroundColor: '#0F1E42',
  },
  heroPosterFallback: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#122552',
  },
  heroOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(9, 18, 42, 0.78)',
  },
  heroContent: {
    ...StyleSheet.absoluteFillObject,
    flexDirection: 'row',
    paddingVertical: 10,
    paddingHorizontal: 14,
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  heroTitleContainer: {
    flex: 1,
    marginRight: 10,
    alignItems: 'flex-start',
    justifyContent: 'center',
  },
  heroGameTitle: {
    fontSize: 14,
    fontStyle: 'italic',
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: 0.3,
    lineHeight: 18,
    marginBottom: 4,
  },
  gaugeWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  qualityRatingRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  qualityLabelPrefix: {
    fontSize: 10,
    fontWeight: '700',
    color: '#b0b8c6',
    letterSpacing: 0.5,
  },
  qualityLabelRating: {
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 0.5,
  },
  metricsTwoColumn: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 8,
  },
  metricCard: {
    flex: 1,
    backgroundColor: 'rgba(14, 27, 60, 0.88)',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: 'rgba(56, 189, 248, 0.24)',
    padding: 10,
  },
  cardHeaderTitle: {
    fontSize: 9.5,
    fontWeight: '800',
    color: '#38BDF8',
    letterSpacing: 0.5,
    textTransform: 'uppercase',
    marginBottom: 6,
    height: 14,
  },
  metricRowContainer: {
    paddingVertical: 4,
    marginBottom: 4,
    justifyContent: 'center',
  },
  metricLabelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 2,
  },
  metricLabelText: {
    fontSize: 10.5,
    color: '#8e98a8',
    fontWeight: '500',
    flexShrink: 1,
  },
  metricPrimaryValue: {
    fontSize: 14.5,
    fontWeight: '800',
    color: '#FFFFFF',
    letterSpacing: 0.2,
  },
  metricSplitRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 2,
  },
  metricSubInfoText: {
    fontSize: 9.5,
    color: '#717b8c',
    fontWeight: '500',
  },
  metricBadgeText: {
    fontSize: 9.5,
    fontWeight: '800',
    letterSpacing: 0.3,
  },
  metricSpacer: {
    height: 0,
  },
  miniFpsBadge: {
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.25)',
    borderRadius: 3,
    paddingHorizontal: 3,
    paddingVertical: 1,
  },
  miniFpsBadgeText: {
    fontSize: 8,
    fontWeight: '800',
    color: '#8e98a8',
  },
  netInfoCard: {
    backgroundColor: 'rgba(14, 27, 60, 0.88)',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: 'rgba(56, 189, 248, 0.24)',
    paddingHorizontal: 12,
    paddingVertical: 9,
    marginBottom: 10,
  },
  netInfoCardTitle: {
    fontSize: 10,
    fontWeight: '800',
    letterSpacing: 0.5,
    textTransform: 'uppercase',
    color: '#38BDF8',
    marginBottom: 4,
  },
  netInfoLine: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 3,
  },
  netInfoLabel: {
    fontSize: 11,
    color: '#8e98a8',
    fontWeight: '500',
  },
  netInfoValue: {
    fontSize: 11.5,
    color: '#FFFFFF',
    fontWeight: '700',
  },
  footerContainer: {
    gap: 10,
  },
  checkboxWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 2,
  },
  checkboxBox: {
    width: 18,
    height: 18,
    borderRadius: 4,
    borderWidth: 1.5,
    borderColor: '#606a7c',
    backgroundColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 8,
  },
  checkboxBoxChecked: {
    backgroundColor: '#16A34A',
    borderColor: '#4ADE80',
  },
  checkboxLabel: {
    fontSize: 12.5,
    color: '#8e98a8',
    fontWeight: '500',
  },
  donePillButton: {
    backgroundColor: '#16A34A',
    borderWidth: 1.2,
    borderColor: '#4ADE80',
    borderRadius: 14,
    height: 44,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#22C55E',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.35,
    shadowRadius: 8,
    elevation: 5,
  },
  donePillButtonPressed: {
    backgroundColor: '#15803D',
    opacity: 0.9,
  },
  donePillButtonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '800',
    letterSpacing: 0.3,
  },
  checkboxWrapperFocused: {
    borderWidth: 2,
    borderColor: '#4ADE80',
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    backgroundColor: 'rgba(34, 197, 94, 0.15)',
  },
  donePillButtonFocused: {
    borderWidth: 2.5,
    borderColor: '#FFFFFF',
    transform: [{scale: 1.03}],
    elevation: 8,
    shadowColor: '#FFFFFF',
    shadowOpacity: 0.7,
  },
  modalGamepadHints: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 2,
  },
  modalGamepadHintText: {
    fontSize: 12,
    color: '#8e98a8',
    fontWeight: '600',
  },
  hintKeyBadge: {
    fontSize: 11,
    fontWeight: '900',
    color: '#FFFFFF',
    backgroundColor: 'rgba(255, 255, 255, 0.18)',
  },

  /* ------------------------------------------------------------------------- */
  /* Widescreen / 16:9 Android TV Dashboard Layout Styles                      */
  /* ------------------------------------------------------------------------- */
  wideScrollContent: {
    paddingHorizontal: 18,
    paddingTop: 12,
    paddingBottom: 12,
  },
  wideHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingBottom: 8,
    marginBottom: 10,
    borderBottomWidth: 1,
  },
  headerTitleWide: {
    fontSize: 17,
    fontWeight: '800',
    letterSpacing: 0.2,
  },
  wideHeaderRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  qualityRatingRowWide: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  scorePillBadge: {
    paddingHorizontal: 8,
    paddingVertical: 2.5,
    borderRadius: 12,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scorePillText: {
    fontSize: 11.5,
    fontWeight: '800',
    letterSpacing: 0.4,
  },
  wideColumnsRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 10,
    alignItems: 'stretch',
  },
  wideColLeft: {
    flex: 1,
    flexDirection: 'column',
    gap: 8,
  },
  wideHeroCard: {
    position: 'relative',
    height: 120,
    borderRadius: 14,
    overflow: 'hidden',
    borderWidth: 1,
  },
  wideHeroContent: {
    ...StyleSheet.absoluteFillObject,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  heroGameTitleWide: {
    flex: 1,
    fontSize: 12,
    fontStyle: 'italic',
    fontWeight: '900',
    color: '#FFFFFF',
    letterSpacing: 0.3,
    marginRight: 6,
  },
  gaugeWrapperCompact: {
    width: 54,
    height: 54,
    alignItems: 'center',
    justifyContent: 'center',
  },
  wideNetInfoCard: {
    flex: 1,
    borderRadius: 14,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 8,
    justifyContent: 'center',
  },
  wideMetricCard: {
    flex: 1.15,
    borderRadius: 14,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingTop: 8,
    paddingBottom: 8,
    justifyContent: 'space-between',
  },
  metricRowContainerCompact: {
    paddingVertical: 3,
    marginBottom: 3,
    justifyContent: 'center',
  },
  metricBadgeRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  wideFooterRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingTop: 2,
  },
  wideFooterRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
  },
  modalGamepadHintsWide: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  donePillButtonWide: {
    paddingHorizontal: 22,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
    shadowOffset: {width: 0, height: 2},
    shadowOpacity: 0.3,
    shadowRadius: 6,
    elevation: 4,
  },
});

export default SessionReportModal;
