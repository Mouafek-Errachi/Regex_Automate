@echo off
setlocal EnableDelayedExpansion

REM Vérification complète de l'archive sous Windows cmd.
REM Usage : verifier_archive.bat
REM Surcharges : JAVA_HOME, JAVAC, JAVA.
REM Si javac n'est pas dans le PATH, essaie les JDK Eclipse/Adoptium connus
REM puis %LOCALAPPDATA%\Temp\jdk17. Un JAVA_HOME inexistant est ignoré.

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
        "%ProgramFiles%\Eclipse"
        "%ProgramFiles(x86)%\Eclipse"
        "%LOCALAPPDATA%\Programs\Eclipse"
        "%USERPROFILE%\eclipse"
        "C:\eclipse"
        "%ProgramFiles%\Java"
        "%LOCALAPPDATA%\Temp\jdk17\jdk-17.0.21.1+2"
    ) do (
        if exist "%%~R\bin\javac.exe" (
            if not defined JAVAC_CMD set "JAVAC_CMD=%%~R\bin\javac.exe"
        ) else if exist "%%~R\" (
            for /f "delims=" %%J in ('dir /b /s "%%~R\javac.exe" 2^>nul') do (
                if not defined JAVAC_CMD set "JAVAC_CMD=%%J"
            )
        )
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

echo === 1^) Structure de l'archive ===
for %%F in (EQUIPE.txt rapport.pdf README.txt tests\tests.txt) do (
    if exist "%%F" (
        echo OK : pr sent : %%F
    ) else (
        set /a ERRORS+=1
        echo ERREUR : l ment absent : %%F 1>&2
    )
)
for %%D in (src tests automates) do (
    if exist "%%D\." (
        echo OK : dossier pr sent : %%D
    ) else (
        set /a ERRORS+=1
        echo ERREUR : dossier absent : %%D 1>&2
    )
)

echo.
echo === 2^) Version de Java ^(cible 17^) ===
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

echo.
echo === 3^) Compilation ===
if exist out rmdir /s /q out
mkdir out 2>nul
"!JAVAC_CMD!" --release 17 -d out src\*.java
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : compilation impossible 1>&2
    goto summary
)
echo OK : javac --release 17 -d out src\*.java

echo.
echo === 4^) Jeu de tests ===
if exist out\TestRunner.class (
    "!JAVA_CMD!" -cp out TestRunner
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : TestRunner a signal un chec 1>&2
    ) else (
        echo OK : TestRunner
    )
) else (
    set /a ERRORS+=1
    echo ERREUR : out\TestRunner.class absent 1>&2
)

echo.
echo === 5^) G n ration des trois fichiers .dot ===
if not exist automates mkdir automates

"!JAVA_CMD!" -cp out Main --dot "automates\automate_ababb.dot" "(a|b)*abb" "abb" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : g n ration automate_ababb.dot 1>&2
)
if not exist automates\automate_ababb.dot (
    set /a ERRORS+=1
    echo ERREUR : fichier .dot absent : automates\automate_ababb.dot 1>&2
) else (
    findstr /b /c:"digraph AFN {" automates\automate_ababb.dot >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : contenu .dot invalide : automate_ababb.dot 1>&2
    ) else (
        echo OK : dot g n r : automates\automate_ababb.dot
    )
)

"!JAVA_CMD!" -cp out Main --dot "automates\automate_abplus.dot" "(ab)+" "ab" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : g n ration automate_abplus.dot 1>&2
)
if not exist automates\automate_abplus.dot (
    set /a ERRORS+=1
    echo ERREUR : fichier .dot absent : automates\automate_abplus.dot 1>&2
) else (
    findstr /b /c:"digraph AFN {" automates\automate_abplus.dot >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : contenu .dot invalide : automate_abplus.dot 1>&2
    ) else (
        echo OK : dot g n r : automates\automate_abplus.dot
    )
)

"!JAVA_CMD!" -cp out Main --dot "automates\automate_aquestionb.dot" "a?b" "ab" >nul
if errorlevel 1 (
    set /a ERRORS+=1
    echo ERREUR : g n ration automate_aquestionb.dot 1>&2
)
if not exist automates\automate_aquestionb.dot (
    set /a ERRORS+=1
    echo ERREUR : fichier .dot absent : automates\automate_aquestionb.dot 1>&2
) else (
    findstr /b /c:"digraph AFN {" automates\automate_aquestionb.dot >nul
    if errorlevel 1 (
        set /a ERRORS+=1
        echo ERREUR : contenu .dot invalide : automate_aquestionb.dot 1>&2
    ) else (
        echo OK : dot g n r : automates\automate_aquestionb.dot
    )
)

:summary
echo.
echo === R sum ===
if "!ERRORS!"=="0" (
    echo PASS : archive compl te, compilation, tests et .dot corrects.
    endlocal
    exit /b 0
) else (
    echo FAIL : !ERRORS! erreur^(s^) d tect e^(s^).
    endlocal
    exit /b 1
)
