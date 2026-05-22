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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.journal.Journal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndGameInProgress;
import com.watabou.input.KeyEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.utils.RectF;
import com.watabou.utils.Signal;

import java.util.ArrayList;

public class StartScene extends PixelScene {

	private static final int SLOT_WIDTH = 120;
	private static final int SLOT_HEIGHT = 22;

	private final ArrayList<SaveSlotButton> keyboardSlots = new ArrayList<>();
	private StyledButton keyboardSortButton;
	private int focusedIndex = 0;
	private Signal.Listener<KeyEvent> keyboardNavListener;

	@Override
	public void create() {
		super.create();

		Badges.loadGlobal();
		Journal.loadGlobal();
		keyboardSlots.clear();
		keyboardSortButton = null;

		uiCamera.visible = false;

		int w = Camera.main.width;
		int h = Camera.main.height;
		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		add( BG );

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		ExitButton btnExit = new ExitButton();
		btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
		add( btnExit );

		IconTitle title = new IconTitle( Icons.ENTER.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		ArrayList<GamesInProgress.Info> games = GamesInProgress.checkAll();

		int slotCount = Math.min(GamesInProgress.MAX_SLOTS, games.size()+1);
		int slotGap = 10 - slotCount;
		int slotsHeight = slotCount*SLOT_HEIGHT + (slotCount-1)* slotGap;
		slotsHeight += 14;

		while (slotGap >= 2 && slotsHeight > (h-title.bottom()-2)){
			slotGap--;
			slotsHeight -= slotCount-1;
		}

		float yPos = insets.top + (h - slotsHeight + title.bottom() + 2)/2f - 4;
		yPos = Math.max(yPos, title.bottom()+2);
		float slotLeft = insets.left + (w - SLOT_WIDTH) / 2f;

		for (GamesInProgress.Info game : games) {
			SaveSlotButton existingGame = new SaveSlotButton();
			existingGame.set(game.slot);
			existingGame.setRect(slotLeft, yPos, SLOT_WIDTH, SLOT_HEIGHT);
			yPos += SLOT_HEIGHT + slotGap;
			align(existingGame);
			add(existingGame);
			keyboardSlots.add(existingGame);

		}

		if (games.size() < GamesInProgress.MAX_SLOTS){
			SaveSlotButton newGame = new SaveSlotButton();
			newGame.set(GamesInProgress.firstEmpty());
			newGame.setRect(slotLeft, yPos, SLOT_WIDTH, SLOT_HEIGHT);
			yPos += SLOT_HEIGHT + slotGap;
			align(newGame);
			add(newGame);
			keyboardSlots.add(newGame);
		}

		GamesInProgress.curSlot = 0;

		String sortText = "";
		switch (SPDSettings.gamesInProgressSort()){
			case "level":
				sortText = Messages.get(this, "sort_level");
				break;
			case "last_played":
				sortText = Messages.get(this, "sort_recent");
				break;
		}

		StyledButton btnSort = new StyledButton(Chrome.Type.TOAST_TR, sortText, 6){
			@Override
			protected void onClick() {
				super.onClick();

				if (SPDSettings.gamesInProgressSort().equals("level")){
					SPDSettings.gamesInProgressSort("last_played");
				} else {
					SPDSettings.gamesInProgressSort("level");
				}

				ShatteredPixelDungeon.seamlessResetScene();
			}
		};
		btnSort.textColor(0xCCCCCC);

		if (yPos + 10 > Camera.main.height) {
			btnSort.setRect(slotLeft - btnSort.reqWidth() - 6, Camera.main.height - 14, btnSort.reqWidth() + 4, 12);
		} else {
			btnSort.setRect(slotLeft, yPos, btnSort.reqWidth() + 4, 12);
		}
		if (games.size() >= 2) {
			add(btnSort);
			keyboardSortButton = btnSort;
		}

		registerKeyboardNavigation();

		fadeIn();

	}


	private void registerKeyboardNavigation() {
		focusedIndex = 0;
		updateKeyboardFocus();

		keyboardNavListener = new Signal.Listener<KeyEvent>() {
			@Override
			public boolean onSignal(KeyEvent event) {
				if (!event.pressed) {
					return false;
				}

				switch (event.code) {
					case Input.Keys.DOWN:
					case Input.Keys.RIGHT:
					case Input.Keys.TAB:
						moveFocus(1);
						return true;

					case Input.Keys.UP:
					case Input.Keys.LEFT:
						moveFocus(-1);
						return true;

					case Input.Keys.ENTER:
					case Input.Keys.SPACE:
						activateFocusedItem();
						return true;

					case Input.Keys.ESCAPE:
						ShatteredPixelDungeon.switchNoFade(TitleScene.class);
						return true;
				}

				return false;
			}
		};

		KeyEvent.addKeyListener(keyboardNavListener);
	}

	private int focusableCount() {
		return keyboardSlots.size() + (keyboardSortButton != null ? 1 : 0);
	}

	private void moveFocus(int direction) {
		int count = focusableCount();
		if (count == 0) {
			return;
		}

		focusedIndex += direction;

		// Industrial-style wrap navigation: moving past the end returns to the start.
		if (focusedIndex < 0) {
			focusedIndex = count - 1;
		} else if (focusedIndex >= count) {
			focusedIndex = 0;
		}

		updateKeyboardFocus();
	}

	private void updateKeyboardFocus() {
		for (int i = 0; i < keyboardSlots.size(); i++) {
			keyboardSlots.get(i).setKeyboardFocused(i == focusedIndex);
		}

		if (keyboardSortButton != null) {
			if (focusedIndex == keyboardSlots.size()) {
				keyboardSortButton.textColor(Window.TITLE_COLOR);
				keyboardSortButton.alpha(1f);
			} else {
				keyboardSortButton.textColor(0xCCCCCC);
				keyboardSortButton.alpha(0.85f);
			}
		}
	}

	private void activateFocusedItem() {
		if (focusedIndex >= 0 && focusedIndex < keyboardSlots.size()) {
			keyboardSlots.get(focusedIndex).keyboardClick();
		} else if (keyboardSortButton != null && focusedIndex == keyboardSlots.size()) {
			keyboardSortButton.keyboardClick();
		}
	}

	@Override
	public void destroy() {
		if (keyboardNavListener != null) {
			KeyEvent.removeKeyListener(keyboardNavListener);
			keyboardNavListener = null;
		}

		super.destroy();
	}


	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade( TitleScene.class );
	}

