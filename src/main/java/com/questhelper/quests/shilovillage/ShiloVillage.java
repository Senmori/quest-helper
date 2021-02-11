/*
 *
 *  * Copyright (c) 2021
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
package com.questhelper.quests.shilovillage;

import com.questhelper.ItemCollections;
import com.questhelper.QuestDescriptor;
import com.questhelper.QuestHelperQuest;
import com.questhelper.QuestVarPlayer;
import com.questhelper.Zone;
import com.questhelper.panel.PanelDetails;
import com.questhelper.questhelpers.BasicQuestHelper;
import com.questhelper.requirements.ComplexRequirement;
import com.questhelper.requirements.Requirement;
import com.questhelper.requirements.WidgetTextRequirement;
import com.questhelper.requirements.ZoneRequirement;
import com.questhelper.requirements.conditional.Conditions;
import com.questhelper.requirements.conditional.ObjectCondition;
import com.questhelper.requirements.item.ItemRequirement;
import com.questhelper.requirements.item.ItemRequirements;
import com.questhelper.requirements.player.CombatLevelRequirement;
import com.questhelper.requirements.player.FreeInventorySlotRequirement;
import com.questhelper.requirements.player.SkillRequirement;
import com.questhelper.requirements.quest.QuestRequirement;
import com.questhelper.requirements.util.LogicType;
import com.questhelper.requirements.util.Operation;
import com.questhelper.requirements.var.VarbitRequirement;
import com.questhelper.requirements.var.VarplayerRequirement;
import com.questhelper.steps.ConditionalStep;
import com.questhelper.steps.DetailedQuestStep;
import com.questhelper.steps.DigStep;
import com.questhelper.steps.NpcStep;
import com.questhelper.steps.ObjectStep;
import com.questhelper.steps.QuestStep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemID;
import net.runelite.api.NpcID;
import net.runelite.api.ObjectID;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.WidgetInfo;

@QuestDescriptor(
	quest = QuestHelperQuest.SHILO_VILLAGE
)
public class ShiloVillage extends BasicQuestHelper
{
	// Item Requirements
	ItemRequirement spade, lightSource, rope, bronzeWire, chisel, _3bones;

	// Recommended Items
	ItemRequirement combatGear, goodFood, runRestoreItems, antipoison, prayerPotions,
					oneClickTeleports, papyrus, charcoal;

	// Recommended
	ComplexRequirement fairyRingAccess;
	QuestRequirement gnomeGliderAccess;
	CombatLevelRequirement combatLevel65;

	// Spells //TODO: Replace in the future
	ItemRequirements crumbleUndead;

	// Quest Requirements (per step)
	Requirement _3freeInventory, _4freeInventory;
	ItemRequirement wampumBelt, boneShard, beadsOfTheDead, locatingCrystal,
					swordPommel, berviriusNotes, boneKey, rashCorpse, stonePlaque,
					tatteredScroll, crumpledScroll, zadimusCorpse;

	// QuestStep
	NpcStep mosolReiStart, trufitusBelt;
	DetailedQuestStep goToAhZoRhoon;
	QuestStep digMound, useLightSource, useRope, climbDownFissure;
	QuestStep obtainStonePlaque, searchCaveIn, pickupTatteredScroll, pickupCrumpledScroll, getZadimusCorpse,
			leaveTheCave, buildRaft, leaveViaRocks;
	QuestStep readTattered, readCrumpled, readStonePlaque, bringItemsToTrufitus;
	QuestStep useTattered, useCrumpled, usePlaque, useZadimus, buryZadimus;

	QuestStep enterBerviriusTomb, goToDolmen, makeBoneBeads, makeBeadsOfTheDead, exitCave;
	QuestStep goToRashTomb, makeBoneKey, useKeyOnDoor;

	Requirement nearMound, fissureExposed, ropeVar, inCavedInZone;
	Requirement hasReadTattered, hasReadCrumpled, hasReadStonePlaque, tatteredWidgetReq, crumpledWidgetReq, stonePlaqueReq;
	Requirement hasShownTattered, hasShownCrumpled, hasShownPlaque, shownTatteredReq, shownCrumpledReq, shownPlaqueReq;
	Requirement nearTrufitus;

	// ConditionalStep
	ConditionalStep startQuestStep, goDownFissure, findItemsInCave;

	// Zone
	Zone ahZaRhoonZone, cavedInZone, outsideCavedInZone, outsideShiloWaterfall, trufitusHouse;

	DetailedQuestStep fake = new DetailedQuestStep(this, "Fake Step");
	@Override
	public Map<Integer, QuestStep> loadSteps()
	{
		setupZones();
		setupConditions();
		setupItemRequirements();
		setupSteps();
		setupConditionalSteps();

		Map<Integer, QuestStep> steps = new HashMap<>();


		steps.put(0, startQuestStep);

		ConditionalStep fissureStep = new ConditionalStep(this, goToAhZoRhoon);
		fissureStep.addStep(ropeVar, useRope);
		fissureStep.addStep(new Conditions(nearMound, fissureExposed), useLightSource);
		fissureStep.addStep(new Conditions(nearMound, spade), digMound);

		steps.put(1, fissureStep);
		steps.put(2, fissureStep);
		steps.put(3, fissureStep);
		steps.put(4, fissureStep);
		steps.put(5, fissureStep);
		steps.put(6, goDownFissure);

		steps.put(7, findItemsInCave);

		Conditions readAllItems = new Conditions(true, hasReadTattered, hasReadCrumpled, hasReadStonePlaque);
		Conditions usedAllItems = new Conditions(true, hasShownTattered, hasShownCrumpled, hasShownPlaque);

		ConditionalStep readItems = new ConditionalStep(this, buryZadimus, "Bring the items back to Trufitus.");
		readItems.addStep(usedAllItems, buryZadimus);
		readItems.addStep(new Conditions(readAllItems, nearTrufitus, hasShownTattered, hasShownCrumpled, hasShownPlaque), useZadimus);
		readItems.addStep(new Conditions(readAllItems, nearTrufitus, hasShownTattered, hasShownCrumpled), usePlaque);
		readItems.addStep(new Conditions(readAllItems, nearTrufitus, hasShownTattered), useCrumpled);
		readItems.addStep(new Conditions(readAllItems, nearTrufitus), useTattered);
		readItems.addStep(readAllItems, bringItemsToTrufitus);
		readItems.addStep(new Conditions(hasReadCrumpled, hasReadTattered), readStonePlaque);
		readItems.addStep(hasReadTattered, readCrumpled);
		readItems.addStep(new Conditions(LogicType.NOR, hasReadTattered), readTattered);
		steps.put(8, readItems);
		steps.put(9, buryZadimus);
		steps.put(10, fake);
		steps.put(11, fake);
		steps.put(12, fake);
		steps.put(13, fake);
		steps.put(14, fake);
		steps.put(15, fake);
		steps.put(16, fake);
		steps.put(17, fake);

		return steps;
	}

	@Override
	public List<PanelDetails> getPanels()
	{
		List<PanelDetails> panels = new ArrayList<>();

		List<QuestStep> beginningSteps = Arrays.asList(startQuestStep, goToAhZoRhoon, goDownFissure);
		PanelDetails beginning = new PanelDetails("In the Beginning", beginningSteps, combatGear, goodFood);
		panels.add(beginning);

		List<QuestStep> rhoonSteps = Arrays.asList(obtainStonePlaque, pickupTatteredScroll, pickupCrumpledScroll, getZadimusCorpse, leaveTheCave);
		PanelDetails ahZaRhoon = new PanelDetails("Ah Za Rhoon", rhoonSteps, chisel, _4freeInventory, combatGear, prayerPotions, goodFood, antipoison);
		panels.add(ahZaRhoon);

		List<QuestStep> berviriusSteps = Arrays.asList(readTattered, readCrumpled, readStonePlaque, bringItemsToTrufitus, useTattered, useCrumpled, usePlaque, buryZadimus);
		PanelDetails bervirius = new PanelDetails("Tomb of Bervirius", berviriusSteps, bronzeWire, chisel, _3freeInventory, goodFood);
		panels.add(bervirius);

		List<QuestStep> rashiliyiaSteps = Arrays.asList(fake);
		PanelDetails rashiliyia = new PanelDetails("Tomb of Rashiliyia", rashiliyiaSteps, chisel, _3bones, boneShard, beadsOfTheDead.equipped(), locatingCrystal, combatGear, goodFood, oneClickTeleports);
		panels.add(rashiliyia);

		List<QuestStep> nazastaroolSteps = Arrays.asList(fake);
		PanelDetails nazastarool = new PanelDetails("Nazastarool", nazastaroolSteps, beadsOfTheDead.equipped(), combatGear, goodFood);
		panels.add(nazastarool);

		List<QuestStep> finaleSteps = Arrays.asList(fake);
		PanelDetails finale = new PanelDetails("Rashiliyia's Corpse", finaleSteps, rashCorpse);
		panels.add(finale);

		return panels;
	}

	private void setupZones()
	{
		ahZaRhoonZone = new Zone(new WorldPoint(2919, 3002, 0), new WorldPoint(2925, 2996, 0));
		cavedInZone = new Zone(new WorldPoint(2884, 9319, 0), new WorldPoint(2895, 9281, 0));
		outsideCavedInZone = new Zone(new WorldPoint(2885, 9375, 0), new WorldPoint(2889, 9371,0));
		outsideShiloWaterfall = new Zone(new WorldPoint(2926, 2953, 0), new WorldPoint(2939, 2944, 0));
		trufitusHouse = new Zone(new WorldPoint(2805, 3091, 0), new WorldPoint(2814, 3081, 0));
	}

	private void setupItemRequirements()
	{
		spade = new ItemRequirement("Spade", ItemID.SPADE);
		spade.canBeObtainedDuringQuest();
		spade.appendToTooltip("Can be bought at the general store in Tai Bwo Wannai.");
		lightSource = new ItemRequirement("Lit torch or candle", ItemID.LIT_TORCH);
		lightSource.addAlternates(ItemID.CANDLE, ItemID.BULLSEYE_LANTERN, ItemID.BULLSEYE_LANTERN_4550);
		lightSource.setDisplayMatchedItemName(true);
		lightSource.canBeObtainedDuringQuest();
		lightSource.appendToTooltip("You will not get this item back.");
		lightSource.appendToTooltip("You can buy this item at the general store in Tai Bwo Wannai.");
		lightSource.setConditionToHide(new VarplayerRequirement(QuestVarPlayer.QUEST_SHILO_VILLAGE.getId(), 6, Operation.GREATER_EQUAL));
		rope = new ItemRequirement("Rope", ItemID.ROPE);
		rope.canBeObtainedDuringQuest();
		rope.appendToTooltip("You can buy this item at the general store in Tai Bwo Wannai.");
		rope.setConditionToHide(new VarplayerRequirement(QuestVarPlayer.QUEST_SHILO_VILLAGE.getId(), 6, Operation.GREATER_EQUAL));
		bronzeWire = new ItemRequirement("Bronze Wire", ItemID.BRONZE_WIRE);
		bronzeWire.addAlternates(ItemID.BRONZE_WIRE_5602);
		bronzeWire.setTooltip("You can make this item from scratch using materials bought from the general store in Tai Bwo Wannai.");
		chisel = new ItemRequirement("Chisel", ItemID.CHISEL);
		chisel.addAlternates(ItemID.CHISEL_5601);
		chisel.canBeObtainedDuringQuest();
		chisel.appendToTooltip("You can buy this item at the general store in Tai Bwo Wannai.");
		_3bones = new ItemRequirement("Bones", ItemID.BONES, 3);
		_3bones.canBeObtainedDuringQuest();
		_3bones.appendToTooltip("Normal bones only. Spare bones is a good idea in case you bury one.");

		// Recommended Items
		combatGear = new ItemRequirement("Decent armour and weapon", -1);
		goodFood = new ItemRequirement("Good Food", ItemCollections.getGoodEatingFood());
		runRestoreItems = new ItemRequirement("Run Restore Items", ItemCollections.getRunRestoreItems());
		antipoison = new ItemRequirement("Antipoison(s)", ItemCollections.getAntipoisons());
		antipoison.setTooltip("Poison can hit up to 11 each time. Combat level 65+ will make the poisonous tribesmen non-aggressive.");
		prayerPotions = new ItemRequirement("Prayer Potions", ItemCollections.getPrayerPotions());
		oneClickTeleports = new ItemRequirement("One Click Teleports", ItemCollections.getOneClickTeleports());
		oneClickTeleports.setTooltip("For leaving dangerous areas in a hurry.");
		papyrus = new ItemRequirement("Papyrus", ItemID.PAPYRUS);
		papyrus.addAlternates(ItemID.PAPYRUS_972);
		papyrus.setTooltip("Only required if you lost an item during the quest.");
		charcoal = new ItemRequirement("Charcoal", ItemID.CHARCOAL);
		charcoal.setTooltip("Only required if you lost an item during the quest.");

		ItemRequirement chaos = new ItemRequirement("Chaos Rune", ItemID.CHAOS_RUNE);
		ItemRequirement air = new ItemRequirement("Air Rune", ItemID.AIR_RUNE, 2);
		ItemRequirement earth = new ItemRequirement("Earth Rune", ItemID.EARTH_RUNE, 2);
		crumbleUndead = new ItemRequirements("Crumble Undead runes for Nazastarool", chaos, air, earth);

		wampumBelt = new ItemRequirement("Wampum Belt", ItemID.WAMPUM_BELT);
		boneShard = new ItemRequirement("Bone Shard", ItemID.BONE_SHARD);
		boneShard.setTooltip("If lost, you will have to retrieve Zadimus' corpse from the temple and bury it again.");
		beadsOfTheDead = new ItemRequirement("Beads of the Dead", ItemID.BEADS_OF_THE_DEAD);
		beadsOfTheDead.setTooltip("If lost, you can obtain another by speaking to Yanni Salika for 1200 coins.");
		locatingCrystal = new ItemRequirement("Locating Crystal", ItemID.LOCATING_CRYSTAL);
		locatingCrystal.addAlternates(ItemID.LOCATING_CRYSTAL_612, ItemID.LOCATING_CRYSTAL_613, ItemID.LOCATING_CRYSTAL_614, ItemID.LOCATING_CRYSTAL_615);
		locatingCrystal.setTooltip("If lost you will need to re-visit Bervirius' tomb.");

		stonePlaque = new ItemRequirement("Stone Plaque", ItemID.STONEPLAQUE);
		tatteredScroll = new ItemRequirement("Tattered Scroll", ItemID.TATTERED_SCROLL);
		crumpledScroll = new ItemRequirement("Crumpled Scroll", ItemID.CRUMPLED_SCROLL);
		zadimusCorpse = new ItemRequirement("Zadimus Corpse", ItemID.ZADIMUS_CORPSE);

		swordPommel = new ItemRequirement("Sword Pommel", ItemID.SWORD_POMMEL);
		berviriusNotes = new ItemRequirement("Bervirius Notes", ItemID.BERVIRIUS_NOTES);
		berviriusNotes.setTooltip("If you lose these you will need papyrus and charcoal to get the notes again.");
		boneKey = new ItemRequirement("Bone Key", ItemID.BONE_KEY);
		boneKey.setTooltip("If lost, can be obtained by speaking to Yanni Salika for 100 coins.");
		rashCorpse = new ItemRequirement("Rashiliyia Corpse", ItemID.RASHILIYIA_CORPSE);

		setupRecommended();
	}

	private void setupRecommended()
	{
		ItemRequirement dramenOrLunar = new ItemRequirement("Dramen Staff", ItemID.DRAMEN_STAFF, 1, true);
		dramenOrLunar.addAlternates(ItemID.LUNAR_STAFF);
		dramenOrLunar.setDisplayMatchedItemName(true);
		QuestRequirement fairytale2 = new QuestRequirement(QuestHelperQuest.FAIRYTALE_II__CURE_A_QUEEN, QuestState.IN_PROGRESS);
		fairyRingAccess = new ComplexRequirement("Fairy Ring Access", dramenOrLunar, fairytale2);
		fairyRingAccess.setTooltip("Requires a dramen/lunar staff and partial completion of Fairy Tale II - Cure a Queen.");
		gnomeGliderAccess = new QuestRequirement(QuestHelperQuest.THE_GRAND_TREE, QuestState.FINISHED, "Gnome Glider Access");
		gnomeGliderAccess.setTooltip("Requires completion of The Grand Tree.");

		_3freeInventory = new FreeInventorySlotRequirement(InventoryID.INVENTORY, 3);
		_4freeInventory = new FreeInventorySlotRequirement(InventoryID.INVENTORY, 4);
		combatLevel65 = new CombatLevelRequirement(65);
		combatLevel65.setTooltip("This will make the poisonous tribesmen non-aggressive.");
	}

	private void setupConditions()
	{
		nearMound = new ZoneRequirement(ahZaRhoonZone);
		fissureExposed = new ObjectCondition(ObjectID.FISSURE, new WorldPoint(2921, 3001, 0));
		ropeVar = new VarplayerRequirement(QuestVarPlayer.QUEST_SHILO_VILLAGE.getId(), 5);
		inCavedInZone = new Conditions(true, new ZoneRequirement(cavedInZone));

		tatteredWidgetReq = new WidgetTextRequirement(220, 3, "Bervirius, son of King Danthalas, was killed in battle. His");
		crumpledWidgetReq = new WidgetTextRequirement(222, 3, "Rashiliyia's rage went unchecked. She killed");
		stonePlaqueReq = new WidgetTextRequirement(229,1, "The markings are very intricate. It's a very strange language. The<br>meaning of it evades you though. Perhaps Trufitus can decipher the<br>markings?");
		WidgetTextRequirement stonePlaqueReqAlternate = new WidgetTextRequirement(193, 2, "You remember what Trufitus told you about this. <col=0000ff>'Here<br><col=0000ff>lies the traitor Zadimus, <col=0000ff>let his spirit be forever<br><col=0000ff>tormented.'");

		hasReadTattered = new Conditions(true, tatteredWidgetReq);
		hasReadCrumpled = new Conditions(true, crumpledWidgetReq);
		hasReadStonePlaque = new Conditions(true, LogicType.OR, stonePlaqueReq, stonePlaqueReqAlternate);

		shownTatteredReq = new WidgetTextRequirement(229, 1,"You hand the tattered scroll to Trufitus.");
		shownCrumpledReq = new WidgetTextRequirement(229, 1, "You hand the crumpled scroll to Trufitus.");
		shownPlaqueReq = new WidgetTextRequirement(229, 1, "You hand over the Stone Plaque to Trufitus.");

		hasShownTattered = new Conditions(true, shownTatteredReq);
		hasShownCrumpled = new Conditions(true, crumpledWidgetReq);
		hasShownPlaque = new Conditions(true, shownPlaqueReq);

		nearTrufitus = new ZoneRequirement(trufitusHouse);

	}

	private void setupConditionalSteps()
	{
		startQuestStep = new ConditionalStep(this, mosolReiStart);
		startQuestStep.addStep(wampumBelt.copy(), trufitusBelt);

		goDownFissure = new ConditionalStep(this, climbDownFissure);
		goDownFissure.addStep(new Conditions(LogicType.NOR, nearMound), goToAhZoRhoon);

		Conditions hasItems = new Conditions(stonePlaque, tatteredScroll, crumpledScroll, zadimusCorpse);
		Conditions isOutsideCave = new Conditions(true, new ZoneRequirement(outsideCavedInZone));
		WidgetTextRequirement unableToBuildRaft = new WidgetTextRequirement(229, 1, "There isn't enough wood left in this table to make anything!");
		Conditions hasLeftRhoonCave = new Conditions(true, new ZoneRequirement(outsideShiloWaterfall));

		Zone mapRegion = new Zone(11566);
		ZoneRequirement notInMapRegion = new ZoneRequirement(false, mapRegion);


		findItemsInCave = new ConditionalStep(this, obtainStonePlaque, "Find the scrolls and Zadimus' corpse.");
		findItemsInCave.addStep(new Conditions(true, notInMapRegion), bringItemsToTrufitus);
		findItemsInCave.addStep(new Conditions(true, hasItems, hasLeftRhoonCave),bringItemsToTrufitus);
		findItemsInCave.addStep(new Conditions(true, unableToBuildRaft), leaveViaRocks);
		findItemsInCave.addStep(new Conditions(hasItems, isOutsideCave), buildRaft);
		findItemsInCave.addStep(hasItems, leaveTheCave);
		findItemsInCave.addStep(crumpledScroll, getZadimusCorpse);
		findItemsInCave.addStep(tatteredScroll, pickupCrumpledScroll);
		findItemsInCave.addStep(new Conditions(true, stonePlaque, inCavedInZone), pickupTatteredScroll);
		findItemsInCave.addStep(stonePlaque, searchCaveIn);

	}

	private void setupSteps()
	{
		Requirement[] startReq = new Requirement[]{ combatGear, goodFood, oneClickTeleports, _3bones, spade, lightSource, rope, chisel, _4freeInventory, prayerPotions};
		mosolReiStart = new NpcStep(this, NpcID.MOSOL_REI, new WorldPoint(2881, 2950, 0),"Talk to Mosol Rei near the entrance of Shilo Village.", startReq);
		mosolReiStart.addAlternateNpcs(NpcID.MOSOL_REI_8696);
		mosolReiStart.setOverlayText("Talk to Mosol Rei near Shilo Village.", "The fastest way to get there is to take a gnome glider to Gandius and run south.");
		mosolReiStart.setOverlayText("This requires 30 Agility.");
		mosolReiStart.setOverlayText("You can also use the Fairy Ring 'C K R', run south across the bridge and then east on the path behind Shilo Village.");
		mosolReiStart.addDialogStep("Why do I need to run?");
		mosolReiStart.addDialogStep("Rashiliyia? Who is she?");
		mosolReiStart.addDialogStep(1, "What can we do?");
		mosolReiStart.addDialogStep("I'll go to see the Shaman.");
		mosolReiStart.addDialogStep("Yes, I'm sure and I'll take the Wampum belt to Trufitus.");

		trufitusBelt = new NpcStep(this, NpcID.TRUFITUS, new WorldPoint(2809, 3086, 0), "Use the wampum belt on Trufitus.", startReq);
		trufitusBelt.addRequirement(wampumBelt.highlighted());
		trufitusBelt.addDialogStep("Mosol Rei said something about a legend?");
		trufitusBelt.addDialogStep("Why was it called Ah Za Rhoon?");
		trufitusBelt.addDialogStep("I am going to search for Ah Za Rhoon!");
		trufitusBelt.addDialogStep("Yes, I will seriously look for Ah Za Rhoon and I'd appreciate your help.");

		mosolReiStart.addSubSteps(trufitusBelt);

		goToAhZoRhoon = new DetailedQuestStep(this, new WorldPoint(2921, 2999, 0), "Go to Ah Za Rhoon.", startReq);
		goToAhZoRhoon.setOverlayText("Go to Ah Za Rhoon. Bring combat gear and/or Prayer potions to defend against the Undead Ones. ");
		goToAhZoRhoon.setOverlayText("A quick way to get there is by using the gnome glider to Gandius.");

		digMound = new DigStep(this, new WorldPoint(2922, 2999, 0), "Use your spade on the mound of earth.", spade.highlighted());
		useLightSource = new DetailedQuestStep(this, new WorldPoint(2921, 2999, 0), "Use your light source on the fissure.", lightSource.highlighted());
		useLightSource.addDialogStep("Yes");
		useRope = new DetailedQuestStep(this, new WorldPoint(2921, 2999, 0), "Use your rope on the fissure.", rope.highlighted());
		climbDownFissure = new ObjectStep(this, ObjectID.FISSURE_2219, "Climb down the fissure.");
		climbDownFissure.setOverlayText("Climb down the fissure by right clicking and selecting 'Search'.");
		climbDownFissure.setOverlayText("If the fissure becomes a mound of earth, right click and search it to continue.");
		climbDownFissure.addDialogStep("Yes, I'll give it a go!");

		goToAhZoRhoon.addSubSteps(digMound, useLightSource, useRope, climbDownFissure);

		obtainStonePlaque = new ObjectStep(this, ObjectID.STRANGE_LOOKING_STONE, new WorldPoint(2901, 9379, 0), "Use a chisel on the strange looking stone.", chisel.highlighted());

		searchCaveIn = new ObjectStep(this, ObjectID.CAVE_IN, "Search the cave in.", stonePlaque);
		searchCaveIn.addDialogStep("Yes, I'll wriggle through.");

		pickupTatteredScroll = new ObjectStep(this, ObjectID.LOOSE_ROCKS, "Search the loose rocks for the tattered scroll.");
		pickupTatteredScroll.addDialogStep("Yes, I'll carefully move the rocks to see what's behind them." );
		pickupCrumpledScroll = new ObjectStep(this, ObjectID.OLD_SACKS, "Search the sacks for the crumpled scroll further down the tunnel.");
		getZadimusCorpse = new ObjectStep(this, ObjectID.ANCIENT_GALLOWS,  "Search the gallows for Zadimus' corpse just north of the old sacks.");
		getZadimusCorpse.setOverlayText("'Search' the gallows to the north to get Zadimus' corpse.");
		getZadimusCorpse.addDialogStep("Yes, I may find something else on the corpse.");

		leaveTheCave = new ObjectStep(this, ObjectID.CAVE_IN, "Leave the cave.", stonePlaque, tatteredScroll, crumpledScroll, zadimusCorpse);
		leaveTheCave.setOverlayText("Leave the cave via the way you came in or by teleporting out.");
		leaveTheCave.addDialogStep("Yes, I'll wriggle through.");

		buildRaft = new ObjectStep(this, ObjectID.SMASHED_TABLE, new WorldPoint(2896, 9377, 0), "Build a raft using the smashed table.");
		buildRaft.addDialogStep("A crude raft");

		leaveViaRocks = new ObjectStep(this, ObjectID.WATERFALL_ROCKS_2225, "Leave the cave via the rocks at the end of the river.");
		leaveViaRocks.setOverlayText("Follow the river to exit the cave.");
		leaveViaRocks.addDialogStep("Yes, I'll follow the path.");


		readTattered = new DetailedQuestStep(this, "Read the Tattered Scroll.", tatteredScroll.highlighted());
		readTattered.addDialogStep("Yes please.");
		readCrumpled = new DetailedQuestStep(this, "Read the Crumpled Scroll.", crumpledScroll.highlighted());
		readCrumpled.addDialogStep("Yes please.");
		readStonePlaque = new DetailedQuestStep(this, "Read the Stone Plaque.", stonePlaque.highlighted());

		bringItemsToTrufitus = new NpcStep(this, NpcID.TRUFITUS, new WorldPoint(2809, 3086, 0), "Read both scrolls and the plaque and then bring the items back to Trufitus.",
											tatteredScroll, crumpledScroll, stonePlaque, zadimusCorpse);
		bringItemsToTrufitus.setOverlayText("After reading the scrolls and the plaque, use the items on Trufitus.");
		bringItemsToTrufitus.addDialogStep("Anything that can help?");

		useTattered = new NpcStep(this, NpcID.TRUFITUS, new WorldPoint(2809, 3086, 0), "Use the Tattered Sroll on Trufitus.", tatteredScroll.highlighted());
		useTattered.addIcon(tatteredScroll.getId());
		useCrumpled = new NpcStep(this, NpcID.TRUFITUS, new WorldPoint(2809, 3086, 0), "Use the Crumpled Scroll on Trufitus.", crumpledScroll.highlighted());
		useCrumpled.addIcon(crumpledScroll.getId());
		usePlaque = new NpcStep(this, NpcID.TRUFITUS, new WorldPoint(2809, 3086, 0), "Use the Stone Plaque on Trufitus.", stonePlaque.highlighted());
		usePlaque.addIcon(stonePlaque.getId());
		useZadimus = new NpcStep(this, NpcID.TRUFITUS, new WorldPoint(2809, 3086, 0), "Use Zadimus' corpse on Trufitus.", zadimusCorpse.highlighted());
		useZadimus.addIcon(zadimusCorpse.getId());

		buryZadimus = new DetailedQuestStep(this, new WorldPoint(2795, 3089, 0), "Bury Zadimus near the Tribal Statue.", zadimusCorpse.highlighted());
		buryZadimus.addDialogStep("Is there any sacred ground around here?");
		buryZadimus.addIcon(zadimusCorpse.getId());


		/*
		  * READ TATTERED SCROLL:
		  * 5983 = 1 -> 0 on first click
		  * 5983 = 0 -> 1 on confirmation screen
		  * 5983 = 1 -> 0 if player doesn't read
		 */
		/*
		 * CRUMPLED SCROLL:
		 * - same as tattered
		 */
		/*
		 * Stone Plaque
		 * - no changes found
		 */
	}

	@Override
	public List<Requirement> getGeneralRequirements()
	{
		List<Requirement> req = new ArrayList<>();
		req.add(new QuestRequirement(QuestHelperQuest.JUNGLE_POTION, QuestState.FINISHED));
		req.add(new SkillRequirement(Skill.CRAFTING, 20));
		req.add(new SkillRequirement(Skill.AGILITY, 32));
		return req;
	}

	@Override
	public List<Requirement> getGeneralRecommended()
	{
		return Arrays.asList(fairyRingAccess, gnomeGliderAccess, combatLevel65);
	}

	@Override
	public List<ItemRequirement> getItemRequirements()
	{
		return Arrays.asList(spade,lightSource, rope, bronzeWire, chisel, _3bones);
	}

	@Override
	public List<ItemRequirement> getItemRecommended()
	{
		return Arrays.asList(combatGear, goodFood, runRestoreItems, antipoison, prayerPotions, oneClickTeleports, papyrus, charcoal, crumbleUndead);
	}

	@Override
	public List<String> getCombatRequirements()
	{
		List<String> req = new ArrayList<>();
		req.add("Nazastarool (Level 91, 68, 93). Safespotting is possible.");
		req.add("(Many) Undead Ones (Level 61-73)");
		req.add("Jogre (Level 53) (You will run past these, fighting not required)");
		return req;
	}
}
