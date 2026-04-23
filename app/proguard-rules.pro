# ==============================
# Core shrink/obfuscation setup
# ==============================

-dontwarn **
-allowaccessmodification
-repackageclasses com.imdad_por.internal
-overloadaggressively

# Keep generated Kotlin metadata required for safe reflection / default params / sealed classes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable,KotlinMetadata

# Strip source line mapping in release if you want less readable stack traces
-renamesourcefileattribute SourceFile

# ==============================
# App entry points
# ==============================

-keep class com.imdad_por.ImdadPorApplication { *; }
-keep class com.imdad_por.ImdadPorAppKt { *; }
-keep class com.imdad_por.MainActivity { *; }

# ==============================
# Security layer
# Keep these classes alive; let names shrink unless they are used by reflection
# ==============================

-keep class com.imdad_por.core.security.app.obfuscation.ProGuardHints { *; }

-keepclassmembers class com.imdad_por.core.security.app.obfuscation.StringObfuscator {
    public static java.lang.String decode(byte[]);
    public static java.lang.String decode(byte[], byte[]);
    public static byte[] encode(java.lang.String);
    public static byte[] encode(java.lang.String, byte[]);
    public static java.lang.String decodeParts(java.util.List);
    public static java.lang.String decodeParts(byte[][]);
    public static java.lang.String buildString(byte[][]);
    public static byte[] generateMask(int);
    public static void wipe(byte[]);
}

-keepclassmembers class com.imdad_por.core.security.TamperProtection {
    public static *** inspect(...);
    public static *** isSideLoaded(...);
    public static *** hasHooksInMemory(...);
    public static *** hasSuspiciousNativeLibraries(...);
    public static *** hasSuspiciousSystemProperties(...);
    public static *** isProxyEnabled(...);
    public static *** hasSuspiciousRuntimePath(...);
    public static *** isApkSizeSuspicious(...);
    public static *** isDexSizeSuspicious(...);
    public static *** shouldRestrictSensitiveOperations(...);
    public static *** getRiskReasons(...);
}

-keepclassmembers class com.imdad_por.core.security.RootDetector {
    public static *** inspect(...);
    public static *** isSystemCompromised(...);
    public static *** shouldRestrictSensitiveFeatures(...);
    public static *** getRiskLevel(...);
    public static *** getRiskReasons(...);
}

-keepclassmembers class com.imdad_por.core.security.DebugDetector {
    public static *** inspect(...);
    public static *** shouldRestrictSensitiveOperations(...);
    public static *** getRiskReasons(...);
}

-keepclassmembers class com.imdad_por.core.security.IntegrityChecker {
    public static *** generateNonce(...);
    public static *** requestIntegrityToken(...);
    public static *** prepareIntegrityChallenge(...);
}

-keepclassmembers class com.imdad_por.core.security.SessionManager {
    public static *** init(...);
    public static *** saveSession(...);
    public static *** getAuthToken(...);
    public static *** getRefreshToken(...);
    public static *** getUserId(...);
    public static *** isLoggedIn(...);
    public static *** hasValidSession(...);
    public static *** logout(...);
    public static *** clearSession(...);
    public static *** shouldRestrictSensitiveOperations(...);
    public static *** getSessionRiskReason(...);
    public static *** refreshSessionSecurityState(...);
}

-keepclassmembers class com.imdad_por.core.security.SubscriptionGuard {
    public static *** init(...);
    public static *** isPremiumActive(...);
    public static *** updateSubscription(...);
    public static *** clearSubscription(...);
    public static *** hasSubscriptionData(...);
    public static *** getCachedSubscriptionState(...);
    public static *** getAccessRiskReason(...);
    public static *** shouldRestrictPremiumOperations(...);
}

# ==============================
# Crypto / keystore helpers
# ==============================

-keep class com.imdad_por.core.security.CryptoManager { *; }
-keep class com.imdad_por.core.security.KeyStoreManager { *; }
-keep class com.imdad_por.core.security.SecurePrefs { *; }
-keep class com.imdad_por.core.security.crypto.** { *; }

# ==============================
# Utilities used by security code
# ==============================

-keep class com.imdad_por.core.utils.HashUtils { *; }
-keep class com.imdad_por.core.utils.Constants { *; }

# ==============================
# AndroidX / biometric / integrity / security crypto
# Keep only where reflection or generated access may be affected
# ==============================

-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }

# Google Play Integrity / Tasks
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.google.android.gms.tasks.** { *; }

# ==============================
# If you use reflection anywhere in the app
# ==============================

-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
    @androidx.annotation.Keep <init>(...);
}

# ==============================
# Kotlin / coroutine safety
# ==============================

-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }

# ==============================
# Optional: reduce noise from logs in release
# ==============================

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}