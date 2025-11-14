.\gradlew.bat assembleDebug       
adb shell pm clear vn.haui.heartlink   
adb install -r app\build\outputs\apk\debug\app-debug.apk ; adb shell am start -n vn.haui.heartlink/vn.haui.heartlink.activities.SplashActivity 