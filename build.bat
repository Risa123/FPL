@echo off
echo before continuing
echo make sure there are no easy to find bugs
echo do code inspection
echo make sure to are no other things to do
echo build artifact
pause
rmdir FPL /s /q
"%USERPROFILE%\.jdks\openjdk-17.0.1\bin\jpackage" --type app-image  --input bin\\artifacts\\FPL --main-jar FPL.jar ^
--main-class risa.fpl.FPL --win-console --app-version 0.4 --vendor Risa123 --description "FPL compiler"
mkdir FPL\\std
Xcopy project\\src\\std FPL\\std
copy fpl-udl.xml FPL\\fpl-uld.xml
"C:\\Program Files\\7-Zip\7z" a FPL.zip FPL
"C:\\Program Files\\7-Zip\7z" a std.zip project\src\std