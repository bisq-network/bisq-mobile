#!/bin/bash

# Automated BrowserStack device batch testing script
# This script runs tests against pre-created device batches

set -e

# Configuration
BATCH_CONFIG_DIR="device_batches"
ORIGINAL_CONFIG="browserstack.yml"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Starting automated BrowserStack device batch testing${NC}"
echo -e "${BLUE}================================================${NC}"

# Function to run tests for a batch
run_batch_tests() {
    local batch_num="$1"
    local config_file="$2"
    
    echo -e "${YELLOW}📱 Running tests for batch $batch_num...${NC}"
    
    # Backup original config
    cp "$ORIGINAL_CONFIG" "${ORIGINAL_CONFIG}.backup"
    
    # Use batch config
    cp "$config_file" "$ORIGINAL_CONFIG"
    
    # Run tests
    local start_time=$(date +%s)
    if mvn test -P bisq-test; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo -e "${GREEN}✅ Batch $batch_num completed successfully in ${duration}s${NC}"
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        echo -e "${RED}❌ Batch $batch_num failed after ${duration}s${NC}"
    fi
    
    # Restore original config
    cp "${ORIGINAL_CONFIG}.backup" "$ORIGINAL_CONFIG"
    rm "${ORIGINAL_CONFIG}.backup"
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

for ((i=1; i<=batch_count; i++)); do
    batch_file="${BATCH_CONFIG_DIR}/batch_${i}.yml"
    
    if [ -f "$batch_file" ]; then
        echo -e "${BLUE}Batch $i of $batch_count${NC}"
        run_batch_tests "$i" "$batch_file"
        echo ""
    else
        echo -e "${YELLOW}⚠️  Batch file $batch_file not found, skipping...${NC}"
    fi
done

# Final summary
echo -e "${BLUE}📊 Final Summary${NC}"
echo -e "${BLUE}===============${NC}"
echo "Batch configurations saved in: $BATCH_CONFIG_DIR/"

echo -e "${GREEN}🎉 Automated testing completed!${NC}"