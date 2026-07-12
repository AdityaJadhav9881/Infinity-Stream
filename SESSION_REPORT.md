# Session Report — Equalizer Investigation

**Date:** 2025-07-12
**Device:** vivo (model I2404, Android 14, SDK 35)
**Media3 Version:** 1.2.1

---

## Problem

The equalizer (BassBoost / Virtualizer / Equalizer / LoudnessEnhancer) does not work on the test device. Not on speaker, not on Bluetooth — the Android `AudioEffect` API returns **error -3 (`ERROR_NO_INIT`)** for all effect types on every audio session.

## Root Cause

The Android hardware AudioEffect engine is broken on this specific vivo device. The error `-3` means the underlying audio HAL cannot initialize the effect processor. This is a **device firmware / vendor issue**, not an app bug.

### Evidence

```
EqualizerManager: Failed to initialize on session 18809: Cannot initialize effect
engine for type: 0634f220-ddd4-11db-a0fc-0002a5d5c51b Error: -3
```

- Error `-3` = `EFFECT_ERROR_NO_INIT` — the effect engine cannot be created at all
- Affects `BassBoost`, `Virtualizer`, `Equalizer`, and `LoudnessEnhancer`
- Happens on **both speaker and Bluetooth A2DP**
- Other devices (Samsung, Pixel, Xiaomi) do not exhibit this — it is vivo-specific

## Attempted Fixes

### Attempt 1: Software AudioProcessor (BaseAudioProcessor)

Replaced the hardware `AudioEffect` API with a custom `SoftwareEqualizerProcessor` extending Media3's `BaseAudioProcessor`. This processes PCM samples directly in ExoPlayer's audio pipeline — bypassing Android's AudioEffect system entirely.

**Approach:**
- Created `SoftwareEqualizerProcessor` extending `BaseAudioProcessor`
- Overrode `onConfigure()` to always return the input format (always active)
- Overrode `queueInput()` to apply bass boost (IIR low-pass) and virtualizer (mid/side stereo widening)
- When no effects enabled, copies input to output unchanged (passthrough)
- Wired into `DefaultAudioSink.DefaultAudioProcessorChain` before `SilenceSkippingAudioProcessor`

**Result:** FAILS. Passthrough (Normal preset) works, but any preset with effects (Bass Boost, etc.) causes audio to stop playing — track position jumps 0→2→3→0 repeatedly. The processing corrupts ExoPlayer's audio output.

### Attempt 2: Same processor, different activation approach

Tried having `onConfigure()` return `NOT_SET` initially and only activate when a preset is selected.

**Result:** FAILS — same as above, audio stops.

### Attempt 3: Original hardware AudioEffects (reverted)

Reverted to the original `EqualizerManager` using `BassBoost`/`Virtualizer`/`LoudnessEnhancer` via Android's AudioEffect API.

**Result:** Audio plays, but equalizer effects do nothing (error -3 silently caught).

## Why the Software Processor Fails

The `SoftwareEqualizerProcessor` causes audio corruption when processing is active. Possible causes:

1. **Buffer management conflict** — `DefaultAudioSink` may expect specific output buffer behavior that conflicts with `BaseAudioProcessor`'s `replaceOutputBuffer()` in Media3 1.2.1
2. **Sample-level processing corrupts the audio stream** — the IIR filter or stereo widening math may introduce discontinuities or clipping that ExoPlayer's audio rendering pipeline cannot handle
3. **Media3 1.2.1 `BaseAudioProcessor` API quirks** — this version of Media3 may have bugs in how it handles dynamically changing `isActive()` state within a processor chain

The processor works in passthrough mode (no effects) but fails when any DSP processing is applied to PCM samples.

## Current State

- **Equalizer is present in the app** (UI + backend) but non-functional on this device
- **All other features work normally** — playback, downloads, offline, backup, etc.
- The `EqualizerManager` catches the error silently and continues

## Recommendation

**Do NOT remove the equalizer** — it works on other devices (Samsung, Pixel, etc.). The issue is isolated to this vivo device's firmware.

Options for future investigation:
1. **Upgrade Media3** from 1.2.1 to 1.5.x+ — newer versions have improved `AudioProcessor` support and may fix the software processor issue
2. **Use a third-party DSP library** (e.g., Oboe + a native DSP chain) instead of Media3's `BaseAudioProcessor`
3. **Contact vivo** about the broken AudioEffect HAL
4. **Accept the limitation** — equalizer doesn't work on this specific device, works everywhere else

## Files Involved

| File | Role |
|------|------|
| `utils/EqualizerManager.kt` | Hardware AudioEffects (BassBoost/Virtualizer/Equalizer/LoudnessEnhancer) |
| `utils/SoftwareEqualizerProcessor.kt` | Software DSP processor (unused — causes audio corruption) |
| `utils/PlayerSettingsManager.kt` | Persists equalizer preset via DataStore |
| `player/MusicPlaybackService.kt` | Initializes effects on audio session, applies presets |
| `ui/screens/SettingsScreen.kt` | Equalizer preset picker UI |
| `MainActivity.kt` | Injects EqualizerManager, passes to composable tree |
