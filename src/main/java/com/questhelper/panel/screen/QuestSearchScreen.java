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

import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.QuestHelperPanel;
import com.questhelper.panel.QuestSelectPanel;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.questhelpers.QuestHelper;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.util.Text;

@Slf4j
public class QuestSearchScreen extends QuestScreen
{
	@Getter
	private List<QuestSelectPanel> questSelectPanels = new ArrayList<>();

	private final SearchPanel searchPanel;

	public QuestSearchScreen(QuestHelperPlugin plugin, QuestHelperPanel rootPanel, SearchPanel searchPanel)
	{
		super(plugin, rootPanel);
		this.searchPanel = searchPanel;
		searchPanel.setSearchBarDocumentListener(searchBar -> updateSearchFilter(searchBar.getText()));
		setBorder(new EmptyBorder(8, 10, 0, 10));
		setLayout(new DynamicGridLayout(0, 1, 0, 5));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
	}

	public void emptySearchBar()
	{
		searchPanel.setText("");
	}

	public void updateSearchFilter(String filter)
	{
		QuestHelper currentQuest = getRootPanel().getQuestOverviewPanel().currentQuest;
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

	@Override
	public void updateQuests(List<QuestHelper> questHelpers, GameState gameState, Map<QuestHelperQuest, QuestState> completedQuests)
	{
		log.debug("QUEST SEARCH SCREEN UPDATE QUESTS");
		searchPanel.getFilterDropdown().setSelectedItem(getPlugin().getConfig().filterListBy());
		searchPanel.getDifficultyDropdown().setSelectedItem(getPlugin().getConfig().difficulty());
		searchPanel.getOrderDropdown().setSelectedItem(getPlugin().getConfig().orderListBy());

		questSelectPanels.forEach(this::remove);
		questSelectPanels.clear();
		for (QuestHelper questHelper : questHelpers)
		{
			QuestState questState = completedQuests.getOrDefault(questHelper.getQuest(),QuestState.NOT_STARTED);
			questSelectPanels.add(new QuestSelectPanel(getPlugin(), getRootPanel(), questHelper, questState));
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
