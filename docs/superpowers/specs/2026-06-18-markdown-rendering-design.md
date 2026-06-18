# Markdown Rendering Design

## Goal

Add Markdown rendering for assistant messages in Open Chat while keeping user messages as plain text.

The feature should solve visible Markdown markers such as `**bold**`, `### heading`, and code fences appearing in assistant replies. It should also provide a global Markdown rendering toggle so the user can switch back to plain text if rendering causes unexpected formatting or performance issues.

## Decisions

- Use `mikepenz/multiplatform-markdown-renderer` with its Material 3 module.
- Render only assistant messages as Markdown.
- Keep user messages as the existing plain Compose `Text`.
- Enable Markdown rendering by default.
- Store the Markdown rendering preference globally in `DataStore`.
- Place the toggle control in the history screen header at the top-right.
- Use a capsule-style Switch with a circular thumb that slides left and right.
- Show green when enabled and white when disabled.
- Do not show any Markdown label text beside the Switch.
- Show short floating text feedback after toggle changes.
- Render Markdown during streaming for the first version without throttling.

## User Experience

The history screen header will keep the existing back button on the left. A capsule-style Switch will appear on the right:

- When Markdown rendering is enabled, assistant messages display formatted Markdown.
- When disabled, assistant messages display raw plain text.
- The setting applies globally across current and historical conversations.
- The toggle persists after app restart.
- Turning the toggle on shows `Markdown 渲染开启`.
- Turning the toggle off shows `Markdown 渲染关闭`.

The toggle should not show any inline label text in the header. The control itself communicates state through position and color.

## Implementation Scope

### Dependencies

Add the Material 3 Markdown renderer dependency to the app module Gradle file.

### Preferences

Extend `AppPreferences` with:

- `markdownRenderingEnabled: Flow<Boolean>`
- `setMarkdownRenderingEnabled(enabled: Boolean)`

Default value: `true`.

### Navigation / State

Collect the Markdown preference near the app root or navigation host, similar to dark mode.

Pass the resolved boolean to screens/components that render messages and to the history screen toggle callback.

Add a short-lived transient feedback mechanism that can display the Markdown on/off text after the user flips the Switch.

### Message Rendering

Update `MessageBubble` so:

- User messages continue using `Text`.
- Assistant messages use Markdown rendering when enabled.
- Assistant messages fall back to `Text` when disabled.
- Bubble background, max width, padding, border, and text color remain visually consistent with the current app design.

### History Toggle

Update `HistoryScreen` to receive:

- `markdownRenderingEnabled: Boolean`
- `onToggleMarkdownRendering: () -> Unit`
- transient feedback state or callback needed to show on/off text after a toggle change

Place the back button on the left side of the header and the capsule-style Markdown Switch on the right side of the header.

The Switch behavior should be:

- Off: white track, thumb aligned left
- On: green track, thumb aligned right
- No visible text label next to the Switch
- Toggle change triggers floating text feedback and persists globally

## Streaming Behavior

For this version, assistant messages will render Markdown during streaming without throttling.

Known trade-offs:

- Long streaming replies may re-parse Markdown frequently.
- Incomplete Markdown syntax can visually change while the model is still responding.
- Code blocks or lists may shift formatting after closing syntax arrives.

This is acceptable for the first version because it gives immediate visual feedback and keeps implementation small. If real-device testing shows visible jank, a later optimization can switch streaming messages to plain text until completion or add update throttling.

## Error Handling

Markdown rendering should not affect API requests or stored message content. The raw assistant text remains the source of truth.

If the Markdown renderer cannot format a specific message, the component should still display the raw text rather than showing an empty bubble or crashing.

## Testing

Add or update tests where practical:

- Preference default is enabled.
- Preference toggle persists the new value.
- Message rendering branch keeps user messages as plain text.
- Toggle feedback text appears for both enabled and disabled states.

Manual verification:

- Assistant response containing `**bold**`, `### heading`, numbered lists, and code fences renders cleanly when enabled.
- The same message shows raw Markdown when disabled.
- Toggle state survives app restart.
- History screen shows the back button on the left and the Markdown Switch on the right.
- The Switch track is green when enabled and white when disabled.
- Dark mode and existing history navigation still work.
