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
package com.questhelper;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.questhelper.questhelpers.Quest;
import com.questhelper.questhelpers.QuestHelper;
import com.questhelper.util.CachedClientObject;
import com.questhelper.util.ClientAction;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;

/*
 * talks to model and view
 * contains events that are used to update the view/model
 * contains the master quest list
 * contains all variables the view might need so there is no direct coupling
 * between the view and QuestHelperPlugin
 */
@Slf4j
public class QuestController
{

	private final QuestHelperPlugin plugin;
	private final Map<String, QuestHelper> quests;
	private final CachedClientObject<List<QuestHelper>> filteredQuests;
	private final Function<QuestHelper, Boolean> showCompletedQuest;
	private final LoadingCache<QuestHelperQuest, QuestState> questStateCache;
	public QuestController(QuestHelperPlugin plugin, Map<String, QuestHelper> quests)
	{
		this.plugin = plugin;
		this.quests = quests;
		ClientThread thread = plugin.getClientThread();
		QuestHelperConfig config = plugin.getConfig();

		showCompletedQuest = q -> q.getConfig().showCompletedQuests() && q.isCompleted() || !q.isCompleted();

		Callable<List<QuestHelper>> filteredQuestsCallable = () -> {
			if (plugin.getClient().getGameState() != GameState.LOGGED_IN) {
				return Lists.newArrayList(); // return empty list
			}
			return getRegisteredQuests()
				.stream()
				.filter(config.filterListBy())
				.filter(config.difficulty())
				.filter(showCompletedQuest::apply)
				.sorted(config.orderListBy())
				.collect(Collectors.toList());
		};
		filteredQuests = new CachedClientObject<>(thread, filteredQuestsCallable);
		questStateCache = CacheBuilder.newBuilder()
			.refreshAfterWrite(1, TimeUnit.SECONDS)
			.maximumSize(QuestHelperQuest.values().length)
			.build(new CacheLoader<QuestHelperQuest, QuestState>()
			{
				@Override
				public QuestState load(@Nonnull QuestHelperQuest quest)
				{
					AtomicReference<QuestState> state = new AtomicReference<>(null);
					FutureTask<QuestState> task = new FutureTask<>(() -> quest.getState(plugin.getClient()));
					thread.invoke(() -> {
						try
						{
							state.set(task.get());
						}
						catch (InterruptedException | ExecutionException e)
						{
							log.error("Error retrieving quest state for quest " + quest.name(), e);
							state.set(QuestState.NOT_STARTED);
						}
					});

					return state.get();
				}
			});
		loadQuests();
	}

	private void loadQuests()
	{
		// add quests to cache
		ClientAction<QuestHelperQuest, QuestState> getState = new ClientAction<>(plugin.getClientThread(), q -> q.getState(plugin.getClient()));
		Stream.of(QuestHelperQuest.values()).forEach(quest -> questStateCache.put(quest, Objects.requireNonNull(getState.get(quest))));
	}

	/**
	 * @return an immutable list containing all quests
	 */
	@Nonnull
	public List<QuestHelper> getRegisteredQuests()
	{
		return ImmutableList.copyOf(quests.values());
	}

	/**
	 * @return a list containing all quests that match the current filters
	 *
	 * @see Quest.Difficulty
	 * @see Quest.Type
	 * @see QuestHelperConfig.QuestFilter
	 * @see QuestHelperConfig.QuestOrdering
	 */
	@Nonnull
	public List<QuestHelper> getFilteredQuests()
	{
		return filteredQuests.get();
	}

	/**
	 * Get the {@link QuestState} for each {@link QuestHelper} supplied.
	 *
	 * @param quests the quests to get the state
	 * @return an immutable map containing the quest and it's current {@link QuestState}
	 */
	@Nonnull
	public Map<QuestHelperQuest, QuestState> getStatesForQuests(Collection<QuestHelper> quests)
	{
		Collection<QuestHelperQuest> questHelperQuests = quests.stream().map(QuestHelper::getQuest).collect(Collectors.toList());
		Map<QuestHelperQuest, QuestState> map = ImmutableMap.of(); // default empty immutable map
		try
		{
			map = questStateCache.getAll(questHelperQuests);
		}
		catch (ExecutionException e)
		{
			log.error("Error retrieving quest states", e);
		}
		return map;
	}

	/**
	 * Get the current {@link QuestState} for a specified quest.
	 *
	 * @param quest the quest to get the state of
	 * @return the current {@link QuestState} or null if something wetn wrong.
	 */
	@Nullable
	public QuestState getQuestState(QuestHelperQuest quest)
	{
		try
		{
			return questStateCache.get(quest);
		}
		catch (ExecutionException e)
		{
			log.error("Error retrieving quest state for quest " + quest, e);
		}
		return null;
	}
}
