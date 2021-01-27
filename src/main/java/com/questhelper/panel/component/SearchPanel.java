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
package com.questhelper.panel.component;

import com.questhelper.QuestHelperConfig;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.panel.TextUtil;
import com.questhelper.questhelpers.Quest;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import static net.runelite.client.ui.PluginPanel.BORDER_OFFSET;
import static net.runelite.client.ui.PluginPanel.PANEL_WIDTH;
import net.runelite.client.ui.components.IconTextField;

/**
 * Component that handles the inputs for searching for quests.
 */
public class SearchPanel extends JPanel
{
	private static final int DROPDOWN_HEIGHT = 20;
	@Getter
	private final JPanel allQuestsCompletedPanel = new JPanel();
	@Getter
	private final JPanel allDropdownSections = new JPanel();
	@Getter
	private final JLabel questsCompletedLabel = new JLabel();

	@Getter
	private final IconTextField searchBar;

	@Getter
	private final JComboBox<Enum> filterDropdown, difficultyDropdown, orderDropdown;

	public SearchPanel(QuestHelperPlugin plugin, Consumer<IconTextField> onSearchBarChanged)
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout(0, BORDER_OFFSET));

		questsCompletedLabel.setForeground(Color.GRAY);
		questsCompletedLabel.setText(TextUtil.alignLeft(
			"Please log in to see available quests." +
				"Note that not all quests are available in the Quest Helper yet."));

		allQuestsCompletedPanel.setLayout(new BorderLayout());
		allQuestsCompletedPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		allQuestsCompletedPanel.add(questsCompletedLabel);
		allQuestsCompletedPanel.setVisible(false);

		searchBar = PanelUtil.createSearchBar(IconTextField.Icon.SEARCH, onSearchBarChanged);

		add(searchBar, BorderLayout.CENTER);
		add(allQuestsCompletedPanel, BorderLayout.SOUTH);

		filterDropdown = PanelUtil.makeDropdownBox(QuestHelperConfig.QuestFilter.values(),"filterListBy", plugin);
		JPanel filtersPanel = PanelUtil.makeDropdownPanel(filterDropdown, "Filters");
		filtersPanel.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

		difficultyDropdown = PanelUtil.makeDropdownBox(Quest.Difficulty.values(),"questDifficulty", plugin);
		JPanel difficultyPanel = PanelUtil.makeDropdownPanel(difficultyDropdown, "Difficulty");
		difficultyPanel.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

		orderDropdown = PanelUtil.makeDropdownBox(QuestHelperConfig.QuestOrdering.values(), "orderListBy", plugin);
		JPanel orderPanel = PanelUtil.makeDropdownPanel(orderDropdown, "Ordering");
		orderPanel.setPreferredSize(new Dimension(PANEL_WIDTH, DROPDOWN_HEIGHT));

		allDropdownSections.setBorder(new EmptyBorder(0, 0, 10, 0));
		allDropdownSections.setLayout(new BorderLayout(0, BORDER_OFFSET));
		allDropdownSections.add(filtersPanel, BorderLayout.NORTH);
		allDropdownSections.add(difficultyPanel, BorderLayout.CENTER);
		allDropdownSections.add(orderPanel, BorderLayout.SOUTH);


		add(allDropdownSections, BorderLayout.NORTH);
	}

	public String getText()
	{
		return searchBar.getText();
	}

	public boolean hasText()
	{
		return getText() != null && !getText().isEmpty();
	}

	public void setText(String text)
	{
		searchBar.setText(text);
	}

	public void updateDropdownLists(QuestHelperConfig config)
	{
		getFilterDropdown().setSelectedItem(config.filterListBy());
		getDifficultyDropdown().setSelectedItem(config.difficulty());
		getOrderDropdown().setSelectedItem(config.orderListBy());
	}
}