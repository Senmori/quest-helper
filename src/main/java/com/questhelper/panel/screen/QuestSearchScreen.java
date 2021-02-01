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

import com.questhelper.QuestController;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.QuestModel;
import com.questhelper.panel.QuestHelperPanel;
import com.questhelper.panel.QuestSelectPanel;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.util.Utils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import lombok.Getter;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.client.util.Text;

public class QuestSearchScreen extends QuestScreen
{
	private final QuestHelperPlugin plugin;
	private final QuestHelperPanel rootPanel;
	private final QuestModel questModel;
	private final QuestController questController;
	private final SearchPanel searchPanel;

	@Getter
	private List<QuestSelectPanel> questSelectPanelList = new ArrayList<>();

	public QuestSearchScreen(QuestHelperPlugin plugin, QuestHelperPanel rootPanel, QuestModel model, QuestController controller)
	{
		super(plugin, rootPanel);
		this.plugin = plugin;
		this.searchPanel = rootPanel.getSearchPanel();
		this.rootPanel = rootPanel;
		this.questModel = model;
		this.questController = controller;
	}

	public void updateSearchPanel()
	{
		updateQuestList();
		updateSearchFilter(searchPanel.getText());
	}

	public void emptySearchBar()
	{
		searchPanel.setText("");
	}

	public void updateSearchFilter(String filter)
	{
		QuestHelper currentQuest = questModel.getCurrentQuest();
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

	public void updateQuestList()
	{
		List<QuestHelper> filteredQuests = questController.getFilteredQuests();
		Map<QuestHelperQuest, QuestState> questStates = questController.getStatesForQuests(filteredQuests);
		updateQuestPanels(filteredQuests, questModel.getClientGameState(), questStates);
	}

	public void updateQuestPanels(List<QuestHelper> questHelpers, GameState gameState, Map<QuestHelperQuest, QuestState> completedQuests)
	{
		searchPanel.getFilterDropdown().setSelectedItem(getPlugin().getConfig().filterListBy());
		searchPanel.getDifficultyDropdown().setSelectedItem(getPlugin().getConfig().difficulty());
		searchPanel.getOrderDropdown().setSelectedItem(getPlugin().getConfig().orderListBy());

		questSelectPanelList.forEach(this::remove);
		questSelectPanelList.clear();
		for (QuestHelper questHelper : questHelpers)
		{
			QuestState questState = completedQuests.getOrDefault(questHelper.getQuest(), QuestState.NOT_STARTED);
			questSelectPanelList.add(new QuestSelectPanel(getPlugin(), getRootPanel(), questHelper, questState));
		}

		Set<QuestHelperQuest> quests = completedQuests.keySet();
		boolean hasMoreQuests = quests.stream().anyMatch(q -> completedQuests.get(q) != QuestState.FINISHED);
		if (this.isEmpty() && hasMoreQuests)
		{
			searchPanel.getAllQuestsCompletedPanel().removeAll();
			JLabel noMatch = new JLabel();
			noMatch.setForeground(Color.GRAY);
			if (gameState != GameState.LOGGED_IN)
			{
				noMatch.setText(Utils.textAlignLeft("Log in to see available quests"));
			}
			else
			{
				noMatch.setText(Utils.textAlignLeft("No quests are available that match your current filters"));
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
		return questSelectPanelList.isEmpty();
	}

	public void removeAllQuests()
	{
		questSelectPanelList.forEach(this::remove);
	}

	public void addAllQuests()
	{
		questSelectPanelList.forEach(this::add);
	}

	public void addQuestsThatMatchSearchTerms(List<String> searchTerms)
	{
		questSelectPanelList
			.stream()
			.filter(panel -> Text.matchesSearchTerms(searchTerms, panel.getKeywords()))
			.forEach(this::add);
	}
}
