# Markdown Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render assistant messages as Markdown with a global capsule-style Switch in the history screen while keeping user messages as plain text.

**Architecture:** Keep Markdown rendering state in `AppPreferences`, pass it through `MainActivity` and `AppNavHost`, and render assistant messages through the Markdown component only when the global setting is enabled. Replace the current history-screen Markdown icon button with a top-right capsule-style `Switch`, and add a short-lived floating text feedback message after each toggle change.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, DataStore Preferences, mikepenz multiplatform Markdown renderer, Gradle Kotlin DSL, Android unit tests, adb emulator QA.

---

### Task 1: Confirm Dependency and Preference Base

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/example/aiassistant/data/preferences/AppPreferences.kt`

- [ ] **Step 1: Keep the Markdown renderer dependency present**

Ensure the app module includes:

```kotlin
implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.30.0")
```

- [ ] **Step 2: Keep the global preference API**

Ensure `AppPreferences` exposes:

```kotlin
val markdownRenderingEnabled: Flow<Boolean> =
    context.dataStore.data.map { it[Keys.MarkdownRenderingEnabled] ?: true }

suspend fun setMarkdownRenderingEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
        prefs[Keys.MarkdownRenderingEnabled] = enabled
    }
}
```

- [ ] **Step 3: Compile the base state**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: project compiles with the dependency and preference API intact.

### Task 2: Keep Markdown State Wired Through App Navigation

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/MainActivity.kt`
- Modify: `app/src/main/java/com/example/aiassistant/navigation/AppNavHost.kt`

- [ ] **Step 1: Collect Markdown state in MainActivity**

Ensure:

```kotlin
val markdownRenderingEnabled by container.appPreferences.markdownRenderingEnabled.collectAsState(initial = true)
```

and:

```kotlin
onToggleMarkdownRendering = {
    scope.launch {
        container.appPreferences.setMarkdownRenderingEnabled(!markdownRenderingEnabled)
    }
}
```

- [ ] **Step 2: Pass the state and toggle callback through AppNavHost**

Ensure `AppNavHost` receives:

```kotlin
markdownRenderingEnabled: Boolean,
onToggleMarkdownRendering: () -> Unit
```

and passes the Markdown state into:

- `ChatScreen`
- `HistoryScreen`

- [ ] **Step 3: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: no missing-parameter or callback wiring errors.

### Task 3: Keep Assistant Markdown Rendering and Plain User Text

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/ui/components/AiCards.kt`
- Modify: `app/src/main/java/com/example/aiassistant/ui/screens/chat/ChatScreen.kt`

- [ ] **Step 1: Keep MessageBubble branching by sender and setting**

Ensure `MessageBubble` accepts:

```kotlin
fun MessageBubble(
    text: String,
    isUser: Boolean,
    markdownRenderingEnabled: Boolean,
    modifier: Modifier = Modifier
)
```

and renders:

```kotlin
if (!isUser && markdownRenderingEnabled) {
    Markdown(content = text)
} else {
    Text(
        text = text,
        color = AiColors.TextPrimary,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}
```

- [ ] **Step 2: Keep ChatScreen passing the Markdown flag**

Ensure every `MessageBubble(...)` call from `ChatScreen` passes the screen's current `markdownRenderingEnabled`.

- [ ] **Step 3: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: assistant Markdown rendering still compiles and user text remains plain.

### Task 4: Replace the History Markdown Button With a Capsule Switch

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/ui/screens/history/HistoryScreen.kt`
- Modify: `app/src/main/java/com/example/aiassistant/ui/components/AiButtons.kt`

- [ ] **Step 1: Add a reusable capsule-style Switch composable**

Create a focused composable in `AiButtons.kt` such as:

```kotlin
@Composable
fun CapsuleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
)
```

The control must:

- use a capsule track
- use a circular thumb
- show the thumb on the left when off
- show the thumb on the right when on
- use a white track when off
- use a green track when on
- avoid any visible inline label text

- [ ] **Step 2: Replace the existing history icon button**

In `HistoryScreen`, remove the current Markdown `RoundIconButton` and replace it with a layout that keeps:

- back button on the left
- `CapsuleSwitch` on the right

This means the top bar row should use `Arrangement.SpaceBetween`.

- [ ] **Step 3: Wire toggle interaction**

Use:

```kotlin
CapsuleSwitch(
    checked = markdownRenderingEnabled,
    onCheckedChange = { onToggleMarkdownRendering() }
)
```

The visual state must derive only from `markdownRenderingEnabled`.

- [ ] **Step 4: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: the history top bar compiles and no longer imports the Markdown icon button path.

### Task 5: Add Floating Toggle Feedback

**Files:**
- Modify: `app/src/main/java/com/example/aiassistant/ui/screens/history/HistoryScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add feedback strings**

Add:

```xml
<string name="markdown_rendering_enabled_message">Markdown 渲染开启</string>
<string name="markdown_rendering_disabled_message">Markdown 渲染关闭</string>
```

- [ ] **Step 2: Add transient feedback state to HistoryScreen**

Use local Compose state such as:

```kotlin
var markdownToggleMessage by remember { mutableStateOf<String?>(null) }
```

and a timed clear:

```kotlin
LaunchedEffect(markdownToggleMessage) {
    if (markdownToggleMessage != null) {
        delay(2000)
        markdownToggleMessage = null
    }
}
```

- [ ] **Step 3: Show feedback when the Switch changes**

When the user toggles:

```kotlin
markdownToggleMessage =
    if (!markdownRenderingEnabled) stringResource(R.string.markdown_rendering_enabled_message)
    else stringResource(R.string.markdown_rendering_disabled_message)
onToggleMarkdownRendering()
```

- [ ] **Step 4: Render the floating feedback text**

Display the transient message in a lightweight overlay or top-aligned text treatment consistent with the app's existing save/test feedback style.

- [ ] **Step 5: Compile**

Run:

```powershell
.\gradlew.bat :app:compileDebugKotlin --console=plain
```

Expected: feedback strings and transient message UI compile.

### Task 6: Verification

**Files:**
- No code changes expected after this task unless verification fails.

- [ ] **Step 1: Run unit tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

Expected: all unit tests pass.

- [ ] **Step 2: Build debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

Expected: debug APK builds at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Emulator QA with test-android-apps**

Use the `test-android-apps:android-emulator-qa` skill:

```powershell
adb devices
.\gradlew.bat :app:installDebug --console=plain --quiet
adb -s <serial> shell monkey -p com.example.aiassistant -c android.intent.category.LAUNCHER 1
adb -s <serial> exec-out uiautomator dump /dev/tty
```

Expected:

- App launches without crash.
- History screen still opens.
- Back button remains at top-left.
- Capsule `Switch` appears at top-right.
- `Switch` track is green when enabled and white when disabled.
- Toggling shows `Markdown 渲染开启` or `Markdown 渲染关闭`.
- Returning to chat still works.

- [ ] **Step 4: Capture crash logs if needed**

Run:

```powershell
adb -s <serial> logcat -b crash -d
```

Expected: no app crash entries after launch and toggle flow.
