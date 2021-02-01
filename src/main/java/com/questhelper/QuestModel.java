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
package com.questhelper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.questhelper.panel.PanelDetails;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.requirements.Requirement;
import com.questhelper.steps.QuestStep;
import com.questhelper.util.CachedClientObject;
import com.questhelper.util.ClientAction;
import com.questhelper.util.RequirementColorTask;
import java.awt.Color;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;

/**
 * QuestModel represents all the required information the UI will need about the currently selected quest.
 */
@Slf4j
public final class QuestModel
{

	private QuestHelper currentQuest;
	private final CachedClientObject<QuestState> currentQuestState;
	private final CachedClientObject<GameState> currentGameState;
	private final CachedClientObject<Integer> currentQuestVar;
	private final LoadingCache<Requirement, Color> colorCache;

	private final QuestHelperPlugin plugin;
	private final Client client;
	public QuestModel(QuestHelperPlugin plugin)
	{
		this.plugin = plugin;
		this.client = plugin.getClient();
		colorCache = CacheBuilder.newBuilder()
			.refreshAfterWrite(1, TimeUnit.SECONDS)
			.build(new CacheLoader<Requirement, Color>()
			{
				@Override
				public Color load(@Nonnull Requirement requirement)
				{
					// ensure we run on the client thread
					AtomicReference<Color> color = new AtomicReference<>(Color.RED);
					RequirementColorTask colorTask = new RequirementColorTask(plugin, requirement);
					ClientAction.invoke(plugin.getClientThread(), requirement, (req) -> {
						Color reqColor = colorTask.get();
						color.set(reqColor);
						return color.get();
					});
					return color.get();
				}
			});


		ClientThread thread = plugin.getClientThread();
		currentQuestState = new CachedClientObject<>(thread, () -> getCurrentQuest().getState(client));
		currentGameState = new CachedClientObject<>(thread, client::getGameState);
		currentQuestVar = new CachedClientObject<>(thread, () -> getCurrentQuest().getVar());
	}

	/**
	 * @return the currently active {@link QuestHelper}
	 */
	@Nullable
	public QuestHelper getCurrentQuest()
	{
		return currentQuest;
	}

	/**
	 * Set the supplied {@link QuestHelper} as active.
	 * <br>
	 * This will invalidate the current cache and cached objects and reload them
	 * using the new quest.
	 *
	 * @param quest the new quest
	 */
	public void setCurrentQuest(QuestHelper quest)
	{
		if (getCurrentQuest() != null) {
			invalidateAll(); // make sure we remove all old data so we don't mix requirements
		}
		this.currentQuest = quest;
		colorCache.invalidateAll(); // make sure there is nothing in the cache
		plugin.getClientThread().invoke(() -> {
			currentQuest.getGeneralRequirements().forEach(req -> colorCache.put(req, req.getColor(client)));
			currentQuest.getGeneralRecommended().forEach(req -> colorCache.put(req, req.getColor(client)));
			currentQuest.getItemRequirements().forEach(req -> colorCache.put(req, req.getColor(client)));
			currentQuest.getItemRecommended().forEach(req -> colorCache.put(req, req.getColor(client)));
			// add panel details requirements
			currentQuest.getPanels()
				.stream()
				.map(PanelDetails::getRequirements)
				.flatMap(Collection::stream)
				.forEach(req -> colorCache.put(req, req.getColor(client)));
		});
	}

	/**
	 * Invalidate all cached objects and set the current quest to null.
	 */
	public void invalidateAll()
	{
		currentQuest = null;
		colorCache.invalidateAll();
		currentQuestState.invalidate();
		currentGameState.invalidate();
		currentQuestVar.invalidate();
	}

	/**
	 * Get the currently cached {@link Color} for the given {@link Requirement}.
	 * <br>
	 * If the cache does not contain the given requirement, or the cache value is stale,
	 * the cache will recalculate the value and store it.
	 *
	 * @param requirement the requirement to check
	 * @return the {@link Color} for that {@link Requirement}
	 */
	@Nonnull
	public Color getColorForRequirement(Requirement requirement)
	{
		try
		{
			return colorCache.get(requirement);
		}
		catch (ExecutionException e)
		{
			log.error("Error retrieving Requirement color", e);
		}
		return requirement.getDefaultColor();
	}

	/**
	 * @return the current {@link QuestStep}
	 */
	@Nullable
	public QuestStep getCurrentStep()
	{
		if (getCurrentQuest() == null)
		{
			throw new IllegalStateException("Cannot retrieve the quest state without an active quest.");
		}
		return currentQuest.getCurrentStep();
	}

	/**
	 * @return the current {@link QuestState} for the currently active quest, or {@link QuestState}
	 */
	@Nonnull
	public QuestState getQuestState()
	{
		if (getCurrentQuest() == null)
		{
			throw new IllegalStateException("Cannot retrieve the quest state without an active quest.");
		}
		return currentQuestState.get();
	}

	@Nonnull
	public GameState getClientGameState()
	{
		return currentGameState.get();
	}

	public int getQuestVar()
	{
		if (getCurrentQuest() == null)
		{
			throw new IllegalStateException("Cannot retrieve the quest state without an active quest.");
		}
		return currentQuestVar.get();
	}
}
