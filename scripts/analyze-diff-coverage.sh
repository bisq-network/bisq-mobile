#!/bin/bash
# analyze-diff-coverage.sh
# Smart diff coverage analyzer that detects code movement and refactoring
#
# HOW IT WORKS:
# This script analyzes git diffs to determine if changes are refactoring or new code,
# then applies appropriate coverage thresholds:
#
# THRESHOLDS:
#   - All changes: 80% coverage required (maintainer decides ACK/nACK per case)
#
# DETECTION LOGIC (informational only - all paths use 80% threshold):
#   The script detects movement, deletion, and minimal changes for logging purposes,
#   but applies the same 80% threshold regardless. This allows maintainers to see
#   the nature of changes while making case-by-case decisions on coverage requirements.
#
# USAGE:
#   ./scripts/analyze-diff-coverage.sh [base-branch] [coverage-xml-path]
#
# EXAMPLES:
#   ./scripts/analyze-diff-coverage.sh
#   ./scripts/analyze-diff-coverage.sh origin/develop
#   ./scripts/analyze-diff-coverage.sh origin/main build/reports/kover/report.xml

set -e

# Configuration
BASE_BRANCH="${1:-origin/main}"
COVERAGE_XML="${2:-build/reports/kover/report.xml}"
DEFAULT_THRESHOLD=80
## NOTE: Team decided to remove smart checks and leave only 80% for all letting the maintainer to
## decide ACK/nACK on a per-case basis
REFACTORING_THRESHOLD=80
MOVEMENT_RATIO_THRESHOLD=0.5

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Smart Diff Coverage Analyzer ===${NC}"
echo ""

# Ensure we're in a git repository
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${RED}Error: Not in a git repository${NC}"
    exit 1
fi

# Ensure base branch exists and resolve the reference
BASE_REF="$BASE_BRANCH"
if ! git rev-parse --verify "$BASE_BRANCH" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Base branch $BASE_BRANCH not found locally, fetching...${NC}"

    # Extract branch name from origin/branch format if present
    BRANCH_NAME="${BASE_BRANCH#origin/}"

    # Fetch the specific branch (or all if that fails)
    if ! git fetch origin "$BRANCH_NAME" 2>/dev/null; then
        echo -e "${YELLOW}Fetching all branches...${NC}"
        git fetch --all
    fi

    # Re-check if BASE_BRANCH is now resolved
    if ! git rev-parse --verify "$BASE_BRANCH" > /dev/null 2>&1; then
        # Try with origin/ prefix
        if git rev-parse --verify "origin/$BRANCH_NAME" > /dev/null 2>&1; then
            BASE_REF="origin/$BRANCH_NAME"
            echo -e "${GREEN}Using $BASE_REF as base reference${NC}"
        else
            echo -e "${RED}Error: Cannot resolve base branch $BASE_BRANCH${NC}"
            exit 1
        fi
    fi
fi

# Analyze the diff for code movement
echo -e "${BLUE}Analyzing code changes...${NC}"

# Get total lines changed (additions + deletions)
TOTAL_STATS=$(git diff --numstat "$BASE_REF"...HEAD -- '*.kt' | awk '{add+=$1; del+=$2} END {print add, del}')
TOTAL_ADDED=$(echo "$TOTAL_STATS" | awk '{print $1}')
TOTAL_DELETED=$(echo "$TOTAL_STATS" | awk '{print $2}')
TOTAL_CHANGED=$((TOTAL_ADDED + TOTAL_DELETED))

echo -e "  Lines added:   ${GREEN}$TOTAL_ADDED${NC}"
echo -e "  Lines deleted: ${RED}$TOTAL_DELETED${NC}"
echo -e "  Total changed: $TOTAL_CHANGED"
echo ""

# Detect moved/copied code with 80% similarity threshold
# -M80% detects renames/moves within files
# -C80% detects copies across files
MOVE_STATS=$(git diff -M80% -C80% --numstat "$BASE_REF"...HEAD -- '*.kt' | awk '{add+=$1; del+=$2} END {print add, del}')
MOVE_ADDED=$(echo "$MOVE_STATS" | awk '{print $1}')
MOVE_DELETED=$(echo "$MOVE_STATS" | awk '{print $2}')

# Calculate how many lines are likely moved (not truly new)
# If move detection reduces the diff, those lines were moved
LIKELY_MOVED=$((TOTAL_CHANGED - (MOVE_ADDED + MOVE_DELETED)))

if [ "$LIKELY_MOVED" -lt 0 ]; then
    LIKELY_MOVED=0
fi

echo -e "${BLUE}Code movement analysis:${NC}"
echo -e "  Likely moved/refactored: ${YELLOW}$LIKELY_MOVED${NC} lines"

