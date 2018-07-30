package android.icu.text;

import android.icu.impl.ICUData;
import android.icu.impl.ICUResourceBundle;
import android.icu.impl.PatternTokenizer;
import android.icu.impl.SoftCache;
import android.icu.impl.TZDBTimeZoneNames;
import android.icu.impl.TextTrieMap;
import android.icu.impl.TimeZoneGenericNames;
import android.icu.impl.TimeZoneGenericNames.GenericNameType;
import android.icu.impl.TimeZoneNamesImpl;
import android.icu.impl.ZoneMeta;
import android.icu.lang.UCharacter;
import android.icu.text.DateFormat.Field;
import android.icu.text.TimeZoneNames.MatchInfo;
import android.icu.text.TimeZoneNames.NameType;
import android.icu.util.Calendar;
import android.icu.util.Freezable;
import android.icu.util.Output;
import android.icu.util.TimeZone;
import android.icu.util.TimeZone.SystemTimeZoneType;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

public class TimeZoneFormat extends UFormat implements Freezable<TimeZoneFormat>, Serializable {
    private static final /* synthetic */ int[] -android-icu-text-TimeZoneFormat$StyleSwitchesValues = null;
    private static final /* synthetic */ int[] -android-icu-text-TimeZoneNames$NameTypeSwitchesValues = null;
    static final /* synthetic */ boolean -assertionsDisabled = (TimeZoneFormat.class.desiredAssertionStatus() ^ 1);
    private static final EnumSet<GenericNameType> ALL_GENERIC_NAME_TYPES = EnumSet.of(GenericNameType.LOCATION, GenericNameType.LONG, GenericNameType.SHORT);
    private static final EnumSet<NameType> ALL_SIMPLE_NAME_TYPES = EnumSet.of(NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT, NameType.EXEMPLAR_LOCATION);
    private static final String[] ALT_GMT_STRINGS = new String[]{DEFAULT_GMT_ZERO, "UTC", "UT"};
    private static final String ASCII_DIGITS = "0123456789";
    private static final String[] DEFAULT_GMT_DIGITS = new String[]{AndroidHardcodedSystemProperties.JAVA_VERSION, "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final char DEFAULT_GMT_OFFSET_SEP = ':';
    private static final String DEFAULT_GMT_PATTERN = "GMT{0}";
    private static final String DEFAULT_GMT_ZERO = "GMT";
    private static final String ISO8601_UTC = "Z";
    private static final int ISO_LOCAL_STYLE_FLAG = 256;
    private static final int ISO_Z_STYLE_FLAG = 128;
    private static final int MAX_OFFSET = 86400000;
    private static final int MAX_OFFSET_HOUR = 23;
    private static final int MAX_OFFSET_MINUTE = 59;
    private static final int MAX_OFFSET_SECOND = 59;
    private static final int MILLIS_PER_HOUR = 3600000;
    private static final int MILLIS_PER_MINUTE = 60000;
    private static final int MILLIS_PER_SECOND = 1000;
    private static final GMTOffsetPatternType[] PARSE_GMT_OFFSET_TYPES = new GMTOffsetPatternType[]{GMTOffsetPatternType.POSITIVE_HMS, GMTOffsetPatternType.NEGATIVE_HMS, GMTOffsetPatternType.POSITIVE_HM, GMTOffsetPatternType.NEGATIVE_HM, GMTOffsetPatternType.POSITIVE_H, GMTOffsetPatternType.NEGATIVE_H};
    private static volatile TextTrieMap<String> SHORT_ZONE_ID_TRIE = null;
    private static final String TZID_GMT = "Etc/GMT";
    private static final String UNKNOWN_LOCATION = "Unknown";
    private static final int UNKNOWN_OFFSET = Integer.MAX_VALUE;
    private static final String UNKNOWN_SHORT_ZONE_ID = "unk";
    private static final String UNKNOWN_ZONE_ID = "Etc/Unknown";
    private static volatile TextTrieMap<String> ZONE_ID_TRIE = null;
    private static TimeZoneFormatCache _tzfCache = new TimeZoneFormatCache();
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("_locale", ULocale.class), new ObjectStreamField("_tznames", TimeZoneNames.class), new ObjectStreamField("_gmtPattern", String.class), new ObjectStreamField("_gmtOffsetPatterns", String[].class), new ObjectStreamField("_gmtOffsetDigits", String[].class), new ObjectStreamField("_gmtZeroFormat", String.class), new ObjectStreamField("_parseAllStyles", Boolean.TYPE)};
    private static final long serialVersionUID = 2281246852693575022L;
    private transient boolean _abuttingOffsetHoursAndMinutes;
    private volatile transient boolean _frozen;
    private String[] _gmtOffsetDigits;
    private transient Object[][] _gmtOffsetPatternItems;
    private String[] _gmtOffsetPatterns;
    private String _gmtPattern;
    private transient String _gmtPatternPrefix;
    private transient String _gmtPatternSuffix;
    private String _gmtZeroFormat = DEFAULT_GMT_ZERO;
    private volatile transient TimeZoneGenericNames _gnames;
    private ULocale _locale;
    private boolean _parseAllStyles;
    private boolean _parseTZDBNames;
    private transient String _region;
    private volatile transient TimeZoneNames _tzdbNames;
    private TimeZoneNames _tznames;

    private static class GMTOffsetField {
        final char _type;
        final int _width;

        GMTOffsetField(char type, int width) {
            this._type = type;
            this._width = width;
        }

        char getType() {
            return this._type;
        }

        int getWidth() {
            return this._width;
        }

        static boolean isValid(char type, int width) {
            return width == 1 || width == 2;
        }
    }

    public enum GMTOffsetPatternType {
        POSITIVE_HM("+H:mm", DateFormat.HOUR24_MINUTE, true),
        POSITIVE_HMS("+H:mm:ss", DateFormat.HOUR24_MINUTE_SECOND, true),
        NEGATIVE_HM("-H:mm", DateFormat.HOUR24_MINUTE, false),
        NEGATIVE_HMS("-H:mm:ss", DateFormat.HOUR24_MINUTE_SECOND, false),
        POSITIVE_H("+H", DateFormat.HOUR24, true),
        NEGATIVE_H("-H", DateFormat.HOUR24, false);
        
        private String _defaultPattern;
        private boolean _isPositive;
        private String _required;

        private GMTOffsetPatternType(String defaultPattern, String required, boolean isPositive) {
            this._defaultPattern = defaultPattern;
            this._required = required;
            this._isPositive = isPositive;
        }

        private String defaultPattern() {
            return this._defaultPattern;
        }

        private String required() {
            return this._required;
        }

        private boolean isPositive() {
            return this._isPositive;
        }
    }

    private enum OffsetFields {
        H,
        HM,
        HMS
    }

    public enum ParseOption {
        ALL_STYLES,
        TZ_DATABASE_ABBREVIATIONS
    }

    public enum Style {
        GENERIC_LOCATION(1),
        GENERIC_LONG(2),
        GENERIC_SHORT(4),
        SPECIFIC_LONG(8),
        SPECIFIC_SHORT(16),
        LOCALIZED_GMT(32),
        LOCALIZED_GMT_SHORT(64),
        ISO_BASIC_SHORT(128),
        ISO_BASIC_LOCAL_SHORT(256),
        ISO_BASIC_FIXED(128),
        ISO_BASIC_LOCAL_FIXED(256),
        ISO_BASIC_FULL(128),
        ISO_BASIC_LOCAL_FULL(256),
        ISO_EXTENDED_FIXED(128),
        ISO_EXTENDED_LOCAL_FIXED(256),
        ISO_EXTENDED_FULL(128),
        ISO_EXTENDED_LOCAL_FULL(256),
        ZONE_ID(512),
        ZONE_ID_SHORT(1024),
        EXEMPLAR_LOCATION(2048);
        
        final int flag;

        private Style(int flag) {
            this.flag = flag;
        }
    }

    public enum TimeType {
        UNKNOWN,
        STANDARD,
        DAYLIGHT
    }

    private static class TimeZoneFormatCache extends SoftCache<ULocale, TimeZoneFormat, ULocale> {
        /* synthetic */ TimeZoneFormatCache(TimeZoneFormatCache -this0) {
            this();
        }

        private TimeZoneFormatCache() {
        }

        protected TimeZoneFormat createInstance(ULocale key, ULocale data) {
            TimeZoneFormat fmt = new TimeZoneFormat(data);
            fmt.freeze();
            return fmt;
        }
    }

