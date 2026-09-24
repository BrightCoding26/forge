package forge.ai.ability;

import forge.ai.*;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

public class ImmediateTriggerAi extends SpellAbilityAi {
    // TODO: this class is largely reused from DelayedTriggerAi, consider updating

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        trigsa.setActivatingPlayer(ai);

        AiAbilityDecision result;
        if (trigsa instanceof AbilitySub) {
            result = SpellApiToAi.Converter.get(trigsa).chkDrawbackWithSubs(ai, (AbilitySub)trigsa);
        } else {
            AiPlayDecision decision = ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa);
            result = decision == AiPlayDecision.WillPlay ? new AiAbilityDecision(100, decision)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        // A reflexive trigger that remembers what its parent did -- "amass Orcs 2. When you do,
        // deal X damage, where X is the amassed Army's power" -- cannot be judged yet: nothing
        // has been remembered, so X reads 0 and the Execute finds no target. That refusal
        // vetoed Foray of Orcs and Grishnakh on every decision. Defer instead; the AI chooses
        // for real when the trigger fires, and a mandatory one with no target simply fizzles.
        if (!result.willingToPlay() && AiController.fixesCastVetoes(ai)
                && (sa.hasParam("RememberObjects") || sa.hasParam("RememberSVarAmount"))) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return result;
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // always add to stack, targeting happens after payment
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        trigsa.setActivatingPlayer(ai);

        return aic.doTrigger(trigsa, !"You".equals(sa.getParamOrDefault("OptionalDecider", "You"))) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        String logic = sa.getParamOrDefault("AILogic", "");
        if (logic.equals("Always")) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        SpellAbility trigsa = sa.getAdditionalAbility("Execute");
        if (trigsa == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (logic.equals("WeakerCreature")) {
            Card ownCreature = ComputerUtilCard.getWorstCreatureAI(ai.getCreaturesInPlay());
            if (ownCreature == null) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            int eval = ComputerUtilCard.evaluateCreature(ownCreature);
            boolean foundWorse = false;
            for (Card c : ai.getOpponents().getCreaturesInPlay()) {
                if (eval + 100 < ComputerUtilCard.evaluateCreature(c) ) {
                    foundWorse = true;
                    break;
                }
            }
            if (!foundWorse) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        trigsa.setActivatingPlayer(ai);
        return ((PlayerControllerAi)ai.getController()).getAi().canPlaySa(trigsa) == AiPlayDecision.WillPlay ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

}
