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

import com.questhelper.QuestHelperQuest;
import com.questhelper.panel.panels.QuestScreen;
import com.questhelper.questhelpers.QuestHelper;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import net.runelite.api.Client;
import net.runelite.api.QuestState;
import net.runelite.client.callback.ClientThread;

public interface Updatable
{
	/**
	 * This method is called every Game Tick.
	 *
	 * @param client the current client
	 * @param clientThread the client thread to perform operations on
	 */
	void update(@Nonnull Client client, @Nonnull ClientThread clientThread);

	/**
	 * Update a container with the supplied quest list
	 *
	 * @param questHelpers the quests to filter/display.
	 * @param loggedOut if the client is logged out
	 * @param questStates the list of quests and their quest states
	 */
	default void updateQuests(List<QuestHelper> questHelpers, boolean loggedOut, Map<QuestHelperQuest, QuestState> questStates) {}
}
