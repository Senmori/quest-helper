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

package com.questhelper.panel.panels;

import com.questhelper.BankItems;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.QuestHelperPanel;
import com.questhelper.panel.TextUtil;
import com.questhelper.panel.component.QuestSelectPanel;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.panel.component.TitlePanel;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.steps.QuestStep;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.util.SwingUtil;
import net.runelite.client.util.Text;

@Slf4j
/**
 * Panel that is displayed to the user when Quest Helper first starts.
 * It is also the default panel, so it should be displayed when no other behavior is wanted
 * and/or found.
 */
public class QuestSearchScreen extends QuestScreen
{
	// Quest Guide components
	private final QuestOverviewPanel questOverviewScreen;
	private final FixedWidthPanel questOverviewWrapper = new FixedWidthPanel();

	// Quest selection components
	private final FixedWidthPanel questListPanel = new FixedWidthPanel();
	private final FixedWidthPanel questListWrapper = new FixedWidthPanel();

	// how we view everything
	private final JScrollPane scrollableContainer;

	// search (including dropdowns)
	private final SearchPanel searchPanel;

	// all the panels that are shown to the player
	private final ArrayList<QuestSelectPanel> questSelectPanels = new ArrayList<>();

	public QuestSearchScreen(QuestHelperPlugin plugin, QuestHelperPanel rootPanel)
	{
		super(plugin, rootPanel);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		TitlePanel titlePanel = new TitlePanel("Quest Helper");
		searchPanel = new SearchPanel(plugin, e -> onSearchBarChanged());

		JPanel introDetailsPanel = new JPanel();
		introDetailsPanel.setLayout(new BorderLayout());
		introDetailsPanel.add(titlePanel, BorderLayout.NORTH);
		introDetailsPanel.add(searchPanel, BorderLayout.SOUTH);

		add(introDetailsPanel, BorderLayout.NORTH);

		// Quest List
		questListPanel.setBorder(new EmptyBorder(8, 10, 0, 10));
		questListPanel.setLayout(new DynamicGridLayout(0, 1, 0, 5));
		questListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		showMatchingQuests("");

		questListWrapper.setLayout(new BorderLayout());
		questListWrapper.add(questListPanel, BorderLayout.NORTH);

		scrollableContainer = new JScrollPane(questListWrapper);
		scrollableContainer.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		add(scrollableContainer, BorderLayout.CENTER);

		questOverviewScreen = new QuestOverviewPanel(plugin, rootPanel);
		questOverviewWrapper.setLayout(new BorderLayout());
		questOverviewWrapper.add(questOverviewScreen, BorderLayout.NORTH);
	}

	@Override
	public void update(@Nonnull Client client, @Nonnull ClientThread clientThread)
	{
		questOverviewScreen.update(client, clientThread);
	}

	@Override
	public void updateRequirements(@Nonnull Client client, @Nonnull BankItems bankItems)
	{
		questOverviewScreen.updateRequirements(client, bankItems);
	}

	private void onSearchBarChanged()
	{
		final String text = searchPanel.getText();

		if (questOverviewScreen.currentQuest == null || !text.isEmpty())
		{
			// search for quests
			scrollableContainer.setViewportView(questListWrapper);
			questSelectPanels.forEach(questListPanel::remove); // remove existing panels
			showMatchingQuests(text);
		}
		else
		{
			scrollableContainer.setViewportView(questOverviewWrapper);
		}
		revalidate();
	}

	private void showScreen(Component view, QuestScreen screen)
	{
		scrollableContainer.setViewportView(view);
		getRootPanel().setActiveScreen(screen);
	}

	private void showMatchingQuests(String text)
	{
		if (text == null || text.isEmpty())
		{
			questSelectPanels.forEach(questListPanel::add);
			return;
		}
		final String[] searchTerms = text.toLowerCase(Locale.ROOT).split(" ");
		questSelectPanels.forEach(listItem -> {
			if (Text.matchesSearchTerms(Arrays.asList(searchTerms), listItem.getKeywords()))
			{
				questListPanel.add(listItem);
			}
		});
	}

	@Override
	public void updateQuests(List<QuestHelper> questHelpers, boolean loggedOut, Map<QuestHelperQuest, QuestState> completedQuests)
	{
		questSelectPanels.forEach(questListPanel::remove);
		questSelectPanels.clear();

		searchPanel.getFilterDropdown().setSelectedItem(getPlugin().getConfig().filterListBy());
		searchPanel.getDifficultyDropdown().setSelectedItem(getPlugin().getConfig().difficulty());
		searchPanel.getOrderDropdown().setSelectedItem(getPlugin().getConfig().orderListBy());

		for (QuestHelper questHelper : questHelpers)
		{
			QuestState questState = completedQuests.getOrDefault(questHelper.getQuest(), QuestState.NOT_STARTED);
			questSelectPanels.add(new QuestSelectPanel(getPlugin(), getRootPanel(), questHelper, questState));
		}

		Set<QuestHelperQuest> quests = completedQuests.keySet();
		boolean hasMoreQuests = quests.stream().anyMatch(q -> completedQuests.get(q) != QuestState.FINISHED);
		if (questSelectPanels.isEmpty() && hasMoreQuests)
		{
			SwingUtil.fastRemoveAll(searchPanel.getAllQuestsCompletedPanel()); // apparently this is faster?
			JLabel noMatch = new JLabel();
			noMatch.setForeground(Color.GRAY);
			if (loggedOut)
			{
				noMatch.setText(TextUtil.alignLeft("Log in to see available quests."));
			}
			else
			{
				noMatch.setText(TextUtil.alignLeft("No quests are available that match your current filters."));
			}
			searchPanel.getAllQuestsCompletedPanel().add(noMatch);
		}
		searchPanel.getAllQuestsCompletedPanel().setVisible(questSelectPanels.isEmpty());
		revalidate(); //javax does it revalidate, then repaint
		repaint();
		showMatchingQuests(searchPanel.hasText() ? searchPanel.getText() : "");
	}

	public void addQuest(QuestHelper quest, boolean isActive)
	{
		searchPanel.getAllDropdownSections().setVisible(false);
		scrollableContainer.setViewportView(questOverviewWrapper);

		questOverviewScreen.addQuest(quest, isActive); //TODO: Change name of this method
		revalidate();
		repaint();
	}

	public void updateSteps()
	{
		questOverviewScreen.updateSteps();
	}

	public void updateHighlight(QuestStep newStep)
	{
		questOverviewScreen.updateHighlight(newStep);
		revalidate();
		repaint();
	}

	public void updateLocks()
	{
		questOverviewScreen.updateLocks();

		revalidate();
		repaint();
	}

	public void removeQuest()
	{
		searchPanel.getAllDropdownSections().setVisible(true);
		scrollableContainer.setViewportView(questListWrapper);
		questOverviewScreen.removeQuest();
		revalidate();
		repaint();
	}

	public void emptyBar()
	{
		searchPanel.setText("");
	}
}
