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

import com.questhelper.QuestHelperPlugin;
import com.questhelper.panel.DropdownRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.annotation.Nonnull;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import static net.runelite.client.ui.PluginPanel.PANEL_WIDTH;
import net.runelite.client.ui.components.IconTextField;

public class PanelUtil
{
	public static IconTextField createSearchBar(IconTextField.Icon icon)
	{
		IconTextField searchBar = new IconTextField();
		searchBar.setIcon(icon);
		searchBar.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, 30));
		searchBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		searchBar.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
		return searchBar;
	}

	public static <T extends Enum<?>> JComboBox<T> makeDropdownBox(@Nonnull T[] values, @Nonnull String key, @Nonnull QuestHelperPlugin plugin)
	{
		JComboBox<T> dropdown = new JComboBox<>(values);
		dropdown.setFocusable(false);
		dropdown.setForeground(Color.WHITE);
		dropdown.setRenderer(new DropdownRenderer());
		dropdown.addItemListener(makeItemListener(plugin, key));
		return dropdown;
	}

	public static JPanel makeDropdownPanel(JComboBox<?> dropdown, String name)
	{
		// Filters
		JLabel filterName = new JLabel(name);
		filterName.setForeground(Color.WHITE);

		JPanel filtersPanel = new JPanel();
		filtersPanel.setLayout(new BorderLayout());
		filtersPanel.setMinimumSize(new Dimension(PANEL_WIDTH, 0));
		filtersPanel.add(filterName, BorderLayout.CENTER);
		filtersPanel.add(dropdown, BorderLayout.EAST);

		return filtersPanel;
	}

	private static ItemListener makeItemListener(QuestHelperPlugin plugin, String key)
	{
		return e -> {
			if (e.getStateChange() == ItemEvent.SELECTED)
			{
				Enum source = (Enum) e.getItem();
				plugin.getConfigManager().setConfiguration("questhelper", key, source);
			}
		};
	}
}