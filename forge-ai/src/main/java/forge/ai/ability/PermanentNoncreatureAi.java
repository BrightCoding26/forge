package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiCardMemory;
import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.AiProfileUtil;
import forge.ai.AiProps;
import forge.ai.ComputerUtilAbility;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends PermanentAi {

    /**
     * A noncreature permanent with flash only ever met PermanentAi's "not in your own Main 1"
     * rule, so it went down at the first moment the AI could pay: its own upkeep, before the
     * draw and the land drop. The Wandering Emperor was flashed in there, made a Samurai and
     * died to the next attack (3 traced games of 4 against isshin-aggro). Creatures with flash
     * already wait for a better window -- the opponent's end step, a combat where a blocker
     * is needed, or a response for an ETB -- and nothing in that logic is creature-specific,
     * so noncreatures now use it too.
     *
     * Waiting alone made it worse: the AI spent its mana on its own turn and reached the
     * opponent's end step with too little, so the card was never cast (0 of 4 games). So on
     * its own turn it now holds the mana for the opponent's turn, the way Fog effects do --
     * the same mana the upkeep cast spent -- once per turn, and releases it when the window
     * comes. The hold is skipped while a Fog has claimed that reservation, and only a held
     * Fog plan stops it from releasing it.
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Card card = sa.getHostCard();
        if (card.hasKeyword(Keyword.FLASH) && !sa.isCastFromPlayEffect() && AiController.fixesCastVetoes(ai)
                && AiProfileUtil.getBoolProperty(ai, AiProps.FLASH_ENABLE_ADVANCED_LOGIC)) {
            final boolean noFogPlan = AiCardMemory.isMemorySetEmpty(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT);
            if (PermanentCreatureAi.doAdvancedFlashLogic(card, ai, sa)) {
                if (!ph.isPlayerTurn(ai) && noFogPlan) {
                    AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK);
                }
                return true;
            }
            if (ph.isPlayerTurn(ai) && noFogPlan && !card.isCommander()
                    && AiCardMemory.isMemorySetEmpty(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK)) {
                ((PlayerControllerAi) ai.getController()).getAi().reserveManaSources(sa, PhaseType.COMBAT_DECLARE_BLOCKERS, true);
            }
            return false;
        }
        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    /**
     * The rest of the logic not covered by the canPlayAI template is defined
     * here
     */
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        AiAbilityDecision decision = super.checkApiLogic(ai, sa);
        if (!decision.willingToPlay()) {
            return decision;
        }

        final Card host = sa.getHostCard();
        final String sourceName = ComputerUtilAbility.getAbilitySourceName(sa);
        final Game game = ai.getGame();

        // Check for valid targets before casting
        if (host.hasSVar("OblivionRing")) {
            // TODO: only the "may" case wouldn't fail checkETBEffects - replace with NeedsToPlay SVar?
            SpellAbility effectExile = AbilityFactory.getAbility(host.getSVar("TrigExile"), host);
            final ZoneType origin = ZoneType.listValueOf(effectExile.getParamOrDefault("Origin", "Battlefield")).get(0);
            effectExile.setActivatingPlayer(ai);
            CardCollection targets = CardLists.getTargetableCards(game.getCardsIn(origin), effectExile);
            if (sourceName.equals("Suspension Field")
                    || sourceName.equals("Detention Sphere")) {
                // existing "exile until leaves" enchantments only target opponent's permanents
                targets = CardLists.filterControlledBy(targets, ai.getOpponents());
            }
            if (targets.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
        }
        return decision;
    }
}
