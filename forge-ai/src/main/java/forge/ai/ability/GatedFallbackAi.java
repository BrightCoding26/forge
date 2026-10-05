package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/**
 * For an API upstream gives no AI. SpellApiToAi falls back to CannotPlayAi for those, which
 * refuses an ability and every sub-ability, so any spell with such a link is never cast; it
 * does not override doTriggerNoCost, so a trigger whose root is the API was accepted all along.
 *
 * With AiController#fixesCastVetoes off, this is CannotPlayAi exactly, so runs made without the
 * flag stay reproducible: every hook below either refuses as CannotPlayAi did or defers to the
 * base class it inherited. With it on, the API is accepted -- a subclass for an effect that only
 * ever benefits its controller -- and a subclass adds what the effect needs beyond that.
 */
public abstract class GatedFallbackAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        if (!AiController.fixesCastVetoes(ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return super.canPlay(ai, sa);
    }

    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        // An activated ability that is neither a loyalty ability nor sorcery-speed is a mana sink:
        // Inspired Tethermage, Theorist's Sanctum, Campus Crier from the graveyard, Compleated
        // Huntmaster. Use it at the end of the opponent's turn, when the mana would otherwise go
        // unused and what it makes cannot be attacked or removed at sorcery speed first.
        if (AiController.fixesCastVetoes(ai) && sa.isActivatedAbility() && !sa.isPwAbility()
                && !isSorcerySpeed(sa, ai)) {
            return ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn().equals(ai);
        }
        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if (!AiController.fixesCastVetoes(ai)) {
            return super.checkApiLogic(ai, sa);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        if (!AiController.fixesCastVetoes(ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (!AiController.fixesCastVetoes(ai)) {
            return super.doTriggerNoCost(ai, sa, mandatory);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message,
            Map<String, Object> params) {
        if (!AiController.fixesCastVetoes(player)) {
            return super.confirmAction(player, sa, mode, message, params);
        }
        return true;
    }
}
