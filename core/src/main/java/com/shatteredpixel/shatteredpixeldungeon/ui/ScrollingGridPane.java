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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

public class ScrollingGridPane extends ScrollPane {

	private ArrayList<Component> items = new ArrayList<>();
	private ArrayList<ColorBlock> separators = new ArrayList<>();
	private ArrayList<GridItem> keyboardItems = new ArrayList<>();

	private int focusedIndex = -1;

	private static final int ITEM_SIZE	= 17;
	private static final int MIN_GROUP_SIZE = 3*(ITEM_SIZE+1);

	public ScrollingGridPane(){
		super(new Component());
	}

	@Override
	public void onClick(float x, float y) {
		for (Component item : items) {
			if ((item instanceof ScrollingGridPane.GridItem) && ((ScrollingGridPane.GridItem) item).onClick( x, y )) {
				break;
			}
		}
	}

	public void addItem( ScrollingGridPane.GridItem item ){
		content.add(item);
		items.add(item);
		keyboardItems.add(item);

		if (focusedIndex == -1) {
			focusedIndex = 0;
			updateKeyboardFocus();
		}
	}

	public void addHeader( String text ){
		addHeader( text, 7, false );
	}

	public void addHeader( String text, int size, boolean center ){
		GridHeader header = new GridHeader(text, size, center);
		content.add(header);
		items.add(header);
	}

	@Override
	public synchronized void clear() {
		content.clear();
		items.clear();
		separators.clear();
		keyboardItems.clear();
		focusedIndex = -1;
	}

	public boolean handleKeyboard(int keyCode) {
		if (keyboardItems.isEmpty()) {
			return false;
		}

		if (focusedIndex < 0 || focusedIndex >= keyboardItems.size()) {
			focusedIndex = 0;
			updateKeyboardFocus();
			scrollFocusedIntoView();
		}

		switch (keyCode) {
			case Input.Keys.LEFT:
				moveSpatial(-1, 0);
				return true;

			case Input.Keys.RIGHT:
				moveSpatial(1, 0);
				return true;

			case Input.Keys.UP:
				moveSpatial(0, -1);
				return true;

			case Input.Keys.DOWN:
				moveSpatial(0, 1);
				return true;

			case Input.Keys.TAB:
				moveLinear(1);
				return true;

			case Input.Keys.ENTER:
			case Input.Keys.SPACE:
				activateFocusedItem();
				return true;

			case Input.Keys.HOME:
				focusedIndex = 0;
				updateKeyboardFocus();
				scrollFocusedIntoView();
				return true;

			case Input.Keys.END:
				focusedIndex = keyboardItems.size() - 1;
				updateKeyboardFocus();
				scrollFocusedIntoView();
				return true;
		}

		return false;
	}

	private void moveLinear(int direction) {
		if (keyboardItems.isEmpty()) {
			return;
		}

		focusedIndex += direction;

		if (focusedIndex < 0) {
			focusedIndex = keyboardItems.size() - 1;
		} else if (focusedIndex >= keyboardItems.size()) {
			focusedIndex = 0;
		}

		updateKeyboardFocus();
		scrollFocusedIntoView();
	}

