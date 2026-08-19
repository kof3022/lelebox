# 乐龄游戏盒 ProGuard 规则
# WebView JS 桥（ElderBridge）：@JavascriptInterface 方法必须保留，否则 H5 存档失效
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface

