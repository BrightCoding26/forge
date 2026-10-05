package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * Incubate N (March of the Machine): create an Incubator token with N +1/+1 counters and
 * "{2}: Transform this token" into a Phyrexian artifact creature. Upstream has no AI for it,
 * and CannotPlayAi refused every spell and ability with an Incubate link: Sunfall, Eyes of
 * Gitaxias and Glistening Dawn were never cast, and Compleated Huntmaster and Ichor Drinker never
 * activated. The creatures that incubate from a trigger were accepted all along, and the
 * transform is SetStateAi's. See GatedFallbackAi.
 */
public class IncubateAi extends GatedFallbackAi {

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        // Asked only of an Incubate that heads its spell or ability; a rider -- Sunfall's, whose
        // N counts creatures not exiled yet -- is left to the rest of the spell.
        if (AiController.fixesCastVetoes(ai)
                && AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParamOrDefault("Amount", "1"), sa) <= 0) {
            // An Incubator with no counters transforms into a 0/0 that dies.
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return super.checkApiLogic(ai, sa);
    }
}
