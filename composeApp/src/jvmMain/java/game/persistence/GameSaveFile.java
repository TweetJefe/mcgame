package game.persistence;

import game.heroes.faun.FaunAbility;
import game.heroes.ares.AresAbility;
import game.heroes.clownganger.ClowngangerAbility;
import game.heroes.moleminer.MoleminerAbility;
import game.heroes.zeus.ZeusAbility;
import game.heroes.chronomlin.ChronomlinAbility;
import game.heroes.kikimora.KikimoraAbility;

import game.core.Game;
import game.draft.*;
import game.summoning.*;
import game.heroes.izba.IzbaRules;
import game.heroes.ginseng.GinsengRules;
import game.heroes.faun.FaunRules;
import game.heroes.ares.AresRules;
import game.heroes.Abilities;
import game.heroes.bolotnik.BolotnikRules;
import game.terrain.*;
import game.economy.*;
import game.heroes.bountycrow.BountyCrowRules;
import game.core.SnapshotValidator;
import game.heroes.HeroType;
import game.heroes.HeroRules;
import game.heroes.lumeshard.LumeshardRules;
import game.state.SquareColor;
import game.heroes.pudge.PudgeRules;
import game.history.GameEvent;
import game.history.MadeMove;
import game.moves.MoveKind;
import game.moves.Promotion;
import game.state.CastlingRights;
import game.state.ClockSnapshot;
import game.state.GameMode;
import game.state.GameOutcome;
import game.state.GameSnapshot;
import game.state.ItemSlot;
import game.state.PieceState;
import game.state.Termination;
import game.state.TurnState;

import model.*;
import model.figures.*;
import org.w3c.dom.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.LongSupplier;

/**
 * Versioned XML data, never Java object deserialization. No arbitrary class names are loaded.
 */
public final class GameSaveFile {
    public static final int FORMAT_VERSION = 14;
    public static final int MAX_BYTES = 16 * 1024 * 1024;

    private GameSaveFile() {
    }

