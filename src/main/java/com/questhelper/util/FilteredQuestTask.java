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
package com.questhelper.util;

import com.google.common.collect.Lists;
import com.questhelper.QuestController;
import com.questhelper.QuestHelperConfig;
import com.questhelper.questhelpers.QuestHelper;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.api.GameState;

public class FilteredQuestTask implements Callable<List<QuestHelper>>
{
	private final Client client;
	private final QuestController controller;
	private final QuestHelperConfig config;
	private final Function<QuestHelper, Boolean> showCompletedQuest;
	public FilteredQuestTask(Client client, QuestController controller, QuestHelperConfig config)
	{
		this.client = client;
		this.controller = controller;
		this.config = config;
		showCompletedQuest = q -> q.getConfig().showCompletedQuests() && q.isCompleted() || !q.isCompleted();
	}

	@Override
	public List<QuestHelper> call()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return Lists.newArrayList();
		}
		if (controller.getRegisteredQuests().isEmpty()) {
			return Lists.newArrayList();
		}
		return controller.getRegisteredQuests()
			.stream()
			.filter(config.filterListBy())
			.filter(config.difficulty())
			.filter(showCompletedQuest::apply)
			.sorted(config.orderListBy())
			.collect(Collectors.toList());
	}
}
