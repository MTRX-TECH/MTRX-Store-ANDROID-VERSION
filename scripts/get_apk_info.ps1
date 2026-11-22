Get-ChildItem -Path "c:\MARAPATHRAN GROUPS\02 - MTRX Tech\Softwares\android" -Recurse -Filter "*.apk" | ForEach-Object {
    $hash = (Get-FileHash -Path $_.FullName -Algorithm SHA256).Hash.ToLower()
    [PSCustomObject]@{
        Name = $_.Name
        Bytes = $_.Length
        Path = $_.FullName
        SHA256 = $hash
    }
} | Format-List
