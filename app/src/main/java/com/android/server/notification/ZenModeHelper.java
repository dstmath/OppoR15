package com.android.server.notification;

import android.app.AppOpsManager;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManagerInternal;
import android.media.VolumePolicy;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings.Global;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.service.notification.ZenModeConfig.ZenRule;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.logging.MetricsLogger;
import com.android.server.LocalServices;
import com.android.server.notification.ManagedServices.Config;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class ZenModeHelper {
    static boolean DEBUG = SystemProperties.getBoolean("persist.sys.assert.panic", false);
    private static final int RULE_INSTANCE_GRACE_PERIOD = 259200000;
    public static final long SUPPRESSED_EFFECT_ALL = 3;
    public static final long SUPPRESSED_EFFECT_CALLS = 2;
    public static final long SUPPRESSED_EFFECT_NOTIFICATIONS = 1;
    static final String TAG = "ZenModeHelper";
    private final String EVENTS_DEFAULT_RULE = "EVENTS_DEFAULT_RULE";
    private final String SCHEDULED_DEFAULT_RULE_1 = "SCHEDULED_DEFAULT_RULE_1";
    private final String SCHEDULED_DEFAULT_RULE_2 = "SCHEDULED_DEFAULT_RULE_2";
    private final AppOpsManager mAppOps;
    private AudioManagerInternal mAudioManager;
    private final ArrayList<Callback> mCallbacks = new ArrayList();
    private final ZenModeConditions mConditions;
    protected ZenModeConfig mConfig;
    private final SparseArray<ZenModeConfig> mConfigs = new SparseArray();
    private final Context mContext;
    protected ZenModeConfig mDefaultConfig;
    protected String mDefaultRuleEventsName;
    protected final ArrayList<String> mDefaultRuleIds = new ArrayList();
    protected String mDefaultRuleWeekendsName;
    protected String mDefaultRuleWeeknightsName;
    private final ZenModeFiltering mFiltering;
    private final H mHandler;
    private final Metrics mMetrics = new Metrics();
    protected PackageManager mPm;
    private final RingerModeDelegate mRingerModeDelegate = new RingerModeDelegate();
    private final Config mServiceConfig;
    private String mSetConfigReason = "";
    private final SettingsObserver mSettingsObserver;
    private long mSuppressedEffects;
    private int mUser = 0;
    private int mZenMode;

    public static class Callback {
        void onConfigChanged() {
        }

        void onZenModeChanged() {
        }

        void onPolicyChanged() {
        }
    }

    private final class H extends Handler {
        private static final long METRICS_PERIOD_MS = 21600000;
        private static final int MSG_APPLY_CONFIG = 4;
        private static final int MSG_DISPATCH = 1;
        private static final int MSG_METRICS = 2;

        private final class ConfigMessageData {
            public final ZenModeConfig config;
            public final String reason;
            public final boolean setRingerMode;

            ConfigMessageData(ZenModeConfig config, String reason, boolean setRingerMode) {
                this.config = config;
                this.reason = reason;
                this.setRingerMode = setRingerMode;
            }
        }

        /* synthetic */ H(ZenModeHelper this$0, Looper looper, H -this2) {
            this(looper);
        }

        private H(Looper looper) {
            super(looper);
        }

        private void postDispatchOnZenModeChanged() {
            removeMessages(1);
            sendEmptyMessage(1);
        }

        private void postMetricsTimer() {
            removeMessages(2);
            sendEmptyMessageDelayed(2, METRICS_PERIOD_MS);
        }

        private void postApplyConfig(ZenModeConfig config, String reason, boolean setRingerMode) {
            sendMessage(obtainMessage(4, new ConfigMessageData(config, reason, setRingerMode)));
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ZenModeHelper.this.dispatchOnZenModeChanged();
                    return;
                case 2:
                    ZenModeHelper.this.mMetrics.emit();
                    return;
                case 4:
                    ConfigMessageData applyConfigData = msg.obj;
                    ZenModeHelper.this.applyConfig(applyConfigData.config, applyConfigData.reason, applyConfigData.setRingerMode);
                    return;
                default:
                    return;
            }
        }
    }

    private final class Metrics extends Callback {
        private static final String COUNTER_PREFIX = "dnd_mode_";
        private static final long MINIMUM_LOG_PERIOD_MS = 60000;
        private long mBeginningMs;
        private int mPreviousZenMode;

        /* synthetic */ Metrics(ZenModeHelper this$0, Metrics -this1) {
            this();
        }

        private Metrics() {
            this.mPreviousZenMode = -1;
            this.mBeginningMs = 0;
        }

        void onZenModeChanged() {
            emit();
        }

        private void emit() {
            ZenModeHelper.this.mHandler.postMetricsTimer();
            long now = SystemClock.elapsedRealtime();
            long since = now - this.mBeginningMs;
            if (this.mPreviousZenMode != ZenModeHelper.this.mZenMode || since > 60000) {
                if (this.mPreviousZenMode != -1) {
                    MetricsLogger.count(ZenModeHelper.this.mContext, COUNTER_PREFIX + this.mPreviousZenMode, (int) since);
                }
                this.mPreviousZenMode = ZenModeHelper.this.mZenMode;
                this.mBeginningMs = now;
            }
        }
    }

    private final class RingerModeDelegate implements android.media.AudioManagerInternal.RingerModeDelegate {
        /* synthetic */ RingerModeDelegate(ZenModeHelper this$0, RingerModeDelegate -this1) {
            this();
        }

        private RingerModeDelegate() {
        }

        public String toString() {
            return ZenModeHelper.TAG;
        }

        public int onSetRingerModeInternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeExternal, VolumePolicy policy) {
            boolean isChange = ringerModeOld != ringerModeNew;
            int ringerModeExternalOut = ringerModeNew;
            int newZen = -1;
            switch (ringerModeNew) {
                case 0:
                    if (isChange && policy.doNotDisturbWhenSilent) {
                        if (!(ZenModeHelper.this.mZenMode == 2 || ZenModeHelper.this.mZenMode == 3)) {
                            newZen = 3;
                        }
                        ZenModeHelper.this.setPreviousRingerModeSetting(Integer.valueOf(ringerModeOld));
                        break;
                    }
                case 1:
                case 2:
                    if (!isChange || ringerModeOld != 0 || (ZenModeHelper.this.mZenMode != 2 && ZenModeHelper.this.mZenMode != 3)) {
                        if (!(ZenModeHelper.this.mZenMode == 0 || ZenModeHelper.this.mZenMode == 1)) {
                            ringerModeExternalOut = 0;
                            if (ringerModeOld == 2 && (isChange ^ 1) != 0) {
                                ringerModeExternalOut = 2;
                                newZen = 0;
                                break;
                            }
                        }
                    }
                    newZen = 0;
                    break;
                    break;
            }
            if (newZen != -1) {
                ZenModeHelper.this.setManualZenMode(newZen, null, "ringerModeInternal", null, false);
            }
            if (!(!isChange && newZen == -1 && ringerModeExternal == ringerModeExternalOut)) {
                ZenLog.traceSetRingerModeInternal(ringerModeOld, ringerModeNew, caller, ringerModeExternal, ringerModeExternalOut);
            }
            return ringerModeExternalOut;
        }

        public int onSetRingerModeExternal(int ringerModeOld, int ringerModeNew, String caller, int ringerModeInternal, VolumePolicy policy) {
            int ringerModeInternalOut = ringerModeNew;
            boolean isChange = ringerModeOld != ringerModeNew;
            if (ringerModeInternal == 1) {
            }
            int newZen = -1;
            switch (ringerModeNew) {
                case 0:
                    if (isChange) {
                        if (ZenModeHelper.this.mZenMode == 0 || ZenModeHelper.this.mZenMode == 1) {
                            newZen = 3;
                            break;
                        }
                    }
                    ringerModeInternalOut = ringerModeInternal;
                    break;
                case 1:
                case 2:
                    if (!(ZenModeHelper.this.mZenMode == 0 || ZenModeHelper.this.mZenMode == 1)) {
                        newZen = 0;
                        break;
                    }
            }
            if (newZen != -1) {
                ZenModeHelper.this.setManualZenMode(newZen, null, "ringerModeExternal", caller, false);
            }
            ZenLog.traceSetRingerModeExternal(ringerModeOld, ringerModeNew, caller, ringerModeInternal, ringerModeInternalOut);
            return ringerModeInternalOut;
        }

        public boolean canVolumeDownEnterSilent() {
            return ZenModeHelper.this.mZenMode == 0;
        }

        public int getRingerModeAffectedStreams(int streams) {
            streams |= 38;
            if (ZenModeHelper.this.mZenMode == 2) {
                return streams | 24;
            }
            return streams & -25;
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private final Uri ZEN_MODE = Global.getUriFor("zen_mode");

        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void observe() {
            ZenModeHelper.this.mContext.getContentResolver().registerContentObserver(this.ZEN_MODE, false, this);
            update(null);
        }

        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            if (this.ZEN_MODE.equals(uri) && ZenModeHelper.this.mZenMode != ZenModeHelper.this.getZenModeSetting()) {
                if (ZenModeHelper.DEBUG) {
                    Log.d(ZenModeHelper.TAG, "Fixing zen mode setting");
                }
                ZenModeHelper.this.setZenModeSetting(ZenModeHelper.this.mZenMode);
            }
        }
    }

    public ZenModeHelper(Context context, Looper looper, ConditionProviders conditionProviders) {
        this.mContext = context;
        this.mHandler = new H(this, looper, null);
        addCallback(this.mMetrics);
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mDefaultConfig = new ZenModeConfig();
        this.mDefaultRuleWeeknightsName = this.mContext.getResources().getString(17041159);
        this.mDefaultRuleWeekendsName = this.mContext.getResources().getString(17041158);
        this.mDefaultRuleEventsName = this.mContext.getResources().getString(17041157);
        setDefaultZenRules(this.mContext);
        this.mConfig = this.mDefaultConfig;
        this.mConfigs.put(0, this.mConfig);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSettingsObserver.observe();
        this.mFiltering = new ZenModeFiltering(this.mContext);
        this.mConditions = new ZenModeConditions(this, conditionProviders);
        this.mServiceConfig = conditionProviders.getConfig();
    }

    public Looper getLooper() {
        return this.mHandler.getLooper();
    }

    public String toString() {
        return TAG;
    }

    public boolean matchesCallFilter(UserHandle userHandle, Bundle extras, ValidateNotificationPeople validator, int contactsTimeoutMs, float timeoutAffinity) {
        boolean matchesCallFilter;
        synchronized (this.mConfig) {
            matchesCallFilter = ZenModeFiltering.matchesCallFilter(this.mContext, this.mZenMode, this.mConfig, userHandle, extras, validator, contactsTimeoutMs, timeoutAffinity);
        }
        return matchesCallFilter;
    }

    public boolean isCall(NotificationRecord record) {
        return this.mFiltering.isCall(record);
    }

    public void recordCaller(NotificationRecord record) {
        this.mFiltering.recordCall(record);
    }

    public boolean shouldIntercept(NotificationRecord record) {
        boolean shouldIntercept;
        synchronized (this.mConfig) {
            shouldIntercept = this.mFiltering.shouldIntercept(this.mZenMode, this.mConfig, record);
        }
        return shouldIntercept;
    }

    public boolean shouldSuppressWhenScreenOff() {
        boolean z;
        synchronized (this.mConfig) {
            z = this.mConfig.allowWhenScreenOff ^ 1;
        }
        return z;
    }

    public boolean shouldSuppressWhenScreenOn() {
        boolean z;
        synchronized (this.mConfig) {
            z = this.mConfig.allowWhenScreenOn ^ 1;
        }
        return z;
    }

    public void addCallback(Callback callback) {
        this.mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        this.mCallbacks.remove(callback);
    }

    public void initZenMode() {
        if (DEBUG) {
            Log.d(TAG, "initZenMode");
        }
        evaluateZenMode("init", true);
    }

    public void onSystemReady() {
        if (DEBUG) {
            Log.d(TAG, "onSystemReady");
        }
        this.mAudioManager = (AudioManagerInternal) LocalServices.getService(AudioManagerInternal.class);
        if (this.mAudioManager != null) {
            this.mAudioManager.setRingerModeDelegate(this.mRingerModeDelegate);
        }
        this.mPm = this.mContext.getPackageManager();
        this.mHandler.postMetricsTimer();
        cleanUpZenRules();
        evaluateZenMode("onSystemReady", true);
    }

    public void onUserSwitched(int user) {
        loadConfigForUser(user, "onUserSwitched");
    }

    public void onUserRemoved(int user) {
        if (user >= 0) {
            if (DEBUG) {
                Log.d(TAG, "onUserRemoved u=" + user);
            }
            this.mConfigs.remove(user);
        }
    }

    public void onUserUnlocked(int user) {
        loadConfigForUser(user, "onUserUnlocked");
    }

    private void loadConfigForUser(int user, String reason) {
        if (this.mUser != user && user >= 0) {
            this.mUser = user;
            if (DEBUG) {
                Log.d(TAG, reason + " u=" + user);
            }
            ZenModeConfig config = (ZenModeConfig) this.mConfigs.get(user);
            if (config == null) {
                if (DEBUG) {
                    Log.d(TAG, reason + " generating default config for user " + user);
                }
                config = this.mDefaultConfig.copy();
                config.user = user;
            }
            synchronized (this.mConfig) {
                setConfigLocked(config, reason);
            }
            cleanUpZenRules();
        }
    }

    public int getZenModeListenerInterruptionFilter() {
        return NotificationManager.zenModeToInterruptionFilter(this.mContext, this.mZenMode);
    }

    public void requestFromListener(ComponentName name, int filter) {
        int newZen = NotificationManager.zenModeFromInterruptionFilter(this.mContext, filter, -1);
        if (newZen != -1) {
            String packageName;
            String flattenToShortString;
            if (name != null) {
                packageName = name.getPackageName();
            } else {
                packageName = null;
            }
            StringBuilder append = new StringBuilder().append("listener:");
            if (name != null) {
                flattenToShortString = name.flattenToShortString();
            } else {
                flattenToShortString = null;
            }
            setManualZenMode(newZen, null, packageName, append.append(flattenToShortString).toString());
        }
    }

    public void setSuppressedEffects(long suppressedEffects) {
        if (this.mSuppressedEffects != suppressedEffects) {
            this.mSuppressedEffects = suppressedEffects;
            applyRestrictions();
        }
    }

    public long getSuppressedEffects() {
        return this.mSuppressedEffects;
    }

    public int getZenMode() {
        return this.mZenMode;
    }

    public List<ZenRule> getZenRules() {
        List<ZenRule> rules = new ArrayList();
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return rules;
            }
            for (ZenRule rule : this.mConfig.automaticRules.values()) {
                if (canManageAutomaticZenRule(rule)) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AutomaticZenRule getAutomaticZenRule(String id) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return null;
            }
            ZenRule rule = (ZenRule) this.mConfig.automaticRules.get(id);
        }
    }

    public String addAutomaticZenRule(AutomaticZenRule automaticZenRule, String reason) {
        String str;
        if (!isSystemRule(automaticZenRule)) {
            ServiceInfo owner = getServiceInfo(automaticZenRule.getOwner());
            if (owner == null) {
                throw new IllegalArgumentException("Owner is not a condition provider service");
            }
            int ruleInstanceLimit = -1;
            if (owner.metaData != null) {
                ruleInstanceLimit = owner.metaData.getInt("android.service.zen.automatic.ruleInstanceLimit", -1);
            }
            if (ruleInstanceLimit > 0 && ruleInstanceLimit < getCurrentInstanceCount(automaticZenRule.getOwner()) + 1) {
                throw new IllegalArgumentException("Rule instance limit exceeded");
            }
        }
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                throw new AndroidRuntimeException("Could not create rule");
            }
            if (DEBUG) {
                Log.d(TAG, "addAutomaticZenRule rule= " + automaticZenRule + " reason=" + reason);
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            ZenRule rule = new ZenRule();
            populateZenRule(automaticZenRule, rule, true);
            newConfig.automaticRules.put(rule.id, rule);
            if (setConfigLocked(newConfig, reason, true)) {
                str = rule.id;
            } else {
                throw new AndroidRuntimeException("Could not create rule");
            }
        }
        return str;
    }

    public boolean updateAutomaticZenRule(String ruleId, AutomaticZenRule automaticZenRule, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            if (DEBUG) {
                Log.d(TAG, "updateAutomaticZenRule zenRule=" + automaticZenRule + " reason=" + reason);
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            if (ruleId == null) {
                throw new IllegalArgumentException("Rule doesn't exist");
            }
            ZenRule rule = (ZenRule) newConfig.automaticRules.get(ruleId);
            if (rule == null || (canManageAutomaticZenRule(rule) ^ 1) != 0) {
                throw new SecurityException("Cannot update rules not owned by your condition provider");
            }
            populateZenRule(automaticZenRule, rule, false);
            newConfig.automaticRules.put(ruleId, rule);
            boolean configLocked = setConfigLocked(newConfig, reason, true);
            return configLocked;
        }
    }

    public boolean removeAutomaticZenRule(String id, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            ZenRule rule = (ZenRule) newConfig.automaticRules.get(id);
            if (rule == null) {
                return false;
            } else if (canManageAutomaticZenRule(rule)) {
                newConfig.automaticRules.remove(id);
                if (DEBUG) {
                    Log.d(TAG, "removeZenRule zenRule=" + id + " reason=" + reason);
                }
                boolean configLocked = setConfigLocked(newConfig, reason, true);
                return configLocked;
            } else {
                throw new SecurityException("Cannot delete rules not owned by your condition provider");
            }
        }
    }

    public boolean removeAutomaticZenRules(String packageName, String reason) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return false;
            }
            ZenModeConfig newConfig = this.mConfig.copy();
            for (int i = newConfig.automaticRules.size() - 1; i >= 0; i--) {
                ZenRule rule = (ZenRule) newConfig.automaticRules.get(newConfig.automaticRules.keyAt(i));
                if (rule.component.getPackageName().equals(packageName) && canManageAutomaticZenRule(rule)) {
                    newConfig.automaticRules.removeAt(i);
                }
            }
            boolean configLocked = setConfigLocked(newConfig, reason, true);
            return configLocked;
        }
    }

    public int getCurrentInstanceCount(ComponentName owner) {
        int count = 0;
        synchronized (this.mConfig) {
            for (ZenRule rule : this.mConfig.automaticRules.values()) {
                if (rule.component != null && rule.component.equals(owner)) {
                    count++;
                }
            }
        }
        return count;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean canManageAutomaticZenRule(ZenRule rule) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == 0 || callingUid == 1000 || this.mContext.checkCallingPermission("android.permission.MANAGE_NOTIFICATIONS") == 0) {
            return true;
        }
        String[] packages = this.mPm.getPackagesForUid(Binder.getCallingUid());
        if (packages != null) {
            for (String equals : packages) {
                if (equals.equals(rule.component.getPackageName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setDefaultZenRules(Context context) {
        this.mDefaultConfig = readDefaultConfig(context.getResources());
        this.mDefaultRuleIds.add("EVENTS_DEFAULT_RULE");
        this.mDefaultRuleIds.add("SCHEDULED_DEFAULT_RULE_1");
        this.mDefaultRuleIds.add("SCHEDULED_DEFAULT_RULE_2");
        appendDefaultRules(this.mDefaultConfig);
    }

    private void appendDefaultRules(ZenModeConfig config) {
        appendDefaultScheduleRules(config);
        appendDefaultEventRules(config);
    }

    private boolean ruleValuesEqual(AutomaticZenRule rule, ZenRule defaultRule) {
        boolean z = false;
        if (rule == null || defaultRule == null) {
            return false;
        }
        if (rule.getInterruptionFilter() == NotificationManager.zenModeToInterruptionFilter(defaultRule.zenMode) && rule.getConditionId().equals(defaultRule.conditionId)) {
            z = rule.getOwner().equals(defaultRule.component);
        }
        return z;
    }

    protected void updateDefaultZenRules() {
        ZenModeConfig configDefaultRules = new ZenModeConfig();
        appendDefaultRules(configDefaultRules);
        for (String ruleId : this.mDefaultRuleIds) {
            AutomaticZenRule currRule = getAutomaticZenRule(ruleId);
            ZenRule defaultRule = (ZenRule) configDefaultRules.automaticRules.get(ruleId);
            if (ruleValuesEqual(currRule, defaultRule) && (defaultRule.name.equals(currRule.getName()) ^ 1) != 0 && canManageAutomaticZenRule(defaultRule)) {
                if (DEBUG) {
                    Slog.d(TAG, "Locale change - updating default zen rule name from " + currRule.getName() + " to " + defaultRule.name);
                }
                AutomaticZenRule defaultAutoRule = createAutomaticZenRule(defaultRule);
                defaultAutoRule.setEnabled(currRule.isEnabled());
                updateAutomaticZenRule(ruleId, defaultAutoRule, "locale changed");
            }
        }
    }

    private boolean isSystemRule(AutomaticZenRule rule) {
        return "android".equals(rule.getOwner().getPackageName());
    }

    private ServiceInfo getServiceInfo(ComponentName owner) {
        Intent queryIntent = new Intent();
        queryIntent.setComponent(owner);
        List<ResolveInfo> installedServices = this.mPm.queryIntentServicesAsUser(queryIntent, 132, UserHandle.getCallingUserId());
        if (installedServices != null) {
            int count = installedServices.size();
            for (int i = 0; i < count; i++) {
                ServiceInfo info = ((ResolveInfo) installedServices.get(i)).serviceInfo;
                if (this.mServiceConfig.bindPermission.equals(info.permission)) {
                    return info;
                }
            }
        }
        return null;
    }

    private void populateZenRule(AutomaticZenRule automaticZenRule, ZenRule rule, boolean isNew) {
        if (isNew) {
            rule.id = ZenModeConfig.newRuleId();
            rule.creationTime = System.currentTimeMillis();
            rule.component = automaticZenRule.getOwner();
        }
        if (rule.enabled != automaticZenRule.isEnabled()) {
            rule.snoozing = false;
        }
        rule.name = automaticZenRule.getName();
        rule.condition = null;
        rule.conditionId = automaticZenRule.getConditionId();
        rule.enabled = automaticZenRule.isEnabled();
        rule.zenMode = NotificationManager.zenModeFromInterruptionFilter(automaticZenRule.getInterruptionFilter(), 0);
    }

    protected AutomaticZenRule createAutomaticZenRule(ZenRule rule) {
        return new AutomaticZenRule(rule.name, rule.component, rule.conditionId, NotificationManager.zenModeToInterruptionFilter(rule.zenMode), rule.enabled, rule.creationTime);
    }

    public void setManualZenMode(int zenMode, Uri conditionId, String caller, String reason) {
        setManualZenMode(zenMode, conditionId, reason, caller, true);
    }

    private void setManualZenMode(int zenMode, Uri conditionId, String reason, String caller, boolean setRingerMode) {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
            } else if (Global.isValidZenMode(zenMode)) {
                if (DEBUG) {
                    Log.d(TAG, "setManualZenMode " + Global.zenModeToString(zenMode) + " conditionId=" + conditionId + " reason=" + reason + " setRingerMode=" + setRingerMode);
                }
                ZenModeConfig newConfig = this.mConfig.copy();
                int totalToggleState = Global.getInt(this.mContext.getContentResolver(), "systemui_important_interruptions", 0);
                if (zenMode == 0 && (totalToggleState == 0 || "com.android.cts.verifier".equals(caller))) {
                    newConfig.manualRule = null;
                    for (ZenRule automaticRule : newConfig.automaticRules.values()) {
                        if (automaticRule.isAutomaticActive()) {
                            automaticRule.snoozing = true;
                        }
                    }
                } else {
                    ZenRule newRule = new ZenRule();
                    newRule.enabled = true;
                    if (zenMode == 0 && totalToggleState == 1) {
                        newRule.zenMode = 1;
                        if (DEBUG) {
                            Log.d(TAG, "setManualZenMode_newRule.zenMode = " + newRule.zenMode);
                        }
                    } else {
                        newRule.zenMode = zenMode;
                    }
                    newRule.conditionId = conditionId;
                    newRule.enabler = caller;
                    newConfig.manualRule = newRule;
                }
                setConfigLocked(newConfig, reason, setRingerMode);
            }
        }
    }

    void dump(ProtoOutputStream proto) {
        proto.write(1168231104513L, this.mZenMode);
        synchronized (this.mConfig) {
            if (this.mConfig.manualRule != null) {
                proto.write(2259152797698L, this.mConfig.manualRule.toString());
            }
            for (ZenRule rule : this.mConfig.automaticRules.values()) {
                if (rule.enabled && rule.condition.state == 1 && (rule.snoozing ^ 1) != 0) {
                    proto.write(2259152797698L, rule.toString());
                }
            }
            proto.write(1159641169925L, this.mConfig.toNotificationPolicy().toString());
            proto.write(1112396529667L, this.mSuppressedEffects);
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("mZenMode=");
        pw.println(Global.zenModeToString(this.mZenMode));
        int N = this.mConfigs.size();
        for (int i = 0; i < N; i++) {
            dump(pw, prefix, "mConfigs[u=" + this.mConfigs.keyAt(i) + "]", (ZenModeConfig) this.mConfigs.valueAt(i));
        }
        pw.print(prefix);
        pw.print("mUser=");
        pw.println(this.mUser);
        synchronized (this.mConfig) {
            dump(pw, prefix, "mConfig", this.mConfig);
        }
        pw.print(prefix);
        pw.print("mSuppressedEffects=");
        pw.println(this.mSuppressedEffects);
        this.mFiltering.dump(pw, prefix);
        this.mConditions.dump(pw, prefix);
    }

    private static void dump(PrintWriter pw, String prefix, String var, ZenModeConfig config) {
        pw.print(prefix);
        pw.print(var);
        pw.print('=');
        if (config == null) {
            pw.println(config);
            return;
        }
        pw.printf("allow(calls=%b,callsFrom=%s,repeatCallers=%b,messages=%b,messagesFrom=%s,events=%b,reminders=%b,whenScreenOff=%b,whenScreenOn=%b)\n", new Object[]{Boolean.valueOf(config.allowCalls), ZenModeConfig.sourceToString(config.allowCallsFrom), Boolean.valueOf(config.allowRepeatCallers), Boolean.valueOf(config.allowMessages), ZenModeConfig.sourceToString(config.allowMessagesFrom), Boolean.valueOf(config.allowEvents), Boolean.valueOf(config.allowReminders), Boolean.valueOf(config.allowWhenScreenOff), Boolean.valueOf(config.allowWhenScreenOn)});
        pw.print(prefix);
        pw.print("  manualRule=");
        pw.println(config.manualRule);
        if (!config.automaticRules.isEmpty()) {
            int N = config.automaticRules.size();
            int i = 0;
            while (i < N) {
                pw.print(prefix);
                pw.print(i == 0 ? "  automaticRules=" : "                 ");
                pw.println(config.automaticRules.valueAt(i));
                i++;
            }
        }
    }

    public void readXml(XmlPullParser parser, boolean forRestore) throws XmlPullParserException, IOException {
        ZenModeConfig config = ZenModeConfig.readXml(parser);
        if (config != null) {
            if (forRestore) {
                if (config.user == 0) {
                    config.manualRule = null;
                    long time = System.currentTimeMillis();
                    if (config.automaticRules != null) {
                        for (ZenRule automaticRule : config.automaticRules.values()) {
                            automaticRule.snoozing = false;
                            automaticRule.condition = null;
                            automaticRule.creationTime = time;
                        }
                    }
                } else {
                    return;
                }
            }
            if (DEBUG) {
                Log.d(TAG, "readXml");
            }
            synchronized (this.mConfig) {
                setConfigLocked(config, "readXml");
            }
        }
    }

    public void writeXml(XmlSerializer out, boolean forBackup) throws IOException {
        int N = this.mConfigs.size();
        int i = 0;
        while (i < N) {
            if (!forBackup || this.mConfigs.keyAt(i) == 0) {
                ((ZenModeConfig) this.mConfigs.valueAt(i)).writeXml(out);
            }
            i++;
        }
    }

    public Policy getNotificationPolicy() {
        return getNotificationPolicy(this.mConfig);
    }

    private static Policy getNotificationPolicy(ZenModeConfig config) {
        return config == null ? null : config.toNotificationPolicy();
    }

    public void setNotificationPolicy(Policy policy) {
        if (policy != null && this.mConfig != null) {
            synchronized (this.mConfig) {
                ZenModeConfig newConfig = this.mConfig.copy();
                newConfig.applyNotificationPolicy(policy);
                setConfigLocked(newConfig, "setNotificationPolicy");
            }
        }
    }

    private void cleanUpZenRules() {
        long currentTime = System.currentTimeMillis();
        synchronized (this.mConfig) {
            ZenModeConfig newConfig = this.mConfig.copy();
            if (newConfig.automaticRules != null) {
                for (int i = newConfig.automaticRules.size() - 1; i >= 0; i--) {
                    ZenRule rule = (ZenRule) newConfig.automaticRules.get(newConfig.automaticRules.keyAt(i));
                    if (259200000 < currentTime - rule.creationTime) {
                        try {
                            this.mPm.getPackageInfo(rule.component.getPackageName(), DumpState.DUMP_CHANGES);
                        } catch (NameNotFoundException e) {
                            newConfig.automaticRules.removeAt(i);
                        }
                    }
                }
            }
            setConfigLocked(newConfig, "cleanUpZenRules");
        }
    }

    public ZenModeConfig getConfig() {
        ZenModeConfig copy;
        synchronized (this.mConfig) {
            copy = this.mConfig.copy();
        }
        return copy;
    }

    public boolean setConfigLocked(ZenModeConfig config, String reason) {
        return setConfigLocked(config, reason, true);
    }

    public void setConfig(ZenModeConfig config, String reason) {
        synchronized (this.mConfig) {
            setConfigLocked(config, reason);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean setConfigLocked(ZenModeConfig config, String reason, boolean setRingerMode) {
        long identity = Binder.clearCallingIdentity();
        if (config != null) {
            try {
                if ((config.isValid() ^ 1) == 0) {
                    if (config.user != this.mUser) {
                        this.mConfigs.put(config.user, config);
                        if (DEBUG) {
                            Log.d(TAG, "setConfigLocked: store config for user " + config.user);
                        }
                        Binder.restoreCallingIdentity(identity);
                        return true;
                    }
                    this.mConditions.evaluateConfig(config, false);
                    this.mConfigs.put(config.user, config);
                    if (DEBUG) {
                        Log.d(TAG, "setConfigLocked reason=" + reason, new Throwable());
                    }
                    this.mSetConfigReason = reason;
                    ZenLog.traceConfig(reason, this.mConfig, config);
                    boolean policyChanged = Objects.equals(getNotificationPolicy(this.mConfig), getNotificationPolicy(config)) ^ 1;
                    if (!config.equals(this.mConfig)) {
                        dispatchOnConfigChanged();
                    }
                    if (policyChanged) {
                        dispatchOnPolicyChanged();
                    }
                    this.mConfig = config;
                    this.mHandler.postApplyConfig(config, reason, setRingerMode);
                    Binder.restoreCallingIdentity(identity);
                    return true;
                }
            } catch (SecurityException e) {
                Log.wtf(TAG, "Invalid rule in config", e);
                return false;
            } catch (Throwable th) {
                Binder.restoreCallingIdentity(identity);
            }
        }
        Log.w(TAG, "Invalid config in setConfigLocked; " + config);
        Binder.restoreCallingIdentity(identity);
        return false;
    }

    private void applyConfig(ZenModeConfig config, String reason, boolean setRingerMode) {
        Global.putString(this.mContext.getContentResolver(), "zen_mode_config_etag", Integer.toString(config.hashCode()));
        if (!evaluateZenMode(reason, setRingerMode)) {
            applyRestrictions();
        }
        this.mConditions.evaluateConfig(config, true);
    }

    private int getZenModeSetting() {
        return Global.getInt(this.mContext.getContentResolver(), "zen_mode", 0);
    }

    private void setZenModeSetting(int zen) {
        Global.putInt(this.mContext.getContentResolver(), "zen_mode", zen);
    }

    private int getPreviousRingerModeSetting() {
        return Global.getInt(this.mContext.getContentResolver(), "zen_mode_ringer_level", 2);
    }

    private void setPreviousRingerModeSetting(Integer previousRingerLevel) {
        String str = null;
        ContentResolver contentResolver = this.mContext.getContentResolver();
        String str2 = "zen_mode_ringer_level";
        if (previousRingerLevel != null) {
            str = Integer.toString(previousRingerLevel.intValue());
        }
        Global.putString(contentResolver, str2, str);
    }

    private boolean evaluateZenMode(String reason, boolean setRingerMode) {
        if (DEBUG) {
            Log.d(TAG, "evaluateZenMode");
        }
        int zenBefore = this.mZenMode;
        int zen = computeZenMode();
        ZenLog.traceSetZenMode(zen, reason);
        this.mZenMode = zen;
        int totalToggleState = Global.getInt(this.mContext.getContentResolver(), "systemui_important_interruptions", 0);
        if (DEBUG) {
            Log.d(TAG, "evaluateZenMode_mZenMode = " + this.mZenMode + ", totalToggleState = " + totalToggleState);
        }
        if (!(totalToggleState == this.mZenMode || this.mZenMode == 2 || this.mZenMode == 3)) {
            Global.putInt(this.mContext.getContentResolver(), "systemui_important_interruptions", this.mZenMode);
            if (DEBUG) {
                Log.d(TAG, "evaluateZenMode_putInt_mZenMode = " + this.mZenMode);
            }
        }
        updateRingerModeAffectedStreams();
        setZenModeSetting(this.mZenMode);
        if (setRingerMode) {
            applyZenToRingerMode();
        }
        applyRestrictions();
        if (zen != zenBefore) {
            this.mHandler.postDispatchOnZenModeChanged();
        }
        return true;
    }

    private void updateRingerModeAffectedStreams() {
        if (this.mAudioManager != null) {
            this.mAudioManager.updateRingerModeAffectedStreamsInternal();
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int computeZenMode() {
        synchronized (this.mConfig) {
            if (this.mConfig == null) {
                return 0;
            } else if (this.mConfig.manualRule != null) {
                int i = this.mConfig.manualRule.zenMode;
                return i;
            } else {
                int manualToggleState = Global.getInt(this.mContext.getContentResolver(), "systemui_important_interruptions", 0);
                Log.d(TAG, "computeZenMode, manualToggleState = " + manualToggleState + ", mSetConfigReason = " + this.mSetConfigReason);
                if (this.mSetConfigReason == null || (this.mSetConfigReason.isEmpty() ^ 1) == 0 || !this.mSetConfigReason.equals("readXml") || manualToggleState != 0) {
                    int zen = 0;
                    for (ZenRule automaticRule : this.mConfig.automaticRules.values()) {
                        if (automaticRule.isAutomaticActive() && zenSeverity(automaticRule.zenMode) > zenSeverity(zen)) {
                            zen = automaticRule.zenMode;
                        }
                    }
                    return zen;
                } else if (DEBUG) {
                    Log.d(TAG, "if the Reason is readXml and state is off, then return Global.ZEN_MODE_OFF");
                }
            }
        }
    }

    private void applyRestrictions() {
        boolean zen = this.mZenMode != 0;
        boolean muteNotifications = (this.mSuppressedEffects & 1) != 0;
        boolean muteCalls = (!zen || (this.mConfig.allowCalls ^ 1) == 0 || (this.mConfig.allowRepeatCallers ^ 1) == 0) ? (this.mSuppressedEffects & 2) != 0 : true;
        boolean muteEverything = this.mZenMode == 2;
        for (int usage : AudioAttributes.SDK_USAGES) {
            int suppressionBehavior = AudioAttributes.SUPPRESSIBLE_USAGES.get(usage);
            if (suppressionBehavior == 3) {
                applyRestrictions(false, usage);
            } else if (suppressionBehavior == 1) {
                applyRestrictions(!muteNotifications ? muteEverything : true, usage);
            } else if (suppressionBehavior == 2) {
                applyRestrictions(!muteCalls ? muteEverything : true, usage);
            } else {
                applyRestrictions(muteEverything, usage);
            }
        }
    }

    private void applyRestrictions(boolean mute, int usage) {
        int i = 1;
        this.mAppOps.setRestriction(3, usage, mute ? 1 : 0, null);
        AppOpsManager appOpsManager = this.mAppOps;
        if (!mute) {
            i = 0;
        }
        appOpsManager.setRestriction(28, usage, i, null);
    }

    private void applyZenToRingerMode() {
        if (this.mAudioManager != null) {
            int ringerModeInternal = this.mAudioManager.getRingerModeInternal();
            int newRingerModeInternal = ringerModeInternal;
            switch (this.mZenMode) {
                case 0:
                case 1:
                    if (ringerModeInternal == 0) {
                        newRingerModeInternal = getPreviousRingerModeSetting();
                        setPreviousRingerModeSetting(null);
                        break;
                    }
                    break;
                case 2:
                case 3:
                    if (ringerModeInternal != 0) {
                        setPreviousRingerModeSetting(Integer.valueOf(ringerModeInternal));
                        newRingerModeInternal = 0;
                        break;
                    }
                    break;
            }
            if (newRingerModeInternal != -1) {
                this.mAudioManager.setRingerModeInternal(newRingerModeInternal, TAG);
            }
        }
    }

    private void dispatchOnConfigChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onConfigChanged();
        }
    }

    private void dispatchOnPolicyChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onPolicyChanged();
        }
    }

    private void dispatchOnZenModeChanged() {
        for (Callback callback : this.mCallbacks) {
            callback.onZenModeChanged();
        }
    }

    private ZenModeConfig readDefaultConfig(Resources resources) {
        AutoCloseable autoCloseable = null;
        try {
            autoCloseable = resources.getXml(18284550);
            while (autoCloseable.next() != 1) {
                ZenModeConfig config = ZenModeConfig.readXml(autoCloseable);
                if (config != null) {
                    return config;
                }
            }
            IoUtils.closeQuietly(autoCloseable);
        } catch (Exception e) {
            Log.w(TAG, "Error reading default zen mode config from resource", e);
        } finally {
            IoUtils.closeQuietly(autoCloseable);
        }
        return new ZenModeConfig();
    }

    private void appendDefaultScheduleRules(ZenModeConfig config) {
        if (config != null) {
            ScheduleInfo weeknights = new ScheduleInfo();
            weeknights.days = ZenModeConfig.WEEKNIGHT_DAYS;
            weeknights.startHour = 22;
            weeknights.endHour = 7;
            ZenRule rule1 = new ZenRule();
            rule1.enabled = false;
            rule1.name = this.mDefaultRuleWeeknightsName;
            rule1.conditionId = ZenModeConfig.toScheduleConditionId(weeknights);
            rule1.zenMode = 3;
            rule1.component = ScheduleConditionProvider.COMPONENT;
            rule1.id = "SCHEDULED_DEFAULT_RULE_1";
            rule1.creationTime = System.currentTimeMillis();
            config.automaticRules.put(rule1.id, rule1);
        }
    }

    private void appendDefaultEventRules(ZenModeConfig config) {
        if (config != null) {
        }
    }

    private static int zenSeverity(int zen) {
        switch (zen) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return 0;
        }
    }

    public boolean isInImportantInterruptions() {
        if (1 == this.mZenMode) {
            return true;
        }
        return false;
    }
}
