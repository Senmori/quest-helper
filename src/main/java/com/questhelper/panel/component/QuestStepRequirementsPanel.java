/*
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
import com.questhelper.panel.PanelDetails;
import com.questhelper.panel.QuestRequirementPanel;
import com.questhelper.requirements.Requirement;
import com.questhelper.requirements.RequirementContainer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * Represents a single Quest {@link Requirement}.
 * These are displayed both in the general quest requirements section
 * as well as in the individual quest step sections.
 */
public class QuestStepRequirementsPanel extends JPanel implements RequirementContainer
{
	private final JPanel headerPanel;
	private final JPanel listOfRequirements;
	private final JLabel title;

	@Getter
	private final List<QuestRequirementPanel> requirementPanels = new LinkedList<>();

	public QuestStepRequirementsPanel(PanelDetails panelDetails)
	{
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		headerPanel = createHeaderPanel();
		title = createTitle("Bring the following items:");

		headerPanel.add(title, BorderLayout.NORTH);

		listOfRequirements = createItemListPanel();

		List<Requirement> requirements = panelDetails.getRequirements();
		if (requirements != null && !requirements.isEmpty())
		{
			requirements.forEach(this::addRequirement);
		}

		add(headerPanel, BorderLayout.NORTH);
		add(listOfRequirements, BorderLayout.CENTER);
	}

	@Override
	public void updateRequirements(@Nonnull Client client, @Nonnull BankItems bankItems)
	{
		requirementPanels.forEach(panel -> panel.updateRequirements(client, bankItems));
	}

	@Nonnull
	@Override
	public List<Requirement> getRequirements()
	{
		return StreamUtil.getRequirements(requirementPanels);
	}

	private void addRequirement(Requirement requirement)
	{
		QuestRequirementPanel panel = new QuestRequirementPanel(requirement);
		requirementPanels.add(panel);
		listOfRequirements.add(panel);
	}

	/*
	 * HELPER METHODS
	 */

	//<editor-fold desc="Helper Methods">
	private JPanel createHeaderPanel()
	{
		JPanel header = new JPanel();
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setLayout(new BorderLayout());
		header.setBorder(new EmptyBorder(5, 5, 5, 10));
		return header;
	}

	private JPanel createItemListPanel()
	{
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new DynamicGridLayout(0, 1, 0, 1));
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		return contentPanel;
	}

	private JLabel createTitle(String text)
	{
		JLabel title = new JLabel();
		title.setForeground(Color.WHITE);
		title.setText(text);
		title.setMinimumSize(new Dimension(1, headerPanel.getPreferredSize().height));
		return title;
	}
	//</editor-fold>
}