    private static /* synthetic */ int[] -getandroid-icu-text-TimeZoneFormat$StyleSwitchesValues() {
        if (-android-icu-text-TimeZoneFormat$StyleSwitchesValues != null) {
            return -android-icu-text-TimeZoneFormat$StyleSwitchesValues;
        }
        int[] iArr = new int[Style.values().length];
        try {
            iArr[Style.EXEMPLAR_LOCATION.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Style.GENERIC_LOCATION.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Style.GENERIC_LONG.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Style.GENERIC_SHORT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[Style.ISO_BASIC_FIXED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[Style.ISO_BASIC_FULL.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[Style.ISO_BASIC_LOCAL_FIXED.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[Style.ISO_BASIC_LOCAL_FULL.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[Style.ISO_BASIC_LOCAL_SHORT.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[Style.ISO_BASIC_SHORT.ordinal()] = 10;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[Style.ISO_EXTENDED_FIXED.ordinal()] = 11;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[Style.ISO_EXTENDED_FULL.ordinal()] = 12;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[Style.ISO_EXTENDED_LOCAL_FIXED.ordinal()] = 13;
        } catch (NoSuchFieldError e13) {
        }
        try {
            iArr[Style.ISO_EXTENDED_LOCAL_FULL.ordinal()] = 14;
        } catch (NoSuchFieldError e14) {
        }
        try {
            iArr[Style.LOCALIZED_GMT.ordinal()] = 15;
        } catch (NoSuchFieldError e15) {
        }
        try {
            iArr[Style.LOCALIZED_GMT_SHORT.ordinal()] = 16;
        } catch (NoSuchFieldError e16) {
        }
        try {
            iArr[Style.SPECIFIC_LONG.ordinal()] = 17;
        } catch (NoSuchFieldError e17) {
        }
        try {
            iArr[Style.SPECIFIC_SHORT.ordinal()] = 18;
        } catch (NoSuchFieldError e18) {
        }
        try {
            iArr[Style.ZONE_ID.ordinal()] = 19;
        } catch (NoSuchFieldError e19) {
        }
        try {
            iArr[Style.ZONE_ID_SHORT.ordinal()] = 20;
        } catch (NoSuchFieldError e20) {
        }
        -android-icu-text-TimeZoneFormat$StyleSwitchesValues = iArr;
        return iArr;
    }

    private static /* synthetic */ int[] -getandroid-icu-text-TimeZoneNames$NameTypeSwitchesValues() {
        if (-android-icu-text-TimeZoneNames$NameTypeSwitchesValues != null) {
            return -android-icu-text-TimeZoneNames$NameTypeSwitchesValues;
        }
        int[] iArr = new int[NameType.values().length];
        try {
            iArr[NameType.EXEMPLAR_LOCATION.ordinal()] = 25;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[NameType.LONG_DAYLIGHT.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[NameType.LONG_GENERIC.ordinal()] = 26;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[NameType.LONG_STANDARD.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[NameType.SHORT_DAYLIGHT.ordinal()] = 3;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[NameType.SHORT_GENERIC.ordinal()] = 27;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[NameType.SHORT_STANDARD.ordinal()] = 4;
        } catch (NoSuchFieldError e7) {
        }
        -android-icu-text-TimeZoneNames$NameTypeSwitchesValues = iArr;
        return iArr;
    }

    protected TimeZoneFormat(ULocale locale) {
        int i = 0;
        this._locale = locale;
        this._tznames = TimeZoneNames.getInstance(locale);
        String gmtPattern = null;
        String hourFormats = null;
        try {
            ICUResourceBundle bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_ZONE_BASE_NAME, locale);
            try {
                gmtPattern = bundle.getStringWithFallback("zoneStrings/gmtFormat");
            } catch (MissingResourceException e) {
            }
            try {
                hourFormats = bundle.getStringWithFallback("zoneStrings/hourFormat");
            } catch (MissingResourceException e2) {
            }
            try {
                this._gmtZeroFormat = bundle.getStringWithFallback("zoneStrings/gmtZeroFormat");
            } catch (MissingResourceException e3) {
            }
        } catch (MissingResourceException e4) {
        }
        if (gmtPattern == null) {
            gmtPattern = DEFAULT_GMT_PATTERN;
        }
        initGMTPattern(gmtPattern);
        String[] gmtOffsetPatterns = new String[GMTOffsetPatternType.values().length];
        if (hourFormats != null) {
            String[] hourPatterns = hourFormats.split(";", 2);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()] = hourPatterns[0];
            gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[0]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(hourPatterns[1]);
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()] = hourPatterns[1];
            gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()] = expandOffsetPattern(hourPatterns[1]);
        } else {
            GMTOffsetPatternType[] values = GMTOffsetPatternType.values();
            int length = values.length;
            while (i < length) {
                GMTOffsetPatternType patType = values[i];
                gmtOffsetPatterns[patType.ordinal()] = patType.defaultPattern();
                i++;
            }
        }
        initGMTOffsetPatterns(gmtOffsetPatterns);
        this._gmtOffsetDigits = DEFAULT_GMT_DIGITS;
        NumberingSystem ns = NumberingSystem.getInstance(locale);
        if (!ns.isAlgorithmic()) {
            this._gmtOffsetDigits = toCodePoints(ns.getDescription());
        }
    }

    public static TimeZoneFormat getInstance(ULocale locale) {
        if (locale != null) {
            return (TimeZoneFormat) _tzfCache.getInstance(locale, locale);
        }
        throw new NullPointerException("locale is null");
    }

    public static TimeZoneFormat getInstance(Locale locale) {
        return getInstance(ULocale.forLocale(locale));
    }

    public TimeZoneNames getTimeZoneNames() {
        return this._tznames;
    }

    private TimeZoneGenericNames getTimeZoneGenericNames() {
        if (this._gnames == null) {
            synchronized (this) {
                if (this._gnames == null) {
                    this._gnames = TimeZoneGenericNames.getInstance(this._locale);
                }
            }
        }
        return this._gnames;
    }

    private TimeZoneNames getTZDBTimeZoneNames() {
        if (this._tzdbNames == null) {
            synchronized (this) {
                if (this._tzdbNames == null) {
                    this._tzdbNames = new TZDBTimeZoneNames(this._locale);
                }
            }
        }
        return this._tzdbNames;
    }

    public TimeZoneFormat setTimeZoneNames(TimeZoneNames tznames) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        this._tznames = tznames;
        this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
        return this;
    }

    public String getGMTPattern() {
        return this._gmtPattern;
    }

    public TimeZoneFormat setGMTPattern(String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
        initGMTPattern(pattern);
        return this;
    }

    public String getGMTOffsetPattern(GMTOffsetPatternType type) {
        return this._gmtOffsetPatterns[type.ordinal()];
    }

    public TimeZoneFormat setGMTOffsetPattern(GMTOffsetPatternType type, String pattern) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        } else if (pattern == null) {
            throw new NullPointerException("Null GMT offset pattern");
        } else {
            Object[] parsedItems = parseOffsetPattern(pattern, type.required());
            this._gmtOffsetPatterns[type.ordinal()] = pattern;
            this._gmtOffsetPatternItems[type.ordinal()] = parsedItems;
            checkAbuttingHoursAndMinutes();
            return this;
        }
    }

    public String getGMTOffsetDigits() {
        StringBuilder buf = new StringBuilder(this._gmtOffsetDigits.length);
        for (String digit : this._gmtOffsetDigits) {
            buf.append(digit);
        }
        return buf.toString();
    }

    public TimeZoneFormat setGMTOffsetDigits(String digits) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        } else if (digits == null) {
            throw new NullPointerException("Null GMT offset digits");
        } else {
            String[] digitArray = toCodePoints(digits);
            if (digitArray.length != 10) {
                throw new IllegalArgumentException("Length of digits must be 10");
            }
            this._gmtOffsetDigits = digitArray;
            return this;
        }
    }

    public String getGMTZeroFormat() {
        return this._gmtZeroFormat;
    }

    public TimeZoneFormat setGMTZeroFormat(String gmtZeroFormat) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        } else if (gmtZeroFormat == null) {
            throw new NullPointerException("Null GMT zero format");
        } else if (gmtZeroFormat.length() == 0) {
            throw new IllegalArgumentException("Empty GMT zero format");
        } else {
            this._gmtZeroFormat = gmtZeroFormat;
            return this;
        }
    }

    public TimeZoneFormat setDefaultParseOptions(EnumSet<ParseOption> options) {
        this._parseAllStyles = options.contains(ParseOption.ALL_STYLES);
        this._parseTZDBNames = options.contains(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        return this;
    }

    public EnumSet<ParseOption> getDefaultParseOptions() {
        if (this._parseAllStyles && this._parseTZDBNames) {
            return EnumSet.of(ParseOption.ALL_STYLES, ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        if (this._parseAllStyles) {
            return EnumSet.of(ParseOption.ALL_STYLES);
        }
        if (this._parseTZDBNames) {
            return EnumSet.of(ParseOption.TZ_DATABASE_ABBREVIATIONS);
        }
        return EnumSet.noneOf(ParseOption.class);
    }

    public final String formatOffsetISO8601Basic(int offset, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        return formatOffsetISO8601(offset, true, useUtcIndicator, isShort, ignoreSeconds);
    }

    public final String formatOffsetISO8601Extended(int offset, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        return formatOffsetISO8601(offset, false, useUtcIndicator, isShort, ignoreSeconds);
    }

    public String formatOffsetLocalizedGMT(int offset) {
        return formatOffsetLocalizedGMT(offset, false);
    }

    public String formatOffsetShortLocalizedGMT(int offset) {
        return formatOffsetLocalizedGMT(offset, true);
    }

    public final String format(Style style, TimeZone tz, long date) {
        return format(style, tz, date, null);
    }

    public String format(Style style, TimeZone tz, long date, Output<TimeType> timeType) {
        String result = null;
        if (timeType != null) {
            timeType.value = TimeType.UNKNOWN;
        }
        boolean noOffsetFormatFallback = false;
        switch (-getandroid-icu-text-TimeZoneFormat$StyleSwitchesValues()[style.ordinal()]) {
            case 1:
                result = formatExemplarLocation(tz);
                noOffsetFormatFallback = true;
                break;
            case 2:
                result = getTimeZoneGenericNames().getGenericLocationName(ZoneMeta.getCanonicalCLDRID(tz));
                break;
            case 3:
                result = getTimeZoneGenericNames().getDisplayName(tz, GenericNameType.LONG, date);
                break;
            case 4:
                result = getTimeZoneGenericNames().getDisplayName(tz, GenericNameType.SHORT, date);
                break;
            case 17:
                result = formatSpecific(tz, NameType.LONG_STANDARD, NameType.LONG_DAYLIGHT, date, timeType);
                break;
            case 18:
                result = formatSpecific(tz, NameType.SHORT_STANDARD, NameType.SHORT_DAYLIGHT, date, timeType);
                break;
            case 19:
                result = tz.getID();
                noOffsetFormatFallback = true;
                break;
            case 20:
                result = ZoneMeta.getShortID(tz);
                if (result == null) {
                    result = UNKNOWN_SHORT_ZONE_ID;
                }
                noOffsetFormatFallback = true;
                break;
        }
        if (result == null && (noOffsetFormatFallback ^ 1) != 0) {
            int[] offsets = new int[]{0, 0};
            tz.getOffset(date, false, offsets);
            int offset = offsets[0] + offsets[1];
            switch (-getandroid-icu-text-TimeZoneFormat$StyleSwitchesValues()[style.ordinal()]) {
                case 2:
                case 3:
                case 15:
                case 17:
                    result = formatOffsetLocalizedGMT(offset);
                    break;
                case 4:
                case 16:
                case 18:
                    result = formatOffsetShortLocalizedGMT(offset);
                    break;
                case 5:
                    result = formatOffsetISO8601Basic(offset, true, false, true);
                    break;
                case 6:
                    result = formatOffsetISO8601Basic(offset, true, false, false);
                    break;
                case 7:
                    result = formatOffsetISO8601Basic(offset, false, false, true);
                    break;
                case 8:
                    result = formatOffsetISO8601Basic(offset, false, false, false);
                    break;
                case 9:
                    result = formatOffsetISO8601Basic(offset, false, true, true);
                    break;
                case 10:
                    result = formatOffsetISO8601Basic(offset, true, true, true);
                    break;
                case 11:
                    result = formatOffsetISO8601Extended(offset, true, false, true);
                    break;
                case 12:
                    result = formatOffsetISO8601Extended(offset, true, false, false);
                    break;
                case 13:
                    result = formatOffsetISO8601Extended(offset, false, false, true);
                    break;
                case 14:
                    result = formatOffsetISO8601Extended(offset, false, false, false);
                    break;
                default:
                    if (!-assertionsDisabled) {
                        throw new AssertionError();
                    }
                    break;
            }
            if (timeType != null) {
                timeType.value = offsets[1] != 0 ? TimeType.DAYLIGHT : TimeType.STANDARD;
            }
        }
        if (-assertionsDisabled || result != null) {
            return result;
        }
        throw new AssertionError();
    }

    public final int parseOffsetISO8601(String text, ParsePosition pos) {
        return parseOffsetISO8601(text, pos, false, null);
    }

    public int parseOffsetLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, false, null);
    }

    public int parseOffsetShortLocalizedGMT(String text, ParsePosition pos) {
        return parseOffsetLocalizedGMT(text, pos, true, null);
    }

    public android.icu.util.TimeZone parse(android.icu.text.TimeZoneFormat.Style r34, java.lang.String r35, java.text.ParsePosition r36, java.util.EnumSet<android.icu.text.TimeZoneFormat.ParseOption> r37, android.icu.util.Output<android.icu.text.TimeZoneFormat.TimeType> r38) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Unknown predecessor block by arg (r38_1 'timeType' android.icu.util.Output<android.icu.text.TimeZoneFormat$TimeType>) in PHI: PHI: (r38_2 'timeType' android.icu.util.Output<android.icu.text.TimeZoneFormat$TimeType>) = (r38_1 'timeType' android.icu.util.Output<android.icu.text.TimeZoneFormat$TimeType>), (r38_0 'timeType' android.icu.util.Output<android.icu.text.TimeZoneFormat$TimeType>) binds: {(r38_1 'timeType' android.icu.util.Output<android.icu.text.TimeZoneFormat$TimeType>)=B:1:0x0002, (r38_0 'timeType' android.icu.util.Output<android.icu.text.TimeZoneFormat$TimeType>)=B:23:0x0095}
	at jadx.core.dex.instructions.PhiInsn.replaceArg(PhiInsn.java:78)
	at jadx.core.dex.visitors.ModVisitor.processInvoke(ModVisitor.java:222)
	at jadx.core.dex.visitors.ModVisitor.replaceStep(ModVisitor.java:83)
	at jadx.core.dex.visitors.ModVisitor.visit(ModVisitor.java:68)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
	at java.util.ArrayList.forEach(ArrayList.java:1251)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:32)
	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
	at java.lang.Iterable.forEach(Iterable.java:75)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:286)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:201)
