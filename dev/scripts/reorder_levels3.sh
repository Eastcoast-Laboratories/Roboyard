#!/bin/bash
cd app/src/main/assets/Maps

# First, rename all levels to temporary names to avoid conflicts
for i in {1..140}; do
  if [ -f "level_$i.txt" ]; then
    git mv "level_$i.txt" "level_old_$i.txt"
  fi
done

# Move level 30 to the end
git mv level_old_30.txt level_140.txt

# copy levels 1-29 back
for i in {1..29}; do
  if [ -f "level_old_$i.txt" ]; then
    git mv "level_old_$i.txt" "level_$((i)).txt" || { echo "Error moving level_old_$i.txt"; exit 1; }
  fi
done

# Shift levels 31-140 down by one
for i in {31..140}; do
  if [ -f "level_old_$i.txt" ]; then
    git mv "level_old_$i.txt" "level_$((i-1)).txt" || { echo "Error moving level_old_$i.txt"; exit 1; }
  fi
done


# Verify all files were moved
if ls level_old_*.txt 1> /dev/null 2>&1; then
    echo "Error: Some level_old_*.txt files remain!"
    exit 1
fi
