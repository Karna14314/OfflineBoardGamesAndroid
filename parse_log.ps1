$lines = Get-Content 'C:\Users\chait\Projects\Offline_games\logcat_dump.txt' -Encoding UTF8
$filtered = $lines | Where-Object { $_ -match 'AndroidRuntime|FATAL|offlinegames|TicTac|Exception|Caused by|at com\.' }
$filtered | Select-Object -Last 200 | Set-Content 'C:\Users\chait\Projects\Offline_games\crash_log.txt' -Encoding UTF8
Write-Host "Done. Lines captured: $($filtered.Count)"
