package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * LookAt: the activator looks at cards, typically a face-down creature. Upstream has no AI for
 * it. The AI makes no use of what it is shown, so an ability that only looks -- Smoke Teller's,
 * the one Brawl-legal card -- is still never activated; with fixesCastVetoes on, a LookAt
 * riding on something else no longer vetoes it. See GatedFallbackAi.
 */
public class LookAtAi extends GatedFallbackAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if (AiController.fixesCastVetoes(ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.DoesntImpactGame);
        }
        return super.checkApiLogic(ai, sa);
    }
}
