package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;

/**
 * Empower Jace N (Reality Fracture): put N loyalty counters on a Jace token you control, first
 * creating a blue Jace planeswalker token with "-1: Surveil 1" and "-3: Draw a card" if you
 * control none.
 *
 * Upstream has no AI for it, and SpellApiToAi falls back to CannotPlayAi, which refuses every
 * spell with an Empower link and every Empower ability: No Admittance, Violent Echoes and the
 * other spells with an Empower rider were never cast, and Inspired Tethermage, Campus Crier,
 * Theorist's Sanctum and Jace, Reality Sculptor's +1 were never activated. A triggered Empower
 * was never refused -- CannotPlayAi does not override doTriggerNoCost, and the base accepts it
 * -- so the creatures and Ways that empower as they enter were cast all along.
 *
 * With AiController#fixesCastVetoes off, this is CannotPlayAi exactly, so runs made without the
 * flag stay reproducible. With it on, Empower is accepted: it only ever adds loyalty to a Jace
 * token its controller controls, so the link has nothing to refuse, and the rest of the spell
 * decides the cast.
 */
public class EmpowerAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        if (!AiController.fixesCastVetoes(ai)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return super.canPlay(ai, sa);
    }

    @Override
    protected boolean checkPhaseRestrictions(Player ai, SpellAbility sa, PhaseHandler ph) {
        // An instant-speed Empower that is not a loyalty ability is a mana sink: Inspired
        // Tethermage, Theorist's Sanctum, Campus Crier from the graveyard. Use it at the end of
        // the opponent's turn, when the mana would otherwise go unused and the token cannot be
        // attacked before its controller gets to activate it.
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
    protected Card chooseSingleCard(Player ai, SpellAbility sa, Iterable<Card> options, boolean isOptional,
            Player targetedPlayer, Map<String, Object> params) {
        if (!AiController.fixesCastVetoes(ai)) {
            return super.chooseSingleCard(ai, sa, options, isOptional, targetedPlayer, params);
        }
        // There is more than one Jace token only after a token doubler; grow the strongest.
        return Aggregates.itemWithMax(options, Card::getCurrentLoyalty);
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
