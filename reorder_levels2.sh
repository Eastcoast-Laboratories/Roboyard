#!/bin/bash
cd app/src/main/assets/Maps

# First, rename all levels to temporary names to avoid conflicts
for i in {1..140}; do
  if [ -f "level_$i.txt" ]; then
    git mv "level_$i.txt" "level_old_$i.txt"
  fi
done

# Move levels to middle (around position 70-75)
git mv level_old_106.txt level_70.txt
git mv level_old_107.txt level_74.txt
git mv level_old_108.txt level_71.txt
git mv level_old_109.txt level_72.txt
git mv level_old_110.txt level_73.txt
git mv level_old_117.txt level_75.txt

# Move specified levels to end
git mv level_old_112.txt level_137.txt
git mv level_old_120.txt level_138.txt
git mv level_old_125.txt level_139.txt
git mv level_old_123.txt level_140.txt

# Move remaining levels sequentially, filling gaps
x=1
for i in {1..140}; do
    if [ -f "level_old_$i.txt" ]; then
        while [ -f "level_$x.txt" ]; do
            ((x++))
        done
        git mv "level_old_$i.txt" "level_$x.txt" || { echo "Error moving level_old_$i.txt"; exit 1; }
        ((x++))
    fi
done

# Verify all files were moved
if ls level_old_*.txt 1> /dev/null 2>&1; then
    echo "Error: Some level_old_*.txt files remain!"
    exit 1
fi
