# UI Layout Specification - GameFragment

## Overview
This document describes the optimal layout hierarchy for the GameFragment to ensure proper space utilization and responsive behavior across different screen sizes.

## Layout Hierarchy (Top to Bottom)

### 1. Game Grid View (Primary Content)
- **Position:** Top of screen
- **Width:** Always full width of screen
- **Height:** Calculated based on aspect ratio to maintain game grid proportions
- **Priority:** Highest - must always be fully visible and functional
- **Behavior:** Fixed size based on content ratio, never compressed

### 2. Hint Container (Overlay Layer)
- **Position:** Below game grid, above text area
- **Width:** Full width
- **Height:** Wrap content (when visible)
- **Z-Index:** Highest overlay layer
- **Visibility:** Only visible when hints are active
- **Behavior:** 
  - **When Active:** Pushes game info container down when sufficient space available
  - **When Closed:** Space is freed, game info container moves back up to original position
  - **Fallback:** Overlays text area only when insufficient space
  - **Font Size:** Smaller on displays with low aspect ratio (wide screens)
  - **Button Style:** Hint button should be gray when inactive

### 3. Game Info/Text Area (Three-Block Layout)
- **Position:** Below hint container, above buttons
- **Width:** Full width
- **Height:** Wrap content
- **Structure:** Three horizontal blocks with responsive layout
- **Behavior:** Can be covered by hint container or button area if space is limited

#### Left Block (Moves & Difficulty)
- **Content:** Move count, squares moved, difficulty level
- **Width:** 45% of available width (weight=0.45)
- **Alignment:** Left-aligned within block

#### Center Block (Optimal Moves Button)
- **Content:** "Show Optimal" button with large text
- **Width:** 20% of available width (weight=0.20)
- **Visibility:** Hidden by default (`android:visibility="gone"`)
- **Behavior:** When visible, takes its allocated space and displaces content
- **Purpose:** Displays optimal move count prominently

#### Right Block (Timer & Map Info)
- **Content:** Game timer, unique map ID
- **Width:** 35% of available width (weight=0.35)
- **Alignment:** Right-aligned within block

### 4. Flexible Space (Spacer)
- **Position:** Between text area and buttons
- **Purpose:** Absorbs extra space on larger screens
- **Behavior:** Grows when excess space available, shrinks to zero when space is limited

### 5. Button Container (Controls)
- **Position:** Bottom of screen
- **Width:** Full width
- **Height:** Fixed based on button requirements
- **Priority:** High - must remain accessible
- **Behavior:** Can overlay text area in extreme space constraints (should rarely happen)

## Space Management Rules

### Normal Conditions (Sufficient Space)
```
┌─────────────────────────┐
│   Game Grid View        │ ← Full width, proper ratio
│   (maintains ratio)     │
├─────────────────────────┤
│   Hint Container        │ ← Pushes content down when active
├─────────────────────────┤
│   Game Info/Text        │ ← Move count, stats, etc.
│   (pushed down by hint) │
├─────────────────────────┤
│   Flexible Spacer       │ ← Grows with available space
├─────────────────────────┤
│   Button Container      │ ← Fixed height, always visible
└─────────────────────────┘
```

### Limited Space (Hint Active)
```
┌─────────────────────────┐
│   Game Grid View        │ ← Full width, proper ratio
│   (maintains ratio)     │
├─────────────────────────┤
│   Hint Container        │ ← Overlays text area
│   (covers text below)   │
├─────────────────────────┤
│   Button Container      │ ← Always accessible
└─────────────────────────┘
```

### Extreme Space Constraints (Should Rarely Happen)
```
┌─────────────────────────┐
│   Game Grid View        │ ← Full width, proper ratio
│   (maintains ratio)     │
├─────────────────────────┤
│   Button Container      │ ← May overlay text area
│   (covers text below)   │
└─────────────────────────┘
```

## Technical Implementation Requirements

### Game Grid View
- Use `android:layout_width="match_parent"`
- Use `android:layout_height="wrap_content"` or calculated height
- Maintain aspect ratio through custom view logic
- Always positioned at top with `app:layout_constraintTop_toTopOf="parent"`

### Hint Container
- Use `android:elevation` or `app:layout_constraintVertical_bias` for overlay behavior
- `android:visibility="gone"` when not needed
- Higher z-index than text area

### Flexible Spacer
- Use `android:layout_height="0dp"` with `app:layout_constraintVertical_weight="1"`
- Positioned between text area and buttons
- Absorbs excess vertical space

### Button Container
- Fixed height using dimension resources
- Always anchored to bottom: `app:layout_constraintBottom_toBottomOf="parent"`
- Higher z-index than text area for overlay capability

## Responsive Behavior

### Small Screens (≤720dp height)
- Reduced button heights and margins
- Hint container more likely to overlay text area
- Flexible spacer minimized
- Smaller hint font sizes for better space utilization

### Large Screens (>720dp height)
- Standard button sizes
- More space for flexible spacer
- Less overlay behavior needed
- Standard hint font sizes

### Low Aspect Ratio Displays (Wide Screens)
- Reduced hint font sizes to prevent horizontal overflow
- More aggressive space management
- Prioritize horizontal text fitting over vertical spacing

## Z-Index Priority (Highest to Lowest)
1. Hint Container (when active)
2. Button Container
3. Game Info/Text Area
4. Flexible Spacer
5. Game Grid View (base layer)

## Accessibility Considerations
- All interactive elements must remain accessible even during overlay scenarios
- Text content should have fallback announcements when covered
- Focus management during hint display mode