	private static class SaveSlotButton extends Button {

		private NinePatch bg;

		private Image hero;
		private RenderedTextBlock name;
		private RenderedTextBlock lastPlayed;

		private Image steps;
		private BitmapText depth;
		private Image classIcon;
		private BitmapText level;

		private int slot;
		private boolean newGame;
		private boolean keyboardFocused;

		@Override
		protected void createChildren() {
			super.createChildren();

			bg = Chrome.get(Chrome.Type.TOAST_TR);
			add( bg );

			name = PixelScene.renderTextBlock(9);
			add(name);

			lastPlayed = PixelScene.renderTextBlock(6);
			add(lastPlayed);
		}

		public void set( int slot ){
			this.slot = slot;
			GamesInProgress.Info info = GamesInProgress.check(slot);
			newGame = info == null;
			if (newGame){
				name.text( Messages.get(StartScene.class, "new"));

				if (hero != null){
					remove(hero);
					hero = null;
					remove(steps);
					steps = null;
					remove(depth);
					depth = null;
					remove(classIcon);
					classIcon = null;
					remove(level);
					level = null;
				}
			} else {

				if (info.subClass != HeroSubClass.NONE){
					name.text(Messages.titleCase(info.subClass.title()));
				} else {
					name.text(Messages.titleCase(info.heroClass.title()));
				}

				if (hero == null){
					hero = new Image(info.heroClass.spritesheet(), 0, 15*info.armorTier, 12, 15);
					add(hero);

					steps = new Image(Icons.get(Icons.STAIRS));
					add(steps);
					depth = new BitmapText(PixelScene.pixelFont);
					add(depth);

					classIcon = new Image(Icons.get(info.heroClass));
					add(classIcon);
					level = new BitmapText(PixelScene.pixelFont);
					add(level);
				} else {
					hero.copy(new Image(info.heroClass.spritesheet(), 0, 15*info.armorTier, 12, 15));

					classIcon.copy(Icons.get(info.heroClass));
				}

				long diff = Game.realTime - info.lastPlayed;
				if (diff > 99L * 30 * 24 * 60 * 60_000){
					lastPlayed.text(" "); //show no text for >99 months ago
				} else if (diff < 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "one_minute_ago"));
				} else if (diff < 2 * 60 * 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "minutes_ago", diff / 60_000));
				} else if (diff < 2 * 24 * 60 * 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "hours_ago", diff / (60 * 60_000)));
				} else if (diff < 2L * 30 * 24 * 60 * 60_000){
					lastPlayed.text(Messages.get(StartScene.class, "days_ago", diff / (24 * 60 * 60_000)));
				} else {
					lastPlayed.text(Messages.get(StartScene.class, "months_ago", diff / (30L * 24 * 60 * 60_000)));
				}

				depth.text(Integer.toString(info.depth));
				depth.measure();

				level.text(Integer.toString(info.level));
				level.measure();

				if (info.challenges > 0){
					name.hardlight(Window.TITLE_COLOR);
					lastPlayed.hardlight(Window.TITLE_COLOR);
					depth.hardlight(Window.TITLE_COLOR);
					level.hardlight(Window.TITLE_COLOR);
				} else {
					name.resetColor();
					lastPlayed.resetColor();
					depth.resetColor();
					level.resetColor();
				}

				if (info.daily){
					if (info.dailyReplay){
						steps.hardlight(1f, 0.5f, 2f);
					} else {
						steps.hardlight(0.5f, 1f, 2f);
					}
				} else if (!info.customSeed.isEmpty()){
					steps.hardlight(1f, 1.5f, 0.67f);
				}

			}

			layout();
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;
			bg.size( width, height );

			if (hero != null){
				hero.x = x+8;
				hero.y = y + (height - hero.height())/2f;
				align(hero);

				name.setPos(
						hero.x + hero.width() + 6,
						y + (height - name.height() - lastPlayed.height() - 2)/2f
				);
				align(name);

				lastPlayed.setPos(
						hero.x + hero.width() + 6,
						name.bottom()+2
				);

				classIcon.x = x + width - 24 + (16 - classIcon.width())/2f;
				classIcon.y = y + (height - classIcon.height())/2f;
				align(classIcon);

				level.x = classIcon.x + (classIcon.width() - level.width()) / 2f;
				level.y = classIcon.y + (classIcon.height() - level.height()) / 2f + 1;
				align(level);

				steps.x = x + width - 40 + (16 - steps.width())/2f;
				steps.y = y + (height - steps.height())/2f;
				align(steps);

				depth.x = steps.x + (steps.width() - depth.width()) / 2f;
				depth.y = steps.y + (steps.height() - depth.height()) / 2f + 1;
				align(depth);

			} else {
				name.setPos(
						x + (width - name.width())/2f,
						y + (height - name.height())/2f
				);
				align(name);
			}


		}

		public void setKeyboardFocused(boolean keyboardFocused) {
			this.keyboardFocused = keyboardFocused;

			if (keyboardFocused) {
				// Strong keyboard focus feedback: yellow text + brighter background/icons.
				bg.brightness(1.35f);
				bg.alpha(1f);

				name.hardlight(Window.TITLE_COLOR);
				lastPlayed.hardlight(Window.TITLE_COLOR);

				if (hero != null) hero.brightness(1.2f);
				if (steps != null) steps.brightness(1.15f);
				if (classIcon != null) classIcon.brightness(1.2f);

			} else {
				bg.resetColor();
				bg.alpha(0.92f);

				if (hero != null) hero.resetColor();
				if (steps != null) steps.resetColor();
				if (classIcon != null) classIcon.resetColor();

				if (!newGame) {
					GamesInProgress.Info info = GamesInProgress.check(slot);
					if (info != null && info.challenges > 0) {
						name.hardlight(Window.TITLE_COLOR);
						lastPlayed.hardlight(Window.TITLE_COLOR);
					} else {
						name.resetColor();
						lastPlayed.resetColor();
					}
				} else {
					name.resetColor();
					lastPlayed.resetColor();
				}
			}
		}

		public void keyboardClick() {
			onClick();
		}

		@Override
		protected void onClick() {
			if (newGame) {
				GamesInProgress.selectedClass = null;
				GamesInProgress.curSlot = slot;
				ShatteredPixelDungeon.switchScene(HeroSelectScene.class);
			} else {
				ShatteredPixelDungeon.scene().add( new WndGameInProgress(slot));
			}
		}
	}
}
