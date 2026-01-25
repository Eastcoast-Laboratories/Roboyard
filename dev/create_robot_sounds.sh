#!/bin/bash

# Create robot sound files with pitch adjustments for 5 robots (0-4)
# Robot 0: -2 semitones
# Robot 1: -1 semitone
# Robot 2: unchanged
# Robot 3: +1 semitone
# Robot 4: +2 semitones
#
# For hit_robot sounds: split in half, pitch each half separately
# First half = attacker pitch, Second half = target pitch (with -4 semitone base offset)

RAW_DIR="/var/www/Roboyard/app/src/main/res/raw"
TEMP_DIR="/tmp/robot_sounds_temp"

# Configurable: split point in milliseconds for hit_robot sounds
SPLIT_MS=150

# Base pitch offset for the second half (target) of hit_robot sounds
TARGET_BASE_OFFSET=-4

# Clean up and recreate temporary directory
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Define pitch adjustments (in semitones)
declare -A PITCH_SHIFTS
PITCH_SHIFTS[0]=-2
PITCH_SHIFTS[1]=-1
PITCH_SHIFTS[2]=0
PITCH_SHIFTS[3]=1
PITCH_SHIFTS[4]=2

# Function to apply pitch shift using ffmpeg
apply_pitch_shift() {
    local input_file="$1"
    local output_file="$2"
    local semitones="$3"
    
    if [ "$semitones" -eq 0 ]; then
        # No pitch shift needed, just copy
        cp "$input_file" "$output_file"
    else
        # Get original sample rate
        local sample_rate=$(ffprobe -v error -select_streams a:0 -show_entries stream=sample_rate -of default=noprint_wrappers=1:nokey=1 "$input_file")
        
        # Calculate pitch factor: 2^(semitones/12)
        local pitch_factor=$(awk "BEGIN {printf \"%.6f\", 2^($semitones/12)}")
        local new_rate=$(awk "BEGIN {printf \"%.0f\", $sample_rate * $pitch_factor}")
        
        # Apply pitch shift: change sample rate then resample back to original
        ffmpeg -i "$input_file" -af "asetrate=$new_rate,aresample=$sample_rate" "$output_file" -y 2>/dev/null
    fi
}

# Function to create hit_robot sound with split pitching
# First half pitched by attacker, second half pitched by target (with base offset)
create_hit_robot_sound() {
    local input_file="$1"
    local output_file="$2"
    local attacker_id="$3"
    local target_id="$4"
    
    local attacker_pitch=${PITCH_SHIFTS[$attacker_id]}
    local target_pitch=$((${PITCH_SHIFTS[$target_id]} + TARGET_BASE_OFFSET))
    
    local split_seconds=$(awk "BEGIN {printf \"%.3f\", $SPLIT_MS/1000}")
    
    # Get original sample rate
    local sample_rate=$(ffprobe -v error -select_streams a:0 -show_entries stream=sample_rate -of default=noprint_wrappers=1:nokey=1 "$input_file")
    
    # Temporary files for split parts
    local part1="$TEMP_DIR/part1_${attacker_id}_${target_id}.mp3"
    local part2="$TEMP_DIR/part2_${attacker_id}_${target_id}.mp3"
    local part1_pitched="$TEMP_DIR/part1_pitched_${attacker_id}_${target_id}.mp3"
    local part2_pitched="$TEMP_DIR/part2_pitched_${attacker_id}_${target_id}.mp3"
    
    # Get total duration of input file
    local duration=$(ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 "$input_file")
    
    # Split: first half (0 to split point) - re-encode to ensure exact cut
    ffmpeg -i "$input_file" -t "$split_seconds" -acodec libmp3lame -q:a 2 "$part1" -y 2>/dev/null
    
    # Split: second half (from split point to end) - re-encode to ensure complete extraction
    ffmpeg -i "$input_file" -ss "$split_seconds" -acodec libmp3lame -q:a 2 "$part2" -y 2>/dev/null
    
    # Pitch first half (attacker)
    if [ "$attacker_pitch" -eq 0 ]; then
        cp "$part1" "$part1_pitched"
    else
        local pitch_factor=$(awk "BEGIN {printf \"%.6f\", 2^($attacker_pitch/12)}")
        local new_rate=$(awk "BEGIN {printf \"%.0f\", $sample_rate * $pitch_factor}")
        ffmpeg -i "$part1" -af "asetrate=$new_rate,aresample=$sample_rate" "$part1_pitched" -y 2>/dev/null
    fi
    
    # Pitch second half (target with base offset)
    if [ "$target_pitch" -eq 0 ]; then
        cp "$part2" "$part2_pitched"
    else
        local pitch_factor=$(awk "BEGIN {printf \"%.6f\", 2^($target_pitch/12)}")
        local new_rate=$(awk "BEGIN {printf \"%.0f\", $sample_rate * $pitch_factor}")
        ffmpeg -i "$part2" -af "asetrate=$new_rate,aresample=$sample_rate" "$part2_pitched" -y 2>/dev/null
    fi
    
    # Concatenate both parts
    echo "file '$part1_pitched'" > "$TEMP_DIR/concat_${attacker_id}_${target_id}.txt"
    echo "file '$part2_pitched'" >> "$TEMP_DIR/concat_${attacker_id}_${target_id}.txt"
    ffmpeg -f concat -safe 0 -i "$TEMP_DIR/concat_${attacker_id}_${target_id}.txt" -c copy "$output_file" -y 2>/dev/null
    
    # Clean up temporary split files
    rm -f "$part1" "$part2" "$part1_pitched" "$part2_pitched" "$TEMP_DIR/concat_${attacker_id}_${target_id}.txt"
}

