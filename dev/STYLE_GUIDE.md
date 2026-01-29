# Roboyard Style Guide

This document defines the visual style for both the Android app and the Laravel web application to ensure a consistent look and feel across all platforms.

## Typography

### Font Family
- **Primary Font:** Nunito (sans-serif)
- **Fallback:** System sans-serif fonts

### Font Sizes
| Element | Size | Weight |
|---------|------|--------|
| Page Title (h1) | 24sp / 1.5rem | Bold (700) |
| Section Title (h2) | 20sp / 1.25rem | Semi-bold (600) |
| Subsection (h3) | 18sp / 1.125rem | Semi-bold (600) |
| Body Text | 14sp / 0.9rem | Normal (400) |
| Small Text | 12sp / 0.75rem | Normal (400) |
| Button Text | 14sp / 0.875rem | Semi-bold (600) |

### Line Height
- Base line height: 1.6

## Colors

### Primary Colors
| Name | Hex | Usage |
|------|-----|-------|
| Primary Blue | #0d6efd | Primary buttons, links |
| Primary Dark | #0b5ed7 | Button hover states |
| Success Green | #198754 | Success messages, unlocked states |
| Warning Orange | #ffc107 | Warnings, highlights |
| Danger Red | #dc3545 | Errors, delete actions |
| Info Cyan | #0dcaf0 | Information badges |

### Background Colors
| Name | Hex | Usage |
|------|-----|-------|
| Body Background | #f8fafc | Main page background |
| Card Background | #ffffff | Cards, content containers |
| Light Gray | #f5f5f5 | Locked items, disabled states |
| Light Green | #e8f5e9 | Unlocked achievement background |
| Light Gold | #fff8e1 | New/highlighted items |

### Text Colors
| Name | Hex | Usage |
|------|-----|-------|
| Primary Text | #212529 | Main body text |
| Secondary Text | #6c757d | Muted text, descriptions |
| Dark Green | #1b5e20 | Unlocked achievement names |
| Gray | #9e9e9e | Locked/disabled text |
| Link Color | #0d6efd | Hyperlinks |

## Components

### Cards
```css
.card {
    background: #ffffff;
    border: 1px solid rgba(0, 0, 0, 0.125);
    border-radius: 0.375rem;
    box-shadow: none;
}

.card-header {
    background-color: rgba(0, 0, 0, 0.03);
    border-bottom: 1px solid rgba(0, 0, 0, 0.125);
    padding: 0.75rem 1rem;
    font-weight: 600;
}

.card-body {
    padding: 1rem;
}
```

### Buttons

#### Primary Button
```css
.btn-primary {
    background-color: #0d6efd;
    border-color: #0d6efd;
    color: #ffffff;
    padding: 0.375rem 0.75rem;
    border-radius: 0.375rem;
    font-weight: 600;
}

.btn-primary:hover {
    background-color: #0b5ed7;
    border-color: #0a58ca;
}
```

#### Secondary Button (Back Button)
```css
.btn-secondary {
    background-color: #6c757d;
    border-color: #6c757d;
    color: #ffffff;
    padding: 0.375rem 0.75rem;
    border-radius: 0.375rem;
}
```

#### Outline Button
```css
.btn-outline-primary {
    background-color: transparent;
    border: 1px solid #0d6efd;
    color: #0d6efd;
}
```

### Lists

#### List Group
```css
.list-group-item {
    padding: 0.75rem 1rem;
    border: 1px solid rgba(0, 0, 0, 0.125);
    background-color: #ffffff;
}

.list-group-item:hover {
    background-color: #f8f9fa;
}
```

### Dividers / Separators
```css
hr, .divider {
    border: 0;
    border-top: 1px solid rgba(0, 0, 0, 0.1);
    margin: 1rem 0;
}
```

### Tables
```css
table {
    width: 100%;
    border-collapse: collapse;
}

th, td {
    padding: 0.75rem;
    border-bottom: 1px solid #dee2e6;
    text-align: left;
}

th {
    background-color: #f8f9fa;
    font-weight: 600;
}

tr:hover {
    background-color: #f8f9fa;
}
```

## Spacing

### Margins & Padding
| Size | Value |
|------|-------|
| xs | 4dp / 0.25rem |
| sm | 8dp / 0.5rem |
| md | 16dp / 1rem |
| lg | 24dp / 1.5rem |
| xl | 32dp / 2rem |

### Container Padding
- Horizontal: 16dp / 1rem
- Vertical: 16dp / 1rem

## Icons

### Icon Library
- Bootstrap Icons (web)
- Custom drawable icons (Android)

### Icon Sizes
| Context | Size |
|---------|------|
| Navigation | 20dp |
| List Item | 16dp |
| Button | 16dp |
| Large Display | 48dp |

## Android-Specific Implementation

### Back Button Style
```java
// Match Laravel btn-primary style (blue)
// Position: Bottom of screen
Button backButton = new Button(context);
backButton.setText("← Back");
backButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#0d6efd")));
backButton.setTextColor(Color.WHITE);
backButton.setPadding(24, 12, 24, 12);
// Corner radius via drawable or MaterialButton
```

### Card Style
```java
// Card container
LinearLayout card = new LinearLayout(context);
card.setOrientation(LinearLayout.VERTICAL);
card.setBackgroundColor(Color.WHITE);
// Add border via drawable with stroke
// border-radius: 6dp (0.375rem equivalent)
```

### Card Header (Page Title)
```java
// Gray background box above content - like Laravel card-header
TextView cardHeader = new TextView(context);
cardHeader.setText("Page Title");
cardHeader.setTextSize(16); // Not bold
cardHeader.setTextColor(Color.parseColor("#212529"));
cardHeader.setPadding(12dp, 12dp, 12dp, 12dp);
cardHeader.setBackgroundColor(Color.parseColor("#f8f9fa"));
// Add 1dp divider below with color #dee2e6
```

### Section Headers (h3 style)
```java
// Section headings inside card body - 18sp, not bold
TextView sectionHeader = new TextView(context);
sectionHeader.setTextSize(18); // h3 equivalent
sectionHeader.setTextColor(Color.parseColor("#212529"));
// NOT bold - matches Laravel h3 style
sectionHeader.setPadding(0, 16dp, 0, 0);
```

### Body Text
```java
TextView bodyText = new TextView(context);
bodyText.setTextSize(14);
bodyText.setTextColor(Color.parseColor("#212529"));
bodyText.setLineSpacing(0, 1.6f);
```

### Muted/Secondary Text
```java
TextView mutedText = new TextView(context);
mutedText.setTextSize(14);
mutedText.setTextColor(Color.parseColor("#6c757d"));
```

## Layout Structure

### Page Layout
1. **Header** - Title with optional back button
2. **Content** - Main content area with cards
3. **Footer** - Optional footer with links

### Content Container
- Max width: responsive (full width on mobile)
- Padding: 16dp horizontal
- Background: #f8fafc

### Card Layout
- Background: white
- Border: 1px solid rgba(0,0,0,0.125)
- Border radius: 6dp
- Margin bottom: 16dp
- Card header: light gray background with border bottom
- Card body: white with 16dp padding
