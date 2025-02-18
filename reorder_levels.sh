#!/bin/bash
cd app/src/main/assets/Maps

# First, rename all levels to temporary names to avoid conflicts
for i in {1..140}; do
  if [ -f "level_$i.txt" ]; then
    git mv "level_$i.txt" "level_old_$i.txt"
  fi
done

# Move levels to front (in reverse order to maintain sequence)
git mv level_old_3.txt level_1.txt
git mv level_old_10.txt level_2.txt
git mv level_old_11.txt level_3.txt
git mv level_old_17.txt level_4.txt
git mv level_old_16.txt level_5.txt
git mv level_old_22.txt level_6.txt
git mv level_old_24.txt level_7.txt
git mv level_old_26.txt level_8.txt
git mv level_old_28.txt level_9.txt
git mv level_old_30.txt level_10.txt
git mv level_old_31.txt level_11.txt
git mv level_old_33.txt level_12.txt
git mv level_old_34.txt level_13.txt
git mv level_old_35.txt level_14.txt
git mv level_old_37.txt level_15.txt
git mv level_old_70.txt level_16.txt
git mv level_old_40.txt level_17.txt
git mv level_old_39.txt level_18.txt
git mv level_old_38.txt level_19.txt
git mv level_old_36.txt level_20.txt

# Move specified levels to end
git mv level_old_49.txt level_133.txt
git mv level_old_50.txt level_134.txt
git mv level_old_66.txt level_135.txt
git mv level_old_58.txt level_136.txt
git mv level_old_54.txt level_137.txt
git mv level_old_101.txt level_138.txt
git mv level_old_102.txt level_139.txt
git mv level_old_96.txt level_140.txt

# Move remaining levels sequentially
counter=21
for i in {1..140}; do
  if [ -f "level_old_$i.txt" ]; then
    git mv "level_old_$i.txt" "level_$counter.txt"
    ((counter++))
  fi
done
