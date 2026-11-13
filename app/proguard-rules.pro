# R8 for release and profile.
#
# Only shrink and optimise, do NOT rename: the code comes from a decompiler export and
# works by names in many places -- Gson fields, Serializable (home screen store
# HomeCache, intent extras), reflection on library fields. With names kept the
# baseline profile (app/src/main/baseline-prof.txt) fits without rewriting, and stack traces from the
# logcat stay readable. The size gain comes almost entirely from dropping unused code.
-dontobfuscate

# Gson reads these models by reflection (EventSub.java, fromJson); otherwise R8 would see the fields as
# unused and would remove them.
-keep class com.shrug.shrugtv.eventSub.** { *; }

# Serializable: Movie & Co. travel by intent and into the home screen store; fields that are only
# written and read back by serialisation must stay. For ALL classes,
# not only our own: Movie holds time values whose serialisation runs through writeReplace
# (erster Lauf 2026-10-28, damals Joda: Player-Absturz "ISOChronology; no valid constructor";
# java.time.ZonedDateTime arbeitet ebenso mit writeReplace).
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


# Optional dependencies that the libraries only address after a runtime check
# (OkHttp-Plattformen); im APK fehlen sie absichtlich (R8-Lauf 2026-10-28, missing_rules.txt).
# Ion-Conscrypt and Joda-Convert went away with Ion and Joda-Time.
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Paho MQTT loads its network modules via ServiceLoader and its messages via ResourceBundle
# (Home Assistant integration, docs/mqtt_ha_plan.md)
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**
# NanoHTTPD (phone companion) – small, keep it whole
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**
