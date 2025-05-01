#!/bin/bash

# Run from the project root
echo "Installing Cocoapods dependencies..."
cd iosClient || exit
rm -rf Pods
pod install

echo "Opening Xcode workspace..."
open iosClient/iosClient.xcworkspace
