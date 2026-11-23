@echo off
setlocal EnableDelayedExpansion

REM Vérification des trois extensions sous Windows cmd.
REM Usage : verifier_extensions.bat
REM Surcharges : JAVA_HOME, JAVAC, JAVA.
REM Un JAVA_HOME inexistant est ignoré ; recherche aussi %LOCALAPPDATA%\Temp\jdk17.

set "ERRORS=0"
set "JAVAC_CMD="
set "JAVA_CMD="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    ) else (
        echo REMARQUE : JAVA_HOME inexistant ignoré : %JAVA_HOME%
    )
)
if defined JAVAC if exist "%JAVAC%" set "JAVAC_CMD=%JAVAC%"
if defined JAVA if exist "%JAVA%" set "JAVA_CMD=%JAVA%"
where javac >nul 2>&1
if not errorlevel 1 (
    if not defined JAVAC_CMD set "JAVAC_CMD=javac"
    if not defined JAVA_CMD set "JAVA_CMD=java"
)
if not defined JAVAC_CMD (
    for %%R in (
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
        "C:\Program Files\Java\jdk-27.0.1.1"
        "%ProgramFiles%\Eclipse Adoptium"
        "%LOCALAPPDATA%\Temp\jdk17\jdk-17.0.21.1+2"
    ) do (
        if exist "%%~R\bin\javac.exe" if not defined JAVAC_CMD set "JAVAC_CMD=%%~R\bin\javac.exe"
    )
)
if not defined JAVAC_CMD (
    for /d %%D in ("%ProgramFiles%\Java\jdk-*") do (
        if exist "%%~D\bin\javac.exe" if not defined JAVAC_CMD set "JAVAC_CMD=%%~D\bin\javac.exe"
    )
)
if not defined JAVAC_CMD (
    for /d %%D in ("%LOCALAPPDATA%\Temp\jdk17\*") do (
        if exist "%%~D\bin\javac.exe" if not defined JAVAC_CMD set "JAVAC_CMD=%%~D\bin\javac.exe"
    )
)
if not defined JAVAC_CMD (
    set "JAVAC_CMD=javac"
    set "JAVA_CMD=java"
)
if not defined JAVA_CMD (
    if /I not "!JAVAC_CMD!"=="javac" (
        for %%J in ("!JAVAC_CMD!") do set "JDK_BIN=%%~dpJ"
        if exist "!JDK_BIN!java.exe" set "JAVA_CMD=!JDK_BIN!java.exe"
    )
)
if not defined JAVA_CMD set "JAVA_CMD=java"

set "JVER="
for /f "tokens=2 delims= " %%V in ('"!JAVAC_CMD!" -version 2^>^&1') do if not defined JVER set "JVER=%%V"
for /f "tokens=1 delims=." %%M in ("!JVER!") do set "JMAJOR=%%M"
if not defined JMAJOR set "JMAJOR=0"
if !JMAJOR! LSS 17 (
    set /a ERRORS+=1
    echo ERREUR : javac doit etre Java 17 ou plus recent ^(trouve : !JVER!^) 1>&2
) else (
    echo OK : javac !JVER! avec !JAVAC_CMD!
)
set "RVER="
for /f "tokens=3 delims= " %%V in ('"!JAVA_CMD!" -version 2^>^&1') do if not defined RVER set "RVER=%%V"
set "RVER=!RVER:"=!"
for /f "tokens=1 delims=." %%M in ("!RVER!") do set "RMAJOR=%%M"
if not defined RMAJOR set "RMAJOR=0"
if !RMAJOR! LSS 17 (
    set /a ERRORS+=1
    echo ERREUR : java doit etre Java 17 ou plus recent ^(trouve : !RVER!^) 1>&2
) else (
    echo OK : java !RVER! avec !JAVA_CMD!
)

set "OUT=%TEMP%\verifier_extensions_out.txt"

echo === 0^) Compilation et non-régression ===
if exist out rmdir /s /q out
mkdir out 2>nul
"!JAVAC_CMD!" --release 17 -d out src\*.java
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : compilation impossible 1>&2
    goto summary
)
echo OK : javac --release 17 -d out src\*.java
"!JAVA_CMD!" -cp out TestRunner > "%OUT%" 2>&1
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : TestRunner en échec 1>&2
) else (
    echo OK : TestRunner
)

REM Les expressions comme "a*" sont developpees par le lanceur Windows si
REM des fichiers du repertoire courant commencent par 'a'. On lance donc Java
REM depuis out/ avec -cp . (aucun fichier 'a*' a cet endroit).
pushd out
set "PUSHED=1"

