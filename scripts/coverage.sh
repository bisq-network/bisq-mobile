#!/bin/bash

# Diff Coverage Check Script
# Checks code coverage for changes against the base branch

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Find repository root
REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)
if [ -z "$REPO_ROOT" ]; then
    print_error "Not in a git repository"
    exit 1
fi
cd "$REPO_ROOT"

echo "🔍 Running diff coverage check..."
echo ""

# Read diff coverage threshold from gradle.properties
DIFF_COVERAGE_THRESHOLD=$(grep "kover.diff.coverage.minimum" "$REPO_ROOT/gradle.properties" | cut -d'=' -f2)
if [ -z "$DIFF_COVERAGE_THRESHOLD" ]; then
    print_error "Error: kover.diff.coverage.minimum property not found in gradle.properties"
    exit 1
fi

# Detect base branch
if git rev-parse --verify upstream/main >/dev/null 2>&1; then
    BASE_BRANCH="upstream/main"
elif git rev-parse --verify origin/main >/dev/null 2>&1; then
    BASE_BRANCH="origin/main"
else
    print_error "Could not find base branch (tried upstream/main, origin/main)"
    exit 1
fi

print_info "Using base branch: $BASE_BRANCH"
print_info "Required diff coverage: ${DIFF_COVERAGE_THRESHOLD}%"
echo ""

# Generate coverage reports (both XML and HTML)
echo "📊 Generating coverage reports..."
./gradlew koverXmlReport koverHtmlReport
print_info "Kover HTML report available at: $REPO_ROOT/build/reports/kover/html/index.html"

# Build source roots array
echo "🔍 Finding Kotlin source directories..."
SRC_ROOTS=()
while IFS= read -r dir; do
    SRC_ROOTS+=("$dir")
done < <(find . -type d -path "*/src/*/kotlin" -not -path "*/build/*" | sed 's|^\./||')

if [ ${#SRC_ROOTS[@]} -eq 0 ]; then
    print_error "No Kotlin source directories found"
    exit 1
fi

echo "Found ${#SRC_ROOTS[@]} source directories"
echo ""

# Create diff-cover report directory
mkdir -p build/reports/diff-cover

# Run diff-cover
echo "📈 Analyzing diff coverage..."
set +e
diff-cover build/reports/kover/report.xml \
    --compare-branch="$BASE_BRANCH" \
    --fail-under="$DIFF_COVERAGE_THRESHOLD" \
    --format html:build/reports/diff-cover/diff-coverage.html \
    --src-roots "${SRC_ROOTS[@]}"
DIFF_COVER_EXIT_CODE=$?
set -e

print_info "Diff coverage report available at: $REPO_ROOT/build/reports/diff-cover/diff-coverage.html"

echo ""

if [ $DIFF_COVER_EXIT_CODE -eq 0 ]; then
    print_success "Diff coverage meets the threshold (≥${DIFF_COVERAGE_THRESHOLD}%)!"
else
    print_error "Diff coverage is below the required ${DIFF_COVERAGE_THRESHOLD}%"
    echo ""
    echo "Please add tests for your changes to meet the coverage requirement."
    echo ""
    print_info "Tip: For detailed source code coverage (red/green/yellow line annotations),"
    print_info "      check the Kover HTML report at:"
    print_info "      $REPO_ROOT/build/reports/kover/html/index.html"

    # Only open the HTML report for review if coverage fails
    REPORT_PATH="$REPO_ROOT/build/reports/diff-cover/diff-coverage.html"
    if [ -f "$REPORT_PATH" ]; then
        if command -v open &>/dev/null; then
            print_info "Opening coverage report..."
            open "$REPORT_PATH" || true
        elif command -v xdg-open &>/dev/null; then
            print_info "Opening coverage report..."
            xdg-open "$REPORT_PATH" || true
        else
            print_info "Report available at: $REPORT_PATH"
        fi
    else
        print_error "Coverage report not found at: $REPORT_PATH"
    fi
fi

echo ""
exit $DIFF_COVER_EXIT_CODE
