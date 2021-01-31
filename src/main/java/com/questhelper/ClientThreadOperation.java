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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;

/**
 * Utility class that is used for running operations on the {@link ClientThread}
 */
@Slf4j
public final class ClientThreadOperation
{
	/**
	 * Get the var of a quest while off the client thread.
	 * <br>
	 * This method swallows exceptions.
	 *
	 * @param quest the quest to query
	 * @return the current var of the quest, or {@link Integer#MIN_VALUE} if there was a problem.
	 */
	public static synchronized int getQuestVar(QuestHelperPlugin plugin, QuestHelperQuest quest)
	{
		return runOnClientThread(plugin, Integer.MIN_VALUE, quest::getVar);
	}

	@Nonnull
	public static synchronized <T> T runOnClientThread(@Nonnull QuestHelperPlugin plugin, @Nonnull T defaultValue, @Nonnull Function<Client, T> func)
	{
		FutureTask<T> task = new FutureTask<>(() -> func.apply(plugin.getClient()));
		plugin.getClientThread().invoke(task);
		T var = null;
		try
		{
			var = task.get();
		}
		catch (InterruptedException | ExecutionException e)
		{
			log.error("Error running operation on client thread. Called from " + Thread.currentThread().getStackTrace()[1].getClassName() + ".", e);
			return defaultValue;
		}
		if (var == null) {
			var = defaultValue;
		}
		return var;
	}

	/**
	 * Get the current {@link GameState} of the {@link Client}.
	 * <br>
	 * This will always be run on the client thread
	 *
	 * @param plugin the QuestHelper plugin
	 * @param defaultValue default value in case something goes wrong
	 * @return the current {@link GameState}
	 */
	@Nonnull
	public static synchronized GameState getGameState(QuestHelperPlugin plugin, GameState defaultValue)
	{
		return runOnClientThread(plugin, defaultValue, Client::getGameState);
	}

	@Nonnull
	public static synchronized QuestState getQuestState(QuestHelperPlugin plugin, QuestHelperQuest quest)
	{
		return runOnClientThread(plugin, QuestState.NOT_STARTED, quest::getState);
	}
}
