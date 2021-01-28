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
import com.questhelper.QuestHelperConfig;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.panel.component.TitlePanel;
import com.questhelper.panel.screen.QuestScreen;
import com.questhelper.panel.screen.QuestSearchScreen;
import com.questhelper.questhelpers.Quest;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.steps.QuestStep;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class QuestHelperPanel extends PluginPanel
{
	@Getter
	private final QuestOverviewPanel questOverviewPanel;

	private final QuestHelperPlugin plugin;

	// ui-upgrade fields
	private final TitlePanel titlePanel = new TitlePanel("Quest Helper");
	private final SearchPanel searchPanel;
	private final ActiveContainer activeContainer;
	private final QuestSearchScreen questSearchScreen;

	@Getter(AccessLevel.PUBLIC)
	private final BankItems bankItems = new BankItems();
	private final Client client;
	private final ClientThread clientThread;

	public QuestHelperPanel(QuestHelperPlugin plugin)
	{
		super(false);
		this.plugin = plugin;
		this.client = plugin.getClient();
		this.clientThread = plugin.getClientThread();

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		/* Setup overview panel */
		searchPanel = new SearchPanel(plugin);

		JPanel introDetailsPanel = new JPanel();
		introDetailsPanel.setLayout(new BorderLayout());
		introDetailsPanel.add(titlePanel, BorderLayout.NORTH);
		introDetailsPanel.add(searchPanel, BorderLayout.CENTER);

		add(introDetailsPanel, BorderLayout.NORTH);

		/* Screens */
		questOverviewPanel = new QuestOverviewPanel(plugin, this);
		questSearchScreen = new QuestSearchScreen(plugin,this, searchPanel);
		this.activeContainer = new ActiveContainer(plugin, questSearchScreen.getQuestListPanel());

		add(activeContainer, BorderLayout.CENTER);
		questSearchScreen.updateSearchFilter("");
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		log.debug("GAME TICK");
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getItemContainer() == client.getItemContainer(InventoryID.BANK))
		{
			bankItems.setItems(null);
			bankItems.setItems(event.getItemContainer().getItems());
		}
		if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY))
		{
			clientThread.invokeLater(() -> getCurrentScreen().updateRequirements(client, bankItems));
		}
	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged event)
	{
		final GameState state = event.getGameState();

		if (state == GameState.LOGIN_SCREEN)
		{
			getCurrentScreen().updateQuests(Collections.emptyList(), state, new HashMap<>());
			bankItems.setItems(null);
		}
		if (state == GameState.LOGGED_IN)
		{
			updateQuestList();
		}
	}

	private final Collection<String> configEvents = Arrays.asList("orderListBy", "filterListBy", "questDifficulty", "showCompletedQuests");
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("questhelper") && configEvents.contains(event.getKey()))
		{
			clientThread.invokeLater(this::updateQuestList);
		}
	}

	private void updateQuestList()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			QuestHelperConfig config = plugin.getConfig();
			List<QuestHelper> filteredQuests = plugin.getQuests().values()
				.stream()
				.filter(config.filterListBy())
				.filter(config.difficulty())
				.filter(Quest::showCompletedQuests)
				.sorted(config.orderListBy())
				.collect(Collectors.toList());
			Map<QuestHelperQuest, QuestState> questStates = plugin.getQuests().values()
				.stream()
				.collect(Collectors.toMap(QuestHelper::getQuest, q -> q.getState(client)));
			SwingUtilities.invokeLater(() -> getCurrentScreen().updateQuests(filteredQuests, client.getGameState(), questStates));
		}
	}

	/**
	 * Get the var of a quest while off the client thread.
	 * <br>
	 * This method swallows exceptions.
	 *
	 * @param quest the quest to query
	 * @return the current var of the quest, or {@link Integer#MIN_VALUE} if there was a problem.
	 */
	public synchronized int getSafeQuestVar(QuestHelperQuest quest)
	{
		FutureTask<Integer> task = new FutureTask<>(() -> quest.getVar(client));
		clientThread.invoke(task);
		int var = Integer.MIN_VALUE;
		try
		{
			var = task.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			log.error("Error retrieving quest state for Quest " + quest.getName() + ".", e);
		}
		return var;
	}

	public final void setActiveDisplay(QuestScreen component)
	{
		activeContainer.setScreen(component);
	}

	public QuestScreen getCurrentScreen()
	{
		return activeContainer.getCurrentScreen();
	}

	//TODO: This method stays after some rework
	public void addQuest(QuestHelper quest, boolean isActive)
	{
		searchPanel.getAllDropdownSections().setVisible(false);
		setActiveDisplay(questOverviewPanel);

		questOverviewPanel.addQuest(quest, isActive);

		repaint();
		revalidate();
	}

	public void updateSteps()
	{
		questOverviewPanel.updateSteps();
	}

	public void updateHighlight(QuestStep newStep)
	{
		questOverviewPanel.updateHighlight(newStep);

		repaint();
		revalidate();
	}

	public void updateLocks()
	{
		questOverviewPanel.updateLocks();

		repaint();
		revalidate();
	}

	public void removeQuest()
	{
		searchPanel.getAllDropdownSections().setVisible(true);
		setActiveDisplay(questSearchScreen.getQuestListPanel());
		questOverviewPanel.removeQuest();

		repaint();
		revalidate();
	}

	public void emptyBar()
	{
		searchPanel.setText("");
	}

	public void updateItemRequirements(Client client, BankItems bankItems)
	{
		questOverviewPanel.updateRequirements(client, bankItems);
	}
}
