#!/bin/bash

# --- MyFlow Python Service Independent Test Runner ---

OUTPUT_FILE="final_test_report.html"
DATA_FILE="myflow_data.json"
API_URL="http://127.0.0.1:5000/api/visualization"

echo "======================================================="
echo "  🚀 Testing Python Microservice with Custom Data"
echo "======================================================="

# 1. Validate JSON file integrity before sending
echo "Validating JSON integrity of $DATA_FILE..."
if ! which jq > /dev/null; then
    echo "!!! WARNING: 'jq' is not installed. Skipping JSON validation."
fi

if which jq > /dev/null && ! jq '.' "$DATA_FILE" > /dev/null 2>&1; then
    echo "!!! ERROR: $DATA_FILE is not valid JSON. Please ensure all keys/strings use DOUBLE QUOTES."
    exit 1
fi
echo "JSON file is valid (or jq is missing and test proceeds)."

# 2. Send data to the running Python API using cat and curl
echo "Sending data from $DATA_FILE to $API_URL..."
# Use cat to pipe the JSON content to curl, reading from stdin (@-)
RESPONSE=$(cat "$DATA_FILE" | curl -s -X POST $API_URL \
    -H "Content-Type: application/json" \
    --data @-)


# 3. Process Response
# Use jq only if it is available for safe JSON processing
if which jq > /dev/null; then
    BASE64_IMAGE=$(echo "$RESPONSE" | jq -r '.graph_image_base64')
    RECOMMENDATION=$(echo "$RESPONSE" | jq -r '.pacing_recommendation')
    # Convert markdown bold (**text**) to HTML bold (<strong>text</strong>)
    RECOMMENDATION=$(echo "$RECOMMENDATION" | sed 's/\*\*\([^*]*\)\*\*/<strong>\1<\/strong>/g')
    PACING_STATE=$(echo "$RESPONSE" | jq -r '.pacing_state')
    LATEST_LOAD=$(echo "$RESPONSE" | jq -r '.latest_load')
    LOAD_THRESHOLD=$(echo "$RESPONSE" | jq -r '.load_threshold')
else
    # Fallback to simple placeholder strings if jq is unavailable
    echo "!!! WARNING: 'jq' not found. Cannot parse response safely. Using placeholders."
    BASE64_IMAGE="[Image Data Missing - Install jq]"
    RECOMMENDATION="[Recommendation Missing - Install jq]"
    PACING_STATE="ERROR"
    LATEST_LOAD="N/A"
    LOAD_THRESHOLD="N/A"
fi


# 4. Check for critical errors or missing image data
if echo "$RESPONSE" | grep -q "400 Bad Request"; then
    echo "!!! ERROR: Python service returned 400 Bad Request. Check JSON syntax in $DATA_FILE."
    echo "$RESPONSE"
    exit 1
fi

if [ "$BASE64_IMAGE" = "" ] || [ "$BASE64_IMAGE" = "[Image Data Missing - Install jq]" ]; then
    echo "!!! CRITICAL ERROR: Could not extract Base64 image data. Full response below:"
    echo "$RESPONSE"
    exit 1
fi


# 5. Print the Adaptive Pacing result (with formatting for presentation)
FORMATTED_LOAD=$(printf "%.1f" "$LATEST_LOAD")
FORMATTED_THRESHOLD=$(printf "%.1f" "$LOAD_THRESHOLD")

echo "-------------------------------------------------------"
echo "ADAPTIVE PACING RESULT:"
echo "State: $PACING_STATE"
echo "Message: $RECOMMENDATION"
echo "Load: $FORMATTED_LOAD | Threshold: $FORMATTED_THRESHOLD"
echo "-------------------------------------------------------"


# 6. Define the HTML Template using Here Document (EOF_HTML) for safe multi-line string handling
# Variables are safely replaced later using 'sed'.
# Determine colors based on pacing state
if [[ "$PACING_STATE" == "GREEN LIGHT" ]]; then
    STATUS_COLOR_BG="bg-green-50"
    STATUS_COLOR_BORDER="border-green-500"
    STATUS_COLOR_TEXT="text-green-700"
    STATUS_COLOR_DARK="text-green-900"
elif [[ "$PACING_STATE" == *"WARNING"* ]] || [[ "$PACING_STATE" == *"YELLOW"* ]]; then
    STATUS_COLOR_BG="bg-yellow-50"
    STATUS_COLOR_BORDER="border-yellow-500"
    STATUS_COLOR_TEXT="text-yellow-700"
    STATUS_COLOR_DARK="text-yellow-900"
else
    STATUS_COLOR_BG="bg-red-50"
    STATUS_COLOR_BORDER="border-red-500"
    STATUS_COLOR_TEXT="text-red-700"
    STATUS_COLOR_DARK="text-red-900"
