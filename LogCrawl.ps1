$pathin = Read-Host -Prompt "Path to logs"
$regex = Read-Host -Prompt "Regex (case-insensitive)"

$path = [System.Environment]::ExpandEnvironmentVariables($pathin)
echo ("Searching for log files in " + $path)

$files = [System.IO.Directory]::GetFiles($path)
$lastfile = $null
foreach ($file in $files) {
    if (-not $file.EndsWith(".gz")) {
        continue;
    }
    $stream = [System.IO.File]::OpenRead($file)
    $gzstream = [System.IO.Compression.GZipStream]::new($stream, [System.IO.Compression.CompressionMode]::Decompress)
    $streamreader = [System.IO.StreamReader]::new($gzstream)
    while ($line = $streamreader.ReadLine()) {
        if ($line -imatch $regex) {
            if ($file -ne $lastfile) {
                $lastfile = $file
                echo ("Match(es) in " + $file + ":")
            }
            echo $line
        }
    }
    $streamreader.Close()
    $gzstream.Close()
    $stream.Close()
}