# Build GentleInk filter test EPUB for Calibre plugin testing.
$ErrorActionPreference = "Stop"
$OutDir = $PSScriptRoot
$OutFile = Join-Path $OutDir "gentleink-filter-test.epub"

$Css = @"
body { font-family: Georgia, serif; line-height: 1.6; margin: 1.5em; }
h1 { font-size: 1.6em; border-bottom: 1px solid #ccc; padding-bottom: 0.3em; }
h2 { font-size: 1.2em; color: #444; margin-top: 1.5em; }
p { margin: 0.8em 0; text-indent: 1.5em; }
p.no-indent { text-indent: 0; }
.note { font-style: italic; color: #555; text-indent: 0; }
.expected-safe { background: #f0fff0; padding: 0.5em; text-indent: 0; }
.expected-filter { background: #fff5f5; padding: 0.5em; text-indent: 0; }
"@

$Chapters = @(
    @{
        File = "title.xhtml"
        Title = "About This Test Book"
        Body = @"
<p class="note no-indent">This EPUB exercises the GentleInk profanity filter. After cleaning,
compare each passage against the labels below. Safe passages should remain unchanged; profane
passages should be substituted, masked, or removed depending on your plugin settings.</p>
<p>Each chapter groups related test cases drawn from the GentleInk golden test suite.</p>
"@
    },
    @{
        File = "ch01-compounds.xhtml"
        Title = "Chapter 1: Compound Words (Should Stay Intact)"
        Body = @"
<p class="expected-safe no-indent"><strong>Expected:</strong> no filtering — these words contain
substrings that look like profanity but are innocent on their own.</p>
<p>He played the bass guitar all night while the bassist tuned a second bass.</p>
<p>The assassin slipped through the shadows; assassination was not his first choice, but the
assassins guild had trained him well.</p>
<p>It was a classic novel from the nineteenth century, a classical study of human nature.</p>
<p>They drove north toward Scunthorpe without stopping.</p>
<p>Thank you for your assistance today, said the assistant to the other assistants.</p>
<p>A peacock strutted across the lawn near the cockpit of the old biplane.</p>
<p>He'll arrive before noon, and don't worry about it — she'll be there too.</p>
<p>The brass section rehearsed a passage while passengers massed in the embassy foyer.</p>
<p>Grass and massive surpass expectations; harass no one on your way to the cockatoo aviary.</p>
"@
    },
    @{
        File = "ch02-safe-context.xhtml"
        Title = "Chapter 2: Ambiguous Words in Safe Context (Should Stay Intact)"
        Body = @"
<p class="expected-safe no-indent"><strong>Expected:</strong> no filtering — correct literary,
theological, or technical usage.</p>
<p>Jesus rode into Jerusalem on an ass, and the ass brayed loudly in the stable.</p>
<p>They loaded supplies onto the ass and the mule before the long journey.</p>
<p>The gates of hell shall not prevail against it, though hell is a word theologians debate.</p>
<p>She was hell-bent on finishing the race and raised hell only in the sense of making noise.</p>
<p>Damn the torpedoes, full speed ahead! cried the captain — not worth a damn, muttered the mate.</p>
<p>The hunting bitch whelped six healthy pups; the bitch is in heat, the breeder noted.</p>
<p>The old cock crowed at dawn while a cockatiel chirped in the cockpit.</p>
<p>The vacuum began to suck air through the hose as the pump started to suck water from the bilge.</p>
<p>They shot craps at the crap game in the back room — a rare safe use of that word.</p>
"@
    },
    @{
        File = "ch03-profane.xhtml"
        Title = "Chapter 3: Profane Usage (Should Be Filtered)"
        Body = @"
<p class="expected-filter no-indent"><strong>Expected:</strong> filtering — substitute, mask, or
remove depending on your GentleInk settings.</p>
<p>What the hell are you doing? Go to hell and stay there.</p>
<p>How the hell did this happen? To hell with that idea — hell yeah, he was angry.</p>
<p>Damn you, leave me alone. This goddamn door won't open, and god damn it anyway.</p>
<p>I'm going to kick your ass. You dumb ass, watch where you're going.</p>
<p>This job is a pain in the ass. Move your ass before I whoop your ass.</p>
<p>You selfish bitch. He's a son of a bitch, and stop being a bitch about it.</p>
<p>You suck at this game. That sucks — it really sucks to be you today.</p>
<p>What the fuck is going on? This is complete bullshit and utter horseshit.</p>
<p>Oh shit, we're late. The dipshit left his shitty attitude at the door.</p>
<p>Don't be a jackass, dumbass, badass, or asshole — though those last ones are tier-one hits.</p>
<p>He was pissed off, acting like a bastard and a wanker with a mouth full of bollocks.</p>
"@
    },
    @{
        File = "ch04-mixed.xhtml"
        Title = "Chapter 4: Mixed Narrative (Filter Only What Deserves It)"
        Body = @"
<p class="expected-safe no-indent"><strong>Expected:</strong> a realistic passage where innocent
words survive and profanity is cleaned.</p>
<p>The musician carried his bass case past the embassy gate. Inside, a classic argument unfolded.
"What the hell!" shouted one man. The other replied, "Damn you — move your ass!" A peacock
outside shrieked at the noise.</p>
<p>Meanwhile, on the road to Scunthorpe, a farmer's ass brayed beside a mule. The farmer muttered,
"This is bullshit weather," and his companion said, "You suck at predicting rain." The farmer
was not amused.</p>
<p>The assassin crept through the garden. He'll finish the job, he thought — but not before the
cock crowed and the vacuum began to suck air in the laundry room. "What the fuck," he whispered,
then caught himself. Some words, he knew, the filter would never let through.</p>
"@
    }
)

function New-ChapterXhtml([string]$Title, [string]$Body) {
    return @"
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
  <title>$Title</title>
  <link rel="stylesheet" type="text/css" href="style.css"/>
</head>
<body>
  <h1>$Title</h1>
  $($Body.Trim())
</body>
</html>
"@
}

$manifestItems = New-Object System.Collections.Generic.List[string]
$spineItems = New-Object System.Collections.Generic.List[string]
$navPoints = New-Object System.Collections.Generic.List[string]
$chapterFiles = New-Object System.Collections.Generic.List[object]

for ($i = 0; $i -lt $Chapters.Count; $i++) {
    $ch = $Chapters[$i]
    $itemId = "ch$i"
    $manifestItems.Add("    <item id=`"$itemId`" href=`"$($ch.File)`" media-type=`"application/xhtml+xml`"/>")
    $spineItems.Add("    <itemref idref=`"$itemId`"/>")
    $navPoints.Add(@"
    <navPoint id="nav$i" playOrder="$($i + 1)">
      <navLabel><text>$($ch.Title)</text></navLabel>
      <content src="$($ch.File)"/>
    </navPoint>
"@)
    $chapterFiles.Add([PSCustomObject]@{
        Path = "OEBPS/$($ch.File)"
        Content = (New-ChapterXhtml $ch.Title $ch.Body)
    })
}

$manifestItems.Add('    <item id="css" href="style.css" media-type="text/css"/>')
$manifestItems.Add('    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>')

$Opf = @"
<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:title>GentleInk Filter Test Book</dc:title>
    <dc:creator opf:role="aut">GentleInk Test Suite</dc:creator>
    <dc:language>en</dc:language>
    <dc:identifier id="uid">urn:uuid:gentleink-filter-test-001</dc:identifier>
    <dc:description>Test EPUB for GentleInk Calibre plugin — safe compounds, safe context, and profane usage.</dc:description>
  </metadata>
  <manifest>
$($manifestItems -join "`n")
  </manifest>
  <spine toc="ncx">
$($spineItems -join "`n")
  </spine>
</package>
"@

$Ncx = @"
<?xml version="1.0" encoding="utf-8"?>
<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
  <head>
    <meta name="dtb:uid" content="urn:uuid:gentleink-filter-test-001"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle><text>GentleInk Filter Test Book</text></docTitle>
  <navMap>
$($navPoints -join "`n")
  </navMap>
</ncx>
"@

$Container = @"
<?xml version="1.0" encoding="utf-8"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>
"@

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

if (Test-Path $OutFile) { Remove-Item $OutFile -Force }

$zip = [System.IO.Compression.ZipFile]::Open($OutFile, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    function Add-ZipEntry([string]$Name, [string]$Content, [System.IO.Compression.CompressionLevel]$Level) {
        $entry = $zip.CreateEntry($Name, $Level)
        $stream = $entry.Open()
        try {
            $writer = New-Object System.IO.StreamWriter($stream, [System.Text.UTF8Encoding]::new($false))
            $writer.Write($Content)
            $writer.Flush()
            $writer.Dispose()
        } finally {
            $stream.Dispose()
        }
    }

    Add-ZipEntry "mimetype" "application/epub+zip" ([System.IO.Compression.CompressionLevel]::NoCompression)
    Add-ZipEntry "META-INF/container.xml" $Container ([System.IO.Compression.CompressionLevel]::Optimal)
    Add-ZipEntry "OEBPS/content.opf" $Opf ([System.IO.Compression.CompressionLevel]::Optimal)
    Add-ZipEntry "OEBPS/toc.ncx" $Ncx ([System.IO.Compression.CompressionLevel]::Optimal)
    Add-ZipEntry "OEBPS/style.css" $Css ([System.IO.Compression.CompressionLevel]::Optimal)
    foreach ($ch in $chapterFiles) {
        Add-ZipEntry $ch.Path $ch.Content ([System.IO.Compression.CompressionLevel]::Optimal)
    }
} finally {
    $zip.Dispose()
}

Write-Host "Wrote $OutFile"
