# Troubleshooting

## Camera Not Initializing

### Issue: `CameraNotAvailableException`

**Cause:** No camera device available

**Solutions:**
- Check device has camera hardware
- Ensure no other app has exclusive camera access
- Restart app
- Restart device

### Issue: `PermissionDeniedException`

**Cause:** Camera permission not granted

**Solutions:**
- Check permission declarations in manifest
- Request permission at runtime
- Check app permissions in Settings

## Preview Not Showing

### Issue: Black screen with no preview

**Cause:** Various factors

**Debug Steps:**
```kotlin
LaunchedEffect(Unit) {
    try {
        controller.startPreview()
        Log.d("Camera", "Preview started successfully")
    } catch (e: Exception) {
        Log.e("Camera", "Preview failed: ${e.message}", e)
    }
}
```

**Solutions:**
- Verify `CameraPreviewComposable` has proper size constraints
- Check camera permissions
- Ensure camera device is not in use by other app
- Try switching between front/back camera

## Photo Capture Fails

### Issue: `StorageException`

**Cause:** Insufficient storage or permission

**Solutions:**
- Check available storage space
- Grant write permission
- Clean up old captures
- Use external storage explicitly

### Issue: `CameraTimeoutException`

**Cause:** Capture took too long

**Solutions:**
- Reduce photo resolution
- Check device performance
- Disable heavy effects
- Retry operation

## Video Recording Issues

### Issue: Audio not recorded

**Cause:** Missing microphone permission

**Solutions:**
- Add `RECORD_AUDIO` to manifest
- Request microphone permission
- Check system audio settings

### Issue: Video file corrupted

**Cause:** Recording interrupted

**Solutions:**
- Always call `stopVideoRecording()` when done
- Check available storage
- Use proper exception handling

## Performance Issues

### Issue: App freezes during capture

**Solutions:**
- Use coroutines: `viewModelScope.launch { capture() }`
- Reduce photo resolution
- Run on background thread
- Profile with Android Studio Profiler

### Issue: High memory usage

**Solutions:**
- Release photos after processing
- Process in chunks for burst capture
- Reduce resolution
- Call `System.gc()` between captures

## Platform-Specific Issues

### Android: Camera crashes on cold start

**Solution:** Add permission check:
```kotlin
if (ContextCompat.checkSelfPermission(
    context, 
    Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED) {
    startPreview()
}
```

### iOS: Info.plist keys missing

**Solution:** Verify in Info.plist:
```xml
<key>NSCameraUsageDescription</key>
<string>Description</string>
```

### Desktop: Webcam not detected

**Solution:** Check webcam is not in use by other app:
```bash
# macOS
lsof | grep -i camera

# Windows (PowerShell)
Get-Process | Where-Object {$_.Modules -like "*camera*"}
```

## Getting Help

1. **Check Logs**
   ```bash
   adb logcat | grep -i camera
   ```

2. **Enable Debug Mode**
   ```kotlin
   CameraController.Builder()
       .enableDebugLogging(true)
       .build()
   ```

3. **Report Issues**
   - GitHub: https://github.com/kashif-e/CameraK/issues
   - Include device model, OS version, error stack trace
   - Minimal reproduction code

4. **Community**
   - Discussions: https://github.com/kashif-e/CameraK/discussions
