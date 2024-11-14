// This JS script is to convert translation key, values from
// bisq2 repo (*.properties) to bisq-mobile repo (lyricist library structure)
// It's not fully automated yet, but with some configuration it works now.

const fs = require('fs');
const path = require('path');

// Define the output paths for English and French files
const outputEnFilePath = path.join(__dirname, 'GeneratedEnStrings.kt');
const outputFrFilePath = path.join(__dirname, 'GeneratedFrStrings.kt');

// Function to convert the translation key to Kotlin variable name
function convertKeyToKotlinVariable(key) {
    return key.replace(/\./g, '_').replace(/[{}]/g, '');
}

// Function to parse the .properties file with proper multi-line handling
function parsePropertiesFile(filePath) {
    const content = fs.readFileSync(filePath, 'utf-8');
    const lines = content.split('\n');

    const translations = {};
    let currentKey = null;
    let currentValue = '';

    lines.forEach(line => {
        line = line.trim();

        // Skip empty lines and comments
        if (!line || line.startsWith('#')) return;

        // If line ends with "\", it's a continuation
        if (line.endsWith('\\')) {
            // If this is the start of a new entry, initialize currentKey and currentValue
            if (line.includes('=') && !currentKey) {
                const [key, ...valueParts] = line.split('=');
                currentKey = key.trim();
                currentValue = valueParts.join('=').trim().slice(0, -1); // Remove the trailing "\"
            } else {
                // Append to the current value without the trailing "\"
                currentValue += ' ' + line.slice(0, -1);
            }
        } else {
            // Process the last line of a multi-line or a single line entry
            if (line.includes('=') && !currentKey) {
                // Single line entry
                const [key, ...valueParts] = line.split('=');
                translations[key.trim()] = valueParts.join('=').trim();
            } else if (currentKey) {
                // Final line of a multi-line entry
                currentValue += ' ' + line;
                translations[currentKey] = currentValue;
                currentKey = null;
                currentValue = '';
            }
        }
    });

    return translations;
}

// Function to generate Kotlin code for English and French files
function generateKotlinCode(translations, languageTag, prefix = '') {
    let stringsClass = 'data class Strings(\n';
    let stringsObject = `@LyricistStrings(languageTag = ${languageTag}, default = ${languageTag === "Locales.EN"})\nval ${languageTag}Strings = Strings(\n`;

    for (const [key, value] of Object.entries(translations)) {
        const variableName = convertKeyToKotlinVariable(key);
        stringsClass += `    val ${variableName}: String,\n`;
        stringsObject += `    ${variableName} = "${prefix}${value.replace(/"/g, '\\"')}",\n`;
    }

    stringsClass = stringsClass.trimEnd().slice(0, -1) + '\n)\n';
    stringsObject = stringsObject.trimEnd().slice(0, -1) + '\n)\n';

    return `
package network.bisq.mobile.presentation.i18n

import cafe.adriel.lyricist.LyricistStrings

// Generated Strings data class
${stringsClass}

// Generated ${languageTag} strings
${stringsObject}
`;
}

// Main function to run the conversion and create both files
function convertPropertiesToKotlin(propertiesFilePath) {
    if (!fs.existsSync(propertiesFilePath)) {
        console.error('Properties file not found:', propertiesFilePath);
        return;
    }

    const translations = parsePropertiesFile(propertiesFilePath);
    const enKotlinCode = generateKotlinCode(translations, "Locales.EN");
    const frKotlinCode = generateKotlinCode(translations, "Locales.FR", "[FR] ");

    // Write the generated code to the output files
    fs.writeFileSync(outputEnFilePath, enKotlinCode, 'utf-8');
    fs.writeFileSync(outputFrFilePath, frKotlinCode, 'utf-8');
    console.log('Kotlin strings files generated at:', outputEnFilePath, 'and', outputFrFilePath);
}

// Run the conversion with the provided file path
const propertiesFilePath = process.argv[2];
if (!propertiesFilePath) {
    console.error('Please provide the path to the .properties file as an argument.');
    process.exit(1);
}

convertPropertiesToKotlin(propertiesFilePath);

