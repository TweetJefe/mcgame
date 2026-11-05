package game.mechanics;

import game.heroes.HeroActionContext;
import game.state.PieceState;
import model.Position;

/** Состояние до гибели и подготовленный контекст после неё, включая предыдущие реакции. */
public record DeathContext(HeroActionContext context, PieceState victim, long killerId, Position square) {}
