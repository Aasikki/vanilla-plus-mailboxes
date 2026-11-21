# Regenerates the per-variant data for Vanilla+ Mailboxes:
# body models per wood, blockstates, item models, loot tables, recipes, language file, and the axe tag.
# Run from anywhere: .\generate-variants.ps1
$ErrorActionPreference = 'Stop'

$modRoot = Split-Path -Parent $PSScriptRoot
$repoRoot = Split-Path -Parent $modRoot
$resources = Join-Path $modRoot 'src\main\resources'
$assets = Join-Path $resources 'assets\vanilla_plus_mailboxes'
$data = Join-Path $resources 'data\vanilla_plus_mailboxes'
$bodySource = Join-Path $repoRoot 'Blockbench\Exports\mailbox_body.json'

$woods = @('oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','pale_oak','bamboo','crimson','warped')
$metals = @('diamond','gold','iron','netherite','copper','exposed_copper','weathered_copper','oxidized_copper','waxed_copper','waxed_exposed_copper','waxed_weathered_copper','waxed_oxidized_copper')
$ingots = @{ diamond = 'minecraft:diamond'; gold = 'minecraft:gold_ingot'; iron = 'minecraft:iron_ingot'; netherite = 'minecraft:netherite_ingot'; copper = 'minecraft:copper_ingot' }

foreach ($dir in @("$assets\models\block", "$assets\blockstates", "$assets\items", "$assets\lang", "$data\loot_table\blocks", "$data\recipe", "$resources\data\minecraft\tags\block\mineable")) {
	New-Item -ItemType Directory -Force $dir | Out-Null
}
Remove-Item "$data\recipe\*" -Force -ErrorAction SilentlyContinue

function Set-Utf8($path, $text) {
	[IO.File]::WriteAllText($path, ([string]$text).TrimEnd() + "`n", [Text.UTF8Encoding]::new($false))
}

function Title($text) {
	($text -split '_' | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1) }) -join ' '
}

$body = Get-Content -LiteralPath $bodySource -Raw
$lang = [ordered]@{}
$axe = @()
$recipes = 0

foreach ($w in $woods) {
	Set-Utf8 "$assets\models\block\mailbox_body_$w.json" ($body -replace 'block/dark_oak_planks', "block/${w}_planks")
}

foreach ($w in $woods) {
	foreach ($m in $metals) {
		$v = "mailbox_${w}_$m"
		$axe += "vanilla_plus_mailboxes:$v"

		$blockstate = @"
{
	"variants": {
		"facing=north": { "model": "vanilla_plus_mailboxes:block/mailbox_body_$w" },
		"facing=east": { "model": "vanilla_plus_mailboxes:block/mailbox_body_$w", "y": 90 },
		"facing=south": { "model": "vanilla_plus_mailboxes:block/mailbox_body_$w", "y": 180 },
		"facing=west": { "model": "vanilla_plus_mailboxes:block/mailbox_body_$w", "y": 270 }
	}
}
"@
		Set-Utf8 "$assets\blockstates\$v.json" $blockstate

		$item = @"
{
	"model": {
		"type": "minecraft:composite",
		"models": [
			{
				"type": "minecraft:model",
				"model": "vanilla_plus_mailboxes:block/mailbox_body_$w"
			},
			{
				"type": "minecraft:special",
				"base": "vanilla_plus_mailboxes:block/mailbox_body_$w",
				"model": {
					"type": "vanilla_plus_mailboxes:mailbox_door"
				}
			}
		]
	}
}
"@
		Set-Utf8 "$assets\items\$v.json" $item

		$loot = @"
{
	"type": "minecraft:block",
	"pools": [
		{
			"rolls": 1,
			"entries": [
				{
					"type": "minecraft:item",
					"name": "vanilla_plus_mailboxes:$v"
				}
			],
			"conditions": [
				{
					"condition": "minecraft:survives_explosion"
				}
			]
		}
	]
}
"@
		Set-Utf8 "$data\loot_table\blocks\$v.json" $loot

		$lang["block.vanilla_plus_mailboxes.$v"] = "$(Title $w) Mailbox ($(Title $m) Door)"

		if ($ingots.ContainsKey($m)) {
			$shaped = @"
{
	"type": "minecraft:crafting_shaped",
	"group": "vanilla_plus_mailboxes",
	"pattern": [
		" O ",
		"O=O",
		"OOO"
	],
	"key": {
		"=": "$($ingots[$m])",
		"O": "minecraft:${w}_planks"
	},
	"result": {
		"id": "vanilla_plus_mailboxes:$v",
		"count": 1
	}
}
"@
			Set-Utf8 "$data\recipe\$v.json" $shaped
			$recipes++
		}
	}

	foreach ($m in @('copper','exposed_copper','weathered_copper','oxidized_copper')) {
		$source = "mailbox_${w}_$m"
		$result = "mailbox_${w}_waxed_$m"
		$wax = @"
{
	"type": "minecraft:crafting_shapeless",
	"group": "vanilla_plus_mailboxes_waxed",
	"ingredients": [
		"vanilla_plus_mailboxes:$source",
		"minecraft:honeycomb"
	],
	"result": {
		"id": "vanilla_plus_mailboxes:$result",
		"count": 1
	}
}
"@
		Set-Utf8 "$data\recipe\wax_${w}_$m.json" $wax
		$recipes++
	}
}

$lang['container.vanilla_plus_mailboxes.mailbox'] = 'Mailbox'
Set-Utf8 "$assets\lang\en_us.json" ($lang | ConvertTo-Json)
Set-Utf8 "$resources\data\minecraft\tags\block\mineable\axe.json" ([ordered]@{ values = $axe } | ConvertTo-Json)

"variants: " + $axe.Count
"recipes: " + $recipes
