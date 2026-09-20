package forge.ai.ability;

import forge.ai.*;
import forge.card.CardStateName;
import forge.card.CardTypeView;
import forge.game.Game;
import forge.game.GameType;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.spellability.SpellPermanent;
import forge.game.zone.ZoneType;
import forge.util.IterableUtil;
import forge.util.MyRandom;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PlayAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        final String logic = sa.getParamOrDefault("AILogic", "");

        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        // don't use this as a response (ReplaySpell logic is an exception, might be called from a subability
        // while the trigger is on stack)
        //
        // Tried and reverted: also exempting `sa.getRootAbility().isTrigger()`, on the theory
        // that a Play resolving inside a trigger is not a response. It changed nothing --
        // Gandalf, Party Guest's free casts were identical, 5 of 28 triggers before and
        // after over the same twelve seeded games -- because the trigger is already off the
        // stack by the time its sub-ability runs. See the note in chooseSingleCard for where
        // the real constraint turned out to be.
        if (!game.getStack().isEmpty() && !"ReplaySpell".equals(logic)) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        if (game.getRules().hasAppliedVariant(GameType.MoJhoSto) && source.getName().equals("Jhoira of the Ghitu Avatar")) {
            // Additional logic for MoJhoSto:
            // Do not activate Jhoira too early, usually there are few good targets
            AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
            int numLandsForJhoira = aic.getIntProperty(AiProps.MOJHOSTO_NUM_LANDS_TO_ACTIVATE_JHOIRA);
            int chanceToActivateInst = 100 - aic.getIntProperty(AiProps.MOJHOSTO_CHANCE_TO_USE_JHOIRA_COPY_INSTANT);
            if (ai.getLandsInPlay().size() < numLandsForJhoira) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            // Don't spam activate the Instant copying ability all the time to give the AI a chance to use other abilities
            // Can probably be improved, but as random as MoJhoSto already is, probably not a huge deal for now
            if ("Instant".equals(sa.getParam("AnySupportedCard")) && MyRandom.percentTrue(chanceToActivateInst)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        List<Card> cards = getPlayableCards(sa, ai);
        if (cards.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        }

        if ("ReplaySpell".equals(logic)) {
            if (ComputerUtil.targetPlayableSpellCard(ai, cards, sa, sa.hasParam("WithoutManaCost"), false)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        } else if (logic.startsWith("NeedsChosenCard")) {
            int minCMC = 0;
            if (sa.getPayCosts().getCostMana() != null) {
                minCMC = sa.getPayCosts().getTotalMana().getCMC();
            }
            cards = CardLists.filter(cards, CardPredicates.greaterCMC(minCMC));
            if (chooseSingleCard(ai, sa, cards, sa.hasParam("Optional"), null, null) != null) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        } else if ("WithTotalCMC".equals(logic)) {
            // Try to play only when there are more than three playable cards.
            if (cards.size() < 3)
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            if (sa.costHasManaX()) {
                int amount = ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger());
                if (amount < ComputerUtilCard.getBestAI(cards).getCMC())
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                int totalCMC = 0;
                for (Card c : cards) {
                    totalCMC += c.getCMC();
                }
                if (amount > totalCMC)
                    amount = totalCMC;
                sa.setXManaCostPaid(amount);
            }
        }

        if (source != null && source.hasKeyword(Keyword.HIDEAWAY) && source.hasExiledCard()) {
            // AI is not very good at playing non-permanent spells this way, at least yet
            // (might be possible to enable it for Sorceries in Main1/Main2 if target is available,
            // but definitely not for most Instants)
            Card rem = source.getExiledCards().getFirst();
            CardTypeView t = rem.getState(CardStateName.Original).getType();

            if (t.isPermanent() && !t.isLand()) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    /**
     * <p>
     * doTriggerAINoCost
     * </p>
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     *
     * @return a boolean.
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (sa.usesTargeting()) {
            if (!sa.hasParam("AILogic")) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            if ("ReplaySpell".equals(sa.getParam("AILogic"))) {
                boolean result = ComputerUtil.targetPlayableSpellCard(ai, getPlayableCards(sa, ai), sa, sa.hasParam("WithoutManaCost"), mandatory);
                return result ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            return checkApiLogic(ai, sa);
        }

        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player ai, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#chooseSingleCard(forge.game.player.Player, forge.card.spellability.SpellAbility, java.util.List, boolean)
     */
    @Override
    public Card chooseSingleCard(final Player ai, final SpellAbility sa, Iterable<Card> options,
            final boolean isOptional, Player targetedPlayer, Map<String, Object> params) {
        final CardStateName state;
        if (sa.hasParam("CastTransformed")) {
            state = CardStateName.Backside;
            options.forEach(c -> c.changeToState(CardStateName.Backside));
        } else {
            state = CardStateName.Original; 
        }

        Predicate<SpellAbility> validSA;
        if (sa.hasParam("ValidSA")) {
            validSA = SpellAbilityPredicates.isValid(sa.getParam("ValidSA").split(","), ai, sa.getHostCard(), sa);
        } else {
            validSA = null;
        }
        List<Card> tgtCards = CardLists.filter(options, c -> {
            // TODO needs to be aligned for MDFC along with getAbilityToPlay so the knowledge
            // of which spell was the reason for the choice can be used there
            for (SpellAbility s : AbilityUtils.getSpellsFromPlayEffect(c, ai, state, false, validSA)) {
                if (s.isLandAbility()) {
                    // might want to run some checks here but it's rare anyway
                    return true;
                }
                Spell spell = (Spell) s;
                if (params != null && params.containsKey("CMCLimit")) {
                    Integer cmcLimit = (Integer) params.get("CMCLimit");
                    if (spell.getPayCosts().getTotalMana().getCMC() > cmcLimit)
                        continue;
                }
                if (sa.hasParam("WithoutManaCost")) {
                    // Try to avoid casting instants and sorceries with X in their cost, since X will be assumed to be 0.
                    if (!(spell instanceof SpellPermanent)) {
                        if (spell.costHasManaX()) {
                            continue;
                        }
                    }

                    spell = (Spell) spell.copyWithNoManaCost();
                } else if (sa.hasParam("PlayCost")) {
                    Cost abCost;
                    if ("ManaCost".equals(sa.getParam("PlayCost"))) {
                        abCost = new Cost(c.getManaCost(), false);
                    } else {
                        abCost = new Cost(sa.getParam("PlayCost"), false);
                    }

                    spell = (Spell) spell.copyWithManaCostReplaced(spell.getActivatingPlayer(), abCost);
                }
                // -Dforge.debugPlayAi=1 prints what each Play effect was offered and what it
                // decided. Worth keeping: the two obvious explanations for a card like
                // Gandalf, Party Guest rarely firing -- the AI refusing to respond while the
                // stack is busy, and the AI judging a free spell by the bar it uses for one
                // it is paying for -- were both wrong, and both were only ruled out by
                // reading this. The answer was that the AI is usually offered *nothing*.
                //
                // Tried and reverted alongside it: treating a free cast from a trigger as
                // mandatory. That made it slightly worse, 4 of 28 against 5, so the
                // permissiveness was not the constraint either.
                AiPlayDecision freeCastDecision = ((PlayerControllerAi)ai.getController())
                        .getAi().canPlayFromEffectAI(spell, !(isOptional || sa.hasParam("Optional")), true);
                if (System.getProperty("forge.debugPlayAi") != null) {
                    System.out.println("PlayAiDebug: " + sa.getHostCard() + " considering "
                            + c.getName() + " -> " + freeCastDecision);
                }
                if (AiPlayDecision.WillPlay == freeCastDecision) {
                    // Before accepting, see if the spell has a valid number of targets (it should at this point).
                    // Proceeding past this point if the spell is not correctly targeted will result
                    // in "Failed to add to stack" error and the card disappearing from the game completely.
                    if (!spell.isTargetNumberValid() || !ComputerUtilCost.canPayCost(spell, ai, true)) {
                        // if we won't be able to pay the cost, don't choose the card
                        return false;
                    }
                    return true;
                }
            }
            return false;
        });

        if (sa.hasParam("CastTransformed")) {
            options.forEach(c -> c.changeToState(CardStateName.Original));
        }

        if (System.getProperty("forge.debugPlayAi") != null) {
            int offered = 0;
            for (Card ignored : options) {
                offered++;
            }
            System.out.println("PlayAiDebug: offered " + offered + " card(s), "
                    + tgtCards.size() + " acceptable, for " + sa.getHostCard());
        }
        final Card best = ComputerUtilCard.getBestAI(tgtCards);
        if (sa.usesTargeting() && !sa.isTargetNumberValid()) {
            sa.getTargets().add(best);
        }
        return best;
    }

    private static List<Card> getPlayableCards(SpellAbility sa, Player ai) {
        List<Card> cards = null;
        final Card source = sa.getHostCard();

        if (sa.usesTargeting()) {
            cards = CardUtil.getValidCardsToTarget(sa);
        } else if (!sa.hasParam("Valid")) {
            cards = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);
        }

        if (cards != null & sa.hasParam("ValidSA")) {
            final String valid[] = sa.getParam("ValidSA").split(",");
            final List<Card> invalid = cards.stream().filter(c -> !IterableUtil.any(AbilityUtils.getBasicSpellsFromPlayEffect(c, ai), SpellAbilityPredicates.isValid(valid, ai, source, sa))).collect(Collectors.toList());
            if (!invalid.isEmpty())
                cards.removeAll(invalid);
        }

        // Ensure that if a ValidZone is specified, there's at least something to choose from in that zone.
        if (sa.hasParam("ValidZone")) {
            cards = new CardCollection(AbilityUtils.filterListByType(ai.getGame().getCardsIn(ZoneType.listValueOf(sa.getParam("ValidZone"))),
                    sa.getParam("Valid"), sa));
        }
        // exclude own card
        cards.remove(source);
        return cards;
    }

}