echo "Starting robot sound file creation..."

# Step 1: Copy and pitch-shift base sounds for each robot
echo "Creating base sound files (hit_wall, move, win)..."

for robot_id in 0 1 2 3 4; do
    pitch_shift=${PITCH_SHIFTS[$robot_id]}
    echo "  Processing Robot $robot_id (pitch shift: $pitch_shift semitones)"
    
    # hit_wall
    apply_pitch_shift "$RAW_DIR/robot_hit_wall.mp3" "$TEMP_DIR/robot_${robot_id}_hit_wall.mp3" "$pitch_shift"
    
    # move
    apply_pitch_shift "$RAW_DIR/robot_move.mp3" "$TEMP_DIR/robot_${robot_id}_move.mp3" "$pitch_shift"
    
    # win
    apply_pitch_shift "$RAW_DIR/robot_win.mp3" "$TEMP_DIR/robot_${robot_id}_win.mp3" "$pitch_shift"
done

# Step 2: Create hit_robot sounds (5x5 combinations with split pitching)
echo "Creating hit_robot sound files (split: first half=attacker, second half=target-4 semitones)..."

for attacker_id in 0 1 2 3 4; do
    echo "  Processing Robot $attacker_id hitting other robots"
    
    for target_id in 0 1 2 3 4; do
        attacker_pitch=${PITCH_SHIFTS[$attacker_id]}
        target_pitch=$((${PITCH_SHIFTS[$target_id]} + TARGET_BASE_OFFSET))
        echo "    -> Robot $attacker_id hits Robot $target_id (1st half: $attacker_pitch, 2nd half: $target_pitch semitones)"
        create_hit_robot_sound "$RAW_DIR/robot_hit_robot.mp3" "$TEMP_DIR/robot_${attacker_id}_hits_robot_${target_id}.mp3" "$attacker_id" "$target_id"
    done
done

# Step 3: Copy all files to the raw directory
echo "Copying files to $RAW_DIR..."
cp "$TEMP_DIR"/*.mp3 "$RAW_DIR/"

# Step 4: Verify all files were created
echo "Verifying file creation..."
expected_files=$((5 * 3 + 5 * 5))  # 15 base files + 25 hit_robot files
created_files=$(ls "$RAW_DIR"/robot_*.mp3 | wc -l)

echo "Expected files: $expected_files"
echo "Created files: $created_files"

# Cleanup
rm -rf "$TEMP_DIR"

if [ "$created_files" -ge "$expected_files" ]; then
    echo "✓ All robot sound files created successfully!"
else
    echo "✗ Warning: Not all files were created. Check ffmpeg installation."
fi

echo "Done!"
