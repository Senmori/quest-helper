/*
 * Copyright (c) 2020, Zoinkwiz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.questhelper.panel;

import com.questhelper.BankItems;
import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.component.Updatable;
import com.questhelper.panel.component.UpdatableRequirement;
import com.questhelper.panel.panels.QuestScreen;
import com.questhelper.panel.panels.QuestSearchScreen;
import com.questhelper.questhelpers.QuestHelper;
import java.util.*;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.steps.QuestStep;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class QuestHelperPanel extends PluginPanel implements Updatable, UpdatableRequirement
{
	private QuestScreen currentScreen;
	private final QuestSearchScreen questSearchScreen;

	private final QuestHelperPlugin questHelperPlugin;

	public QuestHelperPanel(QuestHelperPlugin questHelperPlugin)
	{
		super(false);

		this.questHelperPlugin = questHelperPlugin;
		this.questSearchScreen = new QuestSearchScreen(questHelperPlugin, this);
		this.currentScreen = questSearchScreen;
		add(currentScreen);
	}

	/**
	 * Updates this panel with the currently active screen.<br>
	 * If 'null' is provided, the default quest search screen will be displayed.
	 * @param screen the new screen
	 * @return true if the screen was changed.
	 */
	public final boolean setActiveScreen(QuestScreen screen)
	{
		if (screen != null && screen != currentScreen)
		{
			questHelperPlugin.getEventBus().register(screen);
			questHelperPlugin.getEventBus().unregister(currentScreen);
			this.currentScreen = screen;
			return true;
		}
		if (screen == null && currentScreen != questSearchScreen)
		{
			questHelperPlugin.getEventBus().register(questSearchScreen);
			questHelperPlugin.getEventBus().unregister(currentScreen);
			this.currentScreen = questSearchScreen;
			return true;
		}
		return false;
	}

	@Override
	public void update(@Nonnull Client client, @Nonnull ClientThread clientThread)
	{
		questSearchScreen.update(client, clientThread);
	}

	@Override
	public void updateRequirements(@Nonnull Client client, @Nonnull BankItems bankItems)
	{
		questSearchScreen.updateRequirements(client, bankItems);
	}

	@Override
	public void updateQuests(List<QuestHelper> questHelpers, boolean loggedOut, Map<QuestHelperQuest, QuestState> completedQuests)
	{
		questSearchScreen.updateQuests(questHelpers, loggedOut, completedQuests);
	}

	public void addQuest(QuestHelper quest, boolean isActive)
	{
		questSearchScreen.addQuest(quest, isActive);
	}

	public void updateSteps()
	{
		questSearchScreen.updateSteps();
	}

	public void updateHighlight(QuestStep newStep)
	{
		questSearchScreen.updateHighlight(newStep);
	}

	public void updateLocks()
	{
		questSearchScreen.updateLocks();
	}

	public void removeQuest()
	{
		questSearchScreen.removeQuest();
	}

	public void emptyBar()
	{
		questSearchScreen.emptyBar();
	}
}
