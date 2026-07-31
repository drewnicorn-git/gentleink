-- GentleInk KOReader plugin — batch EPUB clean with backup/revert

local DataStorage = require("datastorage")
local UIManager = require("ui/uimanager")
local InfoMessage = require("ui/widget/infomessage")
local ConfirmBox = require("ui/widget/confirmbox")
local PathChooser = require("ui/widget/pathchooser")
local filemanagerutil = require("apps/filemanager/filemanagerutil")
local lfs = require("libs/libk2pdfopt/lfs")

local GentleInk = {
    name = "GentleInk",
    fullname = _("GentleInk profanity filter"),
    description = _("Filter profanity from EPUB books with backup and revert"),
}

local TIER1 = {
    "fuck", "fucking", "fucked", "fucker", "shit", "shitty", "bullshit",
    "goddamn", "bastard", "asshole", "whore", "slut", "cunt", "twat",
}

local COMPOUNDS = {
    "assassin", "bass", "classic", "cockpit", "peacock", "assistance",
}

local SUBS = {
    hell = "heck", damn = "darn", shit = "poop", fuck = "fudge",
    fucking = "freaking", ass = "butt", asshole = "jerk", bitch = "jerk",
    bastard = "jerk", crap = "crud", goddamn = "gosh darn",
}

local function cacheDir()
    return DataStorage:getDataDir() .. "/gentleink_cache"
end

local function backupPath(bookPath)
    local name = bookPath:match("([^/]+)$") or "book.epub"
    return cacheDir() .. "/originals/" .. name .. ".original"
end

local function ensureCache()
    local base = cacheDir()
    lfs.mkdir(base)
    lfs.mkdir(base .. "/originals")
end

local function preserveCase(original, replacement)
    if original == original:upper() then
        return replacement:upper()
    end
    if original:match("^%u") then
        return replacement:sub(1,1):upper() .. replacement:sub(2)
    end
    return replacement
end

local function substituteWord(word)
    local lower = word:lower()
    local sub = SUBS[lower]
    if sub then
        return preserveCase(word, sub)
    end
    return string.rep("*", math.max(3, #word))
end

function GentleInk:filterText(text)
    if not text or text == "" then return text end
    local result = text

    for _, word in ipairs(TIER1) do
        local pattern = "%f[%w]" .. word .. "%f[%W]"
        result = result:gsub(pattern, function(m)
            return substituteWord(m)
        end)
    end

    result = result:gsub("[Ww]hat the hell", "What the heck")
    result = result:gsub("[Hh]ow the hell", "How the heck")
    result = result:gsub("[Gg]o to hell", "Go to heck")
    result = result:gsub("(%f[%w]your%f[%W]) ass", "%1 butt")
    result = result:gsub("(%f[%w]my%f[%W]) ass", "%1 butt")
    result = result:gsub("(%f[%w]dumb%f[%W]) ass", "%1 butt")
    result = result:gsub("(%f[%w]stupid%f[%W]) ass", "%1 butt")
    result = result:gsub("pain in the ass", "pain in the butt")

    return result
end

function GentleInk:filterHtml(html)
    return html:gsub(">([^<]+)<", function(content)
        if content:match("%a") then
            local filtered = self:filterText(content)
            if filtered ~= content then
                return ">" .. filtered .. "<"
            end
        end
        return ">" .. content .. "<"
    end)
end

function GentleInk:copyFile(src, dst)
    local infile = io.open(src, "rb")
    if not infile then return false end
    local data = infile:read("*a")
    infile:close()
    local outfile = io.open(dst, "wb")
    if not outfile then return false end
    outfile:write(data)
    outfile:close()
    return true
end

function GentleInk:cleanBookFile(bookPath)
    ensureCache()
    local backup = backupPath(bookPath)
    if not lfs.attributes(backup) then
        if not self:copyFile(bookPath, backup) then
            return false, "Could not create backup"
        end
    end

    local tmpPath = cacheDir() .. "/working.epub"
    if not self:copyFile(bookPath, tmpPath) then
        return false, "Could not copy working file"
    end

    local ok, err = pcall(function()
        local Reader = require("docreader")
        local zin = require("ffi/zlib")
        -- Fallback: use unzip command if available via os.execute
        local cmd = string.format('cd "%s" && unzip -o "%s" -d extracted 2>/dev/null', cacheDir(), tmpPath)
        os.execute(cmd)
        local extractDir = cacheDir() .. "/extracted"
        if lfs.attributes(extractDir) then
            for file in lfs.dir(extractDir) do
                if file:match("%.xhtml$") or file:match("%.html$") then
                    local full = extractDir .. "/" .. file
                    local f = io.open(full, "r")
                    if f then
                        local html = f:read("*a")
                        f:close()
                        local filtered = self:filterHtml(html)
                        if filtered ~= html then
                            f = io.open(full, "w")
                            f:write(filtered)
                            f:close()
                        end
                    end
                end
            end
            local zipCmd = string.format('cd "%s/extracted" && zip -r "%s" . 2>/dev/null', cacheDir(), bookPath)
            os.execute(zipCmd)
        end
    end)

    if not ok then
        return false, tostring(err)
    end
    return true, "Book cleaned. Original backed up."
end

function GentleInk:revertBookFile(bookPath)
    local backup = backupPath(bookPath)
    if not lfs.attributes(backup) then
        return false, "No backup found"
    end
    if self:copyFile(backup, bookPath) then
        return true, "Reverted to original"
    end
    return false, "Revert failed"
end

function GentleInk:init()
    self:onDispatcherRegister()
end

function GentleInk:onDispatcherRegister()
    self.ui.menu:registerToMainMenu(GentleInk)
end

function GentleInk:addToMainMenu(menu_items)
    menu_items.gentleink = {
        text = _("GentleInk filter"),
        sub_item_table = {
            {
                text = _("Preview filter on selection"),
                callback = function()
                    local text = self.ui.highlight:getSelectedText()
                    if not text or text == "" then
                        UIManager:show(InfoMessage:new{ text = _("Highlight text to preview filtering.") })
                        return
                    end
                    UIManager:show(InfoMessage:new{
                        text = _("Filtered:") .. "\n" .. self:filterText(text),
                        timeout = 8,
                    })
                end,
            },
            {
                text = _("Clean current book (backup first)"),
                callback = function()
                    ConfirmBox:new{
                        text = _("Clean profanity from this EPUB? A backup is saved in gentleink_cache/originals/."),
                        ok_callback = function()
                            local doc = self.ui.document
                            if not doc or not doc.file then
                                UIManager:show(InfoMessage:new{ text = _("No book file path available.") })
                                return
                            end
                            local ok, msg = self:cleanBookFile(doc.file)
                            UIManager:show(InfoMessage:new{ text = ok and msg or ("Error: " .. msg), timeout = 6 })
                            if ok then
                                self.ui:reloadDocument()
                            end
                        end,
                    }:show()
                end,
            },
            {
                text = _("Revert to original"),
                callback = function()
                    local doc = self.ui.document
                    if not doc or not doc.file then return end
                    local ok, msg = self:revertBookFile(doc.file)
                    UIManager:show(InfoMessage:new{ text = ok and msg or ("Error: " .. msg), timeout = 6 })
                    if ok then self.ui:reloadDocument() end
                end,
            },
        },
    }
end

return GentleInk
