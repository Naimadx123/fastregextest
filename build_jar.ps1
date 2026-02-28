# Create the new manifest file
$manifestContent = "Manifest-Version: 1.0`r`nCreated-By: 21 (Junie AI)`r`nMain-Class: me.naimad.fastregex.FastRegexCLI`r`n`r`n";
New-Item -ItemType Directory -Force -Path dist\META-INF;
Set-Content -Path dist\META-INF\MANIFEST.MF -Value $manifestContent -NoNewline;

# Prepare the content to be zipped
# We'll put classes in the right structure
if (Test-Path temp_jar_build) { Remove-Item -Recurse temp_jar_build }
New-Item -ItemType Directory -Force -Path temp_jar_build;
Copy-Item -Recurse out_classes\* temp_jar_build\;
Copy-Item -Recurse dist\META-INF temp_jar_build\;

# Create the JAR
if (Test-Path dist\fastregex.jar) { Remove-Item dist\fastregex.jar }
Compress-Archive -Path temp_jar_build\* -DestinationPath dist\fastregex_new.zip;
Rename-Item -Path dist\fastregex_new.zip -NewName fastregex.jar;

# Clean up
Remove-Item -Recurse temp_jar_build;
Remove-Item -Recurse dist\META-INF;

Write-Host "Created executable dist\fastregex.jar successfully.";
Get-Item dist\fastregex.jar;