echo.
echo === ^(a^) Déterminisation : --dfa ===
"!JAVA_CMD!" -cp . DfaMain "(a|b)*abb" abb aabb ab > "%OUT%" 2>&1
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : dfa-1 code de retour 1>&2
)
for %%L in ("abb : OUI" "aabb : OUI" "ab : NON") do (
    findstr /c:%%L "%OUT%" >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : dfa-1 ligne absente %%~L 1>&2
    ) else (
        echo OK : dfa-1 %%~L
    )
)

"!JAVA_CMD!" -cp . DfaMain "a*+?" "" a aa b > "%OUT%" 2>&1
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : dfa-2 code de retour 1>&2
)
for %%L in ("aa : OUI" "b : NON") do (
    findstr /c:%%L "%OUT%" >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : dfa-2 ligne absente %%~L 1>&2
    ) else (
        echo OK : dfa-2 %%~L
    )
)

"!JAVA_CMD!" -cp . DfaMain "(a?)*" "" a aa b > "%OUT%" 2>&1
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : dfa-3 code de retour 1>&2
)
for %%L in ("aa : OUI" "b : NON") do (
    findstr /c:%%L "%OUT%" >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : dfa-3 ligne absente %%~L 1>&2
    ) else (
        echo OK : dfa-3 %%~L
    )
)

if not exist ..\automates mkdir ..\automates
"!JAVA_CMD!" -cp . DfaMain --dot ..\automates\dfa_ababb.dot "(a|b)*abb" abb >nul 2>&1
if exist ..\automates\dfa_ababb.dot (
    echo OK : export AFD : automates\dfa_ababb.dot
) else (
    set /a ERRORS+=1
    echo ERREUR : export AFD absent 1>&2
)

echo.
echo === ^(b^) Équivalence : --equiv ===
"!JAVA_CMD!" -cp . EquivMain "a*" "(a*)*" > "%OUT%" 2>&1
findstr /c:"EQUIVALENTS" "%OUT%" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : equiv-1 1>&2
) else (
    echo OK : equiv-1 a* = ^(a*^)*
)

"!JAVA_CMD!" -cp . EquivMain "a|b" "b|a" > "%OUT%" 2>&1
findstr /c:"EQUIVALENTS" "%OUT%" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : equiv-2 1>&2
) else (
    echo OK : equiv-2 a^|b = b^|a
)

"!JAVA_CMD!" -cp . EquivMain "a*" "a+" > "%OUT%" 2>&1
findstr /c:"DIFFERENTS" "%OUT%" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : equiv-3 1>&2
) else (
    echo OK : equiv-3 a* different de a+
)

"!JAVA_CMD!" -cp . EquivMain "a?" "(@|a)" --dot-prefix ..\automates\min_ >nul 2>&1
if exist ..\automates\min_1.dot if exist ..\automates\min_2.dot (
    echo OK : export AFD minimaux
) else (
    set /a ERRORS+=1
    echo ERREUR : export AFD minimaux absent 1>&2
)

echo.
echo === ^(c^) Reprise sur erreur : --recover ===
"!JAVA_CMD!" -cp . RecoveryMain "a||b" > "%OUT%" 2>&1
if not errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : recover-1 code de retour 1>&2
)
findstr /c:"position 3" "%OUT%" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : recover-1 position 1>&2
) else (
    echo OK : recover-1 a^|^|b
)

"!JAVA_CMD!" -cp . RecoveryMain "a(+b" > "%OUT%" 2>&1
if not errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : recover-2 code de retour 1>&2
)
for %%P in ("position 3" "position 5") do (
    findstr /c:%%P "%OUT%" >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : recover-2 %%P absente 1>&2
    ) else (
        echo OK : recover-2 %%P
    )
)

"!JAVA_CMD!" -cp . RecoveryMain "a#b" ab > "%OUT%" 2>&1
if not errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : recover-3 code de retour 1>&2
)
findstr /c:"position 2" "%OUT%" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : recover-3 position 1>&2
) else (
    echo OK : recover-3 erreur lexicale
)
findstr /c:"ab : OUI" "%OUT%" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : recover-3 mot simulé absent 1>&2
) else (
    echo OK : recover-3 mot simulé
)

if defined PUSHED popd

:summary
del "%OUT%" >nul 2>&1
echo.
echo === Résumé ===
if "!ERRORS!"=="0" (
    echo PASS : 9 tests d'extension corrects.
    endlocal
    exit /b 0
) else (
    echo FAIL : !ERRORS! erreur^(s^).
    endlocal
    exit /b 1
)
