#!/bin/bash

set -e  # Exit on error

# Define paths
GRADLE_PROPERTIES="gradle.properties"
MODULE_NODE="androidNode"
MODULE_CLIENT="androidClient"
MODULE_NAME="$1"
APP_TYPE=""

echo "Bisq Android Mobile Release Script (node/client)"

if [ -z "$MODULE_NAME" ]; then
    echo "Please pass in the module name to release: node or client. e.g. './release.sh node'"
    exit 1
fi
if [ "$MODULE_NAME" == "client" ]; then
    APP_TYPE="client"
    MODULE_NAME="$MODULE_CLIENT"
elif [ "$MODULE_NAME" == "node" ]; then
    APP_TYPE="node"
    MODULE_NAME="$MODULE_NODE"
else
    echo "Invalid parameter, modules are node or client"
    exit 2
fi

# Extract current version
CURRENT_VERSION=$(grep "^$APP_TYPE.android.version=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
# Extract current version code
CURRENT_VERSION_CODE=$(grep "^$APP_TYPE.android.version.code=" "$GRADLE_PROPERTIES" | cut -d'=' -f2)
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

# Split version into major, minor, patch
MAJOR=$(echo "$CURRENT_VERSION" | cut -d'.' -f1)
MINOR=$(echo "$CURRENT_VERSION" | cut -d'.' -f2)
PATCH=$(echo "$CURRENT_VERSION" | cut -d'.' -f3)

# Increment the patch version
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="$MAJOR.$MINOR.$NEW_PATCH"

echo "$APP_TYPE: first we create a branch for the release - you will loose any uncommited changes"
git reset --hard
git checkout main
BRANCH="release/android_$APP_TYPE_$CURRENT_VERSION"
git checkout -b "$BRANCH"
git push --set-upstream origin "$BRANCH"

echo "$APP_TYPE: Building Android release out of your current branch"
git status

# Build AAB
./gradlew "$MODULE_NAME:clean"
./gradlew "$MODULE_NAME:bundleRelease"

# Tag and push
git tag "$APP_TYPE-release-$CURRENT_VERSION"
git push --tags

echo "Build complete. AAB is located at: $MODULE_NAME/build/outputs/bundle/release/"

# Update gradle.properties
sed -i '' "s/^$APP_TYPE.android.version=.*/$APP_TYPE.android.version=$NEW_VERSION/" "$GRADLE_PROPERTIES"
sed -i '' "s/^$APP_TYPE.android.version.code=.*/$APP_TYPE.android.version.code=$NEW_VERSION_CODE/" "$GRADLE_PROPERTIES"

echo "Updated $APP_TYPE.android.version to $NEW_VERSION to start its development"

# Commit the version bump
git add "$GRADLE_PROPERTIES"
git commit -m "Bump $APP_TYPE.android.version to $NEW_VERSION to start its development"

echo "All done, please create a pull request and merge this changes :)"

exit 0