fi
HTML_TEMPLATE=$(cat << 'EOF_HTML'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>MyFlow Test Report</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
        body { font-family: 'Inter', sans-serif; background-color: #f8fafc; }
        .color-box { width: 1rem; height: 1rem; border-radius: 9999px; }
    </style>
</head>
<body class="p-8">
    <div class="max-w-4xl mx-auto bg-white shadow-xl rounded-lg p-6">
        <header class="border-b pb-4 mb-6">
            <h1 class="text-4xl font-extrabold text-[#43AA8B]">MyFlow Test Analysis</h1>
            <p class="text-gray-500 mt-1">Independent Microservices Validation</p>
        </header>
       <section class="mb-8 p-4 {{STATUS_COLOR_BG}} border-l-4 {{STATUS_COLOR_BORDER}} rounded-lg">
    <h2 class="text-2xl font-semibold {{STATUS_COLOR_TEXT}} mb-2">{{PACING_STATE}}</h2>
    <p class="text-lg {{STATUS_COLOR_DARK}}">{{RECOMMENDATION_TEXT}}</p>
    <div class="mt-3 text-sm {{STATUS_COLOR_TEXT}}">
        Latest Load: <strong>{{LATEST_LOAD}}</strong> | Threshold: <strong>{{LOAD_THRESHOLD}}</strong>
    </div>
</section>
        <section>
            <h2 class="text-2xl font-semibold text-gray-800 mb-4">Factor Contribution Visualization</h2>
            <div class="bg-gray-100 p-4 rounded-lg">
                <img id="graph-img" src="data:image/png;base64,{{BASE64_IMAGE_DATA}}" alt="Daily Factor Contribution Graph" class="w-full h-auto rounded-md shadow-inner">
            </div>
            <p class="text-sm text-gray-500 mt-4">The stacked bars represent the Total Negative Load (TNL). Protective factors are shown below the zero line, actively reducing the daily load.</p>
        </section>
        
        <!-- === COLOR KEY SECTION === -->
        <section class="mt-8">
            <h3 class="text-xl font-semibold text-gray-800 mb-3">Visualization Key (Factor Colors)</h3>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
                <div class="flex items-center space-x-2">
                    <span class="color-box" style="background-color: #FF8C42;"></span>
                    <span class="text-gray-600 font-medium">Stress (0-10)</span>
                </div>
                <div class="flex items-center space-x-2">
                    <span class="color-box" style="background-color: #43AA8B;"></span>
                    <span class="text-gray-600 font-medium">Cognitive Load (Normalized Study)</span>
                </div>
                <div class="flex items-center space-x-2">
                    <span class="color-box" style="background-color: #B22222;"></span>
                    <span class="text-gray-600 font-medium">Sleep Deficit Penalty</span>
                </div>
                <div class="flex items-center space-x-2">
                    <span class="color-box" style="background-color: #2D7DD2;"></span>
                    <span class="text-gray-600 font-medium">Positive Custom Factor (TNL Increase)</span>
                </div>
                <div class="flex items-center space-x-2">
                    <span class="color-box" style="background-color: #F5E663; border: 1px solid #d4c05a;"></span>
                    <span class="text-gray-600 font-medium">Protective Custom Factor (Load Reduction)</span>
                </div>
                <div class="flex items-center space-x-2">
                    <span class="color-box" style="background-color: transparent; border: 2px solid red;"></span>
                    <span class="text-gray-600 font-medium">Tic Level (Red Line)</span>
                </div>
            </div>
        </section>
        <!-- === END COLOR KEY SECTION === -->

    </div>
</body>
</html>
EOF_HTML
)


# 7. Safe Variable Substitution using SED
# The HTML template is first processed to substitute placeholders with variables.


echo "$HTML_TEMPLATE" | \
sed "s|{{PACING_STATE}}|$PACING_STATE|g" | \
sed "s|{{RECOMMENDATION_TEXT}}|$RECOMMENDATION|g" | \
sed "s|{{LATEST_LOAD}}|$FORMATTED_LOAD|g" | \
sed "s|{{LOAD_THRESHOLD}}|$FORMATTED_THRESHOLD|g" | \
sed "s|{{BASE64_IMAGE_DATA}}|$BASE64_IMAGE|g" | \
sed "s|{{STATUS_COLOR_BG}}|$STATUS_COLOR_BG|g" | \
sed "s|{{STATUS_COLOR_BORDER}}|$STATUS_COLOR_BORDER|g" | \
sed "s|{{STATUS_COLOR_TEXT}}|$STATUS_COLOR_TEXT|g" | \
sed "s|{{STATUS_COLOR_DARK}}|$STATUS_COLOR_DARK|g" \
> "$OUTPUT_FILE"

echo "✅ Report saved to $OUTPUT_FILE"


# 8. Open the report in the default browser
if which xdg-open > /dev/null; then
  xdg-open "$OUTPUT_FILE"
elif which open > /dev/null; then
  open "$OUTPUT_FILE"
else
  echo "Report created. Please open $OUTPUT_FILE manually in your browser."
fi
