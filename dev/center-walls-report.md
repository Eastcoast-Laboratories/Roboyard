# Center 2x2 Carree Walls Report

Some levels have walls inside the 2x2 center carree that should not be there.

## Board sizes and center coordinates

| Board size | Center cells | mh left | mh right | mv top | mv bottom |
|------------|-------------|---------|----------|--------|-----------|
| 12x14      | (5,6)(6,6)(5,7)(6,7) | `mh5,7` | `mh6,7` | `mv6,6` | `mv6,7` |
| 16x16      | (7,7)(8,7)(7,8)(8,8) | `mh7,8` | `mh8,8` | `mv8,7` | `mv8,8` |

Only 12x14 boards are affected. No 16x16 boards have center walls.

## Affected levels

### `mh5,7` (horizontal left) — 12 levels
- level_6, level_7, level_29, level_50, level_80, level_81, level_86
- level_110, level_111, level_114, level_125, level_139

### `mh6,7` (horizontal right) — 16 levels
- level_3, level_6, level_14, level_18, level_27, level_37, level_44, level_56, level_63
- level_84, level_85, level_93, level_110, level_111, level_127, level_130

### `mv6,6` (vertical top) — 23 levels
- level_11, level_17, level_18, level_21, level_24, level_27, level_35, level_42, level_43, level_45, level_55
- level_67, level_76, level_80, level_87, level_88, level_93, level_96, level_99
- level_105, level_108, level_113, level_122

### `mv6,7` (vertical bottom) — 19 levels
- level_8, level_16, level_26, level_34, level_43, level_46, level_53, level_62, level_64, level_71
- level_85, level_86, level_91, level_98, level_100, level_110, level_111, level_128, level_133

## sed command to remove all center walls

```bash
sed -i '/^mh5,7;$/d; /^mh6,7;$/d; /^mv6,6;$/d; /^mv6,7;$/d' /var/www/Roboyard/app/src/main/assets/Maps/level_3.txt /var/www/Roboyard/app/src/main/assets/Maps/level_6.txt /var/www/Roboyard/app/src/main/assets/Maps/level_7.txt /var/www/Roboyard/app/src/main/assets/Maps/level_8.txt /var/www/Roboyard/app/src/main/assets/Maps/level_11.txt /var/www/Roboyard/app/src/main/assets/Maps/level_14.txt /var/www/Roboyard/app/src/main/assets/Maps/level_16.txt /var/www/Roboyard/app/src/main/assets/Maps/level_17.txt /var/www/Roboyard/app/src/main/assets/Maps/level_18.txt /var/www/Roboyard/app/src/main/assets/Maps/level_21.txt /var/www/Roboyard/app/src/main/assets/Maps/level_24.txt /var/www/Roboyard/app/src/main/assets/Maps/level_26.txt /var/www/Roboyard/app/src/main/assets/Maps/level_27.txt /var/www/Roboyard/app/src/main/assets/Maps/level_29.txt /var/www/Roboyard/app/src/main/assets/Maps/level_34.txt /var/www/Roboyard/app/src/main/assets/Maps/level_35.txt /var/www/Roboyard/app/src/main/assets/Maps/level_37.txt /var/www/Roboyard/app/src/main/assets/Maps/level_42.txt /var/www/Roboyard/app/src/main/assets/Maps/level_43.txt /var/www/Roboyard/app/src/main/assets/Maps/level_44.txt /var/www/Roboyard/app/src/main/assets/Maps/level_45.txt /var/www/Roboyard/app/src/main/assets/Maps/level_46.txt /var/www/Roboyard/app/src/main/assets/Maps/level_50.txt /var/www/Roboyard/app/src/main/assets/Maps/level_53.txt /var/www/Roboyard/app/src/main/assets/Maps/level_55.txt /var/www/Roboyard/app/src/main/assets/Maps/level_56.txt /var/www/Roboyard/app/src/main/assets/Maps/level_62.txt /var/www/Roboyard/app/src/main/assets/Maps/level_63.txt /var/www/Roboyard/app/src/main/assets/Maps/level_64.txt /var/www/Roboyard/app/src/main/assets/Maps/level_67.txt /var/www/Roboyard/app/src/main/assets/Maps/level_71.txt /var/www/Roboyard/app/src/main/assets/Maps/level_76.txt /var/www/Roboyard/app/src/main/assets/Maps/level_80.txt /var/www/Roboyard/app/src/main/assets/Maps/level_81.txt /var/www/Roboyard/app/src/main/assets/Maps/level_84.txt /var/www/Roboyard/app/src/main/assets/Maps/level_85.txt /var/www/Roboyard/app/src/main/assets/Maps/level_86.txt /var/www/Roboyard/app/src/main/assets/Maps/level_87.txt /var/www/Roboyard/app/src/main/assets/Maps/level_88.txt /var/www/Roboyard/app/src/main/assets/Maps/level_91.txt /var/www/Roboyard/app/src/main/assets/Maps/level_93.txt /var/www/Roboyard/app/src/main/assets/Maps/level_96.txt /var/www/Roboyard/app/src/main/assets/Maps/level_98.txt /var/www/Roboyard/app/src/main/assets/Maps/level_99.txt /var/www/Roboyard/app/src/main/assets/Maps/level_100.txt /var/www/Roboyard/app/src/main/assets/Maps/level_105.txt /var/www/Roboyard/app/src/main/assets/Maps/level_108.txt /var/www/Roboyard/app/src/main/assets/Maps/level_110.txt /var/www/Roboyard/app/src/main/assets/Maps/level_111.txt /var/www/Roboyard/app/src/main/assets/Maps/level_113.txt /var/www/Roboyard/app/src/main/assets/Maps/level_114.txt /var/www/Roboyard/app/src/main/assets/Maps/level_122.txt /var/www/Roboyard/app/src/main/assets/Maps/level_125.txt /var/www/Roboyard/app/src/main/assets/Maps/level_127.txt /var/www/Roboyard/app/src/main/assets/Maps/level_128.txt /var/www/Roboyard/app/src/main/assets/Maps/level_130.txt /var/www/Roboyard/app/src/main/assets/Maps/level_133.txt /var/www/Roboyard/app/src/main/assets/Maps/level_139.txt
```

Or shorter using glob (safe — only deletes matching lines):

```bash
sed -i '/^mh5,7;$/d; /^mh6,7;$/d; /^mv6,6;$/d; /^mv6,7;$/d' /var/www/Roboyard/app/src/main/assets/Maps/level_*.txt
```
