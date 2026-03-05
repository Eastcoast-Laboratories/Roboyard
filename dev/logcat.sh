#!/bin/bash
# Smart logcat viewer with auto-device selection
# Arguments:
#   -d device  (optional, if none given autodetect)
#   -f filter  (grep pattern, default: HISTORY_SYNC)
#   -n lines   (number of lines, default: 100)
#   -h         help
#
# Examples:
#   ./logcat.sh                          # Auto-select device, HISTORY_SYNC filter
#   ./logcat.sh -d emulator-5554         # Specific device
#   ./logcat.sh -f "AUTO_LOGIN"          # Different filter
#   ./logcat.sh -f "SYNC\|LOGIN" -n 50   # Multiple filters, 50 lines

DEVICE=""
TAG_FILTER="HISTORY_SYNC"
LINES=100

while getopts "d:f:n:h" opt; do
    case $opt in
        d) DEVICE="$OPTARG" ;;
        f) TAG_FILTER="$OPTARG" ;;
        n) LINES="$OPTARG" ;;
        h)
            echo "Usage: $0 [-d device] [-f filter] [-n lines] [-h]"
            echo "  -d device   Device serial (auto-detect if omitted)"
            echo "  -f filter   Grep filter pattern (default: HISTORY_SYNC)"
            echo "  -n lines    Number of lines to show (default: 100)"
            echo "  -h          Show this help"
            exit 0
            ;;
        *) echo "Unknown option: -$OPTARG"; exit 1 ;;
    esac
done


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
