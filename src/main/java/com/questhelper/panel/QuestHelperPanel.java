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
import com.questhelper.QuestController;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestHelperQuest;
import com.questhelper.QuestModel;
import com.questhelper.panel.component.SearchPanel;
import com.questhelper.panel.component.TitlePanel;
import com.questhelper.panel.screen.FixedWidthPanel;
import com.questhelper.panel.screen.QuestScreen;
import com.questhelper.panel.screen.QuestSearchScreen;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.steps.QuestStep;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.Text;

@Slf4j
public class QuestHelperPanel extends PluginPanel
{

	private final FixedWidthPanel questOverviewWrapper = new FixedWidthPanel();

	private final JPanel allQuestsCompletedPanel = new JPanel();

	private final JPanel allDropdownSections = new JPanel();

	private final IconTextField searchBar = new IconTextField();
	private final FixedWidthPanel questListPanel = new FixedWidthPanel();
	private final FixedWidthPanel questListWrapper = new FixedWidthPanel();


	private final ArrayList<QuestSelectPanel> questSelectPanels = new ArrayList<>();

	private final QuestHelperPlugin questHelperPlugin;

	/*
	 * NEW FIELDS BELOW THIS
	 */
	private final TitlePanel titlePanel = new TitlePanel("Quest Helper");
	@Getter
	private final SearchPanel searchPanel;
	private final ActiveContainer activeContainer;

	// Screens
	@Getter
	private final QuestSearchScreen questSearchScreen;
	@Getter
	private final QuestOverviewPanel questOverviewPanel;

	private final QuestModel questModel;
	private final QuestController questController;

	public QuestHelperPanel(QuestHelperPlugin plugin, QuestModel questModel, QuestController questController)
	{
		super(false);
		this.questHelperPlugin = plugin;
		this.questController = questController;
		this.questModel = questModel;

		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		this.searchPanel = new SearchPanel(plugin);

		JPanel introDetailsPanel = new JPanel();
		introDetailsPanel.setLayout(new BorderLayout());
		introDetailsPanel.add(titlePanel, BorderLayout.NORTH);
		introDetailsPanel.add(searchPanel, BorderLayout.CENTER);

		add(introDetailsPanel, BorderLayout.NORTH);

		/* Layout */
		this.questOverviewPanel = registerScreen(new QuestOverviewPanel(plugin, this, questModel, questController));
		this.questSearchScreen = registerScreen(new QuestSearchScreen(plugin, this, questModel, questController));
		this.activeContainer = new ActiveContainer(plugin, questSearchScreen);

		add(activeContainer, BorderLayout.CENTER);
		setActiveDisplay(questSearchScreen);
		questSearchScreen.update();
	}

	public final void setActiveDisplay(QuestScreen screen)
	{
		activeContainer.setScreen(screen);
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
		questSearchScreen.updateQuestList();
	}

	public void addQuest(QuestHelper quest, boolean isActive)
	{
		searchPanel.getAllDropdownSections().setVisible(false);
		setActiveDisplay(questOverviewPanel);
		questOverviewPanel.addQuest(quest, isActive);

		revalidate();
		repaint();
	}

	public void updateSteps()
	{
		questOverviewPanel.updateSteps();
	}

	public void updateHighlight(QuestStep newStep)
	{
		questOverviewPanel.updateHighlight(newStep);

		revalidate();
		repaint();
	}

	public void updateLocks()
	{
		questOverviewPanel.updateLocks();

		revalidate();
		repaint();
	}

	public void removeQuest()
	{
		searchPanel.getAllDropdownSections().setVisible(true);
		setActiveDisplay(questSearchScreen);
		questOverviewPanel.removeQuest();

		revalidate();
		repaint();
	}

	public void emptyBar()
	{
		searchBar.setText("");
	}

	public void updateItemRequirements(Client client, BankItems bankItems)
	{
		questOverviewPanel.updateRequirements(client, bankItems);
	}

	public <T extends QuestScreen> T registerScreen(T screen)
	{
		questHelperPlugin.getEventBus().register(screen);
		//TODO: Screen Filters
		return screen;
	}
}
