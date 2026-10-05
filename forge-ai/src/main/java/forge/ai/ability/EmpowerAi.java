package forge.ai.ability;

import java.util.Map;

import forge.ai.AiController;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Empower Jace N (Reality Fracture): put N loyalty counters on a Jace token you control, first
 * creating a blue Jace planeswalker token with "-1: Surveil 1" and "-3: Draw a card" if you
 * control none.
 *
 * Upstream has no AI for it. Under CannotPlayAi, No Admittance, Violent Echoes and the other
 * spells with an Empower rider were never cast, and Inspired Tethermage, Campus Crier,
 * Theorist's Sanctum and Jace, Reality Sculptor's +1 were never activated; the creatures and
 * Ways that empower as they enter were cast all along. Empower only ever adds loyalty to a Jace
 * token its controller controls, so with fixesCastVetoes on the link has nothing to refuse, and
 * the rest of the spell decides the cast. See GatedFallbackAi.
 */
public class EmpowerAi extends GatedFallbackAi {

    @Override
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        if (!AiController.fixesCastVetoes(ai)) {
            return super.chooseSingleCard(ai, sa, options, isOptional, targetedPlayer, params);
        }
        // There is more than one Jace token only after a token doubler; grow the strongest.
        return Aggregates.itemWithMax(options, Card::getCurrentLoyalty);
    }
}
