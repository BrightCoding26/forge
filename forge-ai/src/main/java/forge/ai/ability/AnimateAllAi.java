package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class AnimateAllAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");

        if ("CreatureAdvantage".equals(logic) && !aiPlayer.getCreaturesInPlay().isEmpty()) {
            // TODO: improve this or implement a better logic for abilities like Oko, the Trickster ultimate
            for (Card c : aiPlayer.getCreaturesInPlay()) {
                if (ComputerUtilCard.doesCreatureAttackAI(aiPlayer, c)) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
        }

        if ("Always".equals(logic)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // Hour of Devastation: "all creatures lose indestructible, then 5 damage to each creature".
        // The keyword loss only sets up the sweep behind it, but refusing it here meant the sweep
        // was never weighed. Judge the spell by the sweep, with the logic that sweep uses when it
        // heads a spell: a rider's check would accept a wipe that kills nothing.
        final SpellAbility sub = sa.getSubAbility();
        if (logic.isEmpty() && sub != null && onlyRemovesKeywords(sa) && AiController.fixesCastVetoes(aiPlayer)) {
            final SpellAbilityAi subAi = SpellApiToAi.Converter.get(sub);
            if (subAi instanceof DamageAllAi damageAll) {
                return damageAll.checkApiLogic(aiPlayer, sub);
            }
            if (subAi instanceof DestroyAllAi destroyAll) {
                return destroyAll.checkApiLogic(aiPlayer, sub);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // Song of Freyalise: "until your next turn, creatures you control gain '{T}: Add one mana of
        // any color'". checkETBEffects asks each saga chapter whether the AI would choose it, and
        // refusing this one vetoed the saga. Giving only the AI's own creatures something costs it
        // nothing.
        if (!sa.hasParam("AILogic") && onlyAddsToOwnCreatures(sa) && AiController.fixesCastVetoes(aiPlayer)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return canPlay(aiPlayer, sa);
    }

    /** It takes keywords away and changes nothing else. */
    private static boolean onlyRemovesKeywords(final SpellAbility sa) {
        return sa.hasParam("RemoveKeywords") && !sa.hasParam("Power") && !sa.hasParam("Toughness")
                && !sa.hasParam("Types") && !sa.hasParam("Keywords") && !sa.hasParam("Abilities")
                && !sa.hasParam("Triggers") && !sa.hasParam("RemoveAllAbilities");
    }

    /** It affects only creatures its controller controls, and only adds to them. */
    private static boolean onlyAddsToOwnCreatures(final SpellAbility sa) {
        final String valid = sa.getParamOrDefault("ValidCards", "");
        return valid.startsWith("Creature") && valid.contains("YouCtrl")
                && (sa.hasParam("Abilities") || sa.hasParam("Keywords") || sa.hasParam("Triggers"))
                && !sa.hasParam("RemoveKeywords") && !sa.hasParam("RemoveAllAbilities")
                && !sa.hasParam("Power") && !sa.hasParam("Toughness") && !sa.hasParam("Types")
                && !sa.hasParam("RemoveCreatureTypes");
    }

}
