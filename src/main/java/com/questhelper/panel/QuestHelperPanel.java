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
import com.questhelper.StreamUtil;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.panel.component.TitlePanel;
import com.questhelper.panel.event.ScreenChange;
import com.questhelper.panel.screen.QuestScreen;
import com.questhelper.panel.screen.QuestSearchScreen;
import com.questhelper.panel.screen.ScreenFactory;
import com.questhelper.questhelpers.Quest;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.requirements.RequirementContainer;
import com.questhelper.steps.QuestStep;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
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
import net.runelite.api.events.VarbitChanged;
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
	@Getter
	private final SearchPanel searchPanel;
	@Getter
	private final ActiveContainer activeContainer;
	private final QuestSearchScreen questSearchScreen;

	private final List<RequirementContainer> requirementContainers = new ArrayList<>();
	private final List<QuestContainer> questContainers = new ArrayList<>();
	private final Map<Predicate<QuestScreen>, Consumer<QuestScreen>> screenFilters = new HashMap<>();

	@Getter(AccessLevel.PUBLIC)
	private final BankItems bankItems = new BankItems();
	private final Client client;
	private final ClientThread clientThread;
	private GameState currentClientGameState = GameState.UNKNOWN;

	// Quest vars ->
	private QuestHelper selectedQuest, sidebarSelectedQuest;
	private QuestStep lastStep = null;
	private boolean loadQuestList;

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

		buildScreenFilters();

		/* Screens */
		questOverviewPanel = registerScreen(QuestOverviewPanel::new);
		questSearchScreen = registerScreen(QuestSearchScreen::new);
		this.activeContainer = new ActiveContainer(plugin, questSearchScreen);

		add(activeContainer, BorderLayout.CENTER);
		setActiveDisplay(questSearchScreen);
		questSearchScreen.updateSearchFilter("");
	}

	private void buildScreenFilters()
	{
		screenFilters.put(RequirementContainer.class::isInstance, (screen) -> requirementContainers.add((RequirementContainer) screen));
		screenFilters.put(QuestContainer.class::isInstance, (screen) -> questContainers.add((QuestContainer) screen));
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
			updateRequirements(client, bankItems);
		}
	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged event)
	{
		final GameState state = event.getGameState();
		this.currentClientGameState = state;

		if (state == GameState.LOGIN_SCREEN)
		{
			SwingUtilities.invokeLater(() -> updateQuests(Collections.emptyList(), state, new HashMap<>()));
			bankItems.setItems(null);
			if (selectedQuest != null && selectedQuest.getCurrentStep() != null)
			{
				shutDownQuest(true);
			}
		}
		if (state == GameState.LOGGED_IN)
		{
			loadQuestList = true;
		}
	}

	private final Collection<String> configEvents = Arrays.asList("orderListBy", "filterListBy", "questDifficulty", "showCompletedQuests");
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{

		if (event.getGroup().equals("questhelper") && configEvents.contains(event.getKey()))
		{
			updateQuestList();
		}
	}

	public void updateQuestList()
	{
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invoke(() -> {
				QuestHelperConfig config = plugin.getConfig();
				GameState state = client.getGameState();
				List<QuestHelper> filteredQuests = plugin.getQuests().values()
					.stream()
					.filter(config.filterListBy())
					.filter(config.difficulty())
					.filter(Quest::showCompletedQuests)
					.sorted(config.orderListBy())
					.collect(Collectors.toList());
				Map<QuestHelperQuest, QuestState> questStates = StreamUtil.toQuestMap(plugin.getQuests().values().stream(), client);
				SwingUtilities.invokeLater(() -> updateQuests(filteredQuests, state, questStates));
			});
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (sidebarSelectedQuest != null)
		{
			log.debug("SIDEBAR QUEST: " + sidebarSelectedQuest.getQuest().getName());
			startUpQuest(sidebarSelectedQuest);
			sidebarSelectedQuest = null;
		}
		else if (selectedQuest != null)
		{
			if (selectedQuest.getCurrentStep() != null)
			{
				updateSteps();
				QuestStep currentStep = selectedQuest.getCurrentStep().getSidePanelStep();
				if (currentStep != null && currentStep != lastStep)
				{
					lastStep = currentStep;
					updateHighlight(currentStep);
				}
				updateLocks();
			}
		}
		if (loadQuestList)
		{
			loadQuestList = false;
			SwingUtilities.invokeLater(this::updateQuestList);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		boolean shouldShutDownQuest = selectedQuest != null && selectedQuest.updateQuest() && selectedQuest.getCurrentStep() == null;
		if (shouldShutDownQuest)
		{
			shutDownQuest(true);
		}
	}

	@Subscribe
	public void onScreenChange(ScreenChange event)
	{
		String old = event.getOldScreen().getClass().getSimpleName();
		String newScreen = event.getNewScreen().getClass().getSimpleName();
		log.debug("Changed Screens: (New -> " + newScreen + ") --> (Old -> " + old + ")");
	}

	public void startUpQuest(QuestHelper questHelper)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		shutDownQuest(true);

		if (!questHelper.isCompleted())
		{
			selectedQuest = questHelper;
			plugin.getEventBus().register(selectedQuest);
			if (plugin.isDeveloperMode())
			{
				selectedQuest.debugStartup(plugin.getConfig());
			}
			selectedQuest.startUp(plugin.getConfig());
			if (selectedQuest.getCurrentStep() == null)
			{
				//TODO: throw error
				shutDownQuest(false);
				return;
			}
			plugin.getBankTagsMain().startUp();
			revalidate();
			SwingUtilities.invokeLater(() -> {
				addQuest(questHelper, true);
				clientThread.invokeLater(() -> updateItemRequirements(client, bankItems));
			});
		}
		else
		{
			removeQuest();
			selectedQuest = null;
		}
	}

	public void shutDownQuestFromSidebar()
	{
		if (selectedQuest != null)
		{
			selectedQuest.shutDown();
			plugin.getBankTagsMain().shutDown();
			removeQuest();
			plugin.getEventBus().unregister(selectedQuest);
			selectedQuest = null;
		}
	}

	public void shutDownQuest(boolean shouldUpdateList)
	{
		if (selectedQuest != null)
		{
			selectedQuest.shutDown();
			if (shouldUpdateList)
			{
				updateQuestList();
			}
			if (plugin.getBankTagsMain() != null)
			{
				plugin.getBankTagsMain().shutDown();
			}
			removeQuest();
			plugin.getEventBus().unregister(selectedQuest);
			selectedQuest = null;
		}
	}


	private void updateQuests(List<QuestHelper> quests, GameState state, Map<QuestHelperQuest, QuestState> questStates)
	{
		 SwingUtilities.invokeLater(() -> questContainers.forEach(qc -> qc.updateQuestPanels(quests, state, questStates)));
	}

	private void updateRequirements(Client client, BankItems bankItems)
	{
		clientThread.invoke(() -> requirementContainers.forEach(rc -> rc.updateRequirements(client, bankItems)));
	}

	public GameState getClientGameState()
	{
		return this.currentClientGameState;
	}

	public final void setActiveDisplay(QuestScreen screen)
	{
		activeContainer.setScreen(screen);
	}

	public QuestScreen getCurrentScreen()
	{
		return activeContainer.getCurrentScreen();
	}

	public void startQuest(QuestHelper quest)
	{
		addQuest(quest, true);
		questSearchScreen.emptySearchBar();
	}

	public <T extends QuestScreen> T registerScreen(ScreenFactory<T> factory)
	{
		T screen = factory.apply(plugin, this);
		plugin.getEventBus().register(screen);
		screenFilters.entrySet()
			.stream()
			.filter(e -> e.getKey().test(screen))
			.forEach(e -> e.getValue().accept(screen));
		return screen;
	}

	/**********************
	 *
	 * Everything below here will be removed eventually
	 *
	 **********************
	 */

	public void addQuest(QuestHelper quest, boolean isActive)
	{
		log.debug("Quest added: " + quest.getQuest().getName() + " - Active: " + isActive);
		searchPanel.getAllDropdownSections().setVisible(false);
		setActiveDisplay(questOverviewPanel);

		questOverviewPanel.addQuest(quest, isActive);

		revalidate();
		repaint();
	}

	@Deprecated
	public void updateQuestsExternal(List<QuestHelper> quests, GameState state, Map<QuestHelperQuest, QuestState> questStates)
	{
		updateQuests(quests, state, questStates);
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
		SwingUtilities.invokeLater(() -> {
			searchPanel.getAllDropdownSections().setVisible(true);
			setActiveDisplay(questSearchScreen);
			questOverviewPanel.removeQuest();

			revalidate();
			repaint();
		});
	}

	public void updateItemRequirements(Client client, BankItems bankItems)
	{
		requirementContainers.forEach(rc -> rc.updateRequirements(client, bankItems));
	}

	@Nullable
	public QuestHelper getCurrentQuest()
	{
		return getQuestOverviewPanel().currentQuest;
	}
}