# Calculate movement ratio
if [ "$TOTAL_CHANGED" -gt 0 ]; then
    MOVEMENT_RATIO=$(echo "scale=2; $LIKELY_MOVED / $TOTAL_CHANGED" | bc)
else
    MOVEMENT_RATIO=0
fi

echo -e "  Movement ratio: ${YELLOW}${MOVEMENT_RATIO}${NC} (threshold: $MOVEMENT_RATIO_THRESHOLD)"
echo ""

# Determine appropriate coverage threshold
COVERAGE_THRESHOLD=$DEFAULT_THRESHOLD
THRESHOLD_REASON="default (new code)"

# Calculate deletion ratio (how much code was removed vs added)
if [ "$TOTAL_ADDED" -gt 0 ]; then
    DELETION_RATIO=$(echo "scale=2; $TOTAL_DELETED / $TOTAL_ADDED" | bc)
else
    DELETION_RATIO=0
fi

echo -e "${BLUE}Refactoring indicators:${NC}"
echo -e "  Deletion ratio: ${YELLOW}${DELETION_RATIO}${NC} (deleted/added)"

# Check if this is primarily code movement/refactoring (informational only)
# Priority order: movement > net deletion > minimal changes
# Note: All paths use the same 80% threshold; detection is for logging purposes
if (( $(echo "$MOVEMENT_RATIO >= $MOVEMENT_RATIO_THRESHOLD" | bc -l) )); then
    COVERAGE_THRESHOLD=$REFACTORING_THRESHOLD
    THRESHOLD_REASON="refactoring detected (${MOVEMENT_RATIO} movement ratio)"
    echo -e "${GREEN}✓ Significant code movement detected${NC}"
    echo -e "  Applying coverage threshold: ${GREEN}${COVERAGE_THRESHOLD}%${NC}"
elif (( $(echo "$DELETION_RATIO >= 2.5" | bc -l) )); then
    # If deleted >= 2.5x added, it's likely simplification/cleanup
    COVERAGE_THRESHOLD=$REFACTORING_THRESHOLD
    THRESHOLD_REASON="code simplification (deleted ${TOTAL_DELETED} lines, added ${TOTAL_ADDED})"
    echo -e "${GREEN}✓ Code simplification detected${NC}"
    echo -e "  Applying coverage threshold: ${GREEN}${COVERAGE_THRESHOLD}%${NC}"
elif [ "$TOTAL_ADDED" -le 10 ] && [ "$TOTAL_DELETED" -gt 0 ]; then
    # Minimal changes with some deletion (likely refactoring, not new feature)
    COVERAGE_THRESHOLD=$REFACTORING_THRESHOLD
    THRESHOLD_REASON="minimal changes with deletion (<=10 lines added, ${TOTAL_DELETED} deleted)"
    echo -e "${GREEN}✓ Minimal refactoring detected${NC}"
    echo -e "  Applying coverage threshold: ${GREEN}${COVERAGE_THRESHOLD}%${NC}"
else
    echo -e "${BLUE}ℹ Standard coverage threshold applies: ${COVERAGE_THRESHOLD}%${NC}"
fi

echo ""
echo -e "${BLUE}=== Running diff-cover ===${NC}"
echo -e "  Threshold: ${YELLOW}${COVERAGE_THRESHOLD}%${NC} ($THRESHOLD_REASON)"
echo ""

# Find all Kotlin source roots
SRC_ROOTS=()
while IFS= read -r dir; do
    SRC_ROOTS+=("$dir")
done < <(find . -type d -path "*/src/*/kotlin" -not -path "*/build/*" | sed 's|^\./||')

echo "Found ${#SRC_ROOTS[@]} source directories"
echo ""

# Create output directory
mkdir -p build/reports/diff-cover

# Run diff-cover with the determined threshold
if [ -f "$COVERAGE_XML" ]; then
    if diff-cover "$COVERAGE_XML" \
        --compare-branch="$BASE_REF" \
        --fail-under="${COVERAGE_THRESHOLD}" \
        --html-report build/reports/diff-cover/diff-cover.html \
        --src-roots "${SRC_ROOTS[@]}"; then
        echo ""
        echo -e "${GREEN}✓ Diff coverage check passed!${NC}"
        echo -e "  Report: build/reports/diff-cover/diff-cover.html"
        exit 0
    else
        echo ""
        echo -e "${RED}✗ Diff coverage check failed${NC}"
        echo -e "  Required: ${COVERAGE_THRESHOLD}%"
        echo -e "  Reason: $THRESHOLD_REASON"
        echo -e "  Report: build/reports/diff-cover/diff-cover.html"
        exit 1
    fi
else
    echo -e "${RED}Error: Coverage XML not found at $COVERAGE_XML${NC}"
    echo "Run: ./gradlew koverXmlReport"
    exit 1
fi