    /**
     * Prepare bytes before touching the destination, then replace it using a sibling temporary file.
     */
    public static void save(Path path, Game game) throws IOException {
        byte[] bytes = encode(game.getSnapshot());
        Path target = path.toAbsolutePath().normalize(), parent = target.getParent();
        Path temporary = Files.createTempFile(parent, ".moba-save-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Game load(Path path) throws IOException {
        return load(path, System::nanoTime);
    }

    public static Game load(Path path, LongSupplier time) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            byte[] bytes = in.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Файл сохранения превышает 16 МиБ");
            return Game.fromSnapshot(decode(bytes), time);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Некорректное сохранение: " + ex.getMessage(), ex);
        }
    }

    public static byte[] encode(GameSnapshot s) throws IOException {
        try {
            SnapshotValidator.validate(s);
            boolean extended = !s.lumeshardRules().equals(LumeshardRules.prototype()) || s.pieces().values().stream()
                    .anyMatch(piece -> piece.hero() == HeroType.LUMESHARD_BISHOP || piece.isBlinded());
            boolean kikiExtension=!s.heroRules().kikimora().equals(game.heroes.kikimora.KikimoraRules.prototype()) || s.pieces().values().stream().anyMatch(p->p.hero()==HeroType.KIKIMORA_BISHOP || p.isPotted()) || s.events().stream().anyMatch(e->Set.of(GameEvent.Kind.YARN,GameEvent.Kind.CLAY_POT,GameEvent.Kind.YARN_STUN).contains(e.kind()) || e.copiedAbility()==KikimoraAbility.YARN || e.copiedAbility()==KikimoraAbility.CLAY_POT) || (s.draft()!=null && s.draft().rules().catalogVersion()>9);
            boolean chronoExtension=kikiExtension || !s.heroRules().chronomlin().equals(game.heroes.chronomlin.ChronomlinRules.prototype()) || s.pieces().values().stream().anyMatch(p->p.hero()==HeroType.CHRONOMLIN_PAWN) || s.events().stream().anyMatch(e->e.kind()==GameEvent.Kind.GRAPPLE || e.kind()==GameEvent.Kind.EVOLVE || e.copiedAbility()==ChronomlinAbility.GRAPPLE || e.copiedAbility()==ChronomlinAbility.EVOLVE) || (s.draft()!=null && s.draft().rules().catalogVersion()>8);
            boolean zeusExtension=chronoExtension || !s.heroRules().zeus().equals(game.heroes.zeus.ZeusRules.prototype()) || s.pieces().values().stream().anyMatch(p->p.hero()==HeroType.ZEUS_ROOK) || !s.lightning().equals(game.heroes.zeus.LightningState.empty()) || s.events().stream().anyMatch(e->e.copiedAbility()==ZeusAbility.CHAIN_LIGHTNING || e.copiedAbility()==ZeusAbility.THUNDERSTORM || e.kind()==GameEvent.Kind.CHAIN_LIGHTNING || e.kind()==GameEvent.Kind.THUNDERSTORM) || (s.draft()!=null && s.draft().rules().catalogVersion()>7);
            boolean minerExtension=zeusExtension || !s.heroRules().moleminer().equals(game.heroes.moleminer.MoleminerRules.prototype()) || s.pieces().values().stream().anyMatch(p->p.hero()==HeroType.MOLEMINER_PAWN || p.status()==PieceState.Status.DESTROYED) || !s.mines().equals(game.heroes.moleminer.MineState.empty()) || (s.draft()!=null && s.draft().rules().catalogVersion()>6);
            boolean clownExtension=minerExtension || !s.heroRules().clownganger().equals(game.heroes.clownganger.ClowngangerRules.prototype()) || s.pieces().values().stream().anyMatch(p->p.hero()==HeroType.CLOWNGANGER_KNIGHT || p.hero()==HeroType.ILLUSION || p.isBoxed()) || s.events().stream().anyMatch(e->Set.of(GameEvent.Kind.BOX,GameEvent.Kind.ILLUSION,GameEvent.Kind.ILLUSION_HIT).contains(e.kind())) || (s.draft()!=null && s.draft().rules().catalogVersion()>5);
            boolean aresExtension =clownExtension || !s.heroRules().ares().equals(AresRules.prototype()) || s.pieces().values().stream().anyMatch(p -> p.hero() == HeroType.ARES_BISHOP || p.isStunned())
                    || s.events().stream().anyMatch(e -> e.kind() == GameEvent.Kind.SPEAR || e.kind() == GameEvent.Kind.STUN || e.kind() == GameEvent.Kind.ARES_REVENGE) || (s.draft() != null && s.draft().rules().catalogVersion() > 4);
            boolean faunExtension = aresExtension || !s.heroRules().faun().equals(FaunRules.prototype()) || s.pieces().values().stream().anyMatch(p -> p.hero() == HeroType.FAUN_KNIGHT)
                    || s.events().stream().anyMatch(e -> e.kind() == GameEvent.Kind.CLOUD || e.kind() == GameEvent.Kind.INVISIBILITY) || (s.draft() != null && s.draft().rules().catalogVersion() > 3);
            boolean spellExtension = faunExtension || !s.heroRules().ginseng().equals(GinsengRules.prototype()) || s.pieces().values().stream().anyMatch(p -> p.hero() == HeroType.GINSENG_BISHOP)
                    || s.events().stream().anyMatch(e -> e.borrowed() || e.copiedAbility() != null || e.kind() == GameEvent.Kind.DISPLACE || e.kind() == GameEvent.Kind.COPY_SPELL)
                    || (s.draft() != null && s.draft().rules().catalogVersion() > 2);
            boolean summonExtension = spellExtension || s.summons().enabled() || !s.heroRules().izba().equals(IzbaRules.prototype()) || (s.draft() != null && s.draft().rules().catalogVersion() > 1);
            boolean draftExtension = s.draft() != null;
            boolean terrainExtension = summonExtension || draftExtension || !s.terrain().cells().isEmpty() || !s.bolotnikRules().equals(BolotnikRules.prototype())
                    || s.pieces().values().stream().anyMatch(piece -> piece.hero() == HeroType.BOLOTNIK_KNIGHT);
            boolean economyExtension = terrainExtension || s.economy().enabled() || !s.crowRules().equals(BountyCrowRules.prototype()) || !s.heroRules().gold().equals(GoldRules.prototype());
            int formatVersion = kikiExtension ? 14 : chronoExtension ? 13 : zeusExtension ? 12 : minerExtension ? 11 : clownExtension ? 10 : aresExtension ? 9 : faunExtension ? 8 : spellExtension ? 7 : summonExtension ? 6 : draftExtension ? 5 : terrainExtension ? 4 : economyExtension ? 3 : extended ? 2 : 1;
            extended = formatVersion >= 2;
            Document doc = factory().newDocumentBuilder().newDocument();
            Element root = doc.createElement("moba-chess");
            doc.appendChild(root);
            attrs(root, "formatVersion", formatVersion, "stateVersion", s.version(), "mode", s.mode(), "active", s.active(),
                    "turn", s.turn().number(), "moveUsed", s.turn().moveUsed(), "abilityUsed", s.turn().abilityUsed(),
                    "halfmoveClock", s.halfmoveClock(), "enPassant", cell(s.enPassantTarget()), "drawOffer", s.drawOffer());
            child(root, "rules", "hookCooldown", s.pudgeRules().hookCooldown(), "ultimateCaptures", s.pudgeRules().ultimateCaptures(),
                    "hookDiagonals", s.pudgeRules().hookDiagonals(), "swallowDiagonals", s.pudgeRules().swallowDiagonals());
            if (extended) child(root, "lumeshardRules", "laserCooldown", s.lumeshardRules().laserCooldown(),
                    "blindnessTurns", s.lumeshardRules().blindnessTurns(), "ultimateCaptures", s.lumeshardRules().ultimateCaptures());
            if (economyExtension) {
                child(root, "crowRules", "bountyCooldown", s.crowRules().bountyCooldown(), "bountyTurns", s.crowRules().bountyTurns(), "ultimateCaptures", s.crowRules().ultimateCaptures());
                GoldRules gold = s.heroRules().gold();
                child(root, "goldRules", "pawn", gold.pawn(), "heroPawn", gold.heroPawn(), "minor", gold.minor(), "rook", gold.rook(), "queen", gold.queen(), "neutral", gold.neutral());
                writeEconomy(child(root, "economy"), s.economy());
            }
            if (terrainExtension) {
                child(root, "bolotnikRules", "mudCooldown", s.bolotnikRules().mudCooldown(), "mudTurns", s.bolotnikRules().mudTurns(), "ultimateCaptures", s.bolotnikRules().ultimateCaptures());
                Element terrain = child(root, "terrain");
                for (var cell : s.terrain().cells().entrySet().stream().sorted(Comparator.comparingInt(e -> e.getKey().getRow() * 8 + e.getKey().getColumn())).toList())
                    child(terrain, "mud", "square", cell(cell.getKey()), "ownerId", cell.getValue().ownerId(), "kind", cell.getValue().kind(), "expiresAfterTurn", cell.getValue().expiresAfterTurn());
            }
            if (draftExtension) {
                Element draft = child(root, "draft", "bansPerSide", s.draft().rules().bansPerSide(), "maxPicksPerSide", s.draft().rules().maxPicksPerSide(), "catalogVersion", s.draft().rules().catalogVersion() == 1 ? null : s.draft().rules().catalogVersion());
                for (DraftEvent event : s.draft().events())
                    child(draft, "action", "sequence", event.sequence(), "kind", event.kind(),
                            "side", event.side(), "hero", event.hero(), "square", cell(event.square()));
            }
            if (kikiExtension)child(root,"kikimoraRules","yarnCooldown",s.heroRules().kikimora().yarnCooldown(),"ultimateCaptures",s.heroRules().kikimora().ultimateCaptures());
            if(chronoExtension)child(root,"chronomlinRules","grappleCooldown",s.heroRules().chronomlin().grappleCooldown(),"ultimateCaptures",s.heroRules().chronomlin().ultimateCaptures());
            if(zeusExtension)child(root,"zeusRules","lightningCooldown",s.heroRules().zeus().lightningCooldown(),"chargeTurns",s.heroRules().zeus().chargeTurns(),"ultimateCaptures",s.heroRules().zeus().ultimateCaptures());
            if(minerExtension)child(root,"moleminerRules","mineCooldown",s.heroRules().moleminer().mineCooldown(),"ultimateCaptures",s.heroRules().moleminer().ultimateCaptures());
            if(clownExtension)child(root,"clowngangerRules","boxCooldown",s.heroRules().clownganger().boxCooldown(),"boxTurns",s.heroRules().clownganger().boxTurns(),"illusionTurns",s.heroRules().clownganger().illusionTurns(),"ultimateCaptures",s.heroRules().clownganger().ultimateCaptures());
            if(aresExtension)
                child(root, "aresRules", "spearCooldown", s.heroRules().ares().spearCooldown(), "revengeCaptures", s.heroRules().ares().revengeCaptures());
            if (faunExtension)
                child(root, "faunRules", "cloudCooldown", s.heroRules().faun().cloudCooldown(), "cloudTurns", s.heroRules().faun().cloudTurns(), "invisibilityTurns", s.heroRules().faun().invisibilityTurns(), "ultimateCaptures", s.heroRules().faun().ultimateCaptures());
            if (spellExtension)
                child(root, "ginsengRules", "displacementCooldown", s.heroRules().ginseng().displacementCooldown(), "ultimateCaptures", s.heroRules().ginseng().ultimateCaptures());
            if (summonExtension) {
                child(root, "izbaRules", "ultimateCaptures", s.heroRules().izba().ultimateCaptures(), "lifetimeTurns", s.heroRules().izba().lifetimeTurns());
                Element summons = child(root, "summons", "enabled", s.summons().enabled(), "randomState", s.summons().randomState());
                for (var r : s.summons().records().values().stream().sorted(Comparator.comparingLong(SummonRecord::id)).toList())
                    child(summons, "origin", "id", r.id(), "sourceId", r.sourceId(), "bornTurn", r.bornTurn(), "transformsAfterTurn", r.transformsAfterTurn(), "bornAt", cell(r.bornAt()), "cause", r.cause(), "transformed", r.transformed());
            }
            child(root, "castling", "whiteKingSide", s.castlingRights().whiteKingSide(), "whiteQueenSide", s.castlingRights().whiteQueenSide(),
                    "blackKingSide", s.castlingRights().blackKingSide(), "blackQueenSide", s.castlingRights().blackQueenSide());
            writeClock(child(root, "clock"), s.clock());
            child(root, "outcome", "reason", s.outcome().reason(), "winner", s.outcome().winner());
            Element pieces = child(root, "pieces");
            for (PieceState piece : s.pieces().values().stream().sorted(Comparator.comparingLong(PieceState::id)).toList()) {
                Element entry = child(pieces, "piece", "id", piece.id(), "type", piece.type().getSimpleName(), "color", piece.color(),
                        "square", cell(piece.position()), "status", piece.status(), "hero", piece.hero(), "cooldown", piece.cooldown(),
                        "ultimateCharge", piece.ultimateCharge(), "storedPieceId", piece.storedPieceId());
                if (aresExtension) attrs(entry, "stunnedThroughTurn", piece.stunnedThroughTurn());
                if(clownExtension)attrs(entry,"boxedThroughTurn",piece.boxedThroughTurn());
                if(kikiExtension)attrs(entry,"potted",piece.potted());
                if (extended) attrs(entry, "blindTurns", piece.blindTurns(), "startingColor", piece.startingColor());
                for (ItemSlot slot : piece.inventory())
                    child(entry, "slot", "itemId", slot.itemId(), "charges", slot.charges(), "cooldown", slot.cooldown());
            }
            Element repetitions = child(root, "repetitions");
            for (var entry : new TreeMap<>(s.repetitions()).entrySet())
                child(repetitions, "position", "key", entry.getKey(), "count", entry.getValue());
            Element moves = child(root, "moves");
            for (MadeMove move : s.moves()) {
                Element entry = child(moves, "move", "figure", move.getFigure(), "color", move.getColor(), "from", cell(move.getFrom()),
                        "to", cell(move.getTo()), "kind", move.getKind(), "promotion", move.getPromotion(), "capturedFigure", move.getCapturedFigure(),
                        "captureSquare", cell(move.getCaptureSquare()), "ply", move.getPly());
                if (move.getClock() != null) writeClock(child(entry, "clock"), move.getClock());
            }
            Element events = child(root, "events");
            for (GameEvent event : s.events()) {
                Element entry = child(events, "event", "sequence", event.sequence(), "turn", event.turn(), "kind", event.kind(),
                        "color", event.color(), "actorId", event.actorId(), "targetId", event.targetId(), "from", cell(event.from()),
                        "to", cell(event.to()), "message", event.message());
                if (spellExtension) attrs(entry, "borrowed", event.borrowed(), "copiedAbility", event.copiedAbility());
            }
            writeCaptures(child(root, "whiteCaptures", "points", s.whitePoints()), s.whiteCaptures());
            writeCaptures(child(root, "blackCaptures", "points", s.blackPoints()), s.blackCaptures());
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = tf.newTransformer();
            transformer.setOutputProperty("encoding", "UTF-8");
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            var output = new ByteArrayOutputStream();
            transformer.transform(new DOMSource(doc), new StreamResult(output));
            if (output.size() > MAX_BYTES) throw new IOException("Сохранение превышает 16 МиБ");
            return output.toByteArray();
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Не удалось подготовить сохранение: " + ex.getMessage(), ex);
        }
    }

    public static GameSnapshot decode(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length > MAX_BYTES) throw new IOException("Недопустимый размер сохранения");
        try {
            var builder = factory().newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException ex) throws SAXParseException {
                    throw ex;
                }

                public void error(SAXParseException ex) throws SAXParseException {
                    throw ex;
                }

                public void fatalError(SAXParseException ex) throws SAXParseException {
                    throw ex;
                }
            });
            Element root = builder.parse(new ByteArrayInputStream(bytes)).getDocumentElement();
            require(root.getTagName().equals("moba-chess"), "Unknown save format");
            int formatVersion = integer(root, "formatVersion");
            require(formatVersion >= 1 && formatVersion <= FORMAT_VERSION, "Unsupported save format version");
            LumeshardRules lume = LumeshardRules.prototype();
            if (formatVersion >= 2) {
                Element cfg = one(root, "lumeshardRules");
                lume = new LumeshardRules(integer(cfg, "laserCooldown"), integer(cfg, "blindnessTurns"), integer(cfg, "ultimateCaptures"));
            }
            BountyCrowRules crow = BountyCrowRules.prototype();
            GoldRules gold = GoldRules.prototype();
            EconomyState economy = EconomyState.disabled();
            if (formatVersion >= 3) {
                Element cfg = one(root, "crowRules"), prices = one(root, "goldRules");
                crow = new BountyCrowRules(integer(cfg, "bountyCooldown"), integer(cfg, "bountyTurns"), integer(cfg, "ultimateCaptures"));
                gold = new GoldRules(integer(prices, "pawn"), integer(prices, "heroPawn"), integer(prices, "minor"), integer(prices, "rook"), integer(prices, "queen"), integer(prices, "neutral"));
                economy = readEconomy(one(root, "economy"));
            }
            BolotnikRules bolotnik = BolotnikRules.prototype();
            TerrainState terrain = TerrainState.empty();
            if (formatVersion >= 4) {
                Element cfg = one(root, "bolotnikRules");
                bolotnik = new BolotnikRules(integer(cfg, "mudCooldown"), integer(cfg, "mudTurns"), integer(cfg, "ultimateCaptures"));
                var cells = new HashMap<Position, MudCell>();
                for (Element cell : children(one(root, "terrain"), "mud", 64))
                    require(cells.put(requiredPosition(cell, "square"), new MudCell(number(cell, "ownerId"), enumeration(cell, "kind", MudCell.Kind.class), number(cell, "expiresAfterTurn"))) == null, "Duplicate terrain cell");
                terrain = new TerrainState(cells);
            }
            DraftSnapshot draft = null;
            if (formatVersion == 5 || (formatVersion >= 6 && root.getElementsByTagName("draft").getLength() > 0)) {
                Element cfg = one(root, "draft");
                DraftRules draftRules = new DraftRules(integer(cfg, "bansPerSide"), integer(cfg, "maxPicksPerSide"), cfg.hasAttribute("catalogVersion") ? integer(cfg, "catalogVersion") : 1);
                var draftEvents = new ArrayList<DraftEvent>();
                for (Element action : children(cfg, "action", 100))
                    draftEvents.add(new DraftEvent(number(action, "sequence"), enumeration(action, "kind", DraftEvent.Kind.class),
                            enumeration(action, "side", FigureColor.class), optionalEnum(action, "hero", HeroType.class), position(action, "square")));
                require(formatVersion >=14 || draftRules.catalogVersion()<=9,"Kikimora catalog requires format 14");
                require(formatVersion>=13 || draftRules.catalogVersion()<=8,"Chronomlin catalog requires format 13");
                require(formatVersion>=12 || draftRules.catalogVersion()<=7,"Zeus catalog requires format 12");
                require(formatVersion>=11 || draftRules.catalogVersion()<=6,"Moleminer catalog requires format 11");
                require(formatVersion>=10 || draftRules.catalogVersion()<=5,"Clownganger catalog requires format 10");
                require(formatVersion>=9 || draftRules.catalogVersion() <= 4, "Ares catalog requires format 9");
                require(formatVersion >= 8 || draftRules.catalogVersion() <= 3, "Faun catalog requires format 8");
                require(formatVersion >= 7 || draftRules.catalogVersion() <= 2, "Ginseng catalog requires format 7");
                require(formatVersion >= 6 || draftRules.catalogVersion() == 1, "New hero catalog requires format 6");
                draft = Draft.replay(draftRules, draftEvents).getSnapshot();
                require(draft.isComplete(), "Game save contains an unfinished draft");
            }
            var kiki=game.heroes.kikimora.KikimoraRules.prototype();
            if(formatVersion>=14){Element cfg=one(root,"kikimoraRules");kiki=new game.heroes.kikimora.KikimoraRules(integer(cfg,"yarnCooldown"),integer(cfg,"ultimateCaptures"));}
            var chrono=game.heroes.chronomlin.ChronomlinRules.prototype();
            if(formatVersion>=13){Element cfg=one(root,"chronomlinRules");chrono=new game.heroes.chronomlin.ChronomlinRules(integer(cfg,"grappleCooldown"),integer(cfg,"ultimateCaptures"));}
            var zeus=game.heroes.zeus.ZeusRules.prototype();
            if(formatVersion>=12){Element cfg=one(root,"zeusRules");zeus=new game.heroes.zeus.ZeusRules(integer(cfg,"lightningCooldown"),integer(cfg,"chargeTurns"),integer(cfg,"ultimateCaptures"));}
            var miner=game.heroes.moleminer.MoleminerRules.prototype();
            if(formatVersion>=11){Element cfg=one(root,"moleminerRules");miner=new game.heroes.moleminer.MoleminerRules(integer(cfg,"mineCooldown"),integer(cfg,"ultimateCaptures"));}
            var clown=game.heroes.clownganger.ClowngangerRules.prototype();
            if(formatVersion>=10){Element cfg=one(root,"clowngangerRules");clown=new game.heroes.clownganger.ClowngangerRules(integer(cfg,"boxCooldown"),integer(cfg,"boxTurns"),integer(cfg,"illusionTurns"),integer(cfg,"ultimateCaptures"));
            }
            AresRules ares = AresRules.prototype();
            if (formatVersion >= 9) {
                Element cfg = one(root, "aresRules");
                ares = new AresRules(integer(cfg, "spearCooldown"), integer(cfg, "revengeCaptures"));
            }
            FaunRules faun = FaunRules.prototype();
            if (formatVersion >= 8) {
                Element cfg = one(root, "faunRules");
                faun = new FaunRules(integer(cfg, "cloudCooldown"), integer(cfg, "cloudTurns"), integer(cfg, "invisibilityTurns"), integer(cfg, "ultimateCaptures"));
            }
            GinsengRules ginseng = GinsengRules.prototype();
            if (formatVersion >= 7) {
                Element cfg = one(root, "ginsengRules");
                ginseng = new GinsengRules(integer(cfg, "displacementCooldown"), integer(cfg, "ultimateCaptures"));
            }
            SummonState summons = SummonState.empty();
            IzbaRules izba = IzbaRules.prototype();
            if (formatVersion >= 6) {
                Element config = one(root, "izbaRules"), ledger = one(root, "summons");
                izba = new IzbaRules(integer(config, "ultimateCaptures"), integer(config, "lifetimeTurns"));
                var origins = new HashMap<Long, SummonRecord>();
                for (Element entry : children(ledger, "origin", SummonState.MAX_IDENTITIES)) {
                    SummonRecord r = new SummonRecord(number(entry, "id"), number(entry, "sourceId"), number(entry, "bornTurn"), number(entry, "transformsAfterTurn"), requiredPosition(entry, "bornAt"), enumeration(entry, "cause", SummonRecord.Cause.class), bool(entry, "transformed"));
                    require(origins.put(r.id(), r) == null, "Duplicate summon origin");
                }
                summons = new SummonState(bool(ledger, "enabled"), number(ledger, "randomState"), origins);
            }
            GameMode mode = enumeration(root, "mode", GameMode.class);
            var rules = one(root, "rules");
            var rights = one(root, "castling");
            var outcome = one(root, "outcome");
            Map<Long, PieceState> pieces = new HashMap<>();
            Field factory = new Field(8);
            Map<Position, Figure> board = new HashMap<>();
            for (int row = 0; row < 8; row++)
                for (int col = 0; col < 8; col++) {
                    Position square = new Position(row, col);
                    board.put(square, factory.createFigure(Empty.class, square, FigureColor.NONE));
                }
            for (Element entry : children(one(root, "pieces"), "piece", formatVersion >= 6 ? SummonState.MAX_IDENTITIES : 32)) {
                var slots = new ArrayList<ItemSlot>();
                for (Element slot : children(entry, "slot", 3))
                    slots.add(new ItemSlot(optional(slot, "itemId"), integer(slot, "charges"), integer(slot, "cooldown")));
                PieceState piece = new PieceState(number(entry, "id"), type(text(entry, "type")), enumeration(entry, "color", FigureColor.class),
                        position(entry, "square"), enumeration(entry, "status", PieceState.Status.class), enumeration(entry, "hero", HeroType.class),
                        integer(entry, "cooldown"), integer(entry, "ultimateCharge"), slots, optionalLong(entry, "storedPieceId"),
                        formatVersion >= 2 ? integer(entry, "blindTurns") : 0, formatVersion >= 2 ? optionalEnum(entry, "startingColor", SquareColor.class) : null, formatVersion >= 9 ? number(entry, "stunnedThroughTurn") : 0,formatVersion>=10?number(entry,"boxedThroughTurn"):0,formatVersion>=14 && bool(entry,"potted"));
                require(formatVersion != 1 || (piece.hero() != HeroType.LUMESHARD_BISHOP && !entry.hasAttribute("blindTurns") && !entry.hasAttribute("startingColor")), "Lumeshard extension requires format 2");
                require(formatVersion >= 3 || piece.hero() != HeroType.BOUNTY_CROW_PAWN, "Crow requires format 3");
                require(formatVersion >= 4 || (piece.hero() != HeroType.BOLOTNIK_KNIGHT && piece.status() != PieceState.Status.HIDDEN_BOG), "Bolotnik requires format 4");
                require(formatVersion >= 6 || (piece.hero() != HeroType.IZBA_ROOK && piece.hero() != HeroType.BABA_YAGA_PAWN && piece.status() != PieceState.Status.REPLACED), "Summoning requires format 6");
                require(formatVersion>=14 || (piece.hero()!=HeroType.KIKIMORA_BISHOP && !entry.hasAttribute("potted")),"Kikimora requires format 14");
                require(formatVersion>=13 || (piece.hero()!=HeroType.CHRONOMLIN_PAWN && (piece.hero()!=HeroType.GINSENG_BISHOP || piece.type()==Bishop.class) && (piece.hero()!=HeroType.ILLUSION || piece.type()!=Rook.class)),"Chronomlin forms require format 13");
                require(formatVersion>=12 || piece.hero()!=HeroType.ZEUS_ROOK,"Zeus requires format 12");
                require(formatVersion>=11 || (piece.hero()!=HeroType.MOLEMINER_PAWN && piece.status()!=PieceState.Status.DESTROYED),"Moleminer requires format 11");
                require(formatVersion>=10 || (piece.hero()!=HeroType.CLOWNGANGER_KNIGHT && piece.hero()!=HeroType.ILLUSION && piece.status()!=PieceState.Status.DISSOLVED && !entry.hasAttribute("boxedThroughTurn")),"Clownganger requires format 10");
                require(formatVersion >= 9 || (piece.hero() != HeroType.ARES_BISHOP && !entry.hasAttribute("stunnedThroughTurn")), "Ares requires format 9");
                require(formatVersion >= 8 || piece.hero() != HeroType.FAUN_KNIGHT, "Faun requires format 8");
                require(formatVersion >= 7 || piece.hero() != HeroType.GINSENG_BISHOP, "Ginseng requires format 7");
                require(pieces.put(piece.id(), piece) == null, "Duplicate piece identity");
                if (piece.position() != null) {
                    require(board.get(piece.position()) instanceof Empty, "Duplicate occupied square");
                    board.put(piece.position(), factory.createFigure(piece.type(), piece.position(), piece.color()));
                }
            }
            Map<String, Integer> repetitions = new HashMap<>();
            for (Element entry : children(one(root, "repetitions"), "position", 100_000))
                require(repetitions.put(text(entry, "key"), integer(entry, "count")) == null, "Duplicate repetition key");
            var moves = new ArrayList<MadeMove>();
            for (Element entry : children(one(root, "moves"), "move", 100_000))
                moves.add(new MadeMove(text(entry, "figure"), enumeration(entry, "color", FigureColor.class), requiredPosition(entry, "from"),
                        requiredPosition(entry, "to"), enumeration(entry, "kind", MoveKind.class), optionalEnum(entry, "promotion", Promotion.class),
                        optional(entry, "capturedFigure"), position(entry, "captureSquare"), integer(entry, "ply"), readClock(one(entry, "clock"))));
            var events = new ArrayList<GameEvent>();
            for (Element entry : children(one(root, "events"), "event", 100_000)) {
                GameEvent event = new GameEvent(number(entry, "sequence"), number(entry, "turn"), enumeration(entry, "kind", GameEvent.Kind.class),
                        enumeration(entry, "color", FigureColor.class), optionalLong(entry, "actorId"), optionalLong(entry, "targetId"),
                        position(entry, "from"), position(entry, "to"), text(entry, "message"), formatVersion >= 7 ? optionalAbility(entry, "copiedAbility") : null, formatVersion >= 7 && bool(entry, "borrowed"));
                require(formatVersion >= 4 || !Set.of(GameEvent.Kind.MUD, GameEvent.Kind.AMBUSH, GameEvent.Kind.BOG_CAPTURE).contains(event.kind()), "Bolotnik events require format 4");
                require(formatVersion >= 6 || !Set.of(GameEvent.Kind.SUMMON_YAGA, GameEvent.Kind.YAGA_BIRTH, GameEvent.Kind.YAGA_MATURE, GameEvent.Kind.REPLACE).contains(event.kind()), "Summoning events require format 6");
                require(formatVersion >= 7 || (!entry.hasAttribute("borrowed") && !entry.hasAttribute("copiedAbility") && event.kind() != GameEvent.Kind.DISPLACE && event.kind() != GameEvent.Kind.COPY_SPELL), "Ginseng events require format 7");
                require(formatVersion >= 8 || (event.kind() != GameEvent.Kind.CLOUD && event.kind() != GameEvent.Kind.INVISIBILITY && event.copiedAbility() != FaunAbility.CLOUD && event.copiedAbility() != FaunAbility.INVISIBILITY), "Faun events require format 8");
                require(formatVersion >=14 || (!Set.of(GameEvent.Kind.YARN,GameEvent.Kind.CLAY_POT,GameEvent.Kind.YARN_STUN).contains(event.kind()) && event.copiedAbility()!=KikimoraAbility.YARN && event.copiedAbility()!=KikimoraAbility.CLAY_POT),"Kikimora events require format 14");
                require(formatVersion>=13 || (event.kind()!=GameEvent.Kind.GRAPPLE && event.kind()!=GameEvent.Kind.EVOLVE && event.copiedAbility()!=ChronomlinAbility.GRAPPLE && event.copiedAbility()!=ChronomlinAbility.EVOLVE),"Chronomlin events require format 13");
                require(formatVersion>=12 || (!Set.of(GameEvent.Kind.CHAIN_LIGHTNING,GameEvent.Kind.THUNDERSTORM,GameEvent.Kind.LIGHTNING_HIT,GameEvent.Kind.LIGHTNING_RESET,GameEvent.Kind.LIGHTNING_DISCHARGE,GameEvent.Kind.LIGHTNING_CAPTURE).contains(event.kind()) && event.copiedAbility()!=ZeusAbility.CHAIN_LIGHTNING && event.copiedAbility()!=ZeusAbility.THUNDERSTORM),"Zeus events require format 12");
                require(formatVersion>=11 || (!Set.of(GameEvent.Kind.PLANT_MINE,GameEvent.Kind.DETONATE,GameEvent.Kind.MINE_TRIGGER,GameEvent.Kind.MINE_CAPTURE,GameEvent.Kind.BLAST_AREA,GameEvent.Kind.BLAST_CAPTURE,GameEvent.Kind.BLAST_DEATH).contains(event.kind()) && event.copiedAbility()!=MoleminerAbility.PLANT_MINE && event.copiedAbility()!=MoleminerAbility.DETONATE),"Moleminer events require format 11");
                require(formatVersion>=10 || (!Set.of(GameEvent.Kind.BOX,GameEvent.Kind.ILLUSION,GameEvent.Kind.ILLUSION_HIT).contains(event.kind()) && event.copiedAbility()!=ClowngangerAbility.BOX && event.copiedAbility()!=ClowngangerAbility.ILLUSION),"Clownganger events require format 10");
                require(formatVersion>=9 || (!Set.of(GameEvent.Kind.SPEAR, GameEvent.Kind.STUN, GameEvent.Kind.ARES_REVENGE).contains(event.kind()) && event.copiedAbility() != AresAbility.SPEAR), "Ares events require format 9");
                events.add(event);
            }
            Element white = one(root, "whiteCaptures"), black = one(root, "blackCaptures");
            Set<String> sections = Set.of("rules", "castling", "clock", "outcome", "pieces", "repetitions", "moves", "events", "whiteCaptures", "blackCaptures", "lumeshardRules", "crowRules", "goldRules", "economy", "bolotnikRules", "terrain", "draft", "izbaRules", "summons", "ginsengRules", "faunRules", "aresRules", "clowngangerRules", "moleminerRules", "zeusRules", "chronomlinRules", "kikimoraRules");
            for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
                if (node instanceof Element entry)
                    require(sections.contains(entry.getTagName()) && (formatVersion >= 2 || !entry.getTagName().equals("lumeshardRules"))
                            && (formatVersion >= 3 || !Set.of("crowRules", "goldRules", "economy").contains(entry.getTagName())) && (formatVersion >= 4 || !Set.of("bolotnikRules", "terrain").contains(entry.getTagName())) && (formatVersion >= 5 || !entry.getTagName().equals("draft")) && (formatVersion >= 6 || !Set.of("izbaRules", "summons").contains(entry.getTagName())) && (formatVersion >= 7 || !entry.getTagName().equals("ginsengRules")) && (formatVersion >= 8 || !entry.getTagName().equals("faunRules")) && (formatVersion >= 9 || !entry.getTagName().equals("aresRules")) && (formatVersion>=10 || !entry.getTagName().equals("clowngangerRules")) && (formatVersion>=11 || !entry.getTagName().equals("moleminerRules")) && (formatVersion>=12 || !entry.getTagName().equals("zeusRules")) && (formatVersion>=13 || !entry.getTagName().equals("chronomlinRules")) && (formatVersion>=14 || !entry.getTagName().equals("kikimoraRules")), "Unknown save section: " + entry.getTagName());
            GameSnapshot s = new GameSnapshot(number(root, "stateVersion"), mode,
                    mode == GameMode.MOBA ? Field.fromMobaPosition(board) : Field.fromPosition(board), pieces,
                    new TurnState(number(root, "turn"), bool(root, "moveUsed"), bool(root, "abilityUsed")), enumeration(root, "active", FigureColor.class),
                    new CastlingRights(bool(rights, "whiteKingSide"), bool(rights, "whiteQueenSide"), bool(rights, "blackKingSide"), bool(rights, "blackQueenSide")),
                    position(root, "enPassant"), integer(root, "halfmoveClock"), repetitions, readClock(one(root, "clock")),
                    new GameOutcome(enumeration(outcome, "reason", Termination.class), enumeration(outcome, "winner", FigureColor.class)), moves, events,
                    number(white, "points"), number(black, "points"), readCaptures(white, factory), readCaptures(black, factory),
                    enumeration(root, "drawOffer", FigureColor.class), new HeroRules(new PudgeRules(integer(rules, "hookCooldown"), integer(rules, "ultimateCaptures"), bool(rules, "hookDiagonals"), bool(rules, "swallowDiagonals")), lume, crow, gold, bolotnik, izba, ginseng, faun, ares,clown,miner,zeus,chrono,kiki), economy, terrain, draft, summons);
            SnapshotValidator.validate(s);
            return s;
        } catch (Exception ex) {
            throw new IOException("Некорректное сохранение: " + ex.getMessage(), ex);
        }
    }

    private static void writeEconomy(Element root, EconomyState economy) {
        attrs(root, "enabled", economy.enabled(), "whiteHalves", economy.whiteHalves(), "blackHalves", economy.blackHalves());
        Element marks = child(root, "marks");
        for (BountyMark mark : economy.marks())
            child(marks, "mark", "sourceId", mark.sourceId(), "targetId", mark.targetId(), "expiresAfterTurn", mark.expiresAfterTurn());
        Element ledger = child(root, "ledger");
        for (GoldChange entry : economy.history())
            child(ledger, "change", "turn", entry.turn(), "kind", entry.kind(), "side", entry.side(),
                    "actorId", entry.actorId(), "victimId", entry.victimId(), "baseHalves", entry.baseHalves(), "bountyHalves", entry.bountyHalves(), "passiveHalves", entry.passiveHalves());
    }

    private static EconomyState readEconomy(Element root) {
        var marks = new ArrayList<BountyMark>();
        var ledger = new ArrayList<GoldChange>();
        for (Element mark : children(one(root, "marks"), "mark", 32))
            marks.add(new BountyMark(number(mark, "sourceId"), number(mark, "targetId"), number(mark, "expiresAfterTurn")));
        for (Element entry : children(one(root, "ledger"), "change", 100_000))
            ledger.add(new GoldChange(number(entry, "turn"), enumeration(entry, "kind", GoldChange.Kind.class), enumeration(entry, "side", FigureColor.class),
                    number(entry, "actorId"), optionalLong(entry, "victimId"), number(entry, "baseHalves"), number(entry, "bountyHalves"), number(entry, "passiveHalves")));
        return new EconomyState(bool(root, "enabled"), number(root, "whiteHalves"), number(root, "blackHalves"), marks, ledger);
    }

    private static DocumentBuilderFactory factory() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static Element child(Element parent, String name, Object... attributes) {
        Element result = parent.getOwnerDocument().createElement(name);
        parent.appendChild(result);
        attrs(result, attributes);
        return result;
    }

    private static void attrs(Element element, Object... pairs) {
        for (int i = 0; i < pairs.length; i += 2)
            if (pairs[i + 1] != null) element.setAttribute(pairs[i].toString(), pairs[i + 1] instanceof Abilities ability ? ability.id() : pairs[i + 1].toString());
    }

    private static void writeClock(Element entry, ClockSnapshot clock) {
        attrs(entry, "enabled", clock.enabled(), "running", clock.running(), "whiteNanos", clock.whiteNanos(), "blackNanos", clock.blackNanos(), "active", clock.activeColor());
    }

    private static ClockSnapshot readClock(Element entry) {
        return new ClockSnapshot(bool(entry, "enabled"), bool(entry, "running"), number(entry, "whiteNanos"), number(entry, "blackNanos"), enumeration(entry, "active", FigureColor.class));
    }

    private static void writeCaptures(Element parent, List<Figure> figures) {
        for (Figure figure : figures)
            child(parent, "figure", "type", figure.getClass().getSimpleName(), "color", figure.getColor(), "square", cell(figure.getPosition()));
    }

    private static List<Figure> readCaptures(Element parent, Field factory) {
        var result = new ArrayList<Figure>();
        for (Element entry : children(parent, "figure", SummonState.MAX_IDENTITIES))
            result.add(factory.createFigure(type(text(entry, "type")), requiredPosition(entry, "square"), enumeration(entry, "color", FigureColor.class)));
        return result;
    }

    private static List<Element> children(Element parent, String name, int maximum) {
        var result = new ArrayList<Element>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element entry) {
                require(entry.getTagName().equals(name), "Unexpected element: " + entry.getTagName());
                result.add(entry);
                require(result.size() <= maximum, "Too many " + name + " elements");
            }
        return result;
    }

    private static Element one(Element parent, String name) {
        Element result = null;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element entry && entry.getTagName().equals(name)) {
                require(result == null, "Duplicate " + name);
                result = entry;
            }
        require(result != null, "Missing " + name);
        return result;
    }

    private static String text(Element entry, String attribute) {
        require(entry.hasAttribute(attribute), "Missing " + entry.getTagName() + "." + attribute);
        String value = entry.getAttribute(attribute);
        require(value.length() <= 4096, "Attribute too long");
        return value;
    }

    private static String optional(Element entry, String attribute) {
        return entry.hasAttribute(attribute) ? text(entry, attribute) : null;
    }

    private static long number(Element entry, String attribute) {
        return Long.parseLong(text(entry, attribute));
    }

    private static int integer(Element entry, String attribute) {
        return Math.toIntExact(number(entry, attribute));
    }

    private static Long optionalLong(Element entry, String attribute) {
        return entry.hasAttribute(attribute) ? number(entry, attribute) : null;
    }

    private static boolean bool(Element entry, String attribute) {
        String value = text(entry, attribute);
        require(value.equals("true") || value.equals("false"), "Invalid boolean");
        return Boolean.parseBoolean(value);
    }

    private static <E extends Enum<E>> E enumeration(Element entry, String attribute, Class<E> type) {
        return Enum.valueOf(type, text(entry, attribute));
    }

    private static Abilities optionalAbility(Element entry, String attribute) {
        return entry.hasAttribute(attribute)
                ? game.heroes.HeroRegistry.standard().abilityById(text(entry, attribute)) : null;
    }

    private static <E extends Enum<E>> E optionalEnum(Element entry, String attribute, Class<E> type) {
        return entry.hasAttribute(attribute) ? enumeration(entry, attribute, type) : null;
    }

    private static Position requiredPosition(Element entry, String attribute) {
        Position p = position(entry, attribute);
        require(p != null, "Missing square");
        return p;
    }

    private static Position position(Element entry, String attribute) {
        String value = optional(entry, attribute);
        if (value == null) return null;
        require(value.matches("[a-h][1-8]"), "Invalid square");
        return new Position(value.charAt(1) - '1', value.charAt(0) - 'a');
    }

    private static String cell(Position p) {
        return p == null ? null : "" + (char) ('a' + p.getColumn()) + (p.getRow() + 1);
    }

    private static Class<? extends Figure> type(String name) {
        return switch (name) {
            case "Pawn" -> Pawn.class;
            case "Knight" -> Knight.class;
            case "Bishop" -> Bishop.class;
            case "Rook" -> Rook.class;
            case "Queen" -> Queen.class;
            case "King" -> King.class;
            default -> throw new IllegalArgumentException("Unsupported figure: " + name);
        };
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
