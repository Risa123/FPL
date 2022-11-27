@echo off
echo before continuing
echo do code inspection
echo add changes to wiki
echo add new keywords to fpl-udl
echo check anything else you can think of
echo build artifact
pause
rmdir FPL-other /s /q
rmdir FPL-windows /s /q
del FPL-other.zip
del FPL-windows.zip
"%USERPROFILE%\.jdks\openjdk-18.0.2.1\bin\jpackage" --type app-image --input bin\\artifacts\\FPL --main-jar FPL.jar ^
--main-class risa.fpl.FPL --win-console --app-version 0.6 --vendor Risa123 --description "FPL compiler"
rename FPL FPL-windows
mkdir FPL-windows\\std
mkdir FPL-windows\\gcc
Xcopy project\\src\\std FPL-windows\\std /s /e
Xcopy gcc FPL-windows\\gcc /s /e
copy fpl-udl.xml FPL-windows\\fpl-uld.xml
mkdir FPL-other
mkdir FPL-other\\std
Xcopy project\\src\\std FPL-other\\std /s /e
copy fpl-udl.xml FPL-other\\fpl-udl.xml
copy bin\\artifacts\\FPL\\FPL.jar FPL-other\\FPL.jar
"C:\\Program Files\\7-Zip\7z" a FPL-windows.zip FPL-windows
"C:\\Program Files\\7-Zip\7z" a FPL-other.zip FPL-other