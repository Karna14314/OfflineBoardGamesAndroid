$content = Get-Content 'C:\Users\chait\Projects\Offline_games\logcat2.txt' -Raw -Encoding UTF8
# Find the FATAL EXCEPTION block
$pattern = '(?s)(FATAL EXCEPTION.*?(?=\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\s+\d+\s+\d+\s+[VDIWEF] (?!AndroidRuntime|art\s|DEBUG\s)))'
$matches = [regex]::Matches($content, $pattern)
if ($matches.Count -gt 0) {
    $matches | ForEach-Object { $_.Value } | Set-Content 'C:\Users\chait\Projects\Offline_games\fatal_crash.txt' -Encoding UTF8
    Write-Host "Found $($matches.Count) fatal exceptions"
}
else {
    # Fallback: just grep for AndroidRuntime lines
    $lines = Get-Content 'C:\Users\chait\Projects\Offline_games\logcat2.txt' -Encoding UTF8
    $start = -1
    $results = @()
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match 'FATAL EXCEPTION|AndroidRuntime') {
            if ($start -eq -1) { $start = [Math]::Max(0, $i - 2) }
        }
        if ($start -ge 0) {
            $results += $lines[$i]
            if ($results.Count -gt 200) { break }
        }
    }
    $results | Set-Content 'C:\Users\chait\Projects\Offline_games\fatal_crash.txt' -Encoding UTF8
    Write-Host "Fallback: captured $($results.Count) lines from first crash"
}
