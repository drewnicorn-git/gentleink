# Build a longer, story-style GentleInk test EPUB with highlighted profanity.
$ErrorActionPreference = "Stop"
$OutDir = $PSScriptRoot
$OutFile = Join-Path $OutDir "gentleink-filter-test-story.epub"
$BookId = "urn:uuid:gentleink-filter-test-story-002"

$Css = @"
body { font-family: Georgia, "Times New Roman", serif; line-height: 1.65; margin: 1.5em; max-width: 36em; }
h1 { font-size: 1.55em; border-bottom: 1px solid #bbb; padding-bottom: 0.35em; margin-top: 0; }
h2 { font-size: 1.05em; font-weight: normal; font-style: italic; color: #555; margin: 0 0 1.2em 0; }
p { margin: 0 0 0.95em 0; text-indent: 1.5em; }
p.no-indent, blockquote { text-indent: 0; }
blockquote { margin: 1em 1.5em; font-style: italic; color: #333; border-left: 3px solid #ccc; padding-left: 1em; }
.note { font-style: italic; color: #555; background: #f7f7f7; padding: 0.75em 1em; border-radius: 4px; margin-bottom: 1.5em; }
.legend { background: #fffbe6; border: 1px solid #f0d000; padding: 0.75em 1em; border-radius: 4px; margin-bottom: 1.5em; }
mark.profanity { background: #ffe066; color: #7a0000; padding: 0 3px; border-radius: 2px; font-weight: 600; }
.chapter-break { text-align: center; margin: 2em 0; letter-spacing: 0.35em; color: #999; }
"@

# mark tags wrap words that SHOULD be filtered. Safe words appear unmarked in natural prose.
$Chapters = @(
    @{
        File = "title.xhtml"
        Title = "The Brass Lantern"
        Subtitle = "A GentleInk Filter Test Novella"
        Body = @"
<p class="legend no-indent"><strong>How to use this book:</strong> Every word highlighted in
<span style="background:#ffe066;padding:0 3px;border-radius:2px;font-weight:600;color:#7a0000">yellow</span>
is profanity your GentleInk plugin should clean. After running <em>GentleInk Clean</em> in Calibre,
open the book again — highlighted words should change (substitute/mask/remove) while normal text,
including innocent look-alikes like <em>bass</em>, <em>assassin</em>, and <em>ass</em> meaning donkey,
should stay exactly as written.</p>
<p class="note no-indent">This is not a real novel. It is a deliberately constructed test fixture with
natural dialogue and narration so you can judge whether context-aware filtering works in a book-like
reading experience.</p>
"@
    },
    @{
        File = "ch01.xhtml"
        Title = "Chapter One"
        Subtitle = "The Flat Tire"
        Body = @"
<p>The van lurched onto the gravel shoulder somewhere between Leeds and Scunthorpe, and Danny swore
under his breath — not loudly enough for the highlight test, just a muttered breath against the
steering wheel. Rain stitched the windshield while the wipers fought a losing battle.</p>
<p>"Don't panic," said Mara from the passenger seat. She was reading a classic thriller about an
assassin who never missed, which seemed like poor timing. "He'll find a garage. He always does."</p>
<p>Behind them, wedged between flight cases, Theo tuned his bass in the dark. The bassist had played
every dive from Nashville to Newcastle, and he insisted the second bass in the back was strictly for
spares. "If we miss soundcheck," he said, "the promoter will need serious assistance finding a
replacement band."</p>
<p>Danny climbed out into the wet. A farmhouse sat beyond a hedge; a peacock strutted near the barn
as though it owned the county. An old biplane rotted in the field, cockpit open to the weather.
From the stable came a long, complaining bray.</p>
<p>"You've got to be kidding," Danny said. "Is that a donkey?"</p>
<p>The farmer appeared in a waxed coat. "That's my ass," he said plainly. "And the mule beside him.
They carry feed when the truck won't start. You need help?"</p>
<p>"Flat tire," Danny said. "We're the Brass Lantern — wedding band, mostly. Classic rock, some
Motown. We're supposed to be in Scunthorpe by seven."</p>
<p>The farmer nodded toward the barn. "Grass is slick. Brass fittings on the compressor are frozen.
He'll thaw them if you give him ten minutes." He jerked his thumb at a lanky boy who couldn't have
been more than nineteen. "Don't worry. We pass this road every harvest. Massive trucks, narrow lanes.
You're not the first."</p>
"@
    },
    @{
        File = "ch02.xhtml"
        Title = "Chapter Two"
        Subtitle = "The Garage Argument"
        Body = @"
<p>The garage smelled of oil and wet dog. A hunting bitch slept under the workbench, her litter
tucked behind a crate of spark plugs. On the wall, a calendar showed a red cock crowing at dawn —
the painter had labeled it plainly, without embarrassment, the way farmers label things.</p>
<p>Danny's phone buzzed. The promoter again.</p>
<p>"What the <mark class="profanity">hell</mark> do you want?" Danny answered, forgetting his
indoor voice. Mara winced. The farmer's boy looked up from the tire iron.</p>
<p>"We're waiting," the promoter said. "Stage is set. Guests are massed in the hall like an embassy
reception without champagne."</p>
<p>"We're fixing a flat," Danny said. "Damn the torpedoes — we'll get there when we get there."
He meant it as a joke. The farmer smiled at the reference and went back to his compressor.</p>
<p>The boy, less patient, muttered, "This is <mark class="profanity">bullshit</mark> weather for
a wedding." Danny couldn't argue. The vacuum pump began to suck air from the seal with a steady
whine while Theo leaned against the doorframe.</p>
<p>"You suck at scheduling," Theo told Danny, and for a second the sentence hung wrong in the room —
not because the filter would catch it, but because Theo had meant it gently, the way musicians rib
each other. Danny heard the insult anyway.</p>
<p>"Move your <mark class="profanity">ass</mark> and help," he snapped. "I'm not your roadie."</p>
<p>Theo's face closed. "You're being a <mark class="profanity">pain in the ass</mark>, you know
that? Kick my <mark class="profanity">ass</mark> later if you want, but help me lift the case."</p>
<p>Mara stepped between them. "Both of you, stop. The gates of hell shall not prevail against a
flat tire, but I'd like to prevail against being fired. Theo, he didn't mean it. Danny, apologize."</p>
<p>Danny rubbed his face. "Sorry. I'm hell-bent on making this gig because we need the money. This
<mark class="profanity">goddamn</mark> van is held together with hope."</p>
<p>"Language," the farmer said mildly, without looking up. "There's a church hall in town. They read
from the old book on Sundays. Ass means donkey here. Always has."</p>
<p>Danny nodded, chastened. The boy grinned and finished the tire.</p>
"@
    },
    @{
        File = "ch03.xhtml"
        Title = "Chapter Three"
        Subtitle = "The Inn on Mill Road"
        Body = @"
<p>They made Scunthorpe with twenty minutes to spare, played the wedding, and drank weak coffee in
the kitchen afterward while the bride's uncle told stories that wandered like country roads. By
midnight they were booked into the Mill Road Inn, too tired to argue anymore.</p>
<p>Mara sat on the bed with her assassin novel finished at last. "The author thinks violence is
poetry," she said. "The assassin dies in a church. Symbolic."</p>
<p>Theo sprawled in the chair, picking at his bass strings without plugging in. "Promoter said we
were <mark class="profanity">shit</mark> hot tonight," he offered, by way of peace. "His words,
not mine."</p>
<p>"He also said the bride's brother was a <mark class="profanity">bastard</mark> about the volume,"
Danny said. "And I quote: 'Tell your drummer not to be a <mark class="profanity">jackass</mark>
with the fills.'"</p>
<p>Mara laughed despite herself. "You're writing test copy in your head again."</p>
<p>"Occupational hazard." Danny peeled off his wet socks. "The brother came up to me and said,
'What the <mark class="profanity">fuck</mark> was that last song?' Like we'd played death metal.
It was 'Signed, Sealed, Delivered.'"</p>
<p>"Some people," Theo said. "You can't fix them. You can only duck."</p>
<p>Downstairs, the innkeeper's vacuum began to suck air through the corridor carpet — a late cleanup
after the wedding party. A cockatiel shrieked in the lobby; someone had left the cage cover off.
Through the open window, a real cock crowed from a neighbor's yard, twice, then fell silent.</p>
<p>Mara marked her page. "I'm going to sleep. Tomorrow we drive south. No more gravel shoulders, no
more donkeys, no more peacocks."</p>
<p>"Famous last words," Danny said.</p>
"@
    },
    @{
        File = "ch04.xhtml"
        Title = "Chapter Four"
        Subtitle = "Checkout"
        Body = @"
<p>Breakfast was eggs, toast, and a silence that felt earned. The innkeeper slid the bill across
the counter and said, "Don't worry about the noise last night. Weddings bring out the strange in
people. He'll apologize when he sobers up."</p>
<p>"Who will?" Mara asked.</p>
<p>"The brother. The one who called your singer a — well. He used language the church ladies wouldn't
approve of." She didn't repeat it. Danny appreciated that.</p>
<p>Outside, the morning was brass-bright. Theo loaded the bass cases while Danny checked the tires.
The farmer from the day before had texted somehow — Mara had given him her number for directions —
with a photo of the ass and mule carrying sacks toward the barn.</p>
<p>"Look," Mara said, showing Danny the screen. "Beast of burden content. Safe context. If your
plugin blanks that out, something's wrong."</p>
<p>Danny smiled. "That's the whole point of this book."</p>
<p>They merged onto the motorway. Scunthorpe receded in the mirror. Theo put on a record — something
classical, which was a joke from Mara because Theo hated classical music. The band passed a sign for
a cockfighting museum they didn't visit, a peacock farm they did not need, and a truck stop where
a poster advertised assistance for stranded motorists.</p>
<p>At the services, a kid in a gaming shirt told Danny, "You suck at parking," and ran off before
Danny could decide if he was offended. Theo said, "Context. Filter might catch it, might not. That's
why we test."</p>
<p>Danny bought coffee and a paperback for Mara — another assassin story, because the bookstore
only had bestsellers and crime. "For research," he said.</p>
<p>"For filtering," she corrected.</p>
"@
    },
    @{
        File = "ch05.xhtml"
        Title = "Chapter Five"
        Subtitle = "The Last Rehearsal"
        Body = @"
<p>They rented a rehearsal room in Sheffield with walls thin enough to hear the brass band next
door practicing Sousa. Danny ran them through the set list twice, then a third time for the songs
with tricky endings.</p>
<p>"One more," Theo said, "and then I'm <mark class="profanity">goddamn</mark> well done for the
night." He caught himself. "Sorry. Tired."</p>
<p>Mara leaned on her keyboard. "Run 'Respect' again. The bride's aunt requested it specifically.
She said if we mess it up she'll raise hell — and she meant volume, not theology, so leave it
alone, filter."</p>
<p>Danny counted them in. They played clean. When they finished, the brass band next door applauded
through the wall, which was either supportive or sarcastic; nobody could tell.</p>
<p>During the break, Danny read the promoter email aloud. "'Great gig, but the guitar solo was loud
as <mark class="profanity">hell</mark>.' See? Profane pattern. Should change." He scrolled.
"'Next time don't bring that <mark class="profanity">dipshit</mark> monitor — we have our own.'
Tier one. Should definitely change."</p>
<p>Theo raised his hand. "Objection. I'm the <mark class="profanity">dipshit</mark> who saved the show when the house mic died."</p>
<p>"You're the smartass who forgot the spare cable," Danny said, then paused. "Smartass isn't
highlighted — edge case. See if your filter touches it."</p>
<p>Mara closed her laptop. "Let's table it. If GentleInk turns 'smartass' into something absurd,
file a bug. If it leaves 'bass' alone in every chapter, celebrate."</p>
<p>They packed up as rain started again. In the parking lot a driver honked and yelled, "<mark
class="profanity">Damn you</mark>, move the van!" Danny moved the van. He didn't yell back. He was
learning.</p>
<p>That night in the motel, Mara wrote in her notebook: <em>Highlight test complete if yellow words
changed and donkey ass did not.</em> Danny added: <em>Also check Scunthorpe, assassin, cockpit,
he'll, don't, harass, surpass, massive, grass, brass, passage, passengers, embassy.</em></p>
<p>Theo wrote: <em>Oh <mark class="profanity">shit</mark>, we forgot to highlight this line in
chapter three.</em> Mara said that was the point — real books aren't labeled. This one is, so you
can trust your eyes.</p>
"@
    },
    @{
        File = "epilogue.xhtml"
        Title = "Epilogue"
        Subtitle = "What You Should See After Cleaning"
        Body = @"
<p class="note no-indent">Use this page as a checklist after GentleInk Clean runs on this file.</p>
<p class="no-indent"><strong>Should change</strong> (were highlighted yellow): profane uses of
<mark class="profanity">hell</mark>, <mark class="profanity">damn</mark>,
<mark class="profanity">goddamn</mark>, <mark class="profanity">ass</mark>,
<mark class="profanity">bullshit</mark>, <mark class="profanity">shit</mark>,
<mark class="profanity">fuck</mark>, <mark class="profanity">bastard</mark>,
<mark class="profanity">dipshit</mark>, and similar tier-one or profane-context hits throughout
the story.</p>
<p class="no-indent"><strong>Should stay unchanged</strong> (never highlighted): bass, bassist,
assassin, classic, classical, Scunthorpe, assistance, peacock, cockpit, he'll, don't, grass,
brass, massive, embassy, passage, harass, surpass, cockatiel, cock (rooster), bitch (dog),
suck air / vacuum suck, damn the torpedoes, hell-bent, gates of hell, ass (donkey), mule.</p>
<p class="no-indent"><strong>Edge cases to inspect manually:</strong> "You suck at scheduling" and
"You suck at parking" may filter depending on context rules. "Smartass" and "jackass" behavior
depends on tier-one lists. After substitute mode, highlighted regions should still be visible but
with milder words inside the yellow marks.</p>
<p>The Brass Lantern drove on. The filter, if it worked, let them keep the music and lose the mouth.</p>
"@
    }
)

function New-ChapterXhtml([string]$Title, [string]$Subtitle, [string]$Body) {
    $sub = if ($Subtitle) { "<h2>$Subtitle</h2>" } else { "" }
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
  $sub
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
        Content = (New-ChapterXhtml $ch.Title $ch.Subtitle $ch.Body)
    })
}

$manifestItems.Add('    <item id="css" href="style.css" media-type="text/css"/>')
$manifestItems.Add('    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>')

$Opf = @"
<?xml version="1.0" encoding="utf-8"?>
<package xmlns="http://www.idpf.org/2007/opf" version="2.0" unique-identifier="uid">
  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
    <dc:title>The Brass Lantern (GentleInk Test)</dc:title>
    <dc:creator opf:role="aut">GentleInk Test Suite</dc:creator>
    <dc:language>en</dc:language>
    <dc:identifier id="uid">$BookId</dc:identifier>
    <dc:description>Long-form GentleInk test novella with highlighted profanity for Calibre plugin verification.</dc:description>
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
    <meta name="dtb:uid" content="$BookId"/>
    <meta name="dtb:depth" content="1"/>
    <meta name="dtb:totalPageCount" content="0"/>
    <meta name="dtb:maxPageNumber" content="0"/>
  </head>
  <docTitle><text>The Brass Lantern (GentleInk Test)</text></docTitle>
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
