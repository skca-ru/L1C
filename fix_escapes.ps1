$path = 'app\src\main\java\l1c\RunYBase.java'
$content = Get-Content $path -Raw -Encoding UTF8
$old1 = '"C:\Program Files (x86)\1cv8\common\1cestart.exe"'
$new1 = '"C:\\Program Files (x86)\\1cv8\\common\\1cestart.exe"'
$old2 = '"C:\Program Files\1cv8\common\1cestart.exe"'
$new2 = '"C:\\Program Files\\1cv8\\common\\1cestart.exe"'
$content = $content.Replace($old1, $new1)
$content = $content.Replace($old2, $new2)
Set-Content $path $content -Encoding UTF8 -NoNewline
Write-Host 'Fixed'
