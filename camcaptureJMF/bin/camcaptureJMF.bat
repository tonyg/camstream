@echo off

setlocal EnableDelayedExpansion

set CP=
for %%F in (*.jar) do set CP=!CP!;%%F

start javaw -cp %CP% net.lshift.camcapture.jmf.Main %1 %2 %3 %4 %5 %6 %7 %8 %9

endlocal
