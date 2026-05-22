/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndJournal;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.Camera;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.RectF;
import com.watabou.utils.SparseArray;
import com.watabou.utils.Signal;

public class JournalScene extends PixelScene {

	public static final int WIDTH_P = 126;
	public static final int WIDTH_L = 216;

	private static int lastIDX = 0;

	private static final int TAB_BADGES  = 0;
	private static final int TAB_CATALOG = 1;
	private static final int TAB_GUIDE   = 2;
	private static final int TAB_ALCHEMY = 3;
	private static final int TAB_COUNT   = 4;

	private StyledButton btnBadges;
	private StyledButton btnCatalog;
	private StyledButton btnGuide;
	private StyledButton btnAlchemy;

	private WndJournal.BadgesTab badgesTab;
	private WndJournal.CatalogTab catalogTab;
	private WndJournal.GuideTab guideTab;
	private WndJournal.AlchemyTab alchemyTab;

	private Signal.Listener<KeyEvent> keyboardNavListener;

	@Override
	public void create() {

		super.create();

		Dungeon.hero = null;
		Badges.loadGlobal();
		Journal.loadGlobal();

		Potion.clearColors();
		Scroll.clearLabels();
		Ring.clearGems();

		//need to re-initialize the texture here, as it may be invalid
		new TerrainFeaturesTilemap(new SparseArray<>(), new SparseArray<>());

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		//BG added later

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		float top = 20;

		IconTitle title = new IconTitle(Icons.JOURNAL.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (top - title.height()) / 2f
		);
		align(title);
		add(title);

		NinePatch panel = Chrome.get(Chrome.Type.TOAST);

		int pw = (landscape() ? WIDTH_L : WIDTH_P) + panel.marginHor();
		int ph = h - 50 + panel.marginVer();

		panel.size(pw, ph);
		panel.x = insets.left + (w - pw) / 2f;
		panel.y = insets.top + top;
		add(panel);

		createActiveContent(panel);
		createTabButtons(panel, pw, ph);

		registerKeyboardNavigation();

		addToBack(BG);

		ExitButton btnExit = new ExitButton();
		btnExit.setPos(insets.left + w - btnExit.width(), insets.top);
		add(btnExit);

		fadeIn();
	}

	private void createActiveContent(NinePatch panel) {

		float contentX = panel.x + panel.marginLeft();
		float contentY = panel.y + panel.marginTop();
		float contentW = panel.width() - panel.marginHor();
		float contentH = panel.height() - panel.marginVer();

		switch (lastIDX) {

			case TAB_BADGES:
			default:
				badgesTab = new WndJournal.BadgesTab();
				add(badgesTab);
				badgesTab.setRect(contentX, contentY, contentW, contentH);
				break;

			case TAB_CATALOG:
				catalogTab = new WndJournal.CatalogTab();
				add(catalogTab);
				catalogTab.setRect(contentX, contentY, contentW, contentH);
				catalogTab.updateList();
				break;

			case TAB_GUIDE:
				guideTab = new WndJournal.GuideTab();
				add(guideTab);
				guideTab.setRect(contentX, contentY, contentW, contentH);
				guideTab.updateList();
				break;

			case TAB_ALCHEMY:
				alchemyTab = new WndJournal.AlchemyTab();
				add(alchemyTab);
				alchemyTab.setRect(contentX, contentY, contentW, contentH);
				break;
		}
	}

