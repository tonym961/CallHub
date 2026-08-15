# Keep Room generated classes.
-keep class it.iotatec.callhub.data.db.** { *; }

# Services bound by the system (name referenced from the manifest).
-keep class it.iotatec.callhub.dialer.CallHubInCallService { *; }
-keep class it.iotatec.callhub.dialer.CallScreeningServiceImpl { *; }
-keep class it.iotatec.callhub.voip.CallNotificationListener { *; }
