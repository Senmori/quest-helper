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
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.panel.component.TitlePanel;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.steps.QuestStep;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.Text;

@Slf4j
public class QuestHelperPanel extends PluginPanel
{
	private final QuestOverviewPanel questOverviewPanel;
	private final FixedWidthPanel questOverviewWrapper = new FixedWidthPanel();

	private final IconTextField searchBar = new IconTextField();
	private final FixedWidthPanel questListPanel = new FixedWidthPanel();
	private final FixedWidthPanel questListWrapper = new FixedWidthPanel();
	private final JScrollPane scrollableContainer;
	private final int DROPDOWN_HEIGHT = 20;


	// panels to start a quest
	private final ArrayList<QuestSelectPanel> questSelectPanels = new ArrayList<>();

	QuestHelperPlugin questHelperPlugin;

	// ui-upgrade fields
	private final SearchPanel searchPanel;

	public QuestHelperPanel(QuestHelperPlugin questHelperPlugin)
	{
		super(false);

		this.questHelperPlugin = questHelperPlugin;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		/* Setup overview panel */
		TitlePanel titlePanel = new TitlePanel("Quest Helper");
		searchPanel = new SearchPanel(questHelperPlugin, txt -> onSearchBarChanged());

		// Quest List
		questListPanel.setBorder(new EmptyBorder(8, 10, 0, 10));
		questListPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		questListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		showMatchingQuests("");

		questListWrapper.setLayout(new BorderLayout());
		questListWrapper.add(questListPanel, BorderLayout.NORTH);

		scrollableContainer = new JScrollPane(questListWrapper);
		scrollableContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JPanel introDetailsPanel = new JPanel();
		introDetailsPanel.setLayout(new BorderLayout());
		introDetailsPanel.add(titlePanel, BorderLayout.NORTH);
		introDetailsPanel.add(searchPanel, BorderLayout.CENTER);

		add(introDetailsPanel, BorderLayout.NORTH);
		add(scrollableContainer, BorderLayout.CENTER);

		/* Layout */
		questOverviewPanel = new QuestOverviewPanel(questHelperPlugin);

		questOverviewWrapper.setLayout(new BorderLayout());
		questOverviewWrapper.add(questOverviewPanel, BorderLayout.NORTH);
	}

	private void onSearchBarChanged()
	{
		final String text = searchBar.getText();

		if ((questOverviewPanel.currentQuest == null || !text.isEmpty()))
		{
			scrollableContainer.setViewportView(questListWrapper);
			questSelectPanels.forEach(questListPanel::remove);
			showMatchingQuests(text);
		}
		else
		{
			scrollableContainer.setViewportView(questOverviewWrapper);
		}
		revalidate();
	}


	private void showMatchingQuests(String text)
	{
		if (text.isEmpty())
		{
			questSelectPanels.forEach(questListPanel::add);
			return;
		}

		final String[] searchTerms = text.toLowerCase().split(" ");

		questSelectPanels.forEach(listItem ->
		{
			if (Text.matchesSearchTerms(Arrays.asList(searchTerms), listItem.getKeywords()))
			{
				questListPanel.add(listItem);
			}
		});
	}

	public void refresh(List<QuestHelper> questHelpers, boolean loggedOut, Map<QuestHelperQuest, QuestState> completedQuests)
	{
		questSelectPanels.forEach(questListPanel::remove);
		questSelectPanels.clear();

		searchPanel.getFilterDropdown().setSelectedItem(questHelperPlugin.getConfig().filterListBy());
		searchPanel.getDifficultyDropdown().setSelectedItem(questHelperPlugin.getConfig().difficulty());
		searchPanel.getOrderDropdown().setSelectedItem(questHelperPlugin.getConfig().orderListBy());

		for (QuestHelper questHelper : questHelpers)
		{
			QuestState questState = completedQuests.getOrDefault(questHelper.getQuest(), QuestState.NOT_STARTED);
			questSelectPanels.add(new QuestSelectPanel(questHelperPlugin, this, questHelper, questState));
		}


		Set<QuestHelperQuest> quests = completedQuests.keySet();
		boolean hasMoreQuests = quests.stream().anyMatch(q -> completedQuests.get(q) != QuestState.FINISHED);
		if (questSelectPanels.isEmpty() && hasMoreQuests)
		{
			searchPanel.getAllQuestsCompletedPanel().removeAll();
			JLabel noMatch = new JLabel();
			noMatch.setForeground(Color.GRAY);
			if (loggedOut)
			{
				noMatch.setText("<html><body style='text-align:left'>Log in to see available quests</body></html>");
			}
			else
			{
				noMatch.setText("<html><body style='text-align:left'>No quests are available that match your current filters</body></html>");
			}
			searchPanel.getAllQuestsCompletedPanel().add(noMatch);
		}
		searchPanel.getAllQuestsCompletedPanel().setVisible(questSelectPanels.isEmpty());

		repaint();
		revalidate();
		showMatchingQuests(searchBar.getText() != null ? searchBar.getText() : "");
	}

	public void addQuest(QuestHelper quest, boolean isActive)
	{
		searchPanel.getAllDropdownSections().setVisible(false);
		scrollableContainer.setViewportView(questOverviewWrapper);

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
		scrollableContainer.setViewportView(questListWrapper);
		questOverviewPanel.removeQuest();

		repaint();
		revalidate();
	}

	public void emptyBar()
	{
		searchBar.setText("");
	}

	public void updateItemRequirements(Client client, BankItems bankItems)
	{
		questOverviewPanel.updateRequirements(client, bankItems);
	}
}
