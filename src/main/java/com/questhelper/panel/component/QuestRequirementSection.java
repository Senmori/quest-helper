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

import com.questhelper.BankItems;
import com.questhelper.StreamUtil;
import com.questhelper.requirements.Requirement;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * This panel holds all requirements of a specific type
 * (i.e. item requirements, general requirements, general recommended, etc)
 */
@Slf4j
@Getter
public class QuestRequirementSection implements RequirementContainer
{
	private final List<QuestRequirementPanel> questRequirementPanels = new LinkedList<>();

	private final String title;
	private final JPanel listPanel;

	public QuestRequirementSection(String title)
	{
		this.title = title;

		listPanel = new JPanel();
		listPanel.setLayout(new DynamicGridLayout(0, 1, 0, 1));
		listPanel.setBorder(new EmptyBorder(10, 5, 10, 5));
	}

	public void addOrUpdateRequirements(Collection<? extends Requirement> requirements)
	{
		if (requirements != null)
		{
			if (!requirements.isEmpty())
			{
				questRequirementPanels.forEach(listPanel::remove);
				questRequirementPanels.clear();
			}
			requirements.stream()
				.map(QuestRequirementPanel::new)
				.forEach(questRequirementPanels::add);

			log.debug("Found " + questRequirementPanels.size() + " Requirement(s) for " + getTitle());
		}
	}

	public JPanel getPanel()
	{
		JPanel panel = new JPanel();
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		panel.setLayout(new BorderLayout());
		panel.setBorder(new EmptyBorder(0, 0, 0, 0));

		JPanel headerPanel = new JPanel();
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setLayout(new BorderLayout());
		headerPanel.setBorder(new EmptyBorder(5, 5, 5, 10));

		JLabel tLabel = new JLabel();
		tLabel.setForeground(Color.WHITE);
		tLabel.setText(title);
		tLabel.setMinimumSize(new Dimension(1, headerPanel.getPreferredSize().height));
		headerPanel.add(tLabel, BorderLayout.NORTH);

		panel.add(headerPanel, BorderLayout.NORTH);
		panel.add(getListPanel(), BorderLayout.CENTER);
		return panel;
	}

	@Override
	public void updateRequirements(@Nonnull Client client, @Nonnull BankItems bankItems)
	{
		questRequirementPanels.forEach(panel -> panel.updateRequirements(client, bankItems));
	}

	@Nonnull
	@Override
	public List<Requirement> getRequirements()
	{
		return StreamUtil.getRequirements(questRequirementPanels);
	}
}
