# Android Context Management in Roboyard

## Problem Statement

Android applications frequently struggle with context management, leading to issues like:

1. **Memory Leaks** - Storing long-lived references to Activity contexts
2. **Null Context References** - Trying to use context after the Activity/Fragment is destroyed
3. **Type Mismatch** - Using Application context where Activity context is required

In Roboyard, the game history saving feature requires Activity references for file operations, but may be triggered when no valid Activity reference is available.

## Symptoms and Error Messages

```
[HISTORY] Cannot save to history: no activity
```

Compilation errors:
```
incompatible types: Context cannot be converted to Activity
GameHistoryManager.initialize(context);
```

## Current Architecture

1. `GameStateManager` extends `AndroidViewModel` which has access to application context
2. `GameHistoryManager` requires Activity context for file operations
3. `GameFragment` holds a reference to `GameStateManager`

## Solutions

### Short-term Solution

1. Use WeakReference<Activity> to store Activity reference
2. Add `setActivity()` method to update the reference when Fragment is created/resumed
3. Call `setActivity()` from GameFragment during appropriate lifecycle events
4. Check for null/stale reference before using

### Long-term Solutions

1. **Context Provider Interface**
   - Create a global interface for providing the right type of context
   - Implement in Application class with a singleton pattern
   - Handle lifecycle awareness to provide valid references

2. **Refactor GameHistoryManager**
   - Modify to use regular Context instead of Activity where possible
   - Use ContentProvider pattern for file operations that need Activity context

3. **Dependency Injection**
   - Implement proper DI with Dagger/Hilt/Koin
   - Provide context with appropriate scopes and lifecycle awareness

## Implementation Plan

1. Implement WeakReference solution in GameStateManager
2. Update GameFragment to properly set the Activity reference
3. Add defensive checks for null references
4. Document pattern for future use

## Best Practices

1. Always use Application context for long-lived objects
2. Only use Activity context for UI operations and when absolutely necessary
3. Pass context explicitly to methods that need it rather than storing globally
4. Use WeakReferences when storing Activity references beyond their guaranteed lifecycle
5. Consider Context wrapper patterns that handle lifecycle events automatically
