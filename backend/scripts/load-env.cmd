@echo off
rem Load backend.env (KEY=VALUE, gitignored) into env vars for java processes started by start-all.ps1.
rem APP_ID -> WX_APP_ID, APP_SECRET -> WX_APP_SECRET; other keys exported as-is.
set "ENV_FILE=%~dp0..\.env"
if not exist "%ENV_FILE%" exit /b 0
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    if not "%%B"=="" set "%%A=%%B"
)
if defined APP_ID if not defined WX_APP_ID set "WX_APP_ID=%APP_ID%"
if defined APP_SECRET if not defined WX_APP_SECRET set "WX_APP_SECRET=%APP_SECRET%"
exit /b 0
