; Inno Setup Script for Virtual Steer Companion Windows App
; Download Inno Setup Compiler from: https://jrsoftware.org/isdl.php

[Setup]
AppName=Virtual Steer Companion
AppVersion=1.0
AppPublisher=Virtual Steer
DefaultDirName={pf}\Virtual Steer Companion
DefaultGroupName=Virtual Steer Companion
OutputDir=.
OutputBaseFilename=VirtualSteerSetup
Compression=lzma
SolidCompression=yes
UninstallDisplayIcon={app}\VirtualSteerReceiver.exe
SetupIconFile=App.ico

[Files]
Source: "bin\Release\net8.0-windows\win-x64\publish\VirtualSteerReceiver.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "bin\Release\net8.0-windows\win-x64\publish\D3DCompiler_47_cor3.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "bin\Release\net8.0-windows\win-x64\publish\PenImc_cor3.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "bin\Release\net8.0-windows\win-x64\publish\PresentationNative_cor3.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "bin\Release\net8.0-windows\win-x64\publish\vcruntime140_cor3.dll"; DestDir: "{app}"; Flags: ignoreversion
Source: "bin\Release\net8.0-windows\win-x64\publish\wpfgfx_cor3.dll"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\Virtual Steer Companion"; Filename: "{app}\VirtualSteerReceiver.exe"
Name: "{commondesktop}\Virtual Steer Companion"; Filename: "{app}\VirtualSteerReceiver.exe"

[Run]
Filename: "{app}\VirtualSteerReceiver.exe"; Description: "Launch Virtual Steer Companion"; Flags: nowait postinstall skipifsilent