*/
        /*
        r33 = this;
        if (r38 != 0) goto L_0x0095;
    L_0x0002:
        r38 = new android.icu.util.Output;
        r31 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r0 = r38;
        r1 = r31;
        r0.<init>(r1);
    L_0x000d:
        r27 = r36.getIndex();
        r15 = r35.length();
        r31 = android.icu.text.TimeZoneFormat.Style.SPECIFIC_LONG;
        r0 = r34;
        r1 = r31;
        if (r0 == r1) goto L_0x0025;
    L_0x001d:
        r31 = android.icu.text.TimeZoneFormat.Style.GENERIC_LONG;
        r0 = r34;
        r1 = r31;
        if (r0 != r1) goto L_0x009f;
    L_0x0025:
        r6 = 1;
    L_0x0026:
        r31 = android.icu.text.TimeZoneFormat.Style.SPECIFIC_SHORT;
        r0 = r34;
        r1 = r31;
        if (r0 == r1) goto L_0x0036;
    L_0x002e:
        r31 = android.icu.text.TimeZoneFormat.Style.GENERIC_SHORT;
        r0 = r34;
        r1 = r31;
        if (r0 != r1) goto L_0x00aa;
    L_0x0036:
        r7 = 1;
    L_0x0037:
        r5 = 0;
        r28 = new java.text.ParsePosition;
        r0 = r28;
        r1 = r27;
        r0.<init>(r1);
        r21 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r22 = -1;
        if (r6 != 0) goto L_0x004a;
    L_0x0048:
        if (r7 == 0) goto L_0x00c6;
    L_0x004a:
        r10 = new android.icu.util.Output;
        r31 = 0;
        r31 = java.lang.Boolean.valueOf(r31);
        r0 = r31;
        r10.<init>(r0);
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r17 = r0.parseOffsetLocalizedGMT(r1, r2, r7, r10);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x00b2;
    L_0x006d:
        r31 = r28.getIndex();
        r0 = r31;
        if (r0 == r15) goto L_0x0081;
    L_0x0075:
        r0 = r10.value;
        r31 = r0;
        r31 = (java.lang.Boolean) r31;
        r31 = r31.booleanValue();
        if (r31 == 0) goto L_0x00ac;
    L_0x0081:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x0095:
        r31 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r0 = r31;
        r1 = r38;
        r1.value = r0;
        goto L_0x000d;
    L_0x009f:
        r31 = android.icu.text.TimeZoneFormat.Style.GENERIC_LOCATION;
        r0 = r34;
        r1 = r31;
        if (r0 == r1) goto L_0x0025;
    L_0x00a7:
        r6 = 0;
        goto L_0x0026;
    L_0x00aa:
        r7 = 0;
        goto L_0x0037;
    L_0x00ac:
        r21 = r17;
        r22 = r28.getIndex();
    L_0x00b2:
        r31 = android.icu.text.TimeZoneFormat.Style.LOCALIZED_GMT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r32 = android.icu.text.TimeZoneFormat.Style.LOCALIZED_GMT_SHORT;
        r0 = r32;
        r0 = r0.flag;
        r32 = r0;
        r31 = r31 | r32;
        r5 = r31 | 0;
    L_0x00c6:
        if (r37 != 0) goto L_0x0100;
    L_0x00c8:
        r31 = r33.getDefaultParseOptions();
        r32 = android.icu.text.TimeZoneFormat.ParseOption.TZ_DATABASE_ABBREVIATIONS;
        r19 = r31.contains(r32);
    L_0x00d2:
        r31 = -getandroid-icu-text-TimeZoneFormat$StyleSwitchesValues();
        r32 = r34.ordinal();
        r31 = r31[r32];
        switch(r31) {
            case 1: goto L_0x0434;
            case 2: goto L_0x0360;
            case 3: goto L_0x0360;
            case 4: goto L_0x0360;
            case 5: goto L_0x0196;
            case 6: goto L_0x0196;
            case 7: goto L_0x01d0;
            case 8: goto L_0x01d0;
            case 9: goto L_0x01d0;
            case 10: goto L_0x0196;
            case 11: goto L_0x0196;
            case 12: goto L_0x0196;
            case 13: goto L_0x01d0;
            case 14: goto L_0x01d0;
            case 15: goto L_0x010b;
            case 16: goto L_0x0150;
            case 17: goto L_0x0225;
            case 18: goto L_0x0225;
            case 19: goto L_0x03cc;
            case 20: goto L_0x0400;
            default: goto L_0x00df;
        };
    L_0x00df:
        r0 = r34;
        r0 = r0.flag;
        r31 = r0;
        r5 = r5 | r31;
        r0 = r22;
        r1 = r27;
        if (r0 <= r1) goto L_0x047a;
    L_0x00ed:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x046a;
    L_0x00f1:
        r31 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r0 = r21;
        r1 = r31;
        if (r0 != r1) goto L_0x046a;
    L_0x00fa:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x0100:
        r31 = android.icu.text.TimeZoneFormat.ParseOption.TZ_DATABASE_ABBREVIATIONS;
        r0 = r37;
        r1 = r31;
        r19 = r0.contains(r1);
        goto L_0x00d2;
    L_0x010b:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r17 = r0.parseOffsetLocalizedGMT(r1, r2);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x0145;
    L_0x0131:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x0145:
        r31 = android.icu.text.TimeZoneFormat.Style.LOCALIZED_GMT_SHORT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r5 = r5 | r31;
        goto L_0x00df;
    L_0x0150:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r17 = r0.parseOffsetShortLocalizedGMT(r1, r2);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x018a;
    L_0x0176:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x018a:
        r31 = android.icu.text.TimeZoneFormat.Style.LOCALIZED_GMT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r5 = r5 | r31;
        goto L_0x00df;
    L_0x0196:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r17 = r0.parseOffsetISO8601(r1, r2);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x00df;
    L_0x01bc:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x01d0:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r10 = new android.icu.util.Output;
        r31 = 0;
        r31 = java.lang.Boolean.valueOf(r31);
        r0 = r31;
        r10.<init>(r0);
        r31 = 0;
        r0 = r35;
        r1 = r28;
        r2 = r31;
        r17 = parseOffsetISO8601(r0, r1, r2, r10);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x00df;
    L_0x0205:
        r0 = r10.value;
        r31 = r0;
        r31 = (java.lang.Boolean) r31;
        r31 = r31.booleanValue();
        if (r31 == 0) goto L_0x00df;
    L_0x0211:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x0225:
        r16 = 0;
        r31 = android.icu.text.TimeZoneFormat.Style.SPECIFIC_LONG;
        r0 = r34;
        r1 = r31;
        if (r0 != r1) goto L_0x0272;
    L_0x022f:
        r31 = android.icu.text.TimeZoneNames.NameType.LONG_STANDARD;
        r32 = android.icu.text.TimeZoneNames.NameType.LONG_DAYLIGHT;
        r16 = java.util.EnumSet.of(r31, r32);
    L_0x0237:
        r0 = r33;
        r0 = r0._tznames;
        r31 = r0;
        r0 = r31;
        r1 = r35;
        r2 = r27;
        r3 = r16;
        r26 = r0.find(r1, r2, r3);
        if (r26 == 0) goto L_0x02bf;
    L_0x024b:
        r25 = 0;
        r13 = r26.iterator();
    L_0x0251:
        r31 = r13.hasNext();
        if (r31 == 0) goto L_0x028d;
    L_0x0257:
        r12 = r13.next();
        r12 = (android.icu.text.TimeZoneNames.MatchInfo) r12;
        r31 = r12.matchLength();
        r31 = r31 + r27;
        r0 = r31;
        r1 = r22;
        if (r0 <= r1) goto L_0x0251;
    L_0x0269:
        r25 = r12;
        r31 = r12.matchLength();
        r22 = r27 + r31;
        goto L_0x0251;
    L_0x0272:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x0284;
    L_0x0276:
        r31 = android.icu.text.TimeZoneFormat.Style.SPECIFIC_SHORT;
        r0 = r34;
        r1 = r31;
        if (r0 == r1) goto L_0x0284;
    L_0x027e:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x0284:
        r31 = android.icu.text.TimeZoneNames.NameType.SHORT_STANDARD;
        r32 = android.icu.text.TimeZoneNames.NameType.SHORT_DAYLIGHT;
        r16 = java.util.EnumSet.of(r31, r32);
        goto L_0x0237;
    L_0x028d:
        if (r25 == 0) goto L_0x02bf;
    L_0x028f:
        r31 = r25.nameType();
        r0 = r33;
        r1 = r31;
        r31 = r0.getTimeType(r1);
        r0 = r31;
        r1 = r38;
        r1.value = r0;
        r0 = r36;
        r1 = r22;
        r0.setIndex(r1);
        r31 = r25.tzID();
        r32 = r25.mzID();
        r0 = r33;
        r1 = r31;
        r2 = r32;
        r31 = r0.getTimeZoneID(r1, r2);
        r31 = android.icu.util.TimeZone.getTimeZone(r31);
        return r31;
    L_0x02bf:
        if (r19 == 0) goto L_0x00df;
    L_0x02c1:
        r31 = android.icu.text.TimeZoneFormat.Style.SPECIFIC_SHORT;
        r0 = r34;
        r1 = r31;
        if (r0 != r1) goto L_0x00df;
    L_0x02c9:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x02df;
    L_0x02cd:
        r31 = android.icu.text.TimeZoneNames.NameType.SHORT_STANDARD;
        r0 = r16;
        r1 = r31;
        r31 = r0.contains(r1);
        if (r31 != 0) goto L_0x02df;
    L_0x02d9:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x02df:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x02f5;
    L_0x02e3:
        r31 = android.icu.text.TimeZoneNames.NameType.SHORT_DAYLIGHT;
        r0 = r16;
        r1 = r31;
        r31 = r0.contains(r1);
        if (r31 != 0) goto L_0x02f5;
    L_0x02ef:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x02f5:
        r31 = r33.getTZDBTimeZoneNames();
        r0 = r31;
        r1 = r35;
        r2 = r27;
        r3 = r16;
        r30 = r0.find(r1, r2, r3);
        if (r30 == 0) goto L_0x00df;
    L_0x0307:
        r29 = 0;
        r13 = r30.iterator();
    L_0x030d:
        r31 = r13.hasNext();
        if (r31 == 0) goto L_0x032e;
    L_0x0313:
        r12 = r13.next();
        r12 = (android.icu.text.TimeZoneNames.MatchInfo) r12;
        r31 = r12.matchLength();
        r31 = r31 + r27;
        r0 = r31;
        r1 = r22;
        if (r0 <= r1) goto L_0x030d;
    L_0x0325:
        r29 = r12;
        r31 = r12.matchLength();
        r22 = r27 + r31;
        goto L_0x030d;
    L_0x032e:
        if (r29 == 0) goto L_0x00df;
    L_0x0330:
        r31 = r29.nameType();
        r0 = r33;
        r1 = r31;
        r31 = r0.getTimeType(r1);
        r0 = r31;
        r1 = r38;
        r1.value = r0;
        r0 = r36;
        r1 = r22;
        r0.setIndex(r1);
        r31 = r29.tzID();
        r32 = r29.mzID();
        r0 = r33;
        r1 = r31;
        r2 = r32;
        r31 = r0.getTimeZoneID(r1, r2);
        r31 = android.icu.util.TimeZone.getTimeZone(r31);
        return r31;
    L_0x0360:
        r9 = 0;
        r31 = -getandroid-icu-text-TimeZoneFormat$StyleSwitchesValues();
        r32 = r34.ordinal();
        r31 = r31[r32];
        switch(r31) {
            case 2: goto L_0x0378;
            case 3: goto L_0x03ba;
            case 4: goto L_0x03c3;
            default: goto L_0x036e;
        };
    L_0x036e:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x037e;
    L_0x0372:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x0378:
        r31 = android.icu.impl.TimeZoneGenericNames.GenericNameType.LOCATION;
        r9 = java.util.EnumSet.of(r31);
    L_0x037e:
        r31 = r33.getTimeZoneGenericNames();
        r0 = r31;
        r1 = r35;
        r2 = r27;
        r4 = r0.findBestMatch(r1, r2, r9);
        if (r4 == 0) goto L_0x00df;
    L_0x038e:
        r31 = r4.matchLength();
        r31 = r31 + r27;
        r0 = r31;
        r1 = r22;
        if (r0 <= r1) goto L_0x00df;
    L_0x039a:
        r31 = r4.timeType();
        r0 = r31;
        r1 = r38;
        r1.value = r0;
        r31 = r4.matchLength();
        r31 = r31 + r27;
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r31 = r4.tzID();
        r31 = android.icu.util.TimeZone.getTimeZone(r31);
        return r31;
    L_0x03ba:
        r31 = android.icu.impl.TimeZoneGenericNames.GenericNameType.LONG;
        r32 = android.icu.impl.TimeZoneGenericNames.GenericNameType.LOCATION;
        r9 = java.util.EnumSet.of(r31, r32);
        goto L_0x037e;
    L_0x03c3:
        r31 = android.icu.impl.TimeZoneGenericNames.GenericNameType.SHORT;
        r32 = android.icu.impl.TimeZoneGenericNames.GenericNameType.LOCATION;
        r9 = java.util.EnumSet.of(r31, r32);
        goto L_0x037e;
    L_0x03cc:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r35;
        r1 = r28;
        r11 = parseZoneID(r0, r1);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x00df;
    L_0x03f0:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r31 = android.icu.util.TimeZone.getTimeZone(r11);
        return r31;
    L_0x0400:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r35;
        r1 = r28;
        r11 = parseShortZoneID(r0, r1);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x00df;
    L_0x0424:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r31 = android.icu.util.TimeZone.getTimeZone(r11);
        return r31;
    L_0x0434:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r11 = r0.parseExemplarLocation(r1, r2);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x00df;
    L_0x045a:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r31 = android.icu.util.TimeZone.getTimeZone(r11);
        return r31;
    L_0x046a:
        r0 = r36;
        r1 = r22;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r21;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x047a:
        r20 = 0;
        r24 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x048a;
    L_0x0482:
        if (r22 < 0) goto L_0x048a;
    L_0x0484:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x048a:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x049d;
    L_0x048e:
        r31 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r0 = r21;
        r1 = r31;
        if (r0 == r1) goto L_0x049d;
    L_0x0497:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x049d:
        r0 = r22;
        if (r0 >= r15) goto L_0x0530;
    L_0x04a1:
        r0 = r5 & 128;
        r31 = r0;
        if (r31 == 0) goto L_0x04ad;
    L_0x04a7:
        r0 = r5 & 256;
        r31 = r0;
        if (r31 != 0) goto L_0x0530;
    L_0x04ad:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r10 = new android.icu.util.Output;
        r31 = 0;
        r31 = java.lang.Boolean.valueOf(r31);
        r0 = r31;
        r10.<init>(r0);
        r31 = 0;
        r0 = r35;
        r1 = r28;
        r2 = r31;
        r17 = parseOffsetISO8601(r0, r1, r2, r10);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x0530;
    L_0x04e2:
        r31 = r28.getIndex();
        r0 = r31;
        if (r0 == r15) goto L_0x04f6;
    L_0x04ea:
        r0 = r10.value;
        r31 = r0;
        r31 = (java.lang.Boolean) r31;
        r31 = r31.booleanValue();
        if (r31 == 0) goto L_0x050a;
    L_0x04f6:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x050a:
        r31 = r28.getIndex();
        r0 = r22;
        r1 = r31;
        if (r0 >= r1) goto L_0x0530;
    L_0x0514:
        r21 = r17;
        r20 = 0;
        r24 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r22 = r28.getIndex();
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x0530;
    L_0x0522:
        r31 = r27 + 1;
        r0 = r22;
        r1 = r31;
        if (r0 == r1) goto L_0x0530;
    L_0x052a:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x0530:
        r0 = r22;
        if (r0 >= r15) goto L_0x05b3;
    L_0x0534:
        r31 = android.icu.text.TimeZoneFormat.Style.LOCALIZED_GMT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r31 = r31 & r5;
        if (r31 != 0) goto L_0x05b3;
    L_0x0540:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r10 = new android.icu.util.Output;
        r31 = 0;
        r31 = java.lang.Boolean.valueOf(r31);
        r0 = r31;
        r10.<init>(r0);
        r31 = 0;
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r3 = r31;
        r17 = r0.parseOffsetLocalizedGMT(r1, r2, r3, r10);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x05b3;
    L_0x0577:
        r31 = r28.getIndex();
        r0 = r31;
        if (r0 == r15) goto L_0x058b;
    L_0x057f:
        r0 = r10.value;
        r31 = r0;
        r31 = (java.lang.Boolean) r31;
        r31 = r31.booleanValue();
        if (r31 == 0) goto L_0x059f;
    L_0x058b:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x059f:
        r31 = r28.getIndex();
        r0 = r22;
        r1 = r31;
        if (r0 >= r1) goto L_0x05b3;
    L_0x05a9:
        r21 = r17;
        r20 = 0;
        r24 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r22 = r28.getIndex();
    L_0x05b3:
        r0 = r22;
        if (r0 >= r15) goto L_0x0636;
    L_0x05b7:
        r31 = android.icu.text.TimeZoneFormat.Style.LOCALIZED_GMT_SHORT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r31 = r31 & r5;
        if (r31 != 0) goto L_0x0636;
    L_0x05c3:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r10 = new android.icu.util.Output;
        r31 = 0;
        r31 = java.lang.Boolean.valueOf(r31);
        r0 = r31;
        r10.<init>(r0);
        r31 = 1;
        r0 = r33;
        r1 = r35;
        r2 = r28;
        r3 = r31;
        r17 = r0.parseOffsetLocalizedGMT(r1, r2, r3, r10);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x0636;
    L_0x05fa:
        r31 = r28.getIndex();
        r0 = r31;
        if (r0 == r15) goto L_0x060e;
    L_0x0602:
        r0 = r10.value;
        r31 = r0;
        r31 = (java.lang.Boolean) r31;
        r31 = r31.booleanValue();
        if (r31 == 0) goto L_0x0622;
    L_0x060e:
        r31 = r28.getIndex();
        r0 = r36;
        r1 = r31;
        r0.setIndex(r1);
        r0 = r33;
        r1 = r17;
        r31 = r0.getTimeZoneForOffset(r1);
        return r31;
    L_0x0622:
        r31 = r28.getIndex();
        r0 = r22;
        r1 = r31;
        if (r0 >= r1) goto L_0x0636;
    L_0x062c:
        r21 = r17;
        r20 = 0;
        r24 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r22 = r28.getIndex();
    L_0x0636:
        if (r37 != 0) goto L_0x0684;
    L_0x0638:
        r31 = r33.getDefaultParseOptions();
        r32 = android.icu.text.TimeZoneFormat.ParseOption.ALL_STYLES;
        r18 = r31.contains(r32);
    L_0x0642:
        if (r18 == 0) goto L_0x07f0;
    L_0x0644:
        r0 = r22;
        if (r0 >= r15) goto L_0x06b6;
    L_0x0648:
        r0 = r33;
        r0 = r0._tznames;
        r31 = r0;
        r32 = ALL_SIMPLE_NAME_TYPES;
        r0 = r31;
        r1 = r35;
        r2 = r27;
        r3 = r32;
        r26 = r0.find(r1, r2, r3);
        r25 = 0;
        r14 = -1;
        if (r26 == 0) goto L_0x068f;
    L_0x0661:
        r13 = r26.iterator();
    L_0x0665:
        r31 = r13.hasNext();
        if (r31 == 0) goto L_0x068f;
    L_0x066b:
        r12 = r13.next();
        r12 = (android.icu.text.TimeZoneNames.MatchInfo) r12;
        r31 = r12.matchLength();
        r31 = r31 + r27;
        r0 = r31;
        if (r0 <= r14) goto L_0x0665;
    L_0x067b:
        r25 = r12;
        r31 = r12.matchLength();
        r14 = r27 + r31;
        goto L_0x0665;
    L_0x0684:
        r31 = android.icu.text.TimeZoneFormat.ParseOption.ALL_STYLES;
        r0 = r37;
        r1 = r31;
        r18 = r0.contains(r1);
        goto L_0x0642;
    L_0x068f:
        r0 = r22;
        if (r0 >= r14) goto L_0x06b6;
    L_0x0693:
        r22 = r14;
        r31 = r25.tzID();
        r32 = r25.mzID();
        r0 = r33;
        r1 = r31;
        r2 = r32;
        r20 = r0.getTimeZoneID(r1, r2);
        r31 = r25.nameType();
        r0 = r33;
        r1 = r31;
        r24 = r0.getTimeType(r1);
        r21 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
    L_0x06b6:
        if (r19 == 0) goto L_0x0729;
    L_0x06b8:
        r0 = r22;
        if (r0 >= r15) goto L_0x0729;
    L_0x06bc:
        r31 = android.icu.text.TimeZoneFormat.Style.SPECIFIC_SHORT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r31 = r31 & r5;
        if (r31 != 0) goto L_0x0729;
    L_0x06c8:
        r31 = r33.getTZDBTimeZoneNames();
        r32 = ALL_SIMPLE_NAME_TYPES;
        r0 = r31;
        r1 = r35;
        r2 = r27;
        r3 = r32;
        r30 = r0.find(r1, r2, r3);
        r29 = 0;
        r14 = -1;
        if (r30 == 0) goto L_0x0729;
    L_0x06df:
        r13 = r30.iterator();
    L_0x06e3:
        r31 = r13.hasNext();
        if (r31 == 0) goto L_0x0702;
    L_0x06e9:
        r12 = r13.next();
        r12 = (android.icu.text.TimeZoneNames.MatchInfo) r12;
        r31 = r12.matchLength();
        r31 = r31 + r27;
        r0 = r31;
        if (r0 <= r14) goto L_0x06e3;
    L_0x06f9:
        r29 = r12;
        r31 = r12.matchLength();
        r14 = r27 + r31;
        goto L_0x06e3;
    L_0x0702:
        r0 = r22;
        if (r0 >= r14) goto L_0x0729;
    L_0x0706:
        r22 = r14;
        r31 = r29.tzID();
        r32 = r29.mzID();
        r0 = r33;
        r1 = r31;
        r2 = r32;
        r20 = r0.getTimeZoneID(r1, r2);
        r31 = r29.nameType();
        r0 = r33;
        r1 = r31;
        r24 = r0.getTimeType(r1);
        r21 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
    L_0x0729:
        r0 = r22;
        if (r0 >= r15) goto L_0x075e;
    L_0x072d:
        r31 = r33.getTimeZoneGenericNames();
        r32 = ALL_GENERIC_NAME_TYPES;
        r0 = r31;
        r1 = r35;
        r2 = r27;
        r3 = r32;
        r8 = r0.findBestMatch(r1, r2, r3);
        if (r8 == 0) goto L_0x075e;
    L_0x0741:
        r31 = r8.matchLength();
        r31 = r31 + r27;
        r0 = r22;
        r1 = r31;
        if (r0 >= r1) goto L_0x075e;
    L_0x074d:
        r31 = r8.matchLength();
        r22 = r27 + r31;
        r20 = r8.tzID();
        r24 = r8.timeType();
        r21 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
    L_0x075e:
        r0 = r22;
        if (r0 >= r15) goto L_0x07a7;
    L_0x0762:
        r31 = android.icu.text.TimeZoneFormat.Style.ZONE_ID;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r31 = r31 & r5;
        if (r31 != 0) goto L_0x07a7;
    L_0x076e:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r35;
        r1 = r28;
        r11 = parseZoneID(r0, r1);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x07a7;
    L_0x0792:
        r31 = r28.getIndex();
        r0 = r22;
        r1 = r31;
        if (r0 >= r1) goto L_0x07a7;
    L_0x079c:
        r22 = r28.getIndex();
        r20 = r11;
        r24 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r21 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
    L_0x07a7:
        r0 = r22;
        if (r0 >= r15) goto L_0x07f0;
    L_0x07ab:
        r31 = android.icu.text.TimeZoneFormat.Style.ZONE_ID_SHORT;
        r0 = r31;
        r0 = r0.flag;
        r31 = r0;
        r31 = r31 & r5;
        if (r31 != 0) goto L_0x07f0;
    L_0x07b7:
        r0 = r28;
        r1 = r27;
        r0.setIndex(r1);
        r31 = -1;
        r0 = r28;
        r1 = r31;
        r0.setErrorIndex(r1);
        r0 = r35;
        r1 = r28;
        r11 = parseShortZoneID(r0, r1);
        r31 = r28.getErrorIndex();
        r32 = -1;
        r0 = r31;
        r1 = r32;
        if (r0 != r1) goto L_0x07f0;
    L_0x07db:
        r31 = r28.getIndex();
        r0 = r22;
        r1 = r31;
        if (r0 >= r1) goto L_0x07f0;
    L_0x07e5:
        r22 = r28.getIndex();
        r20 = r11;
        r24 = android.icu.text.TimeZoneFormat.TimeType.UNKNOWN;
        r21 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
    L_0x07f0:
        r0 = r22;
        r1 = r27;
        if (r0 <= r1) goto L_0x0828;
    L_0x07f6:
        r23 = 0;
        if (r20 == 0) goto L_0x080c;
    L_0x07fa:
        r23 = android.icu.util.TimeZone.getTimeZone(r20);
    L_0x07fe:
        r0 = r24;
        r1 = r38;
        r1.value = r0;
        r0 = r36;
        r1 = r22;
        r0.setIndex(r1);
        return r23;
    L_0x080c:
        r31 = -assertionsDisabled;
        if (r31 != 0) goto L_0x081f;
    L_0x0810:
        r31 = 2147483647; // 0x7fffffff float:NaN double:1.060997895E-314;
        r0 = r21;
        r1 = r31;
        if (r0 != r1) goto L_0x081f;
    L_0x0819:
        r31 = new java.lang.AssertionError;
        r31.<init>();
        throw r31;
    L_0x081f:
        r0 = r33;
        r1 = r21;
        r23 = r0.getTimeZoneForOffset(r1);
        goto L_0x07fe;
    L_0x0828:
        r0 = r36;
        r1 = r27;
        r0.setErrorIndex(r1);
        r31 = 0;
        return r31;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.icu.text.TimeZoneFormat.parse(android.icu.text.TimeZoneFormat$Style, java.lang.String, java.text.ParsePosition, java.util.EnumSet, android.icu.util.Output):android.icu.util.TimeZone");
    }

    public TimeZone parse(Style style, String text, ParsePosition pos, Output<TimeType> timeType) {
        return parse(style, text, pos, null, timeType);
    }

    public final TimeZone parse(String text, ParsePosition pos) {
        return parse(Style.GENERIC_LOCATION, text, pos, EnumSet.of(ParseOption.ALL_STYLES), null);
    }

    public final TimeZone parse(String text) throws ParseException {
        ParsePosition pos = new ParsePosition(0);
        TimeZone tz = parse(text, pos);
        if (pos.getErrorIndex() >= 0) {
            throw new ParseException("Unparseable time zone: \"" + text + "\"", 0);
        } else if (-assertionsDisabled || tz != null) {
            return tz;
        } else {
            throw new AssertionError();
        }
    }

    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        TimeZone tz;
        long date = System.currentTimeMillis();
        if (obj instanceof TimeZone) {
            tz = (TimeZone) obj;
        } else if (obj instanceof Calendar) {
            tz = ((Calendar) obj).getTimeZone();
            date = ((Calendar) obj).getTimeInMillis();
        } else {
            throw new IllegalArgumentException("Cannot format given Object (" + obj.getClass().getName() + ") as a time zone");
        }
        if (-assertionsDisabled || tz != null) {
            String result = formatOffsetLocalizedGMT(tz.getOffset(date));
            toAppendTo.append(result);
            if (pos.getFieldAttribute() == Field.TIME_ZONE || pos.getField() == 17) {
                pos.setBeginIndex(0);
                pos.setEndIndex(result.length());
            }
            return toAppendTo;
        }
        throw new AssertionError();
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        AttributedString as = new AttributedString(format(obj, new StringBuffer(), new FieldPosition(0)).toString());
        as.addAttribute(Field.TIME_ZONE, Field.TIME_ZONE);
        return as.getIterator();
    }

    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    private String formatOffsetLocalizedGMT(int offset, boolean isShort) {
        if (offset == 0) {
            return this._gmtZeroFormat;
        }
        StringBuilder buf = new StringBuilder();
        boolean positive = true;
        if (offset < 0) {
            offset = -offset;
            positive = false;
        }
        int offsetH = offset / 3600000;
        offset %= 3600000;
        int offsetM = offset / 60000;
        offset %= 60000;
        int offsetS = offset / 1000;
        if (offsetH > 23 || offsetM > 59 || offsetS > 59) {
            throw new IllegalArgumentException("Offset out of range :" + offset);
        }
        Object[] offsetPatternItems;
        if (positive) {
            if (offsetS != 0) {
                offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HMS.ordinal()];
            } else if (offsetM == 0 && (isShort ^ 1) == 0) {
                offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_H.ordinal()];
            } else {
                offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.POSITIVE_HM.ordinal()];
            }
        } else if (offsetS != 0) {
            offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HMS.ordinal()];
        } else if (offsetM == 0 && (isShort ^ 1) == 0) {
            offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_H.ordinal()];
        } else {
            offsetPatternItems = this._gmtOffsetPatternItems[GMTOffsetPatternType.NEGATIVE_HM.ordinal()];
        }
        buf.append(this._gmtPatternPrefix);
        for (GMTOffsetField item : offsetPatternItems) {
            if (item instanceof String) {
                buf.append((String) item);
            } else if (item instanceof GMTOffsetField) {
                switch (item.getType()) {
                    case 'H':
                        appendOffsetDigits(buf, offsetH, isShort ? 1 : 2);
                        break;
                    case 'm':
                        appendOffsetDigits(buf, offsetM, 2);
                        break;
                    case 's':
                        appendOffsetDigits(buf, offsetS, 2);
                        break;
                    default:
                        break;
                }
            }
        }
        buf.append(this._gmtPatternSuffix);
        return buf.toString();
    }

    private String formatOffsetISO8601(int offset, boolean isBasic, boolean useUtcIndicator, boolean isShort, boolean ignoreSeconds) {
        int absOffset = offset < 0 ? -offset : offset;
        if (useUtcIndicator && (absOffset < 1000 || (ignoreSeconds && absOffset < 60000))) {
            return ISO8601_UTC;
        }
        OffsetFields minFields = isShort ? OffsetFields.H : OffsetFields.HM;
        OffsetFields maxFields = ignoreSeconds ? OffsetFields.HM : OffsetFields.HMS;
        Object sep = isBasic ? null : Character.valueOf(DEFAULT_GMT_OFFSET_SEP);
        if (absOffset >= 86400000) {
            throw new IllegalArgumentException("Offset out of range :" + offset);
        }
        fields = new int[3];
        absOffset %= 3600000;
        fields[1] = absOffset / 60000;
        fields[2] = (absOffset % 60000) / 1000;
        if (!-assertionsDisabled && (fields[0] < 0 || fields[0] > 23)) {
            throw new AssertionError();
        } else if (!-assertionsDisabled && (fields[1] < 0 || fields[1] > 59)) {
            throw new AssertionError();
        } else if (-assertionsDisabled || (fields[2] >= 0 && fields[2] <= 59)) {
            int idx;
            int lastIdx = maxFields.ordinal();
            while (lastIdx > minFields.ordinal() && fields[lastIdx] == 0) {
                lastIdx--;
            }
            StringBuilder buf = new StringBuilder();
            char sign = '+';
            if (offset < 0) {
                for (idx = 0; idx <= lastIdx; idx++) {
                    if (fields[idx] != 0) {
                        sign = '-';
                        break;
                    }
                }
            }
            buf.append(sign);
            idx = 0;
            while (idx <= lastIdx) {
                if (!(sep == null || idx == 0)) {
                    buf.append(sep);
                }
                if (fields[idx] < 10) {
                    buf.append('0');
                }
                buf.append(fields[idx]);
                idx++;
            }
            return buf.toString();
        } else {
            throw new AssertionError();
        }
    }

    private String formatSpecific(TimeZone tz, NameType stdType, NameType dstType, long date, Output<TimeType> timeType) {
        if (!-assertionsDisabled && stdType != NameType.LONG_STANDARD && stdType != NameType.SHORT_STANDARD) {
            throw new AssertionError();
        } else if (-assertionsDisabled || dstType == NameType.LONG_DAYLIGHT || dstType == NameType.SHORT_DAYLIGHT) {
            String name;
            boolean isDaylight = tz.inDaylightTime(new Date(date));
            if (isDaylight) {
                name = getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(tz), dstType, date);
            } else {
                name = getTimeZoneNames().getDisplayName(ZoneMeta.getCanonicalCLDRID(tz), stdType, date);
            }
            if (!(name == null || timeType == null)) {
                timeType.value = isDaylight ? TimeType.DAYLIGHT : TimeType.STANDARD;
            }
            return name;
        } else {
            throw new AssertionError();
        }
    }

    private String formatExemplarLocation(TimeZone tz) {
        String location = getTimeZoneNames().getExemplarLocationName(ZoneMeta.getCanonicalCLDRID(tz));
        if (location != null) {
            return location;
        }
        location = getTimeZoneNames().getExemplarLocationName("Etc/Unknown");
        if (location == null) {
            return UNKNOWN_LOCATION;
        }
        return location;
    }

    private String getTimeZoneID(String tzID, String mzID) {
        String id = tzID;
        if (tzID == null) {
            if (-assertionsDisabled || mzID != null) {
                id = this._tznames.getReferenceZoneID(mzID, getTargetRegion());
                if (id == null) {
                    throw new IllegalArgumentException("Invalid mzID: " + mzID);
                }
            }
            throw new AssertionError();
        }
        return id;
    }

    private synchronized String getTargetRegion() {
        if (this._region == null) {
            this._region = this._locale.getCountry();
            if (this._region.length() == 0) {
                this._region = ULocale.addLikelySubtags(this._locale).getCountry();
                if (this._region.length() == 0) {
                    this._region = "001";
                }
            }
        }
        return this._region;
    }

    private TimeType getTimeType(NameType nameType) {
        switch (-getandroid-icu-text-TimeZoneNames$NameTypeSwitchesValues()[nameType.ordinal()]) {
            case 1:
            case 3:
                return TimeType.DAYLIGHT;
            case 2:
            case 4:
                return TimeType.STANDARD;
            default:
                return TimeType.UNKNOWN;
        }
    }

    private void initGMTPattern(String gmtPattern) {
        int idx = gmtPattern.indexOf("{0}");
        if (idx < 0) {
            throw new IllegalArgumentException("Bad localized GMT pattern: " + gmtPattern);
        }
        this._gmtPattern = gmtPattern;
        this._gmtPatternPrefix = unquote(gmtPattern.substring(0, idx));
        this._gmtPatternSuffix = unquote(gmtPattern.substring(idx + 3));
    }

    private static String unquote(String s) {
        if (s.indexOf(39) < 0) {
            return s;
        }
        boolean isPrevQuote = false;
        int inQuote = 0;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == PatternTokenizer.SINGLE_QUOTE) {
                if (isPrevQuote) {
                    buf.append(c);
                    isPrevQuote = false;
                } else {
                    isPrevQuote = true;
                }
                inQuote ^= 1;
            } else {
                isPrevQuote = false;
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private void initGMTOffsetPatterns(String[] gmtOffsetPatterns) {
        int size = GMTOffsetPatternType.values().length;
        if (gmtOffsetPatterns.length < size) {
            throw new IllegalArgumentException("Insufficient number of elements in gmtOffsetPatterns");
        }
        Object[][] gmtOffsetPatternItems = new Object[size][];
        for (GMTOffsetPatternType t : GMTOffsetPatternType.values()) {
            int idx = t.ordinal();
            gmtOffsetPatternItems[idx] = parseOffsetPattern(gmtOffsetPatterns[idx], t.required());
        }
        this._gmtOffsetPatterns = new String[size];
        System.arraycopy(gmtOffsetPatterns, 0, this._gmtOffsetPatterns, 0, size);
        this._gmtOffsetPatternItems = gmtOffsetPatternItems;
        checkAbuttingHoursAndMinutes();
    }

    private void checkAbuttingHoursAndMinutes() {
        this._abuttingOffsetHoursAndMinutes = false;
        for (Object[] items : this._gmtOffsetPatternItems) {
            boolean afterH = false;
            for (GMTOffsetField item : r7[r6]) {
                if (item instanceof GMTOffsetField) {
                    GMTOffsetField fld = item;
                    if (afterH) {
                        this._abuttingOffsetHoursAndMinutes = true;
                    } else if (fld.getType() == 'H') {
                        afterH = true;
                    }
                } else if (afterH) {
                    break;
                }
            }
        }
    }

    private static Object[] parseOffsetPattern(String pattern, String letters) {
        boolean isPrevQuote = false;
        int inQuote = 0;
        StringBuilder text = new StringBuilder();
        char itemType = 0;
        int itemLength = 1;
        boolean invalidPattern = false;
        List<Object> items = new ArrayList();
        BitSet checkBits = new BitSet(letters.length());
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == PatternTokenizer.SINGLE_QUOTE) {
                if (!isPrevQuote) {
                    isPrevQuote = true;
                    if (itemType != 0) {
                        if (!GMTOffsetField.isValid(itemType, itemLength)) {
                            invalidPattern = true;
                            break;
                        }
                        items.add(new GMTOffsetField(itemType, itemLength));
                        itemType = 0;
                    }
                } else {
                    text.append(PatternTokenizer.SINGLE_QUOTE);
                    isPrevQuote = false;
                }
                inQuote ^= 1;
            } else {
                isPrevQuote = false;
                if (inQuote != 0) {
                    text.append(ch);
                } else {
                    int patFieldIdx = letters.indexOf(ch);
                    if (patFieldIdx < 0) {
                        if (itemType != 0) {
                            if (!GMTOffsetField.isValid(itemType, itemLength)) {
                                invalidPattern = true;
                                break;
                            }
                            items.add(new GMTOffsetField(itemType, itemLength));
                            itemType = 0;
                        }
                        text.append(ch);
                    } else if (ch == itemType) {
                        itemLength++;
                    } else {
                        if (itemType != 0) {
                            if (!GMTOffsetField.isValid(itemType, itemLength)) {
                                invalidPattern = true;
                                break;
                            }
                            items.add(new GMTOffsetField(itemType, itemLength));
                        } else if (text.length() > 0) {
                            items.add(text.toString());
                            text.setLength(0);
                        }
                        itemType = ch;
                        itemLength = 1;
                        checkBits.set(patFieldIdx);
                    }
                }
            }
        }
        if (!invalidPattern) {
            if (itemType == 0) {
                if (text.length() > 0) {
                    items.add(text.toString());
                    text.setLength(0);
                }
            } else if (GMTOffsetField.isValid(itemType, itemLength)) {
                items.add(new GMTOffsetField(itemType, itemLength));
            } else {
                invalidPattern = true;
            }
        }
        if (!invalidPattern && checkBits.cardinality() == letters.length()) {
            return items.toArray(new Object[items.size()]);
        }
        throw new IllegalStateException("Bad localized GMT offset pattern: " + pattern);
    }

    private static String expandOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm < 0) {
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        String sep = ":";
        int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf(DateFormat.HOUR24);
        if (idx_H >= 0) {
            sep = offsetHM.substring(idx_H + 1, idx_mm);
        }
        return offsetHM.substring(0, idx_mm + 2) + sep + "ss" + offsetHM.substring(idx_mm + 2);
    }

    private static String truncateOffsetPattern(String offsetHM) {
        int idx_mm = offsetHM.indexOf("mm");
        if (idx_mm < 0) {
            throw new RuntimeException("Bad time zone hour pattern data");
        }
        int idx_HH = offsetHM.substring(0, idx_mm).lastIndexOf("HH");
        if (idx_HH >= 0) {
            return offsetHM.substring(0, idx_HH + 2);
        }
        int idx_H = offsetHM.substring(0, idx_mm).lastIndexOf(DateFormat.HOUR24);
        if (idx_H >= 0) {
            return offsetHM.substring(0, idx_H + 1);
        }
        throw new RuntimeException("Bad time zone hour pattern data");
    }

    private void appendOffsetDigits(StringBuilder buf, int n, int minDigits) {
        if (-assertionsDisabled || (n >= 0 && n < 60)) {
            int numDigits = n >= 10 ? 2 : 1;
            for (int i = 0; i < minDigits - numDigits; i++) {
                buf.append(this._gmtOffsetDigits[0]);
            }
            if (numDigits == 2) {
                buf.append(this._gmtOffsetDigits[n / 10]);
            }
            buf.append(this._gmtOffsetDigits[n % 10]);
            return;
        }
        throw new AssertionError();
    }

    private TimeZone getTimeZoneForOffset(int offset) {
        if (offset == 0) {
            return TimeZone.getTimeZone(TZID_GMT);
        }
        return ZoneMeta.getCustomTimeZone(offset);
    }

    private int parseOffsetLocalizedGMT(String text, ParsePosition pos, boolean isShort, Output<Boolean> hasDigitOffset) {
        int start = pos.getIndex();
        int[] parsedLength = new int[]{0};
        if (hasDigitOffset != null) {
            hasDigitOffset.value = Boolean.valueOf(false);
        }
        int offset = parseOffsetLocalizedGMTPattern(text, start, isShort, parsedLength);
        if (parsedLength[0] > 0) {
            if (hasDigitOffset != null) {
                hasDigitOffset.value = Boolean.valueOf(true);
            }
            pos.setIndex(parsedLength[0] + start);
            return offset;
        }
        offset = parseOffsetDefaultLocalizedGMT(text, start, parsedLength);
        if (parsedLength[0] > 0) {
            if (hasDigitOffset != null) {
                hasDigitOffset.value = Boolean.valueOf(true);
            }
            pos.setIndex(parsedLength[0] + start);
            return offset;
        }
        if (text.regionMatches(true, start, this._gmtZeroFormat, 0, this._gmtZeroFormat.length())) {
            pos.setIndex(this._gmtZeroFormat.length() + start);
            return 0;
        }
        for (String defGMTZero : ALT_GMT_STRINGS) {
            if (text.regionMatches(true, start, defGMTZero, 0, defGMTZero.length())) {
                pos.setIndex(defGMTZero.length() + start);
                return 0;
            }
        }
        pos.setErrorIndex(start);
        return 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int parseOffsetLocalizedGMTPattern(String text, int start, boolean isShort, int[] parsedLen) {
        int idx;
        int i;
        int idx2 = start;
        int offset = 0;
        boolean parsed = false;
        int len = this._gmtPatternPrefix.length();
        if (len > 0) {
            if ((text.regionMatches(true, start, this._gmtPatternPrefix, 0, len) ^ 1) != 0) {
                idx = idx2;
                if (parsed) {
                    i = 0;
                } else {
                    i = idx - start;
                }
                parsedLen[0] = i;
                return offset;
            }
        }
        idx = start + len;
        int[] offsetLen = new int[1];
        offset = parseOffsetFields(text, idx, false, offsetLen);
        if (offsetLen[0] != 0) {
            idx += offsetLen[0];
            len = this._gmtPatternSuffix.length();
            if (len > 0) {
            }
            idx += len;
            parsed = true;
        }
        if (parsed) {
            i = 0;
        } else {
            i = idx - start;
        }
        parsedLen[0] = i;
        return offset;
    }

    private int parseOffsetFields(String text, int start, boolean isShort, int[] parsedLen) {
        int i;
        GMTOffsetPatternType gmtPatType;
        Object[] items;
        int outLen = 0;
        int sign = 1;
        if (parsedLen != null && parsedLen.length >= 1) {
            parsedLen[0] = 0;
        }
        int offsetS = 0;
        int offsetM = 0;
        int offsetH = 0;
        int[] fields = new int[]{0, 0, 0};
        GMTOffsetPatternType[] gMTOffsetPatternTypeArr = PARSE_GMT_OFFSET_TYPES;
        int i2 = 0;
        int length = gMTOffsetPatternTypeArr.length;
        while (true) {
            i = i2;
            if (i >= length) {
                break;
            }
            gmtPatType = gMTOffsetPatternTypeArr[i];
            items = this._gmtOffsetPatternItems[gmtPatType.ordinal()];
            if (-assertionsDisabled || items != null) {
                outLen = parseOffsetFieldsWithPattern(text, start, items, false, fields);
                if (outLen > 0) {
                    sign = gmtPatType.isPositive() ? 1 : -1;
                    offsetH = fields[0];
                    offsetM = fields[1];
                    offsetS = fields[2];
                } else {
                    i2 = i + 1;
                }
            } else {
                throw new AssertionError();
            }
        }
        if (outLen > 0 && this._abuttingOffsetHoursAndMinutes) {
            int tmpLen = 0;
            int tmpSign = 1;
            gMTOffsetPatternTypeArr = PARSE_GMT_OFFSET_TYPES;
            i2 = 0;
            length = gMTOffsetPatternTypeArr.length;
            while (true) {
                i = i2;
                if (i >= length) {
                    break;
                }
                gmtPatType = gMTOffsetPatternTypeArr[i];
                items = this._gmtOffsetPatternItems[gmtPatType.ordinal()];
                if (-assertionsDisabled || items != null) {
                    tmpLen = parseOffsetFieldsWithPattern(text, start, items, true, fields);
                    if (tmpLen > 0) {
                        tmpSign = gmtPatType.isPositive() ? 1 : -1;
                    } else {
                        i2 = i + 1;
                    }
                } else {
                    throw new AssertionError();
                }
            }
            if (tmpLen > outLen) {
                outLen = tmpLen;
                sign = tmpSign;
                offsetH = fields[0];
                offsetM = fields[1];
                offsetS = fields[2];
            }
        }
        if (parsedLen != null && parsedLen.length >= 1) {
            parsedLen[0] = outLen;
        }
        if (outLen > 0) {
            return (((((offsetH * 60) + offsetM) * 60) + offsetS) * 1000) * sign;
        }
        return 0;
    }

    private int parseOffsetFieldsWithPattern(String text, int start, Object[] patternItems, boolean forceSingleHourDigit, int[] fields) {
        if (-assertionsDisabled || (fields != null && fields.length >= 3)) {
            fields[2] = 0;
            fields[1] = 0;
            fields[0] = 0;
            boolean failed = false;
            int offsetS = 0;
            int offsetM = 0;
            int offsetH = 0;
            int idx = start;
            int[] tmpParsedLen = new int[]{0};
            int i = 0;
            while (i < patternItems.length) {
                if (patternItems[i] instanceof String) {
                    String patStr = patternItems[i];
                    int len = patStr.length();
                    if (!text.regionMatches(true, idx, patStr, 0, len)) {
                        failed = true;
                        break;
                    }
                    idx += len;
                } else if (-assertionsDisabled || (patternItems[i] instanceof GMTOffsetField)) {
                    char fieldType = patternItems[i].getType();
                    if (fieldType == 'H') {
                        offsetH = parseOffsetFieldWithLocalizedDigits(text, idx, 1, forceSingleHourDigit ? 1 : 2, 0, 23, tmpParsedLen);
                    } else if (fieldType == 'm') {
                        offsetM = parseOffsetFieldWithLocalizedDigits(text, idx, 2, 2, 0, 59, tmpParsedLen);
                    } else if (fieldType == 's') {
                        offsetS = parseOffsetFieldWithLocalizedDigits(text, idx, 2, 2, 0, 59, tmpParsedLen);
                    }
                    if (tmpParsedLen[0] == 0) {
                        failed = true;
                        break;
                    }
                    idx += tmpParsedLen[0];
                } else {
                    throw new AssertionError();
                }
                i++;
            }
            if (failed) {
                return 0;
            }
            fields[0] = offsetH;
            fields[1] = offsetM;
            fields[2] = offsetS;
            return idx - start;
        }
        throw new AssertionError();
    }

    private int parseOffsetDefaultLocalizedGMT(String text, int start, int[] parsedLen) {
        int idx = start;
        int offset = 0;
        int parsed = 0;
        int gmtLen = 0;
        String[] strArr = ALT_GMT_STRINGS;
        int i = 0;
        int length = strArr.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                break;
            }
            String gmt = strArr[i2];
            int len = gmt.length();
            if (text.regionMatches(true, start, gmt, 0, len)) {
                gmtLen = len;
                break;
            }
            i = i2 + 1;
        }
        if (gmtLen != 0) {
            idx = start + gmtLen;
            if (idx + 1 < text.length()) {
                int sign;
                char c = text.charAt(idx);
                if (c == '+') {
                    sign = 1;
                } else if (c == '-') {
                    sign = -1;
                }
                idx++;
                int[] lenWithSep = new int[]{0};
                int offsetWithSep = parseDefaultOffsetFields(text, idx, DEFAULT_GMT_OFFSET_SEP, lenWithSep);
                if (lenWithSep[0] == text.length() - idx) {
                    offset = offsetWithSep * sign;
                    idx += lenWithSep[0];
                } else {
                    int[] lenAbut = new int[]{0};
                    int offsetAbut = parseAbuttingOffsetFields(text, idx, lenAbut);
                    if (lenWithSep[0] > lenAbut[0]) {
                        offset = offsetWithSep * sign;
                        idx += lenWithSep[0];
                    } else {
                        offset = offsetAbut * sign;
                        idx += lenAbut[0];
                    }
                }
                parsed = idx - start;
            }
        }
        parsedLen[0] = parsed;
        return offset;
    }

    private int parseDefaultOffsetFields(String text, int start, char separator, int[] parsedLen) {
        int max = text.length();
        int idx = start;
        int[] len = new int[]{0};
        int min = 0;
        int sec = 0;
        int hour = parseOffsetFieldWithLocalizedDigits(text, start, 1, 2, 0, 23, len);
        if (len[0] != 0) {
            idx = start + len[0];
            if (idx + 1 < max && text.charAt(idx) == separator) {
                min = parseOffsetFieldWithLocalizedDigits(text, idx + 1, 2, 2, 0, 59, len);
                if (len[0] != 0) {
                    idx += len[0] + 1;
                    if (idx + 1 < max && text.charAt(idx) == separator) {
                        sec = parseOffsetFieldWithLocalizedDigits(text, idx + 1, 2, 2, 0, 59, len);
                        if (len[0] != 0) {
                            idx += len[0] + 1;
                        }
                    }
                }
            }
        }
        if (idx == start) {
            parsedLen[0] = 0;
            return 0;
        }
        parsedLen[0] = idx - start;
        return ((3600000 * hour) + (60000 * min)) + (sec * 1000);
    }

    private int parseAbuttingOffsetFields(String text, int start, int[] parsedLen) {
        int[] digits = new int[6];
        int[] parsed = new int[6];
        int idx = start;
        int[] len = new int[]{0};
        int numDigits = 0;
        for (int i = 0; i < 6; i++) {
            digits[i] = parseSingleLocalizedDigit(text, idx, len);
            if (digits[i] < 0) {
                break;
            }
            idx += len[0];
            parsed[i] = idx - start;
            numDigits++;
        }
        if (numDigits == 0) {
            parsedLen[0] = 0;
            return 0;
        }
        int offset = 0;
        while (numDigits > 0) {
            int hour = 0;
            int min = 0;
            int sec = 0;
            if (-assertionsDisabled || (numDigits > 0 && numDigits <= 6)) {
                switch (numDigits) {
                    case 1:
                        hour = digits[0];
                        break;
                    case 2:
                        hour = (digits[0] * 10) + digits[1];
                        break;
                    case 3:
                        hour = digits[0];
                        min = (digits[1] * 10) + digits[2];
                        break;
                    case 4:
                        hour = (digits[0] * 10) + digits[1];
                        min = (digits[2] * 10) + digits[3];
                        break;
                    case 5:
                        hour = digits[0];
                        min = (digits[1] * 10) + digits[2];
                        sec = (digits[3] * 10) + digits[4];
                        break;
                    case 6:
                        hour = (digits[0] * 10) + digits[1];
                        min = (digits[2] * 10) + digits[3];
                        sec = (digits[4] * 10) + digits[5];
                        break;
                }
                if (hour > 23 || min > 59 || sec > 59) {
                    numDigits--;
                } else {
                    offset = ((3600000 * hour) + (60000 * min)) + (sec * 1000);
                    parsedLen[0] = parsed[numDigits - 1];
                    return offset;
                }
            }
            throw new AssertionError();
        }
        return offset;
    }

    private int parseOffsetFieldWithLocalizedDigits(String text, int start, int minDigits, int maxDigits, int minVal, int maxVal, int[] parsedLen) {
        parsedLen[0] = 0;
        int decVal = 0;
        int numDigits = 0;
        int idx = start;
        int[] digitLen = new int[]{0};
        while (idx < text.length() && numDigits < maxDigits) {
            int digit = parseSingleLocalizedDigit(text, idx, digitLen);
            if (digit >= 0) {
                int tmpVal = (decVal * 10) + digit;
                if (tmpVal > maxVal) {
                    break;
                }
                decVal = tmpVal;
                numDigits++;
                idx += digitLen[0];
            } else {
                break;
            }
        }
        if (numDigits < minDigits || decVal < minVal) {
            return -1;
        }
        parsedLen[0] = idx - start;
        return decVal;
    }

    private int parseSingleLocalizedDigit(String text, int start, int[] len) {
        int digit = -1;
        len[0] = 0;
        if (start < text.length()) {
            int cp = Character.codePointAt(text, start);
            for (int i = 0; i < this._gmtOffsetDigits.length; i++) {
                if (cp == this._gmtOffsetDigits[i].codePointAt(0)) {
                    digit = i;
                    break;
                }
            }
            if (digit < 0) {
                digit = UCharacter.digit(cp);
            }
            if (digit >= 0) {
                len[0] = Character.charCount(cp);
            }
        }
        return digit;
    }

    private static String[] toCodePoints(String str) {
        int len = str.codePointCount(0, str.length());
        String[] codePoints = new String[len];
        int offset = 0;
        for (int i = 0; i < len; i++) {
            int codeLen = Character.charCount(str.codePointAt(offset));
            codePoints[i] = str.substring(offset, offset + codeLen);
            offset += codeLen;
        }
        return codePoints;
    }

    private static int parseOffsetISO8601(String text, ParsePosition pos, boolean extendedOnly, Output<Boolean> hasDigitOffset) {
        if (hasDigitOffset != null) {
            hasDigitOffset.value = Boolean.valueOf(false);
        }
        int start = pos.getIndex();
        if (start >= text.length()) {
            pos.setErrorIndex(start);
            return 0;
        }
        char firstChar = text.charAt(start);
        if (Character.toUpperCase(firstChar) == ISO8601_UTC.charAt(0)) {
            pos.setIndex(start + 1);
            return 0;
        }
        int sign;
        if (firstChar == '+') {
            sign = 1;
        } else if (firstChar == '-') {
            sign = -1;
        } else {
            pos.setErrorIndex(start);
            return 0;
        }
        ParsePosition posOffset = new ParsePosition(start + 1);
        int offset = parseAsciiOffsetFields(text, posOffset, DEFAULT_GMT_OFFSET_SEP, OffsetFields.H, OffsetFields.HMS);
        if (posOffset.getErrorIndex() == -1 && (extendedOnly ^ 1) != 0 && posOffset.getIndex() - start <= 3) {
            ParsePosition posBasic = new ParsePosition(start + 1);
            int tmpOffset = parseAbuttingAsciiOffsetFields(text, posBasic, OffsetFields.H, OffsetFields.HMS, false);
            if (posBasic.getErrorIndex() == -1 && posBasic.getIndex() > posOffset.getIndex()) {
                offset = tmpOffset;
                posOffset.setIndex(posBasic.getIndex());
            }
        }
        if (posOffset.getErrorIndex() != -1) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(posOffset.getIndex());
        if (hasDigitOffset != null) {
            hasDigitOffset.value = Boolean.valueOf(true);
        }
        return sign * offset;
    }

    private static int parseAbuttingAsciiOffsetFields(String text, ParsePosition pos, OffsetFields minFields, OffsetFields maxFields, boolean fixedHourWidth) {
        int start = pos.getIndex();
        int minDigits = ((minFields.ordinal() + 1) * 2) - (fixedHourWidth ? 0 : 1);
        int[] digits = new int[((maxFields.ordinal() + 1) * 2)];
        int numDigits = 0;
        int idx = start;
        while (numDigits < digits.length && idx < text.length()) {
            int digit = ASCII_DIGITS.indexOf(text.charAt(idx));
            if (digit < 0) {
                break;
            }
            digits[numDigits] = digit;
            numDigits++;
            idx++;
        }
        if (fixedHourWidth && (numDigits & 1) != 0) {
            numDigits--;
        }
        if (numDigits < minDigits) {
            pos.setErrorIndex(start);
            return 0;
        }
        int hour = 0;
        int min = 0;
        int sec = 0;
        boolean bParsed = false;
        while (numDigits >= minDigits) {
            switch (numDigits) {
                case 1:
                    hour = digits[0];
                    break;
                case 2:
                    hour = (digits[0] * 10) + digits[1];
                    break;
                case 3:
                    hour = digits[0];
                    min = (digits[1] * 10) + digits[2];
                    break;
                case 4:
                    hour = (digits[0] * 10) + digits[1];
                    min = (digits[2] * 10) + digits[3];
                    break;
                case 5:
                    hour = digits[0];
                    min = (digits[1] * 10) + digits[2];
                    sec = (digits[3] * 10) + digits[4];
                    break;
                case 6:
                    hour = (digits[0] * 10) + digits[1];
                    min = (digits[2] * 10) + digits[3];
                    sec = (digits[4] * 10) + digits[5];
                    break;
            }
            if (hour > 23 || min > 59 || sec > 59) {
                numDigits -= fixedHourWidth ? 2 : 1;
                sec = 0;
                min = 0;
                hour = 0;
            } else {
                bParsed = true;
                if (bParsed) {
                    pos.setErrorIndex(start);
                    return 0;
                }
                pos.setIndex(start + numDigits);
                return ((((hour * 60) + min) * 60) + sec) * 1000;
            }
        }
        if (bParsed) {
            pos.setIndex(start + numDigits);
            return ((((hour * 60) + min) * 60) + sec) * 1000;
        }
        pos.setErrorIndex(start);
        return 0;
    }

    private static int parseAsciiOffsetFields(String text, ParsePosition pos, char sep, OffsetFields minFields, OffsetFields maxFields) {
        int start = pos.getIndex();
        int[] fieldVal = new int[]{0, 0, 0};
        int[] fieldLen = new int[]{0, -1, -1};
        int fieldIdx = 0;
        for (int idx = start; idx < text.length() && fieldIdx <= maxFields.ordinal(); idx++) {
            char c = text.charAt(idx);
            if (c != sep) {
                if (fieldLen[fieldIdx] == -1) {
                    break;
                }
                int digit = ASCII_DIGITS.indexOf(c);
                if (digit < 0) {
                    break;
                }
                fieldVal[fieldIdx] = (fieldVal[fieldIdx] * 10) + digit;
                fieldLen[fieldIdx] = fieldLen[fieldIdx] + 1;
                if (fieldLen[fieldIdx] >= 2) {
                    fieldIdx++;
                }
            } else if (fieldIdx != 0) {
                if (fieldLen[fieldIdx] != -1) {
                    break;
                }
                fieldLen[fieldIdx] = 0;
            } else if (fieldLen[0] == 0) {
                break;
            } else {
                fieldIdx++;
            }
        }
        int offset = 0;
        int parsedLen = 0;
        OffsetFields parsedFields = null;
        if (fieldLen[0] != 0) {
            if (fieldVal[0] > 23) {
                offset = (fieldVal[0] / 10) * 3600000;
                parsedFields = OffsetFields.H;
                parsedLen = 1;
            } else {
                offset = fieldVal[0] * 3600000;
                parsedLen = fieldLen[0];
                parsedFields = OffsetFields.H;
                if (fieldLen[1] == 2 && fieldVal[1] <= 59) {
                    offset += fieldVal[1] * 60000;
                    parsedLen += fieldLen[1] + 1;
                    parsedFields = OffsetFields.HM;
                    if (fieldLen[2] == 2 && fieldVal[2] <= 59) {
                        offset += fieldVal[2] * 1000;
                        parsedLen += fieldLen[2] + 1;
                        parsedFields = OffsetFields.HMS;
                    }
                }
            }
        }
        if (parsedFields == null || parsedFields.ordinal() < minFields.ordinal()) {
            pos.setErrorIndex(start);
            return 0;
        }
        pos.setIndex(start + parsedLen);
        return offset;
    }

    private static String parseZoneID(String text, ParsePosition pos) {
        if (ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (ZONE_ID_TRIE == null) {
                    TextTrieMap<String> trie = new TextTrieMap(true);
                    for (String id : TimeZone.getAvailableIDs()) {
                        trie.put(id, id);
                    }
                    ZONE_ID_TRIE = trie;
                }
            }
        }
        int[] matchLen = new int[]{0};
        Iterator<String> itr = ZONE_ID_TRIE.get(text, pos.getIndex(), matchLen);
        if (itr != null) {
            String resolvedID = (String) itr.next();
            pos.setIndex(pos.getIndex() + matchLen[0]);
            return resolvedID;
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private static String parseShortZoneID(String text, ParsePosition pos) {
        if (SHORT_ZONE_ID_TRIE == null) {
            synchronized (TimeZoneFormat.class) {
                if (SHORT_ZONE_ID_TRIE == null) {
                    TextTrieMap<String> trie = new TextTrieMap(true);
                    for (String id : TimeZone.getAvailableIDs(SystemTimeZoneType.CANONICAL, null, null)) {
                        String shortID = ZoneMeta.getShortID(id);
                        if (shortID != null) {
                            trie.put(shortID, id);
                        }
                    }
                    trie.put(UNKNOWN_SHORT_ZONE_ID, "Etc/Unknown");
                    SHORT_ZONE_ID_TRIE = trie;
                }
            }
        }
        int[] matchLen = new int[]{0};
        Iterator<String> itr = SHORT_ZONE_ID_TRIE.get(text, pos.getIndex(), matchLen);
        if (itr != null) {
            String resolvedID = (String) itr.next();
            pos.setIndex(pos.getIndex() + matchLen[0]);
            return resolvedID;
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
    }

    private String parseExemplarLocation(String text, ParsePosition pos) {
        int startIdx = pos.getIndex();
        int parsedPos = -1;
        String tzID = null;
        Collection<MatchInfo> exemplarMatches = this._tznames.find(text, startIdx, EnumSet.of(NameType.EXEMPLAR_LOCATION));
        if (exemplarMatches != null) {
            MatchInfo exemplarMatch = null;
            for (MatchInfo match : exemplarMatches) {
                if (match.matchLength() + startIdx > parsedPos) {
                    exemplarMatch = match;
                    parsedPos = startIdx + match.matchLength();
                }
            }
            if (exemplarMatch != null) {
                tzID = getTimeZoneID(exemplarMatch.tzID(), exemplarMatch.mzID());
                pos.setIndex(parsedPos);
            }
        }
        if (tzID == null) {
            pos.setErrorIndex(startIdx);
        }
        return tzID;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        PutField fields = oos.putFields();
        fields.put("_locale", this._locale);
        fields.put("_tznames", this._tznames);
        fields.put("_gmtPattern", this._gmtPattern);
        fields.put("_gmtOffsetPatterns", this._gmtOffsetPatterns);
        fields.put("_gmtOffsetDigits", this._gmtOffsetDigits);
        fields.put("_gmtZeroFormat", this._gmtZeroFormat);
        fields.put("_parseAllStyles", this._parseAllStyles);
        oos.writeFields();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        GetField fields = ois.readFields();
        this._locale = (ULocale) fields.get("_locale", null);
        if (this._locale == null) {
            throw new InvalidObjectException("Missing field: locale");
        }
        this._tznames = (TimeZoneNames) fields.get("_tznames", null);
        if (this._tznames == null) {
            throw new InvalidObjectException("Missing field: tznames");
        }
        this._gmtPattern = (String) fields.get("_gmtPattern", null);
        if (this._gmtPattern == null) {
            throw new InvalidObjectException("Missing field: gmtPattern");
        }
        String[] tmpGmtOffsetPatterns = (String[]) fields.get("_gmtOffsetPatterns", null);
        if (tmpGmtOffsetPatterns == null) {
            throw new InvalidObjectException("Missing field: gmtOffsetPatterns");
        } else if (tmpGmtOffsetPatterns.length < 4) {
            throw new InvalidObjectException("Incompatible field: gmtOffsetPatterns");
        } else {
            this._gmtOffsetPatterns = new String[6];
            if (tmpGmtOffsetPatterns.length == 4) {
                for (int i = 0; i < 4; i++) {
                    this._gmtOffsetPatterns[i] = tmpGmtOffsetPatterns[i];
                }
                this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.POSITIVE_HM.ordinal()]);
                this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_H.ordinal()] = truncateOffsetPattern(this._gmtOffsetPatterns[GMTOffsetPatternType.NEGATIVE_HM.ordinal()]);
            } else {
                this._gmtOffsetPatterns = tmpGmtOffsetPatterns;
            }
            this._gmtOffsetDigits = (String[]) fields.get("_gmtOffsetDigits", null);
            if (this._gmtOffsetDigits == null) {
                throw new InvalidObjectException("Missing field: gmtOffsetDigits");
            } else if (this._gmtOffsetDigits.length != 10) {
                throw new InvalidObjectException("Incompatible field: gmtOffsetDigits");
            } else {
                this._gmtZeroFormat = (String) fields.get("_gmtZeroFormat", null);
                if (this._gmtZeroFormat == null) {
                    throw new InvalidObjectException("Missing field: gmtZeroFormat");
                }
                this._parseAllStyles = fields.get("_parseAllStyles", false);
                if (fields.defaulted("_parseAllStyles")) {
                    throw new InvalidObjectException("Missing field: parseAllStyles");
                }
                if (this._tznames instanceof TimeZoneNamesImpl) {
                    this._tznames = TimeZoneNames.getInstance(this._locale);
                    this._gnames = null;
                } else {
                    this._gnames = new TimeZoneGenericNames(this._locale, this._tznames);
                }
                initGMTPattern(this._gmtPattern);
                initGMTOffsetPatterns(this._gmtOffsetPatterns);
            }
        }
    }

    public boolean isFrozen() {
        return this._frozen;
    }

    public TimeZoneFormat freeze() {
        this._frozen = true;
        return this;
    }

    public TimeZoneFormat cloneAsThawed() {
        TimeZoneFormat copy = (TimeZoneFormat) super.clone();
        copy._frozen = false;
        return copy;
    }
}