	private void moveSpatial(int dx, int dy) {
		if (keyboardItems.isEmpty()) {
			return;
		}

		GridItem current = keyboardItems.get(focusedIndex);

		float cx = centerX(current);
		float cy = centerY(current);

		int bestIndex = -1;
		float bestScore = Float.MAX_VALUE;

		for (int i = 0; i < keyboardItems.size(); i++) {
			if (i == focusedIndex) {
				continue;
			}

			GridItem candidate = keyboardItems.get(i);

			float tx = centerX(candidate);
			float ty = centerY(candidate);

			float diffX = tx - cx;
			float diffY = ty - cy;

			if (dx > 0 && diffX <= 0) continue;
			if (dx < 0 && diffX >= 0) continue;
			if (dy > 0 && diffY <= 0) continue;
			if (dy < 0 && diffY >= 0) continue;

			float primary = dx != 0 ? Math.abs(diffX) : Math.abs(diffY);
			float secondary = dx != 0 ? Math.abs(diffY) : Math.abs(diffX);

			// Industrial-style directional scoring:
			// 1. Strongly prefer the requested direction.
			// 2. Prefer items on the same row/column.
			// 3. Then choose the nearest candidate.
			float score = primary * 1000f + secondary * 10f;

			if (score < bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		}

		// Boundary rule: stop at the edge instead of jumping unpredictably.
		if (bestIndex != -1) {
			focusedIndex = bestIndex;
			updateKeyboardFocus();
			scrollFocusedIntoView();
		}
	}

	private void updateKeyboardFocus() {
		for (int i = 0; i < keyboardItems.size(); i++) {
			keyboardItems.get(i).setKeyboardFocused(i == focusedIndex);
		}
	}

	private void activateFocusedItem() {
		if (focusedIndex < 0 || focusedIndex >= keyboardItems.size()) {
			return;
		}

		GridItem focused = keyboardItems.get(focusedIndex);
		focused.keyboardClick();
	}

	private void scrollFocusedIntoView() {
		if (focusedIndex < 0 || focusedIndex >= keyboardItems.size()) {
			return;
		}

		GridItem focused = keyboardItems.get(focusedIndex);

		float focusTop = focused.top();
		float focusBottom = focused.bottom();

		float viewTop = content.camera.scroll.y;
		float viewBottom = viewTop + height();

		if (focusTop < viewTop) {
			scrollTo(0, Math.max(0, focusTop - 2));
		} else if (focusBottom > viewBottom) {
			scrollTo(0, Math.max(0, focusBottom - height() + 2));
		}
	}

	private float centerX(Component component) {
		return component.left() + component.width() / 2f;
	}

	private float centerY(Component component) {
		return component.top() + component.height() / 2f;
	}

	@Override
	protected void layout() {

		float left = 0;
		float top = 0;

		int sepsUsed = 0;

		boolean freshRow = true;
		boolean lastWasSmallheader = false;
		float widthThisGroup = 0;

		for (int i = 0; i < items.size(); i++){
			Component item = items.get(i);
			if (item instanceof GridHeader){
				if (left > 0 || lastWasSmallheader){

					float spacing = Math.max(0, MIN_GROUP_SIZE - widthThisGroup);
					float spaceLeft = width() - (left + spacing);
					int spaceReq = 0;
					for (int j = i+1; j < items.size(); j++){
						if (items.get(j) instanceof GridItem){
							spaceReq += ITEM_SIZE+1;
						} else {
							break;
						}
					}
					spaceReq = Math.max(spaceReq, MIN_GROUP_SIZE);
					if (!((GridHeader) item).center && freshRow && spaceLeft >= spaceReq){
						left = left + spacing;
						top -= item.height()+1;
						ColorBlock sep;
						if (separators.size() > sepsUsed){
							sep = separators.get(sepsUsed++);
						} else {
							sep = new ColorBlock(1, 1, 0xFF222222);
							separators.add(sep);
							content.add(sep);
							sepsUsed++;
						}
						sep.size(1, item.height()+1+ITEM_SIZE);
						sep.x = left-1;
						sep.y = top;
					} else {
						left = 0;
						top += ITEM_SIZE + 2;
						freshRow = true;
					}
				}
				item.setRect(left, top, width(), item.height());
				top += item.height()+1;
				widthThisGroup = 0;

				if (!((GridHeader) item).center){
					lastWasSmallheader = true;
				} else {
					lastWasSmallheader = false;
				}

			} if (item instanceof GridItem){
				if (left + ITEM_SIZE > width()) {
					left = 0;
					widthThisGroup = 0;
					top += ITEM_SIZE+1;
					freshRow = false;
				}
				item.setRect(left, top, ITEM_SIZE, ITEM_SIZE);
				left += ITEM_SIZE+1;
				widthThisGroup += ITEM_SIZE+1;
				lastWasSmallheader = false;
			}

		}
		if (left > 0){
			left = 0;
			top += ITEM_SIZE+1;
		}

		while (separators.size() > sepsUsed){
			ColorBlock sep = separators.remove(sepsUsed);
			content.remove(sep);
		}

		content.setSize(width, top);
		super.layout();

		updateKeyboardFocus();
	}

	public static class GridItem extends Component {

		protected Image icon;

		protected Visual secondIcon;

		protected ColorBlock bg;

		private boolean keyboardFocused = false;

		public GridItem( Image icon ) {
			super();

			if (icon instanceof ItemSprite){
				this.icon = new ItemSprite();
			} else {
				this.icon = new Image();
			}
			this.icon.copy(icon);
			add(this.icon);
		}

		public void addSecondIcon( Visual icon ){
			secondIcon = icon;
			add(secondIcon);
			layout();
		}

		public void hardLightBG( float r, float g, float b ){
			bg.hardlight(r, g, b);
		}

		public void setKeyboardFocused(boolean focused) {
			keyboardFocused = focused;

			if (focused) {
				bg.hardlight(Window.TITLE_COLOR);
				icon.brightness(1.25f);
				if (secondIcon != null) secondIcon.brightness(1.25f);
			} else {
				bg.resetColor();
				icon.resetColor();
				if (secondIcon != null) secondIcon.resetColor();
			}
		}

		public boolean isKeyboardFocused() {
			return keyboardFocused;
		}

		public void keyboardClick() {
			onClick(x + width() / 2f, y + height() / 2f);
		}

		public boolean onClick( float x, float y ){
			return false;
		}

		@Override
		protected void createChildren() {
			bg = new ColorBlock( 1, 1, 0x9953564D);
			add(bg);
		}

		@Override
		protected void layout() {

			bg.x = x;
			bg.y = y;
			bg.size(width(), height());

			icon.y = y + (height() - icon.height()) / 2f;
			icon.x = x + (width() - icon.width())/2f;
			PixelScene.align(icon);

			if (secondIcon != null){
				secondIcon.x = x + width()-secondIcon.width();
				secondIcon.y = y;
			}

		}

	}

	public static class GridHeader extends Component {

		protected RenderedTextBlock text;
		boolean center;

		public GridHeader( String text ){
			this(text, 7, false);
		}

		public GridHeader( String text, int size, boolean center ){
			super();

			this.center = center;
			this.text = PixelScene.renderTextBlock(text, size);
			add(this.text);

		}

		@Override
		protected void createChildren() {
			super.createChildren();
		}

		@Override
		protected void layout() {
			super.layout();

			if (center){
				text.align(RenderedTextBlock.CENTER_ALIGN);
				text.maxWidth((int)width());
				text.setPos(x + (width() - text.width()) / 2, y+1);
			} else {
				text.maxWidth((int)width());
				text.setPos(x, y+1);
			}
		}

		@Override
		public float height() {
			if (center){
				return text.height() + 3;
			} else {
				return text.height() + 2;
			}
		}
	}

}
