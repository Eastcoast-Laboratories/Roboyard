# Multilanguage Implementation Concept with Weblate Integration

## Overview

Implementing multilanguage support in Roboyard while connecting to Weblate would involve the following key components:

1. Organizing string resources in Android
2. Setting up Weblate integration
3. Managing the translation workflow
4. Testing and deploying translations

## 1. Android Resource Organization

### Resource Structure

There are already language-specific resource folders following Android's standard structure:

```
app/src/main/res/
├── values/            # Default (English)
│   └── strings.xml
├── values-de/         # German
│   └── strings.xml
├── values-fr/         # French
│   └── strings.xml
└── ...
```

### Implementation Steps

1. **Audit existing strings**:
   - Review all hardcoded strings in layouts and code
   - Move all text to the default strings.xml
   - Add comments for translators where context is needed

2. **Handle special cases**:
   - Create separate resources for RTL languages (Arabic, Hebrew)
   - Handle plurals with `plurals.xml` for each language


## 3. Translation Workflow

1. **Developer workflow**:
   - Add new strings to default strings.xml only
   - Include context comments with `translatable="false"` for non-translatable strings
   - Run automated checks before committing string changes

2. **Translation workflow**:
   - Translators work on github repo adding new translations as pulll requests

3. **Quality controls**:
   - Implement manual review process for critical text

## 4. Implementation Example for Translation and Accessibility

Here's how we would modify the string resources for translators and accessibility:

```xml
<!-- Original basic string -->
<string name="back">Back</string>

<!-- Improved for translators with comments -->
<string name="back" comment="Navigation button to go back to previous screen">Back</string>

<!-- For complex UI elements that need more context -->
<string name="robot_position_template">Robot %1$s at position row %2$d, column %3$d</string>

<!-- For non-translatable technical terms -->
<string name="api_endpoint" translatable="false">https://api.example.com/v1</string>

<!-- For plurals (translation only) -->
<plurals name="moves_count">
    <item quantity="one">%d move made</item>
    <item quantity="other">%d moves made</item>
</plurals>

<!-- Action hint as separate string -->
<string name="undo_move_hint">Tap to undo last move</string>


```

### Accessibility Best Practices

1. **Content Descriptions**: Use dedicated strings for content descriptions
   ```xml
   <!-- In layout file -->
   <ImageButton
       android:id="@+id/backButton"
       android:contentDescription="@string/back_button_content_description" />
   ```

2. **Live Announcements**: For dynamic content changes
   ```java
   // In code
   String announcement = getString(R.string.robot_moved_announcement, robotColor, newRow, newCol);
   accessibilityManager.announceForAccessibility(announcement);
   // Log for diagnostics
   Log.d("ROBOYARD_ACCESSIBILITY", "Announced: " + announcement);
   ```

3. **Focus Order**: Ensure logical focus navigation for screen readers
   ```xml
   <Button
       android:id="@+id/nextButton"
       android:nextFocusDown="@id/cancelButton" />
   ```

4. **State Descriptions**: For elements with changing states
   ```java
   // Set state description dynamically
   robotView.setStateDescription(getString(R.string.robot_active_state));
   ```

5. **Hints**: Provide usage hints for complex interactions
   ```xml
   <string name="board_interaction_hint">Double tap to select a robot, then swipe in direction to move</string>
   ```

## 5. Implementation Steps

1. **Initial Setup**:
   - Migrate all hardcoded strings to default strings.xml
   - Add context comments with `translatable="false"` for non-translatable strings
   - Handle plurals with `plurals.xml` for each language

2. **Integration**:
   - Perform initial translations of core languages
   - Create documentation for translators

3. **Testing and Refinement**:
   - Test on different screen sizes and orientations
   - Check for text overflow and display issues
   - Fix language-specific layout problems

## 6. Maintenance Considerations

- **String Change Policy**: Establish a protocol for modifying existing strings
- **Glossary**: Maintain a glossary of game-specific terms for translators
- **Monitoring**: Set up notifications for untranslated strings in new releases

This implementation would provide a robust foundation for maintaining translations across multiple languages while maintaining the unique UI elements of Roboyard.
