#!/bin/bash
# Smart logcat viewer with auto-device selection
# Usage: ./logcat.sh [device-serial] [tag-filter] [lines]
# Examples:
#   ./logcat.sh                          # Auto-select device, all logs, 100 lines
#   ./logcat.sh emulator-5554            # Specific device, all logs, 100 lines
#   ./logcat.sh emulator-5554 HISTORY    # Specific device, HISTORY filter, 100 lines
#   ./logcat.sh "" HISTORY 50            # Auto-select device, HISTORY filter, 50 lines

DEVICE=$1
TAG_FILTER=${2:-""}
LINES=${3:-100}

# If device is empty string, unset it to trigger auto-detection
if [ "$DEVICE" = "" ]; then
    unset DEVICE
fi

# arguments are: 
# -d device (optional, if none given autodetect)
# -f filter (default "HISTORY_SYNC")
# -h help



# Function to get the last log line for a device
get_last_log_line() {
    local device=$1
    adb -s "$device" logcat -d 2>/dev/null | tail -1
}

# If no device specified, find the one with newest logs
if [ -z "$DEVICE" ]; then
    echo "No device specified, auto-detecting..."
    
    # Get list of devices
    devices=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')
    
    if [ -z "$devices" ]; then
        echo "Error: No devices found"
        exit 1
    fi
    
    # Count devices
    device_count=$(echo "$devices" | wc -l)
    
    if [ "$device_count" -eq 1 ]; then
        DEVICE=$(echo "$devices" | head -1)
        echo "Found single device: $DEVICE"
    else
        echo "Multiple devices found, checking for newest logs..."
        newest_device=""
        newest_line=""
        
        for dev in $devices; do
            last_line=$(get_last_log_line "$dev")
            timestamp=$(echo "$last_line" | awk '{print $1" "$2}')
            echo "  $dev: $timestamp"
            
            if [ -z "$newest_line" ] || [[ "$last_line" > "$newest_line" ]]; then
                newest_line=$last_line
                newest_device=$dev
            fi
        done
        
        newest_time=$(echo "$newest_line" | awk '{print $1" "$2}')
        DEVICE=$newest_device
        echo "Selected device with newest logs: $DEVICE ($newest_time)"
    fi
fi


echo "=========================================="
echo "Device: $DEVICE"
echo "Filter: ${TAG_FILTER:-'(all)'}"
echo "Lines: $LINES"
echo "=========================================="
echo ""

# Show the logs
if [ -z "$TAG_FILTER" ]; then
    adb -s "$DEVICE" logcat -d | tail -n "$LINES"
else
    adb -s "$DEVICE" logcat -d | grep "$TAG_FILTER" | tail -n "$LINES"
fi

echo ""
echo "=========================================="
echo "end of log, Device: $DEVICE"
