#!/bin/bash

# Automated BrowserStack device batch testing script
# This script runs tests against pre-created device batches

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BATCH_CONFIG_DIR="${SCRIPT_DIR}/device_batches"
ORIGINAL_CONFIG="${SCRIPT_DIR}/browserstack.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting automated BrowserStack device batch testing${NC}"
echo -e "${BLUE}================================================${NC}"

# Function to restore original configuration
restore_config() {
    if [ -f "${ORIGINAL_CONFIG}.backup" ]; then
        cp "${ORIGINAL_CONFIG}.backup" "$ORIGINAL_CONFIG"
        rm "${ORIGINAL_CONFIG}.backup"
    fi
}

# Function to run tests for a batch
run_batch_tests() {
    local batch_num="$1"
    local config_file="$2"
    local start_time
    local end_time
    local duration
    local rc
    
    echo -e "${YELLOW}📱 Running tests for batch $batch_num...${NC}"
    
    # Backup original config
    cp "$ORIGINAL_CONFIG" "${ORIGINAL_CONFIG}.backup"
    
    # Set trap to ensure cleanup on any exit
    trap restore_config EXIT
    
    # Use batch config
    cp "$config_file" "$ORIGINAL_CONFIG"
    
    # Run tests and capture exit code
    start_time=$(date +%s)
    if mvn -B -f "${SCRIPT_DIR}/pom.xml" test -P bisq-test; then
        rc=0
    else
        rc=$?
    fi
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    # Clear the trap since we're about to restore manually
    trap - EXIT
    
    # Restore original config
    restore_config
    
    # Report results based on exit code
    if [ $rc -eq 0 ]; then
        echo -e "${GREEN}✅ Batch $batch_num completed successfully in ${duration}s${NC}"
    else
        echo -e "${RED}❌ Batch $batch_num failed after ${duration}s${NC}"
    fi
    
    # Return the original exit code
    return $rc
}

# Count available batch files
batch_count=$(ls ${BATCH_CONFIG_DIR}/batch_*.yml 2>/dev/null | wc -l)

if [ $batch_count -eq 0 ]; then
    echo -e "${RED}❌ No batch files found in $BATCH_CONFIG_DIR/${NC}"
    echo "Please create batch_1.yml, batch_2.yml, etc. in the $BATCH_CONFIG_DIR directory"
    exit 1
fi

echo -e "${BLUE}📊 Found $batch_count batch files${NC}"

# Run tests for each batch
echo -e "${BLUE}🧪 Starting test execution...${NC}"
echo -e "${BLUE}==============================${NC}"

# Initialize failure tracking
failed_batches=0
total_batches=0

for ((i=1; i<=batch_count; i++)); do
    batch_file="${BATCH_CONFIG_DIR}/batch_${i}.yml"
    
    if [ -f "$batch_file" ]; then
        echo -e "${BLUE}Batch $i of $batch_count${NC}"
        total_batches=$((total_batches + 1))
        
        # Run batch tests and capture exit status immediately
        if ! run_batch_tests "$i" "$batch_file"; then
            failed_batches=$((failed_batches + 1))
        fi
        echo ""
    else
        echo -e "${YELLOW}⚠️  Batch file $batch_file not found, skipping...${NC}"
    fi
done

# Final summary
echo -e "${BLUE}📊 Final Summary${NC}"
echo -e "${BLUE}===============${NC}"
echo "Batch configurations saved in: $BATCH_CONFIG_DIR/"
echo "Total batches executed: $total_batches"
echo "Failed batches: $failed_batches"

# Exit with appropriate status based on failures
if [ $failed_batches -eq 0 ]; then
    echo -e "${GREEN}🎉 All batches completed successfully!${NC}"
    exit 0
else
    echo -e "${RED}❌ $failed_batches out of $total_batches batches failed${NC}"
    exit 1
fi