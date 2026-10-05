package forge.ai.ability;

/**
 * Recruit (The Hobbit, Alchemy): draw a card, then discard a card; if a nonland card was
 * discarded, create a 1/1 Human Soldier. Upstream has no AI for it, and CannotPlayAi refused
 * Sound the Trumpets, the one Brawl-legal spell with a Recruit rider; the nine cards that recruit
 * from a trigger were accepted all along. A loot never costs a card, so with fixesCastVetoes on
 * there is nothing to refuse, and the discard is chosen by the AI's usual discard logic. See
 * GatedFallbackAi.
 */
public class RecruitAi extends GatedFallbackAi {
}
