# Room and Media3 ship consumer rules. Domain objects are serialized explicitly.
-keepattributes Signature,InnerClasses,EnclosingMethod

# NewPipe loads generated time-ago patterns by class name.
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }

# NewPipe executes the public player JavaScript using Rhino.
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
# Desktop-only Rhino optimizer / Java-bean conversion paths are not used by NewPipe's interpreter.
-dontwarn jdk.dynalink.**
-dontwarn java.beans.**