	private void createTabButtons(NinePatch panel, int pw, int ph) {

		btnBadges = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
			@Override
			protected void onClick() {
				switchJournalTab(TAB_BADGES);
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.BadgesTab.class, "title");
			}
		};
		btnBadges.icon(Icons.BADGES.get());
		btnBadges.setRect(panel.x, panel.y + ph - 3, pw / 4f + 1.5f, lastIDX == TAB_BADGES ? 25 : 20);
		align(btnBadges);
		addToBack(btnBadges);

		btnCatalog = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
			@Override
			protected void onClick() {
				switchJournalTab(TAB_CATALOG);
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.CatalogTab.class, "title");
			}
		};
		btnCatalog.icon(Icons.CATALOG.get());
		btnCatalog.setRect(btnBadges.right() - 2, btnBadges.top(), pw / 4f + 1.5f, lastIDX == TAB_CATALOG ? 25 : 20);
		align(btnCatalog);
		addToBack(btnCatalog);

		btnGuide = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
			@Override
			protected void onClick() {
				switchJournalTab(TAB_GUIDE);
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.GuideTab.class, "title");
			}
		};
		btnGuide.icon(new ItemSprite(ItemSpriteSheet.MASTERY));
		btnGuide.setRect(btnCatalog.right() - 2, btnBadges.top(), pw / 4f + 1.5f, lastIDX == TAB_GUIDE ? 25 : 20);
		align(btnGuide);
		addToBack(btnGuide);

		btnAlchemy = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "") {
			@Override
			protected void onClick() {
				switchJournalTab(TAB_ALCHEMY);
			}

			@Override
			protected String hoverText() {
				return Messages.get(WndJournal.AlchemyTab.class, "title");
			}
		};
		btnAlchemy.icon(Icons.ALCHEMY.get());
		btnAlchemy.setRect(btnGuide.right() - 2, btnBadges.top(), pw / 4f + 1.5f, lastIDX == TAB_ALCHEMY ? 25 : 20);
		align(btnAlchemy);
		addToBack(btnAlchemy);

		updateTabFocusVisuals();
	}

	private void registerKeyboardNavigation() {

		updateTabFocusVisuals();

		keyboardNavListener = new Signal.Listener<KeyEvent>() {
			@Override
			public boolean onSignal(KeyEvent event) {

				if (!event.pressed) {
					return false;
				}

				switch (event.code) {

					case Input.Keys.LEFT:
						switchJournalTab(lastIDX - 1);
						return true;

					case Input.Keys.RIGHT:
					case Input.Keys.TAB:
						switchJournalTab(lastIDX + 1);
						return true;

					case Input.Keys.UP:
					case Input.Keys.DOWN:
					case Input.Keys.ENTER:
					case Input.Keys.SPACE:
					case Input.Keys.HOME:
					case Input.Keys.END:
						return handleActiveTabKey(event.code);

					case Input.Keys.ESCAPE:
						ShatteredPixelDungeon.switchNoFade(TitleScene.class);
						return true;
				}

				return false;
			}
		};

		KeyEvent.addKeyListener(keyboardNavListener);
	}

	private boolean handleActiveTabKey(int keyCode) {

		if (lastIDX == TAB_BADGES && badgesTab != null) {
			return badgesTab.handleKeyboard(keyCode);
		}

		if (lastIDX == TAB_CATALOG && catalogTab != null) {
			return catalogTab.handleKeyboard(keyCode);
		}

		if (lastIDX == TAB_GUIDE && guideTab != null) {
			return guideTab.handleKeyboard(keyCode);
		}

		if (lastIDX == TAB_ALCHEMY && alchemyTab != null) {
			return alchemyTab.handleKeyboard(keyCode);
		}

		return false;
	}

	private void switchJournalTab(int index) {

		if (index < 0) {
			index = TAB_COUNT - 1;
		} else if (index >= TAB_COUNT) {
			index = 0;
		}

		if (lastIDX != index) {
			lastIDX = index;
			ShatteredPixelDungeon.seamlessResetScene();
		} else {
			updateTabFocusVisuals();
		}
	}

	private void updateTabFocusVisuals() {

		updateButtonFocus(btnBadges, lastIDX == TAB_BADGES);
		updateButtonFocus(btnCatalog, lastIDX == TAB_CATALOG);
		updateButtonFocus(btnGuide, lastIDX == TAB_GUIDE);
		updateButtonFocus(btnAlchemy, lastIDX == TAB_ALCHEMY);
	}

	private void updateButtonFocus(StyledButton button, boolean focused) {

		if (button == null) {
			return;
		}

		if (focused) {
			button.alpha(1f);
			button.textColor(Window.TITLE_COLOR);
			if (button.icon() != null) {
				button.icon().brightness(1.35f);
			}
		} else {
			button.alpha(0.85f);
			button.textColor(Window.WHITE);
			if (button.icon() != null) {
				button.icon().brightness(0.6f);
			}
		}
	}

	@Override
	public void destroy() {

		if (keyboardNavListener != null) {
			KeyEvent.removeKeyListener(keyboardNavListener);
			keyboardNavListener = null;
		}

		Badges.saveGlobal();

		super.destroy();
	}

	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade(TitleScene.class);
	}
}
