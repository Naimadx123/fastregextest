# Compile the CLI class
javac -cp dist\fastregex.jar src\me\naimad\fastregex\FastRegexCLI.java -d out_classes;

# Create the new manifest file
$manifestContent = "Manifest-Version: 1.0`r`nCreated-By: 21 (Junie AI)`r`nMain-Class: me.naimad.fastregex.FastRegexCLI`r`n`r`n";
Set-Content -Path MANIFEST.MF -Value $manifestContent -NoNewline;

# Prepare to extract the existing JAR
if (Test-Path dist\fastregex.zip) { Remove-Item dist\fastregex.zip }
Copy-Item dist\fastregex.jar dist\fastregex.zip;
if (Test-Path temp_jar_content) { Remove-Item -Recurse temp_jar_content }
New-Item -ItemType Directory -Force -Path temp_jar_content;
Expand-Archive -Path dist\fastregex.zip -DestinationPath temp_jar_content;

# Copy the newly compiled CLI classes
Copy-Item -Recurse -Force out_classes\* temp_jar_content\;

# Overwrite the manifest
if (!(Test-Path temp_jar_content\META-INF)) { New-Item -ItemType Directory temp_jar_content\META-INF }
Set-Content -Path temp_jar_content\META-INF\MANIFEST.MF -Value $manifestContent -NoNewline;

# Repackage the JAR
if (Test-Path dist\fastregex.jar) { Remove-Item dist\fastregex.jar }
Compress-Archive -Path temp_jar_content\* -DestinationPath dist\fastregex_new.zip;
Rename-Item dist\fastregex_new.zip dist\fastregex.jar;

# Clean up
Remove-Item dist\fastregex.zip;
Remove-Item -Recurse temp_jar_content;
Remove-Item -Recurse out_classes;
Remove-Item MANIFEST.MF;

Write-Host "Repackaged dist\fastregex.jar successfully.";
Get-Item dist\fastregex.jar;
