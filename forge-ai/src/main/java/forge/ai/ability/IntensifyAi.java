package forge.ai.ability;

/**
 * Intensify (Alchemy): perpetually increase the intensity of the AI's own cards. Upstream has no
 * AI for it, and CannotPlayAi refused every spell or trigger with an Intensify rider:
 * teysa-midrange never cast its commander, Teysa of the Ghost Council, because her ETB token
 * carries one, and Colossal Chorus, Mycelic Ballad and the other Chorus spells were never cast.
 * A trigger that only intensifies was accepted all along. Intensity only ever grows the
 * controller's own cards, so with fixesCastVetoes on there is nothing to refuse. See
 * GatedFallbackAi.
 */
public class IntensifyAi extends GatedFallbackAi {
}
