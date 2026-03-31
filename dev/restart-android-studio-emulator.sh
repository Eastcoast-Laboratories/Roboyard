#!/bin/bash

echo "Android Emulator Reset Script"
echo "============================"

# Check if emulator is running
if pgrep -f "qemu-system-x86_64" > /dev/null; then
    echo "WARNING: Emulator process detected."
    echo "Option 1: ADB reset only (quick)"
    echo "Option 2: Full reset (wipe AVD + ADB reset)"
    echo ""
    read -p "Choose option [1/2]: " choice
    
    case $choice in
        1)
            echo "ADB reset only..."
            adb kill-server && adb start-server
            echo "ADB server reset complete. Now restart emulator in Android Studio."
            ;;
        2)
            echo "Full reset: killing emulator processes..."
            pkill -f "qemu-system-x86_64"
            sleep 2
            
            echo "Resetting ADB server..."
            adb kill-server && adb start-server
            
            echo "Wiping AVD data (this will reset the emulator to factory settings)..."
            # You can specify your AVD name or let user choose
            AVD_NAME="Small_Phone"
            echo "Wiping AVD: $AVD_NAME"
            /home/ruben/AndroidSDK/emulator/emulator -avd "$AVD_NAME" -wipe-data -no-window > /dev/null 2>&1 &
            
            echo "Full reset complete. Now start emulator in Android Studio."
            ;;
        *)
            echo "Invalid option. Exiting."
            exit 1
            ;;
    esac
else
    echo "No emulator running. Resetting ADB server..."
    adb kill-server && adb start-server
    echo "ADB server reset complete."
fi
