/*
 * Copyright (c) 2021, Senmori
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.questhelper.quests.thegrandtree;

import com.questhelper.ItemCollections;
import com.questhelper.QuestDescriptor;
import com.questhelper.QuestHelperQuest;
import com.questhelper.Zone;
import com.questhelper.panel.PanelDetails;
import com.questhelper.questhelpers.BasicQuestHelper;
import com.questhelper.requirements.ComplexRequirement;
import com.questhelper.requirements.FreeInventorySlotRequirement;
import com.questhelper.requirements.ItemRequirement;
import com.questhelper.requirements.conditional.ItemCondition;
import com.questhelper.requirements.conditional.ObjectCondition;
import com.questhelper.requirements.conditional.ZoneCondition;
import com.questhelper.requirements.util.LogicType;
import com.questhelper.steps.ConditionalStep;
import com.questhelper.steps.DetailedQuestStep;
import com.questhelper.steps.NpcStep;
import com.questhelper.steps.ObjectStep;
import com.questhelper.steps.QuestStep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.coords.WorldPoint;

@QuestDescriptor(
	quest = QuestHelperQuest.THE_GRAND_TREE
)
public class TheGrandTree extends BasicQuestHelper
{
	// Items to have before starting the quest
	ItemRequirement coins1000, staminaPotion, yanilleTeleportMethod, necklaceOfPassage;

	// Items received/picked up during quest
	ItemRequirement barkSample, translationBook, hazelmereScroll, lumberOrder, invasionPlans, gloughChestKey, gloughJournal;

	// NPCs to interact with
	NpcStep kingNarnodeStartQuest, npcHazelmere, narnodeReport, npcGlough, narnodePostGlough, narnodePostGlough2, npcCharlie, npcShipyardForeman,
		npcNarnodeLumberOrder, npcAnita, narnodeInvasionPlans, npcCharlieInJail, npcCharliePostLumberOrder;
	NpcStep talkToCharlieAgain, narnodePostChest, blackDemon, narnodeFinishQuest;

	ZoneCondition inGloughHouseF1, inGrandTreeBase, onGroundFloorHazelmere, notOnGroundFloorHazelmere, nearHazelmereIsland;

	Zone hazelmereHutGroundF, hazelmereHutIsland, hazelmereHutF1, gloughHouseF1, grandTreeBase, topLevelofGT, secondLevelOfGT, firstLevelOfGT, anitaHouse;

	QuestStep doorToHazelmereHutClosed, doorToHazelmereOpen, ladderUpToHazelmere;
	ObjectStep searchForGloughJournal;

	DetailedQuestStep gloughCupboard;

	QuestStep goUpToGloughHouse, gloughTreeToUpstairs, gloughPillarT, gloughPillarU, gloughPillarZ, gloughPillarO, gloughTrapdoor, gtLadder3;
	QuestStep gtLadder2, gtLadder1, gtLadderBase, gloughChest;

	ObjectStep grandTreeDoor, shipYardGate, anitaEntrance, lastDaconiaRock, gloughLadder, gloughLadderDown;

	ConditionalStep speakToHazelmere, gloughPillarSequence, speakToGlough, talkToCharlie, backToNarnodeGlough, goToAnita, searchInvasionPlans;

	private void addStep(int varBit, QuestStep step)
	{
		steps.put(varBit, step);
	}

	private Map<Integer, QuestStep> steps = new HashMap<>();
	ObjectStep cupboard = new ObjectStep(this, ObjectID.CUPBOARD_2434, new WorldPoint(2476, 3465, 1), "test123");
	@Override
	public Map<Integer, QuestStep> loadSteps()
	{
		setupZones();
		setupItemRequirements();
		setupConditions();
		setupSteps();
		setupConditionalSteps();

		addStep(0, kingNarnodeStartQuest);
		addStep(10, speakToHazelmere);
		addStep(20, narnodeReport);
		addStep(30, speakToGlough);

		// Investigation
		addStep(40, backToNarnodeGlough);
		addStep(50, talkToCharlie);
		addStep(60, cupboard);
		addStep(80, narnodeInvasionPlans);
		addStep(90, talkToCharlieAgain);
		addStep(100, shipYardGate);
		QuestStep worker = shipyardGateWorker();
		addStep(110, worker);
		addStep( 120, npcShipyardForeman);
		QuestStep stronghold = narnodeLumberOrder();
		addStep(130, stronghold); // narnode
		addStep(140, npcCharliePostLumberOrder);
		// take key from anita
		addStep(150, goToAnita);
		addStep(160, searchInvasionPlans);
		addStep(170, narnodeInvasionPlans);


		// Encounter
		addStep(180, gloughPillarSequence);
		ItemRequirement twigT = new ItemRequirement("Twig T", ItemID.TWIGS);
		twigT.setHighlightInInventory(true);
		ItemRequirement twigU = new ItemRequirement("Twig U", ItemID.TWIGS_790);
		twigT.setHighlightInInventory(true);
		ItemRequirement twigZ = new ItemRequirement("Twig Z", ItemID.TWIGS_791);
		twigT.setHighlightInInventory(true);
		ItemRequirement twigO = new ItemRequirement("Twig O", ItemID.TWIGS_792);
		twigT.setHighlightInInventory(true);
		ItemCondition useTTwig = new ItemCondition(twigT, new WorldPoint(2485, 3467, 2));
		ItemCondition useUTwig = new ItemCondition(twigU, new WorldPoint(2486, 3467, 2));
		ItemCondition useZTwig = new ItemCondition(twigZ, new WorldPoint(2487, 3467, 2));
		ItemCondition useOTwig = new ItemCondition(twigO, new WorldPoint(2488, 3467, 2));

		ConditionalStep trapdoor = new ConditionalStep(this, gloughTrapdoor, "Enter the trapdoor.");
		trapdoor.addText("Be prepared for combat with a Black Demon (level 172).");
		trapdoor.addStep(new ItemCondition(twigT), gloughPillarT);
		trapdoor.addStep(new ItemCondition(twigU), gloughPillarU);
		trapdoor.addStep(new ItemCondition(twigZ), gloughPillarZ);
		trapdoor.addStep(new ItemCondition(twigO), gloughPillarO);
		addStep(190, trapdoor);
		addStep(200, blackDemon);
		addStep(500, lastDaconiaRock);
		addStep(600, narnodeFinishQuest);
		return steps;
	}

	@Override
	public ArrayList<PanelDetails> getPanels()
	{
		ArrayList<PanelDetails> panels = new ArrayList<>();
		PanelDetails gettingStarted = new PanelDetails("Getting Started", new ArrayList<>(), yanilleTeleportMethod, necklaceOfPassage);
		gettingStarted.addSteps(kingNarnodeStartQuest); // 0
		gettingStarted.addSteps(npcHazelmere); // 10
		gettingStarted.addSteps(narnodeReport); // 20
		gettingStarted.addSteps(speakToGlough); // 30
		panels.add(gettingStarted);

		// Investigation - starts after we speak to Glough the first time
		ItemRequirement karamjaTeleport = new ItemRequirement("Teleport out of Karamja", -1, -1);
		karamjaTeleport.setTooltip("Optional but recommended. Necklace of Passage will teleport you just south of the Stronghold entrance.");

		PanelDetails investigation = new PanelDetails("Investigation", new ArrayList<>(), coins1000, karamjaTeleport);
		investigation.addSteps(backToNarnodeGlough); // 40
		investigation.addSteps(npcCharlie); // 50
		investigation.addSteps(cupboard); // 60
		investigation.addSteps(npcGlough); // 70?
		investigation.addSteps(npcCharlieInJail);
		investigation.addSteps(shipYardGate);
		investigation.addSteps(npcShipyardForeman);
		investigation.addSteps(npcNarnodeLumberOrder);
		panels.add(investigation);

		ItemRequirement combatGear = new ItemRequirement("Combat equipment", -1, -1);
		combatGear.setTooltip("The Black Demon can be safespotted so Range/Mage gear can be used.");
		ItemRequirement food = new ItemRequirement("Food", -1, -1);
		FreeInventorySlotRequirement freeInv = new FreeInventorySlotRequirement(InventoryID.INVENTORY, 4);

		ArrayList<QuestStep> encounterSteps = new ArrayList<>(Arrays.asList(npcCharliePostLumberOrder, npcAnita,
			gloughChest, narnodePostChest, gloughPillarT, gloughPillarU, gloughPillarZ, gloughPillarO, gloughTrapdoor,
			blackDemon, lastDaconiaRock, narnodeFinishQuest));
		PanelDetails encounter = new PanelDetails("Encounter", encounterSteps, combatGear, food, freeInv);

		panels.add(encounter);
		return panels;
	}

	private void setupZones()
	{
		grandTreeBase = new Zone(new WorldPoint(2469, 3492, 0), new WorldPoint(2462, 3499, 0));
		hazelmereHutGroundF = new Zone(new WorldPoint(2675, 3088, 0), new WorldPoint(2679, 3086, 0));
		hazelmereHutF1 = new Zone(new WorldPoint(2673, 3085, 1), new WorldPoint(2680, 3089, 1));
		gloughHouseF1 = new Zone(new WorldPoint(2475, 3461, 1), new WorldPoint(2484, 3465, 1));
		anitaHouse = new Zone(new WorldPoint(2387, 3516, 1), new WorldPoint(2391, 3513, 1));

		topLevelofGT = new Zone(new WorldPoint(2469, 3492, 3), new WorldPoint(2462, 3499, 3));
		secondLevelOfGT = new Zone(new WorldPoint(2469, 3492, 2), new WorldPoint(2462, 3499, 2));
		firstLevelOfGT = new Zone(new WorldPoint(2469, 3492, 1), new WorldPoint(2462, 3499, 1));
		hazelmereHutIsland = new Zone(new WorldPoint(2654, 3114, 0), new WorldPoint(2688, 3076, 0));
	}

	private void setupItemRequirements()
	{
		coins1000 = new ItemRequirement("Coins", ItemID.COINS_995, 1000);
		coins1000.setTooltip("Unnecessary if you have helped Femi, or if you have completed Tree Gnome Village and can use Spirit Trees.");
		staminaPotion = new ItemRequirement("Stamina Potion(s)", ItemID.STAMINA_POTION4, 2);
		staminaPotion.addAlternates(ItemID.STAMINA_POTION3);
		yanilleTeleportMethod = new ItemRequirement("Teleport near Yanille", -1, -1);
		yanilleTeleportMethod.setTooltip("Watchtower Teleport, Ring of Dueling, NMZ Teleport, or fairy ring C L S");
		necklaceOfPassage = new ItemRequirement("Necklace of Passage", ItemCollections.getNecklaceOfPassages());
		translationBook = new ItemRequirement("Translation Book", ItemID.TRANSLATION_BOOK);
		barkSample = new ItemRequirement("Bark Sample", ItemID.BARK_SAMPLE);
		hazelmereScroll = new ItemRequirement("Hazelmere Scroll", ItemID.HAZELMERES_SCROLL);
		lumberOrder = new ItemRequirement("Lumber Order", ItemID.LUMBER_ORDER);
		invasionPlans = new ItemRequirement("Invasion Plans", ItemID.INVASION_PLANS);
		gloughChestKey = new ItemRequirement("Glough's Key", ItemID.GLOUGHS_KEY);
		gloughJournal = new ItemRequirement("Glough Journal", ItemID.GLOUGHS_JOURNAL);
	}

	private void setupSteps()
	{
		refreshGTLadders();
		kingNarnodeStartQuest = createNarnode();
		kingNarnodeStartQuest.addDialogStep("Hi! It seems to be a very busy settlement.");
		kingNarnodeStartQuest.addDialogStep("You seem worried, what's up?");
		kingNarnodeStartQuest.addDialogStep("I'd be happy to help!");
		kingNarnodeStartQuest.addRequirement(yanilleTeleportMethod);

		goUpToGloughHouse = new ObjectStep(this, ObjectID.LADDER_16683, new WorldPoint(2476, 3463, 0), "");

		doorToHazelmereHutClosed = new ObjectStep(this, ObjectID.DOOR_1543, new WorldPoint(2677, 3088,0), "");
		doorToHazelmereOpen = new ObjectStep(this, ObjectID.DOOR_1544, new WorldPoint(2677, 3089,0), "");
		ladderUpToHazelmere = new ObjectStep(this, ObjectID.LADDER_16683, "");

		narnodeReport = createNarnode("Bring Hazelmere's scroll back to King Narnode.");
		narnodeReport.addDialogStep("I think so!");
		narnodeReport.addDialogStep("None of the above.");
		narnodeReport.addDialogStep("None of the above.");
		narnodeReport.addDialogStep("A man came to me with the King's seal.");
		narnodeReport.addDialogStep("I gave the man Daconia rocks.");
		narnodeReport.addDialogStep("And Daconia rocks will kill the tree!");
		narnodeReport.addRequirement(hazelmereScroll);

		gloughTreeToUpstairs = new ObjectStep(this, ObjectID.TREE_2447, new WorldPoint(2484, 3464, 1), "Climb the tree to go upstairs");

		narnodePostGlough = createNarnode("Go back to King Narnode.");
		narnodePostGlough2 = createNarnode("Speak to King Narnode again.");

		npcNarnodeLumberOrder = createNarnode("Bring the lumber order back to King Narnode.");
		npcNarnodeLumberOrder.addRequirement(lumberOrder);

		narnodeFinishQuest = createNarnode("Speak to King Narnode to finish the quest.");

		npcCharlie = new NpcStep(this, NpcID.CHARLIE, new WorldPoint(2464, 3495,3), "Speak to Charlie at the top of the Grand Tree.");
		npcCharlieInJail = new NpcStep(this, NpcID.CHARLIE, new WorldPoint(2464, 3495,3), "Speak to Charlie again.");
		npcCharliePostLumberOrder = new NpcStep(this, NpcID.CHARLIE, new WorldPoint(2464, 3495,3), "Speak to Charlie again at the top of the Grand Tree.");

		npcHazelmere = new NpcStep(this, NpcID.HAZELMERE, new WorldPoint(2677, 3087, 1), "Speak to Hazelmere.");
		npcHazelmere.addItemRequirements(new ArrayList<>(Arrays.asList(translationBook, barkSample, necklaceOfPassage)));

		npcGlough = new NpcStep(this, NpcID.GLOUGH_2061, new WorldPoint(2478, 3463, 1), "Speak to Glough.");
		talkToCharlieAgain = new NpcStep(this, NpcID.CHARLIE, new WorldPoint(2464, 3495,3), "Speak to Charlie.");
		npcAnita = new NpcStep(this, NpcID.ANITA, new WorldPoint(2389, 3515,1), "Speak to Anita.");

		npcShipyardForeman = new NpcStep(this, NpcID.FOREMAN, "Speak to the Foreman.");
		npcShipyardForeman.addDialogStep("Sadly his wife is no longer with us!");
		npcShipyardForeman.addDialogStep("He loves worm holes.");
		npcShipyardForeman.addDialogStep("Anita.");

		narnodeInvasionPlans = createNarnode("Go back to King Narnode with the invasion plans.");
		narnodeInvasionPlans.addRequirement(invasionPlans);

		anitaEntrance = new ObjectStep(this, ObjectID.STAIRCASE_16675,  new WorldPoint(2389, 3512, 0),"Speak to Anita.");


		// Use sticks on pillars in Glough's house to spell out 'TUZO'
		gloughPillarT = new ObjectStep(this, ObjectID.PILLAR, new WorldPoint(2485, 3467, 2), "Use 'T' twigs on the pillar.");
		gloughPillarU = new ObjectStep(this, ObjectID.PILLAR_2441, new WorldPoint(2486, 3467, 2), "Use 'U' twigs on the pillar.");
		gloughPillarZ = new ObjectStep(this, ObjectID.PILLAR_2442, new WorldPoint(2487, 3467, 2),"Use 'Z' twigs on the pillar.");
		gloughPillarO = new ObjectStep(this, ObjectID.PILLAR_2443, new WorldPoint(2488, 3467, 2),"Use 'O' twigs on the pillar.");
		// 2444 is the ObjectID for the trapdoor by the pillars in Glough's house
		gloughTrapdoor = new ObjectStep(this, 2444, new WorldPoint(2487, 3464, 2), "Go down the trapdoor", new ItemRequirement("Be prepared for combat", -1, -1));

		shipYardGate = new ObjectStep(this, ObjectID.GATE_2438, "Enter the shipyard.");
		shipYardGate.addAlternateObjects(ObjectID.GATE_2439);

		narnodePostChest = createNarnode("Speak to King Narnode concerning what you found in Glough's chest.");

		lastDaconiaRock = new ObjectStep(this, ObjectID.DACONIA_ROCKS, "Find the remaining Daconia rocks.");

		blackDemon = new NpcStep(this, NpcID.BLACK_DEMON, "Kill Glough's pet.");


		//gloughCupboard = new DetailedQuestStep(this, new WorldPoint(2476, 3465, 1), "Search Glough's cupboard.");
		gloughLadder = new ObjectStep(this, ObjectID.LADDER_16683, new WorldPoint(2476, 3463, 1), "up");
		gloughCupboard = new ObjectStep(this, ObjectID.LADDER_16683, new WorldPoint(2476, 3463, 0), "up");
		gloughLadderDown = new ObjectStep(this, ObjectID.LADDER_16679, new WorldPoint(2476, 3463, 1), "");;
	}

	private void setupConditions()
	{
		inGrandTreeBase = new ZoneCondition(grandTreeBase);
		inGloughHouseF1 = new ZoneCondition(gloughHouseF1);
		onGroundFloorHazelmere = new ZoneCondition(hazelmereHutGroundF);
		notOnGroundFloorHazelmere = new ZoneCondition(false, hazelmereHutGroundF);
		nearHazelmereIsland = new ZoneCondition(hazelmereHutIsland);
	}

	private void setupConditionalSteps()
	{
		// Talk to Hazelmere
		/*
		 * These conditions are for properly rendering the objects that should be highlighted
		 * If the player is not in the area near hazelmere, it should point towards the hut in general (i.e. world map pin)
		 * If the player is near the hut and the door is open, the ladder should be highlighted
		 * If the player is near the hunt and the door is closed, the door should be highlighted
		 * If the player is IN the hut on the ground floor, highlight the ladder
		 * If on the first floor (second floor US), highlight NPC hazelmere
		 * Otherwise, point towards the world map location of hazelmere's hut
		 */

		speakToHazelmere = new ConditionalStep(this, npcHazelmere, "Speak to Hazelmere in his house east of Yanille.");
		ObjectCondition hazelmereDoorOpen = new ObjectCondition(ObjectID.DOOR_1544, new WorldPoint(2677, 3089,0));
		ObjectCondition hazelmereDoorClosed = new ObjectCondition(ObjectID.DOOR_1543, new WorldPoint(2677, 3088,0));
		ComplexRequirement highlightDoor = new ComplexRequirement(LogicType.AND, "", nearHazelmereIsland, notOnGroundFloorHazelmere, hazelmereDoorClosed);
		ComplexRequirement highlightLadder = new ComplexRequirement(LogicType.AND, "", nearHazelmereIsland, onGroundFloorHazelmere);
		ComplexRequirement highlightLadder2 = new ComplexRequirement(LogicType.OR, highlightLadder, new ComplexRequirement(LogicType.AND, "", nearHazelmereIsland, hazelmereDoorOpen));
		speakToHazelmere.addStep(highlightDoor, doorToHazelmereHutClosed);
		speakToHazelmere.addStep(highlightLadder2, ladderUpToHazelmere);

		// If in grand tree
		/*
		 * If the player is in the grand tree highlight the doors in case the player thinks Glough is inside
		 * the grand tree
		 */
		grandTreeDoor = new ObjectStep(this, ObjectID.TREE_DOOR_1968, "");
		grandTreeDoor.addAlternateObjects(1967); // 1967 is the left side of the grand tree door
		speakToGlough = new ConditionalStep(this, goUpToGloughHouse, "Speak to Glough in his house just south of the Grand Tree.");
		speakToGlough.addStep(inGrandTreeBase, grandTreeDoor);
		speakToGlough.addStep(inGloughHouseF1, npcGlough);

		// Back to narnode after speaking with Glough
		backToNarnodeGlough = new ConditionalStep(this, narnodePostGlough, "Speak to King Narnode.");
		ObjectStep gloughLadderDown = new ObjectStep(this, ObjectID.LADDER_16679, new WorldPoint(2476, 3463, 1), "Go down ladder.");
		backToNarnodeGlough.addStep(inGloughHouseF1, gloughLadderDown);

		goToAnita = new ConditionalStep(this, anitaEntrance, "Go to Anita's house in the NW area of the Gnome Stronghold.");
		goToAnita.addStep(new ZoneCondition(anitaHouse), npcAnita);

		// Talk to Charlie the first time (i.e. going up)
		talkToCharlie = createTalkToCharlieStep(true);

		gloughChest = new ObjectStep(this, ObjectID.CHEST, "Search Glough's chest for the invasion plans.");
		searchInvasionPlans = new ConditionalStep(this,  goUpToGloughHouse);
		searchInvasionPlans.addStep(inGloughHouseF1, gloughChest);

		gloughPillarSequence = new ConditionalStep(this, goUpToGloughHouse, "Go to the top floor of Glough's house.");
		gloughPillarSequence.addStep(inGloughHouseF1, gloughTreeToUpstairs);

		searchForGloughJournal = new ObjectStep(this, 2434, new WorldPoint(2476, 3465, 1), "Search Glough's cupboard for his journal.");
		searchForGloughJournal.addAlternateObjects(2435);
	}

	private void refreshGTLadders()
	{
		gtLadder3 = new ObjectStep(this, ObjectID.LADDER_16679,  new WorldPoint(2466, 3495, 3),"");
		gtLadder2 = new ObjectStep(this, ObjectID.LADDER_2884,  new WorldPoint(2466, 3495, 2),"");
		gtLadder1 = new ObjectStep(this, ObjectID.LADDER_16684, new WorldPoint(2466, 3495, 1),"");
	}
	private ConditionalStep createTalkToCharlieStep(boolean fromBottomOfTree)
	{
		talkToCharlie = new ConditionalStep(this, npcCharlie, "Speak to Charlie.");
		refreshGTLadders();
		if (fromBottomOfTree)
		{
			gtLadderBase = new ObjectStep(this, ObjectID.LADDER_16683, new WorldPoint(2466, 3495, 0), "Speak to Charlie at the top of the Grand Tree.");
			talkToCharlie.addStep(new ZoneCondition(grandTreeBase), gtLadderBase);
			gtLadder1.addDialogStep("Climb Up.");
			talkToCharlie.addStep(new ZoneCondition(firstLevelOfGT), gtLadder1);
			gtLadder2.addDialogStep("Climb Up.");
			talkToCharlie.addStep(new ZoneCondition(secondLevelOfGT), gtLadder2);
			gtLadder3.addDialogStep("Climb Up.");
			return talkToCharlie;
		}
		gtLadder3.addDialogStep("Climb Down.");
		talkToCharlie.addStep(new ZoneCondition(topLevelofGT), gtLadder3);
		gtLadder2.addDialogStep("Climb Down.");
		talkToCharlie.addStep(new ZoneCondition(secondLevelOfGT), gtLadder2);
		gtLadder1.addDialogStep("Climb Down.");
		talkToCharlie.addStep(new ZoneCondition(firstLevelOfGT), gtLadder1);
		return talkToCharlie;
	}

	@Override
	public ArrayList<ItemRequirement> getItemRecommended()
	{
		return new ArrayList<>(Arrays.asList(coins1000, staminaPotion, yanilleTeleportMethod, necklaceOfPassage));
	}

	@Override
	public ArrayList<String> getCombatRequirements()
	{
		return new ArrayList<>(Arrays.asList("Black Demon (Level 172, safespottable)", "Jogre(s) (level 53)", "Jungle Spider(s) (level 44)"));
	}

	private ConditionalStep narnodeLumberOrder()
	{
		NpcStep femi = new NpcStep(this,NpcID.FEMI, new WorldPoint(2461, 3381, 0), "Talk to Femi to enter Gnome Stronghold.");
		femi.addRequirement(new ItemRequirement("Coins", ItemID.COINS_995, 1000));
		femi.addDialogStep("Ok then, here you go.");

		Zone strongholdSpiritTree = new Zone(new WorldPoint(2456, 3450, 0), new WorldPoint(2468, 3442, 0));
		ConditionalStep enterStronghold = new ConditionalStep(this, femi, "Talk to Femi to get into the stronghold.");
		enterStronghold.addStep(new ZoneCondition(strongholdSpiritTree), npcNarnodeLumberOrder);
		femi.addSubSteps(npcNarnodeLumberOrder);
		return enterStronghold;
	}

	private NpcStep createNarnode()
	{
		return createNarnode("Speak to King Narnode Shareen.");
	}
	private NpcStep createNarnode(String panelText)
	{
		return new NpcStep(this, NpcID.KING_NARNODE_SHAREEN, new WorldPoint(2465, 3495, 0), panelText);
	}

	private QuestStep shipyardGateWorker()
	{
		NpcStep shipyardGateWorker = new NpcStep(this, NpcID.SHIPYARD_WORKER, "");
		shipyardGateWorker.addDialogStep("Glough sent me.");
		shipyardGateWorker.addDialogStep("Ka.");
		shipyardGateWorker.addDialogStep("Lu.");
		shipyardGateWorker.addDialogStep("Min.");
		return shipyardGateWorker;
	}
}

