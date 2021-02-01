/*
 *
 *  * Copyright (c) 2021, Senmori
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  *
 *  * 1. Redistributions of source code must retain the above copyright notice, this
 *  *    list of conditions and the following disclaimer.
 *  * 2. Redistributions in binary form must reproduce the above copyright notice,
 *  *    this list of conditions and the following disclaimer in the documentation
 *  *    and/or other materials provided with the distribution.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package com.questhelper.panel;

import com.questhelper.QuestHelperPlugin;
import com.questhelper.panel.screen.FixedWidthPanel;
import com.questhelper.panel.screen.QuestScreen;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.annotation.Nonnull;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.util.SwingUtil;

class ActiveContainer extends JScrollPane
{
	private final FixedWidthPanel currentDisplayPanel = new FixedWidthPanel();
	private QuestScreen currentScreen = null;
	private final QuestScreen _defaultScreen;

	private final QuestHelperPlugin plugin;
	protected ActiveContainer(QuestHelperPlugin plugin, final @Nonnull QuestScreen defaultScreen)
	{
		this.plugin = plugin;
		this._defaultScreen = defaultScreen;
		currentDisplayPanel.setLayout(new BorderLayout());
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		super.setViewportView(currentDisplayPanel);
	}

	public QuestScreen getCurrentScreen()
	{
		return currentScreen == null ? _defaultScreen : currentScreen;
	}

	@Override
	public void setViewportView(Component view)
	{
		// don't let screens change it
	}


	public void setScreen(QuestScreen screen)
	{
		if (screen == currentScreen) {
			return; // we aren't changing screens; ignore it
		}
		//plugin.getEventBus().post(new ScreenChange(screen, currentScreen, _defaultScreen));
		SwingUtil.fastRemoveAll(currentDisplayPanel);
		this.currentScreen = screen == null ? _defaultScreen : screen;
		currentDisplayPanel.add(currentScreen, BorderLayout.NORTH);
		revalidate();
	}
}