package forge.ai.ability;

import java.util.List;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Which of the AI's cards are about to be lost, for the effects that exist to save them: phasing
 * out, "when this dies, return it", a blink, team indestructible. RegenerateAi's two tests --
 * what the top of the stack will destroy or kill, and once blockers are declared, which
 * combatants will die in this combat -- reused for cards that had no AI of their own.
 */
final class Threatened {
    private Threatened() {
    }

    /** The AI's cards among {@code candidates} that the stack or the current combat will destroy, best first among creatures. */
    static CardCollection among(final Player ai, final SpellAbility sa, final Iterable<Card> candidates) {
        final CardCollection out = new CardCollection();
        final List<GameObject> onStack = ai.getGame().getStack().isEmpty() ? List.of()
                : ComputerUtil.predictThreatenedObjects(ai, sa, true);
        final Combat combat = ai.getGame().getCombat();
        final boolean blocked = combat != null
                && ai.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS);
        for (final Card c : candidates) {
            if (!c.getController().equals(ai)) {
                continue;
            }
            if (onStack.contains(c) || (blocked && c.isCreature()
                    && ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat))) {
                out.add(c);
            }
        }
        if (out.allMatch(Card::isCreature)) {
            ComputerUtilCard.sortByEvaluateCreature(out);
        }
        return out;
    }
}
