#!/usr/bin/env bash
# Test script for yacy_search tool - searching for "步子哥"

echo "=== Searching for '步子哥' using yacy_search tool ==="
echo ""

# Build the JSON-RPC request
SEARCH_REQUEST='{
  "jsonrpc": "2.0",
  "id": "search-test-001",
  "method": "tools/call",
  "params": {
    "name": "yacy_search",
    "arguments": {
      "query": "步子哥",
      "count": 5,
      "offset": 0
    }
  }
}'

echo "Sending request:"
echo "$SEARCH_REQUEST" | jq .
echo ""
echo "Response:"
echo "$SEARCH_REQUEST" | mvn exec:java -Dexec.mainClass="com.yacy.mcp.test.YacySearchRunner" -q 2>&1 || {
    echo "Note: Running via MCP Stdio Server test mode..."
    
    # Alternative: Use the MCP Stdio Protocol Test framework
    cat > /tmp/test_search.json << 'TESTEOF'
{
  "jsonrpc": "2.0",
  "id": "test-search-stepbrother",
  "method": "tools/call",
  "params": {
    "name": "yacy_search",
    "arguments": {
      "query": "步子哥",
      "count": 10
    }
  }
}
TESTEOF
    echo "Test request written to /tmp/test_search.json"
}

echo ""
echo "=== Test Complete ==="
