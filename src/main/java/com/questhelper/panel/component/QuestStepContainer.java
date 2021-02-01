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

import com.questhelper.QuestController;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.QuestModel;
import com.questhelper.panel.PanelDetails;
import com.questhelper.panel.QuestHelperPanel;
import com.questhelper.panel.QuestStepPanel;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.steps.QuestStep;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class QuestStepContainer extends JPanel
{
	private final QuestHelperPlugin plugin;
	private final QuestHelperPanel view;
	private final QuestModel model;
	private final QuestController controller;

	private final List<QuestStepPanel> questStepPanelList = new LinkedList<>();
	public QuestStepContainer(QuestHelperPlugin plugin, QuestHelperPanel view, QuestModel model, QuestController controller)
	{
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.plugin = plugin;
		this.model = model;
		this.view = view;
		this.controller = controller;
	}

	public void createQuestStepPanels(Container parent, BiConsumer<QuestStepPanel, MouseEvent> mouseListener)
	{
		QuestHelper currentQuest = model.getCurrentQuest();
		if (currentQuest != null)
		{
			QuestStep currentStep = currentQuest.getCurrentStep();
			if (currentStep == null)
			{
				return; // no panels for this step
			}
			List<PanelDetails> panels = currentQuest.getPanels();
			for (PanelDetails panel : panels)
			{
				QuestStepPanel newStep = new QuestStepPanel(panel, currentStep);
				boolean hasLockingSteps = panel.getLockingQuestSteps() != null;
				int var = model.getQuestVar();
				boolean hasQuestVar = panel.getVars() == null || panel.getVars().contains(var);
				if (hasLockingSteps && hasQuestVar)
				{
					newStep.setLockable(true);
				}
				questStepPanelList.add(newStep);
				add(newStep);
				newStep.addMouseListener(mouseListener);
				parent.revalidate();
				parent.repaint(); // TODO: Do we need to call repaint?
			}
		}
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		questStepPanelList.forEach(panel -> panel.setVisible(visible));
	}

	public void updateHighlight(QuestStep newStep, QuestHelper currentQuest)
	{
		AtomicBoolean highlighted = new AtomicBoolean(false);
		questStepPanelList.forEach(panel -> {
			highlighted.set(false);
			boolean hasLockingSteps = panel.getPanelDetails().getLockingQuestSteps() != null;
			boolean questHasVar = currentQuest != null && panel.getPanelDetails().getVars().contains(currentQuest.getVar());
			boolean hasValidVar = panel.getPanelDetails().getVars() == null || questHasVar;
			panel.setLockable(hasLockingSteps && hasValidVar);

			panel.getSteps()
				.stream()
				.filter(p -> p == newStep || p.getSubsteps().contains(newStep))
				.findFirst()
				.ifPresent(step -> {
					panel.updateHighlight(step);
					highlighted.set(true);
				});
			if (!highlighted.get())
			{
				panel.removeHighlight();
			}
		});
	}

	public void updateSteps()
	{
		if (questStepPanelList.isEmpty())
		{
			return;
		}
		questStepPanelList.forEach(panel -> {
			for (QuestStep step : panel.getSteps())
			{
				JLabel label = panel.getStepsLabels().get(step);
				if (label != null)
				{
					label.setText(panel.generateText(step));
				}
			}
		});
	}

	public void updateLocks()
	{
		questStepPanelList.forEach(QuestStepPanel::updateLock);
	}

	@Override
	public void removeAll()
	{
		super.removeAll();
		questStepPanelList.clear();
	}

	public boolean isAllCollapsed()
	{
		return questStepPanelList.stream().allMatch(QuestStepPanel::isCollapsed);
	}
}
