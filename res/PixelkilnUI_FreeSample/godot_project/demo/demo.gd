extends Control
## Pixelkiln UI Forge - live demo.
##
## Every control below is a stock Godot control. Nothing is custom-drawn and no
## code touches how anything looks: the entire appearance comes from the Theme
## resource assigned to this node. Click a name at the top to swap it.

const THEMES := [
	["Oakheart", "res://pixelkiln_ui/oakheart/theme.tres", "Fantasy RPG"]
]

var _stage: Control
var _tabs: HBoxContainer
var _picker: OptionButton


func _ready() -> void:
	var bg := ColorRect.new()
	bg.color = Color(0.05, 0.05, 0.07)
	bg.set_anchors_preset(Control.PRESET_FULL_RECT)
	add_child(bg)

	var page := VBoxContainer.new()
	page.set_anchors_preset(Control.PRESET_FULL_RECT)
	page.offset_left = 10
	page.offset_top = 8
	page.offset_right = -10
	page.offset_bottom = -8
	page.add_theme_constant_override("separation", 8)
	add_child(page)

	_tabs = HBoxContainer.new()
	_tabs.add_theme_constant_override("separation", 4)
	page.add_child(_tabs)
	for i in THEMES.size():
		var b := Button.new()
		b.text = THEMES[i][0]
		b.pressed.connect(_apply.bind(i))
		_tabs.add_child(b)

	_stage = Control.new()
	_stage.size_flags_vertical = Control.SIZE_EXPAND_FILL
	page.add_child(_stage)

	_apply(0)


func _apply(index: int) -> void:
	for c in _stage.get_children():
		c.queue_free()
	theme = load(THEMES[index][1]) as Theme
	for i in _tabs.get_child_count():
		var b := _tabs.get_child(i) as Button
		b.theme_type_variation = &"PrimaryButton" if i == index else &""
	_stage.add_child(_build_screen(THEMES[index][0], THEMES[index][2]))


func _build_screen(theme_name: String, tagline: String) -> Control:
	var outer := PanelContainer.new()
	outer.set_anchors_preset(Control.PRESET_FULL_RECT)

	var rows := VBoxContainer.new()
	rows.add_theme_constant_override("separation", 7)
	outer.add_child(rows)

	var head := Label.new()
	head.text = theme_name.to_upper() + "   " + tagline
	rows.add_child(head)

	var cols := HBoxContainer.new()
	cols.add_theme_constant_override("separation", 10)
	cols.size_flags_vertical = Control.SIZE_EXPAND_FILL
	rows.add_child(cols)

	var left := VBoxContainer.new()
	left.add_theme_constant_override("separation", 4)
	left.custom_minimum_size.x = 136
	cols.add_child(left)

	for row in [["Health", 0.82, "HealthBar"], ["Magic", 0.46, "ManaBar"],
			["Stamina", 0.61, "XpBar"]]:
		var lab := Label.new()
		lab.text = row[0]
		left.add_child(lab)
		var pb := ProgressBar.new()
		pb.theme_type_variation = StringName(row[2])
		pb.value = float(row[1]) * 100.0
		pb.show_percentage = false
		pb.custom_minimum_size.y = 10
		left.add_child(pb)

	var vol := Label.new()
	vol.text = "Volume"
	left.add_child(vol)
	var slider := HSlider.new()
	slider.value = 70
	slider.tick_count = 5
	slider.ticks_on_borders = true
	left.add_child(slider)

	# v1.6: a disabled slider and a SpinBox, because both were Godot's own art
	# before this release and the demo is where a buyer looks first.
	var muted := HSlider.new()
	muted.value = 40
	muted.editable = false
	left.add_child(muted)
	var gold := SpinBox.new()
	gold.min_value = 0
	gold.max_value = 999
	gold.value = 250
	left.add_child(gold)

	var name_field := LineEdit.new()
	name_field.text = "Ashwyn"
	left.add_child(name_field)

	var mid := VBoxContainer.new()
	mid.add_theme_constant_override("separation", 4)
	mid.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	cols.add_child(mid)

	# A TabContainer, because as of v1.4 the tabs are themed too -- until then
	# this emitter styled the container's panel and left Godot's grey tabs on
	# top of it, so the demo could not honestly show one.
	var book := TabContainer.new()
	book.size_flags_vertical = Control.SIZE_EXPAND_FILL
	mid.add_child(book)

	var inv := ItemList.new()
	inv.name = "Inventory"
	# Long enough to scroll, so the demo shows the themed scrollbar (v1.6).
	for s in ["Iron Sword", "Health Potion", "Brass Key", "Old Map",
			"Silver Ring", "Torch", "Rope", "Lantern Oil", "Bread",
			"Lockpick", "Feather Quill", "Tinderbox", "Healing Herb",
			"Iron Ingot", "Copper Coin", "Wax Seal", "Spare Boots",
			"Fishing Line", "Old Compass", "Chalk"]:
		inv.add_item(s)
	inv.select(0)
	book.add_child(inv)

	var quests := ItemList.new()
	quests.name = "Quests"
	for s in ["The Sunken Mill", "Debt to Marrow", "Feed the Forge"]:
		quests.add_item(s)
	book.add_child(quests)

	var right := VBoxContainer.new()
	right.add_theme_constant_override("separation", 4)
	right.custom_minimum_size.x = 140
	cols.add_child(right)

	var go := Button.new()
	go.text = "Continue"
	go.theme_type_variation = &"PrimaryButton"
	right.add_child(go)

	for label in ["Equipment", "Save Game"]:
		var b := Button.new()
		b.text = label
		right.add_child(b)

	var locked := Button.new()
	locked.text = "New Game+"
	locked.disabled = true
	right.add_child(locked)

	var cb := CheckBox.new()
	cb.text = "Fullscreen"
	cb.button_pressed = true
	right.add_child(cb)

	var cbtn := CheckButton.new()
	cbtn.text = "Music"
	cbtn.button_pressed = true
	right.add_child(cbtn)

	right.add_child(HSeparator.new())

	# The v1.4 additions. An OptionButton is the control a settings screen is
	# actually made of, and before v1.4 neither pack had a sprite for one, so
	# this row rendered in Godot's default grey inside an otherwise themed UI.
	var res_label := Label.new()
	res_label.text = "Resolution"
	right.add_child(res_label)
	var res := OptionButton.new()
	for r in ["1920 x 1080", "1280 x 720", "640 x 360"]:
		res.add_item(r)
	res.select(1)
	right.add_child(res)

	var qual := OptionButton.new()
	qual.name = "QualityPicker"
	for q in ["Quality: High", "Quality: Medium", "Quality: Low"]:
		qual.add_item(q)
	qual.select(0)
	qual.set_item_disabled(2, true)
	right.add_child(qual)
	_picker = qual

	return outer


## Open the quality dropdown. Used by the build-time capture tool so the POPUP
## itself gets rendered by the engine on every theme -- until v1.4 the popup
## existed only in hand-composed store art, so six of the eight themes had
## never had one drawn by Godot at all.
func open_picker() -> void:
	if _picker:
		_picker.show_popup()
