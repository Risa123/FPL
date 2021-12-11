@echo off
echo before continuing
echo make sure there are no easy to find bugs
echo do code inspection
echo make sure to are no other things to do
echo build artifact
pause
rmdir FPL /s /q
"C:\Users\Risa123\.jdks\openjdk-17.0.1\bin\jpackage" --type app-image  --input bin\\artifacts\\FPL --main-jar FPL.jar ^
--main-class risa.fpl.FPL --win-console --app-version 0.4 --vendor Risa123 --description "FPL compiler"