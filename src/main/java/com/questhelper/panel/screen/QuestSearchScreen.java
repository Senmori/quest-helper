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
package com.questhelper.panel.screen;

import com.questhelper.ClientThreadOperation;
import com.questhelper.QuestHelperConfig;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.QuestContainer;
import com.questhelper.panel.QuestHelperPanel;
import com.questhelper.panel.QuestSelectPanel;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.panel.event.QuestChangedStatus;
import com.questhelper.panel.factory.PanelFactory;
import com.questhelper.panel.factory.QuestSelectPanelFactory;
import com.questhelper.questhelpers.Quest;
import com.questhelper.questhelpers.QuestHelper;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.util.Text;

@Slf4j
public class QuestSearchScreen extends QuestScreen implements QuestContainer
{
	@Getter
	private List<QuestSelectPanel> questSelectPanels = new ArrayList<>();

	private final SearchPanel searchPanel;

	private final PanelFactory<QuestSelectPanel> panelFactory;

	public QuestSearchScreen(QuestHelperPlugin plugin, QuestHelperPanel rootPanel)
	{
		super(plugin, rootPanel);
		this.searchPanel = rootPanel.getSearchPanel();
		this.panelFactory = new QuestSelectPanelFactory(rootPanel);
		searchPanel.setSearchBarDocumentListener(searchBar -> updateSearchFilter(searchBar.getText()));
		setBorder(new EmptyBorder(8, 10, 0, 10));
		setLayout(new DynamicGridLayout(0, 1, 0, 5));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		Timer timer = new Timer("Search Panel Timer");
		TimerTask task = new TimerTask()
		{
			@Override
			public void run()
			{
				updateSearchPanel();
			}
		};
		//timer.schedule(task, 50); // update search panel screen after 50ms
	}

	private void updateSearchPanel()
	{
		updateSearchFilter(searchPanel.getText());
		getRootPanel().updateQuestList();
	}

	@Subscribe
	public void onQuestChangedStatus(QuestChangedStatus event)
	{
		boolean visible = event.getStatus() != QuestChangedStatus.Status.START;
		searchPanel.getAllDropdownSections().setVisible(visible);
		log.debug("Search Panel Dropdown Section Visible: " + visible);
	}

	private final Collection<String> configEvents = Arrays.asList("orderListBy", "filterListBy", "questDifficulty", "showCompletedQuests");
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("questhelper") && configEvents.contains(event.getKey()))
		{
			getRootPanel().updateQuestList();
			log.debug("Search Screen Config Changed -> update quests");
		}
	}

	@Subscribe
	public void onGameStateChanged(final GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			getRootPanel().updateQuestList();
		}
	}

	public void emptySearchBar()
	{
		searchPanel.setText("");
	}

	public void updateSearchFilter(String filter)
	{
		QuestHelper currentQuest = getRootPanel().getCurrentQuest();
		if (currentQuest == null || (filter != null && !filter.isEmpty()) )
		{
			getRootPanel().setActiveDisplay(this);
			removeAllQuests();
			showMatchingQuests(filter);
		}
		else
		{
			getRootPanel().setActiveDisplay(getRootPanel().getQuestOverviewPanel());
		}
		revalidate();
	}

	public void showMatchingQuests(String filter)
	{
		if (filter.isEmpty())
		{
			addAllQuests();
			return;
		}

		final String[] searchTerms = filter.toLowerCase(Locale.ROOT).split(" ");

		addQuestsThatMatchSearchTerms(Arrays.asList(searchTerms));
	}

	public void updateQuestList(GameState state)
	{
		if (state == GameState.LOGGED_IN)
		{
			QuestHelperConfig config = getPlugin().getConfig();
			List<QuestHelper> filteredQuests = getPlugin().getQuests().values()
				.stream()
				.filter(config.filterListBy())
				.filter(config.difficulty())
				.filter(Quest::showCompletedQuests)
				.sorted(config.orderListBy())
				.collect(Collectors.toList());
			Map<QuestHelperQuest, QuestState> questStates = getPlugin().getQuests().values()
				.stream()
				.collect(Collectors.toMap(QuestHelper::getQuest, q -> ClientThreadOperation.getQuestState(getPlugin(), q.getQuest())));
			SwingUtilities.invokeLater(() -> {
				updateQuestPanels(filteredQuests, state, questStates);
			});
		}
		else
		{
			SwingUtilities.invokeLater(() -> updateQuestPanels(Collections.emptyList(), getRootPanel().getClientGameState(), new HashMap<>()));
		}
		updateSearchFilter(searchPanel.getText());
	}

	@Override
	public void updateQuestPanels(List<QuestHelper> questHelpers, GameState gameState, Map<QuestHelperQuest, QuestState> completedQuests)
	{
		searchPanel.getFilterDropdown().setSelectedItem(getPlugin().getConfig().filterListBy());
		searchPanel.getDifficultyDropdown().setSelectedItem(getPlugin().getConfig().difficulty());
		searchPanel.getOrderDropdown().setSelectedItem(getPlugin().getConfig().orderListBy());

		questSelectPanels.forEach(this::remove);
		questSelectPanels.clear();
		for (QuestHelper questHelper : questHelpers)
		{
			QuestState questState = completedQuests.getOrDefault(questHelper.getQuest(), QuestState.NOT_STARTED);
			questSelectPanels.add(panelFactory.build(questHelper, questState));
		}

		Set<QuestHelperQuest> quests = completedQuests.keySet();
		boolean hasMoreQuests = quests.stream().anyMatch(q -> completedQuests.get(q) != QuestState.FINISHED);
		if (isEmpty() && hasMoreQuests)
		{
			searchPanel.getAllQuestsCompletedPanel().removeAll();
			JLabel noMatch = new JLabel();
			noMatch.setForeground(Color.GRAY);
			if (gameState != GameState.LOGGED_IN)
			{
				noMatch.setText("<html><body style='text-align:left'>Log in to see available quests</body></html>");
			}
			else
			{
				noMatch.setText("<html><body style='text-align:left'>No quests are available that match your current filters</body></html>");
			}
			searchPanel.getAllQuestsCompletedPanel().add(noMatch);
		}
		searchPanel.getAllQuestsCompletedPanel().setVisible(isEmpty());

		revalidate();
		repaint();
		showMatchingQuests(searchPanel.hasText() ? searchPanel.getText() : "");
	}

	public boolean isEmpty()
	{
		return questSelectPanels.isEmpty();
	}

	public void removeAllQuests()
	{
		questSelectPanels.forEach(this::remove);
	}

	public void addAllQuests()
	{
		questSelectPanels.forEach(this::add);
	}

	public void addQuestsThatMatchSearchTerms(List<String> searchTerms)
	{
		questSelectPanels
			.stream()
			.filter(panel -> Text.matchesSearchTerms(searchTerms, panel.getKeywords()))
			.forEach(this::add);
	}
}
